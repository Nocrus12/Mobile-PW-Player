package com.example.lab_8_player.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.lab_8_player.db.model.Song

class HomeFragment : Fragment() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var allSongsRecyclerView: RecyclerView

    private lateinit var favoriteAdapter: FavoriteSongsAdapter
    private lateinit var playlistAdapter: PlaylistsAdapter
    private lateinit var allSongsAdapter: AllSongsAdapter

    private lateinit var favoritesSection: View


    // Initialize database once
    private val db by lazy { AppDatabase.getInstance(requireContext().applicationContext) }

    // ViewModels with factories
    private val songViewModel: SongViewModel by viewModels {
        SongViewModelFactory(requireActivity().application, SongRepository(db.songDao()))
    }

    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application, PlaylistRepository(db.playlistDao()))
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
        playlistAdapter = PlaylistsAdapter(requireContext()) { showAddPlaylistDialog() }
        allSongsAdapter = AllSongsAdapter(requireContext()) { song -> toggleFavorite(song) }


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

        val startIntent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_START"
        }
        context?.let { ContextCompat.startForegroundService(it, startIntent) }
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

    private fun toggleFavorite(song: Song) {
        songViewModel.updateFavorite(song.id, !song.isFavorite)
    }



}

// TODO Fix: on init app start empty recyclers and no FG service. Fixes after switching pages/app restart
// TODO Fix: Flow is not quite live (Flow emissions are viewed only after Homepage reloaded) on first app launch

// TODO Make Favorites section view conditional (show if exist)
// TODO Add button "Create playlist" which triggers Alert dialog window

// TODO Improve item_track layout:
//  Format duration ms to mm:ss
//  Add button "Add to favorites"
//  Add button "Add to playlist"

// Recyclers works fine, live updates from Flow emissions are viewed instantly