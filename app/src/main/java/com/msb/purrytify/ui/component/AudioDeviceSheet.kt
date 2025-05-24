package com.msb.purrytify.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.R
import com.msb.purrytify.model.AudioDevice
import com.msb.purrytify.model.AudioDeviceType
import com.msb.purrytify.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceSheet(
    onDismiss: () -> Unit,
    onDeviceSelected: (AudioDevice) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentDevice by viewModel.currentAudioDevice
    val availableDevices by viewModel.availableAudioDevices

    LaunchedEffect(Unit) {
        viewModel.refreshAudioDevices()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF282828),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Audio Output",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableDevices) { device ->
                    AudioDeviceItem(
                        device = device,
                        isSelected = device.id == currentDevice?.id,
                        onClick = {
                            onDeviceSelected(device)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AudioDeviceItem(
    device: AudioDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF1DB954) else Color(0xFF404040),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = when (device.type) {
                        AudioDeviceType.BLUETOOTH_DEVICE,
                        AudioDeviceType.USB_HEADSET -> R.drawable.ic_bluetooth_speaker
                        AudioDeviceType.WIRED_HEADSET -> R.drawable.ic_headset
                        else -> R.drawable.ic_speaker
                    }
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}