package com.msb.purrytify.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.msb.purrytify.ui.component.NoInternet
import com.msb.purrytify.ui.component.soundcapsule.SoundCapsuleSection
import com.msb.purrytify.ui.navigation.Screen
import com.msb.purrytify.utils.NetworkMonitor
import com.msb.purrytify.viewmodel.AuthViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.viewmodel.ProfileUiState
import com.msb.purrytify.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun logout() {
        authViewModel.logout()
        playerViewModel.stopMediaPlayer()
        playerViewModel.setMiniPlayerVisible(false)
    }

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            Log.d("ProfileScreen", "ProfileScreen STARTED, refreshing profile")
            viewModel.refreshProfile()
        }
    }

    LaunchedEffect(Unit) {
        NetworkMonitor.observeNetworkStatus(context).collectLatest { connected ->
            isConnected = connected
        }
    }

    val profileUiState by viewModel.profileState.collectAsState(initial = ProfileUiState.Loading)

    if (isConnected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101010))
        ) {
            when (profileUiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ProfileUiState.Success -> {
                    ProfileContent(
                        profile = (profileUiState as ProfileUiState.Success).profile,
                        logout = { logout() },
                        modifier = Modifier.verticalScroll(scrollState),
                        navController = navController
                    )
                }

                is ProfileUiState.Error -> {
                    ErrorScreen(errorMessage = (profileUiState as ProfileUiState.Error).errorMessage)
                }
            }
        }
    } else {
        NoInternet()
    }
}

@Composable
fun ProfileContent(
    profile: com.msb.purrytify.data.model.Profile,
    logout: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
    ) {
        // Gradient section (50% of screen height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00667B),
                            Color(0xFF002F38),
                            Color(0xFF101010)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                // Profile photo
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .wrapContentWidth()
                        .width(120.dp)
                        .height(120.dp)
                ) {
                    AsyncImage(
                        model = profile.profilePhotoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // User info
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = profile.username,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.location,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // First row of buttons
                Row {
                    Button(
                        onClick = { navController.navigate("editProfileGraph") },
                        shape = RoundedCornerShape(45.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = ButtonDefaults.MinWidth,
                                minHeight = 10.dp
                            )
                            .width(105.dp)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E3f3f),
                            contentColor = Color.White,
                        )
                    ) {
                        Text(
                            "Edit Profile",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { logout() },
                        shape = RoundedCornerShape(45.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = ButtonDefaults.MinWidth,
                                minHeight = 10.dp
                            )
                            .width(105.dp)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E3f3f),
                            contentColor = Color.White,
                        )
                    ) {
                        Text(
                            "Logout",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(label = "SONGS", count = profile.addedSongsCount)
                    ProfileStat(label = "LIKED", count = profile.likedSongsCount)
                    ProfileStat(label = "LISTENED", count = profile.listenedSongsCount)
                }
            }
        }

        // Sound Capsule Section (outside gradient, on dark background)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoundCapsuleSection(
                navController = navController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProfileStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun ErrorScreen(errorMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = "Error Icon",
            tint = Color.Red,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "An error occurred:",
            color = Color.Red,
            textAlign = TextAlign.Center
        )
        Text(
            text = errorMessage,
            color = Color.Red,
            textAlign = TextAlign.Center
        )
    }
}