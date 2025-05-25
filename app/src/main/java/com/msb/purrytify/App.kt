package com.msb.purrytify

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.media.AudioManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.msb.purrytify.service.RefreshTokenService
import com.msb.purrytify.receiver.AudioDeviceReceiver
import com.msb.purrytify.worker.RecommendationWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class Purritify : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var audioDeviceReceiver: AudioDeviceReceiver

    override fun onCreate() {
        super.onCreate()
        scheduleRefreshTokenWork()
        scheduleRecommendationWork()

        // Register audio device receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        registerReceiver(audioDeviceReceiver, filter)
    }

    private fun scheduleRefreshTokenWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val refreshTokenRequest: WorkRequest = OneTimeWorkRequest.Builder(RefreshTokenService::class.java)
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(refreshTokenRequest)
    }
    
    private fun scheduleRecommendationWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val recommendationRequest = PeriodicWorkRequestBuilder<RecommendationWorker>(
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            RecommendationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            recommendationRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(audioDeviceReceiver)
    }
}
