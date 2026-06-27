package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Book
import com.example.data.model.ReadingSession
import com.example.data.model.formatDate
import com.example.ui.viewmodel.BookViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookViewModel,
    onNavigateToBookDetail: (Int) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToNewSession: (Int) -> Unit,
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

    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDate by viewModel.selectedDate.collectAsState()

    // Header State
    val yearMonthFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)
    val currentMonthLabel = yearMonthFormat.format(calendar.time)

    // Parse session dates to associate with calendar
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { formatDate(it.startDate) }
    }

    // Get selected day sessions
    val selectedDayStr = remember(selectedDate) {
        val sdf = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
        sdf.format(selectedDate.time)
    }
    val selectedSessions = sessionsByDate[selectedDayStr] ?: emptyList()

    // Calculate current month stats dynamically
    val currentMonthKey = remember(calendar) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
    }
    val stats = remember(books, sessions, calendar) {
        viewModel.getMonthStats(currentMonthKey)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBook,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_book_fab")
                    .padding(bottom = 16.dp),
                shape = FloatingCornerShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "책 등록하기")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // 1. Bento Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "아이의 독서 성장",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$childNameWithJosa 포트폴리오",
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
                                if (childPhotoUri.isNotEmpty() && !childPhotoUri.startsWith("emoji:")) Color.Transparent
                                else when (childGender) {
                                    "BOY" -> Color(0xFFE0F2FE) // Soft boy blue
                                    "GIRL" -> Color(0xFFFCE7F3) // Soft girl pink
                                    else -> Color(0xFFFFF59D) // Default yellow
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
                            if (childPhotoUri.startsWith("emoji:")) {
                                Text(
                                    text = childPhotoUri.removePrefix("emoji:"),
                                    fontSize = 28.sp
                                )
                            } else {
                                AsyncImage(
                                    model = childPhotoUri,
                                    contentDescription = "아이 프로필",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
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

            // 2. Bento Calendar Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month Selector Bar Inside Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newCal = calendar.clone() as Calendar
                                    newCal.add(Calendar.MONTH, -1)
                                    calendar = newCal
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "이전 달",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = currentMonthLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF44403C)
                            )

                            IconButton(
                                onClick = {
                                    val newCal = calendar.clone() as Calendar
                                    newCal.add(Calendar.MONTH, 1)
                                    calendar = newCal
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "다음 달",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Week Days
                        val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDays.forEachIndexed { index, day ->
                                val color = when (index) {
                                    0 -> Color(0xFFE57373) // Red for Sun
                                    6 -> Color(0xFF64B5F6) // Blue for Sat
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid Days
                        val days = remember(calendar) { generateCalendarDays(calendar) }
                        days.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                week.forEach { date ->
                                    if (date == null) {
                                        Box(modifier = Modifier.weight(1f))
                                    } else {
                                        val isSelected = isSameDay(date, selectedDate)
                                        val isToday = isSameDay(date, Calendar.getInstance())
                                        val formattedDateStr = remember(date) {
                                            val sdf = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
                                            sdf.format(date.time)
                                        }
                                        val daySessions = sessionsByDate[formattedDateStr] ?: emptyList()

                                        CalendarDayCell(
                                            date = date,
                                            isSelected = isSelected,
                                            isToday = isToday,
                                            sessions = daySessions,
                                            books = books,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                viewModel.setSelectedDate(date.clone() as Calendar)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Divider below the last row of the calendar
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Detailed Reading Record of Selected Date in a wide card format
                        val headerDateStr = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).format(selectedDate.time)
                        Text(
                            text = "$headerDateStr 독서 기록",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF44403C),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (selectedSessions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🌱 이 날은 아직 독서 기록이 없어요.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "자녀와 함께 책을 읽고 따뜻한 첫 기록을 남겨보세요!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                selectedSessions.forEach { session ->
                                    val associatedBook = books.find { it.id == session.bookId }
                                    if (associatedBook != null) {
                                        ReadingSessionItemCard(
                                            session = session,
                                            book = associatedBook,
                                            onClick = { onNavigateToBookDetail(associatedBook.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bento Stats Grid (Side-by-side stats)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: 이번 달 완독
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF9C4) // Warm light yellow container
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Warm Book background illustration
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 10.dp, y = 10.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "이번 달 완독",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF44403C)
                                )
                                Column {
                                    if (stats.booksReadCount == 0) {
                                        Text(
                                            text = "아이와 함께 채워갈 첫 페이지",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B5CF6),
                                            lineHeight = 16.sp
                                        )
                                    } else {
                                        Text(
                                            text = "${stats.booksReadCount}권",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF8B5CF6),
                                            lineHeight = 36.sp
                                        )
                                        Text(
                                            text = "목표 대비 +${stats.booksReadCount}권",
                                            fontSize = 10.sp,
                                            color = Color(0xFF44403C),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: 연속 독서일
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3EDF7)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "연속 독서일",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D192B)
                            )
                            Column {
                                Text(
                                    text = "${stats.currentStreak}일",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1D192B),
                                    lineHeight = 36.sp
                                )
                                Text(
                                    text = "최고 기록 15일",
                                    fontSize = 10.sp,
                                    color = Color(0xFF49454F),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Currently Reading Bento Block
            item {
                val readingBooks = remember(books) { books.filter { it.status == Book.STATUS_READING } }
                if (readingBooks.isNotEmpty()) {
                    val readingBook = readingBooks.first()
                    val bookSessions = remember(sessions) { sessions.filter { it.bookId == readingBook.id } }
                    val progressPercent = if (bookSessions.isEmpty()) 20 else (bookSessions.size * 20).coerceIn(20, 95)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onNavigateToBookDetail(readingBook.id) },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "현재 읽고 있는 책",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEADDFF))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$progressPercent%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21005D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover image or placeholder
                                Box(
                                    modifier = Modifier
                                        .size(width = 60.dp, height = 90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!readingBook.coverUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = readingBook.coverUrl,
                                            contentDescription = readingBook.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = readingBook.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${readingBook.author} · ${readingBook.category}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Custom Progress Bar
                                    LinearProgressIndicator(
                                        progress = { progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = Color(0xFFEADDFF)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val lastRecordText = if (bookSessions.isNotEmpty()) {
                                        val lastSession = bookSessions.maxByOrNull { it.timestamp }
                                        "어제 ${lastSession?.memo?.take(8) ?: ""}... 기록함"
                                    } else {
                                        "아직 기록된 독서 세션이 없습니다."
                                    }
                                    Text(
                                        text = lastRecordText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }


        }
    }
}

@Composable
fun CalendarDayCell(
    date: Calendar,
    isSelected: Boolean,
    isToday: Boolean,
    sessions: List<ReadingSession>,
    books: List<Book>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)

    val baseTextColor = when (dayOfWeek) {
        Calendar.SUNDAY -> Color(0xFFEF9A9A)
        Calendar.SATURDAY -> Color(0xFF90CAF9)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val cellBackground = when {
        isSelected -> Color(0xFFFFD600).copy(alpha = 0.15f) // Warm yellow accent for selected
        isToday -> Color(0xFFFFD600).copy(alpha = 0.1f) // Soft warm highlight for today
        else -> Color.Transparent
    }

    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, Color(0xFF8B5CF6)) // Warm purple border for selection
        isToday -> BorderStroke(1.5.dp, Color(0xFFFFD600)) // Warm yellow border for today
        else -> BorderStroke(1.5.dp, Color.Transparent) // fallback transparent border of exact same width to avoid shift
    }

    val textColor = when {
        isSelected -> Color(0xFF8B5CF6) // Highlight selected with warm purple
        else -> baseTextColor
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cellBackground
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // flat style avoids shift
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Book cover as background with Opacity 0.3 if reading session exists
            if (sessions.isNotEmpty()) {
                val book = books.find { it.id == sessions.first().bookId }
                if (book != null && !book.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.3f),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                    )
                }
            }

            // Day content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                Text(
                    text = dayOfMonth.toString(),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isToday || sessions.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                if (sessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    // Cute little sprout badge 🌱
                    Text(
                        text = "🌱",
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingSessionItemCard(
    session: ReadingSession,
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(book.category, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFEADDFF),
                            labelColor = Color(0xFF21005D)
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatDate(session.startDate)} ~ ${formatDate(session.endDate)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = book.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!session.title.isNullOrEmpty()) {
                    Text(
                        text = session.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = session.memo,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < session.rating) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (session.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = session.tags.split(",").joinToString(" ") { "#${it.trim()}" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Helper: Generate calendar list including pre-padding empty spots
fun generateCalendarDays(calendar: Calendar): List<Calendar?> {
    val temp = calendar.clone() as Calendar
    temp.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sun = 0)
    val maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

    val list = mutableListOf<Calendar?>()
    for (i in 0 until firstDayOfWeek) {
        list.add(null)
    }
    for (day in 1..maxDay) {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, day)
        list.add(c)
    }
    // Pad the end with nulls to make it a perfect multiple of 7
    while (list.size % 7 != 0) {
        list.add(null)
    }
    return list
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

val FloatingCornerShape = RoundedCornerShape(16.dp)
