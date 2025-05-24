package com.msb.purrytify.data.repository

import android.content.Context
import android.util.Log
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.model.SongResponse
import com.msb.purrytify.utils.FileDownloadUtil
import com.msb.purrytify.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineSongDownloadRepository @Inject constructor(
    private val context: Context,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository
) {
    suspend fun downloadSong(
        songResponse: SongResponse,
        userId: Long
    ): Song = withContext(Dispatchers.IO) {
        try {
            if (songRepository.isSongDownloaded(songResponse.id, userId)) {
                Log.w("OnlineSongDownloadRepo", "Song already downloaded: ${songResponse.title}")
                throw IllegalStateException("Song is already downloaded")
            }
            
            val songFileName = "${songResponse.id}_${FileUtils.sanitizeFileName(songResponse.title)}.mp3"
            val artworkFileName = "${songResponse.id}_${FileUtils.sanitizeFileName(songResponse.title)}.png"
            
            val localSongPath = FileDownloadUtil.downloadSongFile(
                context,
                songResponse.url,
                songFileName,
                "songs"
            )
            
            val localArtworkPath = FileDownloadUtil.downloadSongFile(
                context,
                songResponse.artwork,
                artworkFileName,
                "artworks"
            )
            
            val artistId = findOrCreateArtist(songResponse.artist, songResponse.artwork)
            
            val song = Song(
                title = songResponse.title,
                artistName = songResponse.artist,
                artistId = artistId,
                duration = convertDurationStringToMs(songResponse.duration),
                filePath = localSongPath,
                artworkPath = localArtworkPath,
                isLiked = false,
                addedAt = System.currentTimeMillis(),
                ownerId = userId,
                isFromApi = false,
                onlineSongId = songResponse.id
            )
            
            val songId = songRepository.insert(song)
            Log.d("OnlineSongDownloadRepo", "Song downloaded and saved: ${song.title}, ID: $songId")
            
            return@withContext song.copy(id = songId)
        } catch (e: Exception) {
            Log.e("OnlineSongDownloadRepo", "Error downloading song: ${e.message}", e)
            throw e
        }
    }
    

    private suspend fun findOrCreateArtist(artistName: String, artworkUrl: String): Long {
        val existingArtist = artistRepository.getArtistByName(artistName.lowercase())
        return if (existingArtist != null) {
            existingArtist.id
        } else {
            artistRepository.insertArtist(
                Artist(
                    name = artistName,
                    imageUrl = artworkUrl
                )
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