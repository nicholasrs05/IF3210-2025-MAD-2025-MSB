package com.msb.purrytify.data.remote.model

import com.google.gson.annotations.SerializedName

data class SongResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("artwork") val artwork: String,
    @SerializedName("url") val url: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("country") val country: String,
    @SerializedName("rank") val rank: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)