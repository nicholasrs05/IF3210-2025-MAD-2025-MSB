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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.viewmodel.PlayerViewModel
import android.net.Uri

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

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playerViewModel.updatePosition()
            kotlinx.coroutines.delay(100)
        }
    }

    var textColor by remember { mutableStateOf(Color.White) }
    val isLiked by playerViewModel.isLiked

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
                containerColor = Color(0xFF212121)
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
                        if (currentSong.artworkPath.isNotEmpty()) {
                            val artworkUri = Uri.parse(currentSong.artworkPath)
                            AsyncImage(
                                model = artworkUri,
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
                            text = currentSong.artist,
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
