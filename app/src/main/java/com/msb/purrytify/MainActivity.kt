package com.msb.purrytify

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import com.msb.purrytify.ui.navigation.NavigationComponent
import com.msb.purrytify.ui.theme.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.msb.purrytify.ui.component.NetworkPopUp
import com.msb.purrytify.utils.NetworkStatusListener

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val isConnected = NetworkStatusListener()

                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent()

                    NetworkPopUp(isConnected = isConnected)
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsState()
    val isLoggedInCheckDone = uiState.isLoggedInCheckDone

    Log.d("MainActivity", "isLoggedInCheckDone: $isLoggedInCheckDone")

    val localView = LocalView.current

    DisposableEffect(Unit) {
        val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                // Log.d("MainActivity", "onPreDraw called, isLoggedInCheckDone: $isLoggedInCheckDone")
                return if (authViewModel.isLoggedInCheckDone()) {
                    // Log.d("MainActivity", "Removing preDrawListener and allowing draw")
                    localView.viewTreeObserver.removeOnPreDrawListener(this)
                    true
                } else {
                    // Log.d("MainActivity", "Preventing draw, waiting for auth check")
                    false
                }
            }
        }

        // Log.d("MainActivity", "Adding preDrawListener")
        localView.viewTreeObserver.addOnPreDrawListener(preDrawListener)

        onDispose {
            Log.d("MainActivity", "Disposing and removing preDrawListener")
            localView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        }
    }

    NavigationComponent()
}