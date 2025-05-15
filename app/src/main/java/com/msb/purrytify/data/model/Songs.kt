package com.msb.purrytify.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val coverArt: Int,
    val duration: Long,
    val filePath: String,
    val artworkPath: String,
    val isLiked: Boolean,
    val addedAt: Long,
    val lastPlayedAt: Long?,
    val playCount: Int = 0
)

