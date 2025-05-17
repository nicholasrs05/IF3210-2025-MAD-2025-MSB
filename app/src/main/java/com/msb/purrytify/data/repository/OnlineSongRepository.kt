package com.msb.purrytify.data.repository

import android.util.Log
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.Resource
import com.msb.purrytify.data.model.SongResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineSongRepository @Inject constructor(
    private val apiService: ApiService
) {
    // Cache data to prevent excessive API calls
    private val globalTopSongsCache = MutableStateFlow<List<SongResponse>>(emptyList())
    
    // Add timestamp to track when data was last fetched
    private var lastFetchTimestamp: Long = 0
    private val cacheValidityPeriod: Long = 30 * 60 * 1000 // 30 minutes in milliseconds

    // Add cache for country top songs
    private val countryTopSongsCache = mutableMapOf<String, Pair<Long, List<SongResponse>>>()
    private val countryCacheValidityPeriod: Long = 30 * 60 * 1000 // 30 minutes
    
    // Fetch global top songs with caching
    suspend fun getGlobalTopSongs(forceRefresh: Boolean = false): Flow<Resource<List<SongResponse>>> = flow {
        emit(Resource.Loading())
        
        val shouldFetch = forceRefresh || 
                         globalTopSongsCache.value.isEmpty() || 
                         (System.currentTimeMillis() - lastFetchTimestamp > cacheValidityPeriod)
        
        // Return cached data if it's still valid
        if (!shouldFetch) {
            emit(Resource.Success(globalTopSongsCache.value))
            return@flow
        }
        
        // Fetch fresh data from API
        try {
            val response = apiService.getGlobalTopSongs()
            if (response.isSuccessful) {
                response.body()?.let { songs ->
                    globalTopSongsCache.value = songs
                    lastFetchTimestamp = System.currentTimeMillis()
                    emit(Resource.Success(songs))
                } ?: emit(Resource.Error("Empty response body"))
            } else {
                emit(Resource.Error("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("OnlineSongRepository", "Error fetching global top songs: ${e.message}")
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    // Fetch country top songs with caching
    suspend fun getCountryTopSongs(countryCode: String, forceRefresh: Boolean = false): Flow<Resource<List<SongResponse>>> = flow {
        emit(Resource.Loading())
        val cacheEntry = countryTopSongsCache[countryCode]
        val shouldFetch = forceRefresh ||
            cacheEntry == null ||
            (System.currentTimeMillis() - cacheEntry.first > countryCacheValidityPeriod)
        if (!shouldFetch) {
            emit(Resource.Success(cacheEntry.second))
            return@flow
        }
        try {
            val response = apiService.getCountryTopSongs(countryCode)
            if (response.isSuccessful) {
                response.body()?.let { songs ->
                    countryTopSongsCache[countryCode] = System.currentTimeMillis() to songs
                    emit(Resource.Success(songs))
                } ?: emit(Resource.Error("Empty response body"))
            } else {
                emit(Resource.Error("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    // Convert to playable songs (without storing in database)
    fun convertToPlayableSongs(songResponses: List<SongResponse>): List<Song> {
        return songResponses.map { songResponse ->
            Song(
                id = songResponse.id,
                title = songResponse.title,
                artist = songResponse.artist,
                filePath = songResponse.url,
                artworkPath = songResponse.artwork,
                duration = convertDurationStringToMs(songResponse.duration),
                isLiked = false,
                ownerId = -1, // Use a special value for online songs
                isFromApi = true
            )
        }
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