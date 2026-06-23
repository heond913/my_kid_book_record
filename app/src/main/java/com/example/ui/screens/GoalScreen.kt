package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
fun GoalScreen(viewModel: BookViewModel) {
    val books by viewModel.books.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val goals by viewModel.goals.collectAsState()

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
                        text = "서준이의 독서 목표",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEADDFF))
                        .border(2.dp, Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SJ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
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

            // --- Create Goal Dialog ---
            if (showCreateGoalDialog) {
                CreateGoalDialog(
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

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onSubmit: (type: String, value: String, count: Int) -> Unit
) {
    var periodType by remember { mutableStateOf("MONTHLY") } // "MONTHLY", "QUARTERLY", "YEARLY"
    var targetCount by remember { mutableStateOf("5") }

    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-indexed

    var selectedYear by remember { mutableStateOf(currentYear.toString()) }
    var selectedMonth by remember { mutableStateOf(String.format("%02d", currentMonth)) }
    var selectedQuarter by remember { mutableStateOf("Q1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val countInt = targetCount.toIntOrNull() ?: 5
                    val periodValue = when (periodType) {
                        "MONTHLY" -> "$selectedYear-$selectedMonth"
                        "QUARTERLY" -> "$selectedYear-$selectedQuarter"
                        else -> selectedYear
                    }
                    onSubmit(periodType, periodValue, countInt)
                },
                modifier = Modifier.testTag("submit_goal_button")
            ) {
                Text("목표 설정")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = { Text("독서 습관 목표 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("목표 주기 구분", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoalTypeChip("월간 목표", periodType == "MONTHLY", modifier = Modifier.weight(1f)) { periodType = "MONTHLY" }
                    GoalTypeChip("분기 목표", periodType == "QUARTERLY", modifier = Modifier.weight(1f)) { periodType = "QUARTERLY" }
                    GoalTypeChip("연간 목표", periodType == "YEARLY", modifier = Modifier.weight(1f)) { periodType = "YEARLY" }
                }

                // Year selector
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = { selectedYear = it },
                    label = { Text("목표 기준 년도 (YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (periodType == "MONTHLY") {
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = { selectedMonth = it },
                        label = { Text("목표 기준 월 (MM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else if (periodType == "QUARTERLY") {
                    Text("목표 분기 선택", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Q1", "Q2", "Q3", "Q4").forEach { q ->
                            FilterChip(
                                selected = selectedQuarter == q,
                                onClick = { selectedQuarter = q },
                                label = { Text(q) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetCount,
                    onValueChange = { targetCount = it },
                    label = { Text("완독 목표 권 수") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_count_input"),
                    singleLine = true
                )
            }
        }
    )
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
