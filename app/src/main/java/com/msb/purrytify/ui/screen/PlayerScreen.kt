package com.msb.purrytify.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.viewmodel.PlaybackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PlayerScreen(
    song: Song,
    onDismiss: () -> Unit,
    onDismissWithAnimation: () -> Unit = {},
    isDismissing: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val mediaPlayerManager = playbackViewModel.mediaPlayerManager
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
                onAnimationComplete()
                onDismiss() 
            }
        }
    )
    
    LaunchedEffect(Unit) {
        val isAlreadyPlaying = mediaPlayerManager.getCurrentSong()?.id == song.id
        val wasPlaying = viewModel.isPlaying.value

        if (!isAlreadyPlaying) {
            mediaPlayerManager.setPlaylist(listOf(song))
            viewModel.playSong(song)
        } else if (wasPlaying) {
            viewModel.resumeCurrentSong()
        }
    }
    
    DisposableEffect(Unit) {
        val songChangeListener = object : MediaPlayerManager.SongChangeListener {
            override fun onSongChanged(newSong: Song) {
                viewModel.updateCurrentSong()
            }
            
            override fun onPlayerReleased() {
                viewModel.resetCurrentSong()
                viewModel.viewModelScope.launch {
                    kotlinx.coroutines.delay(300)
                    if (mediaPlayerManager.getCurrentSong() == null) {
                        localIsDismissing = true
                        onDismissWithAnimation()
                    }
                }
            }
        }
        mediaPlayerManager.addSongChangeListener(songChangeListener)
        
        onDispose {
            mediaPlayerManager.removeSongChangeListener(songChangeListener)
        }
    }
    
    var backgroundColor by remember { mutableStateOf(Color(0xFF121212)) }
    var textColor by remember { mutableStateOf(Color.White) }
    var accentColor by remember { mutableStateOf(Color(0xFF1DB954)) }
    
    val isPlaying by viewModel.isPlaying
    val isLiked by viewModel.isLiked
    val currentPosition by viewModel.currentPosition
    val duration by viewModel.duration
    
    DisposableEffect(currentPlayingSong.id) {
        val extractColors = suspend {
            if (currentPlayingSong.artworkPath.isNotEmpty() && File(currentPlayingSong.artworkPath).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(currentPlayingSong.artworkPath)
                    withContext(Dispatchers.Default) {
                        val palette = Palette.from(bitmap).generate()
                        val darkColor = palette.getDarkVibrantColor(palette.getDarkMutedColor(Color(0xFF121212).toArgb()))
                        val vibrantColor = palette.getVibrantColor(palette.getLightVibrantColor(Color(0xFF1DB954).toArgb()))
                        
                        backgroundColor = Color(darkColor)
                        accentColor = Color(vibrantColor)
                        textColor = if (ColorUtils.calculateLuminance(darkColor) > 0.5) Color.Black else Color.White
                    }
                } catch (_: Exception) {
                }
            }
        }
        
        val job = viewModel.viewModelScope.launch { extractColors() }
        onDispose { job.cancel() }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(100)
            viewModel.updatePosition()
        }
    }
    
    // Cache artwork
    val artworkContent = remember(currentPlayingSong.artworkPath) {
        @Composable {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (currentPlayingSong.artworkPath.isNotEmpty() && File(currentPlayingSong.artworkPath).exists()) {
                    AsyncImage(
                        model = File(currentPlayingSong.artworkPath),
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
            }
        }
    }
    
    // Cache player controls
    val playerControls = remember(isPlaying, accentColor, backgroundColor, textColor) {
        @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = textColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor, RoundedCornerShape(32.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = backgroundColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = textColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
    
    val progressSlider = remember(duration) {
        @Composable {
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition,
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..duration.coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = accentColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition.toInt()),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatDuration(duration.toInt()),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    
    val offsetY = with(density) { slideOffset * 1000.dp.toPx() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.dp)
            .background(backgroundColor)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.setLargePlayerVisible(false)
                    localIsDismissing = true
                    onDismissWithAnimation() 
                }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }
                
                Text(
                    text = "Now Playing",
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium
                )
                
                IconButton(onClick = { /* Menu */ }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu",
                        tint = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            artworkContent()
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPlayingSong.title,
                        color = textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentPlayingSong.artist,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
                
                IconButton(onClick = { viewModel.toggleLike() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color.Red else textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            progressSlider()
            Spacer(modifier = Modifier.weight(1f))
            playerControls()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.isShuffle.value) accentColor else textColor.copy(alpha = 0.5f)
                    )
                }
                
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (viewModel.repeatMode.value) {
                            PlayerViewModel.RepeatMode.NONE -> Icons.Filled.Repeat
                            PlayerViewModel.RepeatMode.ONE -> Icons.Filled.RepeatOne
                            PlayerViewModel.RepeatMode.ALL -> Icons.Filled.RepeatOn
                        },
                        contentDescription = "Repeat",
                        tint = if (viewModel.repeatMode.value != PlayerViewModel.RepeatMode.NONE) 
                            accentColor else textColor.copy(alpha = 0.5f)
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
