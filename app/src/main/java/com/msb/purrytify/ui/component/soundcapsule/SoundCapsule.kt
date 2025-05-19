package com.msb.purrytify.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.SoundCapsule

@Composable
fun SoundCapsuleCard(
    soundCapsule: SoundCapsule,
    dayStreaks: List<DayStreak>,
    onShare: () -> Unit,
    onTopArtistClick: () -> Unit,
    onTopSongClick: () -> Unit,
    onTimeListenedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "${soundCapsule.month} ${soundCapsule.year}",
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
                    artist = soundCapsule.topArtist,
                    modifier = Modifier.weight(1f),
                    onClick = onTopArtistClick
                )
                TopSongSection(
                    songTitle = soundCapsule.topSong,
                    modifier = Modifier.weight(1f),
                    onClick = onTopSongClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day Streaks
            DayStreaksSection(dayStreaks)
        }
    }
}

@Composable
private fun TimeListenedSection(
    minutes: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
            .padding(12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time listened",
                color = Color.White,
                fontSize = 8.sp
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View more",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "$minutes minutes",
            color = Color(0xFF1DB954),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TopArtistSection(
    artist: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
            .padding(12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top artist",
                color = Color.White,
                fontSize = 8.sp
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View more",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = artist,
            color = Color(0xFF669BEC),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = "Artist album cover",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun TopSongSection(
    songTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
            .padding(12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top song",
                color = Color.White,
                fontSize = 8.sp
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View more",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = songTitle,
            color = Color(0xFFF8E747),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = "Song album cover",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DayStreaksSection(dayStreaks: List<DayStreak>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Streak",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        dayStreaks.forEach { streak ->
            Text(
                text = "${streak.songTitle} by ${streak.artist}",
                color = Color.White,
                fontSize = 11.sp
            )
            Text(
                text = "${streak.streakDays} days streak",
                color = Color(0xFFB3B3B3),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
} 