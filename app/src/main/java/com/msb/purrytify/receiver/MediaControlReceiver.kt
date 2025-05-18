package com.msb.purrytify.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.msb.purrytify.service.AudioService

class MediaControlReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Create PendingIntent for notification actions
         */
        fun createPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MediaControlReceiver::class.java).apply {
                this.action = action
            }
            
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MediaControlReceiver", "Received action: ${intent.action}")

        val serviceIntent = Intent(context, AudioService::class.java).apply {
            action = intent.action
        }

        context.startService(serviceIntent)
    }
}