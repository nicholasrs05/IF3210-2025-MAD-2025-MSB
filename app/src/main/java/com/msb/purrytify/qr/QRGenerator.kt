package com.msb.purrytify.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QRGenerator {
    fun generateSongQRCode(
        songId: String,
        size: Int = 512,
        scheme: String = "purrytify", 
        host: String = "song"
    ): Bitmap {
        // Create deep link URI
        val deepLink = "$scheme://$host/$songId"
        
        // Configure QR code parameters for optimal scanning
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        
        try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(deepLink, BarcodeFormat.QR_CODE, size, size, hints)
            
            // Convert to bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height)
            
            // Fill bitmap with QR code data
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            
            return bitmap
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate QR code: ${e.message}", e)
        }
    }
    
    fun generateQRCodeWithInfo(
        songId: String,
        title: String,
        artist: String,
        qrSize: Int = 512
    ): Bitmap {
        // First generate the QR code
        val qrBitmap = generateSongQRCode(songId, qrSize)
        
        // Create a new bitmap with space for text
        val textHeight = 150
        val combinedBitmap = createBitmap(qrSize, qrSize + textHeight)
        
        // Draw the QR code and text onto the new bitmap
        val canvas = android.graphics.Canvas(combinedBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        
        // Draw song info
        val titlePaint = android.graphics.Paint().apply {
            color = Color.BLACK
            textSize = 40f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val artistPaint = android.graphics.Paint().apply {
            color = Color.GRAY
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        // Position text
        val centerX = qrSize / 2f
        val titleY = qrSize + 60f
        val artistY = titleY + 50f
        
        // Draw text
        canvas.drawText(title, centerX, titleY, titlePaint)
        canvas.drawText(artist, centerX, artistY, artistPaint)
        
        return combinedBitmap
    }
}
