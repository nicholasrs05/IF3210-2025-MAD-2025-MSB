package com.msb.purrytify.data.ml.engine

import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.ml.model.RecommendationConfig
import com.msb.purrytify.data.ml.model.RecommendationScore
import com.msb.purrytify.data.ml.model.SongFeature
import com.msb.purrytify.data.ml.tfidf.TfIdfCalculator
import com.msb.purrytify.data.ml.knn.KNNCalculator
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min


class RecommendationEngine(
    private val config: RecommendationConfig = RecommendationConfig(),
    private val tfidfCalculator: TfIdfCalculator = TfIdfCalculator(),
    private val knnCalculator: KNNCalculator = KNNCalculator()
) {
    
    private var cachedSongFeatures: Map<Long, SongFeature> = emptyMap()
    private var cachedSimilarityMatrix: Map<Long, List<Pair<Long, Double>>> = emptyMap()
    private var lastFeatureUpdateTime: Long = 0
    private var lastSimilarityUpdateTime: Long = 0
    private val featureCacheExpiry = 3600000L // 1 hour in milliseconds
    private val similarityCacheExpiry = 7200000L // 2 hours in milliseconds

    fun getRecommendations(
        songs: List<Song>,
        recentlyPlayed: List<Song> = emptyList(),
        currentSongId: Long? = null,
        topN: Int = 20
    ): List<RecommendationScore> {
        if (songs.isEmpty()) return emptyList()
        updateFeaturesIfNeeded(songs)
        updateSimilarityMatrixIfNeeded(songs, recentlyPlayed)
        val scores = songs.map { song ->
            calculateRecommendationScore(song, songs, recentlyPlayed, currentSongId)
        }
        return scores
            .sortedByDescending { it.score }
            .take(topN)
    }

    private fun calculateRecommendationScore(
        song: Song,
        allSongs: List<Song>,
        recentlyPlayed: List<Song>,
        currentSongId: Long?
    ): RecommendationScore {
        // Popularity score (normalized play count)
        val popularityScore = calculatePopularityScore(song, allSongs)
        
        // Recency score (exponential decay based on last played time)
        val recencyScore = calculateRecencyScore(song)
        
        // Content-based score (TF-IDF similarity)
        val contentScore = if (currentSongId != null && currentSongId != song.id) {
            calculateContentScore(song.id, currentSongId)
        } else {
            0.0
        }
        
        // k-NN
        val collaborativeScore = calculateEnhancedCollaborativeScore(song, recentlyPlayed)
        
        val finalScore = config.popularityWeight * popularityScore +
                        config.recencyWeight * recencyScore +
                        config.contentWeight * contentScore +
                        config.collaborativeWeight * collaborativeScore
        
        return RecommendationScore(
            songId = song.id,
            score = finalScore,
            popularityScore = popularityScore,
            recencyScore = recencyScore,
            contentScore = contentScore,
            collaborativeScore = collaborativeScore
        )
    }

    private fun calculatePopularityScore(song: Song, allSongs: List<Song>): Double {
        val maxPlayCount = allSongs.maxByOrNull { it.playCount }?.playCount ?: 1
        return if (maxPlayCount > 0) {
            song.playCount.toDouble() / maxPlayCount
        } else {
            0.0
        }
    }
    

    private fun calculateRecencyScore(song: Song): Double {
        val lastPlayedAt = song.lastPlayedAt ?: return 0.0
        val nowMillis = System.currentTimeMillis()
        val ageHours = (nowMillis - lastPlayedAt) / 3_600_000.0
        
        // Exponential decay: score = e^(-γ * age)
        return exp(-config.recencyDecayFactor * ageHours)
    }

    private fun calculateContentScore(songId: Long, currentSongId: Long): Double {
        val songFeature = cachedSongFeatures[songId] ?: return 0.0
        val currentFeature = cachedSongFeatures[currentSongId] ?: return 0.0
        
        val tfidfSimilarity = tfidfCalculator.cosineSimilarity(
            songFeature.tfidfVector,
            currentFeature.tfidfVector
        )
        
        val artistBonus = if (songFeature.artistSimilarity > 0) 0.2 else 0.0
        
        return min(1.0, tfidfSimilarity + artistBonus)
    }

    private fun calculateEnhancedCollaborativeScore(song: Song, recentlyPlayed: List<Song>): Double {
        if (recentlyPlayed.isEmpty()) return 0.0
        
        val recentlyPlayedIds = recentlyPlayed.map { it.id }
        
        val knnScore = knnCalculator.calculateCollaborativeScore(
            song.id,
            recentlyPlayedIds,
            cachedSimilarityMatrix
        )
        
        val simpleCollaborativeScore = if (knnScore > 0.0) {
            knnScore
        } else {
            calculateSimpleCollaborativeScore(song, recentlyPlayed)
        }
        
        return simpleCollaborativeScore
    }

    private fun calculateSimpleCollaborativeScore(song: Song, recentlyPlayed: List<Song>): Double {
        val recentSongIds = recentlyPlayed.map { it.id }.toSet()
        
        return if (song.id in recentSongIds) {
            0.8
        } else if (song.artistId in recentlyPlayed.map { it.artistId }) {
            0.5
        } else {
            0.0
        }
    }

    private fun updateFeaturesIfNeeded(songs: List<Song>) {
        val now = System.currentTimeMillis()
        if (now - lastFeatureUpdateTime < featureCacheExpiry && cachedSongFeatures.isNotEmpty()) {
            return
        }
        
        val documents = songs.map { "${it.title} ${it.artistName}" }
        val tfidfVectors = tfidfCalculator.calculateTfIdf(documents)
        
        val features = songs.mapIndexed { index, song ->
            song.id to SongFeature(
                songId = song.id,
                tfidfVector = tfidfVectors.getOrNull(index) ?: emptyMap(),
                artistSimilarity = 1.0
            )
        }.toMap()
        
        cachedSongFeatures = features
        lastFeatureUpdateTime = now
    }

    private fun updateSimilarityMatrixIfNeeded(songs: List<Song>, recentlyPlayed: List<Song>) {
        val now = System.currentTimeMillis()
        if (now - lastSimilarityUpdateTime < similarityCacheExpiry && cachedSimilarityMatrix.isNotEmpty()) {
            return
        }
        
        // Calculate item-item similarity matrix using k-NN
        cachedSimilarityMatrix = knnCalculator.calculateItemSimilarity(songs, recentlyPlayed)
        lastSimilarityUpdateTime = now
    }

    fun clearCache() {
        cachedSongFeatures = emptyMap()
        cachedSimilarityMatrix = emptyMap()
        lastFeatureUpdateTime = 0
        lastSimilarityUpdateTime = 0
    }
}