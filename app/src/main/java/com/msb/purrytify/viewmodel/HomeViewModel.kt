package com.msb.purrytify.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.service.PlayerManager
import com.msb.purrytify.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    private val playerManager: PlayerManager,
    profileModel: ProfileModel,
) : AndroidViewModel(application) {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError
    
    private val userId = profileModel.currentProfile.value.id
    
    val recentlyPlayedSongs: LiveData<List<Song>> = songRepository.fetchRecentlyPlayedSongs(userId).asLiveData()
    
    val newSongs: LiveData<List<Song>> = songRepository.fetchNewSongs(userId).asLiveData()

    
    fun playSong(song: Song) {
        viewModelScope.launch {
            // Check if file is accessible before attempting to play
            if (!song.isFromApi && !FileUtils.isFileAccessible(getApplication(), song.filePath)) {
                _playbackError.value = "Song file not found or moved. Cannot play this song."
                return@launch
            }
            
            playerManager.playSong(song)
        }
    }
    
    fun playRecentSongs(songs: List<Song>, selectedSong: Song) {
        try {
            Log.d("HomeViewModel", "Playing recent song: ${selectedSong.title}, artwork: ${selectedSong.artworkPath}")
            
            // Filter out inaccessible local songs
            val accessibleSongs = songs.filter { song ->
                if (song.isFromApi) {
                    true
                } else {
                    val isAccessible = FileUtils.isFileAccessible(getApplication(), song.filePath)
                    if (!isAccessible) {
                        Log.w("HomeViewModel", "Skipping inaccessible song: ${song.title}")
                    }
                    isAccessible
                }
            }
            
            if (accessibleSongs.isEmpty()) {
                _playbackError.value = "No accessible songs found in the playlist."
                return
            }
            
            // Check if selected song is accessible
            if (!selectedSong.isFromApi && !FileUtils.isFileAccessible(getApplication(), selectedSong.filePath)) {
                _playbackError.value = "Selected song file not found or moved. Cannot play this song."
                return
            }
            
            // Find the index of the selected song in the accessible playlist
            val songIndex = accessibleSongs.indexOfFirst { it.id == selectedSong.id }
            if (songIndex >= 0) {
                // Set the playlist with the correct starting index
                playerManager.setPlaylist(accessibleSongs, songIndex)
            } else {
                // If song not found in accessible playlist, just play it directly
                playSong(selectedSong)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error playing recent songs: ${e.message}", e)
            _playbackError.value = "Error playing song: ${e.message}"
        }
    }
    
    fun playNewSongs(songs: List<Song>, selectedSong: Song) {
        try {
            Log.d("HomeViewModel", "Playing new song: ${selectedSong.title}, artwork: ${selectedSong.artworkPath}")
            
            // Filter out inaccessible local songs
            val accessibleSongs = songs.filter { song ->
                if (song.isFromApi) {
                    true
                } else {
                    val isAccessible = FileUtils.isFileAccessible(getApplication(), song.filePath)
                    if (!isAccessible) {
                        Log.w("HomeViewModel", "Skipping inaccessible song: ${song.title}")
                    }
                    isAccessible
                }
            }
            
            if (accessibleSongs.isEmpty()) {
                _playbackError.value = "No accessible songs found in the playlist."
                return
            }
            
            // Check if selected song is accessible
            if (!selectedSong.isFromApi && !FileUtils.isFileAccessible(getApplication(), selectedSong.filePath)) {
                _playbackError.value = "Selected song file not found or moved. Cannot play this song."
                return
            }
            
            // Find the index of the selected song in the accessible playlist
            val songIndex = accessibleSongs.indexOfFirst { it.id == selectedSong.id }
            if (songIndex >= 0) {
                // Set the playlist with the correct starting index
                playerManager.setPlaylist(accessibleSongs, songIndex)
            } else {
                // If song not found in accessible playlist, just play it directly
                playSong(selectedSong)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error playing new songs: ${e.message}", e)
            _playbackError.value = "Error playing song: ${e.message}"
        }
    }
    
    fun clearPlaybackError() {
        _playbackError.value = null
    }
}
