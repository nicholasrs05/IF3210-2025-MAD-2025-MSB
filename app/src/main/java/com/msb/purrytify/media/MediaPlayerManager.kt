package com.msb.purrytify.media

import android.content.Context
import android.media.MediaPlayer
import com.msb.purrytify.data.local.entity.Song

class MediaPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongIdx: Int = -1
    private var isRepeating: Boolean = false

    var onCompletion: (() -> Unit)? = null

    fun setPlaylist(songs: List<Song>) {
        playlist = songs
        currentSongIdx = -1
    }

    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun play(song: Song) {
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.filePath)
            prepare()
            start()

            setOnCompletionListener {
                onCompletion?.invoke()
                playNext()
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun playNext() {
        if (playlist.isEmpty()){
            releasePlayer()
            return
        }

        if (isRepeating) {
            currentSongIdx = (currentSongIdx + 1) % playlist.size
            play(playlist[currentSongIdx])
        } else {
            if (currentSongIdx < playlist.size - 1) {
                currentSongIdx++
                play(playlist[currentSongIdx])
            } else {
                releasePlayer()
            }
        }
    }

    fun playPrevious() {
        if (playlist.isEmpty()){
            releasePlayer()
            return
        }

        if (isRepeating) {
            currentSongIdx = (currentSongIdx - 1 + playlist.size) % playlist.size
            play(playlist[currentSongIdx])
        } else {
            if (currentSongIdx > 0) {
                currentSongIdx--
                play(playlist[currentSongIdx])
            } else {
                releasePlayer()
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

    fun repeat(){
        isRepeating = !isRepeating
    }
}