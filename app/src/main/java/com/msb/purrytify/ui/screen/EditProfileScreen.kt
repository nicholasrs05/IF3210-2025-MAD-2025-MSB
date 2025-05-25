package com.msb.purrytify.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.msb.purrytify.ui.navigation.Screen
import com.msb.purrytify.viewmodel.EditProfileViewModel

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry("editProfileGraph")
    }
    val viewModel: EditProfileViewModel = hiltViewModel(parentEntry)
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    val countryCode = state.countryCode
    LaunchedEffect(countryCode) {
        Log.d("EditProfileScreen", "Country code changed to: $countryCode")
    }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.onPhotoSelected(it) }
        }
    )
    
    // Check current permission state (recomposes on change)
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    // Location permission launcher with proper handling
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                safeDetectLocation(viewModel, context)
            } else {
                onLocationPermissionDenied(context)
            }
        }
    )
    
    // Used for navigation after successful update
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val successMessage = state.successMessage
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCameraOptions by remember { mutableStateOf(false) }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(1500) // Delay to show success message
            shouldNavigateBack = true
        }
    }
    
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF101010),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF101010)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF101010))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                ) {
                    AsyncImage(
                        model = state.photoUri ?: state.currentProfilePhotoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .clickable { showCameraOptions = true },
                        contentScale = ContentScale.Crop
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Photo",
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp),
                        tint = Color(0xFF101010)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Country Code",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = state.countryCode,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color(0xFF00667B)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { 
                            if (hasLocationPermission) {
                                safeDetectLocation(viewModel, context)
                            } else {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(
                                        context as androidx.activity.ComponentActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                ) {
                                    showPermissionDialog = true
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = ButtonDefaults.outlinedButtonBorder(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00667B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Auto-detect Location",
                            tint = Color(0xFF00667B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-detect")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            navController.navigate(Screen.MapLocationPicker.route)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = ButtonDefaults.outlinedButtonBorder(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00667B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Select on Map",
                            tint = Color(0xFF00667B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick on Map")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save Button
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00667B),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF3E3F3F),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Error message
                state.error?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                // Success message
                state.successMessage?.let { successMsg ->
                    Text(
                        text = successMsg,
                        color = Color.Green,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Location permission dialog
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Location Permission") },
                    text = { Text("We need location permission to detect your country. This helps personalize your experience.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                showPermissionDialog = false
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    containerColor = Color(0xFF202020),
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray
                )
            }
            
            // Photo options dialog
            if (showCameraOptions) {
                Dialog(onDismissRequest = { showCameraOptions = false }) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF202020)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Choose Photo",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                        showCameraOptions = false
                                    },
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF303030))
                                        .padding(16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Photo,
                                            contentDescription = "Gallery",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Gallery",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = {
                                        viewModel.launchCamera(context)
                                        showCameraOptions = false
                                    },
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF303030))
                                        .padding(16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Camera,
                                            contentDescription = "Camera",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Camera",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            TextButton(
                                onClick = { showCameraOptions = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFF00667B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safely call detectLocation with proper error handling
 */
private fun safeDetectLocation(viewModel: EditProfileViewModel, context: Context) {
    try {
        viewModel.detectLocation(context)
    } catch (se: SecurityException) {
        Log.e("EditProfile", "Location permission lost at runtime", se)
        Toast.makeText(
            context,
            "Location permission was revoked. Please grant permission again.",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Handle location permission denial
 */
private fun onLocationPermissionDenied(context: Context) {
    Toast.makeText(
        context,
        "Location permission is required to auto-detect your country. You can still enter it manually.",
        Toast.LENGTH_LONG
    ).show()
}
