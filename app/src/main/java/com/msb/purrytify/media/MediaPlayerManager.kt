package com.msb.purrytify.media

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateOf
import com.msb.purrytify.data.local.entity.Song
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.net.Uri

enum class RepeatMode {
    NONE,
    ALL,
    ONE
}

class MediaPlayerManager(private val context: Context) {
    init {
        Log.d("MediaPlayerManager", "Instance created: $this")
    }
    interface SongChangeListener {
        fun onSongChanged(newSong: Song)
        fun onPlayerReleased()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongIdx: Int = -1
    private var repeatMode = mutableStateOf(RepeatMode.NONE)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val songChangeListeners = mutableListOf<SongChangeListener>()
    var onCompletion: (() -> Unit)? = null

    fun addSongChangeListener(listener: SongChangeListener) {
        songChangeListeners.add(listener)
    }

    fun removeSongChangeListener(listener: SongChangeListener) {
        songChangeListeners.remove(listener)
    }

    private fun notifySongChanged(song: Song) {
        songChangeListeners.forEach { it.onSongChanged(song) }
    }

    fun updateCurrentSongIdx() {
        currentSongIdx++
    }

    fun setPlaylist(songs: List<Song>) {
        Log.d("MediaPlayerManager", "setPlaylist called on: $this with ${songs.size} songs")
        playlist = songs
        if (currentSongIdx == -1) {
            currentSongIdx = 0
        }
    }

    fun releasePlayer(notifyListeners: Boolean = true) {
        mediaPlayer?.release()
        mediaPlayer = null

        if (notifyListeners && currentSongIdx != -1 && playlist.isNotEmpty()) {
            currentSongIdx = -1
            songChangeListeners.forEach { it.onPlayerReleased() }
        }
    }

    fun playByIndex(index: Int) {
        Log.d("MediaPlayerManager", "Playing song at index: $index")
        Log.d("MediaPlayerManager", "Playlist size: ${playlist.size}")
        if (index !in playlist.indices) return

        currentSongIdx = index
        val song = playlist[currentSongIdx]

        releasePlayer(notifyListeners = false)

        mediaPlayer = MediaPlayer().apply {
            try {
                if (song.filePath.startsWith("content:")) {
                    val uri = Uri.parse(song.filePath)
                    setDataSource(context, uri)
                } else {
                    setDataSource(song.filePath)
                }
                prepare()
                start()

                setOnCompletionListener {
                    when (repeatMode.value) {
                        RepeatMode.ONE -> {
                            mainHandler.postDelayed({
                                playByIndex(currentSongIdx)
                            }, 200)
                        }
                        else -> {
                            playNext()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        notifySongChanged(song)
    }


    fun play(song: Song) {
        val index = playlist.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            playByIndex(index)
        } else {
            playlist = playlist + song
            playByIndex(playlist.size - 1)
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun playNext() {
        Log.d("MediaPlayerManager", "playNext called on: $this, playlist size: ${playlist.size}")
        if (playlist.isEmpty()) {
            Log.d("MediaPlayerManager", "Playlist is empty, releasing player")
            releasePlayer(notifyListeners = true)
            return
        }

        if (repeatMode.value == RepeatMode.ALL) {
            val nextIndex = (currentSongIdx + 1) % playlist.size
            playByIndex(nextIndex)
        } else {
            if (currentSongIdx < playlist.size - 1) {
                playByIndex(currentSongIdx + 1)
            } else {
                releasePlayer(notifyListeners = true)
            }
        }
    }

    fun playPrevious() {
        if (playlist.isEmpty()) {
            releasePlayer(notifyListeners = true)
            return
        }

        if (repeatMode.value == RepeatMode.ALL) {
            val prevIndex = (currentSongIdx - 1 + playlist.size) % playlist.size
            playByIndex(prevIndex)
        } else {
            if (currentSongIdx > 0) {
                playByIndex(currentSongIdx - 1)
            } else {
                releasePlayer(notifyListeners = true)
            }
        }
    }

    fun getCurrentSong(): Song? {
        return if (currentSongIdx in playlist.indices) {
            playlist[currentSongIdx]
        } else {
            null
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun stop() {
        mediaPlayer?.stop()
        releasePlayer(notifyListeners = false)
        currentSongIdx = -1
        playlist = emptyList()
        Log.d("MediaPlayerManager", "Player stopped and released")
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun shuffle(){
        if (playlist.isEmpty() || currentSongIdx !in playlist.indices) {
            return
        }

        val currentSong = playlist[currentSongIdx]
        val remaining = playlist.toMutableList().apply {
            removeAt(currentSongIdx)
        }
        remaining.shuffle()

        playlist = listOf(currentSong) + remaining
        currentSongIdx = 0
    }

    fun noRepeat(){
        repeatMode.value = RepeatMode.NONE
    }

    fun repeatAll(){
        repeatMode.value = RepeatMode.ALL
    }

    fun repeatOne(){
        repeatMode.value = RepeatMode.ONE
    }
}