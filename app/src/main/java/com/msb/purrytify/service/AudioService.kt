package com.msb.purrytify.service

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
import android.graphics.drawable.BitmapDrawable
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
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.BitmapImage
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
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class AudioService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "PurrytifyMusicChannel"

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

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Add flag to track if MediaPlayer is prepared
    private var mediaPlayerPrepared = false

    private var playlist: List<Song> = emptyList()
    private var currentSongIndex: Int = -1

    private var repeatMode = 0 // 0: none, 1: all, 2: one
    private var shuffleMode = false
    private var originalPlaylist: List<Song> = emptyList()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

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

    inner class AudioServiceBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AudioService", "Creating audio service")

        createNotificationChannel()

        initMediaSession()

        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_TOGGLE_PLAY)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_STOP)
        }

        registerReceiver(mediaControlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
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
            playlist = playlist + song
            currentSongIndex = playlist.size - 1
            _currentSong.value = song
            prepareAndPlay(song)
        }
    }

    private fun prepareAndPlay(song: Song) {
        try {
            releaseMediaPlayer()

            mediaPlayer = MediaPlayer()
            // Reset prepared flag when creating a new MediaPlayer
            mediaPlayerPrepared = false
            
            mediaPlayer?.setOnErrorListener { mp, what, extra ->
                Log.e("AudioService", "MediaPlayer error: $what, $extra")
                releaseMediaPlayer()
                _isPlaying.value = false
                updatePlaybackState()
                true
            }
            
            try {
                if (song.filePath.startsWith("content:")) {
                    mediaPlayer?.setDataSource(applicationContext, Uri.parse(song.filePath))
                } else {
                    mediaPlayer?.setDataSource(song.filePath)
                }
            } catch (e: Exception) {
                Log.e("AudioService", "Error setting data source: ${e.message}")
                releaseMediaPlayer()
                return
            }
            
            mediaPlayer?.setOnPreparedListener {
                try {
                    // Set prepared flag to true when MediaPlayer is prepared
                    mediaPlayerPrepared = true
                    it.start()
                    _isPlaying.value = true
                    updatePlaybackState()
                    startPlaybackTracking()
                    showNotification()
                } catch (e: Exception) {
                    Log.e("AudioService", "Error starting playback: ${e.message}")
                }
            }
            
            mediaPlayer?.setOnCompletionListener {
                playNext()
            }
            
            try {
                mediaPlayer?.prepareAsync()
            } catch (e: Exception) {
                Log.e("AudioService", "Error preparing media player: ${e.message}")
                releaseMediaPlayer()
            }
            
            updateMediaMetadata(song)
        } catch (e: Exception) {
            Log.e("AudioService", "Error playing song: ${e.message}")
            releaseMediaPlayer()
        }
    }

    private fun updateMediaMetadata(song: Song) {
        val metadataBuilder = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            
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
            if (artworkPath.isEmpty()) {
                return null
            }
            
            if (artworkPath.startsWith("http")) {
                var bitmap: Bitmap? = null
                runBlocking {
                    val loader = ImageLoader(applicationContext)
                    val request = ImageRequest.Builder(applicationContext)
                        .data(artworkPath)
                        .allowHardware(false)
                        .build()

                    try {
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            bitmap = (result.image as? BitmapImage)?.bitmap
                        }
                    } catch (e: Exception) {
                        Log.e("AudioService", "Error loading artwork from URL: ${e.message}", e)
                    }
                }
                return bitmap
            } else if (artworkPath.startsWith("content:")) {
                val inputStream = contentResolver.openInputStream(Uri.parse(artworkPath))
                BitmapFactory.decodeStream(inputStream)
            } else {
                BitmapFactory.decodeFile(artworkPath)
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error loading artwork: ${e.message}", e)
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
                if (_isPlaying.value && mediaPlayer != null && mediaPlayerPrepared) {
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
        Log.d("AudioService", "Showing notification for song: ${song.title}, artwork path: ${song.artworkPath}")
        
        val contentIntent = PendingIntent.getActivity(
            this,
            REQ_CONTENT,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
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
        
        var artwork: Bitmap? = null
        try {
            if (song.artworkPath.isNotEmpty()) {
                artwork = loadArtwork(song.artworkPath)
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error loading artwork: ${e.message}", e)
        }
        
        if (artwork == null) {
            Log.d("AudioService", "Using default artwork for notification")
            artwork = BitmapFactory.decodeResource(resources, R.drawable.image)
        }
        
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2) // Display prev, play/pause, next in compact view
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setLargeIcon(artwork)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .setColorized(true)
            .setColor(0xFF121212.toInt()) // Match the app's dark theme
            .addAction(R.drawable.ic_prev, "Previous", prevIntent)
            .addAction(
                if (_isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play,
                if (_isPlaying.value) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.ic_next, "Next", nextIntent)
            .addAction(R.drawable.ic_close, "Close", stopIntent)
            .setOngoing(_isPlaying.value)
            .build()
        
        if (_isPlaying.value) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
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
        
        if (repeatMode == 2) {
            _currentSong.value?.let { prepareAndPlay(it) }
            return
        }
        
        val nextIndex = if (currentSongIndex < playlist.size - 1) {
            currentSongIndex + 1
        } else {
            if (repeatMode == 1) {
                0
            } else {
                releaseMediaPlayer()
                _currentSong.value = null
                _isPlaying.value = false
                updatePlaybackState()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                
                return
            }
        }
        
        currentSongIndex = nextIndex
        _currentSong.value = playlist[nextIndex]
        prepareAndPlay(playlist[nextIndex])
    }

    fun playPrevious() {
        if (playlist.isEmpty() || currentSongIndex == -1) return
        
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            mediaPlayer?.seekTo(0)
            return
        }
        
        if (repeatMode == 2) {
            _currentSong.value?.let { prepareAndPlay(it) }
            return
        }
        
        val prevIndex = if (currentSongIndex > 0) {
            currentSongIndex - 1
        } else {
            if (repeatMode == 1) {
                playlist.size - 1
            } else {
                0
            }
        }
        
        currentSongIndex = prevIndex
        _currentSong.value = playlist[prevIndex]
        prepareAndPlay(playlist[prevIndex])
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        updatePlaybackState()
    }

    fun stopPlayback() {
        mediaPlayer?.stop()
        _isPlaying.value = false
        updatePlaybackState()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        stopSelf()
    }

    fun getCurrentPosition(): Int {
        return try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.currentPosition ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error getting current position: ${e.message}")
            0
        }
    }

    fun getDuration(): Int {
        return try {
            if (mediaPlayer != null && mediaPlayerPrepared) {
                // Only call duration on the MediaPlayer if it's prepared
                mediaPlayer?.duration ?: _currentSong.value?.duration?.toInt() ?: 0
            } else {
                // Fall back to the song's metadata duration if MediaPlayer isn't ready
                _currentSong.value?.duration?.toInt() ?: 0
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error getting duration: ${e.message}")
            _currentSong.value?.duration?.toInt() ?: 0
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayerPrepared = false
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error releasing media player: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    private fun releaseResources() {
        releaseMediaPlayer()
        mediaSession.release()
    }

    fun isCurrentlyPlaying(): Boolean {
        return _isPlaying.value && mediaPlayer != null
    }

    fun shuffle() {
        if (playlist.isEmpty()) return
        
        if (!shuffleMode) {
            shuffleMode = true
            originalPlaylist = playlist.toList()
            
            val currentSong = if (currentSongIndex >= 0) playlist[currentSongIndex] else null
            val remainingSongs = playlist.toMutableList()
            if (currentSong != null) {
                remainingSongs.remove(currentSong)
            }
            
            remainingSongs.shuffle()
            
            if (currentSong != null) {
                playlist = if (currentSongIndex >= 0) {
                    remainingSongs.toMutableList().apply {
                        add(currentSongIndex, currentSong)
                    }
                } else {
                    remainingSongs
                }
            } else {
                playlist = remainingSongs
            }
        } else {
            shuffleMode = false
            
            val currentSong = if (currentSongIndex >= 0) playlist[currentSongIndex] else null
            playlist = originalPlaylist
            
            if (currentSong != null) {
                currentSongIndex = playlist.indexOfFirst { it.id == currentSong.id }
            }
        }
    }
    
    fun setRepeatMode(mode: Int) {
        repeatMode = mode
    }
    
    fun noRepeat() {
        repeatMode = 0
    }
    
    fun repeatAll() {
        repeatMode = 1
    }
    
    fun repeatOne() {
        repeatMode = 2
    }
    
    fun getRepeatMode(): Int {
        return repeatMode
    }
    
    fun isShuffleEnabled(): Boolean {
        return shuffleMode
    }
}