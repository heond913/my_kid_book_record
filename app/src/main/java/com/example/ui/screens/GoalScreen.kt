package com.example.ui.screens

import coil.compose.AsyncImage
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.ReadingGoal
import com.example.ui.viewmodel.BookViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    viewModel: BookViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val books by viewModel.books.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val goals by viewModel.goals.collectAsState()

    val childName by viewModel.childNameState.collectAsState()
    val childGender by viewModel.childGenderState.collectAsState()
    val childPhotoUri by viewModel.childPhotoUriState.collectAsState()

    val childNameWithJosa = remember(childName) {
        val name = childName.ifEmpty { "서준" }
        if (name == "서준") "서준이의"
        else if (name.lastOrNull()?.let { (it.code - 0xAC00) % 28 > 0 && it.code in 0xAC00..0xD7A3 } == true) {
            "${name}이의"
        } else {
            "${name}의"
        }
    }

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var triggerCelebration by remember { mutableStateOf(false) }
    var celebrationGoalName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGoalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_goal_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "목표 추가")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Bento Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "꿈을 키우는 독서 습관",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$childNameWithJosa 독서 목표",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .background(
                            if (childPhotoUri.isNotEmpty()) Color.Transparent
                            else when (childGender) {
                                "BOY" -> Color(0xFFE0F2FE)
                                "GIRL" -> Color(0xFFFCE7F3)
                                else -> Color(0xFFFFF59D)
                            },
                            CircleShape
                        )
                        .border(
                            2.dp,
                            when (childGender) {
                                "BOY" -> Color(0xFF0284C7).copy(alpha = 0.5f)
                                "GIRL" -> Color(0xFFDB2777).copy(alpha = 0.5f)
                                else -> Color(0xFF8B5CF6).copy(alpha = 0.5f)
                            },
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    if (childPhotoUri.isNotEmpty()) {
                        AsyncImage(
                            model = childPhotoUri,
                            contentDescription = "아이 프로필",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChildCare,
                            contentDescription = "아이 프로필",
                            tint = when (childGender) {
                                "BOY" -> Color(0xFF0284C7)
                                "GIRL" -> Color(0xFFDB2777)
                                else -> Color(0xFF8B5CF6)
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "설정된 독서 목표가 없습니다.\n우측 하단의 [+] 버튼을 눌러\n아이의 첫 독서 목표를 설정해 보세요!",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "목표 달성 시 성취감을 고취하는 축하 카드가 나타납니다!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    items(goals) { goal ->
                        // Calculate completed books matching this goal's period
                        val completedCount = remember(books, sessions, goal) {
                            calculateCompletedCountForPeriod(books, sessions, goal)
                        }
                        
                        val progressRatio = if (goal.targetCount > 0) {
                            completedCount.toFloat() / goal.targetCount
                        } else {
                            0f
                        }
                        
                        val progressPercent = (progressRatio * 100).toInt().coerceAtMost(100)
                        val isAchieved = completedCount >= goal.targetCount

                        // If achieved just now, trigger once
                        LaunchedEffect(isAchieved) {
                            if (isAchieved && goal.targetCount > 0) {
                                celebrationGoalName = getPeriodKoreanName(goal)
                                triggerCelebration = true
                            }
                        }

                        GoalProgressCard(
                            goal = goal,
                            completedCount = completedCount,
                            progressPercent = progressPercent,
                            isAchieved = isAchieved,
                            onDelete = { viewModel.deleteGoal(goal) },
                            onCelebrate = {
                                celebrationGoalName = getPeriodKoreanName(goal)
                                triggerCelebration = true
                            }
                        )
                    }
                }
            }
            }

            // --- Confetti Canvas Overlay ---
            if (triggerCelebration) {
                ConfettiAnimation(
                    onFinished = { triggerCelebration = false },
                    goalName = celebrationGoalName
                )
            }

            // --- Create Goal Bottom Sheet ---
            if (showCreateGoalDialog) {
                CreateGoalBottomSheet(
                    onDismiss = { showCreateGoalDialog = false },
                    onSubmit = { type, value, count ->
                        viewModel.setReadingGoal(type, value, count)
                        showCreateGoalDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun GoalProgressCard(
    goal: ReadingGoal,
    completedCount: Int,
    progressPercent: Int,
    isAchieved: Boolean,
    onDelete: () -> Unit,
    onCelebrate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getPeriodKoreanName(goal),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "목표 기간: ${goal.periodValue}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAchieved) {
                        IconButton(
                            onClick = onCelebrate,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFD54F).copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = "축하하기",
                                tint = Color(0xFFFF8F00)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress status text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "완독 달성: ${completedCount}권 / ${goal.targetCount}권",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$progressPercent%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { completedCount.toFloat() / goal.targetCount.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (isAchieved) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )

            if (isAchieved) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🏆 대단해요! 자녀와 정해둔 독서 목표를 멋지게 완수하였습니다!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (type: String, value: String, count: Int) -> Unit
) {
    var periodType by remember { mutableStateOf("MONTHLY") } // "MONTHLY", "QUARTERLY", "YEARLY"
    var targetCountInt by remember { mutableStateOf(5) }

    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-indexed

    var selectedYear by remember { mutableStateOf(currentYear.toString()) }
    var selectedMonth by remember { mutableStateOf(String.format("%02d", currentMonth)) }
    var selectedQuarter by remember { mutableStateOf("Q1") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFDFCF0), // Match cream background perfectly!
        contentColor = Color(0xFF44403C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "독서 습관 목표 추가",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFF44403C),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic interactive book stack illustration (visual "책이 쌓이는 시각적 효과")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier.height(110.dp).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val displayCount = targetCountInt.coerceIn(1, 10)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-4).dp)
                    ) {
                        for (i in 0 until displayCount) {
                            val bookColors = listOf(
                                Color(0xFF8B5CF6), // Warm purple
                                Color(0xFFFFD600), // Yellow
                                Color(0xFF4CAF50), // Green
                                Color(0xFFEF5350), // Red
                                Color(0xFF29B6F6)  // Blue
                            )
                            val bookColor = bookColors[i % bookColors.size]
                            
                            Card(
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = bookColor),
                                border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .width((60 + (i * 3.5)).dp)
                                    .height(10.dp)
                            ) {}
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "책 ${targetCountInt}권 쌓기 습관 기르기 📚",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6)
                )
            }

            HorizontalDivider(color = Color(0xFF44403C).copy(alpha = 0.1f))

            // 1. Goal Period Segmented Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "목표 주기 구분",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44403C)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoalTypeChip("월간 목표", periodType == "MONTHLY", modifier = Modifier.weight(1f)) { periodType = "MONTHLY" }
                    GoalTypeChip("분기 목표", periodType == "QUARTERLY", modifier = Modifier.weight(1f)) { periodType = "QUARTERLY" }
                    GoalTypeChip("연간 목표", periodType == "YEARLY", modifier = Modifier.weight(1f)) { periodType = "YEARLY" }
                }
            }

            // 2. Year Selector (Horizontal scrollable chips)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "목표 기준 년도 선택",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44403C)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val years = listOf((currentYear - 1).toString(), currentYear.toString(), (currentYear + 1).toString())
                    years.forEach { y ->
                        FilterChip(
                            selected = selectedYear == y,
                            onClick = { selectedYear = y },
                            label = { Text(text = "${y}년") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. Month or Quarter selection (Horizontal scrollable chips)
            if (periodType == "MONTHLY") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "목표 기준 월 선택",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44403C)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..12).forEach { m ->
                            val mStr = String.format("%02d", m)
                            FilterChip(
                                selected = selectedMonth == mStr,
                                onClick = { selectedMonth = mStr },
                                label = { Text(text = "${m}월") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            } else if (periodType == "QUARTERLY") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "목표 분기 선택",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44403C)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Q1", "Q2", "Q3", "Q4").forEach { q ->
                            FilterChip(
                                selected = selectedQuarter == q,
                                onClick = { selectedQuarter = q },
                                label = { Text(text = q) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 4. Stepper for Book Count
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "완독 목표 권 수",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44403C),
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (targetCountInt > 1) targetCountInt-- },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFFF9C4), CircleShape) // Light yellow round stepper
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "줄이기",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "${targetCountInt}권",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF44403C),
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .testTag("goal_count_input"),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { if (targetCountInt < 99) targetCountInt++ },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFFF9C4), CircleShape)
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "늘리기",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val periodValue = when (periodType) {
                        "MONTHLY" -> "$selectedYear-$selectedMonth"
                        "QUARTERLY" -> "$selectedYear-$selectedQuarter"
                        else -> selectedYear
                    }
                    onSubmit(periodType, periodValue, targetCountInt)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_goal_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6), // Warm purple
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "목표 설정 완료",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun GoalTypeChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF8B5CF6) else Color(0xFFFFF9C4)) // Purple selection vs Warm light yellow background
            .border(1.dp, if (isSelected) Color(0xFF8B5CF6) else Color(0xFF8B5CF6).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF44403C)
        )
    }
}

// Custom Confetti Particle System Canvas Drawing Celebration overlay
@Composable
fun ConfettiAnimation(onFinished: () -> Unit, goalName: String) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Confetti particles local states
    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                speed = Random.nextFloat() * 10f + 5f,
                color = Color(
                    Random.nextInt(120, 255),
                    Random.nextInt(120, 255),
                    Random.nextInt(120, 255)
                ),
                size = Random.nextFloat() * 12f + 8f,
                angle = Random.nextFloat() * 360f
            )
        }
    }

    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        // Run animation steps
        for (i in 1..100) {
            step = i
            delay(25)
        }
        isVisible = false
        onFinished()
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { isVisible = false; onFinished() },
            contentAlignment = Alignment.Center
        ) {
            // Draw particles on Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    val calculatedY = (p.y + (p.speed * step / 100f)) * size.height
                    val calculatedX = (p.x + (Math.sin(step.toDouble() / 10) * 0.05f).toFloat()) * size.width
                    
                    drawRect(
                        color = p.color,
                        topLeft = Offset(calculatedX, calculatedY),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size)
                    )
                }
            }

            // Foreground Achievement Announcement Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD54F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFFF8F00),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "목표 달성을 축하합니다!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "[$goalName] 독서 계획 목표를 완벽하게 성공적으로 달성했습니다! " +
                                "지속적으로 기록을 쌓아 아이에게 큰 독서 성장 포트폴리오 선물을 선사해주세요. 🎉",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(onClick = { isVisible = false; onFinished() }) {
                        Text("우와! 정말 기뻐요")
                    }
                }
            }
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val angle: Float
)

// Helper calculations
fun calculateCompletedCountForPeriod(books: List<Book>, sessions: List<com.example.data.model.ReadingSession>, goal: ReadingGoal): Int {
    val periodValue = goal.periodValue
    return when (goal.periodType) {
        "MONTHLY" -> {
            // Completed books that have matching end dates or addition dates in the goal YYYY-MM period
            books.filter { book ->
                book.status == Book.STATUS_COMPLETED && (
                    sessions.any { it.bookId == book.id && (it.startDate.contains(periodValue.substring(2)) || it.startDate.contains(periodValue)) } ||
                    formatLongTimestamp(book.addedTimestamp, "yyyy-MM") == periodValue
                )
            }.size
        }
        "QUARTERLY" -> {
            // Quarter definitions: Q1 (01-03), Q2 (04-06), Q3 (07-09), Q4 (10-12)
            val year = periodValue.substringBefore("-")
            val quarter = periodValue.substringAfter("-")
            val months = when (quarter) {
                "Q1" -> listOf("01", "02", "03")
                "Q2" -> listOf("04", "05", "06")
                "Q3" -> listOf("07", "08", "09")
                else -> listOf("10", "11", "12")
            }.map { "$year-$it" }

            books.filter { book ->
                book.status == Book.STATUS_COMPLETED && (
                    sessions.any { sess ->
                        sess.bookId == book.id && months.any { sess.startDate.contains(it.substring(2)) || sess.startDate.contains(it) }
                    } ||
                    months.contains(formatLongTimestamp(book.addedTimestamp, "yyyy-MM"))
                )
            }.size
        }
        else -> {
            // YEARLY
            val year = periodValue
            books.filter { book ->
                book.status == Book.STATUS_COMPLETED && (
                    sessions.any { it.bookId == book.id && (it.startDate.contains(year.substring(2)) || it.startDate.contains(year)) } ||
                    formatLongTimestamp(book.addedTimestamp, "yyyy") == year
                )
            }.size
        }
    }
}

fun getPeriodKoreanName(goal: ReadingGoal): String {
    val value = goal.periodValue
    return when (goal.periodType) {
        "MONTHLY" -> {
            val parts = value.split("-")
            "${parts[0]}년 ${parts[1].toInt()}월 목표"
        }
        "QUARTERLY" -> {
            val parts = value.split("-")
            "${parts[0]}년 ${parts[1]} 분기 목표"
        }
        else -> "${value}년 연간 목표"
    }
}

fun formatLongTimestamp(timestamp: Long, pattern: String): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}
