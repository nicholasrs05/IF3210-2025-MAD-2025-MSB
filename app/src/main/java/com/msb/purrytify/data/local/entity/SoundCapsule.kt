package com.msb.purrytify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.msb.purrytify.data.local.converter.DateTimeConverter
import java.time.LocalDateTime

@Entity(tableName = "sound_capsules")
@TypeConverters(DateTimeConverter::class)
data class SoundCapsule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val timeListenedMinutes: Int,
    val topArtistId: Long,
    val topSongId: Long,
    val lastUpdated: LocalDateTime,
    val ownerId: Long
)

@Entity(tableName = "day_streaks")
@TypeConverters(DateTimeConverter::class)
data class DayStreak(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val soundCapsuleId: Long,
    val songId: Long,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val streakDays: Int
)

@Entity(tableName = "daily_listening_times")
@TypeConverters(DateTimeConverter::class)
data class DailyListeningTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val soundCapsuleId: Long,
    val date: LocalDateTime,
    val minutes: Int
)

@Entity(tableName = "monthly_song_play_counts")
@TypeConverters(DateTimeConverter::class)
data class MonthlySongPlayCount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val soundCapsuleId: Long,
    val playCount: Int = 0,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
