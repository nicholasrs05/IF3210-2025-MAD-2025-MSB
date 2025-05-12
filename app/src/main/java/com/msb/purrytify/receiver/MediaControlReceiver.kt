package com.msb.purrytify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.msb.purrytify.media.MediaPlayerManager
import com.msb.purrytify.service.MusicNotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaControlReceiver : BroadcastReceiver() {
    @Inject
    lateinit var mediaPlayerManager: MediaPlayerManager

    @Inject
    lateinit var notificationService: MusicNotificationService

    companion object {
        const val ACTION_PLAY = "com.msb.purrytify.action.PLAY"
        const val ACTION_PAUSE = "com.msb.purrytify.action.PAUSE"
        const val ACTION_NEXT = "com.msb.purrytify.action.NEXT"
        const val ACTION_PREVIOUS = "com.msb.purrytify.action.PREVIOUS"
        const val ACTION_DISMISS = "com.msb.purrytify.action.DISMISS"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val currentSong = mediaPlayerManager.getCurrentSong() ?: return

        when (intent?.action) {
            ACTION_PLAY -> {
                mediaPlayerManager.resume()
                notificationService.showPlayingNotification(currentSong, true)
            }
            ACTION_PAUSE -> {
                mediaPlayerManager.pause()
                notificationService.showPlayingNotification(currentSong, false)
            }
            ACTION_NEXT -> {
                mediaPlayerManager.playNext()
                mediaPlayerManager.getCurrentSong()?.let { song ->
                    notificationService.showPlayingNotification(song, mediaPlayerManager.isPlaying())
                }
            }
            ACTION_PREVIOUS -> {
                mediaPlayerManager.playPrevious()
                mediaPlayerManager.getCurrentSong()?.let { song ->
                    notificationService.showPlayingNotification(song, mediaPlayerManager.isPlaying())
                }
            }
            ACTION_DISMISS -> {
                mediaPlayerManager.pause()
                notificationService.hidePlayingNotification()
            }
        }
    }
} 