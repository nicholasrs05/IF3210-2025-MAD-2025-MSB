package com.msb.purrytify.data.ml.model


data class RecommendationConfig(
    val popularityWeight: Double = 0.3,
    val recencyWeight: Double = 0.2,
    val contentWeight: Double = 0.3,
    val collaborativeWeight: Double = 0.2,
    val recencyDecayFactor: Double = 0.1
)