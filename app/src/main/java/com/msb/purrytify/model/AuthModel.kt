package com.msb.purrytify.model

import android.util.Log
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.model.LoginRequest
import com.msb.purrytify.data.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val exception: Exception, val message: String? = null) : AuthResult<Nothing>()
}

@Singleton
class AuthModel @Inject constructor(
    private val apiService: ApiService,
) {

    suspend fun login(username: String, password: String): AuthResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(username, password)
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val loginResponse = response.body()!!

                    AuthResult.Success(loginResponse)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("AuthRepository", "Login failed: ${response.code()} - $errorBody")
                    AuthResult.Error(Exception("API Error: ${response.code()}"), errorBody)
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Login exception", e)
                AuthResult.Error(e, "Network or parsing error: ${e.message}")
            }
        }
    }
}