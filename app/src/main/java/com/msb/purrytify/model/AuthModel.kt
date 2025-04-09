package com.msb.purrytify.model

import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.storage.DataStoreManager // Import DataStoreManager
import com.msb.purrytify.data.model.LoginError
import com.msb.purrytify.data.model.LoginRequest
import com.msb.purrytify.data.model.LoginResponse
import com.msb.purrytify.data.model.Profile
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T : Any> {
    data class Success<out T : Any>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Singleton
class AuthModel @Inject constructor(
    private val apiService: ApiService,
    private val moshi: Moshi,
    private val dataStoreManager: DataStoreManager // Inject DataStoreManager
) {
    suspend fun login(username: String, password: String): AuthResult<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(username = username, password = password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        dataStoreManager.saveAuthToken(loginResponse.accessToken) // Store tokens
                        dataStoreManager.saveRefreshToken(loginResponse.refreshToken)
                        AuthResult.Success(loginResponse)
                    } else {
                        AuthResult.Error("Login successful but response body is null")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        try {
                            val adapter = moshi.adapter(LoginError::class.java)
                            val loginError = adapter.fromJson(errorBody)
                            AuthResult.Error(loginError?.error ?: "Login failed with an unknown error")
                        } catch (e: IOException) {
                            AuthResult.Error("Failed to parse error response: ${e.message}")
                        }
                    } else {
                        AuthResult.Error("Login failed with HTTP status code: ${response.code()}")
                    }
                }
            } catch (e: HttpException) {
                AuthResult.Error("HTTP error during login: ${e.message()}")
            } catch (e: IOException) {
                AuthResult.Error("Network error during login: ${e.message}")
            }
        }



    suspend fun clearAuthData() {
        dataStoreManager.clearCredentials()
    }
}