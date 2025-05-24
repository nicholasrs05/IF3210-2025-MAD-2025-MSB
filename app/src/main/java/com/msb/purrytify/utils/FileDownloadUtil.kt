package com.msb.purrytify.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileDownloadUtil {
    fun downloadCsvFile(context: Context, csvData: String, fileName: String) {
        try {
            // Get the Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            // Create the file in Downloads directory
            val file = File(downloadsDir, fileName)
            
            // Write the CSV data
            FileWriter(file).use { writer ->
                writer.write(csvData)
            }
            
            // Show success message
            Toast.makeText(
                context,
                "Sound Capsule data saved to Downloads/$fileName",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error message
            Toast.makeText(
                context,
                "Failed to save Sound Capsule data: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    suspend fun downloadSongFile(
        context: Context,
        fileUrl: String,
        fileName: String,
        subFolder: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, subFolder).apply {
                if (!exists()) mkdirs()
            }
            
            val file = File(directory, FileUtils.sanitizeFileName(fileName))
            
            val connection = URL(fileUrl).openConnection() as HttpURLConnection
            try {
                connection.connect()
                file.outputStream().use { outputStream ->
                    connection.inputStream.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } finally {
                connection.disconnect()
            }
            
            Log.d("FileDownloadUtil", "File downloaded to: ${file.absolutePath}")
            
            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e("FileDownloadUtil", "Error downloading file: ${e.message}", e)
            throw e
        }
    }
} 