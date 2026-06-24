package com.example.ui.screens

import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.ui.viewmodel.BookViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: BookViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val books by viewModel.books.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

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

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val currentMonthName = remember {
        SimpleDateFormat("M월", Locale.getDefault()).format(Date())
    }

    val stats = remember(books, sessions) { viewModel.getMonthStats(currentMonthKey) }
    val categoryPercentages = remember(books, sessions) { viewModel.getCategoryPercentages(currentMonthKey) }
    val trendData = remember(books, sessions) { viewModel.getMonthlyTrendData() }

    // Find top category
    val topCategory = remember(categoryPercentages) {
        categoryPercentages.entries.maxByOrNull { it.value }?.let {
            if (it.value > 0f) it.key else "없음"
        } ?: "없음"
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Bento Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "성장 보고서 및 분석",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$childNameWithJosa 리포트",
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
            }

            // 2. Bento Stats Dashboard Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "$currentMonthName 독서 대시보드",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DashboardStatItem(
                                icon = Icons.Default.BookmarkAdded,
                                label = "완독 도서",
                                value = "${stats.booksReadCount}권",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatItem(
                                icon = Icons.Default.CalendarMonth,
                                label = "독서일수",
                                value = "${stats.readingDaysCount}일",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatItem(
                                icon = Icons.Default.LocalFireDepartment,
                                label = "연속 독서",
                                value = "${stats.currentStreak}일째",
                                tint = Color(0xFFFF7043),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Category Share Section (Bento Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "선호하는 분야 (카테고리 분석)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val sortedCats = categoryPercentages.entries.sortedByDescending { it.value }
                        val colors = listOf(
                            Color(0xFFFF8A80), // Pastel Red
                            Color(0xFFFFD54F), // Pastel Orange/Yellow
                            Color(0xFF81C784), // Pastel Green
                            Color(0xFF4FC3F7), // Pastel Blue
                            Color(0xFFBA68C8)  // Pastel Purple
                        )

                        // Draw a custom segmented status bar representation of shares
                        val totalShares = sortedCats.sumOf { it.value.toDouble() }.toFloat()
                        if (totalShares > 0f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            ) {
                                sortedCats.forEachIndexed { idx, entry ->
                                    if (entry.value > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(entry.value.coerceAtLeast(0.01f))
                                                .background(colors[idx % colors.size])
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Category rows with values
                        sortedCats.forEachIndexed { index, entry ->
                            val percentText = String.format("%.0f%%", entry.value * 100)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors[index % colors.size])
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.key,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = percentText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Monthly Trends Chart (Bento Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "최근 독서량 추이 (6개월)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Rendered Bar Chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            trendData.forEach { trendItem ->
                                val maxVal = (trendData.maxOfOrNull { maxOf(it.readingCount, it.completedCount) } ?: 5).coerceAtLeast(1)

                                val readingBarHeightRatio = trendItem.readingCount.toFloat() / maxVal
                                val completedBarHeightRatio = trendItem.completedCount.toFloat() / maxVal

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Sessions Bar
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .fillMaxHeight(readingBarHeightRatio.coerceIn(0.02f, 1f))
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(Color(0xFFEADDFF))
                                        )
                                        // Completed Bar
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .fillMaxHeight(completedBarHeightRatio.coerceIn(0.02f, 1f))
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = trendItem.monthLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFEADDFF))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("독서 활동(기록수)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("완독 수", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 5. Growth Portfolio Bento Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8DEF8)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "💡 우리아이 성장 포트폴리오 분석",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF21005D)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (stats.booksReadCount == 0) {
                                "아직 완독 기록이 없습니다. 완독 후 성장 리포트에서 아이의 독서 패턴 분석을 확인해보세요!"
                            } else {
                                "이번 달 아이는 총 ${stats.readingDaysCount}일간 독서하며 책의 세계를 탐험했습니다. " +
                                "특히 [$topCategory] 분야를 가장 많이 접하여 균형 있는 인지 발달이 이루어지고 있습니다. " +
                                "지속적인 독서 습관 유지를 위해 독서 목표를 함께 세워 성취감을 느끼게 해주세요!"
                            },
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF1D192B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3EDF7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
