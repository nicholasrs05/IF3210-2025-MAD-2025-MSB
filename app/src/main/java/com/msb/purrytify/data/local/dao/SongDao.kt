package com.msb.purrytify.data.local.dao

import androidx.room.*
import com.msb.purrytify.data.local.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert
    suspend fun insert(song: Song): Long

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("SELECT * FROM songs WHERE ownerId = :ownerId ORDER BY addedAt DESC")
    fun getAllSongs(ownerId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 AND ownerId = :ownerId ORDER BY addedAt DESC")
    fun getLikedSongs(ownerId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL AND ownerId = :ownerId ORDER BY lastPlayedAt DESC LIMIT 10")
    fun getRecentlyPlayedSongs(ownerId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): Song?

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE id = :songId")
    suspend fun updateLastPlayedAt(songId: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM songs WHERE ownerId = :ownerId")
    fun getSongCount(ownerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE isLiked = 1 AND ownerId = :ownerId")
    fun getLikedSongCount(ownerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE lastPlayedAt IS NOT NULL AND ownerId = :ownerId")
    fun getListenedSongCount(ownerId: Long): Flow<Int>

    @Query("SELECT * FROM songs WHERE ownerId = :ownerId ORDER BY addedAt DESC LIMIT 10")
    fun getNewSongs(ownerId: Long): Flow<List<Song>>
}