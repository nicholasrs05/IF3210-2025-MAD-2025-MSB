package com.msb.purrytify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
//import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.msb.purrytify.ui.navigation.Screen
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
fun NavigationBarComponent(
    navController: NavHostController,
    isLandscape: Boolean = false
) {
    val items = listOf(Screen.Home, Screen.Library, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (isLandscape) {
        NavigationRail(
            containerColor = Color(0xFF121212),
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .fillMaxHeight()
        ) {
            items.forEach { screen ->
                val isSelected = currentDestination?.route == screen.route
                NavigationRailItem(
                    icon = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            screen.icon(isSelected)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = screen.label,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    } else {
        NavigationBar (
            containerColor = Color(0xFF121212),
            contentColor = Color.White
        ) {
            items.forEach { screen ->
                val isSelected = currentDestination?.route == screen.route

                NavigationBarItem(
                    icon = {
                        screen.icon(isSelected)
                    },
                    label = {
                        Text(
                            text = screen.label,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    selected = false,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

}