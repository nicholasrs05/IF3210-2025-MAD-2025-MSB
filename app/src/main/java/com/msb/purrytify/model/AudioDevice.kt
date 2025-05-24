package com.msb.purrytify.model

enum class AudioDeviceType {
    PHONE_SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH_DEVICE,
    USB_HEADSET
}

data class AudioDevice(
    val id: Int,
    val name: String,
    val type: AudioDeviceType,
    val isActive: Boolean = false
)

