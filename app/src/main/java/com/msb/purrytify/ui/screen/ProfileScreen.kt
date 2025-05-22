package com.msb.purrytify.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.msb.purrytify.ui.profile.ProfileUiState
import com.msb.purrytify.ui.profile.ProfileViewModel
import androidx.compose.runtime.*
import com.msb.purrytify.utils.NetworkMonitor
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.ui.navigation.Screen
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.viewmodel.AuthViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.ui.component.NoInternet
import com.msb.purrytify.viewmodel.SoundCapsuleViewModel
import com.msb.purrytify.ui.component.soundcapsule.SoundCapsuleSection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.msb.purrytify.utils.FileShareUtil

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
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
                    RectangleGradient()
                    ProfileContent(
                        profile = (profileUiState as ProfileUiState.Success).profile,
                        logout = { logout() },
                        onScanQRCode = { navController.navigate(Screen.QRScanner.route) },
                        navController = navController,
                        modifier = if (isLandscape) Modifier.verticalScroll(scrollState) else Modifier
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
    onScanQRCode: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(52.dp))

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
            EditButton()
        }

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // First row of buttons
        Row {
            Button(
                onClick = { navController.navigate(Screen.EditProfile.route) },
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

        Spacer(modifier = Modifier.height(16.dp))

        // QR Scanner button
        Button(
            onClick = onScanQRCode,
            shape = RoundedCornerShape(45.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .defaultMinSize(
                    minWidth = ButtonDefaults.MinWidth,
                    minHeight = 10.dp
                )
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E8B57),
                contentColor = Color.White,
            )
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan QR Code",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Scan QR Code",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat(label = "SONGS", count = profile.addedSongsCount)
            ProfileStat(label = "LIKED", count = profile.likedSongsCount)
            ProfileStat(label = "LISTENED", count = profile.listenedSongsCount)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sound Capsule Section
        SoundCapsuleSection(
            navController = navController,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RectangleGradient() {
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
    }
}

@Composable
fun EditButton() {
    Button(
        onClick = {},
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .defaultMinSize(
                minWidth = 10.dp,
                minHeight = 10.dp
            )
            .size(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Edit",
            tint = Color.Black,
            modifier = Modifier
                .size(32.dp)
                .padding(all = 4.dp)
        )
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

@Composable
fun NoInternet() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No internet connection",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Don't worry! You can still play your music!",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

