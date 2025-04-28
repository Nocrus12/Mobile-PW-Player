package com.example.lab_8_player.repository

import com.example.lab_8_player.db.dao.PlaylistDao
import com.example.lab_8_player.db.model.Playlist
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    suspend fun getPlaylistById(playlistId: Long): Flow<Playlist> = playlistDao.getPlaylistById(playlistId)
    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)
    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)
}
