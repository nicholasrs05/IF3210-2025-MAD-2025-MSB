package com.msb.purrytify.viewmodel

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.service.AudioService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    @SuppressLint("StaticFieldLeak")
    private var audioService: AudioService? = null
    private var bound = false
    
    // UI state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()
    
    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.getService()
            bound = true
            
            // Start collecting flows from the service
            collectServiceFlows()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            bound = false
        }
    }
    
    init {
        connectToService()
    }
    
    private fun connectToService() {
        Intent(context, AudioService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    private fun collectServiceFlows() {
        audioService?.let { service ->
            viewModelScope.launch {
                service.currentSong.collect { song ->
                    _currentSong.value = song
                }
            }
            
            viewModelScope.launch {
                service.isPlaying.collect { playing ->
                    _isPlaying.value = playing
                }
            }
            
            viewModelScope.launch {
                service.playbackProgress.collect { progress ->
                    _playbackProgress.value = progress
                }
            }
        }
    }
    
    // User actions
    fun playSong(song: Song) {
        ensureServiceStarted()
        audioService?.play(song)
    }
    
    fun togglePlayPause() {
        audioService?.togglePlayPause()
    }
    
    fun playNext() {
        audioService?.playNext()
    }
    
    fun playPrevious() {
        audioService?.playPrevious()
    }
    
    fun seekTo(position: Int) {
        audioService?.seekTo(position)
    }
    
    fun stopPlayback() {
        audioService?.stopPlayback()
    }
    
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        ensureServiceStarted()
        audioService?.setPlaylist(songs, startIndex)
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
    
    override fun onCleared() {
        super.onCleared()
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
    }
}