package com.msb.purrytify.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val profileUiState by viewModel.profileState.collectAsState(initial = ProfileUiState.Loading)

    Box(
        modifier = Modifier
            .fillMaxSize().background(Color(0xFF101010))
    ) {

        when (profileUiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ProfileUiState.Success -> {
                RectangleGradient()
                ProfileContent(profile = (profileUiState as ProfileUiState.Success).profile)
            }

            is ProfileUiState.Error -> {
                ErrorScreen(errorMessage = (profileUiState as ProfileUiState.Error).errorMessage)
            }

            else -> {
                // SKIP
            }
        }
    }
}

@Composable
fun ProfileContent(profile: com.msb.purrytify.data.model.Profile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(52.dp))

        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.wrapContentWidth().width(120.dp).height(120.dp)){
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
            // Username
            Text(
                text = profile.username,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Location
            Text(
                text = profile.location,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

        }

        Spacer(modifier = Modifier.height(16.dp))

        Button (
            onClick = {},
            shape = RoundedCornerShape(45.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = 10.dp
            ).width(105.dp).height(32.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Song Counts
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
        Button (
            onClick = {},
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.defaultMinSize(
                minWidth = 10.dp,
                minHeight = 10.dp
            ).size(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit",
                tint = Color.Black,
                modifier = Modifier.size(32.dp).padding(all = 4.dp)
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
        // Error Icon using Material Icons
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = "Error Icon",
            tint = Color.Red,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
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