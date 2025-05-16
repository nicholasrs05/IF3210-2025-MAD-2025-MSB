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

    // Player UI state
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

    // UI visibility states
    private val _isMiniPlayerVisible = mutableStateOf(false)
    val isMiniPlayerVisible: State<Boolean> = _isMiniPlayerVisible
    private val _isLargePlayerVisible = mutableStateOf(false)
    val isLargePlayerVisible: State<Boolean> = _isLargePlayerVisible
    val userId = profileModel.currentProfile.value.id

    // Audio Service
    private var audioService: AudioService? = null
    private var bound = false

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.getService()
            bound = true
            
            // Start collecting flows from the service
            collectServiceFlows()
            
            // Initialize current song if already playing
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
        // Connect to AudioService
        connectToService()

        // Collect state from PlayerManager
        viewModelScope.launch {
            playerManager.currentSong.collectLatest { song ->
                song?.let {
                    _currentSong.value = it
                    _duration.floatValue = playerManager.getDuration()
                    checkLikedStatus(it.id)
                } ?: run {
                    // Song became null (end of playlist)
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

    /**
     * Sets visibility for the large player screen
     */
    fun setLargePlayerVisible(isVisible: Boolean) {
        playerManager.setLargePlayerVisible(isVisible)
    }

    /**
     * Plays a specific song and updates UI state accordingly
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            songRepository.updateLastPlayedAt(song.id)
            checkLikedStatus(song.id)
            playerManager.playSong(song)
        }
    }

    /**
     * Sets playlist for playback
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playerManager.setPlaylist(songs, startIndex)
    }

    /**
     * Toggles play/pause state of the current song
     */
    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    /**
     * Skips to the next song in the playlist
     */
    fun skipToNext() {
        playerManager.playNext()
    }

    /**
     * Skips to the previous song in the playlist
     */
    fun skipToPrevious() {
        playerManager.playPrevious()
    }

    /**
     * Seeks to a specific position in the current song
     */
    fun seekTo(position: Float) {
        playerManager.seekTo(position)
    }

    /**
     * Toggles like status for the current song
     */
    fun toggleLike() {
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                val newLikeStatus = !_isLiked.value
                songRepository.updateLikeStatus(song.id, newLikeStatus)
                _isLiked.value = newLikeStatus
            }
        }
    }

    /**
     * Toggles shuffle mode for the playlist
     */
    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    /**
     * Cycles through repeat modes (NONE -> ALL -> ONE -> NONE)
     */
    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    /**
     * Updates the current playback position
     */
    fun updatePosition() {
        playerManager.updatePosition()
    }

    /**
     * Updates the current song information from the repository
     */
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

    /**
     * Checks and updates the liked status of a song
     */
    private fun checkLikedStatus(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            _isLiked.value = song?.isLiked == true
        }
    }

    /**
     * Resets the player state completely
     */
    fun resetCurrentSong() {
        _currentSong.value = null
        _isPlaying.value = false
        _duration.floatValue = 0f
        _currentPosition.floatValue = 0f
        _isLiked.value = false
    }

    /**
     * Sets the current song with a specified song
     */
    fun setCurrentSong(song: Song) {
        _currentSong.value = song
        checkLikedStatus(song.id)
    }

    /**
     * Resumes playback of the current song
     */
    fun resumeCurrentSong() {
        playerManager.resumePlayback()
    }

    /**
     * Stops the media player and resets its state
     */
    fun stopMediaPlayer() {
        playerManager.stopPlayback()
    }

    /**
     * Share the current song via QR code
     */
    fun shareCurrentSongViaQR() {
        currentSong.value?.let { song ->
            qrSharingService.shareSongViaQR(song)
        }
    }

    /**
     * Sets the visibility of the mini player
     */
    fun setMiniPlayerVisible(isVisible: Boolean) {
        playerManager.setMiniPlayerVisible(isVisible)
    }

    /**
     * Adds a new song to the repository
     */
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

    /**
     * Gets metadata for a song file
     */
    fun getSongMetadata(filePath: String): Pair<String?, String?> {
        return SongRepository.extractMetadata(filePath)
    }

    /**
     * Gets the duration of a song file
     */
    fun getSongDuration(filePath: String): Long {
        return SongRepository.getDuration(filePath)
    }

    /**
     * Play a song by its ID (for deep links)
     * @param songIdStr The song ID as a string from the deep link
     */
    fun playSongById(songIdStr: String) {
        viewModelScope.launch {
            try {
                // First try to find a local song with this ID
                val songId = songIdStr.toLongOrNull()
                if (songId != null) {
                    val song = songRepository.getSongById(songId)
                    if (song != null) {
                        playSong(song)
                        setLargePlayerVisible(true)
                        return@launch
                    }
                }
                
                // If not found locally, try to fetch from API
                val apiSong = apiSongRepository.fetchSongById(songIdStr)
                if (apiSong != null) {
                    // Convert the API song to a local Song entity for playback
                    val localSong = apiSongRepository.convertApiSongToLocalSong(apiSong, userId)
                    
                    // Save the song to the database to make it available in the library
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

    /**
     * Updates an existing song in the database
     */
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
                    
                    // Update current song if it's the one being edited
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
        
        // Unbind from the service
        if (bound) {
            getApplication<Application>().unbindService(serviceConnection)
            bound = false
        }
    }
}