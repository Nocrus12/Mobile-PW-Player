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

    fun getSongsByName(nameInput: String) = songRepository.getSongsByName(nameInput)

    suspend fun getAllUris() = songRepository.getAllUris()

    suspend fun existsByUri(uri: String) = songRepository.existsByUri(uri)

    fun insertSongs(songs: List<Song>) = viewModelScope.launch {
        songRepository.insertSongs(songs)
    }

    fun updateFavorite(songId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            songRepository.updateSong(songId, isFavorite)
        }
    }



    fun deleteSong(song: Song) = viewModelScope.launch {
        songRepository.deleteSong(song)
    }
}
