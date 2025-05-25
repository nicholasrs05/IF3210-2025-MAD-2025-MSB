package com.msb.purrytify.data.local.dao

import androidx.room.*
import com.msb.purrytify.data.local.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert
    suspend fun insert(song: Song): Long

    @Insert
    suspend fun insertAll(songs: List<Song>): List<Long>

    @Update
    suspend fun update(song: Song)

    @Update
    suspend fun updateAll(songs: List<Song>)

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

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long)

    @Query("SELECT COUNT(*) FROM songs WHERE ownerId = :ownerId")
    fun getSongCount(ownerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE isLiked = 1 AND ownerId = :ownerId")
    fun getLikedSongCount(ownerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE lastPlayedAt IS NOT NULL AND ownerId = :ownerId")
    fun getListenedSongCount(ownerId: Long): Flow<Int>

    @Query("SELECT * FROM songs WHERE ownerId = :ownerId ORDER BY addedAt DESC LIMIT 10")
    fun getNewSongs(ownerId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE onlineSongId IS NOT NULL AND ownerId = :ownerId ORDER BY addedAt DESC")
    fun getDownloadedSongs(ownerId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE onlineSongId = :onlineSongId AND ownerId = :ownerId LIMIT 1")
    suspend fun getSongByOnlineSongId(onlineSongId: Long, ownerId: Long): Song?

    // Optimized queries for recommendation system
    @Query("SELECT * FROM songs WHERE ownerId = :ownerId ORDER BY playCount DESC LIMIT :limit")
    suspend fun getTopPlayedSongs(ownerId: Long, limit: Int = 50): List<Song>

    @Query("SELECT * FROM songs WHERE ownerId = :ownerId AND playCount > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getRecentlyPlayedSongsSync(ownerId: Long, limit: Int = 20): List<Song>

    @Query("SELECT * FROM songs WHERE ownerId = :ownerId AND artistId = :artistId ORDER BY playCount DESC")
    suspend fun getSongsByArtist(ownerId: Long, artistId: Long): List<Song>

    // Batch update operations for better performance
    @Transaction
    suspend fun batchUpdatePlayCounts(songIds: List<Long>, timestamps: List<Long>) {
        for (i in songIds.indices) {
            incrementPlayCount(songIds[i])
            updateLastPlayedAt(songIds[i], timestamps[i])
        }
    }

    @Transaction
    suspend fun batchUpdateLikeStatus(songIds: List<Long>, likeStatuses: List<Boolean>) {
        for (i in songIds.indices) {
            updateLikeStatus(songIds[i], likeStatuses[i])
        }
    }

    @Query("""
        SELECT s1.id as song1Id, s2.id as song2Id, s1.artistId as artistId, s2.artistId as artistId2, s1.playCount as playCount, s2.playCount as playCount2
        FROM songs s1 
        CROSS JOIN songs s2 
        WHERE s1.ownerId = :ownerId AND s2.ownerId = :ownerId AND s1.id != s2.id
        AND (s1.playCount > 0 OR s2.playCount > 0)
        ORDER BY s1.playCount DESC, s2.playCount DESC
        LIMIT :limit
    """)
    suspend fun getSongPairsForSimilarity(ownerId: Long, limit: Int = 1000): List<SongPairData>

    data class SongPairData(
        val song1Id: Long,
        val song2Id: Long,
        val artistId: Long,
        val artistId2: Long,
        val playCount: Int,
        val playCount2: Int
    )
}