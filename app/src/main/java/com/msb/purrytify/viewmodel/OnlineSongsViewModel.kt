package com.msb.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.Resource
import com.msb.purrytify.data.model.SongResponse
import com.msb.purrytify.data.repository.OnlineSongRepository
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSongsViewModel @Inject constructor(
    private val onlineSongRepository: OnlineSongRepository,
    private val profileModel: ProfileModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSongsUiState())
    val uiState: StateFlow<OnlineSongsUiState> = _uiState.asStateFlow()
    
    private val _countryUiState = MutableStateFlow(OnlineSongsUiState())
    val countryUiState: StateFlow<OnlineSongsUiState> = _countryUiState.asStateFlow()

    private val _currentCountryCode = MutableStateFlow("")
    val currentCountryCode: StateFlow<String> = _currentCountryCode.asStateFlow()

    private val availableCountries = setOf("ID", "MY", "US", "GB", "CH", "DE", "BR")
    
    init {
        fetchGlobalTopSongs()
        updateCountryCodeFromProfile()
    }

    private fun updateCountryCodeFromProfile() {
        viewModelScope.launch {
            profileModel.currentProfile.collect { profile ->
                val countryCode = profile.location.uppercase()
                if (countryCode.isNotBlank() && countryCode != _currentCountryCode.value) {
                    _currentCountryCode.value = countryCode
                    fetchCountryTopSongs(countryCode)
                }
            }
        }
    }
    
    fun fetchGlobalTopSongs() {
        viewModelScope.launch {
            onlineSongRepository.getGlobalTopSongs().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        result.data?.let { songResponses ->
                            val playableSongs = onlineSongRepository.convertToPlayableSongs(songResponses)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    songs = playableSongs,
                                    error = null
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun fetchCountryTopSongs(countryCode: String = _currentCountryCode.value) {
        if (countryCode !in availableCountries) {
            _countryUiState.update { it.copy(isLoading = false, songs = emptyList(), error = "Top 10 country songs are not available in your location yet") }
            return
        }
        viewModelScope.launch {
            onlineSongRepository.getCountryTopSongs(countryCode).collect { result ->
                when (result) {
                    is Resource.Loading -> _countryUiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        val playableSongs = onlineSongRepository.convertToPlayableSongs(result.data ?: emptyList())
                        _countryUiState.update { it.copy(isLoading = false, songs = playableSongs, error = null) }
                    }
                    is Resource.Error -> _countryUiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun fetchSongById(songId: String, onResult: (Song?) -> Unit) {
        viewModelScope.launch {
            onlineSongRepository.getSongById(songId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                       // Do nothing
                    }
                    is Resource.Success -> {
                        result.data?.let { songResponse ->
                            val playableSong = onlineSongRepository.convertSingleSongResponse(songResponse)
                            Log.d("OnlineSongsViewModel", "Fetched song: $playableSong")
                            onResult(playableSong)
                        } ?: onResult(null)
                    }
                    is Resource.Error -> {
                        onResult(null)
                    }
                }
            }
        }
    }

    fun getSongToPlay(song: Song): Pair<Song, List<Song>> {
        val songs = uiState.value.songs
        val index = songs.indexOfFirst { it.id == song.id }
        
        return if (index != -1) {
            Pair(song, songs)
        } else {
            Pair(song, listOf(song))
        }
    }

    fun getCountrySongToPlay(song: Song): Pair<Song, List<Song>> {
        val songs = countryUiState.value.songs
        val index = songs.indexOfFirst { it.id == song.id }
        
        return if (index != -1) {
            Pair(song, songs)
        } else {
            Pair(song, listOf(song))
        }
    }

    fun getSongById(id: Long): SongResponse? {
        return _uiState.value.songs.find { it.id == id }?.let { song ->
            SongResponse(
                id = song.id,
                title = song.title,
                artist = song.artistName,
                artwork = song.artworkPath,
                url = song.filePath,
                duration = formatDurationToString(song.duration),
                country = if (_currentCountryCode.value.isBlank()) "GLOBAL" else _currentCountryCode.value,
                rank = _uiState.value.songs.indexOf(song) + 1,
                createdAt = "",
                updatedAt = ""
            )
        } ?: _countryUiState.value.songs.find { it.id == id }?.let { song ->
            SongResponse(
                id = song.id,
                title = song.title,
                artist = song.artistName,
                artwork = song.artworkPath,
                url = song.filePath,
                duration = formatDurationToString(song.duration),
                country = _currentCountryCode.value,
                rank = _countryUiState.value.songs.indexOf(song) + 1,
                createdAt = "",
                updatedAt = ""
            )
        }
    }

    private fun formatDurationToString(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

data class OnlineSongsUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
) 