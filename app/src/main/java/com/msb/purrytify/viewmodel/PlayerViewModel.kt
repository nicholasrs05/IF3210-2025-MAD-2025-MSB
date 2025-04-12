package com.msb.purrytify.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Integrated ViewModel that combines the functionality of PlaybackViewModel and PlayerViewModel.
 * This ViewModel handles all audio playback related operations including playlist management,
 * player controls, and UI state.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    val mediaPlayerManager: MediaPlayerManager,
    private val profileModel: ProfileModel
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

    init {
        // Load playlist from repository (from PlaybackViewModel)
        loadPlaylist()

        // Setup current song state (from PlayerViewModel)
        mediaPlayerManager.getCurrentSong()?.let { song ->
            _currentSong.value = song
            _isPlaying.value = mediaPlayerManager.isPlaying()
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()
            checkLikedStatus(song.id)
        }

        // Set up completion handler
        mediaPlayerManager.onCompletion = {
            handleSongCompletion()
        }
    }

    /**
     * Loads the playlist for the current user
     */
    private fun loadPlaylist() {
        viewModelScope.launch {
            val userId = profileModel.currentProfile.value.id
            songRepository.fetchAllSongs(userId).collect { songs ->
                mediaPlayerManager.setPlaylist(songs)
            }
        }
    }

    /**
     * Sets visibility for the large player screen
     */
    fun setLargePlayerVisible(isVisible: Boolean) {
        _isLargePlayerVisible.value = isVisible
        if (!isVisible) {
            // When closing large player, ensure mini player is visible if a song exists
            _isMiniPlayerVisible.value = _currentSong.value != null
        } else {
            // When opening large player, hide mini player
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

            mediaPlayerManager.play(song)
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = 0f
            _isMiniPlayerVisible.value = true
        }
    }

    /**
     * Toggles play/pause state of the current song
     */
    fun togglePlayPause() {
        if (_isPlaying.value) {
            mediaPlayerManager.pause()
            _isPlaying.value = false
        } else {
            if (_currentSong.value != null) {
                mediaPlayerManager.resume()
                _isPlaying.value = true
            } else {
                mediaPlayerManager.getCurrentSong()?.let { song ->
                    _currentSong.value = song
                    mediaPlayerManager.resume()
                    _isPlaying.value = true
                }
            }
        }
    }

    /**
     * Skips to the next song in the playlist
     */
    fun skipToNext() {
        mediaPlayerManager.playNext()
        updateCurrentSong()
    }

    /**
     * Skips to the previous song in the playlist
     */
    fun skipToPrevious() {
        mediaPlayerManager.playPrevious()
        updateCurrentSong()
    }

    /**
     * Seeks to a specific position in the current song
     */
    fun seekTo(position: Float) {
        mediaPlayerManager.seekTo(position.toInt())
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
     * Updates the current song index in the MediaPlayerManager
     */
    fun updateCurrentSongIdx() {
        mediaPlayerManager.updateCurrentSongIdx()
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
     * Sets the current song to null without resetting other states
     */
    fun setCurrentSongNull() {
        _currentSong.value = null
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
        mediaPlayerManager.stop()
        _currentSong.value = null
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

    override fun onCleared() {
        super.onCleared()
    }
}