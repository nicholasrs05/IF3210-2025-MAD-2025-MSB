package com.msb.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.data.repository.SoundCapsuleRepository
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
    profileModel: ProfileModel,
    private val soundCapsuleRepository: SoundCapsuleRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val userId = profileModel.currentProfile.value.id
    
    val recentlyPlayedSongs: LiveData<List<Song>> = songRepository.fetchRecentlyPlayedSongs(userId).asLiveData()
    
    val newSongs: LiveData<List<Song>> = songRepository.fetchNewSongs(userId).asLiveData()

    
    fun playSong(song: Song) {
        viewModelScope.launch {
            // Update last played timestamp
            songRepository.updateLastPlayedAt(song.id)
            soundCapsuleRepository.incrementSongPlayCount(song.id, userId)

            // Play the song using PlayerManager
            playerManager.playSong(song)
        }
    }
    
    fun playRecentSongs(songs: List<Song>, selectedSong: Song) {
        try {
            Log.d("HomeViewModel", "Playing recent song: ${selectedSong.title}, artwork: ${selectedSong.artworkPath}")
            
            // Find the index of the selected song in the playlist
            val songIndex = songs.indexOfFirst { it.id == selectedSong.id }
            if (songIndex >= 0) {
                // Set the playlist with the correct starting index
                playerManager.setPlaylist(songs, songIndex)
                
                // Update last played timestamp
                viewModelScope.launch {
                    songRepository.updateLastPlayedAt(selectedSong.id)
                    soundCapsuleRepository.incrementSongPlayCount(selectedSong.id, userId)
                }
            } else {
                // If song not found in playlist (shouldn't happen), just play it directly
                playSong(selectedSong)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error playing recent songs: ${e.message}", e)
            // Fallback to direct song play in case of error
            try {
                playSong(selectedSong)
            } catch (e2: Exception) {
                Log.e("HomeViewModel", "Failed fallback attempt: ${e2.message}", e2)
            }
        }
    }
    
    fun playNewSongs(songs: List<Song>, selectedSong: Song) {
        try {
            Log.d("HomeViewModel", "Playing new song: ${selectedSong.title}, artwork: ${selectedSong.artworkPath}")
            
            // Find the index of the selected song in the playlist
            val songIndex = songs.indexOfFirst { it.id == selectedSong.id }
            if (songIndex >= 0) {
                // Set the playlist with the correct starting index
                playerManager.setPlaylist(songs, songIndex)
                
                // Update last played timestamp
                viewModelScope.launch {
                    soundCapsuleRepository.incrementSongPlayCount(selectedSong.id, userId)
                    songRepository.updateLastPlayedAt(selectedSong.id)
                }
            } else {
                // If song not found in playlist (shouldn't happen), just play it directly
                playSong(selectedSong)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error playing new songs: ${e.message}", e)
            // Fallback to direct song play in case of error
            try {
                playSong(selectedSong)
            } catch (e2: Exception) {
                Log.e("HomeViewModel", "Failed fallback attempt: ${e2.message}", e2)
            }
        }
    }
}
