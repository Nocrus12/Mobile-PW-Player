package com.example.lab_8_player.repository

import com.example.lab_8_player.db.dao.SongDao
import com.example.lab_8_player.db.model.Song
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getAllFavorites(): Flow<List<Song>> = songDao.getAllFavorites()

    suspend fun getSongById(id: Long): Song = songDao.getSongById(id)

    fun getSongsByName(nameInput: String): Flow<List<Song>> = songDao.getSongsByName(nameInput)

    suspend fun existsByUri(uri: String): Boolean = songDao.existsByUri(uri)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)

    suspend fun toggleFavSong(songId: Long, isFavorite: Boolean) = songDao.toggleFavSong(songId, isFavorite)

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)
}
