package com.msb.purrytify.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    application: Application,
    private val repository: SongRepository
) : AndroidViewModel(application) {

    val allSongs: LiveData<List<Song>> = repository.allSongs.asLiveData()
    val likedSongs: LiveData<List<Song>> = repository.likedSongs.asLiveData()
    val recentlyPlayedSongs: LiveData<List<Song>> = repository.recentlyPlayedSongs.asLiveData()
    val newSongs: LiveData<List<Song>> = repository.newSongs.asLiveData()
    val songCount: LiveData<Int> = repository.songCount.asLiveData()
    val likedSongCount: LiveData<Int> = repository.likedSongCount.asLiveData()
    val listenedSongCount: LiveData<Int> = repository.listenedSongCount.asLiveData()

    // Rest of your methods remain the same
    fun addSong(title: String, artist: String, filePath: String, artworkPath: String, duration: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val song = Song(
                title = title,
                artist = artist,
                filePath = filePath,
                artworkPath = artworkPath,
                duration = duration
            )
            repository.insert(song)
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(song)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(song)
        }
    }

    fun toggleLike(songId: Long, currentLikeStatus: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLikeStatus(songId, !currentLikeStatus)
        }
    }

    fun markAsPlayed(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLastPlayedAt(songId)
        }
    }

    fun getSongMetadata(filePath: String): Pair<String?, String?> {
        return SongRepository.extractMetadata(getApplication(), filePath)
    }

    fun getSongDuration(filePath: String): Long {
        return SongRepository.getDuration(filePath)
    }
}