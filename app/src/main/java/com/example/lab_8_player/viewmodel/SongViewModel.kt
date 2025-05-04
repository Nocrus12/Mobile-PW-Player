package com.example.lab_8_player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.SongRepository
import kotlinx.coroutines.launch

class SongViewModel(app: Application, private val songRepository: SongRepository) : AndroidViewModel(app) {

    fun getAllSongs() = songRepository.getAllSongs()

    fun getAllFavorites() = songRepository.getAllFavorites()

    suspend fun getSongById(id: Long) = songRepository.getSongById(id)

    fun getSongsByName(nameInput: String) = songRepository.getSongsByName(nameInput)

    fun toggleFavSong(songId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            songRepository.toggleFavSong(songId, isFavorite)
        }
    }

    // Left to keep full CRUD
    fun deleteSong(song: Song) = viewModelScope.launch {
        songRepository.deleteSong(song)
    }
}
