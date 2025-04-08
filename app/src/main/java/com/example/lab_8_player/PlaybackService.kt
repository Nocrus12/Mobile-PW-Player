package com.example.lab_8_player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat

class MusicPlayerService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "music_playback_channel"
    private val NOTIF_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (!::mediaPlayer.isInitialized) {
            // TODO Make library parser. BG service
            mediaPlayer = MediaPlayer.create(this, R.raw.sample_track) // Add `sample_music.mp3` in res/raw
        }

        when (action) {
            "ACTION_PLAY" -> {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            }
            "ACTION_PAUSE" -> {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
            "ACTION_STOP" -> {
                stopSelf()
            }
            "ACTION_NEXT" -> {
                Toast.makeText(this, "Next track", Toast.LENGTH_SHORT).show()
            }
            "ACTION_PREVIOUS" -> {
                Toast.makeText(this, "Previous track", Toast.LENGTH_SHORT).show()
            }
        }

        startForeground(NOTIF_ID, createNotification())

        return START_STICKY
    }

    override fun onDestroy() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val playIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_PLAY" }
        val pauseIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_PAUSE" }
        val stopIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_STOP" }
        val nextIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_NEXT" }
        val previousIntent = Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_PREVIOUS" }

        val playPending = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopPending = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevPending = PendingIntent.getService(this, 4, previousIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText("Sample Music")
            .setSmallIcon(R.drawable.music_note)
            .addAction(R.drawable.prev, "Prev", prevPending)
            .addAction(R.drawable.play, "Play", playPending)
            .addAction(R.drawable.pause, "Pause", pausePending)
            .addAction(R.drawable.next, "Next", nextPending)
            .addAction(R.drawable.stop, "Stop", stopPending)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
