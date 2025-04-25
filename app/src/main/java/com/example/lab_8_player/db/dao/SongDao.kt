package com.example.lab_8_player.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lab_8_player.db.model.Song

@Dao
interface SongDao {

    @Query("SELECT * FROM Song")
    suspend fun getAllSongs(): List<Song>

    @Query("SELECT * FROM Song WHERE id = :songId")
    fun getSongById(songId: Long): Song?

    @Query("SELECT uri FROM Song")
    suspend fun getAllUris(): List<String>

    @Query("SELECT * FROM Song WHERE isFavorite = true")
    suspend fun getAllFavorites(): List<Song>

    @Query("SELECT EXISTS(SELECT 1 FROM Song WHERE uri = :uri)")
    suspend fun existsByUri(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)
}
