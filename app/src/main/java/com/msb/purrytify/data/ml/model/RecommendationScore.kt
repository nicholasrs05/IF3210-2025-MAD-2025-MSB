package com.msb.purrytify.data.ml.model

data class RecommendationScore(
    val songId: Long,
    val score: Double,
    val popularityScore: Double = 0.0,
    val recencyScore: Double = 0.0,
    val contentScore: Double = 0.0,
    val collaborativeScore: Double = 0.0
)