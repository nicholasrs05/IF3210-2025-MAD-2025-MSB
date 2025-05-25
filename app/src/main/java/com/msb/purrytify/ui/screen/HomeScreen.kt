package com.msb.purrytify.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.viewmodel.HomeViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.navigation.NavController
import com.msb.purrytify.ui.navigation.Screen
import androidx.core.net.toUri
import com.msb.purrytify.ui.component.recommendation.RecommendationSection
import com.msb.purrytify.viewmodel.RecommendationViewModel
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay


@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    recommendationViewModel: RecommendationViewModel = hiltViewModel(),
    navController: NavController? = null,
    onScanQRCode: () -> Unit
) {
    val context = LocalContext.current
    val recentlyPlayedState: State<List<Song>> =
        homeViewModel.recentlyPlayedSongs.observeAsState(initial = emptyList())
    val newSongsState: State<List<Song>> =
        homeViewModel.newSongs.observeAsState(initial = emptyList())
    val recentlyPlayed: List<Song> = recentlyPlayedState.value
    val newSongs: List<Song> = newSongsState.value
    val recommendedSongs by recommendationViewModel.recommendedSongs.collectAsState()
    val playbackError by homeViewModel.playbackError.collectAsState()
    
    // QR scan success notification state
    var showQRSuccessNotification by remember { mutableStateOf(false) }
    val currentSong by playerViewModel.currentSong
    val isFromQRScan by playerViewModel.isFromQRScan
    
    // Show success notification when song is played from QR scan
    LaunchedEffect(isFromQRScan, currentSong) {
        if (isFromQRScan && currentSong != null) {
            showQRSuccessNotification = true
            delay(4000) // Show for 4 seconds
            showQRSuccessNotification = false
            playerViewModel.clearQRScanFlag()
        }
    }
    
    // Show error messages when playback errors occur
    LaunchedEffect(playbackError) {
        playbackError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            homeViewModel.clearPlaybackError()
        }
    }
    
    LaunchedEffect(Unit) {
        recommendationViewModel.loadTrendingSongs()
    }

    val onClickedRecent: (Song) -> Unit = { song ->
        homeViewModel.playRecentSongs(recentlyPlayed, song)
        playerViewModel.setMiniPlayerVisible(true)
    }

    val onClickedNew: (Song) -> Unit = { song ->
        homeViewModel.playNewSongs(newSongs, song)
        playerViewModel.setMiniPlayerVisible(true)
    }
    
    val onClickedRecommended: (Song) -> Unit = { song ->
        homeViewModel.playSong(song)
        playerViewModel.setMiniPlayerVisible(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Charts",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.W800
                        )

                        Button(
                            onClick = onScanQRCode,
                            shape = RoundedCornerShape(45.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .defaultMinSize(
                                    minWidth = ButtonDefaults.MinWidth,
                                    minHeight = 10.dp
                                )
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E8B57),
                                contentColor = Color.White,
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR Code",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    OnlineSongsSection(
                        onFiftyGlobalClick = {
                            navController?.navigate(Screen.FiftyGlobal.route)
                        },
                        onTop10CountryClick = {
                            navController?.navigate(Screen.Top10Country.route)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RecommendationSection(
                        title = "Recommended for You",
                        songs = recommendedSongs,
                        onSongClick = onClickedRecommended
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "New Songs",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W800,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    NewSongsSection(newSongs, onSongClick = onClickedNew)
                }

                item {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    RecentlyPlayedSection(recentlyPlayed, onSongClick = onClickedRecent)
                }
            }
        }
        
        // QR Scan Success Notification
        AnimatedVisibility(
            visible = showQRSuccessNotification && currentSong != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(10f)
        ) {
            currentSong?.let { song ->
                QRScanSuccessNotification(
                    song = song,
                    onDismiss = { showQRSuccessNotification = false }
                )
            }
        }
    }
}


@Composable
fun NewSongsSection(songs: List<Song>?, onSongClick: (Song) -> Unit) {
    if (songs == null || songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No new songs available.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(songs) { song ->
                NewSongItem(
                    song = song,
                    onSongClick = onSongClick
                )
            }
        }
    }
}


@Composable
fun NewSongItem(song: Song, onSongClick: (Song) -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable { onSongClick(song) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (song.artworkPath.isNotEmpty()) {
            val artworkUri = song.artworkPath.toUri()
            AsyncImage(
                model = artworkUri,
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.image),
                contentDescription = "Default Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentlyPlayedSection(songs: List<Song>?, onSongClick: (Song) -> Unit) {
    if (songs == null || songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recently played songs.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            songs.forEach { song ->
                RecentlyPlayedItem(song, onSongClick = onSongClick)
            }
        }
    }
}

@Composable
fun RecentlyPlayedItem(song: Song, onSongClick: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { onSongClick(song) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (song.artworkPath.isNotEmpty()) {
            val artworkUri = song.artworkPath.toUri()
            AsyncImage(
                model = artworkUri,
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(50.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.image),
                contentDescription = "Default Album Art",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(50.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OnlineSongsSection(
    onFiftyGlobalClick: () -> Unit,
    onTop10CountryClick: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OnlineSongItem(
                title = "Fifty Global",
                description = "Daily update of world's most played songs",
                onClick = onFiftyGlobalClick,
                backgroundColor = Color(0xFF1E88E5), // Blue
                textOverlay = "Top 50\nGlobal"
            )
        }

        item {
            OnlineSongItem(
                title = "Top 10 Country",
                description = "Best hits from your country",
                onClick = onTop10CountryClick,
                backgroundColor = Color(0xFFE53935), // Red
                textOverlay = "Top 10\nCountry"
            )
        }
    }
}

@Composable
fun OnlineSongItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textOverlay: String
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = textOverlay,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QRScanSuccessNotification(
    song: Song,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .border(
                width = 1.dp,
                color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QR Code Scanned Successfully!",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Now playing: ${song.title}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${song.artistName}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}