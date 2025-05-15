package com.msb.purrytify.data.repository

import com.msb.purrytify.data.local.dao.SoundCapsuleDao
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun getSoundCapsuleByMonth(ownerId: Long, year: Int, month: String): SoundCapsule? {
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

    suspend fun deleteDayStreaksForCapsule(soundCapsuleId: Long) {
        soundCapsuleDao.deleteDayStreaksForCapsule(soundCapsuleId)
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

    suspend fun deleteDailyListeningTimesForCapsule(soundCapsuleId: Long) {
        soundCapsuleDao.deleteDailyListeningTimesForCapsule(soundCapsuleId)
    }
} 