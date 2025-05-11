package com.msb.purrytify.ui.navigation

import android.content.res.Configuration
import android.util.Log
import androidx.collection.isNotEmpty
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.viewmodel.AuthViewModel
import com.msb.purrytify.viewmodel.PlayerViewModel
import com.msb.purrytify.ui.component.NavigationBarComponent
import com.msb.purrytify.ui.component.PlayerContainer
import com.msb.purrytify.ui.screen.*
import androidx.compose.ui.unit.dp
import com.msb.purrytify.R
import androidx.compose.ui.res.painterResource
import com.msb.purrytify.ui.theme.AppTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

sealed class Screen(
    val route: String,
    val label: String,
    val icon: @Composable (isSelected: Boolean) -> Unit = { _ -> }
) {
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

    data object SongDetail : Screen("song/{songId}", "Song Detail") {
        fun createRoute(songId: String): String {
            return "song/$songId"
        }
    }
}

// Preview annotations remain the same

@Composable
fun NavigationComponent(
    authViewModel: AuthViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isLandscape: Boolean
) {
    AppTheme {
        Surface(
            color = Color(0xFF121212)
        ) {
            val navController = rememberNavController()
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
            val isLoggedIn = uiState.isLoggedIn
            val isLoggedInCheckDone = uiState.isLoggedInCheckDone


            val startDestination =
                if (isLoggedInCheckDone && isLoggedIn) Screen.Home.route else Screen.Login.route

            LaunchedEffect(isLoggedIn, isLoggedInCheckDone) {
                if (isLoggedInCheckDone) {
                    val currentBackStackEntry = navController.currentBackStackEntry
                    val currentActualRoute = currentBackStackEntry?.destination?.route
                    if (isLoggedIn && currentActualRoute == Screen.Login.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else if (!isLoggedIn && currentActualRoute != Screen.Login.route) {
                        if (currentActualRoute != null) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            Log.d(
                                "NavigationEffect",
                                "Skipping navigation to Login: currentActualRoute is null."
                            )
                        }
                    }
                } else {
                    Log.w(
                        "NavigationEffect",
                        "Skipping navigation: NavController graph not ready yet."
                    )
                }

            }

            val navBackStackEntryForUI by navController.currentBackStackEntryAsState()
            val currentRouteForUI = navBackStackEntryForUI?.destination?.route

            if (!isLoggedInCheckDone) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PlayerContainer(
                    isLandscape = isLandscape,
                    playerViewModel = playerViewModel,
                    content = {
                        if (isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val showNavBar = isLoggedIn && currentRouteForUI != Screen.Login.route
                                if (showNavBar) {
                                    NavigationBarComponent(navController = navController, isLandscape = true)
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = startDestination,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        composable(Screen.Home.route) {
                                            HomeScreen(
                                                playerViewModel = playerViewModel,
                                            )
                                        }
                                        composable(Screen.Library.route) {
                                            LibraryScreen(
                                                playerViewModel = playerViewModel,
                                            )
                                        }
                                        composable(Screen.Profile.route) {
                                            ProfileScreen(
                                                authViewModel = authViewModel,
                                                playerViewModel = playerViewModel
                                            )
                                        }
                                        composable(Screen.Login.route) {
                                            LoginScreen(
                                                authViewModel = authViewModel
                                            )
                                        }
                                        composable(
                                            route = Screen.SongDetail.route,
                                            arguments = listOf(
                                                navArgument("songId") { type = NavType.StringType }
                                            ),
                                            deepLinks = listOf(
                                                navDeepLink {
                                                    uriPattern = "purritfy://song/{songId}"
                                                }
                                            )
                                        ) { backStackEntry ->
                                            val songId = backStackEntry.arguments?.getString("songId")
                                            LaunchedEffect(songId) {
                                                if (songId != null) {
                                                    playerViewModel.playSongById(songId)
                                                } else {
                                                    Log.e("Navigation", "Song ID is null")
                                                }
                                            }

                                            // Show minimal UI while loading or redirect
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                if (isLoggedIn) {
                                                    HomeScreen(playerViewModel = playerViewModel)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Scaffold(
                                bottomBar = {
                                    val showBottomBar =
                                        isLoggedIn && currentRouteForUI != Screen.Login.route
                                    if (showBottomBar) {
                                        NavigationBarComponent(navController)
                                    }
                                }
                            ) { innerPadding ->
                                NavHost(
                                    navController = navController,
                                    startDestination = startDestination,
                                    modifier = Modifier.padding(innerPadding)
                                ) {
                                    composable(Screen.Home.route) { HomeScreen(playerViewModel = playerViewModel) }
                                    composable(Screen.Library.route) {
                                        LibraryScreen(
                                            playerViewModel = playerViewModel,
                                        )
                                    }
                                    composable(Screen.Profile.route) { ProfileScreen(authViewModel=authViewModel, playerViewModel = playerViewModel) }
                                    composable(Screen.Login.route) {
                                        LoginScreen(
                                            authViewModel = authViewModel
                                        )
                                    }
                                    composable(
                                        route = Screen.SongDetail.route,
                                        arguments = listOf(
                                            navArgument("songId") { type = NavType.StringType }
                                        ),
                                        deepLinks = listOf(
                                            navDeepLink {
                                                uriPattern = "purrytify://song/{songId}"
                                            }
                                        )
                                    ) { backStackEntry ->
                                        val songId = backStackEntry.arguments?.getString("songId")
                                        LaunchedEffect(songId) {
                                            if (songId != null) {
                                                if (!isLoggedIn) {
                                                    // If not logged in, store the songId to play after login
                                                    // This could be stored in a temporary preference or viewmodel state
                                                    navController.navigate(Screen.Login.route)
                                                } else {
                                                    playerViewModel.playSongById(songId)
                                                    navController.navigate(Screen.Home.route) {
                                                        popUpTo(Screen.SongDetail.route) { inclusive = true }
                                                    }
                                                }
                                            } else {
                                                Log.e("Navigation", "Song ID is null")
                                                navController.navigate(Screen.Home.route)
                                            }
                                        }

                                        // Show minimal UI while loading or redirect
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}