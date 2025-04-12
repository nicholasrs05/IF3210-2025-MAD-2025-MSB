package com.msb.purrytify.ui.screen

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.msb.purrytify.R
import com.msb.purrytify.utils.FileUtils
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.viewmodel.SongViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongScreen(
    navController: NavController,
    showBottomSheet: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val songViewModel: SongViewModel = viewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    // UI Colors
    val backgroundColor = Color(0xFF121212)
    val buttonGreen = Color(0xFF1DB954)
    val buttonGray = Color(0xFF555555)
    val borderColor = Color(0xFF444444)
    val textFieldBgColor = Color(0xFF1E1E1E)

    // State variables
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
            containerColor = backgroundColor,
            dragHandle = null,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "Upload Song",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Upload containers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Upload Photo Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { requestImagePermission() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedArtworkUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedArtworkUri),
                                    contentDescription = "Selected Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.add),
                                        contentDescription = "Upload Image",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Upload Photo",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            // Checkbox indicator if artwork is selected
                            if (selectedArtworkUri != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.add),
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Upload File Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { requestAudioPermission() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = "Upload File",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Upload File",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            if (selectedAudioUri != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.add),
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (duration > 0) {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                                TimeUnit.MINUTES.toSeconds(minutes)

                        Text(
                            text = "Duration: ${minutes}:${String.format("%02d", seconds)}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Title TextField
                    Text(
                        text = "Title",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 8.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedContainerColor = textFieldBgColor,
                            unfocusedContainerColor = textFieldBgColor,
                            disabledContainerColor = textFieldBgColor,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(4.dp),
                        singleLine = true
                    )

                    // Artist TextField
                    Text(
                        text = "Artist",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 16.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        placeholder = { Text("Artist", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedContainerColor = textFieldBgColor,
                            unfocusedContainerColor = textFieldBgColor,
                            disabledContainerColor = textFieldBgColor,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(4.dp),
                        singleLine = true
                    )

                    // Spacer to push buttons to bottom
                    Spacer(modifier = Modifier.height(40.dp))

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonGray
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Save Button
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

                                        playerViewModel.updateCurrentSongIdx()

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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonGreen
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        )
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