package com.msb.purrytify.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.msb.purrytify.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
)  {
    Column {
        Text("Profile Screen")
    }
}