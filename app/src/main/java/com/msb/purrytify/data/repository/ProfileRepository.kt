package com.msb.purrytify.data.repository

import android.util.Log
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

sealed class ProfileUpdateResult {
    data class Success(val message: String, val profile: Profile) : ProfileUpdateResult()
    data class Error(val message: String) : ProfileUpdateResult()
}

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun updateProfile(
        location: MultipartBody.Part?,
        profilePhoto: MultipartBody.Part?
    ): ProfileUpdateResult = withContext(Dispatchers.IO) {
        try {
            if (location == null && profilePhoto == null) {
                return@withContext ProfileUpdateResult.Error("No changes to save")
            }
            
            val response = apiService.updateProfile(location, profilePhoto)
            
            if (response.isSuccessful) {
                val updateResponse = response.body()
                val successMessage = updateResponse?.message ?: "Profile updated successfully"
                
                // After successful update, fetch the updated profile
                try {
                    val profileResponse = apiService.getProfile()
                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        return@withContext ProfileUpdateResult.Success(
                            message = successMessage,
                            profile = profileResponse.body()!!
                        )
                    } else {
                        // If we can't get the profile, return a success with a default profile
                        Log.w("ProfileRepository", "Update successful but couldn't fetch profile")
                        return@withContext ProfileUpdateResult.Success(
                            message = successMessage,
                            profile = Profile()
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ProfileRepository", "Error fetching profile after update", e)
                    return@withContext ProfileUpdateResult.Success(
                        message = successMessage,
                        profile = Profile()
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to update profile: HTTP ${response.code()}" +
                        (errorBody?.let { " - $it" } ?: "")
                Log.e("ProfileRepository", errorMessage)
                return@withContext ProfileUpdateResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating profile", e)
            return@withContext ProfileUpdateResult.Error("Error updating profile: ${e.message}")
        }
    }
}
