package com.msb.purrytify.data.api

import android.util.Log
import com.msb.purrytify.data.storage.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import java.io.IOException
import javax.inject.Provider

class TokenInterceptor @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val moshiConverterFactory: MoshiConverterFactory,
    private val apiServiceProvider: Provider<ApiService>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403 && request.header("Authorization") != null) {
            Log.d("ForbiddenInterceptor", "Received 403, attempting token refresh.")
            val currentRefreshToken = runBlocking { dataStoreManager.refreshTokenFlow.first() }
            val currentAuthToken = runBlocking { dataStoreManager.authTokenFlow.first() }

            if (!currentRefreshToken.isNullOrBlank() && !currentAuthToken.isNullOrBlank()) {
                val apiService = apiServiceProvider.get()
                val newAuthTokenAndRefreshToken = runBlocking {
                    try {
                        val refreshTokenResponse = apiService.refreshToken(mapOf("refreshToken" to currentRefreshToken))
                        if (refreshTokenResponse.isSuccessful) {
                            refreshTokenResponse.body()?.let {
                                Pair(it.accessToken, it.refreshToken)
                            }
                        } else {
                            Log.e("ForbiddenInterceptor", "Refresh token failed: ${refreshTokenResponse.code()}")
                            runBlocking {
                                dataStoreManager.saveAuthToken("")
                                dataStoreManager.saveRefreshToken("")
                                // Potentially notify user to log in again
                            }
                            null
                        }
                    } catch (e: IOException) {
                        Log.e("ForbiddenInterceptor", "Network error during refresh: ${e.message}")
                        null
                    }
                }

                newAuthTokenAndRefreshToken?.let { (newToken, newRefreshToken) ->
                    runBlocking {
                        dataStoreManager.saveAuthToken(newToken)
                        newRefreshToken.let { dataStoreManager.saveRefreshToken(it) }
                    }
                    Log.d("ForbiddenInterceptor", "Token refreshed. Retrying original request.")
                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $newToken")
                        .build()

                    response.close()
                    return chain.proceed(newRequest)
                }
            } else {
                Log.d("ForbiddenInterceptor", "No refresh token available.")
            }
        }

        return response
    }
}
