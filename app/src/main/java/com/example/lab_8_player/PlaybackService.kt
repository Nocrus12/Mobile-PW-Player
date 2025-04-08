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
import androidx.core.app.NotificationCompat


class PlaybackService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "playback_channel"
    private val NOTIF_ID = 1

    // TODO Implement BG service to fetch soundtracks from local storage
    // soundtracks are hardcoded until BG service implemented

    private val playlist = listOf(
        R.raw.all_my_life,
        R.raw.music_of_pain,
        R.raw.sing_lucifer,
        R.raw.the_night_before_fight
    )
    private var currentTrackIndex = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            "ACTION_PLAY" -> {
                if (!::mediaPlayer.isInitialized) {
                    initMediaPlayer()
                }
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            }
            "ACTION_PAUSE" -> {
                if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
            "ACTION_STOP" -> {
                stopSelf()
            }
            "ACTION_NEXT" -> {
                nextTrack()
            }
            "ACTION_PREVIOUS" -> {
                previousTrack()
            }
        }

        startForeground(NOTIF_ID, createNotification())
        return START_STICKY
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, playlist[currentTrackIndex])
        mediaPlayer.setOnCompletionListener {
            nextTrack()
        }
    }

    private fun nextTrack() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        initMediaPlayer()
        mediaPlayer.start()
    }

    private fun previousTrack() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex =
            if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
        initMediaPlayer()
        mediaPlayer.start()
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
        fun pending(action: String, requestCode: Int) = PendingIntent.getService(
            this, requestCode,
            Intent(this, PlaybackService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText("Track ${currentTrackIndex + 1}")
            .setSmallIcon(R.drawable.music_note)
            .addAction(R.drawable.prev, "Prev", pending("ACTION_PREVIOUS", 4))
            .addAction(R.drawable.play, "Play", pending("ACTION_PLAY", 0))
            .addAction(R.drawable.pause, "Pause", pending("ACTION_PAUSE", 1))
            .addAction(R.drawable.next, "Next", pending("ACTION_NEXT", 3))
            .addAction(R.drawable.stop, "Stop", pending("ACTION_STOP", 2))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

