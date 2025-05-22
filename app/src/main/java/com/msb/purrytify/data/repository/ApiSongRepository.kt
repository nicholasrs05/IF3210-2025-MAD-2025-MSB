package com.msb.purrytify.data.repository

import android.util.Log
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.SongResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSongRepository @Inject constructor(
    private val apiService: ApiService,
    private val artistRepository: ArtistRepository
) {
    
    /**
     * Fetch a song from the API by ID
     * @return A SongResponse or null if there was an error
     */
    suspend fun fetchSongById(songId: String): SongResponse? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSongById(songId)
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
        // First, check if artist exists (case insensitive)
        val existingArtist = artistRepository.getArtistByName(songResponse.artist.lowercase())
        val artistId = if (existingArtist != null) {
            // If artist exists, update their image if the new song has an artwork
            if (songResponse.artwork.isNotEmpty()) {
                artistRepository.updateArtist(existingArtist.copy(imageUrl = songResponse.artwork))
            }
            existingArtist.id
        } else {
            // Create new artist if doesn't exist
            artistRepository.insertArtist(
                Artist(
                    name = songResponse.artist,
                    imageUrl = if (songResponse.artwork.isNotEmpty()) songResponse.artwork else null
                )
            )
        }
        
        // Create a Song entity with the API data
        val durationMs = convertDurationStringToMs(songResponse.duration)
        
        return@withContext Song(
            id = songResponse.id,
            title = songResponse.title,
            artistName = songResponse.artist,
            artistId = artistId,
            filePath = songResponse.url,
            artworkPath = songResponse.artwork,
            duration = durationMs,
            isLiked = false,
            ownerId = userId,
            isFromApi = true
        )
    }

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