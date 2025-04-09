package com.msb.purrytify.ui.screen

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import com.msb.purrytify.R
import com.msb.purrytify.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    navController: NavController,
    // viewModel: PlayerViewModel = hiltViewModel()
) {
    // Default colors (will be overridden by palette extraction)
    var backgroundColor by remember { mutableStateOf(Color(0xFF8B0032)) }
    var textColor by remember { mutableStateOf(Color.White) }
    var buttonTintColor by remember { mutableStateOf(Color.White) }

    var isPlaying by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(104f) }
    var totalDuration by remember { mutableFloatStateOf(230f) }

    // Context for accessing resources
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Extract dominant color from album art
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Get the drawable
                    val drawable = ContextCompat.getDrawable(context, R.drawable.image)
                    val bitmap = (drawable as BitmapDrawable).bitmap
                    
                    // Generate palette from  bitmap
                    val palette = Palette.from(bitmap).generate()
                    
                    // Extract dominant dark color
                    val darkVibrantColor = palette.getDarkVibrantColor(Color(0xFF8B0032).toArgb())

                    // Vibrant color
                    backgroundColor = Color(darkVibrantColor)
                    
                    // Extract vibrant color
                    val vibrantColor = palette.getVibrantColor(Color.White.toArgb())
                    buttonTintColor = Color(vibrantColor)
                    
                    // Brightness
                    val luminance = ColorUtils.calculateLuminance(darkVibrantColor)
                    textColor = if (luminance > 0.5) Color.Black else Color.White
                    
                } catch (_: Exception) {
                    // Fallback to default colors on error
                    backgroundColor = Color(0xFF8B0032)
                    textColor = Color.White
                    buttonTintColor = Color.White
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { /* TODO: Navigate back */ }) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            
            IconButton(onClick = { /* TODO: Show menu */ }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = textColor
                )
            }
        }

        // Content Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Album artwork
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.image),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Song info and like button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Starboy",
                        color = textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The Weeknd, Daft Punk",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
                
                IconButton(onClick = { isLiked = !isLiked }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition,
                    onValueChange = { currentPosition = it },
                    valueRange = 0f..totalDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = buttonTintColor,
                        activeTrackColor = buttonTintColor,
                        inactiveTrackColor = buttonTintColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Time indicators
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
                        text = formatDuration(totalDuration.toInt()),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* TODO: Previous track */ },
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
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(64.dp)
                        .background(buttonTintColor, RoundedCornerShape(32.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = backgroundColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                IconButton(
                    onClick = { /* TODO: Next track */ },
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
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

@Preview(showBackground = true)
@Composable
fun PreviewPlayerScreen() {
    AppTheme {
        PlayerScreen(navController = rememberNavController())
    }
}
