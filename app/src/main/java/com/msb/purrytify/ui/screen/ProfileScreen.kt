package com.msb.purrytify.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.msb.purrytify.ui.navigation.Screen

@Composable
fun ProfileScreen(navController: NavHostController) {
    val isAuthenticated = false //Dummy auth

    if (isAuthenticated) {
        Column {
            Text("Profile Screen")
        }
    } else {
        navController.navigate(Screen.Login.route)
    }
}