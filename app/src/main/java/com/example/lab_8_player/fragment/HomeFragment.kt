package com.example.lab_8_player.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.adapter.AllSongsAdapter
import com.example.lab_8_player.adapter.FavoriteSongsAdapter
import com.example.lab_8_player.adapter.PlaylistsAdapter
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var allSongsRecyclerView: RecyclerView

    private lateinit var favoriteAdapter: FavoriteSongsAdapter
    private lateinit var playlistAdapter: PlaylistsAdapter
    private lateinit var allSongsAdapter: AllSongsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView)
        playlistsRecyclerView = view.findViewById(R.id.playlistsRecyclerView)
        allSongsRecyclerView = view.findViewById(R.id.all_songs_recycler_view)

        val context = requireContext()

        // Initialize adapters with empty lists
        favoriteAdapter = FavoriteSongsAdapter(context, mutableListOf())
        playlistAdapter = PlaylistsAdapter(context, mutableListOf())
        allSongsAdapter = AllSongsAdapter(context, mutableListOf())

        favoritesRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        playlistsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        allSongsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        favoritesRecyclerView.adapter = favoriteAdapter
        playlistsRecyclerView.adapter = playlistAdapter
        allSongsRecyclerView.adapter = allSongsAdapter

        loadData()

        val startIntent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_START"
        }
        ContextCompat.startForegroundService(context, startIntent)
    }

    private fun loadData() {
        val context = requireContext().applicationContext
        val db = AppDatabase.getInstance(context)
        val songDao = db.songDao()
        val playlistDao = db.playlistDao()

        lifecycleScope.launch {
            val favorites = withContext(Dispatchers.IO) { songDao.getAllFavorites() }
            val playlists = withContext(Dispatchers.IO) { playlistDao.getAllPlaylists() }
            val allSongs = withContext(Dispatchers.IO) { songDao.getAllSongs() }

            // Replace contents without creating new adapter
            (favoriteAdapter.songList as MutableList).apply {
                clear()
                addAll(favorites)
            }
            favoriteAdapter.notifyDataSetChanged()

            (playlistAdapter.playlists as MutableList).apply {
                clear()
                addAll(playlists)
            }
            playlistAdapter.notifyDataSetChanged()

            (allSongsAdapter.songList as MutableList).apply {
                clear()
                addAll(allSongs)
            }
            allSongsAdapter.notifyDataSetChanged()
        }
    }
}


// TODO Make Favorites section conditional (show if exist)
// TODO Add button "Create playlist" which triggers Alert dialog window
// TODO Make initial click on a song trigger ACTION_PLAY so it starts playing automatically. Handle ACTION_START in other way
// TODO Make Playback service ACTION_PLAY trigger with specific song passed from the Home fragment (currently it starts playing one particular song)



// TODO Improve item_track layout:
//  Format duration ms to mm:ss
//  Add button "Add to favorites"
//  Add button "Add to playlist"
