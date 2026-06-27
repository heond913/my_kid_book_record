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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChildProfile
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
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

        // 1. Check if we launched because of an uncaught exception
        val errorExtra = intent?.getStringExtra("KEY_ERROR_MSG")
        if (errorExtra != null) {
            setContent {
                MyApplicationTheme {
                    ErrorFallbackScreen(
                        errorMsg = errorExtra,
                        onReset = {
                            clearAppDataAndRestart()
                        }
                    )
                }
            }
            return
        }

        // 2. Set default uncaught exception handler to prevent hard crashes and show standard Error Screen
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val errorDetails = "Error: ${throwable.localizedMessage ?: throwable.toString()}\n\n" +
                        throwable.stackTrace.take(15).joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
                
                val intent = android.content.Intent(this, MainActivity::class.java).apply {
                    putExtra("KEY_ERROR_MSG", errorDetails)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                java.lang.System.exit(10)
            } catch (e: Exception) {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }

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

    private fun clearAppDataAndRestart() {
        try {
            // Delete the database file
            deleteDatabase("kids_book_journal_db")
            
            // Clear SharedPreferences
            getSharedPreferences("book_journal_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
                
            // Restart the app normally
            val intent = android.content.Intent(this, MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(0)
        } catch (e: Exception) {
            // Fallback: just restart normally
            val intent = android.content.Intent(this, MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(0)
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
                    if (!navController.popBackStack()) {
                        (navController.context as? android.app.Activity)?.finish()
                    }
                },
                onComplete = { name, gender, photoUri ->
                    viewModel.setChildName(name)
                    viewModel.setChildGender(gender)
                    viewModel.setChildPhotoUri(photoUri)
                    val profile = ChildProfile(
                        name = name,
                        gender = gender,
                        photoUri = photoUri,
                        colorHex = "#8B5CF6"
                    )
                    viewModel.addProfile(profile)
                    navController.navigate("main_tabs") {
                        popUpTo("child_name_input") { inclusive = true }
                    }
                }
            )
        }

        composable("add_profile") {
            ChildNameInputScreen(
                initialName = "",
                initialGender = "BOY",
                initialPhotoUri = "",
                initialColorHex = "#8B5CF6",
                initialBirthDate = "",
                isNewProfile = true,
                onBack = { navController.popBackStack() },
                onCompleteNew = { name, gender, photoUri, colorHex, birthDate ->
                    val profile = ChildProfile(
                        name = name,
                        gender = gender,
                        photoUri = photoUri,
                        colorHex = colorHex,
                        birthDate = birthDate
                    )
                    viewModel.addProfile(profile)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "edit_profile/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val profiles = viewModel.getProfiles()
            val profile = profiles.find { it.name == name }
            ChildNameInputScreen(
                initialName = profile?.name ?: "",
                initialGender = profile?.gender ?: "BOY",
                initialPhotoUri = profile?.photoUri ?: "",
                initialColorHex = profile?.colorHex ?: "#8B5CF6",
                initialBirthDate = profile?.birthDate ?: "",
                isNewProfile = false,
                onBack = { navController.popBackStack() },
                onProfileDelete = {
                    viewModel.deleteProfile(name)
                    navController.popBackStack()
                },
                onCompleteNew = { updatedName, gender, photoUri, colorHex, birthDate ->
                    val updated = ChildProfile(
                        name = updatedName,
                        gender = gender,
                        photoUri = photoUri,
                        colorHex = colorHex,
                        birthDate = birthDate
                    )
                    viewModel.updateProfile(name, updated)
                    navController.popBackStack()
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
                    // Handled inside MainTabsScreen via bottom sheet
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    var showProfileSwitcher by remember { mutableStateOf(false) }
    
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
                        onNavigateToProfile = { showProfileSwitcher = true }
                    )
                    1 -> ReportScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { showProfileSwitcher = true }
                    )
                    2 -> SearchScreen(
                        viewModel = viewModel,
                        onNavigateToBookDetail = onNavigateToBookDetail,
                        onNavigateToProfile = { showProfileSwitcher = true }
                    )
                    3 -> GoalScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { showProfileSwitcher = true }
                    )
                }
            }
        }
    }

    if (showProfileSwitcher) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val profiles by viewModel.profilesState.collectAsState()
        val currentChildName by viewModel.childNameState.collectAsState()
        val allBooks by viewModel.books.collectAsState()
        val allSessions by viewModel.sessions.collectAsState()
        
        ModalBottomSheet(
            onDismissRequest = { showProfileSwitcher = false },
            sheetState = sheetState,
            containerColor = Color(0xFFFDFCF0),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF78716C).copy(alpha = 0.4f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "누구의 독서기록장을 볼까요?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF44403C),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    profiles.forEach { profile ->
                        val isSelected = profile.name == currentChildName
                        val pColor = try {
                            Color(android.graphics.Color.parseColor(profile.colorHex))
                        } catch (e: Exception) {
                            Color(0xFF8B5CF6)
                        }
                        
                        // Calculate some summary information for this profile
                        val childBooksCount = allBooks.filter { it.childName == profile.name }.size
                        val summaryText = if (childBooksCount > 0) {
                            "등록된 책 ${childBooksCount}권 🌱"
                        } else {
                            "아직 등록된 책이 없어요 🌱"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchProfile(profile)
                                    showProfileSwitcher = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) pColor.copy(alpha = 0.08f) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) pColor else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shadow(elevation = 2.dp, shape = CircleShape)
                                            .background(
                                                if (profile.photoUri.isNotEmpty() && !profile.photoUri.startsWith("emoji:")) Color.Transparent
                                                else when (profile.gender) {
                                                    "BOY" -> Color(0xFFE0F2FE)
                                                    "GIRL" -> Color(0xFFFCE7F3)
                                                    else -> Color(0xFFFFF9C4)
                                                },
                                                CircleShape
                                            )
                                            .border(2.dp, pColor, CircleShape)
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profile.photoUri.isNotEmpty()) {
                                            if (profile.photoUri.startsWith("emoji:")) {
                                                val emoji = profile.photoUri.removePrefix("emoji:")
                                                Text(text = emoji, fontSize = 24.sp)
                                            } else {
                                                AsyncImage(
                                                    model = profile.photoUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = if (profile.gender == "BOY") "👦🏻" else "👧🏻",
                                                fontSize = 24.sp
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF44403C)
                                            )
                                            Text(
                                                text = if (profile.gender == "BOY") "👦🏻" else "👧🏻",
                                                fontSize = 13.sp
                                            )
                                            if (profile.birthDate.isNotEmpty()) {
                                                Text(
                                                    text = profile.birthDate,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF78716C),
                                                    modifier = Modifier
                                                        .background(Color(0xFFF1F0E8), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = summaryText,
                                            fontSize = 12.sp,
                                            color = Color(0xFF78716C)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            showProfileSwitcher = false
                                            navController.navigate("edit_profile/${profile.name}")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "프로필 수정",
                                            tint = Color(0xFF78716C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "선택됨",
                                            tint = pColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        showProfileSwitcher = false
                        navController.navigate("add_profile")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF44403C)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "새 프로필 등록",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorFallbackScreen(
    errorMsg: String,
    onReset: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val backgroundColor = Color(0xFFFDFCF0) // Cream background
    val textColor = Color(0xFF44403C) // Dark brown text
    val errorAccent = Color(0xFFEF4444) // Friendly Red

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp)
            .testTag("error_fallback_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            // Cute animated-looking construction / warning emoji
            Text(
                text = "🚧 🐞 🚧",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "앗! 앱에 문제가 발생했어요",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "아이의 데이터나 프로필 정보 처리 도중 예상치 못한 오류가 발견되었습니다. 앱을 초기화하거나 안전하게 다시 작동시켜보세요.",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Scrollable error details Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Text(
                        text = errorMsg,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Button(
                onClick = onReset,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = errorAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_and_restart_button")
            ) {
                Text(
                    text = "데이터 초기화 및 다시 시작",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(errorMsg))
                    android.widget.Toast.makeText(context, "오류 로그가 클립보드에 복사되었습니다! 📋", android.widget.Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = textColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("copy_error_log_button")
            ) {
                Text(
                    text = "오류 상세 정보 복사하기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    // Just trigger a normal restart
                    val intent = android.content.Intent(context, MainActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier.testTag("just_restart_button")
            ) {
                Text(
                    text = "그냥 다시 시도해보기",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

