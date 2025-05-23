package com.example.lab_8_player.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lab_8_player.db.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE id = (:id)")
    suspend fun getSongById(id: Long): Song

    @Query("SELECT * FROM song WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Int>): List<Song>

    @Query("SELECT * FROM Song WHERE name LIKE '%' || :nameInput || '%'")
    fun getSongsByName(nameInput: String): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE isFavorite = true")
    fun getAllFavorites(): Flow<List<Song>>

    @Query("SELECT EXISTS(SELECT 1 FROM Song WHERE uri = :uri)")
    suspend fun existsByUri(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("UPDATE Song SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun toggleFavSong(songId: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteSong(song: Song)
}
