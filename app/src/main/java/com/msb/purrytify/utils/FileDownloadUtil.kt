package com.msb.purrytify.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileWriter

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
} 