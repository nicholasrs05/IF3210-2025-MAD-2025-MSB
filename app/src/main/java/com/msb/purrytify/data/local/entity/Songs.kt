package com.msb.purrytify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val duration: Long,
    val filePath: String,
    val artworkPath: String,
    val isLiked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val ownerId: Long,
    val isFromApi: Boolean = false  // Flag to identify if this song came from the API
)