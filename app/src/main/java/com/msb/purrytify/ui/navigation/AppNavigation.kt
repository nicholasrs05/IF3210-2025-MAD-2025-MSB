package com.msb.purrytify.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.ui.component.NavigationBarComponent
import com.msb.purrytify.ui.screen.*
import com.msb.purrytify.viewmodel.AuthViewModel


sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Home : Screen("home", "Home", { Icon(Icons.Filled.Home, contentDescription = "Home") })
    data object Library : Screen("library", "Library", { Icon(Icons.Filled.Menu, contentDescription = "Library") })
    data object Profile : Screen("profile", "Profile", { Icon(Icons.Filled.Person, contentDescription = "Profile") })
    data object Login : Screen("login", "Login", {})
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
fun NavigationComponent(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isLoggedIn = authViewModel.uiState.collectAsState().value.isLoggedIn

    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    Scaffold(
        bottomBar = { if (isLoggedIn) NavigationBarComponent(navController) else null }
    ) { innerPadding ->
        NavHost(navController, startDestination = startDestination, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Library.route) { LibraryScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Login.route) { LoginScreen(navController) }
        }
    }
}
