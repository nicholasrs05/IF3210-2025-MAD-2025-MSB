package com.msb.purrytify.data.local.dao

import androidx.room.*
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.converter.DateTimeConverter
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(DateTimeConverter::class)
interface SoundCapsuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundCapsule(soundCapsule: SoundCapsule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayStreak(dayStreak: DayStreak): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyListeningTime(dailyListeningTime: DailyListeningTime): Long

    @Update
    suspend fun updateSoundCapsule(soundCapsule: SoundCapsule)

    @Update
    suspend fun updateDayStreak(dayStreak: DayStreak)

    @Update
    suspend fun updateDailyListeningTime(dailyListeningTime: DailyListeningTime)

    @Query("SELECT * FROM sound_capsules WHERE ownerId = :ownerId ORDER BY year DESC, month DESC")
    fun getAllSoundCapsules(ownerId: Long): Flow<List<SoundCapsule>>

    @Query("SELECT * FROM sound_capsules WHERE id = :id")
    suspend fun getSoundCapsuleById(id: Long): SoundCapsule?

    @Query("SELECT * FROM day_streaks WHERE soundCapsuleId = :soundCapsuleId")
    fun getDayStreaksForCapsule(soundCapsuleId: Long): Flow<List<DayStreak>>

    @Query("SELECT * FROM daily_listening_times WHERE soundCapsuleId = :soundCapsuleId ORDER BY date ASC")
    fun getDailyListeningTimesForCapsule(soundCapsuleId: Long): Flow<List<DailyListeningTime>>

    @Query("SELECT * FROM sound_capsules WHERE ownerId = :ownerId AND year = :year AND month = :month")
    suspend fun getSoundCapsuleByMonth(ownerId: Long, year: Int, month: String): SoundCapsule?

    @Query("DELETE FROM sound_capsules WHERE id = :id")
    suspend fun deleteSoundCapsule(id: Long)

    @Query("DELETE FROM day_streaks WHERE soundCapsuleId = :soundCapsuleId")
    suspend fun deleteDayStreaksForCapsule(soundCapsuleId: Long)

    @Query("DELETE FROM daily_listening_times WHERE soundCapsuleId = :soundCapsuleId")
    suspend fun deleteDailyListeningTimesForCapsule(soundCapsuleId: Long)
} 