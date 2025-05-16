package com.msb.purrytify.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.utils.NetworkStatusListener
import com.msb.purrytify.ui.component.NoInternet

@Composable
fun TenCountryScreen(
    onDismiss: () -> Unit,
    onDismissWithAnimation: () -> Unit = {},
    isDismissing: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    var localIsDismissing by remember { mutableStateOf(false) }
    val actualIsDismissing = isDismissing || localIsDismissing
    
    val density = LocalDensity.current
    val isConnected = NetworkStatusListener()
    val isMiniPlayerVisible = playerViewModel.isMiniPlayerVisible.value

    BackHandler {
        if (!actualIsDismissing) {
            localIsDismissing = true
            onDismissWithAnimation()
        }
    }

    val slideOffset by animateFloatAsState(
        targetValue = if (actualIsDismissing) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = {
            if (actualIsDismissing) {
                playerViewModel.setMiniPlayerVisible(playerViewModel.currentSong.value != null)
                onAnimationComplete()
                onDismiss()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 10) {
                        localIsDismissing = true
                        onDismissWithAnimation()
                    }
                }
            }
    ) {
        if (isConnected) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            localIsDismissing = true
                            onDismissWithAnimation()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(200.dp))
                
                Text(
                    text = "This is the top 10 country page",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            localIsDismissing = true
                            onDismissWithAnimation()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                NoInternet()
            }
        }
    }
}