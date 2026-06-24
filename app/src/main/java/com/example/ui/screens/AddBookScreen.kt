package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
    var initialStatus by remember { mutableStateOf(Book.STATUS_WANT_TO_READ) }

    // Dropdown state for category selection
    var categoryExpanded by remember { mutableStateOf(false) }

    // Simulated scanner overlay dialog state
    var showScannerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 책 등록하기", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab row to switch registration style
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { 
                        activeTab = 0 
                        searchQuery = viewModel.lastQuery
                        if (searchQuery.isNotBlank()) {
                            viewModel.restoreSearchResults(searchQuery, searchMode)
                        }
                    },
                    text = { Text("도서 검색 등록", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { 
                        activeTab = 1
                    },
                    text = { Text("직접 수동 입력", fontWeight = FontWeight.Bold) }
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
                        placeholder = { Text("책 제목, 저자, ISBN 검색...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("일반 검색")
                        }

                        Button(
                            onClick = { 
                                searchMode = "AI"
                                viewModel.performExternalSearch(searchQuery, "AI")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🤖 AI 검색")
                        }

                        // Scanner simulation button
                        IconButton(
                            onClick = { showScannerDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "바코드 스캔")
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
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchMode == "AI") "🤖 AI가 똑똑하게 어린이 도서 데이터를 분석하는 중..." else "온라인 도서 정보 검색 중...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "원하는 책을 검색해보세요!\n어린이 베스트셀러 도서는 [🤖 AI 검색]을 추천합니다.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "도서 검색 결과 (${searchResults.size}건)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "도서 기본 정보",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("책 제목 (필수)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_title_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("저자 (필수)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_author_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("출판사") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = publishDate,
                            onValueChange = { publishDate = it },
                            label = { Text("출간일 (예: 2018-05)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = isbn,
                            onValueChange = { isbn = it },
                            label = { Text("ISBN 번호") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Category Selector Dropdown
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
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                Book.CATEGORIES.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("커버 이미지 URL (선택)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "독서 초기 상태 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusSelectCard(
                            label = "읽고 싶은 책",
                            isSelected = initialStatus == Book.STATUS_WANT_TO_READ,
                            color = Color(0xFF90A4AE),
                            modifier = Modifier.weight(1f),
                            onClick = { initialStatus = Book.STATUS_WANT_TO_READ }
                        )
                        StatusSelectCard(
                            label = "읽는 중",
                            isSelected = initialStatus == Book.STATUS_READING,
                            color = Color(0xFF64B5F6),
                            modifier = Modifier.weight(1f),
                            onClick = { initialStatus = Book.STATUS_READING }
                        )
                        StatusSelectCard(
                            label = "완독",
                            isSelected = initialStatus == Book.STATUS_COMPLETED,
                            color = Color(0xFF81C784),
                            modifier = Modifier.weight(1f),
                            onClick = { initialStatus = Book.STATUS_COMPLETED }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                    onSuccess = onSuccess
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_book_button"),
                        enabled = title.isNotBlank() && author.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("책 등록하고 저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- SIMULATED BARCODE SCANNER DIALOG ---
            if (showScannerDialog) {
                SimulatedScannerDialog(
                    onDismiss = { showScannerDialog = false },
                    onScanSuccess = { scannedIsbn, scannedTitle ->
                        showScannerDialog = false
                        searchQuery = scannedIsbn
                        activeTab = 0
                        viewModel.performExternalSearch(scannedIsbn, "ALL")
                    }
                )
            }
        }
    }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            Card(
                modifier = Modifier.size(55.dp, 75.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (!result.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = result.coverUrl,
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
                        Text(
                            text = result.title.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${result.author} • ${result.publisher}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.description.isNotEmpty()) {
                    Text(
                        text = result.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(result.category, fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp),
                        border = null,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "선택하기",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun StatusSelectCard(
    label: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) color else Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        title = { Text("바코드 스캐너 시뮬레이션", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "책 뒷면의 ISBN 바코드를 카메라에 인식시킵니다.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Barcode simulation view box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Visual scanning line decoration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Red)
                    )
                    Text(
                        text = "[ 바코드 탐색 중... ]",
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "시뮬레이션용 가상 도서 바코드를 선택하세요:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    mockBooks.forEach { (isbn, title) ->
                        Button(
                            onClick = { onScanSuccess(isbn, title) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "ISBN: $isbn", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    )
}
