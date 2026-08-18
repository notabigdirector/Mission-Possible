package com.mission.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mission.app.ui.ProjectsRoute
import com.mission.app.ui.SyncRoute
import com.mission.app.ui.TaskEditRoute
import com.mission.app.ui.TaskListRoute
import com.mission.app.ui.theme.MissionAppTheme

object Routes {
    const val TASKS = "tasks"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
    const val TASK_NEW = "task_new"
    const val TASK_EDIT = "task_edit/{taskId}"
    const val TASK_EDIT_ARG = "taskId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MissionApp).container
        setContent {
            MissionAppTheme {
                MissionNavHost(container)
            }
        }
    }
}

@Composable
fun MissionNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.TASKS, Routes.PROJECTS, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(currentRoute = currentRoute, onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TASKS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TASKS) {
                TaskListRoute(
                    container = container,
                    onAddTask = { navController.navigate(Routes.TASK_NEW) },
                    onEditTask = { id -> navController.navigate("task_edit/$id") },
                    onOpenSync = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsRoute(container)
            }
            composable(Routes.SETTINGS) {
                SyncRoute(container, onClose = { navController.popBackStack() })
            }
            composable(Routes.TASK_NEW) {
                TaskEditRoute(
                    container = container,
                    taskId = null,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.TASK_EDIT,
                arguments = listOf(navArgument(Routes.TASK_EDIT_ARG) { type = NavType.StringType })
            ) { entry ->
                TaskEditRoute(
                    container = container,
                    taskId = entry.arguments?.getString(Routes.TASK_EDIT_ARG),
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple(Routes.TASKS, "任务", Icons.AutoMirrored.Filled.List),
        Triple(Routes.PROJECTS, "项目", Icons.Default.Folder),
        Triple(Routes.SETTINGS, "设置", Icons.Default.Settings)
    )
    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}