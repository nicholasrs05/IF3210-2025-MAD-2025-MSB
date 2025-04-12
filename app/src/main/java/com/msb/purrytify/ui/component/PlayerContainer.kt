package com.msb.purrytify.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msb.purrytify.media.MediaPlayerManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.ui.screen.PlayerScreen
import com.msb.purrytify.viewmodel.PlayerViewModel
import androidx.activity.compose.BackHandler

@Composable
fun PlayerContainer(
    content: @Composable () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isLandscape: Boolean = false
) {
    var showFullPlayer by remember { mutableStateOf(false) }

    val currentSong by playerViewModel.currentSong
    val isPlaying by playerViewModel.isPlaying
    val isMiniPlayerVisible by playerViewModel.isMiniPlayerVisible

    var selectedSong by remember { mutableStateOf<Song?>(null) }

    DisposableEffect(playerViewModel) {
        val songChangeListener = object : MediaPlayerManager.SongChangeListener {
            override fun onSongChanged(newSong: Song) {
                playerViewModel.updateCurrentSong()
                if (!showFullPlayer) {
                    playerViewModel.setMiniPlayerVisible(true)
                }
            }

            override fun onPlayerReleased() {
                playerViewModel.resetCurrentSong()
            }
        }

        playerViewModel.mediaPlayerManager.addSongChangeListener(songChangeListener)

        onDispose {
            playerViewModel.mediaPlayerManager.removeSongChangeListener(songChangeListener)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues)
            ) {
                content()
            }
        }

        if (currentSong != null && isMiniPlayerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!isLandscape) Modifier.padding(bottom = 80.dp) else Modifier.padding(bottom = 0.dp))
            ) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                        onPlayerClick = { song ->
                            val playingSong =
                                playerViewModel.mediaPlayerManager.getCurrentSong() ?: song
                            selectedSong = playingSong
                            showFullPlayer = true
                            playerViewModel.setLargePlayerVisible(true)
                        },
                        isLandscape = isLandscape
                    )
                }
            }
        }

        var isDismissingPlayer by remember { mutableStateOf(false) }

        if (showFullPlayer) {
            BackHandler {
                isDismissingPlayer = true
            }
        }

        if (showFullPlayer && selectedSong != null) {
            PlayerScreen(
                song = selectedSong!!,
                onDismiss = {
                    showFullPlayer = false
                    playerViewModel.setMiniPlayerVisible(currentSong != null)
                },
                onDismissWithAnimation = {
                    isDismissingPlayer = true
                },
                isDismissing = isDismissingPlayer,
                onAnimationComplete = {
                    isDismissingPlayer = false
                    showFullPlayer = false
                    playerViewModel.setMiniPlayerVisible(currentSong != null)
                },
                viewModel = playerViewModel,
            )
        }
    }
}