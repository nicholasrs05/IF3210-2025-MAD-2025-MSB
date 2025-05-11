package com.msb.purrytify.data.remote

import com.msb.purrytify.data.remote.model.SongResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SongApiService {
    
    @GET("/api/song/{songId}")
    suspend fun getSongById(@Path("songId") songId: String): Response<SongResponse>
    
    // Other API methods can be added here
}