package com.msb.purrytify.ui.screen

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.ui.component.qrcode.ShareSongQRDialog
import com.msb.purrytify.utils.DeepLinkUtils
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.service.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import coil3.request.ImageRequest
import coil3.request.crossfade
import android.util.Log
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.SuccessResult
import coil3.request.allowHardware
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import java.util.Locale
import androidx.core.net.toUri


object EnhancedColorUtils {
    private fun Color.isLight(): Boolean = luminance() > 0.5f

    fun ensureContrast(
        foreground: Color,
        background: Color,
        minContrast: Float = 4.5f
    ): Color {
        val contrastRatio = calculateContrastRatio(foreground, background)
        
        return if (contrastRatio < minContrast) {
            if (background.isLight()) {
                adjustColorForContrast(foreground, background, targetLuminance = 0.15f)
            } else {
                adjustColorForContrast(foreground, background, targetLuminance = 0.85f)
            }
        } else {
            foreground
        }
    }

    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val lum1 = color1.luminance()
        val lum2 = color2.luminance()
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun adjustColorForContrast(
        color: Color,
        background: Color,
        targetLuminance: Float
    ): Color {
        val currentLuminance = color.luminance()
        val factor = if (targetLuminance > currentLuminance) {
            // Need to lighten
            1.0f + (targetLuminance - currentLuminance) * 2
        } else {
            // Need to darken
            targetLuminance / currentLuminance
        }
        
        return Color(
            red = (color.red * factor).coerceIn(0f, 1f),
            green = (color.green * factor).coerceIn(0f, 1f),
            blue = (color.blue * factor).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }
    
    /**
     * Creates an enhanced color scheme from palette with better contrast
     */
    fun createEnhancedColorScheme(palette: Palette): EnhancedColorScheme {
        // Extract base colors
        val dominantColor = Color(palette.getDominantColor(Color(0xFF121212).toArgb()))
        val vibrantColor = Color(palette.getVibrantColor(Color(0xFF1DB954).toArgb()))
        val darkVibrantColor = Color(palette.getDarkVibrantColor(Color(0xFF0D7534).toArgb()))
        val lightVibrantColor = Color(palette.getLightVibrantColor(Color(0xFF4CAF50).toArgb()))
        val mutedColor = Color(palette.getMutedColor(Color(0xFF666666).toArgb()))
        val darkMutedColor = Color(palette.getDarkMutedColor(Color(0xFF1A1A1A).toArgb()))
        
        // Determine base background - prefer dark muted over dominant for better music player aesthetics
        val baseBackground = if (darkMutedColor.luminance() < 0.3f) darkMutedColor else dominantColor
        
        // Ensure background is sufficiently dark for music player
        val backgroundColor = if (baseBackground.luminance() > 0.2f) {
            Color(
                red = baseBackground.red * 0.3f,
                green = baseBackground.green * 0.3f,
                blue = baseBackground.blue * 0.3f,
                alpha = 1f
            )
        } else baseBackground
        
        // Choose accent color with better saturation
        val accentColor = when {
            vibrantColor.luminance() > 0.15f && calculateSaturation(vibrantColor) > 0.4f -> vibrantColor
            lightVibrantColor.luminance() > 0.3f -> lightVibrantColor
            else -> Color(0xFF1DB954) // Fallback Spotify green
        }
        
        // Ensure accent has good contrast against background
        val enhancedAccent = ensureContrast(accentColor, backgroundColor, 3.0f)
        
        // Calculate text colors with high contrast
        val primaryText = ensureContrast(Color.White, backgroundColor, 7.0f)
        val secondaryText = ensureContrast(Color.White.copy(alpha = 0.7f), backgroundColor, 4.5f)
        
        return EnhancedColorScheme(
            background = backgroundColor,
            accent = enhancedAccent,
            primaryText = primaryText,
            secondaryText = secondaryText,
            controlBackground = enhancedAccent,
            controlForeground = ensureContrast(Color.White, enhancedAccent, 4.5f)
        )
    }
    
    private fun calculateSaturation(color: Color): Float {
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        return if (max != 0f) (max - min) / max else 0f
    }
}

data class EnhancedColorScheme(
    val background: Color,
    val accent: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val controlBackground: Color,
    val controlForeground: Color
)

@Composable
fun PlayerScreen(
    song: Song,
    onDismiss: () -> Unit,
    onDismissWithAnimation: () -> Unit = {},
    isDismissing: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val currentPlayingSong = viewModel.currentSong.value ?: song
    
    var localIsDismissing by remember { mutableStateOf(false) }
    val actualIsDismissing = isDismissing || localIsDismissing
    
    val density = LocalDensity.current

    BackHandler {
        if (!actualIsDismissing) {
            viewModel.setLargePlayerVisible(false)
            localIsDismissing = true
            onDismissWithAnimation()
        }
    }

    val slideOffset by animateFloatAsState(
        targetValue = if (actualIsDismissing) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = {
            if (actualIsDismissing) {
                viewModel.setMiniPlayerVisible(viewModel.currentSong.value != null)
                onAnimationComplete()
                onDismiss()
            }
        }
    )
    
    LaunchedEffect(Unit) {
        val isAlreadyPlaying = currentPlayingSong.id == song.id
        Log.d("PlayerScreen", "currentPlayingSong id: ${currentPlayingSong.id}, song id: ${song.id}")
        val wasPlaying = viewModel.isPlaying.value

        if (!isAlreadyPlaying) {
            Log.d("PlayerScreen", "Not already playing, setting new song")
            viewModel.setPlaylist(listOf(song))
            viewModel.playSong(song)
        } else if (wasPlaying) {
            Log.d("PlayerScreen", "Already playing, resuming song")
            viewModel.resumeCurrentSong()
        }
    }
    
    LaunchedEffect(viewModel.currentSong.value) {
        if (viewModel.currentSong.value == null) {
            localIsDismissing = true
            onDismissWithAnimation()
        }
    }
    
    // Enhanced color scheme with better contrast
    var colorScheme by remember { 
        mutableStateOf(
            EnhancedColorScheme(
                background = Color(0xFF0A0A0A),
                accent = Color(0xFF1DB954),
                primaryText = Color.White,
                secondaryText = Color.White.copy(alpha = 0.7f),
                controlBackground = Color(0xFF1DB954),
                controlForeground = Color.White
            )
        )
    }
    
    val isPlaying by viewModel.isPlaying
    val isLiked by viewModel.isLiked
    val currentPosition by viewModel.currentPosition
    val duration by viewModel.duration

    val context = LocalContext.current

    // Enhanced palette extraction with better color processing
    LaunchedEffect(currentPlayingSong.id, currentPlayingSong.artworkPath) {
        try {
            val bitmap = if (currentPlayingSong.artworkPath.isNotEmpty()) {
                if (currentPlayingSong.artworkPath.startsWith("http")) {
                    var loadedBitmap: Bitmap? = null
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(currentPlayingSong.artworkPath)
                        .allowHardware(false)
                        .build()

                    try {
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            loadedBitmap = (result.image as? BitmapImage)?.bitmap
                        }
                    } catch (e: Exception) {
                        Log.e("PlayerScreen", "Error loading artwork from URL: ${e.message}", e)
                    }
                    loadedBitmap
                } else {
                    val artworkUri = Uri.parse(currentPlayingSong.artworkPath)
                    val inputStream = context.contentResolver.openInputStream(artworkUri)
                    inputStream?.use { BitmapFactory.decodeStream(it) }
                }
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.image)
            }

            bitmap?.let {
                withContext(Dispatchers.Default) {
                    val palette = Palette.from(it)
                        .maximumColorCount(16) // Increased for better color extraction
                        .generate()
                    
                    colorScheme = EnhancedColorUtils.createEnhancedColorScheme(palette)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            viewModel.updatePosition()
        }
    }
    
    // Enhanced artwork display with subtle gradient overlay
    val artworkContent = remember(currentPlayingSong.artworkPath) {
        @Composable {
            Box(
                modifier = Modifier
                    .size(320.dp) // Slightly larger for better visual impact
                    .clip(RoundedCornerShape(12.dp)) // More rounded corners
            ) {
                val artworkUriString = currentPlayingSong.artworkPath

                if (artworkUriString.isNotEmpty()) {
                    val artworkUri = artworkUriString.takeIf { it.isNotEmpty() }?.let {
                        it.toUri()
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.image),
                        contentDescription = "Default Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Subtle gradient overlay for better text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colorScheme.background.copy(alpha = 0.1f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
        }
    }
    
    // Enhanced player controls with better contrast
    val playerControls = remember(isPlaying, colorScheme) {
        @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colorScheme.primaryText,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Enhanced play/pause button with better contrast and glow effect
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            colorScheme.controlBackground,
                            RoundedCornerShape(40.dp)
                        )
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colorScheme.controlForeground,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = colorScheme.primaryText,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
    
    // Enhanced progress slider with better visibility
    val progressSlider = remember(duration, colorScheme) {
        @Composable {
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition,
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..duration.coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.accent,
                        activeTrackColor = colorScheme.accent,
                        inactiveTrackColor = colorScheme.accent.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp) // Slightly taller for easier interaction
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition.toInt()),
                        color = colorScheme.secondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDuration(duration.toInt()),
                        color = colorScheme.secondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    val offsetY = with(density) { slideOffset * 1000.dp.toPx() }
    
    // Enhanced background with subtle gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colorScheme.background.copy(alpha = 0.95f),
                        colorScheme.background
                    ),
                    radius = 800f
                )
            )
            .zIndex(10f)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        viewModel.setLargePlayerVisible(false)
                        localIsDismissing = true
                        onDismissWithAnimation()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 50) {
                            change.consume()
                            localIsDismissing = true
                            onDismissWithAnimation()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp), // Slightly more padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enhanced header with better contrast
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.setLargePlayerVisible(false)
                    viewModel.setMiniPlayerVisible(true)
                    localIsDismissing = true
                    onDismissWithAnimation()
                }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = colorScheme.primaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "Now Playing",
                    color = colorScheme.primaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                var showMenu by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showQRDialog by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Menu",
                            tint = colorScheme.primaryText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(colorScheme.background)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit Song",
                                        tint = colorScheme.primaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Song", color = colorScheme.primaryText)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share URL",
                                        tint = if (viewModel.canShareSong()) colorScheme.primaryText else colorScheme.secondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Share URL", 
                                        color = if (viewModel.canShareSong()) colorScheme.primaryText else colorScheme.secondaryText
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                if (viewModel.canShareSong()) {
                                    DeepLinkUtils.shareSong(context, currentPlayingSong)
                                } else {
                                    Toast.makeText(
                                        context, 
                                        "Only online songs can be shared", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = viewModel.canShareSong()
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode,
                                        contentDescription = "Share QR Code",
                                        tint = if (viewModel.canShareSong()) colorScheme.primaryText else colorScheme.secondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Share QR Code", 
                                        color = if (viewModel.canShareSong()) colorScheme.primaryText else colorScheme.secondaryText
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                if (viewModel.canShareSong()) {
                                    viewModel.shareCurrentSongViaQR()
                                } else {
                                    Toast.makeText(
                                        context, 
                                        "Only online songs can be shared via QR code", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = viewModel.canShareSong()
                        )
                    }
                }
                
                if (showEditDialog) {
                    EditSongDialog(
                        song = currentPlayingSong,
                        onDismiss = { showEditDialog = false },
                        colorScheme = colorScheme,
                        viewModel = viewModel
                    )
                }
                
                if (showQRDialog) {
                    ShareSongQRDialog(
                        song = currentPlayingSong,
                        onDismiss = { showQRDialog = false }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            artworkContent()
            Spacer(modifier = Modifier.height(32.dp))
            
            // Enhanced song info section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPlayingSong.title,
                        color = colorScheme.primaryText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentPlayingSong.artist,
                        color = colorScheme.secondaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                
                // Enhanced like button
                IconButton(
                    onClick = { viewModel.toggleLike() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color(0xFFE53E3E) else colorScheme.primaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            progressSlider()
            Spacer(modifier = Modifier.weight(1f))
            playerControls()

            // Enhanced secondary controls with better contrast
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.isShuffle.value) colorScheme.accent else colorScheme.secondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (viewModel.repeatMode.value) {
                            RepeatMode.NONE -> Icons.Filled.Repeat
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            RepeatMode.ALL -> Icons.Filled.RepeatOn
                        },
                        contentDescription = "Repeat",
                        tint = if (viewModel.repeatMode.value != RepeatMode.NONE) 
                            colorScheme.accent else colorScheme.secondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun formatDuration(miliseconds: Int): String {
    val seconds = miliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    colorScheme: EnhancedColorScheme,
    viewModel: PlayerViewModel
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var selectedArtworkUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val buttonGreen = colorScheme.accent
    val buttonGray = colorScheme.background.copy(alpha = 0.7f)
    val borderColor = colorScheme.secondaryText.copy(alpha = 0.3f)
    val textFieldBgColor = colorScheme.background.copy(alpha = 0.8f)
    
    val pickArtworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedArtworkUri = it
            } catch (e: Exception) {
                Toast.makeText(context, "Error selecting artwork: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = if (Build.VERSION.SDK_INT >= 33) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        if (isGranted) {
            pickArtworkLauncher.launch("image/*")
        } else {
            showPermissionDialog = true
        }
    }
    
    fun requestImagePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            imagePermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            ))
        } else {
            imagePermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = colorScheme.background,
        dragHandle = null,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Song",
                    color = colorScheme.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { requestImagePermission() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedArtworkUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedArtworkUri),
                                contentDescription = "Selected Artwork",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (song.artworkPath.isNotEmpty()) {
                            val artworkUri = Uri.parse(song.artworkPath)
                            AsyncImage(
                                model = artworkUri,
                                contentDescription = "Current Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = "Upload Image",
                                    tint = colorScheme.secondaryText,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Upload Photo",
                                    color = colorScheme.secondaryText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = "Song Info",
                                tint = colorScheme.secondaryText,
                                modifier = Modifier.size(40.dp)
                            )
                            
                            val minutes = song.duration / 60000
                            val seconds = (song.duration % 60000) / 1000
                            Text(
                                text = "Duration: ${minutes}:${String.format(Locale.getDefault(), "%02d", seconds)}",
                                color = colorScheme.secondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = "Title",
                    color = colorScheme.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 8.dp, bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", color = colorScheme.secondaryText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.primaryText,
                        unfocusedTextColor = colorScheme.primaryText,
                        disabledTextColor = colorScheme.primaryText,
                        focusedContainerColor = textFieldBgColor,
                        unfocusedContainerColor = textFieldBgColor,
                        disabledContainerColor = textFieldBgColor,
                        cursorColor = colorScheme.accent,
                        focusedBorderColor = colorScheme.accent,
                        unfocusedBorderColor = borderColor
                    ),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true
                )
                
                Text(
                    text = "Artist",
                    color = colorScheme.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 16.dp, bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    placeholder = { Text("Artist", color = colorScheme.secondaryText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.primaryText,
                        unfocusedTextColor = colorScheme.primaryText,
                        disabledTextColor = colorScheme.primaryText,
                        focusedContainerColor = textFieldBgColor,
                        unfocusedContainerColor = textFieldBgColor,
                        disabledContainerColor = textFieldBgColor,
                        cursorColor = colorScheme.accent,
                        focusedBorderColor = colorScheme.accent,
                        unfocusedBorderColor = borderColor
                    ),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonGray
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = colorScheme.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val coroutineScope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            if (title.isBlank() || artist.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please fill all required fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                coroutineScope.launch {
                                    try {
                                        val artworkFilePath = selectedArtworkUri?.toString() ?: song.artworkPath
                                        
                                        viewModel.updateSong(
                                            songId = song.id,
                                            title = title,
                                            artist = artist,
                                            artworkPath = artworkFilePath
                                        )

                                        viewModel.updateSongFromRepo()
                                        
                                        Toast.makeText(
                                            context,
                                            "Song updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        
                                        onDismiss()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(
                                            context,
                                            "Error updating song: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonGreen
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Save",
                            color = colorScheme.controlForeground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    )
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required", color = colorScheme.primaryText) },
            text = {
                Text("Storage permission is required to select files. Please grant this permission in app settings.", 
                     color = colorScheme.secondaryText)
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK", color = colorScheme.accent)
                }
            },
            containerColor = colorScheme.background
        )
    }
}
