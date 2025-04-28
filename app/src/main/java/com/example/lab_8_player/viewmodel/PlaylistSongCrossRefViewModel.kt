package com.example.lab_8_player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.PlaylistSongCrossRefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistSongCrossRefViewModel(app: Application,
    private val playlistSongCrossRefRepository: PlaylistSongCrossRefRepository) : AndroidViewModel(app) {

    fun insertCrossRef(crossRef: PlaylistSongCrossRef) {
        viewModelScope.launch {
            playlistSongCrossRefRepository.insertCrossRef(crossRef)
        }
    }

    fun deleteCrossRef(crossRef: PlaylistSongCrossRef) {
        viewModelScope.launch {
            playlistSongCrossRefRepository.deleteCrossRef(crossRef)
        }
    }

    suspend fun getCrossRefsForSong(songId: Long): List<PlaylistSongCrossRef> {
        return withContext(Dispatchers.IO) {
            playlistSongCrossRefRepository.getCrossRefsForSong(songId)
        }
    }

    suspend fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return withContext(Dispatchers.IO) {
            playlistSongCrossRefRepository.getSongsForPlaylist(playlistId)
        }
    }
}
