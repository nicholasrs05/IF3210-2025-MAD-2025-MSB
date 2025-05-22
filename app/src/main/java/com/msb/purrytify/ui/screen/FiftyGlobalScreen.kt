package com.msb.purrytify.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.ui.component.NoInternet
import com.msb.purrytify.utils.NetworkStatusListener
import com.msb.purrytify.viewmodel.OnlineSongsViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FiftyGlobalScreen(
    onDismiss: () -> Unit,
    onDismissWithAnimation: () -> Unit = {},
    isDismissing: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    viewModel: OnlineSongsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    var localIsDismissing by remember { mutableStateOf(false) }
    val actualIsDismissing = isDismissing || localIsDismissing

    val isConnected = NetworkStatusListener()
    val uiState by viewModel.uiState.collectAsState()

    var backgroundColor by remember { mutableStateOf(Color(0xFF121212)) }
    var textColor by remember { mutableStateOf(Color.White) }
    var accentColor by remember { mutableStateOf(Color(0xFF1DB954)) }
    var gradientColor by remember { mutableStateOf(Color(0xFF121212)) }
    var gradientColor2 by remember { mutableStateOf(Color(0xFF232A4D)) }

    val slideOffset by animateFloatAsState(
        targetValue = if (actualIsDismissing) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = {
            if (actualIsDismissing) {
                playerViewModel.setMiniPlayerVisible(playerViewModel.currentSong.value != null)
                onAnimationComplete()
                onDismiss()
            }
        }
    )

    val context = LocalContext.current

    val totalDurationMs = uiState.songs.sumOf { it.duration }

    fun formatTotalDuration(ms: Long): String {
        val totalMinutes = ms / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes} min" else "${minutes} min"
    }
    val totalDurationString = formatTotalDuration(totalDurationMs)

    LaunchedEffect(Unit) {
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.fiftyglobal)
            withContext(Dispatchers.Default) {
                val palette = Palette.from(bitmap).generate()
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
                gradientColor = Color(darkColor)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    BackHandler {
        if (!actualIsDismissing) {
            localIsDismissing = true
            onDismissWithAnimation()
        }
    }

    val playSong: (Song) -> Unit = { song ->
        val (selectedSong, playlist) = viewModel.getSongToPlay(song)
        val index = playlist.indexOfFirst { it.id == selectedSong.id }
        
        playerViewModel.setPlaylist(playlist, if (index != -1) index else 0)
        playerViewModel.playSong(selectedSong)
        playerViewModel.setLargePlayerVisible(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientColor, gradientColor2)
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 10) {
                        localIsDismissing = true
                        onDismissWithAnimation()
                    }
                }
            }
    ) {
        if (isConnected) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = {
                        localIsDismissing = true
                        onDismissWithAnimation()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 4.dp, top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(gradientColor, Color(0xFF121212)),
                                    startY = 0f,
                                    endY = 600f // adjust as needed for fade
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .size(200.dp)
                                    .align(Alignment.CenterHorizontally),
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.fiftyglobal),
                                    contentDescription = "Fifty Global Background",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Daily update of world's most played songs",
                                color = textColor.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                    contentDescription = "Purrytify",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Purrytify",
                                    color = textColor.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$totalDurationString",
                                    color = textColor.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        if (uiState.songs.isNotEmpty()) playSong(uiState.songs[0])
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB95B)),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color(0xFF191414),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF121212))
                            .padding(horizontal = 16.dp)
                    ) {
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = accentColor)
                                }
                            }
                            uiState.error != null -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Error loading songs",
                                            color = textColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = uiState.error ?: "Unknown error",
                                            color = textColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.fetchGlobalTopSongs() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor
                                            )
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                            uiState.songs.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No songs available",
                                        color = textColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    itemsIndexed(uiState.songs) { idx, song ->
                                        NumberedSongItem(
                                            number = idx + 1,
                                            song = song,
                                            onSongClick = { playSong(song) },
                                            textColor = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            localIsDismissing = true
                            onDismissWithAnimation()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                NoInternet()
            }
        }
    }
}

@Composable
fun NumberedSongItem(
    number: Int,
    song: Song,
    onSongClick: () -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { onSongClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number.toString(),
            color = textColor.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        AsyncImage(
            model = song.artworkPath,
            contentDescription = "Album Artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}