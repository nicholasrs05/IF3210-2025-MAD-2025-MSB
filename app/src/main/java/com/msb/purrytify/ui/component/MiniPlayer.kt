package com.msb.purrytify.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.utils.DeepLinkUtils
import com.msb.purrytify.viewmodel.PlayerViewModel
import android.net.Uri
import androidx.palette.graphics.Palette
import androidx.core.graphics.ColorUtils
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toArgb
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import coil3.ImageLoader
import coil3.request.SuccessResult
import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.request.allowHardware
import com.msb.purrytify.model.AudioDeviceType
import androidx.core.net.toUri

@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onPlayerClick: (Song) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isLandscape: Boolean = false
) {
    if (currentSong == null) return

    val currentPosition by playerViewModel.currentPosition
    val duration by playerViewModel.duration
    val showAudioDeviceSheet by playerViewModel.showAudioDeviceSheet
    val currentAudioDevice by playerViewModel.currentAudioDevice

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playerViewModel.updatePosition()
            kotlinx.coroutines.delay(100)
        }
    }

    var textColor by remember { mutableStateOf(Color.White) }
    var backgroundColor by remember { mutableStateOf(Color(0xFF212121)) }
    var accentColor by remember { mutableStateOf(Color(0xFF1DB954)) }
    val isLiked by playerViewModel.isLiked
    val context = LocalContext.current

    LaunchedEffect(currentSong.id, currentSong.artworkPath) {
        try {
            val bitmap = if (currentSong.artworkPath.isNotEmpty()) {
                if (currentSong.artworkPath.startsWith("http")) {
                    var loadedBitmap: Bitmap? = null
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(currentSong.artworkPath)
                        .allowHardware(false)
                        .build()

                    try {
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            loadedBitmap = (result.image as? BitmapImage)?.bitmap
                        }
                    } catch (e: Exception) {
                        Log.e("MiniPlayer", "Error loading artwork from URL: ${e.message}", e)
                    }
                    loadedBitmap
                } else {
                    val artworkUri = currentSong.artworkPath.toUri()
                    val inputStream = context.contentResolver.openInputStream(artworkUri)
                    inputStream?.use { BitmapFactory.decodeStream(it) }
                }
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.image)
            }

            bitmap?.let {
                withContext(Dispatchers.Default) {
                    val palette = Palette.from(it).generate()
                    val darkColor = palette.getDarkVibrantColor(
                        palette.getDarkMutedColor(Color(0xFF121212).toArgb())
                    )
                    val vibrantColor = palette.getVibrantColor(
                        palette.getLightVibrantColor(Color(0xFF1DB954).toArgb())
                    )

                    backgroundColor = Color(darkColor)
                    accentColor = Color(vibrantColor)
                    textColor = if (ColorUtils.calculateLuminance(darkColor) > 0.5)
                        Color.Black else Color.White
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (showAudioDeviceSheet) {
        AudioDeviceSheet(
            onDismiss = { playerViewModel.hideAudioDeviceSheet() },
            onDeviceSelected = { device ->
                playerViewModel.selectAudioDevice(device)
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .then(
                    if (isLandscape) {
                        Modifier.fillMaxWidth(0.33f)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .height(80.dp)
                .padding(horizontal = 8.dp, vertical = if (isLandscape) 0.dp else 4.dp)
                .align(Alignment.CenterStart)
                .clickable {
                    playerViewModel.setLargePlayerVisible(true)
                    onPlayerClick(currentSong)
                },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        val artworkUriString = currentSong.artworkPath

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
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = currentSong.artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { playerViewModel.toggleLike() },
                        modifier = Modifier
                            .size(36.dp)
                    ) {
                        if (isLiked) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Unlike",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.miniplayer_add),
                                contentDescription = "Like",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    val context = LocalContext.current
                    IconButton(
                        onClick = { 
                            currentSong?.let { DeepLinkUtils.shareSong(context, it) }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerViewModel.showAudioDeviceSheet() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = when {
                                    currentAudioDevice?.type == AudioDeviceType.BLUETOOTH_DEVICE ||
                                    currentAudioDevice?.type == AudioDeviceType.USB_HEADSET ->
                                        R.drawable.ic_bluetooth_speaker
                                    currentAudioDevice?.type == AudioDeviceType.WIRED_HEADSET ->
                                        R.drawable.ic_headset
                                    else -> R.drawable.ic_speaker
                                }
                            ),
                            contentDescription = "Select Audio Output",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                CustomMiniSlider(
                    progress = currentPosition,
                    duration = duration,
                    onSeek = { playerViewModel.seekTo(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CustomMiniSlider(
    progress: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderFraction = if (duration > 0f) {
        (progress / duration).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val percent = offset.x / size.width
                    onSeek(percent * duration)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp))
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(sliderFraction)
                .background(Color.White, shape = RoundedCornerShape(2.dp))
        )
    }
}
