package com.example.lab_8_player.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val name: String,
    val artist: String,
    val duration: Int,
    val isFavorite: Boolean = false
)
