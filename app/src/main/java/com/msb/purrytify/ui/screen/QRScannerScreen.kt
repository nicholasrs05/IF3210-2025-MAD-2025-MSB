package com.msb.purrytify.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.qr.QRScannerScreen as RealQRScannerScreen
import com.msb.purrytify.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A screen that shows a QR code scanner that creates deep link intents
 * When QR code is scanned, it creates a deep link intent to handle the song
 * 
 * @param navigateUp Callback to navigate back
 * @param onQRCodeScanned Callback when a QR code is scanned and deep link is executed
 * @param playerViewModel The PlayerViewModel to show current song state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernQRScannerScreen(
    navigateUp: () -> Unit,
    onQRCodeScanned: (String) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State management
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var isProcessingQR by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    
    val currentSong by playerViewModel.currentSong
    val isPlaying by playerViewModel.isPlaying
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                scope.launch {
                    snackbarHostState.showSnackbar("Camera permission is required to scan QR codes")
                }
            }
        }
    )
    
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    val handleQRCodeScanned = { songId: String ->
        if (!isProcessingQR) {
            isProcessingQR = true
            processingMessage = "Processing QR code..."
            
            scope.launch {
                try {
                    val songIdLong = songId.toLongOrNull()
                    if (songIdLong != null) {
                        processingMessage = "Creating deep link for song ID: $songId"
                        val deepLinkUri = Uri.parse("purrytify://song/$songId")
                        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                            setPackage(context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                            delay(500)
                            onQRCodeScanned(songId)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error executing deep link: ${e.message}")
                        }
                    } else {
                        snackbarHostState.showSnackbar("Invalid QR code format: $songId")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error processing QR code: ${e.message}")
                } finally {
                    isProcessingQR = false
                    processingMessage = ""
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (hasCameraPermission) {
            Box(modifier = Modifier.padding(paddingValues)) {
                // QR Scanner Camera View
                RealQRScannerScreen(
                    onQRCodeDetected = handleQRCodeScanned
                )
                
                // Instruction overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isProcessingQR) processingMessage else "Position the QR code within the frame to scan",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Processing overlay
                if (isProcessingQR) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = processingMessage,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Current playing song indicator (if any)
                currentSong?.let { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isPlaying) "♪ Now Playing" else "⏸ Paused",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${song.title} - ${song.artistName}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else {
            // Camera permission request UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Camera",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "To scan QR codes and play songs, we need permission to use your camera. This is only used while you're in the scanner screen.",
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant Permission", modifier = Modifier.padding(vertical = 8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = navigateUp,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Go Back", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}