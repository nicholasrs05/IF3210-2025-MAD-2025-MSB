package com.msb.purrytify.data.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email")
    val username: String,
    @Json(name = "password")
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "accessToken")
    val accessToken: String,
    @Json(name = "refreshToken")
    val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class LoginError(
    @Json(name = "error")
    val error: String
)
