package com.msb.purrytify.ui.component.soundcapsule

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.ui.navigation.Screen
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import com.msb.purrytify.utils.DateUtil
import com.msb.purrytify.utils.FileShareUtil
import com.msb.purrytify.utils.FileDownloadUtil
import com.msb.purrytify.viewmodel.SoundCapsuleViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewModelScope


@Composable
fun SoundCapsuleSection(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SoundCapsuleViewModel = hiltViewModel(),
) {
    fun shareSoundCapsule(
        capsuleToShare: SoundCapsule?,
        soundCapsuleViewModel: SoundCapsuleViewModel,
        context: Context
    ) {
        val csvData = soundCapsuleViewModel.exportToCSV(capsuleToShare)
        if (csvData.startsWith("No SoundCapsule data")) {
            Log.w("ProfileScreen", "Attempted to share but no capsule data: $csvData")
        } else {
            val fileName = "sound_capsule_${capsuleToShare?.month}_${capsuleToShare?.year}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.csv"
            FileShareUtil.shareCsvFile(context, csvData, fileName)
        }
    }

    fun downloadAllSoundCapsules(
        soundCapsuleViewModel: SoundCapsuleViewModel,
        context: Context
    ) {
        soundCapsuleViewModel.downloadAllSoundCapsules(context)
    }

    val context = LocalContext.current
    val soundCapsulesState by viewModel.soundCapsulesState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAllSoundCapsules()
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading && soundCapsulesState.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text(
                    text = error ?: "An unknown error occurred",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
            soundCapsulesState.isNotEmpty() -> {
                SoundCapsuleTitle(
                    onDownloadAll = { downloadAllSoundCapsules(viewModel, context) }
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(soundCapsulesState) { capsule ->
                        SoundCapsuleCard(
                            soundCapsule = capsule,
                            viewModel = viewModel,
                            onShare = { shareSoundCapsule(capsule, viewModel, context) },
                            onTopArtistClick = { navController.navigate(Screen.TopArtists.createRoute(capsule.id)) },
                            onTopSongClick = { navController.navigate(Screen.TopSongs.createRoute(capsule.id))  },
                            onTimeListenedClick = { navController.navigate(Screen.TimeListened.createRoute(capsule.id)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            else -> {
                EmptySoundCapsuleState()
            }
        }
    }
}

@Composable
private fun EmptySoundCapsuleState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No Sound Capsule Available",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your listening history will appear here",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SoundCapsuleTitle(
    onDownloadAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Sound Capsule",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDownloadAll) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Download All",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SoundCapsuleCard(
    soundCapsule: SoundCapsule,
    viewModel: SoundCapsuleViewModel,
    onShare: () -> Unit,
    onTopArtistClick: () -> Unit,
    onTopSongClick: () -> Unit,
    onTimeListenedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var longestStreak by remember { mutableStateOf<DayStreak?>(null) }
    var isLoadingStreak by remember { mutableStateOf(false) }
    val streakSong by viewModel.streakSong.collectAsState()
    val streakArtist by viewModel.streakArtist.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(soundCapsule.id) {
        isLoadingStreak = true
        coroutineScope.launch {
            longestStreak = viewModel.getLongestDayStreak(soundCapsule.id)
            isLoadingStreak = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header - Outside the box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${DateUtil.getMonthString(soundCapsule.month)} ${soundCapsule.year}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color(0xFFB3B3B3)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main content box
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Time Listened
            TimeListenedSection(
                minutes = soundCapsule.timeListenedMinutes,
                onClick = onTimeListenedClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Top Artist and Top Song
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TopArtistSection(
                    artist = "${streakArtist?.name}",
                    modifier = Modifier.weight(1f),
                    onClick = onTopArtistClick
                )
                TopSongSection(
                    songTitle = "${streakSong?.title}",
                    modifier = Modifier.weight(1f),
                    onClick = onTopSongClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Longest Day Streak
            when {
                isLoadingStreak -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1DB954)
                        )
                    }
                }
                longestStreak != null && longestStreak?.streakDays!! >= 2 -> {
                    DayStreaksSection(
                        streak = longestStreak,
                        song = streakSong,
                        artist = streakArtist,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}



