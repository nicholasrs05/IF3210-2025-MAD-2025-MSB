package com.msb.purrytify.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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

@Singleton
class QRSharingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "QRSharingService"
    private val qrCacheDir get() = File(context.cacheDir, "qrcodes").apply { mkdirs() }

    fun shareSongViaQR(song: Song) {
        if (!song.isFromApi && song.onlineSongId == null) {
            Toast.makeText(context, "Only online and downloaded songs can be shared via QR code", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val songIdToShare = song.onlineSongId ?: song.id
            val qrBitmap = QRGenerator.generateQRCodeWithInfo(
                songId = songIdToShare.toString(),
                title = song.title,
                artist = song.artistName,
                qrSize = 512
            )

            val qrUri = saveQRBitmapToCache(qrBitmap, "song_${songIdToShare}_${System.currentTimeMillis()}.png")

            qrUri?.let { uri ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    putExtra(Intent.EXTRA_SUBJECT, "Lagu terbaru! ${song.title}")
                    putExtra(Intent.EXTRA_TEXT, "Lihat lagu ii \"${song.title}\" dari ${song.artistName} hanya di purrytify!")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    clipData = android.content.ClipData.newUri(context.contentResolver, "QR Code", uri)
                }

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

    private fun saveQRBitmapToCache(bitmap: Bitmap, fileName: String): Uri? {
        val file = File(qrCacheDir, fileName)

        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }

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
}
