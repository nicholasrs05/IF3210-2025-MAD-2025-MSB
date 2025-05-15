package com.msb.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SoundCapsuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SoundCapsuleViewModel @Inject constructor(
    private val repository: SoundCapsuleRepository
) : ViewModel() {

    private val _soundCapsuleState = MutableStateFlow<SoundCapsule?>(null)
    val soundCapsuleState: StateFlow<SoundCapsule?> = _soundCapsuleState.asStateFlow()

    private val _dayStreaksState = MutableStateFlow<List<DayStreak>>(emptyList())
    val dayStreaksState: StateFlow<List<DayStreak>> = _dayStreaksState.asStateFlow()

    private val _dailyListeningTimesState = MutableStateFlow<List<DailyListeningTime>>(emptyList())
    val dailyListeningTimesState: StateFlow<List<DailyListeningTime>> = _dailyListeningTimesState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSoundCapsule(month: String, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _dailyListeningTimesState.value = emptyList()
            _dayStreaksState.value = emptyList()
            _soundCapsuleState.value = null

            try {
                // TODO: Uncomment repository logic when ready
                /*
                val soundCapsule = repository.getSoundCapsuleByMonth(ownerId, year, month)
                _soundCapsuleState.value = soundCapsule

                if (soundCapsule != null) {
                    repository.getDayStreaksForCapsule(soundCapsule.id)
                        .collect { streaks ->
                            _dayStreaksState.value = streaks
                        }

                    repository.getDailyListeningTimesForCapsule(soundCapsule.id)
                        .collect { times ->
                            _dailyListeningTimesState.value = times
                        }
                }
                */

                // Generate dummy data for now
                val now = LocalDateTime.now()
                val dailyListeningTimes = (1..30).map { day ->
                    DailyListeningTime(
                        soundCapsuleId = 1L,
                        date = now.withDayOfMonth(day),
                        minutes = (30..120).random() // Random minutes between 30 and 120
                    )
                }
                _dailyListeningTimesState.value = dailyListeningTimes

                // Create dummy sound capsule
                val dummyCapsule = SoundCapsule(
                    month = month,
                    year = year,
                    timeListenedMinutes = dailyListeningTimes.sumOf { it.minutes },
                    topArtist = "The Beatles",
                    topSong = "Starboy",
                    lastUpdated = now,
                    ownerId = 1L
                )
                _soundCapsuleState.value = dummyCapsule

                // Create dummy day streak
                val dummyStreak = DayStreak(
                    soundCapsuleId = 1L,
                    songTitle = "Starboy",
                    artist = "The Weeknd",
                    startDate = now.minusDays(3),
                    endDate = now,
                    streakDays = 3
                )
                _dayStreaksState.value = listOf(dummyStreak)

            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSoundCapsule(soundCapsule: SoundCapsule) {
        viewModelScope.launch {
            val capsuleId = repository.createSoundCapsule(soundCapsule)
            _soundCapsuleState.value = soundCapsule.copy(id = capsuleId)
        }
    }

    fun updateSoundCapsule(soundCapsule: SoundCapsule) {
        viewModelScope.launch {
            repository.updateSoundCapsule(soundCapsule)
            _soundCapsuleState.value = soundCapsule
        }
    }

    fun addDayStreak(dayStreak: DayStreak) {
        viewModelScope.launch {
            val streakId = repository.insertDayStreak(dayStreak)
            _dayStreaksState.value = _dayStreaksState.value + dayStreak.copy(id = streakId)
        }
    }

    fun updateDayStreak(dayStreak: DayStreak) {
        viewModelScope.launch {
            repository.updateDayStreak(dayStreak)
            _dayStreaksState.value = _dayStreaksState.value.map {
                if (it.id == dayStreak.id) dayStreak else it
            }
        }
    }

    fun addDailyListeningTime(dailyListeningTime: DailyListeningTime) {
        viewModelScope.launch {
            val timeId = repository.insertDailyListeningTime(dailyListeningTime)
            _dailyListeningTimesState.value = _dailyListeningTimesState.value + dailyListeningTime.copy(id = timeId)
        }
    }

    fun updateDailyListeningTime(dailyListeningTime: DailyListeningTime) {
        viewModelScope.launch {
            repository.updateDailyListeningTime(dailyListeningTime)
            _dailyListeningTimesState.value = _dailyListeningTimesState.value.map {
                if (it.id == dailyListeningTime.id) dailyListeningTime else it
            }
        }
    }

    fun deleteSoundCapsule(id: Long) {
        viewModelScope.launch {
            repository.deleteSoundCapsule(id)
            _soundCapsuleState.value = null
            _dayStreaksState.value = emptyList()
            _dailyListeningTimesState.value = emptyList()
        }
    }

    fun exportToCSV(): String {
        val capsule = _soundCapsuleState.value ?: return "No data available"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        return buildString {
            appendLine("Sound Capsule Report - ${capsule.month} ${capsule.year}")
            appendLine("Generated on: ${capsule.lastUpdated.format(formatter)}")
            appendLine()
            appendLine("Time Listened: ${capsule.timeListenedMinutes} minutes")
            appendLine("Top Artist: ${capsule.topArtist}")
            appendLine("Top Song: ${capsule.topSong}")
            appendLine()
            appendLine("Daily Listening Times:")
            _dailyListeningTimesState.value.forEach { daily ->
                appendLine("- ${daily.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}: ${daily.minutes} minutes")
            }
            appendLine()
            appendLine("Day Streaks:")
            _dayStreaksState.value.forEach { streak ->
                appendLine("- ${streak.songTitle} by ${streak.artist}")
                appendLine("  Streak: ${streak.streakDays} days")
                appendLine("  From: ${streak.startDate.format(formatter)}")
                appendLine("  To: ${streak.endDate.format(formatter)}")
                appendLine()
            }
        }
    }
} 