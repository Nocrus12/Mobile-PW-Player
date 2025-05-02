package com.example.lab_8_player


import android.content.Intent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.net.Uri
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var receiver: BroadcastReceiver

    // Keeps the last known playback state
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val miniPlayer: View = findViewById(R.id.mini_player)
        miniPlayer.visibility = View.GONE


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

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                // Read out the new state
                val title = intent.getStringExtra("title") ?: ""
                val artist = intent.getStringExtra("artist") ?: ""
                isPlaying = intent.getBooleanExtra("isPlaying", false)

                miniPlayer.visibility = View.VISIBLE

                // Update mini-player UI
                findViewById<TextView>(R.id.miniTitle).text = title
                findViewById<TextView>(R.id.miniArtist).text = artist
                findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(
                    if (isPlaying) R.drawable.baseline_pause_24_white else R.drawable.baseline_play_arrow_24_white
                )
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("PLAYBACK_STATE_CHANGED"))

        // 2) Wire up the mini-player buttons

        // Play / Pause toggle
        findViewById<ImageButton>(R.id.btnPlayPause).setOnClickListener {
            val action = if (isPlaying) "ACTION_PAUSE" else "ACTION_RESUME"
            ContextCompat.startForegroundService(
                this,
                Intent(this, PlaybackService::class.java).setAction(action)
            )

        }

        // Previous
        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                Intent(this, PlaybackService::class.java).setAction("ACTION_PREV")
            )
        }

        // Next
        findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                Intent(this, PlaybackService::class.java).setAction("ACTION_NEXT")
            )
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
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


