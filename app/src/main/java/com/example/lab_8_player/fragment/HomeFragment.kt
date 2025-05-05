package com.example.lab_8_player.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.adapter.AllSongsAdapter
import com.example.lab_8_player.adapter.FavoriteSongsAdapter
import com.example.lab_8_player.adapter.PlaylistsAdapter
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.repository.PlaylistRepository
import com.example.lab_8_player.repository.SongRepository
import com.example.lab_8_player.viewmodel.PlaylistViewModel
import com.example.lab_8_player.viewmodel.PlaylistViewModelFactory
import com.example.lab_8_player.viewmodel.SongViewModel
import com.example.lab_8_player.viewmodel.SongViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.app.AlertDialog.Builder
import androidx.navigation.findNavController
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.PlaylistSongCrossRefRepository
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModel
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModelFactory
import kotlinx.coroutines.flow.firstOrNull

class HomeFragment : Fragment() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var allSongsRecyclerView: RecyclerView

    private lateinit var favoriteAdapter: FavoriteSongsAdapter
    private lateinit var playlistAdapter: PlaylistsAdapter
    private lateinit var allSongsAdapter: AllSongsAdapter

    private lateinit var favoritesSection: View

    private var currentSongList: List<Song> = emptyList()



    // Initialize database once
    private val db by lazy { AppDatabase.getInstance(requireContext().applicationContext) }

    // ViewModels with factories
    private val songViewModel: SongViewModel by viewModels {
        SongViewModelFactory(requireActivity().application, SongRepository(db.songDao()))
    }

    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application, PlaylistRepository(db.playlistDao()))
    }
    private val playlistSongCrossRefViewModel: PlaylistSongCrossRefViewModel by viewModels {
        PlaylistSongCrossRefViewModelFactory(requireActivity().application, PlaylistSongCrossRefRepository(db.playlistSongCrossRefDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoritesSection = view.findViewById(R.id.favoritesSection)

        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView)
        playlistsRecyclerView = view.findViewById(R.id.playlistsRecyclerView)
        allSongsRecyclerView = view.findViewById(R.id.all_songs_recycler_view)

        favoriteAdapter = FavoriteSongsAdapter(requireContext())
        playlistAdapter = PlaylistsAdapter(
            requireContext(),
            onAddPlaylistClicked = { showAddPlaylistDialog() },
            onPlaylistClicked = { playlistId -> openPlaylistFragment(playlistId) }
        )

        allSongsAdapter = AllSongsAdapter(requireContext(),
            onFavoriteClick = { song -> toggleFavorite(song) },
            onAddToPlaylistClick = { song -> showAddToPlaylistDialog(song) }
        )

        favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoriteAdapter
        }

        playlistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = playlistAdapter
        }

        allSongsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = allSongsAdapter
        }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(STARTED) {

                launch {
                    songViewModel.getAllFavorites().collectLatest { favorites ->
                        favoriteAdapter.differ.submitList(favorites)

                        // Hide favorites section if no favorites
                        favoritesSection.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    playlistViewModel.getAllPlaylists().collectLatest { playlists ->
                        playlistAdapter.differ.submitList(playlists)
                    }
                }
                launch {
                    songViewModel.getAllSongs().collectLatest { allSongs ->
                        currentSongList = allSongs
                        allSongsAdapter.differ.submitList(allSongs)
                    }
                }
            }
        }
    }

    private fun showAddPlaylistDialog() {
        val builder = Builder(requireContext())
        builder.setTitle("Create New Playlist")

        val input = android.widget.EditText(requireContext())
        input.hint = "Playlist name"
        builder.setView(input)

        builder.setPositiveButton("Create") { dialog, _ ->
            val playlistName = input.text.toString().trim()
            if (playlistName.isNotEmpty()) {
                playlistViewModel.insertPlaylist(playlistName)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
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

    private fun toggleFavorite(song: Song) {
        songViewModel.toggleFavSong(song.id, !song.isFavorite)
    }

    private fun openPlaylistFragment(playlistId: Long) {
        val action = HomeFragmentDirections.actionHomeFragmentToPlaylistFragment(playlistId)
        requireView().findNavController().navigate(action)
    }


}

// TODO Improve item_track layout:
//  Format duration ms to mm:ss