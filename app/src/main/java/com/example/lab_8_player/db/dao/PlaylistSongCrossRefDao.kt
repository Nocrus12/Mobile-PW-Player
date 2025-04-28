package com.example.lab_8_player.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.lab_8_player.db.model.PlaylistSongCrossRef
import com.example.lab_8_player.db.model.PlaylistWithSongs
import com.example.lab_8_player.db.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("SELECT * FROM PlaylistSongCrossRef WHERE songId = :songId")
    suspend fun getCrossRefsForSong(songId: Long): List<PlaylistSongCrossRef>

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    suspend fun deleteAllCrossRefsForPlaylist(playlistId: Long)

    @Transaction
    @Query("""
        SELECT * FROM Playlist
        WHERE id = :playlistId
    """)
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs>

    @Transaction
    @Query("""
    SELECT * FROM PlaylistSongCrossRef
    INNER JOIN Song ON PlaylistSongCrossRef.songId = Song.id
    WHERE PlaylistSongCrossRef.playlistId = :playlistId
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>

}
