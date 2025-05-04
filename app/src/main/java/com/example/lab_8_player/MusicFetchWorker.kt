package com.example.lab_8_player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicFetchWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        var musicFolderUri: Uri? = null
    }


    override suspend fun doWork(): Result {
        return try {

            val db by lazy { AppDatabase.getInstance(context.applicationContext) }
            val songRepository = SongRepository(db.songDao())


            val root = MusicFetchWorker.musicFolderUri
                ?.let { DocumentFile.fromTreeUri(context, it) }

            val songs = root
                ?.listFiles()
                ?.filter { it.isFile && isAudio(it.name.orEmpty()) }
                ?.map { file ->
                    // 1. Extract raw metadata
                    val retriever = MediaMetadataRetriever().apply {
                        setDataSource(context, file.uri)
                    }
                    val rawTitle    = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val rawArtist   = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    retriever.release()

                    // 2. Derive fallback filename (no extension)
                    val fileName = file.name
                        .orEmpty()
                        .substringBeforeLast('.')
                        .substringAfterLast('/')

                    // 3. Build Song entity with safe defaults
                    Song(
                        uri      = file.uri.toString(),
                        name     = rawTitle.takeIf { !it.isNullOrBlank() }
                            ?: fileName,
                        artist   = rawArtist.takeIf { !it.isNullOrBlank() }
                            ?: "Unknown Artist",
                        duration = rawDuration
                            ?.toLongOrNull()
                            ?.coerceAtMost(Int.MAX_VALUE.toLong())
                            ?.toInt()
                            ?: 0
                    )
                }

                .orEmpty()

            if (songs.isNotEmpty()) {
                val uniqueSongs = withContext(Dispatchers.IO) {
                    songs.filterNot { songRepository.existsByUri(it.uri) }
                }

                if (uniqueSongs.isNotEmpty()) {
                    songRepository.insertSongs(uniqueSongs)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun isAudio(fileName: String) =
        listOf("mp3", "wav", "ogg").any { fileName.endsWith(".$it", ignoreCase = true) }
}

