package com.msb.purrytify.data.repository

import com.msb.purrytify.data.local.dao.SoundCapsuleDao
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.MonthlySongPlayCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime

@Singleton
class SoundCapsuleRepository @Inject constructor(
    private val soundCapsuleDao: SoundCapsuleDao
) {
    suspend fun createSoundCapsule(soundCapsule: SoundCapsule): Long {
        return soundCapsuleDao.insertSoundCapsule(soundCapsule)
    }

    suspend fun updateSoundCapsule(soundCapsule: SoundCapsule) {
        soundCapsuleDao.updateSoundCapsule(soundCapsule)
    }

    fun getAllSoundCapsules(ownerId: Long): Flow<List<SoundCapsule>> {
        return soundCapsuleDao.getAllSoundCapsules(ownerId)
    }

    suspend fun getSoundCapsuleById(id: Long): SoundCapsule? {
        return soundCapsuleDao.getSoundCapsuleById(id)
    }

    suspend fun getSoundCapsuleByMonth(ownerId: Long, year: Int, month: Int): SoundCapsule? {
        return soundCapsuleDao.getSoundCapsuleByMonth(ownerId, year, month)
    }

    suspend fun deleteSoundCapsule(id: Long) {
        soundCapsuleDao.deleteSoundCapsule(id)
    }

    // Day Streak methods
    suspend fun insertDayStreak(dayStreak: DayStreak): Long {
        return soundCapsuleDao.insertDayStreak(dayStreak)
    }

    suspend fun updateDayStreak(dayStreak: DayStreak) {
        soundCapsuleDao.updateDayStreak(dayStreak)
    }

    fun getDayStreaksForCapsule(soundCapsuleId: Long): Flow<List<DayStreak>> {
        return soundCapsuleDao.getDayStreaksForCapsule(soundCapsuleId)
    }

    suspend fun getDayStreakById(id: Long): DayStreak? {
        return soundCapsuleDao.getDayStreakById(id)
    }

    suspend fun deleteDayStreaksForCapsule(soundCapsuleId: Long) {
        soundCapsuleDao.deleteDayStreaksForCapsule(soundCapsuleId)
    }

    suspend fun deleteDayStreakById(id: Long) {
        soundCapsuleDao.deleteDayStreakById(id)
    }

    // Daily Listening Time methods
    suspend fun insertDailyListeningTime(dailyListeningTime: DailyListeningTime): Long {
        return soundCapsuleDao.insertDailyListeningTime(dailyListeningTime)
    }

    suspend fun updateDailyListeningTime(dailyListeningTime: DailyListeningTime) {
        soundCapsuleDao.updateDailyListeningTime(dailyListeningTime)
    }

    fun getDailyListeningTimesForCapsule(soundCapsuleId: Long): Flow<List<DailyListeningTime>> {
        return soundCapsuleDao.getDailyListeningTimesForCapsule(soundCapsuleId)
    }

    suspend fun getDailyListeningTimeById(id: Long): DailyListeningTime? {
        return soundCapsuleDao.getDailyListeningTimeById(id)
    }

    suspend fun deleteDailyListeningTimesForCapsule(soundCapsuleId: Long) {
        soundCapsuleDao.deleteDailyListeningTimesForCapsule(soundCapsuleId)
    }

    suspend fun deleteDailyListeningTimeById(id: Long) {
        soundCapsuleDao.deleteDailyListeningTimeById(id)
    }

    // Monthly Play Count methods
    suspend fun incrementSongPlayCount(songId: Long, ownerId: Long) {
        val currentMonth = LocalDateTime.now().monthValue
        val currentYear = LocalDateTime.now().year
        val currentDate = LocalDateTime.now()
        
        // Get or create sound capsule for current month
        val soundCapsule = soundCapsuleDao.getSoundCapsuleByMonth(ownerId, currentYear, currentMonth)
            ?: run {
                val newSoundCapsule = SoundCapsule(
                    month = currentMonth,
                    year = currentYear,
                    timeListenedMinutes = 0,
                    topArtistId = 0,
                    topSongId = 0,
                    lastUpdated = currentDate,
                    ownerId = ownerId
                )
                val soundCapsuleId = createSoundCapsule(newSoundCapsule)
                newSoundCapsule.copy(id = soundCapsuleId)
            }
        
        // Get or create monthly play count
        val monthlyPlayCount = soundCapsuleDao.getMonthlyPlayCount(songId, soundCapsule.id)
        if (monthlyPlayCount == null) {
            soundCapsuleDao.insertMonthlyPlayCount(MonthlySongPlayCount(
                songId = songId,
                soundCapsuleId = soundCapsule.id
            ))
        }
        
        // Increment play count
        soundCapsuleDao.incrementMonthlyPlayCount(songId, soundCapsule.id, currentDate.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)

        // Update day streak
        val existingStreak = soundCapsuleDao.getDayStreakByDate(soundCapsule.id, currentDate)
        if (existingStreak != null) {
            // If there's an existing streak for today, update it
            if (existingStreak.songId == songId) {
                // Same song, update streak
                soundCapsuleDao.updateDayStreak(existingStreak.copy(
                    endDate = currentDate,
                    streakDays = existingStreak.streakDays + 1
                ))
            } else {
                // Different song, create new streak
                soundCapsuleDao.insertDayStreak(DayStreak(
                    soundCapsuleId = soundCapsule.id,
                    songId = songId,
                    startDate = currentDate,
                    endDate = currentDate,
                    streakDays = 1
                ))
            }
        } else {
            // No streak for today, create new one
            soundCapsuleDao.insertDayStreak(DayStreak(
                soundCapsuleId = soundCapsule.id,
                songId = songId,
                startDate = currentDate,
                endDate = currentDate,
                streakDays = 1
            ))
        }

        // Check if this song is now the top song for this month
        val topSongs = soundCapsuleDao.getTop5Songs(soundCapsule.id)
        if (topSongs.isNotEmpty() && topSongs[0].id == songId) {
            // Get the song to get its artist ID
            val song = soundCapsuleDao.getSongById(songId)
            if (song != null) {
                // Update sound capsule with new top song and artist
                soundCapsuleDao.updateSoundCapsule(soundCapsule.copy(
                    topSongId = songId,
                    topArtistId = song.artistId,
                    lastUpdated = currentDate
                ))
            }
        }
    }

    suspend fun getMonthlyPlayCount(songId: Long, soundCapsuleId: Long): MonthlySongPlayCount? {
        return soundCapsuleDao.getMonthlyPlayCount(songId, soundCapsuleId)
    }

    // Top 5 methods
    suspend fun getTop5Artists(soundCapsuleId: Long): List<Artist> {
        return soundCapsuleDao.getTop5Artists(soundCapsuleId)
    }

    suspend fun getTop5Songs(soundCapsuleId: Long): List<Song> {
        return soundCapsuleDao.getTop5Songs(soundCapsuleId)
    }
} 