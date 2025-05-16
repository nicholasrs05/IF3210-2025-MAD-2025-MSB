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
class FiftyGlobalViewModel @Inject constructor(
    private val onlineSongRepository: OnlineSongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiftyGlobalUiState())
    val uiState: StateFlow<FiftyGlobalUiState> = _uiState.asStateFlow()
    
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
    
    // Instead of directly using playerViewModel in this class, 
    // we expose functions that the UI can call to play songs
    
    // Just return the song and play state for the UI to handle
    fun getSongToPlay(song: Song): Pair<Song, List<Song>> {
        // Find song index in list
        val songs = uiState.value.songs
        val index = songs.indexOfFirst { it.id == song.id }
        
        // Return the song and full song list
        return if (index != -1) {
            Pair(song, songs)
        } else {
            Pair(song, listOf(song))
        }
    }
}

data class FiftyGlobalUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
) 