package com.msb.purrytify.ui.navigation

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.viewmodel.AuthViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.viewmodel.PlaybackViewModel
import com.msb.purrytify.ui.component.NavigationBarComponent
import com.msb.purrytify.ui.component.PlayerContainer
import com.msb.purrytify.ui.screen.*
import androidx.compose.ui.unit.dp
import com.msb.purrytify.R
import androidx.compose.ui.res.painterResource
import com.msb.purrytify.ui.theme.AppTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color

sealed class Screen(val route: String, val label: String, val icon: @Composable (isSelected: Boolean) -> Unit = { _ -> }) {
    data object Home : Screen("home", "Home", { isSelected ->
        Icon(
            painter = painterResource(
                id = if (isSelected) R.drawable.home_bold else R.drawable.home
            ),
            contentDescription = "Home",
            modifier = Modifier.size(24.dp)
        )
    })
    data object Library : Screen("library", "Your Library", { isSelected ->
        Icon(
            painter = painterResource(
                id = if (isSelected) R.drawable.library_bold else R.drawable.library
            ),
            contentDescription = "Your Library",
            modifier = Modifier.size(24.dp)
        )
    })
    data object Profile : Screen("profile", "Profile", { isSelected ->
        Icon(
            painter = painterResource(
                id = if (isSelected) R.drawable.profile_bold else R.drawable.profile
            ),
            contentDescription = "Profile",
            modifier = Modifier.size(24.dp)
        )
    })
    data object Login : Screen("login", "Login")
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DefaultPreviewDark"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "DefaultPreviewLight"
)

@Composable
fun NavigationComponent(
    authViewModel: AuthViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    AppTheme {
        Surface(
            color = Color(0xFF121212)
        ) {
            val navController = rememberNavController()
            val uiState by authViewModel.uiState.collectAsState()
            val isLoggedIn = uiState.isLoggedIn
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Observe navigation events from AuthViewModel
            LaunchedEffect(key1 = true) {
                authViewModel.navigateToHome.collect { shouldNavigate ->
                    if (shouldNavigate && currentRoute != Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }

            // Determine the start destination
            val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
            Log.d("Navigation", "isLoggedIn: $isLoggedIn, current route: $currentRoute")

            // When login status changes, navigate accordingly
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn && currentRoute == Screen.Login.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else if (!isLoggedIn && currentRoute != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            PlayerContainer(
                playerViewModel = playerViewModel,
                playbackViewModel = playbackViewModel,
                content = {
                    Scaffold(
                        bottomBar = {
                    if (isLoggedIn && currentRoute != Screen.Login.route) {
                        NavigationBarComponent(navController)
                    }
                }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) { HomeScreen() }
                            composable(Screen.Library.route) {
                                LibraryScreen(
                                    navController = navController,
                                    playerViewModel = playerViewModel,
                                    playbackViewModel = playbackViewModel
                                )
                            }
                            composable(Screen.Profile.route) { ProfileScreen() }
                            composable(Screen.Login.route) { LoginScreen(navController) }
                        }
                    }
                }
            )
        }
    }
}