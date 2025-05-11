package com.msb.purrytify.data.repository

import android.media.MediaMetadataRetriever
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.local.entity.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(private val songDao: SongDao) {
    suspend fun insert(song: Song): Long {
        return songDao.insert(song)
    }

    suspend fun update(song: Song) {
        songDao.update(song)
    }

    suspend fun delete(song: Song) {
        songDao.delete(song)
    }

    suspend fun getSongById(songId: Long): Song? {
        return songDao.getSongById(songId)
    }

    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean) {
        songDao.updateLikeStatus(songId, isLiked)
    }

    suspend fun updateLastPlayedAt(songId: Long) {
        songDao.updateLastPlayedAt(songId, System.currentTimeMillis())
    }

    fun fetchAllSongs(userId: Long): Flow<List<Song>> {
        return songDao.getAllSongs(userId)
    }

    fun fetchLikedSongs(userId: Long): Flow<List<Song>> {
        return songDao.getLikedSongs(userId)
    }

    fun fetchRecentlyPlayedSongs(userId: Long): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs(userId)
    }

    fun fetchNewSongs(userId: Long): Flow<List<Song>> {
        return songDao.getNewSongs(userId)
    }

    fun getSongCount(userId: Long): Flow<Int> {
        return songDao.getSongCount(userId)
    }

    fun getLikedSongCount(userId: Long): Flow<Int> {
        return songDao.getLikedSongCount(userId)
    }

    fun getListenedSongCount(userId: Long): Flow<Int> {
        return songDao.getListenedSongCount(userId)
    }

    companion object {
        fun extractMetadata(audioFilePath: String): Pair<String?, String?> {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(audioFilePath)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                return Pair(title, artist)
            } catch (e: Exception) {
                e.printStackTrace()
                return Pair(null, null)
            } finally {
                retriever.release()
            }
        }

        fun getDuration(audioFilePath: String): Long {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(audioFilePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                return durationStr?.toLongOrNull() ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
                return 0
            } finally {
                retriever.release()
            }
        }
    }
}