package com.msb.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import com.msb.purrytify.ui.navigation.NavigationComponent
import com.msb.purrytify.ui.theme.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.msb.purrytify.ui.component.NetworkPopUp
import com.msb.purrytify.utils.NetworkStatusListener
import com.msb.purrytify.viewmodel.PlayerViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val isConnected = NetworkStatusListener()
                val playerViewModel: PlayerViewModel = hiltViewModel()
                val isMiniPlayerVisible = playerViewModel.currentSong.value != null

                Box(modifier = Modifier.fillMaxSize()) {
                    NavigationComponent()

                    NetworkPopUp(
                        isConnected = isConnected,
                        isMiniPlayerVisible = isMiniPlayerVisible
                    )
                }
            }
        }
    }
}
