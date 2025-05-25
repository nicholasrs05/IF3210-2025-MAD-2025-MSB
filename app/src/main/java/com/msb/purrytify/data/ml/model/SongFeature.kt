package com.msb.purrytify.data.ml.model

data class SongFeature(
    val songId: Long,
    val tfidfVector: Map<String, Double> = emptyMap(), // TF-IDF features
    val artistSimilarity: Double = 0.0
)