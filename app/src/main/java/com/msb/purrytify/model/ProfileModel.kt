package com.msb.purrytify.model

import android.util.Log
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class ProfileResult<out T : Any> {
    data class Success<out T : Any>(val data: T) : ProfileResult<T>()
    data class Error(val message: String) : ProfileResult<Nothing>()
    object Loading : ProfileResult<Nothing>()
}

@Singleton
class ProfileModel @Inject constructor(
    private val apiService: ApiService,
) {

    private val _currentProfile = MutableStateFlow(Profile())
    val currentProfile: StateFlow<Profile> = _currentProfile
    private val _fetchProfileResult = MutableStateFlow<ProfileResult<Unit>>(ProfileResult.Loading)
    val fetchProfileResult: StateFlow<ProfileResult<Unit>> = _fetchProfileResult

    init {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            fetchProfileInternal()
        }
    }

    private suspend fun fetchProfileInternal() {
        _fetchProfileResult.value = ProfileResult.Loading
        _currentProfile.value = Profile()
        try {
            val response = apiService.getProfile()
            Log.d("ProfileModel", "Response: $response")

            if (response.isSuccessful) {
                response.body()?.let { profile ->
                    _currentProfile.value = profile
                    _fetchProfileResult.value = ProfileResult.Success(Unit)
                } ?: run {
                    _fetchProfileResult.value = ProfileResult.Error("Response body is null")
                }


            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch profile: HTTP ${response.code()}" +
                        (errorBody?.let { " - $it" } ?: "")
                _fetchProfileResult.value = ProfileResult.Error(errorMessage)
            }
        } catch (e: HttpException) {
            _fetchProfileResult.value = ProfileResult.Error("HTTP error fetching profile: ${e.message()}")
        } catch (e: IOException) {
            _fetchProfileResult.value = ProfileResult.Error("Network error fetching profile: ${e.message}")
        }
    }

    suspend fun fetchProfile() = withContext(Dispatchers.IO) {
        fetchProfileInternal()
    }
}