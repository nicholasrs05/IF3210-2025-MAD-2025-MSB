package com.msb.purrytify.media

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateOf
import com.msb.purrytify.data.local.entity.Song

enum class RepeatMode {
    NONE,
    ALL,
    ONE
}

class MediaPlayerManager(private val context: Context) {
    interface SongChangeListener {
        fun onSongChanged(newSong: Song)
        fun onPlayerReleased()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongIdx: Int = -1
    private var repeatMode = mutableStateOf(RepeatMode.NONE)

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

    fun setPlaylist(songs: List<Song>) {
        playlist = songs
        if (currentSongIdx == -1) {
            currentSongIdx = 0
        }
    }

    fun addSongToPlaylist(song: Song) {
        playlist = playlist + song
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

    fun play(song: Song) {
        val songIndex = playlist.indexOfFirst { it.id == song.id }
        if (songIndex >= 0) {
            currentSongIdx = songIndex
        } else {
            playlist = playlist + song
            currentSongIdx = playlist.size - 1
        }
        
        releasePlayer(notifyListeners = false)

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(song.filePath)
                prepare()
                start()

                setOnCompletionListener {
                    onCompletion?.invoke()

                    when (repeatMode.value) {
                        RepeatMode.ONE -> {
                            play(song)
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

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun playNext() {
        if (playlist.isEmpty()){
            releasePlayer(notifyListeners = true)
            return
        }

        if (repeatMode.value == RepeatMode.ALL) {
            currentSongIdx = (currentSongIdx + 1) % playlist.size
            play(playlist[currentSongIdx])
        } else {
            if (currentSongIdx < playlist.size - 1) {
                currentSongIdx++
                play(playlist[currentSongIdx])
            } else {
                releasePlayer(notifyListeners = true)
            }
        }
    }

    fun playPrevious() {
        if (playlist.isEmpty()){
            releasePlayer(notifyListeners = true)
            return
        }

        if (repeatMode.value == RepeatMode.ALL) {
            currentSongIdx = (currentSongIdx - 1 + playlist.size) % playlist.size
            play(playlist[currentSongIdx])
        } else {
            if (currentSongIdx > 0) {
                currentSongIdx--
                play(playlist[currentSongIdx])
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