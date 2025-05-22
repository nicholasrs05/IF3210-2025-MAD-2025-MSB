package com.msb.purrytify.ui.navigation

import android.util.Log
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
import com.msb.purrytify.qr.ModernQRScannerScreen
import com.msb.purrytify.ui.screen.EditProfileScreen
import com.msb.purrytify.ui.screen.HomeScreen
import com.msb.purrytify.ui.screen.LibraryScreen
import com.msb.purrytify.ui.screen.LoginScreen
import com.msb.purrytify.ui.screen.MapLocationPickerScreen
import com.msb.purrytify.ui.screen.ProfileScreen
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
import androidx.navigation.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.msb.purrytify.ui.screen.FiftyGlobalScreen
import com.msb.purrytify.ui.screen.TenCountryScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.msb.purrytify.data.DummyData
import com.msb.purrytify.ui.screen.TimeListenedScreen
import com.msb.purrytify.ui.screen.TopArtistScreen
import com.msb.purrytify.ui.screen.TopSongScreen

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

    data object QRScanner : Screen("qr_scanner", "QR Scanner")

    data object EditProfile : Screen("edit_profile", "Edit Profile")

    data object MapLocationPicker : Screen("map_location_picker", "Map Location Picker")

    data object FiftyGlobal : Screen("fifty_global", "Fifty Global")

    data object Top10Country : Screen("top10_country", "Top 10 Country")

    data object SongDetail : Screen("song/{songId}", "Song Detail") {
        fun createRoute(songId: String): String {
            return "song/$songId"
        }
    }

    data object TopArtists : Screen("top_artists/{soundCapsuleId}", "Top Artists") {
        fun createRoute(soundCapsuleId: Long): String {
            return "top_artists/$soundCapsuleId"
        }
    }

    data object TopSongs : Screen("top_songs/{soundCapsuleId}", "Top Songs") {
        fun createRoute(soundCapsuleId: Long): String {
            return "top_songs/$soundCapsuleId"
        }
    }

    data object TimeListened : Screen("time_listened/{soundCapsuleId}", "Time Listened") {
        fun createRoute(soundCapsuleId: Long): String {
            return "time_listened/$soundCapsuleId"
        }
    }
}

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
                                                navController = navController
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
                                                playerViewModel = playerViewModel,
                                                navController = navController
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
                                                    HomeScreen(
                                                        playerViewModel = playerViewModel,
                                                        navController = navController
                                                    )
                                                }
                                            }
                                        }
                                        composable(
                                            route = Screen.TopArtists.route,
                                            arguments = listOf(
                                                navArgument("soundCapsuleId") { type = NavType.LongType },
                                            )
                                        ) { backStackEntry ->
                                            val soundCapsuleId = backStackEntry.arguments?.getLong("soundCapsuleId") ?: 0L
                                            TopArtistScreen(
                                                soundCapsuleId = soundCapsuleId,
                                                onBackClick = { navController.popBackStack() }
                                            )
                                        }
                                        composable(
                                            route = Screen.TopSongs.route,
                                            arguments = listOf(
                                                navArgument("soundCapsuleId") { type = NavType.LongType },
                                            )
                                        ) { backStackEntry ->
                                            val soundCapsuleId = backStackEntry.arguments?.getLong("soundCapsuleId") ?: 0L
                                            TopSongScreen(
                                                soundCapsuleId = soundCapsuleId,
                                                onBackClick = { navController.popBackStack() }
                                            )
                                        }
                                        composable(
                                            route = Screen.TimeListened.route,
                                            arguments = listOf(
                                                navArgument("soundCapsuleId") { type = NavType.LongType },
                                            )
                                        ) { backStackEntry ->
                                            val soundCapsuleId = backStackEntry.arguments?.getLong("soundCapsuleId") ?: 0L
                                            TimeListenedScreen(
                                                    soundCapsuleId,
                                                    onBackClick = { navController.popBackStack() }
                                                )
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
                                    composable(Screen.Home.route) {
                                        HomeScreen(
                                            playerViewModel = playerViewModel,
                                            navController = navController
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
                                            playerViewModel = playerViewModel,
                                            navController = navController
                                        )
                                    }
                                    composable(Screen.Login.route) {
                                        LoginScreen(
                                            authViewModel = authViewModel
                                        )
                                    }
                                    composable(Screen.QRScanner.route) {
                                        ModernQRScannerScreen(
                                            navigateUp = { navController.navigateUp() },
                                            onQRCodeScanned = { songId ->
                                                playerViewModel.playSongById(songId)
                                                navController.navigate(Screen.Home.route) {
                                                    popUpTo(Screen.QRScanner.route) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    navigation(
                                        route = "editProfileGraph",
                                        startDestination = Screen.EditProfile.route
                                    ) {
                                        composable(Screen.EditProfile.route) {
                                            EditProfileScreen(
                                                navController = navController
                                            )
                                        }
                                        composable(Screen.MapLocationPicker.route) {
                                            MapLocationPickerScreen(
                                                navController = navController
                                            )
                                        }
                                    }
                                    composable(Screen.FiftyGlobal.route) {
                                        var isDismissing by remember { mutableStateOf(false) }

                                        FiftyGlobalScreen(
                                            onDismiss = {
                                                navController.navigateUp()
                                            },
                                            onDismissWithAnimation = {
                                                isDismissing = true
                                            },
                                            isDismissing = isDismissing,
                                            onAnimationComplete = {
                                                isDismissing = false
                                            },
                                            playerViewModel = playerViewModel
                                        )
                                    }
                                    composable(Screen.Top10Country.route) {
                                        var isDismissing by remember { mutableStateOf(false) }

                                        TenCountryScreen(
                                            onDismiss = {
                                                navController.navigateUp()
                                            },
                                            onDismissWithAnimation = {
                                                isDismissing = true
                                            },
                                            isDismissing = isDismissing,
                                            onAnimationComplete = {
                                                isDismissing = false
                                            },
                                            playerViewModel = playerViewModel
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
                                    composable(
                                        route = Screen.TopArtists.route,
                                        arguments = listOf(
                                            navArgument("soundCapsuleId") { type = NavType.LongType },
                                        )
                                    ) { backStackEntry ->
                                        val soundCapsuleId = backStackEntry.arguments?.getLong("soundCapsuleId") ?: 0L
                                        TopArtistScreen(
                                            soundCapsuleId = soundCapsuleId,
                                            onBackClick = { navController.popBackStack() }
                                        )
                                    }
                                    composable(
                                        route = Screen.TopSongs.route,
                                        arguments = listOf(
                                            navArgument("soundCapsuleId") { type = NavType.LongType },
                                        )
                                    ) { backStackEntry ->
                                        val soundCapsuleId = backStackEntry.arguments?.getLong("soundCapsuleId") ?: 0L
                                        TopSongScreen(
                                            soundCapsuleId = soundCapsuleId,
                                            onBackClick = { navController.popBackStack() }
                                        )
                                    }
                                    composable(
                                        route = Screen.TimeListened.route,
                                        arguments = listOf(
                                            navArgument("soundCapsuleId") { type = NavType.LongType },
                                        )
                                    ) { backStackEntry ->
                                        val soundCapsuleId = backStackEntry.arguments?.getLong("soundCapsuleId") ?: 0L
                                        TimeListenedScreen(
                                                    soundCapsuleId,
                                                    onBackClick = { navController.popBackStack() }
                                                )
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