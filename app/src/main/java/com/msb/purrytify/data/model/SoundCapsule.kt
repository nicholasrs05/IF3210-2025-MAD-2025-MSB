package com.msb.purrytify.data.model

import java.time.LocalDateTime

data class SoundCapsule(
    val month: String,
    val year: Int,
    val timeListenedMinutes: Int,
    val topArtist: String,
    val topSong: Song,
    val dayStreaks: List<DayStreak>,
    val dailyListeningTimes: List<DailyListeningTime>,
    val lastUpdated: LocalDateTime
)

data class DayStreak(
    val songTitle: String,
    val artist: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val streakDays: Int
)

data class DailyListeningTime(
    val date: LocalDateTime,
    val minutes: Int
) 