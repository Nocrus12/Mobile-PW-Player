package com.example.lab_8_player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.lab_8_player.db.model.Song
import androidx.core.net.toUri

class PlaybackService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private var trackList: List<Song> = emptyList()
    private var currentTrackIndex = 0
    private val CHANNEL_ID = "playback_channel"
    private val NOTIF_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
//            "ACTION_START" -> {
//                val songs = intent.getParcelableArrayListExtra("SONG_LIST", Song::class.java) ?: return START_STICKY
//                val newTrackList = songs.toList()
//
//                if (!::mediaPlayer.isInitialized || newTrackList != trackList) {
//                    // Restart playback with new list
//
//                    trackList = newTrackList
//                    currentTrackIndex = 0
//                    initMediaPlayer()
//                }
//            }

//            "ACTION_PLAY" -> {
//                val uri = intent.getStringExtra("SONG_URI")?.toUri()
//                uri?.let {
//                    val index = trackList.indexOfFirst { song -> song.uri.toUri() == it }.takeIf { it >= 0 } ?: 0
//                    playAt(index)
//                } ?: run {
//                    // fallback: resume playback if already initialized
//                    if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
//                        mediaPlayer.start()
//                    }
//                }
//            }

            "ACTION_BEGIN" -> {
                val songs = intent.getParcelableArrayListExtra("EXTRA_SONG_LIST", Song::class.java)
                    ?: return START_STICKY
                val index = intent.getIntExtra("EXTRA_PLAY_INDEX", 0)
                // always restart if list or index differ
                if (songs != trackList || index != currentTrackIndex) {
                    if (::mediaPlayer.isInitialized) { mediaPlayer.stop() }
                    trackList = songs
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
            "ACTION_STOP" -> {
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.stop()
                    stopSelf()
                }
            }
        }

        startForeground(NOTIF_ID, createNotification())
        return START_STICKY
    }

    private fun initMediaPlayer() {
        if (trackList.isEmpty()) return

        val song = trackList[currentTrackIndex]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlaybackService, song.uri.toUri())
            prepare()
            setOnCompletionListener { nextTrack() }
        }
    }

    private fun nextTrack() {
        if (!::mediaPlayer.isInitialized || trackList.isEmpty()) return
        mediaPlayer.stop()
        mediaPlayer.release()
        currentTrackIndex = (currentTrackIndex + 1) % trackList.size
        initMediaPlayer()
        mediaPlayer.start()
    }

    private fun previousTrack() {
        if (!::mediaPlayer.isInitialized || trackList.isEmpty()) return
        mediaPlayer.stop()
        mediaPlayer.release()
        currentTrackIndex =
            if (currentTrackIndex - 1 < 0) trackList.lastIndex else currentTrackIndex - 1
        initMediaPlayer()
        mediaPlayer.start()
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val isPlaying = ::mediaPlayer.isInitialized && mediaPlayer.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseAction = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"

        val playPausePI = PendingIntent.getService(this, 0, Intent(this, PlaybackService::class.java).apply {
            action = playPauseAction
        }, PendingIntent.FLAG_IMMUTABLE)

        val prevPI = PendingIntent.getService(this, 1, Intent(this, PlaybackService::class.java).apply {
            action = "ACTION_PREVIOUS"
        }, PendingIntent.FLAG_IMMUTABLE)

        val nextPI = PendingIntent.getService(this, 2, Intent(this, PlaybackService::class.java).apply {
            action = "ACTION_NEXT"
        }, PendingIntent.FLAG_IMMUTABLE)

        val song = trackList.getOrNull(currentTrackIndex)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.name ?: "Unknown Title")
            .setContentText(song?.artist ?: "Unknown Artist")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun playAt(index: Int) {

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex = index
        initMediaPlayer()
        mediaPlayer.start()
    }
}
