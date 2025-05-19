package com.msb.purrytify.data.repository

import com.msb.purrytify.data.local.dao.ArtistDao
import com.msb.purrytify.data.local.entity.Artist
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(
    private val artistDao: ArtistDao
) {
    suspend fun insertArtist(artist: Artist): Long {
        return artistDao.insertArtist(artist)
    }

    suspend fun updateArtist(artist: Artist) {
        artistDao.updateArtist(artist)
    }

    suspend fun deleteArtist(artist: Artist) {
        artistDao.deleteArtist(artist)
    }

    suspend fun getArtistById(id: Long): Artist? {
        return artistDao.getArtistById(id)
    }

    suspend fun getArtistByName(name: String): Artist? {
        return artistDao.getArtistByName(name)
    }

    fun getAllArtists(): Flow<List<Artist>> {
        return artistDao.getAllArtists()
    }

    suspend fun deleteArtistById(id: Long) {
        artistDao.deleteArtistById(id)
    }
} 