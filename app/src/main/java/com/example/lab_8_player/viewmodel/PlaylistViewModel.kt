package com.example.lab_8_player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab_8_player.db.model.Playlist
import com.example.lab_8_player.repository.PlaylistRepository
import kotlinx.coroutines.launch

class PlaylistViewModel(app: Application, private val playlistRepository: PlaylistRepository) : AndroidViewModel(app) {

    fun getAllPlaylists() = playlistRepository.getAllPlaylists()

    fun getPlaylistById(playlistId: Long) = playlistRepository.getPlaylistById(playlistId)

    fun insertPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.insertPlaylist(Playlist(name = name))
        }
    }


    fun updatePlaylist(playlist: Playlist) = viewModelScope.launch {
        playlistRepository.updatePlaylist(playlist)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch {
        playlistRepository.deletePlaylist(playlist)
    }
}
