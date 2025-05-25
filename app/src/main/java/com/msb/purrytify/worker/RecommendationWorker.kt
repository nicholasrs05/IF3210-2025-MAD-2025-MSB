package com.msb.purrytify.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msb.purrytify.data.repository.RecommendationRepository
import com.msb.purrytify.model.ProfileModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class RecommendationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val recommendationRepository: RecommendationRepository,
    private val profileModel: ProfileModel
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "recommendation_update_work"
        private const val TAG = "RecommendationWorker"
    }
    
    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Starting recommendation update work")
        
        return try {
            val userId = profileModel.currentProfile.value.id
            
            if (userId != -1L) {
                Log.d(TAG, "Processing recommendations for user: $userId")
                recommendationRepository.clearCache()
                recommendationRepository.precomputeRecommendations(userId)
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Recommendation update completed successfully in ${duration}ms")
                
                Result.success()
            } else {
                Log.w(TAG, "No user logged in, skipping recommendation update")
                Result.success()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Recommendation update failed after ${duration}ms", e)
            
            if (runAttemptCount < 3) {
                Log.d(TAG, "Retrying recommendation update (attempt ${runAttemptCount + 1})")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached, failing recommendation update")
                Result.failure()
            }
        }
    }
}