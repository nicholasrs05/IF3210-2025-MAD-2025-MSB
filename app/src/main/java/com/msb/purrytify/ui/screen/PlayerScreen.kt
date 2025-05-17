package com.msb.purrytify.ui.screen

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.ui.component.qrcode.ShareSongQRDialog
import com.msb.purrytify.utils.DeepLinkUtils
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.service.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import coil3.request.ImageRequest
import coil3.request.crossfade
import android.util.Log
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.SuccessResult
import coil3.request.allowHardware

@Composable
fun PlayerScreen(
    song: Song,
    onDismiss: () -> Unit,
    onDismissWithAnimation: () -> Unit = {},
    isDismissing: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val currentPlayingSong = viewModel.currentSong.value ?: song
    
    var localIsDismissing by remember { mutableStateOf(false) }
    val actualIsDismissing = isDismissing || localIsDismissing
    
    val density = LocalDensity.current

    BackHandler {
        if (!actualIsDismissing) {
            viewModel.setLargePlayerVisible(false)
            localIsDismissing = true
            onDismissWithAnimation()
        }
    }

    val slideOffset by animateFloatAsState(
        targetValue = if (actualIsDismissing) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = {
            if (actualIsDismissing) {
                viewModel.setMiniPlayerVisible(viewModel.currentSong.value != null)
                onAnimationComplete()
                onDismiss()
            }
        }
    )
    
    LaunchedEffect(Unit) {
        val isAlreadyPlaying = currentPlayingSong.id == song.id
        Log.d("PlayerScreen", "currentPlayingSong id: ${currentPlayingSong.id}, song id: ${song.id}")
        val wasPlaying = viewModel.isPlaying.value

        if (!isAlreadyPlaying) {
            Log.d("PlayerScreen", "Not already playing, setting new song")
            viewModel.setPlaylist(listOf(song))
            viewModel.playSong(song)
        } else if (wasPlaying) {
            Log.d("PlayerScreen", "Already playing, resuming song")
            viewModel.resumeCurrentSong()
        }
    }
    
    LaunchedEffect(viewModel.currentSong.value) {
        if (viewModel.currentSong.value == null) {
            localIsDismissing = true
            onDismissWithAnimation()
        }
    }
    
    var backgroundColor by remember { mutableStateOf(Color(0xFF121212)) }
    var textColor by remember { mutableStateOf(Color.White) }
    var accentColor by remember { mutableStateOf(Color(0xFF1DB954)) }
    
    val isPlaying by viewModel.isPlaying
    val isLiked by viewModel.isLiked
    val currentPosition by viewModel.currentPosition
    val duration by viewModel.duration

    val context = LocalContext.current

    LaunchedEffect(currentPlayingSong.id, currentPlayingSong.artworkPath) {
        try {
            val bitmap = if (currentPlayingSong.artworkPath.isNotEmpty()) {
                if (currentPlayingSong.artworkPath.startsWith("http")) {
                    var loadedBitmap: Bitmap? = null
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(currentPlayingSong.artworkPath)
                        .allowHardware(false)
                        .build()

                    try {
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            loadedBitmap = (result.image as? BitmapImage)?.bitmap
                        }
                    } catch (e: Exception) {
                        Log.e("MiniPlayer", "Error loading artwork from URL: ${e.message}", e)
                    }
                    loadedBitmap
                } else {
                    val artworkUri = Uri.parse(currentPlayingSong.artworkPath)
                    val inputStream = context.contentResolver.openInputStream(artworkUri)
                    inputStream?.use { BitmapFactory.decodeStream(it) }
                }
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.image)
            }

            bitmap?.let {
                withContext(Dispatchers.Default) {
                    val palette = Palette.from(it).generate()
                    val darkColor = palette.getDarkVibrantColor(
                        palette.getDarkMutedColor(Color(0xFF121212).toArgb())
                    )
                    val vibrantColor = palette.getVibrantColor(
                        palette.getLightVibrantColor(Color(0xFF1DB954).toArgb())
                    )

                    backgroundColor = Color(darkColor)
                    accentColor = Color(vibrantColor)
                    textColor = if (ColorUtils.calculateLuminance(darkColor) > 0.5)
                        Color.Black else Color.White
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            viewModel.updatePosition()
        }
    }
    
    val artworkContent = remember(currentPlayingSong.artworkPath) {
        @Composable {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val artworkUriString = currentPlayingSong.artworkPath

                if (artworkUriString.isNotEmpty()) {
                    val artworkUri = artworkUriString.takeIf { it.isNotEmpty() }?.let {
                        Uri.parse(it)
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.image),
                        contentDescription = "Default Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
    
    val playerControls = remember(isPlaying, accentColor, backgroundColor, textColor) {
        @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = textColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor, RoundedCornerShape(32.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = backgroundColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = textColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
    
    val progressSlider = remember(duration) {
        @Composable {
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition,
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..duration.coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = accentColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition.toInt()),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatDuration(duration.toInt()),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    
    val offsetY = with(density) { slideOffset * 1000.dp.toPx() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = offsetY.dp)
            .background(backgroundColor)
            .zIndex(10f)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        viewModel.setLargePlayerVisible(false)
                        localIsDismissing = true
                        onDismissWithAnimation()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 50) {
                            change.consume()
                            localIsDismissing = true
                            onDismissWithAnimation()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.setLargePlayerVisible(false)
                    viewModel.setMiniPlayerVisible(true)
                    localIsDismissing = true
                    onDismissWithAnimation()
                }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }
                
                Text(
                    text = "Now Playing",
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium
                )
                
                var showMenu by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showQRDialog by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Menu",
                            tint = textColor
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(backgroundColor)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit Song",
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Song", color = textColor)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share URL",
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share URL", color = textColor)
                                }
                            },
                            onClick = {
                                showMenu = false
                                DeepLinkUtils.shareSong(context, currentPlayingSong)
                            }
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode,
                                        contentDescription = "Share QR Code",
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share QR Code", color = textColor)
                                }
                            },
                            onClick = {
                                showMenu = false
                                viewModel.shareCurrentSongViaQR()
                            }
                        )
                    }
                }
                
                if (showEditDialog) {
                    EditSongDialog(
                        song = currentPlayingSong,
                        onDismiss = { showEditDialog = false },
                        backgroundColor = backgroundColor,
                        textColor = textColor,
                        accentColor = accentColor,
                        viewModel = viewModel
                    )
                }
                
                if (showQRDialog) {
                    ShareSongQRDialog(
                        song = currentPlayingSong,
                        onDismiss = { showQRDialog = false }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            artworkContent()
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPlayingSong.title,
                        color = textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentPlayingSong.artist,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
                
                IconButton(onClick = { viewModel.toggleLike() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color.Red else textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            progressSlider()
            Spacer(modifier = Modifier.weight(1f))
            playerControls()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.isShuffle.value) accentColor else textColor.copy(alpha = 0.5f)
                    )
                }
                
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (viewModel.repeatMode.value) {
                            RepeatMode.NONE -> Icons.Filled.Repeat
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            RepeatMode.ALL -> Icons.Filled.RepeatOn
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (viewModel.repeatMode.value != RepeatMode.NONE) 
                            accentColor else textColor.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun formatDuration(miliseconds: Int): String {
    val seconds = miliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    viewModel: PlayerViewModel
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var selectedArtworkUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val buttonGreen = accentColor
    val buttonGray = Color(0xFF555555)
    val borderColor = Color(0xFF444444)
    val textFieldBgColor = backgroundColor.copy(alpha = 0.7f)
    
    val pickArtworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedArtworkUri = it
            } catch (e: Exception) {
                Toast.makeText(context, "Error selecting artwork: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    
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
                Text(
                    text = "Edit Song",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                        } else if (song.artworkPath.isNotEmpty()) {
                            val artworkUri = Uri.parse(song.artworkPath)
                            AsyncImage(
                                model = artworkUri,
                                contentDescription = "Current Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
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
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = "Song Info",
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            
                            val minutes = song.duration / 60000
                            val seconds = (song.duration % 60000) / 1000
                            Text(
                                text = "Duration: ${minutes}:${String.format("%02d", seconds)}",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = "Title",
                    color = textColor,
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
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor,
                        focusedContainerColor = textFieldBgColor,
                        unfocusedContainerColor = textFieldBgColor,
                        disabledContainerColor = textFieldBgColor,
                        cursorColor = textColor,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true
                )
                
                Text(
                    text = "Artist",
                    color = textColor,
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
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor,
                        focusedContainerColor = textFieldBgColor,
                        unfocusedContainerColor = textFieldBgColor,
                        disabledContainerColor = textFieldBgColor,
                        cursorColor = textColor,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val coroutineScope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            if (title.isBlank() || artist.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please fill all required fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                coroutineScope.launch {
                                    try {
                                        val artworkFilePath = selectedArtworkUri?.toString() ?: song.artworkPath
                                        
                                        viewModel.updateSong(
                                            songId = song.id,
                                            title = title,
                                            artist = artist,
                                            artworkPath = artworkFilePath
                                        )

                                        viewModel.updateSongFromRepo()
                                        
                                        Toast.makeText(
                                            context,
                                            "Song updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        
                                        onDismiss()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(
                                            context,
                                            "Error updating song: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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
