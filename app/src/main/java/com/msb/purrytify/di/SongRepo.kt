package com.msb.purrytify.di

import android.content.Context
import android.media.MediaMetadataRetriever
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.local.entity.Song
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val likedSongs: Flow<List<Song>> = songDao.getLikedSongs()
    val recentlyPlayedSongs: Flow<List<Song>> = songDao.getRecentlyPlayedSongs()
    val songCount: Flow<Int> = songDao.getSongCount()
    val likedSongCount: Flow<Int> = songDao.getLikedSongCount()
    val listenedSongCount: Flow<Int> = songDao.getListenedSongCount()

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

    companion object {
        fun extractMetadata(context: Context, audioFilePath: String): Pair<String?, String?> {
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