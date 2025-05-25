package com.msb.purrytify.data.repository

import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.ml.engine.RecommendationEngine
import com.msb.purrytify.data.ml.model.RecommendationConfig
import com.msb.purrytify.data.ml.model.RecommendationScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RecommendationRepository @Inject constructor(
    private val songDao: SongDao
) {
    private val recommendationEngine = RecommendationEngine()
    
    companion object {
        private const val TAG = "RecommendationRepository"
        private const val DEFAULT_RECOMMENDATION_COUNT = 20
        private const val MAX_SONGS_FOR_PROCESSING = 1000
    }

    suspend fun getRecommendations(
        ownerId: Long,
        currentSongId: Long? = null,
        topN: Int = DEFAULT_RECOMMENDATION_COUNT
    ): List<RecommendationScore> = withContext(Dispatchers.Default) {
        try {
            val allSongs = songDao.getAllSongs(ownerId).first()
            val recentlyPlayed = songDao.getRecentlyPlayedSongsSync(ownerId, 20)
            val songsToProcess = if (allSongs.size > MAX_SONGS_FOR_PROCESSING) {
                Log.w(TAG, "Large song collection (${allSongs.size}), limiting to top played songs")
                songDao.getTopPlayedSongs(ownerId, MAX_SONGS_FOR_PROCESSING)
            } else {
                allSongs
            }
            recommendationEngine.getRecommendations(
                songs = songsToProcess,
                recentlyPlayed = recentlyPlayed,
                currentSongId = currentSongId,
                topN = topN
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating recommendations", e)
            emptyList()
        }
    }

    fun getRecommendationsFlow(ownerId: Long, topN: Int = DEFAULT_RECOMMENDATION_COUNT): Flow<List<RecommendationScore>> {
        return combine(
            songDao.getAllSongs(ownerId),
            songDao.getRecentlyPlayedSongs(ownerId)
        ) { allSongs, recentlyPlayed ->
            try {
                // Limit songs for processing to maintain performance
                val songsToProcess = if (allSongs.size > MAX_SONGS_FOR_PROCESSING) {
                    allSongs.sortedByDescending { it.playCount }.take(MAX_SONGS_FOR_PROCESSING)
                } else {
                    allSongs
                }
                
                recommendationEngine.getRecommendations(
                    songs = songsToProcess,
                    recentlyPlayed = recentlyPlayed,
                    topN = topN
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in recommendations flow", e)
                emptyList()
            }
        }.flowOn(Dispatchers.Default)
    }


    suspend fun getTrendingSongs(
        ownerId: Long,
        topN: Int = DEFAULT_RECOMMENDATION_COUNT
    ): List<RecommendationScore> = withContext(Dispatchers.Default) {
        try {
            val topPlayedSongs = songDao.getTopPlayedSongs(ownerId, MAX_SONGS_FOR_PROCESSING)
            val recentlyPlayed = songDao.getRecentlyPlayedSongsSync(ownerId, 50)
            
            val trendingConfig = RecommendationConfig(
                popularityWeight = 0.5,
                recencyWeight = 0.4,
                contentWeight = 0.05,
                collaborativeWeight = 0.05
            )
            
            val engine = RecommendationEngine(trendingConfig)
            engine.getRecommendations(
                songs = topPlayedSongs,
                recentlyPlayed = recentlyPlayed,
                topN = topN
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating trending songs", e)
            emptyList()
        }
    }

    suspend fun precomputeRecommendations(ownerId: Long) = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting precomputation for user $ownerId")
            getRecommendations(ownerId)
            getTrendingSongs(ownerId)
            Log.d(TAG, "Precomputation completed for user $ownerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error during precomputation", e)
        }
    }

    fun clearCache() {
        try {
            recommendationEngine.clearCache()
            Log.d(TAG, "Recommendation cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
}