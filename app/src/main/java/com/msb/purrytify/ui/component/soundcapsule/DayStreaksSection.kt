package com.msb.purrytify.ui.component.soundcapsule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.local.entity.Artist
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

@Composable
fun DayStreaksSection(
    streak: DayStreak? = null,
    song: Song? = null,
    artist: Artist? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
    ) {
        // Song artwork at the top
        song?.artworkPath?.let { artworkPath ->
            AsyncImage(
                model = artworkPath,
                contentDescription = "Song artwork",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Text content
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "You had a ${streak?.streakDays ?: 0}-day streak",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val songTitle = song?.title ?: "Unknown Song"
            val artistName = artist?.name ?: "Unknown Artist"
            Text(
                text = "You played $songTitle by $artistName day after day. You were on fire",
                color = Color(0xFFB3B3B3),
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
            val startDate = streak?.startDate?.format(dateFormatter) ?: ""
            val endDate = streak?.endDate?.format(dateFormatter) ?: ""
            val year = streak?.endDate?.year ?: LocalDateTime.now().year
            Text(
                text = "$startDate-$endDate, $year",
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            )
        }
    }
}
