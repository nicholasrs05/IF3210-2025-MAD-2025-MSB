package com.msb.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.ml.model.RecommendationScore
import com.msb.purrytify.data.repository.RecommendationRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val songRepository: SongRepository,
    private val profileModel: ProfileModel
) : ViewModel() {
    
    companion object {
        private const val TAG = "RecommendationViewModel"
    }
    
    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadRecommendations()
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userId = profileModel.currentProfile.value.id
                if (userId != -1L) {
                    // Get recommendations as Flow for reactive updates
                    recommendationRepository.getRecommendationsFlow(userId)
                        .catch { e ->
                            Log.e(TAG, "Error loading recommendations", e)
                            _error.value = "Failed to load recommendations"
                            emit(emptyList())
                        }
                        .collect { recommendations ->
                            loadSongsFromRecommendations(recommendations) { songs ->
                                _recommendedSongs.value = songs
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadRecommendations", e)
                _error.value = "Failed to load recommendations"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTrendingSongs() {
        viewModelScope.launch {
            try {
                val userId = profileModel.currentProfile.value.id
                if (userId != -1L) {
                    val recommendations = recommendationRepository.getTrendingSongs(userId)
                    loadSongsFromRecommendations(recommendations) { songs ->
                        _trendingSongs.value = songs
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading trending songs", e)
                _error.value = "Failed to load trending songs"
            }
        }
    }

    private suspend fun loadSongsFromRecommendations(
        recommendations: List<RecommendationScore>,
        onResult: (List<Song>) -> Unit
    ) {
        try {
            val userId = profileModel.currentProfile.value.id
            if (userId != -1L) {
                songRepository.fetchAllSongs(userId).first().let { allSongs ->
                    val songMap = allSongs.associateBy { it.id }
                    val recommendedSongs = recommendations.mapNotNull { rec ->
                        songMap[rec.songId]
                    }
                    onResult(recommendedSongs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting recommendations to songs", e)
            onResult(emptyList())
        }
    }
}