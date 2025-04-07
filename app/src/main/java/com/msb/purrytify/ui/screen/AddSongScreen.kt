package com.msb.purrytify.ui.screen

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.msb.purrytify.R
import com.msb.purrytify.utils.FileUtils
import com.msb.purrytify.viewmodel.SongViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongScreen(navController: NavController, showBottomSheet: Boolean = true, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val songViewModel: SongViewModel = viewModel()
    
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var duration by remember { mutableLongStateOf(0L) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedArtworkUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Content launcher for audio files
    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                selectedAudioUri = it
                val filePath = FileUtils.getPath(context, it)
                if (filePath != null) {
                    duration = songViewModel.getSongDuration(filePath)
                    val (extractedTitle, extractedArtist) = songViewModel.getSongMetadata(filePath)
                    title = extractedTitle ?: ""
                    artist = extractedArtist ?: ""
                } else {
                    Toast.makeText(context, "Could not get file path", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading audio file: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    
    // Content launcher for image files
    val pickArtworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                selectedArtworkUri = it
            } catch (e: Exception) {
                Toast.makeText(context, "Error selecting artwork: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    
    // Permission launcher for audio files
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = if (Build.VERSION.SDK_INT >= 33) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        if (isGranted) {
            pickAudioLauncher.launch("audio/*")
        } else {
            showPermissionDialog = true
        }
    }
    
    // Permission launcher for image files
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = if (Build.VERSION.SDK_INT >= 33) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        if (isGranted) {
            pickArtworkLauncher.launch("image/*")
        } else {
            showPermissionDialog = true
        }
    }
    
    // Function to request audio permissions
    fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            audioPermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO
            ))
        } else {
            audioPermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }
    
    // Function to request image permissions
    fun requestImagePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            imagePermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            ))
        } else {
            imagePermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            dragHandle = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    BottomSheetDefaults.DragHandle() 
                    Text("Add New Song", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio selection button
            Button(
                onClick = { requestAudioPermission() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Audio File")
            }
            
            // Duration display
            if (duration > 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(minutes)
                
                Text(
                    text = "Duration: ${minutes}:${String.format("%02d", seconds)}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Song Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Artist input
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Artwork selection
            Button(
                onClick = { requestImagePermission() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Artwork")
            }
            
            // Artwork preview
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (selectedArtworkUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedArtworkUri),
                        contentDescription = "Song Artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "Add Artwork",
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button
            Button(
                onClick = {
                    if (title.isBlank() || artist.isBlank() || selectedAudioUri == null) {
                        Toast.makeText(
                            context,
                            "Please fill all required fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        try {
                            val audioFilePath = FileUtils.saveFileToAppStorage(
                                context,
                                selectedAudioUri!!,
                                "songs"
                            )
                            
                            if (audioFilePath.isEmpty()) {
                                Toast.makeText(context, "Failed to save audio file", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val artworkFilePath = selectedArtworkUri?.let {
                                try {
                                    FileUtils.saveFileToAppStorage(context, it, "artwork")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    "" // Return empty string if artwork saving fails
                                }
                            } ?: ""
                            
                            songViewModel.addSong(
                                title = title,
                                artist = artist,
                                filePath = audioFilePath,
                                artworkPath = artworkFilePath,
                                duration = duration
                            )
                            
                            Toast.makeText(
                                context,
                                "Song added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            onDismiss()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context,
                                "Error adding song: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Song")
            }
        }
        }
    }
    
    // Permission denial dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { 
                Text("Storage permission is required to select files. Please grant this permission in app settings.")
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
