package com.msb.purrytify.data.api

import com.msb.purrytify.data.model.LoginRequest
import com.msb.purrytify.data.model.LoginResponse
import com.msb.purrytify.data.model.Profile
import com.msb.purrytify.data.model.SongResponse
import com.msb.purrytify.data.model.UpdateProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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

    @Multipart
    @PATCH("/api/profile")
    suspend fun updateProfile(
        @Part location: MultipartBody.Part?,
        @Part profilePhoto: MultipartBody.Part?
    ): Response<UpdateProfileResponse>

    /* Song API */
    @GET("/api/song/{songId}")
    suspend fun getSongById(@Path("songId") songId: String): Response<SongResponse>
}