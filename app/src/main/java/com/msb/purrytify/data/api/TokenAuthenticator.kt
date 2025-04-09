package com.msb.purrytify.data.api

import android.util.Log
import com.msb.purrytify.data.storage.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val converterFactory: MoshiConverterFactory
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "Authenticating for response: ${response.request.url}")
        Log.d(
            "TokenAuthenticator",
            "Req authorization: ${response.request.header("Authorization")}"
        )

        if (response.request.header("Authorization") == null) {
            return null
        }

        val currentRefreshToken = runBlocking { dataStoreManager.refreshTokenFlow.first() }
        val currentAuthToken = runBlocking { dataStoreManager.authTokenFlow.first() }

        // Avoid infinite loops if refresh token also fails
        if (currentRefreshToken.isNullOrBlank() || currentAuthToken.isNullOrBlank()) {
            return null
        }

        // Synchronously call the refresh token API
        val newAuthTokenAndRefreshToken = runBlocking {
            try {
                val apiService = Retrofit.Builder()
                    .baseUrl("http://34.101.226.132:3000")
                    .addConverterFactory(converterFactory)
                    .build()
                    .create(ApiService::class.java)

                val refreshTokenResponse =
                    apiService.refreshToken(mapOf("refreshToken" to currentRefreshToken))
                if (refreshTokenResponse.isSuccessful) {
                    refreshTokenResponse.body()?.let {
                        Pair(it.accessToken, it.refreshToken)
                    }
                } else {
                    // Handle refresh token failure (e.g., clear tokens, force login)
                    null
                }
            } catch (e: IOException) {
                // Handle network errors during refresh
                null
            }
        }

        return newAuthTokenAndRefreshToken?.let { (newToken, newRefreshToken) ->
            runBlocking {
                dataStoreManager.saveAuthToken(newToken)
                newRefreshToken.let { dataStoreManager.saveRefreshToken(it) }
            }
            response.request.newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", "Bearer $newToken")
                .build()
        }
        return null
    }
}