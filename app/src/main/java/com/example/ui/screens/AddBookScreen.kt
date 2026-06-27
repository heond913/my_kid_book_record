package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.BookSearchResult
import com.example.data.model.Book
import com.example.ui.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit,
    onSuccess: (Int) -> Unit
) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Search Online, 1 = Manual Input
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf("ALL") } // "ALL" = Standard, "AI" = Gemini

    // Manual Form States
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var publishDate by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("동화") }
    var coverUrl by remember { mutableStateOf("") }
    var initialStatus by remember { mutableStateOf(Book.STATUS_READING) }

    // Dropdown state for category selection
    var categoryExpanded by remember { mutableStateOf(false) }

    // Simulated scanner overlay dialog state
    var showScannerDialog by remember { mutableStateOf(false) }
    
    // Cover Picker Dialog state
    var showCoverPickerDialog by remember { mutableStateOf(false) }

    // Unified Premium Colors
    val creamBgColor = Color(0xFFFDFCF0)
    val darkBrownColor = Color(0xFF44403C)
    val brandPurpleColor = Color(0xFF8B5CF6)
    val lightBorderColor = Color(0xFFE5E7EB)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = darkBrownColor,
        unfocusedTextColor = darkBrownColor,
        focusedBorderColor = brandPurpleColor,
        unfocusedBorderColor = lightBorderColor,
        focusedLabelColor = brandPurpleColor,
        unfocusedLabelColor = darkBrownColor.copy(alpha = 0.6f),
        focusedLeadingIconColor = brandPurpleColor,
        unfocusedLeadingIconColor = darkBrownColor.copy(alpha = 0.6f),
        focusedTrailingIconColor = brandPurpleColor,
        unfocusedTrailingIconColor = darkBrownColor.copy(alpha = 0.6f),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    fun showPublishDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, _ ->
                val formattedMonth = String.format("%02d", month + 1)
                publishDate = "$year-$formattedMonth"
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    var readingDate by remember {
        val sdf = java.text.SimpleDateFormat("yy/MM/dd", java.util.Locale.getDefault())
        mutableStateOf(sdf.format(java.util.Date()))
    }

    fun showReadingDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        try {
            val sdf = java.text.SimpleDateFormat("yy/MM/dd", java.util.Locale.getDefault())
            val date = sdf.parse(readingDate)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                }
                val sdf = java.text.SimpleDateFormat("yy/MM/dd", java.util.Locale.getDefault())
                readingDate = sdf.format(cal.time)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        containerColor = creamBgColor,
        topBar = {
            TopAppBar(
                title = { Text("새 책 등록하기", fontWeight = FontWeight.Bold, color = darkBrownColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "뒤로가기",
                            tint = darkBrownColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = creamBgColor
                )
            )
        },
        bottomBar = {
            if (activeTab == 1) {
                Surface(
                    color = creamBgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && author.isNotBlank()) {
                                viewModel.insertBook(
                                    title = title,
                                    author = author,
                                    publisher = publisher,
                                    publishDate = publishDate,
                                    isbn = isbn,
                                    category = category,
                                    coverUrl = coverUrl.ifBlank { null },
                                    status = initialStatus,
                                    readingDateStr = readingDate,
                                    onSuccess = onSuccess
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_book_button"),
                        enabled = title.isNotBlank() && author.isNotBlank(),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandPurpleColor,
                            disabledContainerColor = brandPurpleColor.copy(alpha = 0.4f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("등록 완료", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(creamBgColor)
        ) {
            // Tab row to switch registration style
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = creamBgColor,
                contentColor = darkBrownColor,
                indicator = { tabPositions ->
                    if (activeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = darkBrownColor
                        )
                    }
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { 
                        activeTab = 0 
                        searchQuery = viewModel.lastQuery
                        if (searchQuery.isNotBlank()) {
                            viewModel.restoreSearchResults(searchQuery, searchMode)
                        }
                    },
                    text = { 
                        Text(
                            text = "도서 검색 등록", 
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) darkBrownColor else darkBrownColor.copy(alpha = 0.6f)
                        ) 
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { 
                        activeTab = 1
                    },
                    text = { 
                        Text(
                            text = "직접 수동 입력", 
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) darkBrownColor else darkBrownColor.copy(alpha = 0.6f)
                        ) 
                    }
                )
            }

            if (activeTab == 0) {
                // --- ONLINE SEARCH REGISTRATION ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_book_input"),
                        placeholder = { Text("책 제목, 저자, ISBN 검색...", color = darkBrownColor.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = darkBrownColor) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = darkBrownColor)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                searchMode = "ALL"
                                viewModel.performExternalSearch(searchQuery, "ALL")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = darkBrownColor, contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("일반 검색", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                searchMode = "AI"
                                viewModel.performExternalSearch(searchQuery, "AI")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandPurpleColor, contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🤖 AI 검색", fontWeight = FontWeight.Bold)
                        }

                        // Scanner simulation button
                        IconButton(
                            onClick = { showScannerDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = brandPurpleColor.copy(alpha = 0.15f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "바코드 스캔", tint = brandPurpleColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = brandPurpleColor)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchMode == "AI") "🤖 AI가 똑똑하게 어린이 도서 데이터를 분석하는 중..." else "온라인 도서 정보 검색 중...",
                                    fontSize = 13.sp,
                                    color = darkBrownColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = darkBrownColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "원하는 책을 검색해보세요!\n어린이 베스트셀러 도서는 [🤖 AI 검색]을 추천합니다.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = darkBrownColor.copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "도서 검색 결과 (${searchResults.size}건)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBrownColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(searchResults) { result ->
                                SearchResultCard(
                                    result = result,
                                    onClick = {
                                        // Auto fill manual form fields
                                        title = result.title
                                        author = result.author
                                        publisher = result.publisher
                                        publishDate = result.publishDate
                                        isbn = result.isbn
                                        category = result.category
                                        coverUrl = result.coverUrl ?: ""
                                        // Switch to editor verification tab
                                        activeTab = 1
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // --- MANUAL INPUT & VERIFICATION FORM ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // [Book Cover Image Picker]
                    Text(
                        text = "도서 커버 이미지",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = darkBrownColor
                    )

                    Card(
                        modifier = Modifier
                            .width(120.dp)
                            .height(160.dp)
                            .align(Alignment.CenterHorizontally)
                            .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp))
                            .clickable { showCoverPickerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, lightBorderColor)
                    ) {
                        if (coverUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (coverUrl.startsWith("emoji:")) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(brandPurpleColor.copy(alpha = 0.08f))
                                    ) {
                                        Text(
                                            text = coverUrl.removePrefix("emoji:"),
                                            fontSize = 48.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "커버 변경하기",
                                            fontSize = 11.sp,
                                            color = brandPurpleColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = "책 표지",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "카메라",
                                    tint = darkBrownColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "표지 사진 추가\n(선택)",
                                    fontSize = 12.sp,
                                    color = darkBrownColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // [도서 기본 정보 Form Group]
                    Text(
                        text = "도서 기본 정보",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = darkBrownColor
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("책 제목 (필수)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_title_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("저자 (필수)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_author_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("출판사") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    // Publication date & ISBN split 50:50 Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Publish Date field (Interactive picker)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = publishDate,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("출간일 (선택)") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "달력 선택",
                                        tint = darkBrownColor
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showPublishDatePicker() }
                            )
                        }

                        // ISBN field (with scanner shortcut)
                        OutlinedTextField(
                            value = isbn,
                            onValueChange = { isbn = it },
                            label = { Text("ISBN 번호") },
                            trailingIcon = {
                                IconButton(onClick = { showScannerDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "바코드 스캔",
                                        tint = darkBrownColor
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }

                    // Category Selector Dropdown using ExposedDropdownMenuBox
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("도서 카테고리") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.background(creamBgColor)
                            ) {
                                Book.CATEGORIES.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = darkBrownColor, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // [독서 초기 상태 설정 Form Group]
                    Text(
                        text = "독서 상태 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = darkBrownColor
                    )

                    // SingleSelect Segmented Toggle Button Group
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, darkBrownColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Transparent)
                    ) {
                        val options = listOf(
                            Pair(Book.STATUS_READING, "읽는 중"),
                            Pair(Book.STATUS_COMPLETED, "완독")
                        )
                        options.forEachIndexed { index, (status, label) ->
                            val isSelected = initialStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) brandPurpleColor else Color.Transparent
                                    )
                                    .clickable { initialStatus = status }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else darkBrownColor
                                )
                            }
                            if (index < options.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(darkBrownColor.copy(alpha = 0.15f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // [읽은 날짜 설정 Form Group]
                    Text(
                        text = "읽은 날짜",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = darkBrownColor
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = readingDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("독서 등록일 (과거 날짜 선택 가능)") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "달력 선택",
                                    tint = darkBrownColor
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showReadingDatePicker() }
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp)) // Padding to scroll comfortably past the fixed CTA button
                }
            }

            // --- COVER PICKER DIALOG ---
            if (showCoverPickerDialog) {
                CoverPickerDialog(
                    onDismiss = { showCoverPickerDialog = false },
                    onCoverSelected = { selectedCover ->
                        coverUrl = selectedCover
                    }
                )
            }

            // --- SIMULATED BARCODE SCANNER DIALOG ---
            if (showScannerDialog) {
                SimulatedScannerDialog(
                    onDismiss = { showScannerDialog = false },
                    onScanSuccess = { scannedIsbn, _ ->
                        showScannerDialog = false
                        isbn = scannedIsbn
                        if (activeTab == 0) {
                            searchQuery = scannedIsbn
                            viewModel.performExternalSearch(scannedIsbn, "ALL")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CoverPickerDialog(
    onDismiss: () -> Unit,
    onCoverSelected: (String) -> Unit
) {
    var customUrl by remember { mutableStateOf("") }
    var customEmoji by remember { mutableStateOf("") }

    val presetCovers = listOf(
        Pair("🧸", "토이랜드"),
        Pair("🧚", "요정나라"),
        Pair("🚀", "우주모험"),
        Pair("🦕", "공룡디노"),
        Pair("🎨", "미술교실"),
        Pair("🐱", "동물친구")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (customEmoji.isNotBlank()) {
                        onCoverSelected("emoji:${customEmoji.trim()}")
                    } else if (customUrl.isNotBlank()) {
                        onCoverSelected(customUrl.trim())
                    }
                    onDismiss()
                }
            ) {
                Text("확인", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF44403C))
            }
        },
        title = { Text("표지 사진 설정", fontWeight = FontWeight.Bold, color = Color(0xFF44403C)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "원하는 일러스트 이모지를 선택하거나 직접 사진 주소(URL)를 입력하세요.",
                    fontSize = 13.sp,
                    color = Color(0xFF44403C).copy(alpha = 0.7f)
                )

                Text(
                    text = "귀여운 이모지 프리셋:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44403C)
                )

                // Grid of cute emojis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetCovers.take(3).forEach { (emoji, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                                .clickable {
                                    onCoverSelected("emoji:$emoji")
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(emoji, fontSize = 24.sp)
                                Text(label, fontSize = 10.sp, color = Color(0xFF44403C).copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetCovers.takeLast(3).forEach { (emoji, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                                .clickable {
                                    onCoverSelected("emoji:$emoji")
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(emoji, fontSize = 24.sp)
                                Text(label, fontSize = 10.sp, color = Color(0xFF44403C).copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFE5E7EB))

                // Custom Emoji Input
                OutlinedTextField(
                    value = customEmoji,
                    onValueChange = { customEmoji = it.take(2) }, // Keep it short for emoji
                    label = { Text("나만의 이모지 직접 입력") },
                    placeholder = { Text("예: 📚, 🦕, 🦄") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                // Custom URL Input
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("인터넷 이미지 URL 주소") },
                    placeholder = { Text("https://example.com/image.jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )
            }
        },
        containerColor = Color(0xFFFDFCF0)
    )
}

@Composable
fun SearchResultCard(
    result: BookSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            Card(
                modifier = Modifier.size(55.dp, 75.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                if (!result.coverUrl.isNullOrEmpty()) {
                    if (result.coverUrl.startsWith("emoji:")) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = result.coverUrl.removePrefix("emoji:"), fontSize = 24.sp)
                        }
                    } else {
                        AsyncImage(
                            model = result.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = result.title.take(1),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF44403C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${result.author} • ${result.publisher}",
                    fontSize = 12.sp,
                    color = Color(0xFF44403C).copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.description.isNotEmpty()) {
                    Text(
                        text = result.description,
                        fontSize = 11.sp,
                        color = Color(0xFF44403C).copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(result.category, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(22.dp),
                        border = null,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                            labelColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "선택하기",
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedScannerDialog(
    onDismiss: () -> Unit,
    onScanSuccess: (String, String) -> Unit
) {
    // List of some popular children's books in Korea for simulation
    val mockBooks = listOf(
        Pair("9788936433597", "알사탕 (백희나)"),
        Pair("9788952784537", "강아지 똥 (권정생)"),
        Pair("9788959195329", "구름빵 (백희나)"),
        Pair("9788936434129", "마당을 나온 암탉 (황선미)"),
        Pair("9788934986614", "설민석의 한국사 대모험")
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "바코드로 간편하게 등록하기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF44403C)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF44403C)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "책 뒷면의 ISBN 바코드를 카메라에 비추면 자동으로 등록됩니다.",
                    fontSize = 13.sp,
                    color = Color(0xFF44403C),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Barcode simulation view box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E1B4B))
                        .border(BorderStroke(2.dp, Color(0xFF8B5CF6).copy(alpha = alpha)), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ 바코드 스캔 준비 완료 ]",
                        color = Color(0xFF8B5CF6).copy(alpha = alpha),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "💡 터치해서 스캔 기능을 먼저 체험해 보세요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(mockBooks) { (isbn, title) ->
                        val displayName = when {
                            title.contains("알사탕") -> "🍬 알사탕"
                            title.contains("강아지 똥") -> "🐶 강아지 똥"
                            title.contains("구름빵") -> "☁️ 구름빵"
                            title.contains("마당을 나온 암탉") -> "🐓 마당을 나온 암탉"
                            else -> "📚 한국사 대모험"
                        }
                        
                        FilterChip(
                            selected = false,
                            onClick = { onScanSuccess(isbn, title) },
                            label = {
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF44403C)
                                    )
                                    Text(
                                        text = "ISBN: $isbn",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFFF5F5F4),
                                labelColor = Color(0xFF44403C)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFFDFCF0)
    )
}
