package com.msb.purrytify.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.msb.purrytify.MainActivity
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.receiver.MediaControlReceiver
import com.msb.purrytify.utils.NotificationPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "music_playback_channel"
    private val notificationId = 1

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(context, MediaControlReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadArtwork(artworkPath: String): Bitmap? {
        return try {
            if (artworkPath.isNotEmpty()) {
                val artworkUri = Uri.parse(artworkPath)
                context.contentResolver.openInputStream(artworkUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.image)
            }
        } catch (e: Exception) {
            BitmapFactory.decodeResource(context.resources, R.drawable.image)
        }
    }

    fun showPlayingNotification(song: Song, isPlaying: Boolean) {
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                R.drawable.pause,
                "Pause",
                createActionIntent(MediaControlReceiver.ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                R.drawable.play,
                "Play",
                createActionIntent(MediaControlReceiver.ACTION_PLAY)
            ).build()
        }

        val previousAction = NotificationCompat.Action.Builder(
            R.drawable.previous,
            "Previous",
            createActionIntent(MediaControlReceiver.ACTION_PREVIOUS)
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            R.drawable.next,
            "Next",
            createActionIntent(MediaControlReceiver.ACTION_NEXT)
        ).build()

        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.close,
            "Dismiss",
            createActionIntent(MediaControlReceiver.ACTION_DISMISS)
        ).build()

        val artwork = loadArtwork(song.artworkPath)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLargeIcon(artwork)
            .setColor(0xFF121212.toInt())
            .setColorized(true)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(dismissAction)
            .setStyle(MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun hidePlayingNotification() {
        notificationManager.cancel(notificationId)
    }
} 