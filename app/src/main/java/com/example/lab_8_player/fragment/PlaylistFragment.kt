package com.example.lab_8_player.fragment

import android.app.AlertDialog.Builder
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.PlaybackService
import com.example.lab_8_player.R
import com.example.lab_8_player.adapter.AllSongsAdapter
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.repository.PlaylistSongCrossRefRepository
import com.example.lab_8_player.repository.SongRepository
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModel
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModelFactory
import com.example.lab_8_player.viewmodel.SongViewModel
import com.example.lab_8_player.viewmodel.SongViewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.lab_8_player.databinding.FragmentPlaylistBinding
import com.example.lab_8_player.db.model.Playlist
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.PlaylistRepository
import com.example.lab_8_player.viewmodel.PlaylistViewModel
import com.example.lab_8_player.viewmodel.PlaylistViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.ArrayList

class PlaylistFragment : Fragment() {

    private lateinit var songsRecyclerView: RecyclerView
    private lateinit var allSongsAdapter: AllSongsAdapter
    private lateinit var playlist: Playlist
    private lateinit var binding: FragmentPlaylistBinding

    private lateinit var playlistSongs: List<Song>

    private val db by lazy { AppDatabase.getInstance(requireContext().applicationContext) }

    private val songViewModel: SongViewModel by viewModels {
        SongViewModelFactory(requireActivity().application, SongRepository(db.songDao()))
    }
    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application, PlaylistRepository(db.playlistDao()))
    }
    private val playlistSongCrossRefViewModel: PlaylistSongCrossRefViewModel by viewModels {
        PlaylistSongCrossRefViewModelFactory(requireActivity().application, PlaylistSongCrossRefRepository(db.playlistSongCrossRefDao()))
    }

    private val args: PlaylistFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        songsRecyclerView = view.findViewById(R.id.songsRecyclerView)

        allSongsAdapter = AllSongsAdapter(
            requireContext(),
            onFavoriteClick = { song -> toggleFavorite(song) },
            onAddToPlaylistClick = { song -> showAddToPlaylistDialog(song) }
        )

        songsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = allSongsAdapter
        }

        // Load data
        observeSongs()

        view.findViewById<View>(R.id.btnBackToHome)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // "Play Playlist" button
        binding.btnPlayPlaylist.setOnClickListener {

            if (!playlistSongs.isEmpty()) {
                val intent = Intent(context, PlaybackService::class.java).apply {
                    action = "ACTION_BEGIN"
                    putParcelableArrayListExtra("EXTRA_SONG_LIST", ArrayList(playlistSongs))
                    putExtra("EXTRA_PLAY_INDEX", 0)
                }
                ContextCompat.startForegroundService(requireContext(), intent)
            }
        }

        // Dropdown menu
        binding.btnPlaylistMenu.setOnClickListener { view ->
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.playlist_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename -> {
                        showRenameDialog()
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmation()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private fun showRenameDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            setText(playlist.name)
            selectAll()
        }

        Builder(requireContext())
            .setTitle("Rename Playlist")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != playlist.name) {
                    // updateViewModel
                    playlistViewModel.updatePlaylist(
                        playlist.copy(name = newName)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        Builder(requireContext())
            .setTitle("Delete Playlist?")
            .setMessage("This will remove the playlist but keep the songs. Are you sure?")
            .setPositiveButton("Delete") { dialog, _ ->
                playlistViewModel.deletePlaylist(playlist)
                dialog.dismiss()
                // navigate back to home
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeSongs() {
        val playlistId = args.playlistId

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(STARTED) {
                playlistSongCrossRefViewModel.getSongsForPlaylist(playlistId).collectLatest { songs ->
                    playlistSongs = songs
                    allSongsAdapter.differ.submitList(songs)

                    binding.tvSongCount.text = "${songs.size} song${if (songs.size == 1) "" else "s"}"

                    val totalMs = songs.sumOf { it.duration.toLong() }
                    val minutes = (totalMs / 1000 / 60)
                    val seconds = (totalMs / 1000 % 60)
                    binding.tvTotalDuration.text = String.format("%d:%02d", minutes, seconds)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playlistViewModel.getPlaylistById(playlistId).collectLatest { loadedPlaylist ->
                playlist = loadedPlaylist
                binding.playlist = loadedPlaylist
            }
        }
    }


    private fun toggleFavorite(song: Song) {
        songViewModel.toggleFavSong(song.id, !song.isFavorite)
    }

    private fun showAddToPlaylistDialog(song: Song) {
        val builder = Builder(requireContext())
        builder.setTitle("Add to Playlist")

        // Collect playlists asynchronously from Flow
        lifecycleScope.launch {
            val playlists = playlistViewModel.getAllPlaylists().firstOrNull() ?: emptyList()
            val playlistNames = playlists.map { it.name } // Extract playlist names

            // Fetch the cross-references (playlist-song relationships) for the song
            val existingCrossRefs = playlistSongCrossRefViewModel.getCrossRefsForSong(song.id)

            // Create an array to keep track of checked states for each playlist
            val checkedItems = BooleanArray(playlistNames.size)

            // Mark the checkboxes that are already associated with the song
            playlists.forEachIndexed { index, playlist ->
                checkedItems[index] = existingCrossRefs.any { it.playlistId == playlist.id }
            }

            // Show the multi-choice dialog
            builder.setMultiChoiceItems(playlistNames.toTypedArray(), checkedItems) { _, index, isChecked ->
                if (isChecked) {
                    // Add the song to the playlist
                    val crossRef = PlaylistSongCrossRef(playlists[index].id, song.id)
                    lifecycleScope.launch {
                        playlistSongCrossRefViewModel.insertCrossRef(crossRef)
                    }
                } else {
                    // Remove the song from the playlist
                    val crossRef = PlaylistSongCrossRef(playlists[index].id, song.id)
                    lifecycleScope.launch {
                        playlistSongCrossRefViewModel.deleteCrossRef(crossRef)
                    }
                }
            }

            builder.setPositiveButton("Add") { dialog, _ ->
                // You can add any final operations here if needed before dismissing
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }
    }
}