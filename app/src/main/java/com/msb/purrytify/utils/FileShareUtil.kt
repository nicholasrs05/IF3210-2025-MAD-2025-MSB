package com.msb.purrytify.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object FileShareUtil {
    fun shareCsvFile(context: Context, csvData: String, fileName: String) {
        try {
            // Create a temporary file
            val file = File(context.cacheDir, fileName)
            FileWriter(file).use { writer ->
                writer.write(csvData)
            }

            // Get the URI for the file using FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Create the share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Start the share activity
            context.startActivity(Intent.createChooser(shareIntent, "Share Sound Capsule Data"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 