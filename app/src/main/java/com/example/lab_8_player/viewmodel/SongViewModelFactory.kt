package com.example.lab_8_player.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lab_8_player.repository.SongRepository

class SongViewModelFactory(
    private val app: Application,
    private val songRepository: SongRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            return SongViewModel(app, songRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
