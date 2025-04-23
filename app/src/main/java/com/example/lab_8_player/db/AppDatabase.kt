package com.example.lab_8_player.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lab_8_player.db.dao.PlaylistDao
import com.example.lab_8_player.db.dao.PlaylistSongCrossRefDao
import com.example.lab_8_player.db.dao.SongDao
import com.example.lab_8_player.db.model.Playlist
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song

@Database(
    entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongCrossRefDao(): PlaylistSongCrossRefDao
}

