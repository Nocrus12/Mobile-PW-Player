package com.example.lab_8_player.repository

import com.example.lab_8_player.db.dao.SongDao
import com.example.lab_8_player.db.model.Song
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getAllFavorites(): Flow<List<Song>> = songDao.getAllFavorites()

    fun getSongsByName(nameInput: String): Flow<List<Song>> = songDao.getSongsByName(nameInput)

    suspend fun getAllUris(): List<String> = songDao.getAllUris()

    suspend fun existsByUri(uri: String): Boolean = songDao.existsByUri(uri)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)

    suspend fun updateSong(songId: Long, isFavorite: Boolean) = songDao.updateSong(songId, isFavorite)

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)
}
