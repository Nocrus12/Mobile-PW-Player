package com.example.lab_8_player.fragment

import android.app.AlertDialog.Builder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab_8_player.R
import com.example.lab_8_player.adapter.AllSongsAdapter
import com.example.lab_8_player.databinding.FragmentSearchBinding
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.PlaylistRepository
import com.example.lab_8_player.repository.PlaylistSongCrossRefRepository
import com.example.lab_8_player.repository.SongRepository
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModel
import com.example.lab_8_player.viewmodel.PlaylistSongCrossRefViewModelFactory
import com.example.lab_8_player.viewmodel.PlaylistViewModel
import com.example.lab_8_player.viewmodel.PlaylistViewModelFactory
import com.example.lab_8_player.viewmodel.SongViewModel
import com.example.lab_8_player.viewmodel.SongViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AllSongsAdapter
    private var searchJob: Job? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupSearchInput()
    }

    private fun setupRecyclerView() {
        adapter = AllSongsAdapter(requireContext(),
            onFavoriteClick = { song -> toggleFavorite(song) },
            onAddToPlaylistClick = { song -> showAddToPlaylistDialog(song) }
        )
        binding.songsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.songsRecyclerView.adapter = adapter
    }

    private fun setupSearchInput() {
        binding.searchInput.addTextChangedListener { editable ->
            val query = editable.toString()
            searchJob?.cancel()

            if (query.isEmpty()) {
                adapter.differ.submitList(emptyList())
                binding.textNoResults.visibility = View.GONE
                return@addTextChangedListener
            }

            searchJob = lifecycleScope.launch {
                songViewModel.getSongsByName(query).collectLatest { songs ->
                    adapter.differ.submitList(songs)
                    binding.textNoResults.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
