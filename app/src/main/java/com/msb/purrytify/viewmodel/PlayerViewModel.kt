package com.msb.purrytify.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.ApiSongRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.qr.QRSharingService
import com.msb.purrytify.service.MusicNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    private val apiSongRepository: ApiSongRepository,
    val mediaPlayerManager: MediaPlayerManager,
    profileModel: ProfileModel,
    private val notificationService: MusicNotificationService,
    private val qrSharingService: QRSharingService,
    private val audioViewModel: AudioViewModel // Inject our new AudioViewModel
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
    val userId = profileModel.currentProfile.value.id

    init {
        // Setup current song state (from PlayerViewModel)
        mediaPlayerManager.getCurrentSong()?.let { song ->
            _currentSong.value = song
            _isPlaying.value = mediaPlayerManager.isPlaying()
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()
            checkLikedStatus(song.id)
            updateNotification(song)
        }

        // Set up completion handler
        mediaPlayerManager.onCompletion = {
            handleSongCompletion()
        }

        mediaPlayerManager.addSongChangeListener(object : MediaPlayerManager.SongChangeListener {
            override fun onSongChanged(newSong: Song) {
                updateCurrentSong()
                updateNotification(newSong)
            }

            override fun onPlayerReleased() {
                resetCurrentSong()
                hideNotification()
            }
        })
    }

    /**
     * Loads the playlist for the current user
     */
//    private fun loadPlaylist() {
//        viewModelScope.launch {
//            val userId = profileModel.currentProfile.value.id
//            songRepository.fetchAllSongs(userId).collect { songs ->
//                Log.d("PlayerViewModel", "Loaded playlist with ${songs.size} songs")
//                mediaPlayerManager.setPlaylist(songs)
//            }
//        }
//    }

    /**
     * Sets visibility for the large player screen
     */
    fun setLargePlayerVisible(isVisible: Boolean) {
        _isLargePlayerVisible.value = isVisible
        if (!isVisible) {
            _isMiniPlayerVisible.value = _currentSong.value != null
        } else {
            _isMiniPlayerVisible.value = false
        }
    }

    /**
     * Plays a specific song and updates UI state accordingly
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            _currentSong.value = song
            _isPlaying.value = true

            songRepository.updateLastPlayedAt(song.id)
            checkLikedStatus(song.id)

            // Use both the legacy MediaPlayerManager and the new AudioViewModel
            mediaPlayerManager.play(song)
            audioViewModel.playSong(song) // This will start the foreground service with notification
            
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = 0f
            _isMiniPlayerVisible.value = true
            
            updateNotification(song)
        }
    }

    /**
     * Toggles play/pause state of the current song
     */
    fun togglePlayPause() {
        if (_isPlaying.value) {
            mediaPlayerManager.pause()
            audioViewModel.togglePlayPause() // Sync with AudioService
            _isPlaying.value = false
            _currentSong.value?.let { updateNotification(it) }
        } else {
            if (_currentSong.value != null) {
                mediaPlayerManager.resume()
                audioViewModel.togglePlayPause() // Sync with AudioService
                _isPlaying.value = true
                _currentSong.value?.let { updateNotification(it) }
            } else {
                mediaPlayerManager.getCurrentSong()?.let { song ->
                    _currentSong.value = song
                    mediaPlayerManager.resume()
                    audioViewModel.togglePlayPause() // Sync with AudioService
                    _isPlaying.value = true
                    updateNotification(song)
                }
            }
        }
    }

    /**
     * Skips to the next song in the playlist
     */
    fun skipToNext() {
        mediaPlayerManager.playNext()
        audioViewModel.playNext() // Sync with AudioService
        updateCurrentSong()
    }

    /**
     * Skips to the previous song in the playlist
     */
    fun skipToPrevious() {
        mediaPlayerManager.playPrevious()
        audioViewModel.playPrevious() // Sync with AudioService
        updateCurrentSong()
    }

    /**
     * Seeks to a specific position in the current song
     */
    fun seekTo(position: Float) {
        mediaPlayerManager.seekTo(position.toInt())
        audioViewModel.seekTo(position.toInt()) // Sync with AudioService
        _currentPosition.floatValue = position
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
        _isShuffle.value = !_isShuffle.value
        mediaPlayerManager.shuffle()
    }

    /**
     * Cycles through repeat modes (NONE -> ALL -> ONE -> NONE)
     */
    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        when (_repeatMode.value) {
            RepeatMode.NONE -> mediaPlayerManager.noRepeat()
            RepeatMode.ALL -> mediaPlayerManager.repeatAll()
            RepeatMode.ONE -> mediaPlayerManager.repeatOne()
        }
    }

    /**
     * Updates the current playback position
     */
    fun updatePosition() {
        if (_isPlaying.value) {
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()
        }
    }

    /**
     * Updates the current song information from the media player
     */
    fun updateCurrentSong() {
        val currentSong = mediaPlayerManager.getCurrentSong()
        if (currentSong != null) {
            _currentSong.value = currentSong
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()
            _isPlaying.value = mediaPlayerManager.isPlaying()
            checkLikedStatus(currentSong.id)

            viewModelScope.launch {
                songRepository.updateLastPlayedAt(currentSong.id)
            }
        }
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
     * Handles song completion based on the current repeat mode
     */
    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.NONE -> skipToNext()
            RepeatMode.ALL -> skipToNext()
            RepeatMode.ONE -> updateCurrentSong()
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
        if (!_isPlaying.value) {
            mediaPlayerManager.resume()
            _isPlaying.value = true
        }
        updateCurrentSong()
    }

    /**
     * Stops the media player and resets its state
     */
    fun stopMediaPlayer() {
        _isMiniPlayerVisible.value = false
        hideNotification()
        mediaPlayerManager.stop()
        audioViewModel.stopPlayback() // Stop the AudioService
        _currentSong.value = null
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
        _isMiniPlayerVisible.value = isVisible
    }

    /**
     * Repeat modes for the player
     */
    enum class RepeatMode {
        NONE, ALL, ONE
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

    private fun updateNotification(song: Song) {
        notificationService.showPlayingNotification(song, _isPlaying.value)
    }

    private fun hideNotification() {
        notificationService.hidePlayingNotification()
    }

    override fun onCleared() {
        super.onCleared()
        hideNotification()
    }
}