package com.msb.purrytify.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.msb.purrytify.qr.QRScannerScreen as RealQRScannerScreen
import com.msb.purrytify.viewmodel.OnlineSongsViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.data.local.entity.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced QR Scanner Screen with smooth animations and better user experience
 * 
 * @param navigateUp Callback to navigate back
 * @param onQRCodeScanned Callback when a QR code is successfully processed
 * @param playerViewModel The PlayerViewModel to control music playback
 * @param onlineSongsViewModel The OnlineSongsViewModel to fetch songs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernQRScannerScreen(
    navigateUp: () -> Unit,
    onQRCodeScanned: (String) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onlineSongsViewModel: OnlineSongsViewModel = hiltViewModel()
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
    
    var scanState by remember { mutableStateOf(ScanState.Scanning) }
    var scannedSong by remember { mutableStateOf<Song?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
        if (scanState == ScanState.Scanning) {
            scanState = ScanState.Processing
            
            scope.launch {
                try {
                    val songIdLong = songId.toLongOrNull()
                    if (songIdLong != null) {
                        onlineSongsViewModel.fetchSongById(songId) { song ->
                            if (song != null) {
                                scannedSong = song
                                scanState = ScanState.Success
                                
                                // Auto-play the song after a brief delay
                                scope.launch {
                                    delay(1500) // Show success state for 1.5 seconds
                                    
                                    // Set up and play the song
                                    playerViewModel.setPlaylist(listOf(song), 0)
                                    playerViewModel.playSongFromQR(song)
                                    playerViewModel.setLargePlayerVisible(true)
                                    
                                    delay(500) // Brief delay before navigation
                                    onQRCodeScanned(songId)
                                }
                            } else {
                                errorMessage = "Song not found or unavailable"
                                scanState = ScanState.Error
                                
                                // Auto-retry after error
                                scope.launch {
                                    delay(3000)
                                    scanState = ScanState.Scanning
                                    errorMessage = null
                                }
                            }
                        }
                    } else {
                        errorMessage = "Invalid QR code format"
                        scanState = ScanState.Error
                        
                        scope.launch {
                            delay(3000)
                            scanState = ScanState.Scanning
                            errorMessage = null
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Error processing QR code: ${e.message}"
                    scanState = ScanState.Error
                    
                    scope.launch {
                        delay(3000)
                        scanState = ScanState.Scanning
                        errorMessage = null
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (scanState) {
                            ScanState.Scanning -> "Scan QR Code"
                            ScanState.Processing -> "Processing..."
                            ScanState.Success -> "Song Found!"
                            ScanState.Error -> "Scan QR Code"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        if (hasCameraPermission) {
            Box(modifier = Modifier.padding(paddingValues)) {
                // QR Scanner Camera View (only show when scanning)
                if (scanState == ScanState.Scanning) {
                    RealQRScannerScreen(
                        onQRCodeDetected = handleQRCodeScanned
                    )
                }
                
                // Animated background for non-scanning states
                AnimatedVisibility(
                    visible = scanState != ScanState.Scanning,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1E1E1E),
                                        Color(0xFF121212)
                                    ),
                                    radius = 800f
                                )
                            )
                    )
                }
                
                // Instruction overlay for scanning state
                AnimatedVisibility(
                    visible = scanState == ScanState.Scanning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Position the QR code within the frame to scan",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Processing state
                AnimatedVisibility(
                    visible = scanState == ScanState.Processing,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(24.dp)
                                .widthIn(max = 400.dp)
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            Color(0xFF1DB954),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Processing",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "Processing QR Code...",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Finding your song",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Success state
                AnimatedVisibility(
                    visible = scanState == ScanState.Success,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(24.dp)
                                .widthIn(max = 400.dp)
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Success animation
                                val scale by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "success_scale"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            Color(0xFF4CAF50),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "Song Found!",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                
                                scannedSong?.let { song ->
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Song artwork
                                    if (song.artworkPath.isNotEmpty()) {
                                        AsyncImage(
                                            model = song.artworkPath,
                                            contentDescription = "Album Artwork",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF2E2E2E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = "Music",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text(
                                        text = song.artistName,
                                        color = Color.Gray,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Text(
                                        text = "Starting playback...",
                                        color = Color(0xFF1DB954),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Error state
                AnimatedVisibility(
                    visible = scanState == ScanState.Error,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(24.dp)
                                .widthIn(max = 400.dp)
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            Color(0xFFE53935),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "Scan Failed",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Returning to scanner...",
                                    color = Color(0xFF1DB954),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Current playing song indicator (bottom overlay)
                currentSong?.let { song ->
                    AnimatedVisibility(
                        visible = scanState == ScanState.Scanning,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.8f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPlaying) "Playing" else "Paused",
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPlaying) "♪ Now Playing" else "⏸ Paused",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${song.title} - ${song.artistName}",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Camera",
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF1DB954)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "To scan QR codes and discover new music, we need permission to use your camera. This is only used while you're in the scanner screen.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954)
                    )
                ) {
                    Text(
                        "Grant Permission", 
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = navigateUp,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Go Back", 
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

private enum class ScanState {
    Scanning,    // Camera is active and scanning for QR codes
    Processing,  // QR code detected, fetching song data
    Success,     // Song found and being prepared for playback
    Error        // Error occurred during scanning or processing
}