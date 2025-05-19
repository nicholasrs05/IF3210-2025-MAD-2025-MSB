package com.msb.purrytify.ui.component.soundcapsule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msb.purrytify.data.local.entity.DayStreak

@Composable
fun DayStreaksSection(
    streak: DayStreak? = null,
    dayStreaks: List<DayStreak>? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Longest Streak",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        when {
            streak != null -> {
                StreakItem(streak = streak)
            }
            dayStreaks != null -> {
                dayStreaks.forEach { streak ->
                    StreakItem(streak = streak)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StreakItem(streak: DayStreak) {
    Text(
        text = "Song ID: ${streak.songId}",
        color = Color.White,
        fontSize = 11.sp
    )
    Text(
        text = "${streak.streakDays} days streak",
        color = Color(0xFFB3B3B3),
        fontSize = 11.sp
    )
}
