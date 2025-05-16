package com.msb.purrytify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.service.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playerManager: PlayerManager,
    private val profileModel: ProfileModel
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Get user ID from profile model
    private val userId = profileModel.currentProfile.value.id
    
    // LiveData for recently played songs
    val recentlyPlayedSongs: LiveData<List<Song>> = songRepository.fetchRecentlyPlayedSongs(userId).asLiveData()
    
    // LiveData for new songs
    val newSongs: LiveData<List<Song>> = songRepository.fetchNewSongs(userId).asLiveData()
    
    init {
        refreshSongs()
    }
    
    fun refreshSongs() {
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
    
    fun playSong(song: Song) {
        viewModelScope.launch {
            // Update last played timestamp
            songRepository.updateLastPlayedAt(song.id)
            
            // Play the song using PlayerManager
            playerManager.playSong(song)
        }
    }
    
    fun playRecentSongs(songs: List<Song>, selectedSong: Song) {
        playerManager.setPlaylist(songs)
        playSong(selectedSong)
    }
    
    fun playNewSongs(songs: List<Song>, selectedSong: Song) {
        playerManager.setPlaylist(songs)
        playSong(selectedSong)
    }
}
