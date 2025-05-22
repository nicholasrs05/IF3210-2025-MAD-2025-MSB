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
import android.util.Log
import com.msb.purrytify.data.repository.SoundCapsuleRepository

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playerManager: PlayerManager,
    profileModel: ProfileModel,
    private val soundCapsuleRepository: SoundCapsuleRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _showAddSongSheet = MutableStateFlow(false)
    val showAddSongSheet: StateFlow<Boolean> = _showAddSongSheet
    
    private val userId = profileModel.currentProfile.value.id
    
    val allSongs: LiveData<List<Song>> = songRepository.fetchAllSongs(userId).asLiveData()
    
    val likedSongs: LiveData<List<Song>> = songRepository.fetchLikedSongs(userId).asLiveData()
    
    val downloadedSongs: LiveData<List<Song>> = songRepository.fetchDownloadedSongs(userId).asLiveData()
    
//    init {
//        refreshLibrary()
//    }
    
//    fun refreshLibrary() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                // The collection happens via LiveData, no explicit refresh needed
//                // This function is more for future extensions
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
    
    fun toggleAddSongSheet(show: Boolean) {
        _showAddSongSheet.value = show
    }
    
    fun playLibrarySong(songs: List<Song>, selectedSong: Song) {
        try {
            playerManager.setPlaylist(songs, songs.indexOf(selectedSong))
            playerManager.playSong(selectedSong)
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Error playing song: ${e.message}")
        }
    }
    
    fun playSong(song: Song) {
        try {
            playerManager.setPlaylist(listOf(song), 0)
            playerManager.playSong(song)
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Error playing song: ${e.message}")
        }
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
