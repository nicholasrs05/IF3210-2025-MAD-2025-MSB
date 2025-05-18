package com.msb.purrytify.qr

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * QR Scanner implementation using CameraX and ML Kit
 */
class QRScanner(private val context: Context) {
    private lateinit var cameraExecutor: ExecutorService
    private var barcodeScanner: BarcodeScanner? = null
    
    /**
     * Initialize the scanner
     */
    fun initialize() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    
    /**
     * Release scanner resources
     */
    fun shutdown() {
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        barcodeScanner?.close()
    }
    
    /**
     * Bind camera use cases to the preview view
     * 
     * @param previewView The camera preview view
     * @param lifecycleOwner The lifecycle owner to bind to
     * @param onQRCodeDetected Callback for detected QR codes
     */
    suspend fun startScanning(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onQRCodeDetected: (String) -> Unit
    ) {
        val cameraProvider = getCameraProvider()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    // Process image for QR codes
                    processImageForQR(imageProxy, onQRCodeDetected)
                }
            }
        
        try {
            // Unbind any previous use cases
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("QRScanner", "Use case binding failed", e)
            Toast.makeText(context, "Could not initialize camera", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Process image for QR codes
     */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageForQR(
        imageProxy: androidx.camera.core.ImageProxy,
        onQRCodeDetected: (String) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            barcodeScanner?.process(image)
                ?.addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && isValidPurrytifyQRCode(rawValue)) {
                            onQRCodeDetected(extractSongId(rawValue))
                        }
                    }
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    /**
     * Check if a scanned value is a valid Purrytify QR code
     */
    private fun isValidPurrytifyQRCode(rawValue: String): Boolean {
        return try {
            val uri = Uri.parse(rawValue)
            uri.scheme == "purrytify" && uri.host == "song" && uri.lastPathSegment != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract song ID from QR code URI
     */
    private fun extractSongId(rawValue: String): String {
        return Uri.parse(rawValue).lastPathSegment ?: ""
    }
    
    /**
     * Get the camera provider asynchronously
     */
    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(context))
    }
}

/**
 * Composable that displays a QR code scanner using CameraX
 * 
 * @param onQRCodeDetected Callback for when a QR code is detected
 */
@Composable
fun QRScannerScreen(
    onQRCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { QRScanner(context) }
    var isScannerReady by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Initialize and shutdown scanner with lifecycle
    DisposableEffect(Unit) {
        scanner.initialize()
        onDispose {
            scanner.shutdown()
        }
    }

    // Move LaunchedEffect outside AndroidView
    LaunchedEffect(previewView) {
        previewView?.let {
            scanner.startScanning(it, lifecycleOwner) { songId ->
                onQRCodeDetected(songId)
            }
            isScannerReady = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isScannerReady) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
