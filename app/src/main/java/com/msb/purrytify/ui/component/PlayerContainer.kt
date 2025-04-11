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
import com.msb.purrytify.viewmodel.PlaybackViewModel
import androidx.activity.compose.BackHandler

@Composable
fun PlayerContainer(
    content: @Composable () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    var showFullPlayer by remember { mutableStateOf(false) }

    val currentSong by playerViewModel.currentSong
    val isPlaying by playerViewModel.isPlaying

    var selectedSong by remember { mutableStateOf<Song?>(null) }

    DisposableEffect(playbackViewModel) {
        val songChangeListener = object : MediaPlayerManager.SongChangeListener {
            override fun onSongChanged(newSong: Song) {
                playerViewModel.updateCurrentSong()
            }

            override fun onPlayerReleased() {
                playerViewModel.resetCurrentSong()
            }
        }

        playbackViewModel.mediaPlayerManager.addSongChangeListener(songChangeListener)

        onDispose {
            playbackViewModel.mediaPlayerManager.removeSongChangeListener(songChangeListener)
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

        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
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
                                playbackViewModel.mediaPlayerManager.getCurrentSong() ?: song
                            selectedSong = playingSong
                            showFullPlayer = true
                        }
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
                onDismiss = { showFullPlayer = false },
                onDismissWithAnimation = {
                    isDismissingPlayer = true
                },
                isDismissing = isDismissingPlayer,
                onAnimationComplete = {
                    isDismissingPlayer = false
                    showFullPlayer = false
                },
                viewModel = playerViewModel,
                playbackViewModel = playbackViewModel
            )
        }
    }
}

fun showPlayer(song: Song, viewModel: PlayerViewModel) {
    viewModel.playSong(song)
}
