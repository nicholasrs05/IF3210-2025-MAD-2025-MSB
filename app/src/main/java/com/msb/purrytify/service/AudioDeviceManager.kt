package com.msb.purrytify.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.msb.purrytify.model.AudioDevice
import com.msb.purrytify.model.AudioDeviceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow<AudioDevice?>(null)
    val currentDevice = _currentDevice.asStateFlow()
    
    private var manuallySelectedDevice: AudioDevice? = null

    private val audioDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG,
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d("AudioDeviceManager", "Audio device state changed: ${intent.action}")
                    updateAvailableDevices()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        context.registerReceiver(audioDeviceReceiver, filter)
        
        updateAvailableDevices()
    }

    fun updateAvailableDevices() {
        val devices = mutableListOf<AudioDevice>()
        val bluetoothDevices = mutableMapOf<String, AudioDevice>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val audioOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            audioOutputDevices.forEach { deviceInfo ->
                val deviceType = when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_DEVICE
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceType.BLUETOOTH_DEVICE
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.PHONE_SPEAKER
                    AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_HEADSET
                    else -> null
                }

                if (deviceType != null) {
                    val deviceName = deviceInfo.productName?.toString() 
                        ?: deviceInfo.type.toFriendlyName()
                    
                    val device = AudioDevice(
                        id = deviceInfo.id,
                        name = deviceName,
                        type = deviceType,
                        isActive = false
                    )
                    
                    if (deviceType == AudioDeviceType.BLUETOOTH_DEVICE) {
                        val existingDevice = bluetoothDevices[deviceName]
                        if (existingDevice == null || deviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                            bluetoothDevices[deviceName] = device
                        }
                    } else {
                        devices.add(device)
                    }
                }
            }
            
            devices.addAll(bluetoothDevices.values)
            
        } else {
            if (audioManager.isWiredHeadsetOn) {
                devices.add(AudioDevice(
                    id = 1, 
                    name = "Wired Headset", 
                    type = AudioDeviceType.WIRED_HEADSET, 
                    isActive = !audioManager.isSpeakerphoneOn
                ))
            }
            devices.add(AudioDevice(
                id = 2, 
                name = "Phone Speaker", 
                type = AudioDeviceType.PHONE_SPEAKER, 
                isActive = audioManager.isSpeakerphoneOn
            ))
            
            if (audioManager.isBluetoothScoOn) {
                devices.add(AudioDevice(
                    id = 3,
                    name = "Bluetooth Device",
                    type = AudioDeviceType.BLUETOOTH_DEVICE,
                    isActive = !audioManager.isSpeakerphoneOn && !audioManager.isWiredHeadsetOn
                ))
            }
        }

        _availableDevices.value = devices.distinctBy { "${it.name}_${it.type}" }
        
        determineCurrentDevice()
    }
    
    private fun determineCurrentDevice() {
        val availableDevices = _availableDevices.value
        
        val manualDevice = manuallySelectedDevice
        if (manualDevice != null && availableDevices.any { it.id == manualDevice.id }) {
            _availableDevices.value = availableDevices.map { device ->
                device.copy(isActive = device.id == manualDevice.id)
            }
            _currentDevice.value = availableDevices.find { it.id == manualDevice.id }
            return
        }
        
        val currentDevice = when {
            audioManager.isSpeakerphoneOn -> {
                availableDevices.find { it.type == AudioDeviceType.PHONE_SPEAKER }
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S && audioManager.isWiredHeadsetOn && !audioManager.isSpeakerphoneOn -> {
                availableDevices.find { it.type == AudioDeviceType.WIRED_HEADSET }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val bluetoothDevice = availableDevices.find { it.type == AudioDeviceType.BLUETOOTH_DEVICE }
                val wiredDevice = availableDevices.find { it.type == AudioDeviceType.WIRED_HEADSET }
                val speakerDevice = availableDevices.find { it.type == AudioDeviceType.PHONE_SPEAKER }
                
                wiredDevice ?: bluetoothDevice ?: speakerDevice
            }
            else -> availableDevices.find { it.type == AudioDeviceType.PHONE_SPEAKER }
        }
        
        _availableDevices.value = availableDevices.map { device ->
            device.copy(isActive = device.id == currentDevice?.id)
        }
        
        _currentDevice.value = currentDevice
    }

    fun handleAudioSelectionRequest(device: AudioDevice) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("AudioDeviceManager", "MODIFY_AUDIO_SETTINGS permission not granted.")
            return
        }

        Log.d("AudioDeviceManager", "Handling selection request for: ${device.name}, Type: ${device.type}")
        
        manuallySelectedDevice = device

        if (device.type != AudioDeviceType.BLUETOOTH_DEVICE) {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                Log.d("AudioDeviceManager", "SCO turned OFF (non-BT or A2DP preferred selection)")
            }
        } else { // Bluetooth device selected
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                Log.d("AudioDeviceManager", "SCO turned OFF (A2DP preferred for BT selection)")
            }
        }
    }

    private fun Int.toFriendlyName(): String {
        return when (this) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Device"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headset"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Unknown Device"
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(audioDeviceReceiver)
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error unregistering receiver: ${e.message}")
        }
    }
}