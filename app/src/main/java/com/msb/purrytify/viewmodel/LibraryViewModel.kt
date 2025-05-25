package com.msb.purrytify.viewmodel

import android.app.Application
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
import android.util.Log
import com.msb.purrytify.data.repository.SoundCapsuleRepository

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    private val playerManager: PlayerManager,
    profileModel: ProfileModel,
) : AndroidViewModel(application) {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _showAddSongSheet = MutableStateFlow(false)
    val showAddSongSheet: StateFlow<Boolean> = _showAddSongSheet
    
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError
    
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

    fun playLibrarySong(songs: List<Song>, selectedSong: Song) {
        Log.d("LibraryViewModel", "playLibrarySong called with ${songs.size} songs")
        Log.d("LibraryViewModel", "Playing song: ${selectedSong.title}, artwork: ${selectedSong.artworkPath}")

        try {
            // Filter out inaccessible local songs
            val accessibleSongs = songs.filter { song ->
                if (song.isFromApi) {
                    true
                } else {
                    val isAccessible = FileUtils.isFileAccessible(getApplication(), song.filePath)
                    if (!isAccessible) {
                        Log.w("LibraryViewModel", "Skipping inaccessible song: ${song.title}")
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
            
            val songIndex = accessibleSongs.indexOfFirst { it.id == selectedSong.id }
            Log.d("LibraryViewModel", "Setting playlist with starting index: $songIndex")
            
            if (songIndex >= 0) {
                playerManager.setPlaylist(accessibleSongs, songIndex)
            } else {
                // If song not found in accessible playlist, just play it directly
                playSong(selectedSong)
            }

            Log.d("LibraryViewModel", "Updating last played timestamp for song ID: ${selectedSong.id}")
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Error playing library song: ${e.message}", e)
            _playbackError.value = "Error playing song: ${e.message}"
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
    
    fun clearPlaybackError() {
        _playbackError.value = null
    }
}
