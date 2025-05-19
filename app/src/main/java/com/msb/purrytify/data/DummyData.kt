package com.msb.purrytify.data

import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song

object DummyData {
    val dummySongs = listOf(
        Song(
            id = 1,
            title = "Shape of You",
            artistName = "Ed Sheeran",
            artistId = 1, // Ed Sheeran
            duration = 235000, // 3:55 in milliseconds
            filePath = "",
            artworkPath = "",
            isLiked = false,
            addedAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis(),
            ownerId = 1,
            isFromApi = true,
            playCount = 2500000
        ),
        Song(
            id = 2,
            title = "Blinding Lights",
            artistName = "The Weeknd",
            artistId = 2, // The Weeknd
            duration = 200000, // 3:20 in milliseconds
            filePath = "",
            artworkPath = "",
            isLiked = false,
            addedAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis(),
            ownerId = 1,
            isFromApi = true,
            playCount = 1800000
        ),
        Song(
            id = 3,
            title = "Dance Monkey",
            artistName = "Tones and I",
            artistId = 3, // Taylor Swift
            duration = 210000, // 3:30 in milliseconds
            filePath = "",
            artworkPath = "",
            isLiked = false,
            addedAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis(),
            ownerId = 1,
            isFromApi = true,
            playCount = 1500000
        ),
        Song(
            id = 4,
            title = "Someone You Loved",
            artistName = "Lewis Capaldi",
            artistId = 4, // Drake
            duration = 182000, // 3:02 in milliseconds
            filePath = "",
            artworkPath = "",
            isLiked = false,
            addedAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis(),
            ownerId = 1,
            isFromApi = true,
            playCount = 1200000
        ),
        Song(
            id = 5,
            title = "Bad Guy",
            artistName = "Billie Eilish",
            artistId = 5, // Billie Eilish
            duration = 194000, // 3:14 in milliseconds
            filePath = "",
            artworkPath = "",
            isLiked = false,
            addedAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis(),
            ownerId = 1,
            isFromApi = true,
            playCount = 1000000
        )
    )

    val dummyArtists = listOf(
        Artist(
            id = 1,
            name = "Ed Sheeran",
            imageUrl = null
        ),
        Artist(
            id = 2,
            name = "The Weeknd",
            imageUrl = null
        ),
        Artist(
            id = 3,
            name = "Taylor Swift",
            imageUrl = null
        ),
        Artist(
            id = 4,
            name = "Drake",
            imageUrl = null
        ),
        Artist(
            id = 5,
            name = "Billie Eilish",
            imageUrl = null
        )
    )
} 