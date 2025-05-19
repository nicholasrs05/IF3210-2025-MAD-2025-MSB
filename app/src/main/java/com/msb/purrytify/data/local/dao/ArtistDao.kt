package com.msb.purrytify.data.local.dao

import androidx.room.*
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist): Long

    @Update
    suspend fun updateArtist(artist: Artist)

    @Delete
    suspend fun deleteArtist(artist: Artist)

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: Long): Artist?

    @Query("SELECT * FROM artists WHERE LOWER(name) = LOWER(:name)")
    suspend fun getArtistByName(name: String): Artist?

    @Query("SELECT * FROM artists")
    fun getAllArtists(): Flow<List<Artist>>

    @Query("DELETE FROM artists WHERE id = :id")
    suspend fun deleteArtistById(id: Long)
} 