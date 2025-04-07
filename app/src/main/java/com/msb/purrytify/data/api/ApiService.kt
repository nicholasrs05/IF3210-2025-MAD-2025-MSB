package com.msb.purrytify.data.api

import com.msb.purrytify.data.model.LoginRequest
import com.msb.purrytify.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

}