package com.msb.purrytify.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.ui.theme.AppTheme
import com.msb.purrytify.viewmodel.SongViewModel
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun LibraryScreen(
    navController: NavController = rememberNavController(),
    songViewModel: SongViewModel = viewModel()
) {
    // State for controlling the add song bottom sheet
    var showAddSongSheet by remember { mutableStateOf(false) }
    // Show the AddSongScreen as a bottom sheet when showAddSongSheet is true
    if (showAddSongSheet) {
        AddSongScreen(
            navController = navController,
            showBottomSheet = true,
            onDismiss = { showAddSongSheet = false }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        Header(navController, showAddSongSheet = { showAddSongSheet = true })
        
        // State of which tab is selected (All or Liked)
        var selectedTab by remember { mutableStateOf(0) }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            FilterButton(
                label = "All", 
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            FilterButton(
                label = "Liked", 
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
        }
        
        // Observe song lists
        val allSongs by songViewModel.allSongs.observeAsState(initial = emptyList())
        val likedSongs by songViewModel.likedSongs.observeAsState(initial = emptyList())
        
        // Display songs based on selected tab
        val songsToDisplay = if (selectedTab == 0) allSongs else likedSongs
        
        if (songsToDisplay.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTab == 0) "No songs in your library" else "No liked songs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                items(songsToDisplay.size) { index ->
                    SongItem(
                        song = songsToDisplay[index],
                        onSongClick = {
                            // Handle song click (play song)
                            songViewModel.markAsPlayed(songId = songsToDisplay[index].id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Header(navController: NavController, showAddSongSheet: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your Library",
                fontSize = 24.sp,
                fontWeight = FontWeight.W800
            )
            IconButton(onClick = { showAddSongSheet() }) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Add Song",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun FilterButton(label: String, isSelected: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = { onClick() },
        modifier = Modifier.padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = label)
    }
}

@Composable
fun SongItem(song: Song, onSongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onSongClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (song.artworkPath.isNotEmpty() && File(song.artworkPath).exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(File(song.artworkPath)),
                        contentDescription = "Album Artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.library),
                        contentDescription = "Music Icon",
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
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
            
            // Duration
            val minutes = TimeUnit.MILLISECONDS.toMinutes(song.duration)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(song.duration) - 
                    TimeUnit.MINUTES.toSeconds(minutes)
            Text(
                text = "${minutes}:${String.format("%02d", seconds)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun LibraryScreenPreview() {
    AppTheme {
        LibraryScreen()
    }
}
