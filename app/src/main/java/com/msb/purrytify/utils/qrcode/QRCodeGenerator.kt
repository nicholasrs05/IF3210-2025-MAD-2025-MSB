package com.msb.purrytify.utils.qrcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for generating QR codes for song sharing
 */
object QRCodeGenerator {
    
    /**
     * Generate a QR code bitmap from the song ID
     * 
     * @param songId The ID of the song to share
     * @param size The size of the QR code in pixels
     * @return A bitmap containing the QR code
     */
    fun generateQRCode(songId: String, size: Int): Bitmap? {
        try {
            // Create the deep link URL
            val content = "purrytify://song/$songId"
            
            // Configure QR code parameters
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 2)
            }
            
            // Create QR code
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            // Convert to bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Generate a QR code with song info text below it
     * 
     * @param songId The ID of the song
     * @param songTitle The title of the song
     * @param artist The artist of the song
     * @param qrSize The size of the QR code in pixels
     * @return A bitmap containing the QR code and song info
     */
    fun generateQRCodeWithInfo(songId: String, songTitle: String, artist: String, qrSize: Int): Bitmap? {
        val qrBitmap = generateQRCode(songId, qrSize) ?: return null
        
        // Add space for text below QR code
        val textHeight = 150
        val finalBitmap = Bitmap.createBitmap(qrSize, qrSize + textHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        
        // Draw white background
        canvas.drawColor(Color.WHITE)
        
        // Draw QR code
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        
        // Draw song info text
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        
        val artistPaint = Paint().apply {
            color = Color.GRAY
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        
        // Calculate text positions
        val titleY = qrSize + 50f
        val artistY = qrSize + 100f
        
        // Draw text
        canvas.drawText(songTitle, qrSize / 2f, titleY, titlePaint)
        canvas.drawText(artist, qrSize / 2f, artistY, artistPaint)
        
        return finalBitmap
    }
    
    /**
     * Save QR code bitmap to cache and return a sharable URI
     * 
     * @param context The context
     * @param bitmap The QR code bitmap to save
     * @param songId The ID of the song (used in filename)
     * @return A content URI that can be used for sharing
     */
    fun saveQRCodeForSharing(context: Context, bitmap: Bitmap, songId: String): Uri? {
        try {
            // Create file in cache directory
            val cachePath = File(context.cacheDir, "qrcodes")
            cachePath.mkdirs()
            
            val fileName = "purrytify_song_${songId}_${System.currentTimeMillis()}.png"
            val file = File(cachePath, fileName)
            
            // Save bitmap to file
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            
            // Get content URI via FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}
