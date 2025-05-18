package com.msb.purrytify.utils.qrcode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/**
 * QR Code Scanner composable that handles permissions and scanning
 * 
 * @param onQrCodeScanned Callback function when a QR code is scanned
 * @param onPermissionDenied Callback function when camera permission is denied
 */
@Composable
fun QRCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                onPermissionDenied()
            }
        }
    )
    
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    if (hasCameraPermission) {
        var scannerView: DecoratedBarcodeView? = null
        
        AndroidView(
            factory = { ctx ->
                DecoratedBarcodeView(ctx).apply {
                    scannerView = this
                    
                    // Configure scanner
                    barcodeView.decoderFactory = DefaultDecoderFactory()
                    initializeFromIntent(null)
                    setStatusText("")
                    
                    // Set callback for scan results
                    barcodeView.decodeContinuous(object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult) {
                            val text = result.text
                            if (text != null) {
                                // Check if it's a valid Purrytify QR code
                                if (text.startsWith("purrytify://song/")) {
                                    Log.d("QRCodeScanner", "Scanned QR code: $text")
                                    
                                    // Extract song ID from the deep link
                                    val songId = text.substringAfterLast('/')
                                    
                                    // Call the callback with the song ID
                                    onQrCodeScanned(songId)
                                    
                                    // Pause scanning after a successful scan
                                    barcodeView.pause()
                                }
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Handle lifecycle events to pause/resume scanner
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> scannerView?.resume()
                    Lifecycle.Event.ON_PAUSE -> scannerView?.pause()
                    else -> {}
                }
            }
            
            lifecycleOwner.lifecycle.addObserver(observer)
            
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                scannerView?.pause()
            }
        }
    }
}

/**
 * Helper function to check camera permission
 */
fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
