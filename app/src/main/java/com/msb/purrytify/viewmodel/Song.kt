package com.msb.purrytify.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.PurrytifyDatabase
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.di.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class SongViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository
    val allSongs: LiveData<List<Song>>
    val likedSongs: LiveData<List<Song>>
    val recentlyPlayedSongs: LiveData<List<Song>>
    val songCount: LiveData<Int>
    val likedSongCount: LiveData<Int>
    val listenedSongCount: LiveData<Int>

    init {
        val songDao = PurrytifyDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao)
        allSongs = repository.allSongs.asLiveData()
        likedSongs = repository.likedSongs.asLiveData()
        recentlyPlayedSongs = repository.recentlyPlayedSongs.asLiveData()
        songCount = repository.songCount.asLiveData()
        likedSongCount = repository.likedSongCount.asLiveData()
        listenedSongCount = repository.listenedSongCount.asLiveData()
    }

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