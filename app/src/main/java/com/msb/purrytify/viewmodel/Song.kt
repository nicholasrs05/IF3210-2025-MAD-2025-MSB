package com.msb.purrytify.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    application: Application,
    private val repository: SongRepository,
    profileModel: ProfileModel
) : AndroidViewModel(application) {

    val userId = profileModel.currentProfile.value.id
    val allSongs: LiveData<List<Song>> = repository.fetchAllSongs(userId).asLiveData()
    val likedSongs: LiveData<List<Song>> = repository.fetchLikedSongs(userId).asLiveData()
    val recentlyPlayedSongs: LiveData<List<Song>> = repository.fetchRecentlyPlayedSongs(userId).asLiveData()
    val newSongs: LiveData<List<Song>> = repository.fetchNewSongs(userId).asLiveData()

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
            repository.insert(song)
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(song)
        }
    }

    fun markAsPlayed(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLastPlayedAt(songId)
        }
    }

    fun getSongMetadata(filePath: String): Pair<String?, String?> {
        return SongRepository.extractMetadata(filePath)
    }

    fun getSongDuration(filePath: String): Long {
        return SongRepository.getDuration(filePath)
    }
}