package com.msb.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.data.repository.SongRepository
import kotlinx.coroutines.launch

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    val mediaPlayerManager: MediaPlayerManager,
    private val songRepository: SongRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            songRepository.fetchAllSongs().collect { songs ->
                mediaPlayerManager.setPlaylist(songs)
            }
        }
    }
}