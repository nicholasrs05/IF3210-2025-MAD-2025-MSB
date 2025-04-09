package com.msb.purrytify.ui.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
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
    private val profileModel: ProfileModel,
    private val songDao: SongDao
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

            val profileResult = profileModel.fetchProfile()

            if (profileResult is ProfileResult.Success) {
                combine(
                    songDao.getSongCount(),
                    songDao.getLikedSongCount(),
                    songDao.getListenedSongCount()
                ) { total, liked, listened ->
                    Triple(total, liked, listened)
                }.collect { (total, liked, listened) ->
                    val updatedProfile = profileResult.data.copy(
                        addedSongsCount = total,
                        likedSongsCount = liked,
                        listenedSongsCount = listened
                    )
                    _profileState.value = ProfileUiState.Success(updatedProfile)
                }
            } else if (profileResult is ProfileResult.Error) {
                _profileState.value = ProfileUiState.Error(profileResult.message)
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