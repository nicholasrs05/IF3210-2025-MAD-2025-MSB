package com.msb.purrytify.ui.component.qrcode

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.utils.qrcode.QRCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun ShareSongQRDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    if (!song.isFromApi && song.onlineSongId == null) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }
    
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(song.id) {
        withContext(Dispatchers.IO) {
            // Use the original online song ID for downloaded songs, or the song ID for online songs
            val songIdToShare = song.onlineSongId ?: song.id
            qrBitmap = QRCodeGenerator.generateQRCodeWithInfo(
                songId = songIdToShare.toString(),
                songTitle = song.title,
                artist = song.artistName,
                qrSize = 600
            )
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Share Song via QR Code",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                qrBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code for ${song.title}",
                            modifier = Modifier.size(260.dp)
                        )
                    }
                } ?: run {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Generating QR Code...")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        shareSongQRCode(context, song, qrBitmap)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = qrBitmap != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Share via ShareSheet")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}


private fun shareSongQRCode(context: Context, song: Song, qrBitmap: Bitmap?) {
    if (!song.isFromApi && song.onlineSongId == null) {
        Toast.makeText(context, "Only online and downloaded songs can be shared via QR code", Toast.LENGTH_LONG).show()
        return
    }
    
    qrBitmap?.let { bitmap ->
        try {
            // Use the original online song ID for downloaded songs, or the song ID for online songs
            val songIdToShare = song.onlineSongId ?: song.id
            val uri = QRCodeGenerator.saveQRCodeForSharing(
                context = context,
                bitmap = bitmap,
                songId = songIdToShare.toString()
            )
            
            // Create share intent
            uri?.let { contentUri ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Purrytify: ${song.title}")
                    putExtra(Intent.EXTRA_TEXT, "Check out \"${song.title}\" by ${song.artistName} on Purrytify!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Show the Android system share sheet
                context.startActivity(Intent.createChooser(shareIntent, "Share song via QR code"))
                
                // Show success toast
                Toast.makeText(context, "Sharing QR code via ShareSheet", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ShareSongQR", "Error sharing QR code: ${e.message}")
        }
    }
}
