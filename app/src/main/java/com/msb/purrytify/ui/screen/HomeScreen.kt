package com.msb.purrytify.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.navigation.NavController
import com.msb.purrytify.ui.navigation.Screen
import androidx.core.net.toUri


@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navController: NavController? = null,
    onScanQRCode: () -> Unit
) {
    val recentlyPlayedState: State<List<Song>> =
        homeViewModel.recentlyPlayedSongs.observeAsState(initial = emptyList())
    val newSongsState: State<List<Song>> =
        homeViewModel.newSongs.observeAsState(initial = emptyList())

    val recentlyPlayed: List<Song> = recentlyPlayedState.value
    val newSongs: List<Song> = newSongsState.value

    val onClickedRecent: (Song) -> Unit = { song ->
        homeViewModel.playRecentSongs(recentlyPlayed, song)
        playerViewModel.setMiniPlayerVisible(true)
    }

    val onClickedNew: (Song) -> Unit = { song ->
        homeViewModel.playNewSongs(newSongs, song)
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
                            text = "Online Songs",
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
            text = song.artist,
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
                text = song.artist,
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
                imageRes = R.drawable.fiftyglobal,
                title = "Fifty Global",
                description = "Daily update of world's most played songs",
                onClick = onFiftyGlobalClick
            )
        }

        item {
            OnlineSongItem(
                imageRes = R.drawable.fiftyglobal, // Using same image for now
                title = "Top 10 Country",
                description = "Best hits from your country",
                onClick = onTop10CountryClick
            )
        }
    }
}

@Composable
fun OnlineSongItem(
    imageRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(90.dp)
                .clip(MaterialTheme.shapes.small)
        )

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