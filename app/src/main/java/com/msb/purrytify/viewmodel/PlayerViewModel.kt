package com.msb.purrytify.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.repository.ArtistRepository
import com.msb.purrytify.data.repository.SongRepository
import com.msb.purrytify.model.ProfileModel
import com.msb.purrytify.qr.QRSharingService
import com.msb.purrytify.service.AudioService
import com.msb.purrytify.service.PlayerManager
import com.msb.purrytify.service.RepeatMode
import com.msb.purrytify.model.AudioDevice
import com.msb.purrytify.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository,
    private val playerManager: PlayerManager,
    profileModel: ProfileModel,
    private val qrSharingService: QRSharingService,
    private val artistRepository: ArtistRepository,
) : AndroidViewModel(application) {

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _isShuffle = mutableStateOf(false)
    val isShuffle: State<Boolean> = _isShuffle

    private val _repeatMode = mutableStateOf(RepeatMode.NONE)
    val repeatMode: State<RepeatMode> = _repeatMode

    private val _currentPosition = mutableFloatStateOf(0f)
    val currentPosition: State<Float> = _currentPosition

    private val _duration = mutableFloatStateOf(0f)
    val duration: State<Float> = _duration

    private val _isLiked = mutableStateOf(false)
    val isLiked: State<Boolean> = _isLiked

    private val _isMiniPlayerVisible = mutableStateOf(false)
    val isMiniPlayerVisible: State<Boolean> = _isMiniPlayerVisible
    private val _isLargePlayerVisible = mutableStateOf(false)
    val userId = profileModel.currentProfile.value.id

    private var audioService: AudioService? = null
    private var bound = false

    private val _showAudioDeviceSheet = mutableStateOf(false)
    val showAudioDeviceSheet: State<Boolean> = _showAudioDeviceSheet

    private val _currentAudioDevice = mutableStateOf<AudioDevice?>(null)
    val currentAudioDevice: State<AudioDevice?> = _currentAudioDevice

    private val _availableAudioDevices = mutableStateOf<List<AudioDevice>>(emptyList())
    val availableAudioDevices: State<List<AudioDevice>> = _availableAudioDevices

    private val _playbackError = mutableStateOf<String?>(null)
    val playbackError: State<String?> = _playbackError

    private val _isFromQRScan = mutableStateOf(false)
    val isFromQRScan: State<Boolean> = _isFromQRScan

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.getService()
            bound = true
            
            collectServiceFlows()
            
            audioService?.currentSong?.value?.let { song ->
                _currentSong.value = song
                _isPlaying.value = audioService?.isPlaying?.value ?: false
                _duration.floatValue = song.duration.toFloat()
                checkLikedStatus(song.id)
                _isMiniPlayerVisible.value = true
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            bound = false
        }
    }

    init {
        connectToService()

        viewModelScope.launch {
            playerManager.currentSong.collectLatest { song ->
                song?.let {
                    _currentSong.value = it
                    _duration.floatValue = it.duration.toFloat()
                    checkLikedStatus(it.id)
                } ?: run {
                    _currentSong.value = null
                    _duration.floatValue = 0f
                    _currentPosition.floatValue = 0f
                    _isLargePlayerVisible.value = false
                    _isMiniPlayerVisible.value = false
                }
            }
        }
        
        viewModelScope.launch {
            playerManager.isPlaying.collectLatest { playing ->
                _isPlaying.value = playing
            }
        }
        
        viewModelScope.launch {
            playerManager.currentPosition.collectLatest { position ->
                _currentPosition.floatValue = position
            }
        }
        
        viewModelScope.launch {
            playerManager.duration.collectLatest { duration ->
                if (duration > _duration.floatValue) {
                    _duration.floatValue = duration
                }
            }
        }
        
        viewModelScope.launch {
            playerManager.isMiniPlayerVisible.collectLatest { visible ->
                _isMiniPlayerVisible.value = visible
            }
        }
        
        viewModelScope.launch {
            playerManager.isLargePlayerVisible.collectLatest { visible ->
                _isLargePlayerVisible.value = visible
            }
        }
        
        viewModelScope.launch {
            playerManager.isShuffle.collectLatest { shuffleEnabled ->
                _isShuffle.value = shuffleEnabled
            }
        }
        
        viewModelScope.launch {
            playerManager.repeatMode.collectLatest { mode ->
                _repeatMode.value = when(mode) {
                    0 -> RepeatMode.NONE
                    1 -> RepeatMode.ALL
                    2 -> RepeatMode.ONE
                    else -> RepeatMode.NONE
                }
            }
        }

        viewModelScope.launch {
            playerManager.showAudioDeviceSheet.collectLatest { show ->
                _showAudioDeviceSheet.value = show
            }
        }

        viewModelScope.launch {
            playerManager.currentAudioDevice.collectLatest { device ->
                _currentAudioDevice.value = device
            }
        }

        viewModelScope.launch {
            playerManager.availableAudioDevices.collectLatest { devices ->
                _availableAudioDevices.value = devices
            }
        }
    }

    private fun connectToService() {
        Intent(getApplication(), AudioService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun collectServiceFlows() {
        audioService?.let { service ->
            viewModelScope.launch {
                service.currentSong.collectLatest { song ->
                    song?.let {
                        _currentSong.value = it
                        _duration.floatValue = it.duration.toFloat()
                        checkLikedStatus(it.id)
                        _isMiniPlayerVisible.value = true
                    }
                }
            }
            
            viewModelScope.launch {
                service.isPlaying.collectLatest { playing ->
                    _isPlaying.value = playing
                }
            }
            
            viewModelScope.launch {
                service.playbackProgress.collectLatest { progress ->
                    val songDuration = if (service.isPlaying.value) service.getDuration() else _currentSong.value?.duration?.toInt() ?: 0
                    if (songDuration > 0) {
                        _currentPosition.floatValue = progress * songDuration
                    }
                }
            }
            
            viewModelScope.launch {
                service.playbackError.collectLatest { error ->
                    _playbackError.value = error
                }
            }
        }
    }

    fun setLargePlayerVisible(isVisible: Boolean) {
        playerManager.setLargePlayerVisible(isVisible)
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            // Check if file is accessible before attempting to play
            if (!song.isFromApi && !FileUtils.isFileAccessible(getApplication(), song.filePath)) {
                _playbackError.value = "Song file not found or moved. Cannot play this song."
                return@launch
            }
            
            checkLikedStatus(song.id)
            playerManager.playSong(song)
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        // Filter out inaccessible local songs
        val accessibleSongs = songs.filter { song ->
            if (song.isFromApi) {
                // Online songs are always considered accessible (network issues handled separately)
                true
            } else {
                // Check local songs for file accessibility
                val isAccessible = FileUtils.isFileAccessible(getApplication(), song.filePath)
                if (!isAccessible) {
                    Log.w("PlayerViewModel", "Skipping inaccessible song: ${song.title}")
                }
                isAccessible
            }
        }
        
        if (accessibleSongs.isEmpty()) {
            _playbackError.value = "No accessible songs found in the playlist."
            return
        }
        
        if (accessibleSongs.size < songs.size) {
            _playbackError.value = "Some songs were skipped because their files are missing or moved."
        }
        
        // Adjust start index if necessary
        val adjustedStartIndex = if (startIndex < accessibleSongs.size) startIndex else 0
        
        playerManager.setPlaylist(accessibleSongs, adjustedStartIndex)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipToNext() {
        playerManager.playNext()
    }

    fun skipToPrevious() {
        playerManager.playPrevious()
    }

    fun seekTo(position: Float) {
        playerManager.seekTo(position)
    }

    fun toggleLike() {
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                val newLikeStatus = !_isLiked.value
                songRepository.updateLikeStatus(song.id, newLikeStatus)
                _isLiked.value = newLikeStatus
            }
        }
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun updatePosition() {
        playerManager.updatePosition()
    }

    fun updateSongFromRepo() {
        _currentSong.value?.let { song ->
            viewModelScope.launch {
                val updatedSong = songRepository.getSongById(song.id)
                if (updatedSong != null) {
                    _currentSong.value = updatedSong
                }
            }
        }
    }

    private fun checkLikedStatus(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            _isLiked.value = song?.isLiked == true
        }
    }

    fun resumeCurrentSong() {
        playerManager.resumePlayback()
    }

    fun stopMediaPlayer() {
        playerManager.stopPlayback()
    }

    fun canShareSong(): Boolean {
        val song = currentSong.value ?: return false
        // Allow sharing of online songs (isFromApi = true) and downloaded songs (onlineSongId != null)
        // But not offline songs (isFromApi = false AND onlineSongId = null)
        return song.isFromApi || song.onlineSongId != null
    }

    fun canEditSong(): Boolean {
        val song = currentSong.value ?: return false
        // Only allow editing of local songs (not from API and not downloaded from online)
        if (song.isFromApi || song.onlineSongId != null) {
            return false
        }
        // Check if the file still exists
        return FileUtils.isFileAccessible(getApplication(), song.filePath)
    }

    fun shareCurrentSongViaQR() {
        currentSong.value?.let { song ->
            if (song.isFromApi || song.onlineSongId != null) {
                qrSharingService.shareSongViaQR(song)
            } else {
                // Show message that only online and downloaded songs can be shared
                Log.w("PlayerViewModel", "Cannot share local song: ${song.title}. Only online and downloaded songs can be shared.")
            }
        }
    }

    fun setMiniPlayerVisible(isVisible: Boolean) {
        playerManager.setMiniPlayerVisible(isVisible)
    }

    fun addSong(title: String, artist: String, filePath: String, artworkPath: String, duration: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingArtist = artistRepository.getArtistByName(artist.lowercase())
            val artistId = if (existingArtist != null) {

                // Random probability (50%) to update artist artwork when adding a new song
                if (artworkPath.isNotEmpty() && kotlin.random.Random.nextBoolean()) {
                    artistRepository.updateArtist(existingArtist.copy(imageUrl = artworkPath))
                }
                existingArtist.id
            } else {
                artistRepository.insertArtist(
                    Artist(
                        name = artist,
                        imageUrl = if (artworkPath.isNotEmpty()) artworkPath else null
                    )
                )
            }

            Log.d("Add Song", "Duration: $duration")

            val song = Song(
                title = title,
                artistName = artist,
                artistId = artistId,
                filePath = filePath,
                artworkPath = artworkPath,
                duration = duration,
                ownerId = userId
            )
            songRepository.insert(song)
        }
    }

    fun getSongMetadata(filePath: String): Pair<String?, String?> {
        return SongRepository.extractMetadata(filePath)
    }

    fun getSongDuration(filePath: String): Long {
        return SongRepository.getDuration(filePath)
    }

    fun getSongMetadataFromStream(inputStream: java.io.InputStream): Pair<String?, String?> {
        return try {
            val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
            // Create a temporary file to store the stream content
            val tempFile = java.io.File.createTempFile("temp_audio", null, getApplication<Application>().cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            mediaMetadataRetriever.setDataSource(tempFile.absolutePath)

            val title = mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)

            mediaMetadataRetriever.release()
            tempFile.delete() // Clean up the temporary file
            Pair(title, artist)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }

    fun getSongDurationFromStream(inputStream: java.io.InputStream): Long {
        return try {
            val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
            // Create a temporary file to store the stream content
            val tempFile = java.io.File.createTempFile("temp_audio", null, getApplication<Application>().cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            mediaMetadataRetriever.setDataSource(tempFile.absolutePath)

            val duration = mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

            mediaMetadataRetriever.release()
            tempFile.delete() // Clean up the temporary file
            duration
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun updateSong(songId: Long, title: String, artist: String, artworkPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getSongById(songId)?.let { existingSong ->
                    val existingArtistId = existingSong.artistId
                    val existingArtist = artistRepository.getArtistById(existingArtistId)
                    existingArtist?.let {
                        artistRepository.updateArtist(it.copy(name = artist))
                    }

                    val updatedSong = existingSong.copy(
                        title = title,
                        artworkPath = artworkPath
                    )

                    songRepository.update(updatedSong)
                    
                    if (_currentSong.value?.id == songId) {
                        _currentSong.value = updatedSong
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showAudioDeviceSheet() {
        playerManager.showAudioDeviceSheet()
    }

    fun hideAudioDeviceSheet() {
        playerManager.hideAudioDeviceSheet()
    }

    fun refreshAudioDevices() {
        playerManager.refreshAudioDevices()
    }

    fun selectAudioDevice(device: AudioDevice) {
        playerManager.selectAudioDevice(device)
    }

    fun clearPlaybackError() {
        _playbackError.value = null
        audioService?.clearPlaybackError()
    }

    fun playSongFromQR(song: Song) {
        viewModelScope.launch {
            // Check if file is accessible before attempting to play
            if (!song.isFromApi && !FileUtils.isFileAccessible(getApplication(), song.filePath)) {
                _playbackError.value = "Song file not found or moved. Cannot play this song."
                return@launch
            }
            
            _isFromQRScan.value = true
            checkLikedStatus(song.id)
            playerManager.playSong(song)
        }
    }

    fun clearQRScanFlag() {
        _isFromQRScan.value = false
    }

    override fun onCleared() {
        super.onCleared()
        
        if (bound) {
            getApplication<Application>().unbindService(serviceConnection)
            bound = false
        }
    }
}