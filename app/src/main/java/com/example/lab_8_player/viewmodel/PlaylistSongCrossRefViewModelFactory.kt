package com.example.lab_8_player.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lab_8_player.repository.PlaylistSongCrossRefRepository

class PlaylistSongCrossRefViewModelFactory(
    private val app: Application,
    private val playlistSongCrossRefRepository: PlaylistSongCrossRefRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistSongCrossRefViewModel::class.java)) {
            return PlaylistSongCrossRefViewModel(app, playlistSongCrossRefRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}