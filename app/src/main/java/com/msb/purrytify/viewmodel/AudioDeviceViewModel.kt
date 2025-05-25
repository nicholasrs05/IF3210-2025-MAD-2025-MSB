package com.msb.purrytify.viewmodel

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.ViewModel
import com.msb.purrytify.model.AudioDevice
import com.msb.purrytify.model.AudioDeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class AudioDeviceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentDevice = MutableStateFlow<AudioDevice?>(null)
    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())

    init {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        updateAvailableDevices()
    }

    fun updateAvailableDevices() {
        val devices = mutableListOf<AudioDevice>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val audioOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val communicationDevice = audioManager.communicationDevice

            audioOutputDevices.forEach { deviceInfo ->
                val deviceType = when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_DEVICE
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.PHONE_SPEAKER
                    AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_HEADSET
                    else -> null
                }

                if (deviceType != null) {
                    devices.add(AudioDevice(
                        id = deviceInfo.id,
                        name = deviceInfo.productName?.toString() ?: deviceInfo.type.toFriendlyName(),
                        type = deviceType,

                        isActive = communicationDevice != null && communicationDevice.id == deviceInfo.id
                    ))
                }
            }
        } else {
            val audioOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            audioOutputDevices.forEach { deviceInfo ->
                val deviceType = when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_DEVICE
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.PHONE_SPEAKER
                    AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_HEADSET
                    else -> null
                }

                if (deviceType != null) {
                    devices.add(AudioDevice(
                        id = deviceInfo.id,
                        name = deviceInfo.productName?.toString() ?: deviceInfo.type.toFriendlyName(),
                        type = deviceType,
                        isActive = deviceType == AudioDeviceType.PHONE_SPEAKER && audioManager.isSpeakerphoneOn
                    ))
                }
            }
        }


        _availableDevices.value = devices.distinctBy { it.id }

        val activeDevice = _availableDevices.value.find { it.isActive }

        _currentDevice.value = activeDevice
            ?: _availableDevices.value.find { it.type == AudioDeviceType.PHONE_SPEAKER }
                    ?: _availableDevices.value.firstOrNull()
    }

    private fun Int.toFriendlyName(): String {
        return when (this) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Device"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headset"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Unknown Device"
        }
    }
}