package com.msb.purrytify.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.msb.purrytify.ui.component.LibraryAdapter
import com.msb.purrytify.ui.theme.AppTheme
import com.msb.purrytify.viewmodel.PlaybackViewModel
import com.msb.purrytify.viewmodel.SongViewModel

@Composable
fun LibraryScreen(
    navController: NavController = rememberNavController(),
    songViewModel: SongViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
) {
    val mediaPlayerManager = playbackViewModel.mediaPlayerManager

    var showAddSongSheet by remember { mutableStateOf(false) }
    var showPlayerSheet by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<com.msb.purrytify.data.local.entity.Song?>(null) }

    if (showAddSongSheet) {
        AddSongScreen(
            navController = navController,
            showBottomSheet = true,
            onDismiss = { showAddSongSheet = false }
        )
    }
    
    val cachedPlayerScreen = remember {
        mutableStateOf<@Composable (() -> Unit)?>(null)
    }
    
    LaunchedEffect(selectedSong) {
        selectedSong?.let {
            cachedPlayerScreen.value = {
                PlayerScreen(
                    song = selectedSong!!,
                    onDismiss = { showPlayerSheet = false },
                )
            }
        }
    }
    
    AnimatedVisibility(
        visible = showPlayerSheet && selectedSong != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300)),
        modifier = Modifier.zIndex(10f)
    ) {
        cachedPlayerScreen.value?.invoke()
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
            Header(showAddSongSheet = { showAddSongSheet = true })

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

            val allSongs by songViewModel.allSongs.observeAsState(initial = emptyList())
            val likedSongs by songViewModel.likedSongs.observeAsState(initial = emptyList())

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
                AndroidView(
                    factory = { ctx: Context ->
                        RecyclerView(ctx).apply {
                            layoutManager = LinearLayoutManager(ctx)
                            adapter = LibraryAdapter(songsToDisplay) { clickedSong ->
                                // Set the selected song and show player
                                selectedSong = clickedSong
                                showPlayerSheet = true
                                
                                // Mark as played
                                songViewModel.markAsPlayed(clickedSong.id)
                            }
                            layoutParams = RecyclerView.LayoutParams(
                                RecyclerView.LayoutParams.MATCH_PARENT,
                                RecyclerView.LayoutParams.MATCH_PARENT
                            )
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