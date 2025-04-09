package com.msb.purrytify.model

import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.model.Profile
import com.msb.purrytify.data.storage.DataStoreManager
import com.msb.purrytify.viewmodel.UiState
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

class ProfileModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val moshi: Moshi // You might need this for error parsing if the API has specific error responses
) {

    private val _currentProfile = MutableStateFlow(Profile())
    val currentProfile: StateFlow<Profile> = _currentProfile

    suspend fun fetchProfile(): ProfileResult<Profile> = withContext(Dispatchers.IO) {
        ProfileResult.Loading
        try {
            val authToken = dataStoreManager.authTokenFlow.first()
            if (authToken.isNullOrBlank()) {
                return@withContext ProfileResult.Error("Authentication token not found.")
            }
            val response = apiService.getProfile()

            if (response.isSuccessful) {
                if (response.body() != null) {
                    _currentProfile.value = response.body()!!
                } else {
                    return@withContext ProfileResult.Error("Response body is null")
                }

                return@withContext ProfileResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch profile: HTTP ${response.code()}" +
                        (errorBody?.let { " - $it" } ?: "")
                return@withContext ProfileResult.Error(errorMessage)
            }
        } catch (e: HttpException) {
            return@withContext ProfileResult.Error("HTTP error fetching profile: ${e.message()}")
        } catch (e: IOException) {
            return@withContext ProfileResult.Error("Network error fetching profile: ${e.message}")
        }
    }

}