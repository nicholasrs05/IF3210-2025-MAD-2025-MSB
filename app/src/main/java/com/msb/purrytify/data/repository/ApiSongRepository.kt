package com.msb.purrytify.data.repository

import android.util.Log
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.remote.RetrofitClient
import com.msb.purrytify.data.remote.model.SongResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSongRepository @Inject constructor() {
    
    private val songApiService = RetrofitClient.songApiService
    
    /**
     * Fetch a song from the API by ID
     * @return A SongResponse or null if there was an error
     */
    suspend fun fetchSongById(songId: String): SongResponse? = withContext(Dispatchers.IO) {
        try {
            val response = songApiService.getSongById(songId)
            if (response.isSuccessful) {
                return@withContext response.body()
            } else {
                Log.e("ApiSongRepository", "Error fetching song: ${response.code()} - ${response.message()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("ApiSongRepository", "Exception fetching song: ${e.message}")
            return@withContext null
        }
    }
    
    /**
     * Convert a SongResponse to a local Song entity
     * This is useful when you need to play an API song using the same player infrastructure
     */
    suspend fun convertApiSongToLocalSong(songResponse: SongResponse, userId: Long): Song = withContext(Dispatchers.IO) {
        // In a real implementation, you would download the artwork and song file
        // and save them locally before creating a Song entity
        
        // For now, we'll create a Song entity with the API data
        // In a real app, you'd implement the file downloading/caching logic here
        val durationMs = convertDurationStringToMs(songResponse.duration)
        
        return@withContext Song(
            id = songResponse.id,
            title = songResponse.title,
            artist = songResponse.artist,
            filePath = songResponse.url, // In reality, you'd download and use a local path
            artworkPath = songResponse.artwork, // In reality, you'd download and use a local path
            duration = durationMs,
            isLiked = false,
            ownerId = userId,
            isFromApi = true
        )
    }
    
    /**
     * Convert duration string ("4:12") to milliseconds
     */
    private fun convertDurationStringToMs(duration: String): Long {
        val parts = duration.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toLongOrNull() ?: 0
            val seconds = parts[1].toLongOrNull() ?: 0
            return ((minutes * 60) + seconds) * 1000
        }
        return 0
    }
}