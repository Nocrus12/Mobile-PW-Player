package com.example.lab_8_player


import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MusicLibrary.tracks.clear()

        setContentView(R.layout.activity_main)

        // Setting up navigation
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        NavigationUI.setupWithNavController(navView, navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO),
                0
            )
        }

        // Setting up and checking Shared Preferences
        val savedUri = getSharedPreferences("music_prefs", MODE_PRIVATE)
            .getString("music_folder_uri", null)

        // Check if the folder URI is saved
        if (savedUri != null) {
            val uri = Uri.parse(savedUri)
            MusicFetchWorker.musicFolderUri = uri

            // Re-request permission to ensure it's valid
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Check if tracks have already been fetched
                if (MusicLibrary.tracks.isEmpty()) {
                    // Only trigger the worker if tracks are not already fetched
                    val fetchRequest = OneTimeWorkRequestBuilder<MusicFetchWorker>().build()
                    WorkManager.getInstance(this).enqueue(fetchRequest)
                }
            } catch (e: SecurityException) {
                // If permission failed, fallback to asking again
                pickMusicFolder.launch(null)
            }
        } else {
            // No saved URI, prompt user to pick folder
            pickMusicFolder.launch(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

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
}


