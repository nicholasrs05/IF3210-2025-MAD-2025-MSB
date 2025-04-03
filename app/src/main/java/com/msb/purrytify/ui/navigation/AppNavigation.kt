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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.MainActivity
import com.msb.purrytify.ui.component.NavigationBarComponent
import com.msb.purrytify.ui.screen.*
import com.msb.purrytify.ui.theme.AppTheme


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
fun NavigationComponent() {
    AppTheme {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = { NavigationBarComponent(navController) }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Library.route) { LibraryScreen() }
                composable(Screen.Profile.route) { ProfileScreen(navController) }
                composable(Screen.Login.route) { LoginScreen(navController) }
            }
        }
    }
}