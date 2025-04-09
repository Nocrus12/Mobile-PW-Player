package com.example.lab_8_player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicFetchWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val supportedExtensions = listOf("mp3", "wav", "ogg")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            MusicLibrary.tracks.clear()

            val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Media._ID)

            val selection = supportedExtensions.joinToString(" OR ") {
                "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.${it}'"
            }

            val cursor = context.contentResolver.query(
                musicUri,
                projection,
                selection,
                null,
                null
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(musicUri, id.toString())
                    MusicLibrary.tracks.add(contentUri)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
