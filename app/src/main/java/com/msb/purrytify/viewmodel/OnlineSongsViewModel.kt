package com.msb.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.Resource
import com.msb.purrytify.data.repository.OnlineSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSongsViewModel @Inject constructor(
    private val onlineSongRepository: OnlineSongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSongsUiState())
    val uiState: StateFlow<OnlineSongsUiState> = _uiState.asStateFlow()
    
    private val _countryUiState = MutableStateFlow(OnlineSongsUiState())
    val countryUiState: StateFlow<OnlineSongsUiState> = _countryUiState.asStateFlow()

    private val availableCountries = setOf("ID", "MY", "US", "GB", "CH", "DE", "BR")
    
    init {
        fetchGlobalTopSongs()
    }
    
    fun fetchGlobalTopSongs(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            onlineSongRepository.getGlobalTopSongs(forceRefresh).collect { result ->
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

    fun fetchCountryTopSongs(countryCode: String, forceRefresh: Boolean = false) {
        if (countryCode !in availableCountries) {
            _countryUiState.update { it.copy(isLoading = false, songs = emptyList(), error = "Top 10 country songs are not available in your location yet") }
            return
        }
        viewModelScope.launch {
            onlineSongRepository.getCountryTopSongs(countryCode, forceRefresh).collect { result ->
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
}

data class OnlineSongsUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
) 