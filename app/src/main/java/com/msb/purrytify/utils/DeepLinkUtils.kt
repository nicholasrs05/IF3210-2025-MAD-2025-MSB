package com.msb.purrytify.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.remote.model.SongResponse

/**
 * Utility class for handling deep links in the application
 */
object DeepLinkUtils {

    private const val SCHEME = "purrytify"
    private const val HOST_SONG = "song"

    /**
     * Creates a deep link URI for a specific local song
     */
    fun createSongDeepLink(songId: Long): Uri {
        return "$SCHEME://$HOST_SONG/$songId".toUri()
    }

    /**
    * Creates a deep link URI for a specific API song
    */
fun createApiSongDeepLink(songId: Long): Uri {
    return "$SCHEME://$HOST_SONG/$songId".toUri()
}

    /**
     * Creates a deep link URI for a specific local song as a string
     */
    fun createSongDeepLinkString(songId: Long): String {
        return "$SCHEME://$HOST_SONG/$songId"
    }

    /**
    * Creates a deep link URI for a specific API song as a string
    */
fun createApiSongDeepLinkString(songId: Long): String {
    return "$SCHEME://$HOST_SONG/$songId"
}

    /**
     * Shares a local song using its deep link
     */
    fun shareSong(context: Context, song: Song) {
        val deepLink = createSongDeepLinkString(song.id)
        val shareText = "Check out this song: ${song.title} by ${song.artist}\n$deepLink"

        val shareIntent = ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText(shareText)
            .setSubject("Check out this song: ${song.title}")
            .createChooserIntent()

        // Add flags to start the activity outside of the current activity
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    /**
     * Shares an API song using its deep link
     */
    fun shareApiSong(context: Context, song: SongResponse) {
        val deepLink = createApiSongDeepLinkString(song.id)
        val shareText = "Check out this song: ${song.title} by ${song.artist}\n$deepLink"

        val shareIntent = ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText(shareText)
            .setSubject("Check out this song: ${song.title}")
            .createChooserIntent()

        // Add flags to start the activity outside of the current activity
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    /**
     * Checks if a URI is a valid deep link for the app
     */
    fun isValidAppDeepLink(uri: Uri): Boolean {
        return uri.scheme == SCHEME && 
               (uri.host == HOST_SONG)
    }

    /**
     * Extracts the song ID from a song deep link URI
     * @return The song ID as a string or null if the URI is invalid
     */
    fun extractSongIdFromUri(uri: Uri): String? {
        if (uri.scheme == SCHEME && uri.host == HOST_SONG) {
            return uri.lastPathSegment
        }
        return null
    }
}