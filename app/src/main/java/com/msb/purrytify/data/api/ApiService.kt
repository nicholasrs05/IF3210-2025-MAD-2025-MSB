package com.msb.purrytify.data.api

import com.msb.purrytify.data.model.LoginRequest
import com.msb.purrytify.data.model.LoginResponse
import com.msb.purrytify.data.model.Profile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/api/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/refresh-token")
    suspend fun refreshToken(@Body refreshTokenRequest: Map<String, String>): Response<LoginResponse>

    @GET("/api/verify-token")
    suspend fun verifyToken(): Response<Unit>

    @GET("/api/profile")
    suspend fun getProfile(): Response<Profile>
}