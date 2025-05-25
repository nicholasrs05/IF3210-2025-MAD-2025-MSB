package com.msb.purrytify.data.ml.tfidf

import kotlin.math.ln
import kotlin.math.sqrt

class TfIdfCalculator {

    fun calculateTfIdf(documents: List<String>): List<Map<String, Double>> {
        if (documents.isEmpty()) return emptyList()
        
        // Tokenize all documents
        val tokenizedDocs = documents.map { tokenize(it) }
        
        // Calculate document frequency for each term
        val documentFrequency = calculateDocumentFrequency(tokenizedDocs)
        
        // Calculate TF-IDF for each document
        return tokenizedDocs.map { tokens ->
            calculateTfIdfForDocument(tokens, documentFrequency, documents.size)
        }
    }

    fun cosineSimilarity(vector1: Map<String, Double>, vector2: Map<String, Double>): Double {
        if (vector1.isEmpty() || vector2.isEmpty()) return 0.0
        
        val allTerms = vector1.keys + vector2.keys
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (term in allTerms) {
            val value1 = vector1[term] ?: 0.0
            val value2 = vector2[term] ?: 0.0
            
            dotProduct += value1 * value2
            norm1 += value1 * value1
            norm2 += value2 * value2
        }
        
        return if (norm1 == 0.0 || norm2 == 0.0) {
            0.0
        } else {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it.length > 2 }
    }
    

    private fun calculateDocumentFrequency(tokenizedDocs: List<List<String>>): Map<String, Int> {
        val documentFrequency = mutableMapOf<String, Int>()
        
        for (tokens in tokenizedDocs) {
            tokens.toSet().forEach { term ->
                documentFrequency[term] = (documentFrequency[term] ?: 0) + 1
            }
        }
        
        return documentFrequency
    }
    

    private fun calculateTfIdfForDocument(
        tokens: List<String>,
        documentFrequency: Map<String, Int>,
        totalDocuments: Int
    ): Map<String, Double> {
        val termFrequency = mutableMapOf<String, Int>()
        for (token in tokens) {
            termFrequency[token] = (termFrequency[token] ?: 0) + 1
        }
        val tfidfVector = mutableMapOf<String, Double>()
        val maxFrequency = termFrequency.values.maxOrNull() ?: 1
        
        for ((term, frequency) in termFrequency) {
            val tf = frequency.toDouble() / maxFrequency
            val idf = ln(totalDocuments.toDouble() / (documentFrequency[term] ?: 1).toDouble())
            tfidfVector[term] = tf * idf
        }
        return tfidfVector
    }
}