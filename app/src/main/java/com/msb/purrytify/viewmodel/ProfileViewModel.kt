package com.msb.purrytify.ui.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.model.Profile
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.model.ProfileResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: Profile) : ProfileUiState()
    data class Error(val errorMessage: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileModel: ProfileModel
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState

    init {
        fetchProfileData()
        // loadDummyProfile()
    }

    fun fetchProfileData() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            when (val result = profileModel.fetchProfile()) {
                is ProfileResult.Success -> {
                    _profileState.value = ProfileUiState.Success(result.data)
                }
                is ProfileResult.Error -> {
                    _profileState.value = ProfileUiState.Error(result.message)
                }
                ProfileResult.Loading -> {
                    _profileState.value = ProfileUiState.Loading
                }
            }
        }
    }

    private fun loadDummyProfile() {
        val dummyProfile = Profile(
            id = 123,
            username = "dummyUser",
            email = "dummy@example.com",
            profilePhoto = "dummy_profile.png",
            location = "Jakarta, Indonesia",
            createdAtString = OffsetDateTime.now().minusDays(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            updatedAtString = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            addedSongsCount = 15,
            likedSongsCount = 42,
            listenedSongsCount = 101
        )
        _profileState.value = ProfileUiState.Success(dummyProfile)
    }

}