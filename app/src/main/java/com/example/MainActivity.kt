package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookViewModel
import com.example.ui.viewmodel.BookViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize the single ViewModel using our factory pattern
                val factory = BookViewModelFactory(application)
                val viewModel: BookViewModel = viewModel(factory = factory)
                val navController = rememberNavController()

                MainAppScreenHost(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "달력", Icons.Default.CalendarMonth)
    object Report : Screen("report", "리포트", Icons.Default.BarChart)
    object Search : Screen("search", "검색", Icons.Default.Search)
    object Goals : Screen("goals", "목표", Icons.Default.Flag)
}

@Composable
fun MainAppScreenHost(
    viewModel: BookViewModel,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if the current screen is a main tab to display the bottom bar
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Report.route,
        Screen.Search.route,
        Screen.Goals.route
    )

    val startDest = remember {
        if (viewModel.getChildName().isEmpty()) "child_name_input" else Screen.Home.route
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(Screen.Home, Screen.Report, Screen.Search, Screen.Goals)
                    tabs.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("child_name_input") {
                ChildNameInputScreen(
                    onBack = {
                        // Pop back stack if possible, otherwise finish activity
                        if (!navController.popBackStack()) {
                            (navController.context as? android.app.Activity)?.finish()
                        }
                    },
                    onComplete = { name, gender, photoUri ->
                        viewModel.setChildName(name)
                        viewModel.setChildGender(gender)
                        viewModel.setChildPhotoUri(photoUri)
                        navController.navigate(Screen.Home.route) {
                            popUpTo("child_name_input") { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToBookDetail = { bookId ->
                        navController.navigate("book_detail/$bookId")
                    },
                    onNavigateToAddBook = {
                        navController.navigate("add_book")
                    },
                    onNavigateToNewSession = { bookId ->
                        navController.navigate("book_detail/$bookId")
                    },
                    onNavigateToProfile = {
                        navController.navigate("child_name_input")
                    }
                )
            }
            
            composable(Screen.Report.route) {
                ReportScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = {
                        navController.navigate("child_name_input")
                    }
                )
            }
            
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToBookDetail = { bookId ->
                        navController.navigate("book_detail/$bookId")
                    },
                    onNavigateToProfile = {
                        navController.navigate("child_name_input")
                    }
                )
            }
            
            composable(Screen.Goals.route) {
                GoalScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = {
                        navController.navigate("child_name_input")
                    }
                )
            }
            
            composable(
                route = "book_detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.IntType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getInt("bookId") ?: 0
                BookDetailScreen(
                    viewModel = viewModel,
                    bookId = bookId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("add_book") {
                AddBookScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { bookId ->
                        // After successfully saving, go back and open details of that book directly!
                        navController.popBackStack()
                        navController.navigate("book_detail/$bookId")
                    }
                )
            }
        }
    }
}
