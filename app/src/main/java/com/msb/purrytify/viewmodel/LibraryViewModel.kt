package com.msb.purrytify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val mediaPlayerManager: MediaPlayerManager,
    private val profileModel: ProfileModel
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _showAddSongSheet = MutableStateFlow(false)
    val showAddSongSheet: StateFlow<Boolean> = _showAddSongSheet
    
    // Get user ID from profile model
    private val userId = profileModel.currentProfile.value.id
    
    // LiveData for all songs
    val allSongs: LiveData<List<Song>> = songRepository.fetchAllSongs(userId).asLiveData()
    
    // LiveData for liked songs
    val likedSongs: LiveData<List<Song>> = songRepository.fetchLikedSongs(userId).asLiveData()
    
    init {
        refreshLibrary()
    }
    
    fun refreshLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // The collection happens via LiveData, no explicit refresh needed
                // This function is more for future extensions
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleAddSongSheet(show: Boolean) {
        _showAddSongSheet.value = show
    }
    
    fun playSong(song: Song) {
        viewModelScope.launch {
            // Update last played timestamp
            songRepository.updateLastPlayedAt(song.id)
            
            // Play the song
            mediaPlayerManager.play(song)
        }
    }
    
    fun playLibrarySong(songs: List<Song>, selectedSong: Song) {
        mediaPlayerManager.setPlaylist(songs)
        playSong(selectedSong)
    }
    
    fun toggleLike(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            song?.let {
                songRepository.updateLikeStatus(songId, !it.isLiked)
            }
        }
    }
}
