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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    private val mediaPlayerManager: MediaPlayerManager
) : AndroidViewModel(application) {

    // Player state
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

    fun setLargePlayerVisible(isVisible: Boolean) {
        _isLargePlayerVisible.value = isVisible
        _isMiniPlayerVisible.value = !isVisible
    }

    init {
        mediaPlayerManager.getCurrentSong()?.let { song ->
            _currentSong.value = song
            _isPlaying.value = mediaPlayerManager.isPlaying()
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()

            checkLikedStatus(song.id)
        }

        mediaPlayerManager.onCompletion = {
            handleSongCompletion()
        }
    }

    // Play a song
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

    // Toggle play/pause
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

    // Skip to next song
    fun skipToNext() {
        mediaPlayerManager.playNext()
        updateCurrentSong()
    }

    // Skip to previous song
    fun skipToPrevious() {
        mediaPlayerManager.playPrevious()
        updateCurrentSong()
    }

    // Seek to position
    fun seekTo(position: Float) {
        mediaPlayerManager.seekTo(position.toInt())
        _currentPosition.floatValue = position
    }

    // Toggle like status
    fun toggleLike() {
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                val newLikeStatus = !_isLiked.value
                songRepository.updateLikeStatus(song.id, newLikeStatus)
                _isLiked.value = newLikeStatus
            }
        }
    }

    fun updateCurrentSongIdx() {
        mediaPlayerManager.updateCurrentSongIdx()
    }

    // Toggle shuffle
    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        mediaPlayerManager.shuffle()
    }

    // Toggle repeat mode
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

    // Update position for the UI
    fun updatePosition() {
        if (_isPlaying.value) {
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()
        }
    }

    // Update the current song data from MediaPlayerManager
    fun updateCurrentSong() {
        val currentSong = mediaPlayerManager.getCurrentSong()
        if (currentSong != null) {
            _currentSong.value = currentSong
            _duration.floatValue = mediaPlayerManager.getDuration().toFloat()
            _currentPosition.floatValue = mediaPlayerManager.getCurrentPosition().toFloat()
            _isPlaying.value = mediaPlayerManager.isPlaying()
            checkLikedStatus(currentSong.id)
        }
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

    // Check if song is liked
    private fun checkLikedStatus(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            _isLiked.value = song?.isLiked == true
        }
    }

    // Handle song completion based on repeat mode
    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.NONE -> skipToNext()
            RepeatMode.ALL -> skipToNext()
            RepeatMode.ONE -> updateCurrentSong()
        }
    }

    // Reset player state when no songs are left or player is released
    fun resetCurrentSong() {
        _currentSong.value = null
        _isPlaying.value = false
        _duration.floatValue = 0f
        _currentPosition.floatValue = 0f
        _isLiked.value = false
    }

    // Resume current song without restarting it
    fun resumeCurrentSong() {
        if (!_isPlaying.value) {
            mediaPlayerManager.resume()
            _isPlaying.value = true
        }
        updateCurrentSong()
    }

    enum class RepeatMode {
        NONE, ALL, ONE
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun stopMediaPlayer(){
        _isMiniPlayerVisible.value = false
        mediaPlayerManager.stop()
        _currentSong.value = null
    }

    fun setMiniPlayerVisible(isVisible: Boolean) {
        _isMiniPlayerVisible.value = isVisible
    }
}