package com.example.lab_8_player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.room.Room
import com.example.lab_8_player.db.AppDatabase
import kotlinx.coroutines.runBlocking
import androidx.core.net.toUri

class PlaybackService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var db: AppDatabase
    private var trackUris: List<Uri> = emptyList()
    private var currentTrackIndex = 0

    private val CHANNEL_ID = "playback_channel"
    private val NOTIF_ID   = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_db"
        ).build()
    }

    override fun onStartCommand(
        intent: Intent?, flags: Int, startId: Int
    ): Int {
        when (intent?.action) {
            "ACTION_START" -> {
                if (!::mediaPlayer.isInitialized) {
                    val songs = runBlocking {
                        db.songDao().getAllUris()
                    }
                    trackUris = songs.map { it.toUri() }

                    if (trackUris.isNotEmpty()) {
                        initMediaPlayer()
                    }
                }
            }
            "ACTION_PLAY" -> {
                val uri = intent.getStringExtra("SONG_URI")?.toUri()
                uri?.let {
                    val index = trackUris.indexOf(it).takeIf { it >= 0 } ?: 0
                    playAt(index)
                }
            }
            "ACTION_PAUSE" -> {
                if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
            "ACTION_NEXT" -> nextTrack()
            "ACTION_PREVIOUS" -> previousTrack()
            "ACTION_STOP" -> stopSelf()
        }

        // 4. Promote to foreground with updated notification
        startForeground(NOTIF_ID, createNotification())
        return START_STICKY
    }

    private fun initMediaPlayer() {
        val uri = trackUris[currentTrackIndex]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlaybackService, uri)
            prepare()
            setOnCompletionListener { nextTrack() }
        }
    }

    private fun nextTrack() {
        if (!::mediaPlayer.isInitialized || trackUris.isEmpty()) return
        mediaPlayer.stop()
        mediaPlayer.release()
        currentTrackIndex = (currentTrackIndex + 1) % trackUris.size
        initMediaPlayer()
        mediaPlayer.start()
    }

    private fun previousTrack() {
        if (!::mediaPlayer.isInitialized || trackUris.isEmpty()) return
        mediaPlayer.stop()
        mediaPlayer.release()
        currentTrackIndex =
            if (currentTrackIndex - 1 < 0) trackUris.lastIndex else currentTrackIndex - 1
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
        // Safely check mediaPlayer before accessing state
        val isPlaying = ::mediaPlayer.isInitialized && mediaPlayer.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseAction = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"

        val playPauseIntent = Intent(this, PlaybackService::class.java).apply {
            action = playPauseAction
        }
        val prevIntent = Intent(this, PlaybackService::class.java).apply {
            action = "ACTION_PREVIOUS"
        }
        val nextIntent = Intent(this, PlaybackService::class.java).apply {
            action = "ACTION_NEXT"
        }

        val playPausePI = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevPI      = PendingIntent.getService(this, 1, prevIntent,      PendingIntent.FLAG_IMMUTABLE)
        val nextPI      = PendingIntent.getService(this, 2, nextIntent,      PendingIntent.FLAG_IMMUTABLE)

        val uri = trackUris.getOrNull(currentTrackIndex)
        val title = uri?.lastPathSegment
            ?.substringAfterLast("/")
            ?.substringBeforeLast(".") ?: "Track ${currentTrackIndex + 1}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.music_note)
            .addAction(R.drawable.ic_prev, "Prev", prevPI)
            .addAction(playPauseIcon, playPauseAction, playPausePI)
            .addAction(R.drawable.ic_next, "Next", nextPI)
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
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun playAt(index: Int) {
        if (trackUris.isEmpty()) return

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex = index
        initMediaPlayer()
        mediaPlayer.start()
    }
}



// TODO Enhance view:
//  add progress bar - depends on song duration and time elapsed, ending means reset of the progress

// TODO Add in-app playback panel