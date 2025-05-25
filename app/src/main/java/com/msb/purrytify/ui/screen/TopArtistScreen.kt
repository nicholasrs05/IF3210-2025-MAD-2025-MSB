package com.msb.purrytify.ui.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import coil3.compose.rememberAsyncImagePainter
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.viewmodel.SoundCapsuleViewModel
import java.util.Locale

@Composable
fun TopArtistScreen(
    soundCapsuleId: Long,
    onBackClick: () -> Unit,
    viewModel: SoundCapsuleViewModel = hiltViewModel()
) {
    val soundCapsule by viewModel.currentSoundCapsule.collectAsStateWithLifecycle()
    val artists by viewModel.topArtists.collectAsStateWithLifecycle()
    val totalArtistCount by viewModel.totalArtistCount.collectAsStateWithLifecycle()
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
                text = "Top Artists",
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
            artists.isEmpty() -> {
                EmptyTopArtistState()
            }
            else -> {
                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    item {
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
                                append("You listened to ")
                                withStyle(style = SpanStyle(color = Color(0xFF669BEC))) {
                                    append("${totalArtistCount} artists")
                                }
                                append(" this month.")
                            },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Artist List
                    itemsIndexed(artists) { index, artist ->
                        ArtistItem(
                            artist = artist,
                            rank = index + 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(108.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTopArtistState() {
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
                text = "No Artist Data Available",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your top artists will appear here",
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ArtistItem(
    artist: Artist,
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
            // Artist Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank
                Text(
                    text = String.format(Locale.getDefault(),"%02d", rank),
                    color = Color(0xFF669BEC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                // Artist Name
                Text(
                    text = artist.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Artist Image
            Image(
                painter = if (artist.imageUrl != null && !artist.imageUrl.isEmpty() ) {
                    rememberAsyncImagePainter(artist.imageUrl)
                } else {
                    painterResource(id = R.drawable.image)
                },
                contentDescription = "Artist image",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
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