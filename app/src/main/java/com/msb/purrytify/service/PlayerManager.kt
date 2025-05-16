package com.msb.purrytify.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.msb.purrytify.data.local.entity.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var audioService: AudioService? = null
    private var bound = false
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private val _isLargePlayerVisible = MutableStateFlow(false)
    val isLargePlayerVisible: StateFlow<Boolean> = _isLargePlayerVisible.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.getService()
            bound = true
            
            audioService?.currentSong?.value?.let { song ->
                _currentSong.value = song
                _isPlaying.value = audioService?.isPlaying?.value ?: false
                _duration.value = audioService?.getDuration()?.toFloat() ?: 0f
                _isMiniPlayerVisible.value = true
            }
            
            _isShuffle.value = audioService?.isShuffleEnabled() ?: false
            _repeatMode.value = audioService?.getRepeatMode() ?: 0
            
            monitorServiceState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            bound = false
        }
    }

    init {
        connectToService()
    }

    private fun monitorServiceState() {
        audioService?.let { service ->
            serviceScope.launch {
                service.currentSong.collectLatest { song ->
                    if (song == null && _currentSong.value != null) {
                        handlePlaylistEnded()
                    }
                    _currentSong.value = song
                }
            }
            
            serviceScope.launch {
                service.isPlaying.collectLatest { isPlaying ->
                    _isPlaying.value = isPlaying
                }
            }
        }
    }

    private fun handlePlaylistEnded() {
        _currentSong.value = null
        _isPlaying.value = false
        _isMiniPlayerVisible.value = false
        _isLargePlayerVisible.value = false
        _currentPosition.value = 0f
        _duration.value = 0f
    }

    private fun connectToService() {
        Intent(context, AudioService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun ensureServiceStarted() {
        if (audioService == null) {
            Intent(context, AudioService::class.java).also { intent ->
                context.startService(intent)
                
                if (!bound) {
                    context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }

    fun playSong(song: Song) {
        try {
            Log.d("PlayerManager", "Playing song: ${song.title}, artwork: ${song.artworkPath}")
            ensureServiceStarted()
            audioService?.play(song)
            _currentSong.value = song
            _isPlaying.value = true
            _isMiniPlayerVisible.value = true
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error playing song: ${e.message}", e)
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        try {
            Log.d("PlayerManager", "Setting playlist of ${songs.size} songs, starting at $startIndex")
            val startSong = if (startIndex >= 0 && startIndex < songs.size) songs[startIndex] else null
            startSong?.let { 
                Log.d("PlayerManager", "Start song: ${it.title}, artwork: ${it.artworkPath}")
            }
            
            ensureServiceStarted()
            Log.d("PlayerManager", "Starting service to set playlist 1111111")
            audioService?.setPlaylist(songs, startIndex)
            
            if (startIndex >= 0 && startIndex < songs.size) {
                Log.d("PlayerManager", "Setting current song to: ${songs[startIndex].title}")
                _currentSong.value = songs[startIndex]
                Log.d("PlayerManager", "Set isPlaying to true")
                _isPlaying.value = true
                Log.d("PlayerManager", "Set isMiniPlayerVisible to true")
                _isMiniPlayerVisible.value = true
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error setting playlist: ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        ensureServiceStarted()
        audioService?.togglePlayPause()
        _isPlaying.value = !_isPlaying.value
    }

    fun playNext() {
        ensureServiceStarted()
        audioService?.playNext()
    }

    fun playPrevious() {
        ensureServiceStarted()
        audioService?.playPrevious()
    }

    fun seekTo(position: Float) {
        ensureServiceStarted()
        audioService?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    fun toggleShuffle() {
        ensureServiceStarted()
        audioService?.shuffle()
        _isShuffle.value = audioService?.isShuffleEnabled() ?: _isShuffle.value
    }

    fun toggleRepeat() {
        ensureServiceStarted()
        when (_repeatMode.value) {
            0 -> {
                audioService?.repeatAll()
                _repeatMode.value = 1
            }
            1 -> {
                audioService?.repeatOne()
                _repeatMode.value = 2
            }
            else -> {
                audioService?.noRepeat()
                _repeatMode.value = 0
            }
        }
    }

    fun updatePosition() {
        if (_isPlaying.value) {
            audioService?.let {
                _currentPosition.value = it.getCurrentPosition().toFloat()
            }
        }
    }

    fun stopPlayback() {
        ensureServiceStarted()
        audioService?.stopPlayback()
        _currentSong.value = null
        _isPlaying.value = false
        _isMiniPlayerVisible.value = false
    }

    fun setMiniPlayerVisible(isVisible: Boolean) {
        _isMiniPlayerVisible.value = isVisible
    }

    fun setLargePlayerVisible(isVisible: Boolean) {
        _isLargePlayerVisible.value = isVisible
        if (!isVisible) {
            _isMiniPlayerVisible.value = _currentSong.value != null
        } else {
            _isMiniPlayerVisible.value = false
        }
    }

    fun resumePlayback() {
        if (!_isPlaying.value) {
            ensureServiceStarted()
            audioService?.resumePlayback()
            _isPlaying.value = true
        }
    }

    fun getCurrentPosition(): Float {
        return audioService?.getCurrentPosition()?.toFloat() ?: _currentPosition.value
    }

    fun getDuration(): Float {
        return audioService?.getDuration()?.toFloat() ?: _duration.value
    }

    fun release() {
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
    }
} 