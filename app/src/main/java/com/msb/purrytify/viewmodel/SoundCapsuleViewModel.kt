package com.msb.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.SoundCapsuleRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SoundCapsuleViewModel @Inject constructor(
    private val repository: SoundCapsuleRepository,
    private val songRepository: SongRepository,
    private val profileModel: ProfileModel
) : ViewModel() {

    private val _soundCapsulesState = MutableStateFlow<List<SoundCapsule>>(emptyList())
    val soundCapsulesState: StateFlow<List<SoundCapsule>> = _soundCapsulesState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // New state variables for each screen
    private val _currentSoundCapsule = MutableStateFlow<SoundCapsule?>(null)
    val currentSoundCapsule: StateFlow<SoundCapsule?> = _currentSoundCapsule.asStateFlow()

    private val _dailyListeningTimes = MutableStateFlow<List<DailyListeningTime>>(emptyList())
    val dailyListeningTimes: StateFlow<List<DailyListeningTime>> = _dailyListeningTimes.asStateFlow()

    private val _topArtists = MutableStateFlow<List<Artist>>(emptyList())
    val topArtists: StateFlow<List<Artist>> = _topArtists.asStateFlow()

    private val _topSongs = MutableStateFlow<List<Song>>(emptyList())
    val topSongs: StateFlow<List<Song>> = _topSongs.asStateFlow()

    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    private val _streakSong = MutableStateFlow<Song?>(null)
    val streakSong: StateFlow<Song?> = _streakSong.asStateFlow()

    private val _streakArtist = MutableStateFlow<Artist?>(null)
    val streakArtist: StateFlow<Artist?> = _streakArtist.asStateFlow()

    init {
        viewModelScope.launch {
            profileModel.currentProfile.collect { profile ->
                _userId.value = profile.id
                Log.d("SoundCapsuleViewModel", "User ID updated: ${profile.id}")
            }
        }
    }

    fun loadAllSoundCapsules() {
        viewModelScope.launch {
            val currentUserId = _userId.value
            if (currentUserId == null) {
                Log.e("SoundCapsuleViewModel", "Cannot load sound capsules: user ID is null")
                _error.value = "User ID not available"
                return@launch
            }

            _isLoading.value = true
            _error.value = null
            _soundCapsulesState.value = emptyList()

            try {
                Log.d("SoundCapsuleViewModel", "Starting to collect sound capsules for user: $currentUserId")
                repository.getAllSoundCapsules(currentUserId)
                    .catch { e ->
                        Log.e("SoundCapsuleViewModel", "Error collecting sound capsules", e)
                        _error.value = e.message ?: "An error occurred"
                        _isLoading.value = false
                    }
                    .onStart {
                        _isLoading.value = true
                    }
                    .onCompletion {
                        Log.d("SoundCapsuleViewModel", "Flow collection completed")
                        _isLoading.value = false
                    }
                    .collect { capsules ->
                        Log.d("SoundCapsuleViewModel", "Received sound capsules: $capsules")
                        _soundCapsulesState.value = capsules
                        // Set loading to false after first emission
                        if (_isLoading.value) {
                            _isLoading.value = false
                            Log.d("SoundCapsuleViewModel", "Initial load complete")
                        }
                    }
            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Exception in loadAllSoundCapsules", e)
                _error.value = e.message ?: "An error occurred"
                _isLoading.value = false
            }
        }
    }

    fun loadSoundCapsule(month: Int, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // This will now fetch a single capsule, decide how to integrate or if it's still needed.
            // For now, it won't update the main list (_soundCapsulesState)
            try {
                val soundCapsule = repository.getSoundCapsuleByMonth(userId.value!!, month, year)
                // If you want to update a specific item or add to the list, handle it here.
                // For now, this function is a bit disconnected from the list state.
                // Consider creating a separate state for a single selected/viewed capsule if needed.
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getLongestDayStreak(soundCapsuleId: Long): DayStreak? {
        val dayStreaks: List<DayStreak>? = repository.getDayStreaksForCapsule(soundCapsuleId).firstOrNull()
        val longestStreak = dayStreaks?.maxByOrNull { it.streakDays }
        
        if (longestStreak != null) {
            // Fetch song details from SongRepository
            val song = songRepository.getSongById(longestStreak.songId)
            _streakSong.value = song
            
            if (song != null) {
                // Still need to get artist from SoundCapsuleRepository since we don't have it in SongRepository
                val artist = repository.getArtistById(song.artistId)
                _streakArtist.value = artist
            }
        }
        
        return longestStreak
    }

    fun createSoundCapsule(soundCapsule: SoundCapsule) {
        viewModelScope.launch {
            val capsuleId = repository.createSoundCapsule(soundCapsule)
            // _soundCapsuleState.value = soundCapsule.copy(id = capsuleId) // Old logic
            loadAllSoundCapsules() // Refresh the list
        }
    }

    fun updateSoundCapsule(soundCapsule: SoundCapsule) {
        viewModelScope.launch {
            repository.updateSoundCapsule(soundCapsule)
            // _soundCapsuleState.value = soundCapsule // Old logic
            loadAllSoundCapsules() // Refresh the list
        }
    }

    fun deleteSoundCapsule(id: Long) {
        viewModelScope.launch {
            repository.deleteSoundCapsule(id)
            // _soundCapsuleState.value = null // Old logic
            loadAllSoundCapsules() // Refresh the list
        }
    }

    fun exportToCSV(capsule: SoundCapsule?): String {
        capsule ?: return "No SoundCapsule data available for export"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        return buildString {
            appendLine("Sound Capsule Report - ${capsule.month} ${capsule.year}")
            appendLine("Generated on: ${capsule.lastUpdated.format(formatter)}")
            appendLine()
            appendLine("Time Listened: ${capsule.timeListenedMinutes} minutes")
            appendLine("Top Artist: ${capsule.topArtistId}")
            appendLine("Top Song: ${capsule.topSongId}")
        }
    }

    // New functions for each screen
    fun loadSoundCapsuleDetails(soundCapsuleId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("SoundCapsuleViewModel", "Loading details for sound capsule: $soundCapsuleId")
                
                // Load sound capsule
                val soundCapsule = repository.getSoundCapsuleById(soundCapsuleId)
                _currentSoundCapsule.value = soundCapsule
                Log.d("SoundCapsuleViewModel", "Loaded sound capsule: $soundCapsule")

                // Load daily listening times
                val times = repository.getDailyListeningTimesForCapsule(soundCapsuleId).first()
                _dailyListeningTimes.value = times
                Log.d("SoundCapsuleViewModel", "Loaded daily listening times: ${times.size} entries")

                // Load top artists and songs
                val artists = repository.getTop5Artists(soundCapsuleId)
                _topArtists.value = artists
                Log.d("SoundCapsuleViewModel", "Loaded top artists: ${artists.size} entries")

                val songs = repository.getTop5Songs(soundCapsuleId)
                Log.d("SoundCapsuleViewModel", "Retrieved top songs: ${songs.size} entries")
                Log.d("SoundCapsuleViewModel", "Top songs data: $songs")
                _topSongs.value = songs
                Log.d("SoundCapsuleViewModel", "Current top songs value: ${_topSongs.value}")

            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
                Log.e("SoundCapsuleViewModel", "Error loading sound capsule details", e)
            } finally {
                _isLoading.value = false
                Log.d("SoundCapsuleViewModel", "Finished loading sound capsule details")
            }
        }
    }

    fun getMonthYearString(soundCapsule: SoundCapsule?): String {
        if (soundCapsule == null) return ""
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return "${monthNames[soundCapsule.month - 1]} ${soundCapsule.year}"
    }
} 