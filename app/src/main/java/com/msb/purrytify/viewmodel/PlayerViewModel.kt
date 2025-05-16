package com.msb.purrytify.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.ApiSongRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.qr.QRSharingService
import com.msb.purrytify.service.AudioService
import com.msb.purrytify.service.PlayerManager
import com.msb.purrytify.service.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    private val apiSongRepository: ApiSongRepository,
    private val playerManager: PlayerManager,
    profileModel: ProfileModel,
    private val qrSharingService: QRSharingService
) : AndroidViewModel(application) {

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _isShuffle = mutableStateOf(false)
    val isShuffle: State<Boolean> = _isShuffle

    private val _repeatMode = mutableStateOf(RepeatMode.NONE)
    val repeatMode: State<RepeatMode> = _repeatMode

    private val _currentPosition = mutableFloatStateOf(0f)
    val currentPosition: State<Float> = _currentPosition

    private val _duration = mutableFloatStateOf(0f)
    val duration: State<Float> = _duration

    private val _isLiked = mutableStateOf(false)
    val isLiked: State<Boolean> = _isLiked

    private val _isMiniPlayerVisible = mutableStateOf(false)
    val isMiniPlayerVisible: State<Boolean> = _isMiniPlayerVisible
    private val _isLargePlayerVisible = mutableStateOf(false)
    val isLargePlayerVisible: State<Boolean> = _isLargePlayerVisible
    val userId = profileModel.currentProfile.value.id

    private var audioService: AudioService? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.getService()
            bound = true
            
            collectServiceFlows()
            
            audioService?.currentSong?.value?.let { song ->
                _currentSong.value = song
                _isPlaying.value = audioService?.isPlaying?.value ?: false
                _duration.floatValue = audioService?.getDuration()?.toFloat() ?: 0f
                checkLikedStatus(song.id)
                _isMiniPlayerVisible.value = true
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            bound = false
        }
    }

    init {
        connectToService()

        viewModelScope.launch {
            playerManager.currentSong.collectLatest { song ->
                song?.let {
                    _currentSong.value = it
                    _duration.floatValue = playerManager.getDuration()
                    checkLikedStatus(it.id)
                } ?: run {
                    _currentSong.value = null
                    _duration.floatValue = 0f
                    _currentPosition.floatValue = 0f
                    _isLargePlayerVisible.value = false
                    _isMiniPlayerVisible.value = false
                }
            }
        }
        
        viewModelScope.launch {
            playerManager.isPlaying.collectLatest { playing ->
                _isPlaying.value = playing
            }
        }
        
        viewModelScope.launch {
            playerManager.currentPosition.collectLatest { position ->
                _currentPosition.floatValue = position
            }
        }
        
        viewModelScope.launch {
            playerManager.duration.collectLatest { duration ->
                _duration.floatValue = duration
            }
        }
        
        viewModelScope.launch {
            playerManager.isMiniPlayerVisible.collectLatest { visible ->
                _isMiniPlayerVisible.value = visible
            }
        }
        
        viewModelScope.launch {
            playerManager.isLargePlayerVisible.collectLatest { visible ->
                _isLargePlayerVisible.value = visible
            }
        }
        
        viewModelScope.launch {
            playerManager.isShuffle.collectLatest { shuffleEnabled ->
                _isShuffle.value = shuffleEnabled
            }
        }
        
        viewModelScope.launch {
            playerManager.repeatMode.collectLatest { mode ->
                _repeatMode.value = when(mode) {
                    0 -> RepeatMode.NONE
                    1 -> RepeatMode.ALL
                    2 -> RepeatMode.ONE
                    else -> RepeatMode.NONE
                }
            }
        }
    }

    private fun connectToService() {
        Intent(getApplication(), AudioService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun collectServiceFlows() {
        audioService?.let { service ->
            viewModelScope.launch {
                service.currentSong.collectLatest { song ->
                    song?.let {
                        _currentSong.value = it
                        _duration.floatValue = service.getDuration().toFloat()
                        checkLikedStatus(it.id)
                        _isMiniPlayerVisible.value = true
                    }
                }
            }
            
            viewModelScope.launch {
                service.isPlaying.collectLatest { playing ->
                    _isPlaying.value = playing
                }
            }
            
            viewModelScope.launch {
                service.playbackProgress.collectLatest { progress ->
                    val duration = service.getDuration()
                    if (duration > 0) {
                        _currentPosition.floatValue = progress * duration
                    }
                }
            }
        }
    }

    private fun ensureServiceStarted() {
        if (audioService == null) {
            Intent(getApplication(), AudioService::class.java).also { intent ->
                getApplication<Application>().startService(intent)
                
                if (!bound) {
                    getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }

    fun setLargePlayerVisible(isVisible: Boolean) {
        playerManager.setLargePlayerVisible(isVisible)
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            songRepository.updateLastPlayedAt(song.id)
            checkLikedStatus(song.id)
            playerManager.playSong(song)
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playerManager.setPlaylist(songs, startIndex)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipToNext() {
        playerManager.playNext()
    }

    fun skipToPrevious() {
        playerManager.playPrevious()
    }

    fun seekTo(position: Float) {
        playerManager.seekTo(position)
    }

    fun toggleLike() {
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                val newLikeStatus = !_isLiked.value
                songRepository.updateLikeStatus(song.id, newLikeStatus)
                _isLiked.value = newLikeStatus
            }
        }
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun updatePosition() {
        playerManager.updatePosition()
    }

    fun updateSongFromRepo() {
        _currentSong.value?.let { song ->
            viewModelScope.launch {
                val updatedSong = songRepository.getSongById(song.id)
                if (updatedSong != null) {
                    _currentSong.value = updatedSong
                }
            }
        }
    }

    private fun checkLikedStatus(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            _isLiked.value = song?.isLiked == true
        }
    }

    fun resetCurrentSong() {
        _currentSong.value = null
        _isPlaying.value = false
        _duration.floatValue = 0f
        _currentPosition.floatValue = 0f
        _isLiked.value = false
    }

    fun setCurrentSong(song: Song) {
        _currentSong.value = song
        checkLikedStatus(song.id)
    }

    fun resumeCurrentSong() {
        playerManager.resumePlayback()
    }

    fun stopMediaPlayer() {
        playerManager.stopPlayback()
    }

    fun shareCurrentSongViaQR() {
        currentSong.value?.let { song ->
            qrSharingService.shareSongViaQR(song)
        }
    }

    fun setMiniPlayerVisible(isVisible: Boolean) {
        playerManager.setMiniPlayerVisible(isVisible)
    }

    fun addSong(title: String, artist: String, filePath: String, artworkPath: String, duration: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val song = Song(
                title = title,
                artist = artist,
                filePath = filePath,
                artworkPath = artworkPath,
                duration = duration,
                ownerId = userId
            )
            songRepository.insert(song)
        }
    }

    fun getSongMetadata(filePath: String): Pair<String?, String?> {
        return SongRepository.extractMetadata(filePath)
    }

    fun getSongDuration(filePath: String): Long {
        return SongRepository.getDuration(filePath)
    }

    fun playSongById(songIdStr: String) {
        viewModelScope.launch {
            try {
                val songId = songIdStr.toLongOrNull()
                if (songId != null) {
                    val song = songRepository.getSongById(songId)
                    if (song != null) {
                        playSong(song)
                        setLargePlayerVisible(true)
                        return@launch
                    }
                }
                
                val apiSong = apiSongRepository.fetchSongById(songIdStr)
                if (apiSong != null) {
                    val localSong = apiSongRepository.convertApiSongToLocalSong(apiSong, userId)
                    
                    val insertedId = songRepository.insert(localSong)
                    val savedSong = songRepository.getSongById(insertedId)
                    
                    if (savedSong != null) {
                        playSong(savedSong)
                        setLargePlayerVisible(true)
                    } else {
                        Log.e("PlayerViewModel", "Failed to save API song")
                    }
                } else {
                    Log.e("PlayerViewModel", "Song not found with ID: $songIdStr")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error playing song by ID: ${e.message}")
            }
        }
    }

    fun updateSong(songId: Long, title: String, artist: String, artworkPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getSongById(songId)?.let { existingSong ->
                    val updatedSong = existingSong.copy(
                        title = title,
                        artist = artist,
                        artworkPath = artworkPath
                    )
                    songRepository.update(updatedSong)
                    
                    if (_currentSong.value?.id == songId) {
                        _currentSong.value = updatedSong
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        
        if (bound) {
            getApplication<Application>().unbindService(serviceConnection)
            bound = false
        }
    }
}