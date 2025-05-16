package com.msb.purrytify.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateProfileResponse(
    @Json(name = "message")
    val message: String,
)
