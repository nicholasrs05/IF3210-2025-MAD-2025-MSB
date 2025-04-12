package com.msb.purrytify.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequest
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit
import com.msb.purrytify.data.api.ApiService
import com.msb.purrytify.data.storage.DataStoreManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@HiltWorker
class RefreshTokenService @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val currentJwt = dataStoreManager.authTokenFlow.first()

        if (currentJwt.isNullOrBlank()) {
            return@withContext Result.success()
        }

        try {
            val verificationResponse = apiService.verifyToken()
            if (verificationResponse.isSuccessful) {
                // Create and enqueue a new work request after successful verification
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val newRefreshTokenRequest = OneTimeWorkRequest.Builder(RefreshTokenService::class.java)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.MINUTES) // Reschedule after 5 minutes
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(newRefreshTokenRequest)
                return@withContext Result.success()
            } else {
                return@withContext Result.failure()
            }
        } catch (e: HttpException) {
            return@withContext Result.failure()
        } catch (e: Exception) {
            return@withContext Result.failure()
        }
    }
}
