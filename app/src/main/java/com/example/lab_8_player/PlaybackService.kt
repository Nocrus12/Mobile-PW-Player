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
import androidx.media.app.NotificationCompat.MediaStyle

class PlaybackService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "playback_channel"
    private val NOTIF_ID = 1

    // TODO Implement BG service to fetch soundtracks from local storage
    // soundtracks are hardcoded until BG service implemented


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
        if (MusicLibrary.tracks.isEmpty()) return

        val uri = MusicLibrary.tracks[currentTrackIndex]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlaybackService, uri)
            prepare()
            setOnCompletionListener {
                nextTrack()
            }
        }
    }

    private fun nextTrack() {
        if (MusicLibrary.tracks.isEmpty()) return

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex = (currentTrackIndex + 1) % MusicLibrary.tracks.size
        initMediaPlayer()
        mediaPlayer.start()
    }

    private fun previousTrack() {
        if (MusicLibrary.tracks.isEmpty()) return

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex =
            if (currentTrackIndex - 1 < 0) MusicLibrary.tracks.size - 1 else currentTrackIndex - 1
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
        val playPauseIcon = if (mediaPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseAction = if (mediaPlayer.isPlaying) "Pause" else "Play"

        val playPauseIntent = if (mediaPlayer.isPlaying) {
            Intent(this, PlaybackService::class.java).apply { action = "ACTION_PAUSE" }
        } else {
            Intent(this, PlaybackService::class.java).apply { action = "ACTION_PLAY" }
        }

        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = "ACTION_PREVIOUS" }
        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = "ACTION_NEXT" }

        val playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextPendingIntent = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        // TODO Fix: prev/next actions performs after double click only

        // TODO Enhance view:
        //  add progress bar
        //  trim fetched path down to title only
        //  add fetch track author from metadata

        // TODO Add: folder picker button as feature: change folder (after UI implemented)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                MusicLibrary.tracks.getOrNull(currentTrackIndex)?.lastPathSegment ?: "Track ${currentTrackIndex + 1}"
            )
            .setContentText("Pavel Plamenev")
            .setSmallIcon(R.drawable.music_note)
            .addAction(R.drawable.ic_prev, "Prev", prevPendingIntent)
            .addAction(playPauseIcon, playPauseAction, playPausePendingIntent)
            .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setStyle(MediaStyle())
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
