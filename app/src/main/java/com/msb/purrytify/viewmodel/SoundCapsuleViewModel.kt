package com.msb.purrytify.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.local.entity.SongWithPlayCount
import com.msb.purrytify.data.repository.SoundCapsuleRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.utils.FileDownloadUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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

    private val _topSongsWithPlayCount = MutableStateFlow<List<SongWithPlayCount>>(emptyList())
    val topSongsWithPlayCount: StateFlow<List<SongWithPlayCount>> = _topSongsWithPlayCount.asStateFlow()

    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    // Total counts for songs and artists
    private val _totalSongCount = MutableStateFlow(0)
    val totalSongCount: StateFlow<Int> = _totalSongCount.asStateFlow()

    private val _totalArtistCount = MutableStateFlow(0)
    val totalArtistCount: StateFlow<Int> = _totalArtistCount.asStateFlow()

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

    suspend fun getLongestDayStreak(soundCapsuleId: Long): DayStreak? {
        val dayStreaks: List<DayStreak>? = repository.getDayStreaksForCapsule(soundCapsuleId).firstOrNull()
        return dayStreaks?.maxByOrNull { it.streakDays }
    }

    suspend fun getTopArtistForCapsule(soundCapsuleId: Long): Artist? {
        return repository.getTopArtistForCapsule(soundCapsuleId)
    }

    suspend fun getTopSongForCapsule(soundCapsuleId: Long): Song? {
        return repository.getTopSongForCapsule(soundCapsuleId)
    }

    suspend fun getStreakSongForCapsule(soundCapsuleId: Long): Song? {
        val longestStreak = getLongestDayStreak(soundCapsuleId)
        return if (longestStreak != null) {
            songRepository.getSongById(longestStreak.songId)
        } else {
            null
        }
    }

    suspend fun getStreakArtistForCapsule(soundCapsuleId: Long): Artist? {
        val streakSong = getStreakSongForCapsule(soundCapsuleId)
        return if (streakSong != null) {
            repository.getArtistById(streakSong.artistId)
        } else {
            null
        }
    }

    fun exportToCSV(capsule: SoundCapsule?): String {
        capsule ?: return "No SoundCapsule data available for export"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        return buildString {
            // Header
            appendLine("Sound Capsule Report")
            appendLine("Generated at,${capsule.lastUpdated.format(formatter)}")
            appendLine()
            
            // Basic Information
            appendLine("Basic Information")
            appendLine("Month,${capsule.month}")
            appendLine("Year,${capsule.year}")
            appendLine("Total Listening Time (minutes),${capsule.timeListenedMinutes}")
            appendLine()
            
            // Top Artists Section
            appendLine("Top Artists")
            appendLine("Rank,Artist Name,Play Count")
            viewModelScope.launch {
                try {
                    val artists = repository.getTop5Artists(capsule.id)
                    artists.forEachIndexed { index, artist ->
                        appendLine("${index + 1},${artist.name},") // Play count not available in current model
                    }
                } catch (e: Exception) {
                    appendLine("Failed to load artists: ${e.message}")
                }
            }
            appendLine()
            
            // Top Songs Section
            appendLine("Top Songs")
            appendLine("Rank,Song Title,Artist,Play Count")
            viewModelScope.launch {
                try {
                    val songs = repository.getTop5Songs(capsule.id)
                    songs.forEachIndexed { index, song ->
                        val artist = repository.getArtistById(song.artistId)
                        appendLine("${index + 1},${song.title},${artist?.name ?: "Unknown"},") // Play count not available in current model
                    }
                } catch (e: Exception) {
                    appendLine("Failed to load songs: ${e.message}")
                }
            }
            appendLine()
            
            // Daily Listening Times
            appendLine("Daily Listening History")
            appendLine("Date,Minutes Listened")
            viewModelScope.launch {
                try {
                    repository.getDailyListeningTimesForCapsule(capsule.id).firstOrNull()?.forEach { daily ->
                        appendLine("${daily.date},${daily.minutes}")
                    }
                } catch (e: Exception) {
                    appendLine("Failed to load daily listening times: ${e.message}")
                }
            }
            appendLine()
            
            // Day Streaks
            appendLine("Listening Streaks")
            appendLine("Start Date,End Date,Days,Song")
            viewModelScope.launch {
                try {
                    repository.getDayStreaksForCapsule(capsule.id).firstOrNull()?.forEach { streak ->
                        val song = songRepository.getSongById(streak.songId)
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        appendLine("${streak.startDate.format(dateFormatter)},${streak.endDate.format(dateFormatter)},${streak.streakDays},${song?.title ?: "Unknown"}")
                    }
                } catch (e: Exception) {
                    appendLine("Failed to load streaks: ${e.message}")
                }
            }
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

                // Load top songs with play counts
                val songsWithPlayCount = repository.getTop5SongsWithPlayCount(soundCapsuleId)
                _topSongsWithPlayCount.value = songsWithPlayCount
                Log.d("SoundCapsuleViewModel", "Loaded top songs with play count: ${songsWithPlayCount.size} entries")

                // Load total counts
                val totalSongs = repository.getTotalSongCount(soundCapsuleId)
                _totalSongCount.value = totalSongs
                Log.d("SoundCapsuleViewModel", "Total songs count: $totalSongs")

                val totalArtists = repository.getTotalArtistCount(soundCapsuleId)
                _totalArtistCount.value = totalArtists
                Log.d("SoundCapsuleViewModel", "Total artists count: $totalArtists")

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

    suspend fun exportAllSoundCapsulestoCSV(): String {
        val capsules = _soundCapsulesState.value
        
        return buildString {
            // Header
            appendLine("All Sound Capsules Report")
            appendLine("Generated at,${LocalDateTime.now()}")
            appendLine()
            
            // Column Headers
            appendLine("Month,Year,Total Minutes,Top Artist,Top Song,Last Updated")
            
            // Data rows
            capsules.forEach { capsule ->
                try {
                    val topArtist = repository.getArtistById(capsule.topArtistId)?.name ?: "Unknown"
                    val topSong = songRepository.getSongById(capsule.topSongId)?.title ?: "Unknown"
                    
                    appendLine("${capsule.month},${capsule.year},${capsule.timeListenedMinutes}," +
                            "${topArtist},${topSong},${capsule.lastUpdated}")
                } catch (e: Exception) {
                    appendLine("${capsule.month},${capsule.year},${capsule.timeListenedMinutes},Error loading data,Error loading data,${capsule.lastUpdated}")
                }
            }
        }
    }

    fun downloadAllSoundCapsules(context: Context) {
        viewModelScope.launch {
            val csvData = exportAllSoundCapsulestoCSV()
            val fileName = "all_sound_capsules_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.csv"
            FileDownloadUtil.downloadCsvFile(context, csvData, fileName)
        }
    }
} 