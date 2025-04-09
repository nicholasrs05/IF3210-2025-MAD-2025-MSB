package com.msb.purrytify.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.msb.purrytify.viewmodel.AuthViewModel
import com.msb.purrytify.ui.component.NavigationBarComponent
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
fun NavigationComponent(authViewModel: AuthViewModel = hiltViewModel()) {
    AppTheme {
        Surface(
            color = Color(0xFF121212) // This is #121212
        ) {
            val navController = rememberNavController()
            val isLoggedIn = authViewModel.uiState.collectAsState().value.isLoggedIn

            val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

            Scaffold(
                bottomBar = { if (isLoggedIn) NavigationBarComponent(navController) else null }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) { HomeScreen() }
                    composable(Screen.Library.route) { LibraryScreen(navController) }
                    composable(Screen.Profile.route) { ProfileScreen() }
                    composable(Screen.Login.route) { LoginScreen(navController) }
                }
            }
        }
    }
}