package com.msb.purrytify.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.msb.purrytify.data.local.entity.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for sharing songs via QR codes
 */
@Singleton
class QRSharingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "QRSharingService"
    private val qrCacheDir get() = File(context.cacheDir, "qrcodes").apply { mkdirs() }
    
    /**
     * Share a song via QR code
     * 
     * @param song The song to share
     */
    fun shareSongViaQR(song: Song) {
        try {
            // Generate QR code with song info
            val qrBitmap = QRGenerator.generateQRCodeWithInfo(
                songId = song.id.toString(),
                title = song.title,
                artist = song.artist,
                qrSize = 512
            )
            
            // Save to cache
            val qrUri = saveQRBitmapToCache(qrBitmap, "song_${song.id}_${System.currentTimeMillis()}.png")
            
            // Share via system share sheet
            qrUri?.let { uri ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    putExtra(Intent.EXTRA_SUBJECT, "Check out this song: ${song.title}")
                    putExtra(Intent.EXTRA_TEXT, "Check out \"${song.title}\" by ${song.artist} on Purrytify!")
                    
                    // Grant permission to receive app
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    
                    // Set clip data for Android 10+ thumbnail previews
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        clipData = android.content.ClipData.newUri(context.contentResolver, "QR Code", uri)
                    }
                }
                
                // Start chooser
                val chooserIntent = Intent.createChooser(shareIntent, "Share Song QR Code")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
                
                Toast.makeText(context, "Sharing QR code for ${song.title}", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(context, "Failed to create QR code", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sharing song via QR: ${e.message}", e)
            Toast.makeText(context, "Failed to share QR code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Save QR bitmap to cache directory
     * 
     * @param bitmap The bitmap to save
     * @param fileName The filename to use
     * @return Uri to the saved file, or null if save failed
     */
    private fun saveQRBitmapToCache(bitmap: Bitmap, fileName: String): Uri? {
        val file = File(qrCacheDir, fileName)
        
        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            
            // Get content URI via FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IOException) {
            Log.e(tag, "Error saving QR bitmap to cache: ${e.message}", e)
            null
        }
    }
    
    /**
     * Clean up old QR codes from cache
     */
    fun cleanupOldQRCodes() {
        try {
            val now = System.currentTimeMillis()
            val maxAge = 24 * 60 * 60 * 1000 // 24 hours
            
            qrCacheDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAge) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error cleaning up old QR codes: ${e.message}", e)
        }
    }
}
