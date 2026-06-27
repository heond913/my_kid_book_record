package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Book
import com.example.data.model.BookPhoto
import com.example.data.model.ReadingSession
import com.example.ui.viewmodel.BookViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookViewModel,
    bookId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Select the book
    LaunchedEffect(bookId) {
        viewModel.selectBook(bookId)
    }

    val book by viewModel.selectedBook.collectAsState()
    val sessions by viewModel.selectedBookSessions.collectAsState()
    val photos by viewModel.selectedBookPhotos.collectAsState()
    val history by viewModel.selectedBookHistory.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAddSessionDialog by remember { mutableStateOf(false) }
    var selectedViewerPhoto by remember { mutableStateOf<BookPhoto?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val activeBook = book!!

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("독서 성장 포트폴리오", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("도서 전체 삭제", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Book Metadata Hero Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Cover image or placeholder
                        Card(
                            modifier = Modifier.size(90.dp, 125.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (!activeBook.coverUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = activeBook.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Metadata detail text
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(activeBook.category, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = null,
                                    modifier = Modifier.height(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (activeBook.isbn.isNotEmpty()) {
                                    Text(
                                        text = "ISBN: ${activeBook.isbn.takeLast(4)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = activeBook.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "저자: ${activeBook.author}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "출판사: ${activeBook.publisher} • ${activeBook.publishDate}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status switcher pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusPill(
                                    label = "읽는중",
                                    isActive = activeBook.status == Book.STATUS_READING,
                                    color = Color(0xFF64B5F6),
                                    onClick = { viewModel.updateBookStatus(activeBook.id, Book.STATUS_READING) }
                                )
                                StatusPill(
                                    label = "완독",
                                    isActive = activeBook.status == Book.STATUS_COMPLETED,
                                    color = Color(0xFF81C784),
                                    onClick = { viewModel.updateBookStatus(activeBook.id, Book.STATUS_COMPLETED) }
                                )
                            }
                        }
                    }
                }

                // 2. Status Transition Timeline (히스토리)
                if (history.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = "⏱️ 독서 상태 변경 이력",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            history.forEachIndexed { index, log ->
                                val statusKorean = when (log.toStatus) {
                                    Book.STATUS_WANT_TO_READ -> "읽고 싶은 책"
                                    Book.STATUS_READING -> "읽는 중"
                                    Book.STATUS_COMPLETED -> "완독"
                                    else -> log.toStatus
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F4)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(end = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = log.changeDate,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = statusKorean,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF44403C)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteHistory(log)
                                                    coroutineScope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "이력이 삭제되었습니다.",
                                                            actionLabel = "실행 취소",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.insertHistory(log)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 6.dp, y = (-4).dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "이력 삭제",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (index < history.size - 1) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Horizontal Photo Gallery
                if (photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "📸 우리아이 독서 갤러리 (${photos.size}개)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(photos) { photo ->
                                Card(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clickable { selectedViewerPhoto = photo },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(photo.rotation.toFloat()),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = photo.purpose,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Cumulative Reading Sessions list
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📖 독서 누적 기록 (${sessions.size}회차)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showAddSessionDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_reading_log_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("기록 쓰기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 작성된 독서 회차 기록이 없어요.\n첫 회차 기록을 남기고 독서 성장을 추적해보세요!",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        sessions.forEachIndexed { index, session ->
                            SessionTimelineItem(
                                session = session,
                                roundNumber = sessions.size - index,
                                onDelete = { viewModel.deleteReadingSession(session) }
                            )
                        }
                    }
                }
            }

            // --- Photo Viewer Dialog Overlay ---
            if (selectedViewerPhoto != null) {
                var photoMemo by remember { mutableStateOf(selectedViewerPhoto!!.memo) }
                var photoRotation by remember { mutableStateOf(selectedViewerPhoto!!.rotation) }

                AlertDialog(
                    onDismissRequest = { selectedViewerPhoto = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updatePhotoMemoAndRotation(
                                    selectedViewerPhoto!!.id,
                                    photoMemo,
                                    photoRotation
                                )
                                selectedViewerPhoto = null
                            }
                        ) {
                            Text("저장", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel.deletePhoto(selectedViewerPhoto!!)
                            selectedViewerPhoto = null
                        }) {
                            Text("삭제", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    title = { Text(selectedViewerPhoto!!.purpose, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Photo preview with interactive rotation
                            Card(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = selectedViewerPhoto!!.uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .rotate(photoRotation.toFloat()),
                                        contentScale = ContentScale.Fit
                                    )
                                    
                                    // Rotate action button overlay
                                    IconButton(
                                        onClick = {
                                            photoRotation = (photoRotation + 90) % 360
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "회전", tint = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = photoMemo,
                                onValueChange = { photoMemo = it },
                                label = { Text("사진 메모 입력 (예: 인상깊은 구절)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }
                    }
                )
            }

            // --- Add Session Dialog Form ---
            if (showAddSessionDialog) {
                AddSessionDialog(
                    bookId = activeBook.id,
                    initialDate = viewModel.getFormattedDate(viewModel.selectedDate.value),
                    onDismiss = { showAddSessionDialog = false },
                    onSubmit = { startDate, endDate, title, memo, rating, tags, photosList ->
                        viewModel.addReadingSession(
                            bookId = activeBook.id,
                            startDate = startDate,
                            endDate = endDate,
                            title = title,
                            memo = memo,
                            rating = rating,
                            tags = tags,
                            photos = photosList,
                            onSuccess = {
                                showAddSessionDialog = false
                            }
                        )
                    }
                )
            }

            // --- Delete Confirm Dialog ---
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("도서 삭제", fontWeight = FontWeight.Bold) },
                    text = { Text("이 도서와 관련된 모든 독서 회차 기록 및 사진이 영구적으로 삭제됩니다. 정말 삭제하시겠습니까?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteBook(activeBook)
                                showDeleteConfirmDialog = false
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("삭제")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("취소")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) color else color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isActive) Color.White else color
        )
    }
}

@Composable
fun SessionTimelineItem(
    session: ReadingSession,
    roundNumber: Int,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = roundNumber.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${session.startDate} ~ ${session.endDate}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "기록 삭제",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!session.title.isNullOrEmpty()) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = session.memo,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars
                Row {
                    repeat(5) { idx ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (idx < session.rating) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Tags
                if (session.tags.isNotEmpty()) {
                    Text(
                        text = session.tags.split(",").joinToString(" ") { "#${it.trim()}" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionDialog(
    bookId: Int,
    initialDate: String,
    onDismiss: () -> Unit,
    onSubmit: (
        startDate: String,
        endDate: String,
        title: String?,
        memo: String,
        rating: Int,
        tags: String,
        photos: List<Pair<String, String>> // List of (Uri, Purpose)
    ) -> Unit
) {
    val context = LocalContext.current
    
    var startDate by remember { mutableStateOf(initialDate) }
    var endDate by remember { mutableStateOf(initialDate) }
    
    var title by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    var tags by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    fun showDatePicker(isStartDate: Boolean) {
        val currentDateStr = if (isStartDate) startDate else endDate
        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        var day = calendar.get(Calendar.DAY_OF_MONTH)
        
        try {
            val parts = currentDateStr.split("/")
            if (parts.size == 3) {
                val yy = parts[0].toInt()
                val fullYear = if (yy < 50) 2000 + yy else 1900 + yy
                val mm = parts[1].toInt() - 1
                val dd = parts[2].toInt()
                year = fullYear
                month = mm
                day = dd
            }
        } catch (e: Exception) {
            // fallback to current date if parsing fails
        }

        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val yyShort = selectedYear % 100
                val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%02d", yyShort, selectedMonth + 1, selectedDay)
                if (isStartDate) {
                    startDate = formattedDate
                } else {
                    endDate = formattedDate
                }
            },
            year,
            month,
            day
        ).show()
    }

    // List of added photos (UriString to Purpose)
    val addedPhotos = remember { mutableStateListOf<Pair<String, String>>() }

    // Image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy URI stream to local cache directory to avoid permissions limits later
            val localPath = copyUriToLocalCache(context, it)
            if (localPath != null) {
                addedPhotos.add(localPath to "책 표지 인증")
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val localPath = saveBitmapToLocalCache(context, bitmap)
                if (localPath != null) {
                    addedPhotos.add(localPath to "중요 내용 기록")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (memo.isNotBlank()) {
                        onSubmit(
                            startDate,
                            endDate,
                            title.ifBlank { null },
                            memo,
                            rating,
                            tags,
                            addedPhotos.toList()
                        )
                    }
                },
                enabled = memo.isNotBlank(),
                modifier = Modifier.testTag("submit_reading_log_button")
            ) {
                Text("기록 저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = { Text("새 독서기록 작성", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date picker inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker(isStartDate = true) }
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("시작일") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "시작일 선택"
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker(isStartDate = true) }
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker(isStartDate = false) }
                    ) {
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("종료일") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "종료일 선택"
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker(isStartDate = false) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("기록 제목 (선택, 예: 1회차 완료)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("느낀 점 및 독서 메모 (필수)") },
                    modifier = Modifier.fillMaxWidth().testTag("log_memo_input"),
                    minLines = 3
                )

                // Stars rating selector
                Column {
                    Text("독서 평점", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        repeat(5) { idx ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (idx < rating) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { rating = idx + 1 }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("태그 입력 (쉼표로 구분, 예: 재밌음, 감동)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Photos add section
                Column {
                    Text("사진 첨부 (무제한)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("갤러리", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                cameraLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("카메라 촬영", fontSize = 12.sp)
                        }
                    }

                    if (addedPhotos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            addedPhotos.forEachIndexed { idx, (uri, purpose) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Card(modifier = Modifier.size(50.dp)) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        
                                        // Dropdown/Selector for Photo Purpose
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("사진 용도 선택:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                BookPhoto.PURPOSES.forEach { purp ->
                                                    val active = purpose == purp
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (active) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f))
                                                            .clickable {
                                                                addedPhotos[idx] = uri to purp
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(text = purp.take(5), fontSize = 9.sp, color = if (active) Color.White else Color.Black)
                                                    }
                                                }
                                            }
                                        }

                                        IconButton(onClick = { addedPhotos.removeAt(idx) }) {
                                            Icon(Icons.Default.Close, contentDescription = "삭제")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

// Helpers to store images in local app cache directory safely (avoids Content Uri permission expiration!)
fun copyUriToLocalCache(context: Context, uri: Uri): String? {
    var originalBitmap: Bitmap? = null
    var rotatedBitmap: Bitmap? = null
    var outputStream: FileOutputStream? = null
    return try {
        val resolver = context.contentResolver
        
        // 1. Get EXIF Orientation flag from stream using androidx.exifinterface
        val orientation = resolver.openInputStream(uri)?.use { stream ->
            val exifInterface = androidx.exifinterface.media.ExifInterface(stream)
            exifInterface.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL

        // Map the orientation flag to rotation degrees
        val degrees = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        // 2. Decode original bitmap from stream
        originalBitmap = resolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)
        } ?: return null

        // 3. Physically rotate if degrees != 0
        rotatedBitmap = if (degrees != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
        } else {
            originalBitmap
        }

        // 4. Save normalized bitmap to local cache as JPEG
        val file = File(context.cacheDir, "kids_book_img_${UUID.randomUUID()}.jpg")
        outputStream = FileOutputStream(file)
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.flush()
        
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        // 5. Safe cleanup and memory recycle
        try {
            outputStream?.close()
        } catch (ignored: Exception) {}
        
        if (originalBitmap != null && originalBitmap != rotatedBitmap) {
            originalBitmap.recycle()
        }
        rotatedBitmap?.recycle()
    }
}

fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.cacheDir, "kids_book_img_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.flush()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
