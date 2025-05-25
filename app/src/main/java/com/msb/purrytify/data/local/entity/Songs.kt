package com.msb.purrytify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["playCount"], name = "idx_song_play_count"),
        Index(value = ["lastPlayedAt"], name = "idx_song_last_played"),
        Index(value = ["artistId"], name = "idx_song_artist_id"),
        Index(value = ["ownerId"], name = "idx_song_owner_id"),
        Index(value = ["addedAt"], name = "idx_song_added_at"),
        Index(value = ["isLiked"], name = "idx_song_is_liked")
    ]
)
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artistName: String,
    val artistId: Long,
    val duration: Long,
    val filePath: String,
    val artworkPath: String,
    val isLiked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val ownerId: Long,
    val isFromApi: Boolean = false,
    val onlineSongId: Long? = null,
    val playCount: Int = 0
)