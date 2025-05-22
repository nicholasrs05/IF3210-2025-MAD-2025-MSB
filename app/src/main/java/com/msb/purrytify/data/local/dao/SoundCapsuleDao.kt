package com.msb.purrytify.data.local.dao

import androidx.room.*
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.converter.DateTimeConverter
import com.msb.purrytify.data.local.entity.MonthlySongPlayCount
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.LocalDate

@Dao
@TypeConverters(DateTimeConverter::class)
interface SoundCapsuleDao {
    // SoundCapsule functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundCapsule(soundCapsule: SoundCapsule): Long

    @Update
    suspend fun updateSoundCapsule(soundCapsule: SoundCapsule)

    @Query("SELECT * FROM sound_capsules WHERE ownerId = :ownerId ORDER BY year DESC, month DESC")
    fun getAllSoundCapsules(ownerId: Long): Flow<List<SoundCapsule>>

    @Query("SELECT * FROM sound_capsules WHERE id = :id")
    suspend fun getSoundCapsuleById(id: Long): SoundCapsule?

    @Query("SELECT * FROM sound_capsules WHERE ownerId = :ownerId AND year = :year AND month = :month")
    suspend fun getSoundCapsuleByMonth(ownerId: Long, year: Int, month: Int): SoundCapsule?

    @Query("DELETE FROM sound_capsules WHERE id = :id")
    suspend fun deleteSoundCapsule(id: Long)

    // Day Streak functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayStreak(dayStreak: DayStreak): Long

    @Update
    suspend fun updateDayStreak(dayStreak: DayStreak)

    @Query("SELECT * FROM day_streaks WHERE soundCapsuleId = :soundCapsuleId")
    fun getDayStreaksForCapsule(soundCapsuleId: Long): Flow<List<DayStreak>>

    @Query("SELECT * FROM day_streaks WHERE id = :id")
    suspend fun getDayStreakById(id: Long): DayStreak?

    @Query("SELECT * FROM day_streaks WHERE soundCapsuleId = :soundCapsuleId AND startDate <= :date AND endDate >= :date")
    suspend fun getDayStreakByDate(soundCapsuleId: Long, date: LocalDateTime): DayStreak?

    @Query("DELETE FROM day_streaks WHERE soundCapsuleId = :soundCapsuleId")
    suspend fun deleteDayStreaksForCapsule(soundCapsuleId: Long)

    @Query("DELETE FROM day_streaks WHERE id = :id")
    suspend fun deleteDayStreakById(id: Long)

    // Daily Listening Time functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyListeningTime(dailyListeningTime: DailyListeningTime): Long

    @Update
    suspend fun updateDailyListeningTime(dailyListeningTime: DailyListeningTime)

    @Query("SELECT * FROM daily_listening_times WHERE soundCapsuleId = :soundCapsuleId ORDER BY date ASC")
    fun getDailyListeningTimesForCapsule(soundCapsuleId: Long): Flow<List<DailyListeningTime>>

    @Query("SELECT * FROM daily_listening_times WHERE id = :id")
    suspend fun getDailyListeningTimeById(id: Long): DailyListeningTime?

    @Query("SELECT * FROM daily_listening_times WHERE soundCapsuleId = :soundCapsuleId AND date = :date")
    suspend fun getDailyListeningTimeByDate(soundCapsuleId: Long, date: LocalDate): DailyListeningTime?

    @Query("DELETE FROM daily_listening_times WHERE soundCapsuleId = :soundCapsuleId")
    suspend fun deleteDailyListeningTimesForCapsule(soundCapsuleId: Long)

    @Query("DELETE FROM daily_listening_times WHERE id = :id")
    suspend fun deleteDailyListeningTimeById(id: Long)

    // Monthly Song Play Count functions
    @Insert
    suspend fun insertMonthlyPlayCount(monthlyPlayCount: MonthlySongPlayCount): Long

    @Update
    suspend fun updateMonthlyPlayCount(monthlyPlayCount: MonthlySongPlayCount)

    @Query("SELECT * FROM monthly_song_play_counts WHERE songId = :songId AND soundCapsuleId = :soundCapsuleId")
    suspend fun getMonthlyPlayCount(songId: Long, soundCapsuleId: Long): MonthlySongPlayCount?

    @Query("UPDATE monthly_song_play_counts SET playCount = playCount + 1, lastUpdated = :timestamp WHERE songId = :songId AND soundCapsuleId = :soundCapsuleId")
    suspend fun incrementMonthlyPlayCount(songId: Long, soundCapsuleId: Long, timestamp: Long)

    // Top 5 queries
    @Query("SELECT * FROM artists WHERE id = :artistId")
    suspend fun getArtistById(artistId: Long): Artist?

    @Query("""
        SELECT a.* FROM artists a
        INNER JOIN songs s ON a.id = s.artistId
        INNER JOIN monthly_song_play_counts mspc ON s.id = mspc.songId
        WHERE mspc.soundCapsuleId = :soundCapsuleId
        GROUP BY a.id
        ORDER BY SUM(mspc.playCount) DESC
        LIMIT 5
    """)
    suspend fun getTop5Artists(soundCapsuleId: Long): List<Artist>

    @Query("""
        SELECT s.*
        FROM songs s
        INNER JOIN monthly_song_play_counts mspc ON s.id = mspc.songId
        WHERE mspc.soundCapsuleId = :soundCapsuleId
        ORDER BY mspc.playCount DESC
        LIMIT 5
    """)
    suspend fun getTop5Songs(soundCapsuleId: Long): List<Song>
} 