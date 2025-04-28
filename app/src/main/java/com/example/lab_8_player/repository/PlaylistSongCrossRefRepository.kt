package com.example.lab_8_player.repository

import com.example.lab_8_player.db.dao.PlaylistSongCrossRefDao
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.Song
import kotlinx.coroutines.flow.Flow

class PlaylistSongCrossRefRepository(private val playlistSongCrossRefDao: PlaylistSongCrossRefDao) {

    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef) {
        playlistSongCrossRefDao.insertCrossRef(crossRef)
    }

    suspend fun deleteCrossRef(crossRef: PlaylistSongCrossRef) {
        playlistSongCrossRefDao.deleteCrossRef(crossRef)
    }

    suspend fun getCrossRefsForSong(songId: Long): List<PlaylistSongCrossRef> {
        return playlistSongCrossRefDao.getCrossRefsForSong(songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistSongCrossRefDao.getSongsForPlaylist(playlistId)
    }
}
