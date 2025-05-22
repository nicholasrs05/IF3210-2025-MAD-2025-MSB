package com.msb.purrytify.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.msb.purrytify.data.repository.ProfileRepository
import com.msb.purrytify.data.repository.ProfileUpdateResult
import com.msb.purrytify.model.ProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val countryCode: String = "",
    val photoUri: Uri? = null,
    val currentProfilePhotoUrl: String? = null,
    val photoChanged: Boolean = false,
    val locationChanged: Boolean = false,
    val canSave: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val profileModel: ProfileModel,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()
    
    private var tempPhotoFile: File? = null

    init {
        viewModelScope.launch {
            profileModel.currentProfile.collect { profile ->
                _uiState.update { currentState ->
                    currentState.copy(
                        countryCode = profile.location,
                        currentProfilePhotoUrl = profile.profilePhotoUrl
                    )
                }
            }
        }
    }
    
    fun onCountryCodeTyped(code: String) {
        viewModelScope.launch {
            val newState = _uiState.value.copy(
                countryCode = code,
                locationChanged = code != profileModel.currentProfile.value.location,
                canSave = code != profileModel.currentProfile.value.location || _uiState.value.photoChanged,
                error = null
            )
            _uiState.emit(newState)
            Log.d("EditProfileViewModel", "Country code updated to: $code, can save: ${newState.canSave}")
        }
    }
    
    fun onPhotoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(
                photoUri = uri,
                photoChanged = true,
                canSave = true
            ))
            Log.d("EditProfileViewModel", "Photo selected: $uri")
        }
    }
    
    fun updateCountryCodeFromExternal(code: String) {
        viewModelScope.launch {
            val newState = _uiState.value.copy(
                countryCode = code,
                locationChanged = code != profileModel.currentProfile.value.location,
                canSave = code != profileModel.currentProfile.value.location || _uiState.value.photoChanged,
                error = null
            )
            _uiState.emit(newState)
            Log.d("EditProfileViewModel", "Country code updated externally to: $code, can save: ${newState.canSave}")
        }
    }
    
    fun detectLocation(context: Context) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(isLoading = true))
            
            try {
                val fusedLocationClient: FusedLocationProviderClient = 
                    LocationServices.getFusedLocationProviderClient(context)
                    
                val cancellationToken = object : CancellationToken() {
                    override fun onCanceledRequested(listener: OnTokenCanceledListener) = 
                        CancellationTokenSource().token
                        
                    override fun isCancellationRequested() = false
                }
                
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            getCountryCodeFromLocation(context, it.latitude, it.longitude)
                        } ?: run {
                            viewModelScope.launch {
                                _uiState.emit(_uiState.value.copy(
                                    isLoading = false,
                                    error = "Could not detect location. Please try again or enter country code manually."
                                ))
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch {
                            val errorMessage = when (e) {
                                is SecurityException -> "Location permission is required. Please grant permission and try again."
                                else -> "Location detection failed: ${e.localizedMessage}"
                            }
                            _uiState.emit(_uiState.value.copy(
                                isLoading = false,
                                error = errorMessage
                            ))
                        }
                    }
            } catch (se: SecurityException) {
                _uiState.emit(_uiState.value.copy(
                    isLoading = false,
                    error = "Location permission is required. Please grant permission and try again."
                ))
                Log.e("EditProfileViewModel", "SecurityException in detectLocation", se)
            } catch (e: Exception) {
                _uiState.emit(_uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.localizedMessage}"
                ))
                Log.e("EditProfileViewModel", "Exception in detectLocation", e)
            }
        }
    }
    
    fun getCountryCodeFromLocation(context: Context, latitude: Double, longitude: Double) {
        val geocoder = Geocoder(context, Locale.getDefault())
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    processAddresses(addresses)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null) {
                    processAddresses(addresses)
                } else {
                    useCountryCodeFromTelephony()
                }
            }
        } catch (e: Exception) {
            useCountryCodeFromTelephony()
        }
    }
    
    private fun processAddresses(addresses: List<Address>) {
        if (addresses.isNotEmpty()) {
            val countryCode = addresses[0].countryCode
            if (!countryCode.isNullOrEmpty()) {
                viewModelScope.launch {
                    _uiState.emit(_uiState.value.copy(
                        countryCode = countryCode,
                        locationChanged = countryCode != profileModel.currentProfile.value.location,
                        isLoading = false,
                        canSave = countryCode != profileModel.currentProfile.value.location || _uiState.value.photoChanged,
                        error = null
                    ))
                    
                    // Log the update
                    Log.d("EditProfileViewModel", "Location retrieved: $countryCode")
                }
            } else {
                useCountryCodeFromTelephony()
            }
        } else {
            useCountryCodeFromTelephony()
        }
    }
    
    private fun useCountryCodeFromTelephony() {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val countryIso = telephonyManager?.networkCountryIso
        
        if (!countryIso.isNullOrBlank()) {
            val code = countryIso.uppercase()
            viewModelScope.launch {
                _uiState.emit(_uiState.value.copy(
                    countryCode = code,
                    locationChanged = code != profileModel.currentProfile.value.location,
                    isLoading = false,
                    canSave = code != profileModel.currentProfile.value.location || _uiState.value.photoChanged,
                    error = null
                ))
                Log.d("EditProfileViewModel", "TelephonyManager country: $code")
            }
        } else {
            viewModelScope.launch {
                _uiState.emit(_uiState.value.copy(
                    isLoading = false,
                    error = "Could not determine country code from network. Please enter it manually."
                ))
            }
        }
    }
    
    fun launchCamera(context: Context) {
        viewModelScope.launch {
            try {
                val photoFile = withContext(Dispatchers.IO) {
                    createImageFile(context)
                }
                tempPhotoFile = photoFile
                
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                
                val takePictureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
                
                if (takePictureIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(takePictureIntent)
                    onPhotoSelected(photoUri)
                }
            } catch (e: Exception) {
                viewModelScope.launch {
                    _uiState.emit(_uiState.value.copy(
                        error = "Failed to launch camera: ${e.localizedMessage}"
                    ))
                }
            }
        }
    }
    
    private fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(null)
        
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            try {
                _uiState.emit(_uiState.value.copy(isLoading = true, error = null, successMessage = null))
                
                val state = _uiState.value
                
                val locationPart = if (state.locationChanged && state.countryCode.isNotBlank()) {
                    MultipartBody.Part.createFormData("location", state.countryCode)
                } else null
                
                val photoPart = if (state.photoChanged && state.photoUri != null) {
                    val photoFile = withContext(Dispatchers.IO) {
                        val file = File(context.cacheDir, "profile_photo.jpg")
                        context.contentResolver.openInputStream(state.photoUri)?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                        file
                    }
                    
                    val requestBody = photoFile.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("profilePhoto", photoFile.name, requestBody)
                } else null
                
                if (locationPart != null || photoPart != null) {
                    val result = profileRepository.updateProfile(locationPart, photoPart)
                    
                    when (result) {
                        is ProfileUpdateResult.Success -> {
                            _uiState.emit(_uiState.value.copy(
                                isLoading = false,
                                successMessage = result.message,
                                error = null,
                                photoChanged = false,
                                locationChanged = false,
                                canSave = false,
                                countryCode = result.profile.location,
                                currentProfilePhotoUrl = result.profile.profilePhotoUrl
                            ))
                            
                            profileModel.fetchProfile()
                            kotlinx.coroutines.delay(1500)
                        }
                        is ProfileUpdateResult.Error -> {
                            _uiState.emit(_uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            ))
                        }
                    }
                } else {
                    _uiState.emit(_uiState.value.copy(
                        isLoading = false,
                        error = "No changes to save"
                    ))
                }
            } catch (e: Exception) {
                _uiState.emit(_uiState.value.copy(
                    isLoading = false,
                    error = "Error updating profile: ${e.localizedMessage}"
                ))
            }
        }
    }
}