package com.msb.purrytify.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.model.Profile
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.model.ProfileResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: Profile) : ProfileUiState()
    data class Error(val errorMessage: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileModel: ProfileModel,
    private val songDao: SongDao
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState

    init {
        fetchProfileData()
        observeProfileData()
    }

    private fun observeProfileData() {
        viewModelScope.launch {
            profileModel.currentProfile.collect { profileData ->
                combine(
                    songDao.getSongCount(),
                    songDao.getLikedSongCount(),
                    songDao.getListenedSongCount()
                ) { total, liked, listened ->
                    profileData.copy(
                        addedSongsCount = total,
                        likedSongsCount = liked,
                        listenedSongsCount = listened
                    )
                }.collect { updatedProfile ->
                    _profileState.value = ProfileUiState.Success(updatedProfile)
                }
            }
        }
    }

    fun fetchProfileData() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            profileModel.fetchProfileResult.collect { result ->
                when (result) {
                    is ProfileResult.Success -> {
                        _profileState.value = ProfileUiState.Success(Profile())
                    }

                    is ProfileResult.Error -> {
                        _profileState.value = ProfileUiState.Error(result.message)
                    }
                    is ProfileResult.Loading -> {
                        _profileState.value = ProfileUiState.Loading
                    }
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            profileModel.fetchProfile()

            profileModel.currentProfile.collect { profileData ->
                combine(
                    songDao.getSongCount(),
                    songDao.getLikedSongCount(),
                    songDao.getListenedSongCount()
                ) { total, liked, listened ->
                    profileData.copy(
                        addedSongsCount = total,
                        likedSongsCount = liked,
                        listenedSongsCount = listened
                    )
                }.collect { updatedProfile ->
                    _profileState.value = ProfileUiState.Success(updatedProfile)
                }
            }
        }
    }
}