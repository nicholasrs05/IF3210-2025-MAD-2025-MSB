package com.msb.purrytify

import android.content.res.Configuration
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
import com.msb.purrytify.ui.navigation.NavigationComponent
import com.msb.purrytify.ui.theme.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.msb.purrytify.ui.component.NetworkPopUp
import com.msb.purrytify.utils.NetworkStatusListener
import com.msb.purrytify.viewmodel.PlayerViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Process deep link if coming from intent
        // Note: This is actually handled by the Navigation component with navDeepLink
        // but we'll log it for debugging purposes
        intent?.data?.let { uri ->
            if (uri.scheme == "purrytify" && uri.host == "song") {
                val songId = uri.lastPathSegment
                Log.d("MainActivity", "Received deep link for song ID: $songId")
            }
        }
        
        setContent {
            AppTheme {
                val configuration = resources.configuration
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                val isConnected = NetworkStatusListener()
                val playerViewModel: PlayerViewModel = hiltViewModel()
                val isMiniPlayerVisible = playerViewModel.isMiniPlayerVisible.value

                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(isLandscape = isLandscape)

                    NetworkPopUp(
                        isLandscape = isLandscape,
                        isConnected = isConnected,
                        isMiniPlayerVisible = isMiniPlayerVisible
                    )

                }
            }
        }
    }
}

@Composable
fun MainContent(
    isLandscape: Boolean = false
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsState()
    val isLoggedInCheckDone = uiState.isLoggedInCheckDone

    Log.d("MainActivity", "isLoggedInCheckDone: $isLoggedInCheckDone")

    val localView = LocalView.current

    DisposableEffect(Unit) {
        val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                return if (authViewModel.isLoggedInCheckDone()) {
                    localView.viewTreeObserver.removeOnPreDrawListener(this)
                    true
                } else {
                    false
                }
            }
        }

        localView.viewTreeObserver.addOnPreDrawListener(preDrawListener)

        onDispose {
            Log.d("MainActivity", "Disposing and removing preDrawListener")
            localView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        }
    }

    NavigationComponent(isLandscape = isLandscape)
}