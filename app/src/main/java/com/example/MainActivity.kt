package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
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
    val startDest = remember {
        if (viewModel.getChildName().isEmpty()) "child_name_input" else "main_tabs"
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("child_name_input") {
            val initialName = viewModel.getChildName()
            val initialGender = viewModel.getChildGender().ifEmpty { "BOY" }
            val initialPhotoUri = viewModel.getChildPhotoUri()

            ChildNameInputScreen(
                initialName = initialName,
                initialGender = initialGender,
                initialPhotoUri = initialPhotoUri,
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
                    navController.navigate("main_tabs") {
                        popUpTo("child_name_input") { inclusive = true }
                    }
                }
            )
        }

        composable("main_tabs") {
            MainTabsScreen(
                viewModel = viewModel,
                navController = navController,
                onNavigateToBookDetail = { bookId ->
                    navController.navigate("book_detail/$bookId")
                },
                onNavigateToAddBook = {
                    navController.navigate("add_book")
                },
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainTabsScreen(
    viewModel: BookViewModel,
    navController: NavHostController,
    onNavigateToBookDetail: (Int) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(Screen.Home, Screen.Report, Screen.Search, Screen.Goals)
                tabs.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
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
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            // Beautiful scale and alpha transitions for premium page swiping effect
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = 0.96f + 0.04f * (1f - pageOffset.coerceIn(0f, 1f))
            val alpha = 0.6f + 0.4f * (1f - pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                when (page) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToBookDetail = onNavigateToBookDetail,
                        onNavigateToAddBook = onNavigateToAddBook,
                        onNavigateToNewSession = onNavigateToBookDetail,
                        onNavigateToProfile = onNavigateToProfile
                    )
                    1 -> ReportScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = onNavigateToProfile
                    )
                    2 -> SearchScreen(
                        viewModel = viewModel,
                        onNavigateToBookDetail = onNavigateToBookDetail,
                        onNavigateToProfile = onNavigateToProfile
                    )
                    3 -> GoalScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
        }
    }
}
