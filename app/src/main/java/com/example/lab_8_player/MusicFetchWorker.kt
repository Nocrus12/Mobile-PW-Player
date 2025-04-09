package com.example.lab_8_player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicFetchWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        var musicFolderUri: Uri? = null
    }

    private val supportedExtensions = listOf("mp3", "wav", "ogg")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            MusicLibrary.tracks.clear()

            val uri = musicFolderUri ?: return@withContext Result.failure()

            val rootFolder = DocumentFile.fromTreeUri(context, uri)
            rootFolder?.listFiles()?.forEach { file ->
                if (file.isFile && isAudio(file.name ?: "")) {
                    MusicLibrary.tracks.add(file.uri)
                }
            }

            if (MusicLibrary.tracks.isNotEmpty()) {
                val startIntent = Intent(applicationContext, PlaybackService::class.java).apply {
                    action = "ACTION_PLAY"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(startIntent)
                } else {
                    applicationContext.startService(startIntent)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun isAudio(fileName: String): Boolean {
        return supportedExtensions.any { fileName.endsWith(".$it", ignoreCase = true) }
    }
}
