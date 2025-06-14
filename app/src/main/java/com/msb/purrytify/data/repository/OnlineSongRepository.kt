package com.msb.purrytify.data.repository

import android.util.Log
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.Resource
import com.msb.purrytify.data.model.SongResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineSongRepository @Inject constructor(
    private val apiService: ApiService
) {
    fun getGlobalTopSongs(): Flow<Resource<List<SongResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getGlobalTopSongs()
            if (response.isSuccessful) {
                response.body()?.let { songs ->
                    emit(Resource.Success(songs))
                } ?: emit(Resource.Error("Empty response body"))
            } else {
                emit(Resource.Error("Error \\${response.code()}: \\${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("OnlineSongRepository", "Error fetching global top songs: \\${e.message}")
            emit(Resource.Error("Network error: \\${e.message}"))
        }
    }

    fun getCountryTopSongs(countryCode: String): Flow<Resource<List<SongResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getCountryTopSongs(countryCode)
            if (response.isSuccessful) {
                response.body()?.let { songs ->
                    emit(Resource.Success(songs))
                } ?: emit(Resource.Error("Empty response body"))
            } else {
                emit(Resource.Error("Error \\${response.code()}: \\${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Network error: \\${e.message}"))
        }
    }

    fun getSongById(songId: String): Flow<Resource<SongResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSongById(songId)
            Log.d("OnlineSongRepository", "Response on get song by id : \\${response.body()}")
            if (response.isSuccessful) {
                response.body()?.let { song ->
                    emit(Resource.Success(song))
                } ?: emit(Resource.Error("Song not found"))
            } else {
                emit(Resource.Error("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("OnlineSongRepository", "Error fetching song by ID: ${e.message}")
            emit(Resource.Error("Network error: ${e.message}"))
        }
    }

    fun convertToPlayableSongs(songResponses: List<SongResponse>): List<Song> {
        return songResponses.map { songResponse ->
            convertSingleSongResponse(songResponse)
        }
    }

    fun convertSingleSongResponse(songResponse: SongResponse): Song {
        return Song(
            id = songResponse.id,
            title = songResponse.title,
            artistName = songResponse.artist,
            filePath = songResponse.url,
            artworkPath = songResponse.artwork,
            duration = convertDurationStringToMs(songResponse.duration),
            isLiked = false,
            ownerId = -1,
            artistId = -1,
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