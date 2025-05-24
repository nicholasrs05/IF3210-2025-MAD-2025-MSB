package com.msb.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.SongResponse
import com.msb.purrytify.data.repository.OnlineSongDownloadRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSongDownloadViewModel @Inject constructor(
    private val onlineSongDownloadRepository: OnlineSongDownloadRepository,
    private val songRepository: SongRepository,
    profileModel: ProfileModel
) : ViewModel() {
    
    private val userId = profileModel.currentProfile.value.id
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    fun downloadSong(songResponse: SongResponse) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading
            try {
                if (songRepository.isSongDownloaded(songResponse.id, userId)) {
                    _downloadState.value = DownloadState.AlreadyDownloaded
                    return@launch
                }
                
                val song = onlineSongDownloadRepository.downloadSong(songResponse, userId)
                _downloadState.value = DownloadState.Success(song)
                Log.d("OnlineSongDownloadVM", "Song downloaded successfully: ${song.title}")
            } catch (e: Exception) {
                Log.e("OnlineSongDownloadVM", "Error downloading song: ${e.message}", e)
                _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun isDownloaded(songId: Long): Flow<Boolean> = flow {
        emit(songRepository.isSongDownloaded(songId, userId))
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        object Downloading : DownloadState()
        data class Success(val song: Song) : DownloadState()
        object AlreadyDownloaded : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
} 