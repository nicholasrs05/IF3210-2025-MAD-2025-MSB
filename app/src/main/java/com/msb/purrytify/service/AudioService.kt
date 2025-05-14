package com.msb.purrytify.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.msb.purrytify.MainActivity
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.receiver.MediaControlReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "PurrytifyMusicChannel"

        // Action constants
        const val ACTION_PLAY = "com.msb.purrytify.action.PLAY"
        const val ACTION_PAUSE = "com.msb.purrytify.action.PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.msb.purrytify.action.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.msb.purrytify.action.NEXT"
        const val ACTION_PREVIOUS = "com.msb.purrytify.action.PREVIOUS"
        const val ACTION_STOP = "com.msb.purrytify.action.STOP"
        
        // Request codes for pending intents
        private const val REQ_PLAY_PAUSE = 1
        private const val REQ_NEXT = 2
        private const val REQ_PREV = 3
        private const val REQ_STOP = 4
        private const val REQ_CONTENT = 5
    }

    // Media player components
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Playlist management
    private var playlist: List<Song> = emptyList()
    private var currentSongIndex: Int = -1

    // State flows for player state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    // Broadcast receiver for media controls
    private val mediaControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> resumePlayback()
                ACTION_PAUSE -> pausePlayback()
                ACTION_TOGGLE_PLAY -> togglePlayPause()
                ACTION_NEXT -> playNext()
                ACTION_PREVIOUS -> playPrevious()
                ACTION_STOP -> stopPlayback()
            }
        }
    }

    // Binder for service connection
    inner class AudioServiceBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AudioService", "Creating audio service")

        // Create notification channel
        createNotificationChannel()

        // Initialize media session
        initMediaSession()

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_TOGGLE_PLAY)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_STOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaControlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mediaControlReceiver, filter)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return AudioServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleCommandIntent(intent)
        return START_NOT_STICKY
    }

    private fun handleCommandIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_TOGGLE_PLAY -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stopPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
        unregisterReceiver(mediaControlReceiver)
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Purrytify Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
            }
            
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } else {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "PurrytifySession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    playNext()
                }

                override fun onSkipToPrevious() {
                    playPrevious()
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos.toInt())
                }
            })
            isActive = true
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        if (songs.isNotEmpty() && startIndex in songs.indices) {
            currentSongIndex = startIndex
            _currentSong.value = songs[startIndex]
            prepareAndPlay(songs[startIndex])
        }
    }

    fun play(song: Song) {
        val index = playlist.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            currentSongIndex = index
            _currentSong.value = song
            prepareAndPlay(song)
        } else {
            // Add to playlist if not present
            playlist = playlist + song
            currentSongIndex = playlist.size - 1
            _currentSong.value = song
            prepareAndPlay(song)
        }
    }

    private fun prepareAndPlay(song: Song) {
        try {
            // Release any existing media player
            releaseMediaPlayer()

            // Create a new media player
            mediaPlayer = MediaPlayer().apply {
                if (song.filePath.startsWith("content:")) {
                    setDataSource(applicationContext, Uri.parse(song.filePath))
                } else {
                    setDataSource(song.filePath)
                }
                
                setOnPreparedListener {
                    start()
                    _isPlaying.value = true
                    updatePlaybackState()
                    startPlaybackTracking()
                    showNotification()
                }
                
                setOnCompletionListener {
                    playNext()
                }
                
                prepareAsync()
            }
            
            updateMediaMetadata(song)
        } catch (e: Exception) {
            Log.e("AudioService", "Error playing song: ${e.message}")
        }
    }

    private fun updateMediaMetadata(song: Song) {
        val metadataBuilder = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            
            // Try to load artwork
            if (song.artworkPath.isNotEmpty()) {
                try {
                    val artwork = loadArtwork(song.artworkPath)
                    if (artwork != null) {
                        putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                    }
                } catch (e: Exception) {
                    Log.e("AudioService", "Error loading artwork: ${e.message}")
                }
            }
        }
        
        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun loadArtwork(artworkPath: String): Bitmap? {
        return try {
            if (artworkPath.startsWith("content:")) {
                val inputStream = contentResolver.openInputStream(Uri.parse(artworkPath))
                BitmapFactory.decodeStream(inputStream)
            } else {
                BitmapFactory.decodeFile(artworkPath)
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error loading artwork: ${e.message}")
            null
        }
    }

    private fun updatePlaybackState() {
        val currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0
        val state = if (_isPlaying.value) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        
        val stateBuilder = PlaybackStateCompat.Builder().apply {
            setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            setState(state, currentPosition, 1.0f)
        }
        
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun startPlaybackTracking() {
        serviceScope.launch {
            while (true) {
                if (_isPlaying.value && mediaPlayer != null) {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    val duration = mediaPlayer?.duration ?: 1
                    _playbackProgress.value = currentPosition.toFloat() / duration.toFloat()
                    updatePlaybackState()
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun showNotification() {
        val song = _currentSong.value ?: return
        
        // Create content intent that opens the app
        val contentIntent = PendingIntent.getActivity(
            this,
            REQ_CONTENT,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create action intents
        val playPauseIntent = MediaControlReceiver.createPendingIntent(
            this,
            if (_isPlaying.value) ACTION_PAUSE else ACTION_PLAY,
            REQ_PLAY_PAUSE
        )
        
        val nextIntent = MediaControlReceiver.createPendingIntent(
            this,
            ACTION_NEXT,
            REQ_NEXT
        )
        
        val prevIntent = MediaControlReceiver.createPendingIntent(
            this,
            ACTION_PREVIOUS,
            REQ_PREV
        )
        
        val stopIntent = MediaControlReceiver.createPendingIntent(
            this,
            ACTION_STOP,
            REQ_STOP
        )
        
        // Load artwork
        var artwork: Bitmap? = null
        if (song.artworkPath.isNotEmpty()) {
            artwork = loadArtwork(song.artworkPath)
        }
        
        // Create media style
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2) // Display prev, play/pause, next in compact view
        
        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Replace with your app icon
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setLargeIcon(artwork)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .addAction(R.drawable.ic_prev, "Previous", prevIntent) // Replace icon
            .addAction(
                if (_isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play,
                if (_isPlaying.value) "Pause" else "Play",
                playPauseIntent
            ) // Replace icons
            .addAction(R.drawable.ic_next, "Next", nextIntent) // Replace icon
            .addAction(R.drawable.ic_close, "Close", stopIntent) // Replace icon
            .setOngoing(_isPlaying.value)
            .build()
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, notification)
        
        // Update notification if paused
        if (!_isPlaying.value) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        updatePlaybackState()
        showNotification()
    }

    fun resumePlayback() {
        mediaPlayer?.start()
        _isPlaying.value = true
        updatePlaybackState()
        showNotification()
    }

    fun playNext() {
        if (playlist.isEmpty() || currentSongIndex == -1) return
        
        val nextIndex = (currentSongIndex + 1) % playlist.size
        currentSongIndex = nextIndex
        _currentSong.value = playlist[nextIndex]
        prepareAndPlay(playlist[nextIndex])
    }

    fun playPrevious() {
        if (playlist.isEmpty() || currentSongIndex == -1) return
        
        val prevIndex = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
        currentSongIndex = prevIndex
        _currentSong.value = playlist[prevIndex]
        prepareAndPlay(playlist[prevIndex])
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        updatePlaybackState()
    }

    fun stopPlayback() {
        // Stop playback
        mediaPlayer?.stop()
        _isPlaying.value = false
        updatePlaybackState()
        
        // Stop foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        // Stop the service
        stopSelf()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun releaseResources() {
        releaseMediaPlayer()
        mediaSession.release()
    }
}