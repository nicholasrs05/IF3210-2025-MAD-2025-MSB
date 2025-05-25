package com.msb.purrytify.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.msb.purrytify.ui.component.LibraryAdapter
import com.msb.purrytify.ui.theme.AppTheme
import com.msb.purrytify.viewmodel.LibraryViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.zIndex

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val showAddSongSheet by libraryViewModel.showAddSongSheet.collectAsState()
    val playbackError by libraryViewModel.playbackError.collectAsState()
    
    // Show error messages when playback errors occur
    LaunchedEffect(playbackError) {
        playbackError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            libraryViewModel.clearPlaybackError()
        }
    }
    
    if (showAddSongSheet) {
        AddSongScreen(
            showBottomSheet = true,
            onDismiss = { libraryViewModel.toggleAddSongSheet(false) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(Color(0xFF121212)),
        ) {
            Header(showAddSongSheet = { libraryViewModel.toggleAddSongSheet(true) })

            var selectedTab by remember { mutableIntStateOf(0) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(Color(0xFF121212))
                    .zIndex(1f),
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
                FilterButton(
                    label = "Downloaded",
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }

            val allSongs by libraryViewModel.allSongs.observeAsState(initial = emptyList())
            val likedSongs by libraryViewModel.likedSongs.observeAsState(initial = emptyList())
            val downloadedSongs by libraryViewModel.downloadedSongs.observeAsState(initial = emptyList())

            val songsToDisplay = when (selectedTab) {
                0 -> allSongs
                1 -> likedSongs
                2 -> downloadedSongs
                else -> allSongs
            }

            if (songsToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No songs in your library" else if (selectedTab == 1) "No liked songs" else "No downloaded songs",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AndroidView(
                    factory = { ctx: Context ->
                        RecyclerView(ctx).apply {
                            layoutManager = LinearLayoutManager(ctx)
                            layoutParams = RecyclerView.LayoutParams(
                                RecyclerView.LayoutParams.MATCH_PARENT,
                                RecyclerView.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { recyclerView ->
                        recyclerView.adapter = LibraryAdapter(songsToDisplay) { clickedSong ->
                            val songIndex = songsToDisplay.indexOfFirst { it.id == clickedSong.id }
                            if (songIndex >= 0) {
                                Log.d("LibraryScreen", "Playing song at index: $songIndex")
                                libraryViewModel.playLibrarySong(songsToDisplay, clickedSong)
                            } else {
                                Log.d("LibraryScreen", "Playing song directly: ${clickedSong.title}")
                                libraryViewModel.playSong(clickedSong)
                            }
                            playerViewModel.setMiniPlayerVisible(true)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun Header(showAddSongSheet: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
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
            containerColor = if (isSelected) Color(0xFF1DB954) else Color(0xFF212121),
            contentColor = if (isSelected) Color(0xFF121212) else Color(0xFFFFFFFF)
        )
    ) {
        Text(text = label)
    }
}

@Preview
@Composable
fun LibraryScreenPreview() {
    AppTheme {
        LibraryScreen()
    }
}