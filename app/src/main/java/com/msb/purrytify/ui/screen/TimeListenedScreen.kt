package com.msb.purrytify.ui.screen

import android.text.SpannableString
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msb.purrytify.viewmodel.SoundCapsuleViewModel
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
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
    soundCapsuleId: Long,
    onBackClick: () -> Unit,
    viewModel: SoundCapsuleViewModel = hiltViewModel()
) {
    val soundCapsule by viewModel.currentSoundCapsule.collectAsStateWithLifecycle()
    val dailyListeningTimes by viewModel.dailyListeningTimes.collectAsStateWithLifecycle()
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
                text = "Time Listened",
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
            dailyListeningTimes.isEmpty() -> {
                EmptyTimeListenedState()
            }
            else -> {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
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

                        // Line Chart
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
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
                                        },
                                        label = axisLabelComponent(color = Color.White),
                                        labelRotationDegrees = 0f,
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                                            val date = dailyListeningTimes[value.toInt()].date
                                            date.format(DateTimeFormatter.ofPattern("dd"))
                                        },
                                        label = axisLabelComponent(color = Color.White),
                                        labelRotationDegrees = 0f
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyTimeListenedState() {
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
                text = "No Listening Data Available",
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

private fun calculateDailyAverage(dailyListeningTimes: List<DailyListeningTime>): Int {
    if (dailyListeningTimes.isEmpty()) return 0
    return dailyListeningTimes.map { it.minutes }.average().toInt()
} 