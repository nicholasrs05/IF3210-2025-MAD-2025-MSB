package com.msb.purrytify.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.msb.purrytify.viewmodel.EditProfileViewModel
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerScreen(
    navController: NavController
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry("editProfileGraph")
    }
    val viewModel: EditProfileViewModel = hiltViewModel(parentEntry)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var countryCode by remember { mutableStateOf<String?>(null) }
    var countryName by remember { mutableStateOf<String?>(null) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
        }
    )
    
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    val defaultCamera = remember {
        CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }
    
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = true
        )
    }
    
    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = false
        )
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCamera
    }
    
    // UI for map screen
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Select Your Country") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF101010),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    if (countryCode != null) {
                        IconButton(
                            onClick = {
                                viewModel.updateCountryCodeFromExternal(countryCode!!)
                                android.util.Log.d("MapLocationPicker", "TopBar: Confirming country code: $countryCode")
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Confirm",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFF101010)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings,
                properties = mapProperties,
                onMapClick = { latLng ->
                    selectedLatLng = latLng
                    isLoading = true
                    
                    coroutineScope.launch {
                        val countryInfo = getCountryInfo(context, latLng)
                        countryCode = countryInfo.first
                        countryName = countryInfo.second
                        isLoading = false
                    }
                }
            ) {
                selectedLatLng?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = "Selected Location"
                    )
                }
            }
            
            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center),
                    color = Color(0xFF00667B)
                )
            }
            
            // Country code info panel
            countryCode?.let { code ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF202020)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${countryName ?: "Unknown Country"} (${code})",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Is this the correct country?",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.updateCountryCodeFromExternal(code)
                                android.util.Log.d("MapLocationPicker", "Confirming country code: $code")
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00667B),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Selection")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun getCountryInfo(context: Context, latLng: LatLng): Pair<String, String?> {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            var countryCode: String? = null
            var countryName: String? = null
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        countryCode = addresses[0].countryCode
                        countryName = addresses[0].countryName
                    }
                }
                kotlinx.coroutines.delay(500)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    countryCode = addresses[0].countryCode
                    countryName = addresses[0].countryName
                }
            }
            
            if (countryCode.isNullOrBlank()) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                countryCode = telephonyManager?.networkCountryIso?.uppercase()
                
                if (countryCode.isNullOrBlank()) {
                    countryCode = "??"
                }
            }
            
            Pair(countryCode, countryName)
        } catch (e: Exception) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val countryCode = telephonyManager?.networkCountryIso?.uppercase() ?: "??"
            Pair(countryCode, null)
        }
    }
}


