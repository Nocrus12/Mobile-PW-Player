package com.example.lab_8_player


import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lab_8_player.ui.theme.Lab_8_PlayerTheme
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*


class MainActivity : ComponentActivity() {

    private val pickMusicFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                // Persist permission
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Save URI
                getSharedPreferences("music_prefs", MODE_PRIVATE).edit()
                    .putString("music_folder_uri", it.toString())
                    .apply()

                // Pass URI and start worker
                MusicFetchWorker.musicFolderUri = it
                val fetchRequest = OneTimeWorkRequestBuilder<MusicFetchWorker>().build()
                WorkManager.getInstance(this).enqueue(fetchRequest)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO),
                0
            )
        }

        val savedUri = getSharedPreferences("music_prefs", MODE_PRIVATE)
            .getString("music_folder_uri", null)

        if (savedUri != null) {
            val uri = Uri.parse(savedUri)
            MusicFetchWorker.musicFolderUri = uri

            // Re-request permission to ensure it's valid
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Launch worker
                val fetchRequest = OneTimeWorkRequestBuilder<MusicFetchWorker>().build()
                WorkManager.getInstance(this).enqueue(fetchRequest)
            } catch (e: SecurityException) {
                // If permission failed, fallback to asking again
                pickMusicFolder.launch(null)
            }
        } else {
            pickMusicFolder.launch(null)
        }


        pickMusicFolder.launch(null)

        val fetchRequest = OneTimeWorkRequestBuilder<MusicFetchWorker>().build()
        WorkManager.getInstance(this).enqueue(fetchRequest)

        setContent {
            Lab_8_PlayerTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(onClick = {
                        val intent = Intent(applicationContext, PlaybackService::class.java).apply {
                            action = "ACTION_PLAY"
                        }
                        ContextCompat.startForegroundService(applicationContext, intent)
                    }) {
                        Text("Start Service")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val intent = Intent(applicationContext, PlaybackService::class.java).apply {
                            action = "ACTION_STOP"
                        }
                        ContextCompat.startForegroundService(applicationContext, intent)
                    }) {
                        Text("Stop Service")
                    }
                }
            }
        }
    }
}

