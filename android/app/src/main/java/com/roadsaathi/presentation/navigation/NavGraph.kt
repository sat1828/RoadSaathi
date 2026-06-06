package com.roadsaathi.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roadsaathi.presentation.camera.CameraScreen
import com.roadsaathi.presentation.map.MapScreen
import com.roadsaathi.presentation.reports.ReportDetailScreen
import com.roadsaathi.presentation.reports.ReportsScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.Map.route, "Map", Icons.Default.Map),
    BottomNavItem(Routes.Reports.route, "Reports", Icons.Default.List),
    BottomNavItem(Routes.Profile.route, "Profile", Icons.Default.Person)
)

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Map.route) {
                MapScreen(
                    onNavigateToCamera = {
                        navController.navigate(Routes.Camera.route)
                    }
                )
            }

            composable(Routes.Camera.route) {
                CameraScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.Reports.route) {
                ReportsScreen(
                    onNavigateToDetail = { localId ->
                        navController.navigate(Routes.ReportDetail.createRoute(localId))
                    }
                )
            }

            composable(
                route = Routes.ReportDetail.ROUTE,
                arguments = listOf(
                    navArgument("localId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val localId = backStackEntry.arguments?.getString("localId") ?: return@composable
                ReportDetailScreen(
                    localId = localId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
