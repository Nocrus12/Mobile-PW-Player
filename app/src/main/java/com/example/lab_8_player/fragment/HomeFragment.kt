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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.navigation.findNavController
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.PlaylistSongCrossRefRepository
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModel
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
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
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_playlist, null)

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(dialogView)

        val input = dialogView.findViewById<EditText>(R.id.playlistNameInput)
        val createButton = dialogView.findViewById<ImageButton>(R.id.createButton)
        val cancelButton = dialogView.findViewById<ImageButton>(R.id.cancelButton)

        createButton.setOnClickListener {
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                playlistViewModel.insertPlaylist(name)
                bottomSheetDialog.dismiss()
            } else {
                input.error = "Name can't be empty"
            }
        }

        cancelButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }


    private fun showAddToPlaylistDialog(song: Song) {
        lifecycleScope.launch {
            val playlists = playlistViewModel.getAllPlaylists().firstOrNull() ?: emptyList()
            val playlistNames = playlists.map { it.name }
            val existingCrossRefs = playlistSongCrossRefViewModel.getCrossRefsForSong(song.id)
            val checkedItems = BooleanArray(playlistNames.size)

            playlists.forEachIndexed { index, playlist ->
                checkedItems[index] = existingCrossRefs.any { it.playlistId == playlist.id }
            }

            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_to_playlist, null)

            val checkboxContainer = dialogView.findViewById<LinearLayout>(R.id.playlistCheckboxContainer)

            // Dynamically add checkboxes
            val inflater = LayoutInflater.from(requireContext())

            playlistNames.forEachIndexed { index, name ->
                val checkbox = inflater.inflate(R.layout.item_playlist_checkbox, checkboxContainer, false) as CheckBox
                checkbox.text = name
                checkbox.isChecked = checkedItems[index]

                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    val crossRef = PlaylistSongCrossRef(playlists[index].id, song.id)
                    lifecycleScope.launch {
                        if (isChecked) {
                            playlistSongCrossRefViewModel.insertCrossRef(crossRef)
                        } else {
                            playlistSongCrossRefViewModel.deleteCrossRef(crossRef)
                        }
                    }
                }

                checkboxContainer.addView(checkbox)
            }


            val dialog = Builder(requireContext())
                .setView(dialogView)
                .create()

            val addButton = dialogView.findViewById<ImageButton>(R.id.addButton)
            val cancelButton = dialogView.findViewById<ImageButton>(R.id.cancelButton)

            addButton.setOnClickListener {
                dialog.dismiss()
            }

            cancelButton.setOnClickListener {
                dialog.cancel()
            }

            dialog.show()
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