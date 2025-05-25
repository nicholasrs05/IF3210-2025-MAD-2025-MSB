package com.msb.purrytify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.msb.purrytify.viewmodel.AudioDeviceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AudioDeviceReceiver @Inject constructor(
    @Named("receiverViewModel") private val viewModel: AudioDeviceViewModel
) : BroadcastReceiver() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    companion object {
        private const val TAG = "AudioDeviceReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        try {
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG -> handleHeadsetPlug(intent)
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> handleBluetoothScoState(intent)
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> handleAudioBecomingNoisy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling audio device event: ${e.message}")
            handleError("Failed to handle audio device change: ${e.message}")
        }
    }

    private fun handleHeadsetPlug(intent: Intent) {
        val state = intent.getIntExtra("state", -1)
        val hasHeadset = state == 1
        
        scope.launch {
            try {
                if (hasHeadset) {
                    Log.d(TAG, "Wired headset connected")
                } else {
                    Log.d(TAG, "Wired headset disconnected")
                }
                viewModel.updateAvailableDevices()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling headset plug: ${e.message}")
                handleError("Failed to handle headset connection: ${e.message}")
            }
        }
    }

    private fun handleBluetoothScoState(intent: Intent) {
        val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
        
        scope.launch {
            try {
                when (state) {
                    AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                        Log.d(TAG, "Bluetooth SCO connected")
                        viewModel.updateAvailableDevices()
                    }
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                        Log.d(TAG, "Bluetooth SCO disconnected")
                        viewModel.updateAvailableDevices()
                    }
                    AudioManager.SCO_AUDIO_STATE_ERROR -> {
                        Log.e(TAG, "Bluetooth SCO connection error")
                        handleError("Bluetooth audio connection error")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Bluetooth SCO state: ${e.message}")
                handleError("Failed to handle Bluetooth connection: ${e.message}")
            }
        }
    }

    private fun handleAudioBecomingNoisy() {
        scope.launch {
            try {
                Log.d(TAG, "Audio becoming noisy - device unplugged")
                viewModel.updateAvailableDevices()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling audio becoming noisy: ${e.message}")
                handleError("Failed to handle device disconnection: ${e.message}")
            }
        }
    }

    private fun handleError(message: String) {
        Log.e(TAG, message)
        scope.launch {
            try {
                // Fallback to phone speaker
                viewModel.updateAvailableDevices()
            } catch (e: Exception) {
                Log.e(TAG, "Error during error handling fallback: ${e.message}")
            }
        }
    }
}