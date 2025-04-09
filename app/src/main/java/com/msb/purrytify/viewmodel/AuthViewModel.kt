package com.msb.purrytify.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.msb.purrytify.data.storage.DataStoreManager
import com.msb.purrytify.model.AuthModel
import com.msb.purrytify.model.AuthResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

data class UiState(
    val isLoggedIn: Boolean = false,
    val loginError: String? = null,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val dataStoreManager: DataStoreManager, private val authModel: AuthModel) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _navigateToHome = MutableSharedFlow<Boolean>()
    val navigateToHome: SharedFlow<Boolean> = _navigateToHome

    init {
        viewModelScope.launch {
            dataStoreManager.authTokenFlow.collect { token ->
                _uiState.value = _uiState.value.copy(isLoggedIn = !token.isNullOrEmpty())
                if (_uiState.value.isLoggedIn) {
                    _navigateToHome.emit(true)
                }
            }
        }
    }

    fun setEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, emailError = null)
    }

    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, passwordError = null)
    }

    fun clearLoginError() {
        _uiState.value = _uiState.value.copy(loginError = null)
    }

    private fun validateEmail(): Boolean {
        return if (_uiState.value.email.isBlank()) {
            _uiState.value = _uiState.value.copy(emailError = "Email cannot be blank")
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(_uiState.value.email).matches()) {
            _uiState.value = _uiState.value.copy(emailError = "Invalid email format")
            false
        } else {
            true
        }
    }

    private fun validatePassword(): Boolean {
        return if (_uiState.value.password.isBlank()) {
            _uiState.value = _uiState.value.copy(passwordError = "Password cannot be blank")
            false
        } else {
            true
        }
    }

    fun loginWithValidation() {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()

        if (isEmailValid && isPasswordValid) {
            login()
        }
    }

    private fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loginError = null)
            val email = _uiState.value.email
            val password = _uiState.value.password

            val result = authModel.login(email, password)

            when (result) {
                is AuthResult.Success -> {
                    dataStoreManager.saveAuthToken(result.data.accessToken)
                    dataStoreManager.saveRefreshToken(result.data.refreshToken)
                    Log.d("AuthViewModel", "Login successful")
                    Log.d("AuthViewModel", "Access Token: ${result.data.accessToken}")
                    Log.d("AuthViewModel", "Refresh Token: ${result.data.refreshToken}")

                    _navigateToHome.emit(true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, loginError = result.message)
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearCredentials()
        }
    }
}