package com.example.lab_8_player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.lab_8_player.db.model.Song
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle

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

            "ACTION_BEGIN"  -> handleBegin(intent)
            "ACTION_PAUSE"  -> pauseAndBroadcast()
            "ACTION_RESUME" -> resumeAndBroadcast()
            "ACTION_NEXT"   -> nextTrack()
            "ACTION_PREV"   -> previousTrack()
            "ACTION_STOP"   -> stop()
            "ACTION_SEEK" -> {
                val to = intent.getIntExtra("EXTRA_SEEK_TO", 0)
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.seekTo(to)
                    broadcastState()
                    updateHandler.postDelayed(updateRunnable, 500)
                }
            }

        }

        startForeground(NOTIF_ID, createNotification())
        return START_STICKY
    }

    private fun handleBegin(intent: Intent) {
        val songs = intent.getParcelableArrayListExtra("EXTRA_SONG_LIST", Song::class.java)
            ?: return
        val index = intent.getIntExtra("EXTRA_PLAY_INDEX", 0)
        // always restart if list or index differ
        if (songs != trackList || index != currentTrackIndex) {
            if (::mediaPlayer.isInitialized) { mediaPlayer.stop() }
            trackList = songs
            playAt(index)
        }
        broadcastState()

        val prefs = getSharedPreferences("playback_prefs", MODE_PRIVATE)
        val uris = songs.joinToString("|") { it.uri }  // pipe-separated list of URIs
        prefs.edit()
            .putString("last_playlist_uris", uris)
            .putInt("last_index", index)
            .apply()
    }

    private fun stop() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            stopSelf()
            broadcastState()
            updateHandler.removeCallbacks(updateRunnable)
        }
    }

    private fun resumeAndBroadcast() {
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) mediaPlayer.start()
        broadcastState()

        updateHandler.removeCallbacks(updateRunnable) // just in case…
        updateHandler.postDelayed(updateRunnable, 500)
    }

    private fun pauseAndBroadcast() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
        broadcastState()
        updateHandler.removeCallbacks(updateRunnable)
    }

    private fun initMediaPlayer() {
        if (trackList.isEmpty()) return

        val song = trackList[currentTrackIndex]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlaybackService, song.uri.toUri())
            prepare()
            setOnCompletionListener { nextTrack() }
        }
        broadcastState()
    }

    private fun nextTrack() {
        if (!::mediaPlayer.isInitialized || trackList.isEmpty()) return
        mediaPlayer.stop()
        mediaPlayer.release()
        currentTrackIndex = (currentTrackIndex + 1) % trackList.size
        initMediaPlayer()
        mediaPlayer.start()
        broadcastState()
    }

    private fun previousTrack() {
        if (!::mediaPlayer.isInitialized || trackList.isEmpty()) return
        mediaPlayer.stop()
        mediaPlayer.release()
        currentTrackIndex =
            if (currentTrackIndex - 1 < 0) trackList.lastIndex else currentTrackIndex - 1
        initMediaPlayer()
        mediaPlayer.start()
        broadcastState()
    }

    private fun playAt(index: Int) {

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        currentTrackIndex = index
        initMediaPlayer()
        mediaPlayer.start()
        broadcastState()
        updateHandler.postDelayed(updateRunnable, 500)
    }

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                broadcastState()

                val notification = createNotification()
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIF_ID, notification)
                updateHandler.postDelayed(this, 500)
            }
        }
    }

    private fun broadcastState() {
        val song = trackList.getOrNull(currentTrackIndex)
        val pos = mediaPlayer.currentPosition

        val intent = Intent("PLAYBACK_STATE_CHANGED").apply {

            putExtra("isPlaying", mediaPlayer.isPlaying)
            putExtra("position", pos)
            putExtra("songId",  song!!.id)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val isPlaying = ::mediaPlayer.isInitialized && mediaPlayer.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
        val playPauseAction = if (isPlaying) "ACTION_PAUSE" else "ACTION_RESUME"

        val progressPercent = if (::mediaPlayer.isInitialized && mediaPlayer.duration > 0)
            (mediaPlayer.currentPosition * 100 / mediaPlayer.duration)
        else 0


        val playPausePI = PendingIntent.getService(this, 0, Intent(this, PlaybackService::class.java).apply {
            action = playPauseAction
        }, PendingIntent.FLAG_IMMUTABLE)

        val prevPI = PendingIntent.getService(this, 1, Intent(this, PlaybackService::class.java).apply {
            action = "ACTION_PREV"
        }, PendingIntent.FLAG_IMMUTABLE)

        val nextPI = PendingIntent.getService(this, 2, Intent(this, PlaybackService::class.java).apply {
            action = "ACTION_NEXT"
        }, PendingIntent.FLAG_IMMUTABLE)

        val song = trackList.getOrNull(currentTrackIndex)

        // Create custom small and big views
        val smallView = RemoteViews(packageName, R.layout.notif_playback_small).apply {
            setTextViewText(R.id.title, song?.name ?: "Unknown Title")
            setTextViewText(R.id.artist, song?.artist ?: "Unknown Artist")
//            setImageViewResource(R.id.play_pause, playPauseIcon)
//            setOnClickPendingIntent(R.id.play_pause, playPausePI)
//            setOnClickPendingIntent(R.id.prev, prevPI)
//            setOnClickPendingIntent(R.id.next, nextPI)
        }

        val bigView = RemoteViews(packageName, R.layout.notif_playback_big).apply {
            setTextViewText(R.id.title, song?.name ?: "Unknown Title")
            setTextViewText(R.id.artist, song?.artist ?: "Unknown Artist")
            setImageViewResource(R.id.btnPlayPause, playPauseIcon)
            setOnClickPendingIntent(R.id.btnPlayPause, playPausePI)
            setOnClickPendingIntent(R.id.btnPrev, prevPI)
            setOnClickPendingIntent(R.id.btnNext, nextPI)
            setProgressBar(R.id.progressBar, 100, progressPercent, false)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setCustomContentView(smallView)
            .setCustomBigContentView(bigView)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setStyle(DecoratedMediaCustomViewStyle())
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




}
