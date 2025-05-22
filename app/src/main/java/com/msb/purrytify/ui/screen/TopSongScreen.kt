package com.msb.purrytify.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.viewmodel.SoundCapsuleViewModel
import java.util.Locale

@Composable
fun TopSongScreen(
    soundCapsuleId: Long,
    onBackClick: () -> Unit,
    viewModel: SoundCapsuleViewModel = hiltViewModel()
) {
    val soundCapsule by viewModel.currentSoundCapsule.collectAsStateWithLifecycle()
    val songs by viewModel.topSongs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(soundCapsuleId) {
        viewModel.loadSoundCapsuleDetails(soundCapsuleId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(8.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(top = 8.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFB3B3B3)
                )
            }
            Text(
                text = "Top Songs",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            }
            songs.isEmpty() -> {
                EmptyTopSongState()
            }
            else -> {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    // Date
                    Text(
                        text = viewModel.getMonthYearString(soundCapsule),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    // Title
                    Text(
                        text = buildAnnotatedString {
                            append("You played ")
                            withStyle(style = SpanStyle(color = Color(0xFFF8E747))) {
                                append("${songs.size} different songs")
                            }
                            append(" this month.")
                        },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Song List
                    songs.forEachIndexed { index, song ->
                        SongItem(
                            song = song,
                            rank = index + 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(108.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTopSongState() {
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
                text = "No Song Data Available",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your top songs will appear here",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    rank: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Song Info
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // Rank and Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", rank),
                        color = Color(0xFFF8E747),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Artist
                Text(
                    text = song.artistName,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 35.dp, top = 2.dp)
                )

                // Play Count
                Text(
                    text = "${song.playCount} plays",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 35.dp, top = 4.dp)
                )
            }

            // Song Image
            Image(
                painter = painterResource(id = R.drawable.image),
                contentDescription = "Song image",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Bottom Border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF535353))
                .align(Alignment.BottomCenter)
        )
    }
} 