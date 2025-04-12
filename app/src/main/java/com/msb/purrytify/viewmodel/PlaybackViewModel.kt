package com.msb.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import kotlinx.coroutines.launch

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    val mediaPlayerManager: MediaPlayerManager,
    private val songRepository: SongRepository,
    private val profileModel: ProfileModel
) : ViewModel() {
    init {
        viewModelScope.launch {
            val userId = profileModel.currentProfile.value.id
            songRepository.fetchAllSongs(userId).collect { songs ->
                mediaPlayerManager.setPlaylist(songs)
            }
        }
    }
}