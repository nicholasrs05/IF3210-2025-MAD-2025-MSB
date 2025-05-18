package com.msb.purrytify.qr

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.R
import com.msb.purrytify.qr.QRScanner
import com.msb.purrytify.qr.QRScannerScreen as RealQRScannerScreen
import com.msb.purrytify.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

/**
 * A screen that shows a QR code scanner or simulation
 * 
 * @param navigateUp Callback to navigate back
 * @param onQRCodeScanned Callback when a QR code is scanned
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernQRScannerScreen(
    navigateUp: () -> Unit,
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val playerViewModel: PlayerViewModel = hiltViewModel()
    
    // Example song IDs for simulation
    val sampleSongIds = listOf("1", "2", "3", "4", "5")
    
    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Generate a sample QR code for display
    val sampleQrBitmap = remember {
        val songId = sampleSongIds.random()
        QRGenerator.generateQRCodeWithInfo(
            songId = songId,
            title = "Sample Song",
            artist = "Sample Artist",
            qrSize = 300
        )
    }
    
    // Permission launcher
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
    
    // Request camera permission if needed
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (hasCameraPermission) {
            // Use the real camera-based QR scanner
            Box(modifier = Modifier.padding(paddingValues)) {
                RealQRScannerScreen(
                    onQRCodeDetected = { songId ->
                        scope.launch {
                            snackbarHostState.showSnackbar("QR code scanned successfully!")
                        }
                        onQRCodeScanned(songId)
                    }
                )
                
                // Add an overlay with instructions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Position the QR code within the frame to scan",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    text = "To scan QR codes, we need permission to use your camera. This is only used while you're in the scanner screen.",
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

/**
 * A view that simulates a camera feed with a QR code
 */
@Composable
fun CameraSimulationView(
    sampleQrBitmap: android.graphics.Bitmap,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera simulation background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color(0xFF333333))
        ) {
            // QR code preview
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    bitmap = sampleQrBitmap.asImageBitmap(),
                    contentDescription = "Sample QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Position a QR code in the frame to scan",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onScan,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Simulate Scan")
                }
            }
        }
        
        // Scanner overlay frame
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                .background(Color.Transparent)
                .padding(16.dp)
        )
    }
}
