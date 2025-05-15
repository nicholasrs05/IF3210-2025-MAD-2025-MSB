package com.msb.purrytify.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.viewmodel.SoundCapsuleViewModel
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.format.DateTimeFormatter

@Composable
fun TimeListenedScreen(
    month: String,
    year: Int,
    onBackClick: () -> Unit,
    soundCapsuleViewModel: SoundCapsuleViewModel = hiltViewModel()
) {
    val dailyListeningTimes by soundCapsuleViewModel.dailyListeningTimesState.collectAsState()
    val isLoading by soundCapsuleViewModel.isLoading.collectAsState()
    val errorState by soundCapsuleViewModel.error.collectAsState()
    val errorMessage = errorState ?: ""

    LaunchedEffect(month, year) {
        soundCapsuleViewModel.loadSoundCapsule(month, year)
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
                text = "Time Listened",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            // Date
            Text(
                text = "$month $year",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Title
            val totalMinutes = dailyListeningTimes.sumOf { it.minutes }
            Text(
                text = "You listened to music for $totalMinutes minutes this month.",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                // Daily Average Text
                val dailyAvg = calculateDailyAverage(dailyListeningTimes)
                Text(
                    text = "Daily average: $dailyAvg min",
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    errorMessage.isNotEmpty() -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    dailyListeningTimes.isEmpty() -> {
                        Text(
                            text = "No listening data available",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        // Line Chart only
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxSize()
                            ) {
                                Chart(
                                    chart = lineChart(
                                        lines = listOf(
                                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                                                lineColor = Color(0xFF1DB954)
                                            )
                                        )
                                    ),
                                    model = entryModelOf(dailyListeningTimes.mapIndexed { index, time ->
                                        entryOf(index.toFloat(), time.minutes.toFloat())
                                    }),
                                    modifier = Modifier.fillMaxSize(),
                                    startAxis = rememberStartAxis(
                                        valueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                                            "${value.toInt()}"
                                        }
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                                            val date = dailyListeningTimes[value.toInt()].date.toLocalDate()
                                            date.format(DateTimeFormatter.ofPattern("dd"))
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDailyAverage(dailyListeningTimes: List<DailyListeningTime>): Int {
    if (dailyListeningTimes.isEmpty()) return 0
    return dailyListeningTimes.map { it.minutes }.average().toInt()
} 