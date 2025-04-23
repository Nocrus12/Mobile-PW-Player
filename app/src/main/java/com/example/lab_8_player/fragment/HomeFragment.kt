package com.example.lab_8_player.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab_8_player.adapter.FavoriteTracksAdapter
import com.example.lab_8_player.adapter.PlaylistAdapter
import com.example.lab_8_player.adapter.TrackAdapter
import com.example.lab_8_player.databinding.FragmentHomeBinding
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.db.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var trackAdapter: TrackAdapter
    private lateinit var favoriteAdapter: FavoriteTracksAdapter
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        db = AppDatabase.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupAdapters()
        loadData()
    }

    private fun setupAdapters() {
        trackAdapter = TrackAdapter(emptyList()) { song ->
            // TODO: handle song click
        }

        favoriteAdapter = FavoriteTracksAdapter(emptyList()) { song ->
            // TODO: handle favorite song click
        }

        playlistAdapter = PlaylistAdapter(emptyList()) { playlist ->
            // TODO: handle playlist click
        }

        binding.recyclerAllTracks.apply {
            adapter = trackAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.recyclerFavorites.apply {
            adapter = favoriteAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.recyclerPlaylists.apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val allTracks = withContext(Dispatchers.IO) {
                db.songDao().getAllSongs()
            }
            val favorites = withContext(Dispatchers.IO) {
                db.songDao().getAllFavorites()
            }
            val playlists = withContext(Dispatchers.IO) {
                db.playlistDao().getAllPlaylists()
            }

            trackAdapter.updateData(allTracks)
            favoriteAdapter.updateData(favorites)
            playlistAdapter.updateData(playlists)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
