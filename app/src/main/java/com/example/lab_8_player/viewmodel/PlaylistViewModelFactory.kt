package com.example.lab_8_player.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lab_8_player.repository.PlaylistRepository

class PlaylistViewModelFactory(
    private val app: Application,
    private val playlistRepository: PlaylistRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            return PlaylistViewModel(app, playlistRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
