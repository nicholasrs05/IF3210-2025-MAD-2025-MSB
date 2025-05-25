package com.msb.purrytify.data.ml.knn

import com.msb.purrytify.data.local.entity.Song
import kotlin.math.sqrt

class KNNCalculator {

    fun calculateItemSimilarity(
        songs: List<Song>,
        recentlyPlayed: List<Song>,
        k: Int = 10
    ): Map<Long, List<Pair<Long, Double>>> {
        if (songs.size < 2) return emptyMap()
        
        val songIds = songs.map { it.id }
        val recentlyPlayedIds = recentlyPlayed.map { it.id }.toSet()
        val similarities = mutableMapOf<Long, MutableList<Pair<Long, Double>>>()
        
        // Calculate pairwise similarities
        for (i in songIds.indices) {
            val songId1 = songIds[i]
            val song1 = songs[i]
            similarities[songId1] = mutableListOf()
            
            for (j in songIds.indices) {
                if (i == j) continue
                
                val songId2 = songIds[j]
                val song2 = songs[j]
                
                val similarity = calculateSongSimilarity(song1, song2, recentlyPlayedIds)
                if (similarity > 0.0) {
                    similarities[songId1]?.add(Pair(songId2, similarity))
                }
            }
            
            similarities[songId1] = similarities[songId1]
                ?.sortedByDescending { it.second }
                ?.take(k)
                ?.toMutableList() ?: mutableListOf()
        }
        
        return similarities
    }

    private fun calculateSongSimilarity(
        song1: Song,
        song2: Song,
        recentlyPlayedIds: Set<Long>
    ): Double {
        var similarity = 0.0
        
        // Artist similarity (high weight)
        if (song1.artistId == song2.artistId) {
            similarity += 0.6
        }
        
        // Co-occurrence in recently played (collaborative signal)
        val song1Recent = song1.id in recentlyPlayedIds
        val song2Recent = song2.id in recentlyPlayedIds
        
        if (song1Recent && song2Recent) {
            similarity += 0.3
        }
        
        // Play count similarity (popularity similarity)
        val playCountSimilarity = calculatePlayCountSimilarity(song1.playCount, song2.playCount)
        similarity += 0.1 * playCountSimilarity
        
        return similarity.coerceIn(0.0, 1.0)
    }

    private fun calculatePlayCountSimilarity(count1: Int, count2: Int): Double {
        if (count1 == 0 && count2 == 0) return 1.0
        if (count1 == 0 || count2 == 0) return 0.0
        
        val magnitude1 = sqrt(count1.toDouble())
        val magnitude2 = sqrt(count2.toDouble())
        
        return (count1 * count2).toDouble() / (magnitude1 * magnitude2)
    }

    fun calculateCollaborativeScore(
        songId: Long,
        recentlyPlayedIds: List<Long>,
        similarityMatrix: Map<Long, List<Pair<Long, Double>>>
    ): Double {
        if (recentlyPlayedIds.isEmpty()) return 0.0
        
        var totalScore = 0.0
        var count = 0
        
        for (recentSongId in recentlyPlayedIds) {
            val similarities = similarityMatrix[recentSongId] ?: continue
            val similarity = similarities.find { it.first == songId }?.second ?: 0.0
            
            if (similarity > 0.0) {
                totalScore += similarity
                count++
            }
        }
        
        return if (count > 0) totalScore / count else 0.0
    }
} 