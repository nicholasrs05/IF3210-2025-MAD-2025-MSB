package com.msb.purrytify.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun getPath(context: Context, uri: Uri): String? {
        // Handle Document Provider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                
                if ("primary".equals(type, ignoreCase = true)) {
                    return "${context.getExternalFilesDir(null)}/${split[1]}"
                }
            } 
            // MediaProvider
            else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                
                return when (type) {
                    "audio" -> getDataColumn(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                    "image" -> getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                    "video" -> getDataColumn(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                    else -> null
                }
            }
        } 
        // Media Provider
        else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        }
        // File
        else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        
        return null
    }
    
    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }
    
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    fun saveFileToAppStorage(context: Context, uri: Uri, subfolder: String): String {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return ""

        val rawName = "${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val safeFileName = sanitizeFileName(rawName)

        val directory = File(context.filesDir, subfolder).apply {
            if (!exists()) mkdirs()
        }

        val file = File(directory, safeFileName)
        saveInputStreamToFile(inputStream, file)

        return file.absolutePath
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    private fun saveInputStreamToFile(inputStream: InputStream, file: File) {
        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var read: Int
        
        try {
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
