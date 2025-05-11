package com.msb.purrytify.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SongResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "artist") val artist: String,
    @Json(name = "artwork") val artwork: String,
    @Json(name = "url") val url: String,
    @Json(name = "duration") val duration: String,
    @Json(name = "country") val country: String,
    @Json(name = "rank") val rank: Int,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String
)