package com.msb.purrytify.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.ui.component.NoInternet
import com.msb.purrytify.utils.networkStatusListener
import com.msb.purrytify.viewmodel.OnlineSongDownloadViewModel
import com.msb.purrytify.viewmodel.OnlineSongsViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import androidx.compose.ui.platform.LocalDensity

enum class OnlineSongsScreenType {
    GLOBAL,
    COUNTRY
}

@Composable
fun OnlineSongsScreen(
    screenType: OnlineSongsScreenType,
    onDismiss: () -> Unit,
    onDismissWithAnimation: () -> Unit = {},
    isDismissing: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    viewModel: OnlineSongsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    downloadViewModel: OnlineSongDownloadViewModel = hiltViewModel()
) {
    val primaryColor = when (screenType) {
        OnlineSongsScreenType.GLOBAL -> Color(0xFF1E88E5) // biru buat top 50 global
        OnlineSongsScreenType.COUNTRY -> Color(0xFFE53935) // merah buat top 10 country
    }
    
    val titleText = when (screenType) {
        OnlineSongsScreenType.GLOBAL -> "Top 50\nGlobal"
        OnlineSongsScreenType.COUNTRY -> {
            val countryCode by viewModel.currentCountryCode.collectAsState()
            "Top 10\nCountry\n$countryCode"
        }
    }
    
    val descriptionText = when (screenType) {
        OnlineSongsScreenType.GLOBAL -> "Daily update of world's most played songs"
        OnlineSongsScreenType.COUNTRY -> "Best hits from your country"
    }

    val uiState = when (screenType) {
        OnlineSongsScreenType.GLOBAL -> viewModel.uiState.collectAsState().value
        OnlineSongsScreenType.COUNTRY -> viewModel.countryUiState.collectAsState().value
    }

    LaunchedEffect(screenType) {
        when (screenType) {
            OnlineSongsScreenType.GLOBAL -> viewModel.fetchGlobalTopSongs()
            OnlineSongsScreenType.COUNTRY -> {
                val countryCode = viewModel.currentCountryCode.value
                if (countryCode.isNotBlank()) {
                    viewModel.fetchCountryTopSongs(countryCode)
                }
            }
        }
    }

    var localIsDismissing by remember { mutableStateOf(false) }
    val actualIsDismissing = isDismissing || localIsDismissing
    
    val isConnected = networkStatusListener()

    var backgroundColor by remember { mutableStateOf(Color(0xFF121212)) }
    var textColor by remember { mutableStateOf(Color.White) }
    var accentColor by remember { mutableStateOf(Color(0xFF1DB954)) }
    var gradientColor by remember { mutableStateOf(Color(0xFF121212)) }
    var gradientColor2 by remember { mutableStateOf(Color(0xFF232A4D)) }
    
    val density = LocalDensity.current

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
            backgroundColor = primaryColor.copy(alpha = 0.8f)
            accentColor = Color.White
            textColor = Color.White
            gradientColor = primaryColor.copy(alpha = 0.8f)
            gradientColor2 = Color(0xFF121212)
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
        val (selectedSong, playlist) = when (screenType) {
            OnlineSongsScreenType.GLOBAL -> viewModel.getSongToPlay(song)
            OnlineSongsScreenType.COUNTRY -> viewModel.getCountrySongToPlay(song)
        }
        val index = playlist.indexOfFirst { it.id == selectedSong.id }
        
        playerViewModel.setPlaylist(playlist, if (index != -1) index else 0)
        playerViewModel.playSong(selectedSong)
        playerViewModel.setLargePlayerVisible(true)
    }

    val offsetY = with(density) { slideOffset * 1000.dp.toPx() }
    
    val headerContent = remember(titleText, descriptionText, totalDurationString, uiState.songs.size) {
        @Composable {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(gradientColor, Color(0xFF121212)),
                            startY = 0f,
                            endY = 600f
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = SolidColor(primaryColor),
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = titleText,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = descriptionText,
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
        }
    }
    

    
    val offlineContent = remember {
        @Composable {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientColor, gradientColor2)
                )
            )
            .offset(y = offsetY.dp)
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
                        .padding(top = 50.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    headerContent()
                    
                    // Song list content without LazyColumn since we're using verticalScroll
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF121212))
                            .padding(horizontal = 16.dp)
                    ) {
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = accentColor)
                                }
                            }
                            uiState.error != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
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
                                            onClick = { 
                                                when (screenType) {
                                                    OnlineSongsScreenType.GLOBAL -> viewModel.fetchGlobalTopSongs()
                                                    OnlineSongsScreenType.COUNTRY -> viewModel.fetchCountryTopSongs()
                                                }
                                            },
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
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
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    uiState.songs.forEachIndexed { idx, song ->
                                        NumberedSongItem(
                                            number = idx + 1,
                                            song = song,
                                            onSongClick = { playSong(song) },
                                            textColor = textColor,
                                            downloadViewModel = downloadViewModel,
                                            viewModel = viewModel
                                        )
                                    }
                                    // Add bottom padding for mini player
                                    Spacer(modifier = Modifier.height(100.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            offlineContent()
        }
    }
}

@Composable
fun NumberedSongItem(
    number: Int,
    song: Song,
    onSongClick: () -> Unit,
    textColor: Color,
    downloadViewModel: OnlineSongDownloadViewModel = hiltViewModel(),
    viewModel: OnlineSongsViewModel = hiltViewModel()
) {
    val songId = song.id
    val songResponse = viewModel.getSongById(songId)
    
    val downloadState by downloadViewModel.downloadState.collectAsState()
    val isDownloadable = songResponse != null
    
    val downloadingSongId = when (downloadState) {
        is OnlineSongDownloadViewModel.DownloadState.Downloading -> 
            (downloadState as? OnlineSongDownloadViewModel.DownloadState.Downloading)?.songId
        else -> null
    }
    
    val downloadingSong = downloadingSongId == songId
    
    val isSomeDownloadInProgress = downloadState is OnlineSongDownloadViewModel.DownloadState.Downloading
    
    val isDownloaded by downloadViewModel.isDownloaded(songId).collectAsState(initial = false)
    
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
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (isDownloadable) {
            when {
                isDownloaded -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 8.dp)
                    )
                }
                downloadingSong -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1DB954)
                    )
                }
                else -> {
                    IconButton(
                        onClick = {
                            songResponse?.let { downloadViewModel.downloadSong(it, songId) }
                        },
                        enabled = !isSomeDownloadInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = if (isSomeDownloadInProgress) 
                                textColor.copy(alpha = 0.3f) 
                            else 
                                textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        IconButton(onClick = onSongClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = textColor
            )
        }
    }
} 