package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.BuildConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Book
import com.example.data.api.BookSearchResult
import com.example.ui.viewmodel.BookViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel,
    onNavigateToBookDetail: (Int) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val books by viewModel.books.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val fastAccessBooks by viewModel.fastAccessBooks.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    // [요구사항 1] 외부 검색 및 로딩 상태 스트림 구독
    val searchUiState by viewModel.searchUiState.collectAsState()
    val onlineSearchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

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

    var searchQuery by remember { mutableStateOf(viewModel.lastSearchQuery) }
    val focusManager = LocalFocusManager.current
    var isSearchFieldFocused by remember { mutableStateOf(false) }
    
    // [요구사항 2] Bento 스타일의 모드 전환 탭(Tab) 레이아웃 도입
    var selectedTabStr by remember { mutableStateOf(viewModel.lastSearchTab) }

    LaunchedEffect(searchQuery) {
        viewModel.lastSearchQuery = searchQuery
    }

    LaunchedEffect(selectedTabStr) {
        viewModel.lastSearchTab = selectedTabStr
    }

    LaunchedEffect(Unit) {
        if (selectedTabStr == "ONLINE_SEARCH" && searchQuery.isNotBlank()) {
            viewModel.restoreSearchResults(searchQuery, viewModel.lastSearchMode)
        }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    // Collect all available tags from sessions to build tag filter list
    val allTags = remember(sessions) {
        sessions.flatMap { sess ->
            sess.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct().take(10)
    }

    // Real-time matching filter
    val filteredBooks = remember(books, sessions, searchQuery, selectedCategory, selectedStatus, selectedTag) {
        books.filter { book ->
            val matchesQuery = searchQuery.isBlank() || 
                    book.title.contains(searchQuery, ignoreCase = true) || 
                    book.author.contains(searchQuery, ignoreCase = true)
            
            val matchesCategory = selectedCategory == null || book.category == selectedCategory
            
            val matchesStatus = selectedStatus == null || book.status == selectedStatus
            
            val matchesTag = selectedTag == null || sessions.any { sess ->
                sess.bookId == book.id && sess.tags.contains(selectedTag!!, ignoreCase = true)
            }

            matchesQuery && matchesCategory && matchesStatus && matchesTag
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Bento Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedTabStr == "ONLINE_SEARCH") "온라인 새 도서 탐색" else "내 서재 검색 필터",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (selectedTabStr == "ONLINE_SEARCH") "새로운 책 발굴하기" else "$childNameWithJosa 도서 검색",
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

            // [요구사항 2] Bento 스타일의 모드 전환 탭(Tab) 레이아웃 도입
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "MY_LIBRARY" to "🏠 읽은 도서 검색",
                    "ONLINE_SEARCH" to "🌐 온라인 새 책 찾기"
                ).forEach { (tabId, label) ->
                    val isSelected = selectedTabStr == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { 
                                selectedTabStr = tabId 
                                if (tabId == "ONLINE_SEARCH") {
                                    searchQuery = viewModel.lastQuery
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.restoreSearchResults(searchQuery, viewModel.lastSearchMode)
                                    }
                                } else {
                                    searchQuery = ""
                                    viewModel.clearSearchResults()
                                }
                            }
                            .testTag("search_tab_$tabId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // [요구사항 3] 검색 텍스트 필드(`OutlinedTextField`)에 외부 검색 액션 연동
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it 
                    if (it.isEmpty() && selectedTabStr == "ONLINE_SEARCH") {
                        viewModel.clearSearchResults()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onFocusChanged { focusState ->
                        isSearchFieldFocused = focusState.isFocused
                    }
                    .testTag("search_input_field"),
                placeholder = { 
                    Text(
                        if (selectedTabStr == "ONLINE_SEARCH") "검색어 입력 후 돋보기/엔터 클릭!" 
                        else "책 제목, 저자명을 검색해보세요", 
                        fontSize = 14.sp
                    ) 
                },
                leadingIcon = { 
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                if (selectedTabStr == "ONLINE_SEARCH") {
                                    viewModel.performExternalSearch(searchQuery, "ALL")
                                } else {
                                    viewModel.addRecentSearch(searchQuery)
                                }
                                focusManager.clearFocus()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = "검색", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = "" 
                            if (selectedTabStr == "ONLINE_SEARCH") {
                                viewModel.clearSearchResults()
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "검색어 지우기")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            if (selectedTabStr == "ONLINE_SEARCH") {
                                viewModel.performExternalSearch(searchQuery, "ALL")
                            } else {
                                viewModel.addRecentSearch(searchQuery)
                            }
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Gemini API Key Status Banner
            val isGeminiConfigured = BuildConfig.GEMINI_API_KEY.isNotEmpty() && 
                    BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" && 
                    !BuildConfig.GEMINI_API_KEY.startsWith("YOUR_")

            if (selectedTabStr == "ONLINE_SEARCH" && !isGeminiConfigured) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "알림",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Gemini API 키가 설정되지 않았습니다",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AI 추천 검색어를 100% 가동하려면 AI Studio 우측의 [Secrets] 설정 패널에 'GEMINI_API_KEY'를 추가해 주세요. 현재는 초정밀 로컬 아동 도서 추천 엔진('소공녀', '해리포터' 등 지원)이 매끄럽게 연동 중입니다.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // 최근 검색어 Chip 리스트 표시 (포커스 시 또는 비어있지 않을 때)
            if (isSearchFieldFocused && recentSearches.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "최근 검색어",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "닫기",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { focusManager.clearFocus() }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentSearches) { query ->
                            SuggestionChip(
                                onClick = {
                                    searchQuery = query
                                    if (selectedTabStr == "ONLINE_SEARCH") {
                                        viewModel.performExternalSearch(query, "ALL")
                                    } else {
                                        viewModel.addRecentSearch(query)
                                    }
                                    focusManager.clearFocus()
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(text = query, fontSize = 11.sp)
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "삭제",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeRecentSearch(query) }
                                        )
                                    }
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // [요구사항 4] 로딩 애니메이션 및 결과 그리드 뷰 분기 처리
            if (selectedTabStr == "ONLINE_SEARCH" && isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "구글 도서 데이터베이스와 AI를 결합하여\n새로운 책을 검색하고 있는 중입니다...",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                if (selectedTabStr == "MY_LIBRARY") {
                    // Bento Filter Rows (Only shown for MY_LIBRARY)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Filter Scrollable Row
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("전체 분야", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEADDFF),
                                        selectedLabelColor = Color(0xFF21005D)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                            items(Book.CATEGORIES) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEADDFF),
                                        selectedLabelColor = Color(0xFF21005D)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("filter_category_$cat")
                                )
                            }
                        }

                        // Reading Status Filter Scrollable Row
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusList = listOf(
                                null to "전체 상태",
                                Book.STATUS_READING to "읽는 중",
                                Book.STATUS_COMPLETED to "완독"
                            )
                            items(statusList) { (status, label) ->
                                FilterChip(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEADDFF),
                                        selectedLabelColor = Color(0xFF21005D)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("filter_status_${status ?: "all"}")
                                )
                            }
                        }

                        // Tags filter list
                        if (allTags.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedTag == null,
                                        onClick = { selectedTag = null },
                                        label = { Text("전체 태그", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFEADDFF),
                                            selectedLabelColor = Color(0xFF21005D)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                                items(allTags) { tag ->
                                    FilterChip(
                                        selected = selectedTag == tag,
                                        onClick = { selectedTag = tag },
                                        label = { Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFEADDFF),
                                            selectedLabelColor = Color(0xFF21005D)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Results Count Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "검색 결과 (${filteredBooks.size}권)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (filteredBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (books.isEmpty()) "등록된 책이 없습니다.\n독서 캘린더에서 새 책을 등록해보세요!" else "조건에 맞는 도서가 없습니다.\n다른 필터를 터치하거나 새 키워드를 입력해보세요.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredBooks) { book ->
                                BookGridCard(
                                    book = book,
                                    onClick = { onNavigateToBookDetail(book.id) }
                                )
                            }
                        }
                    }

                    // Fast Access Shelf
                    if (searchQuery.isEmpty() && fastAccessBooks.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📌 최근에 연 독서 기록",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(fastAccessBooks) { book ->
                                        Column(
                                            modifier = Modifier
                                                .width(76.dp)
                                                .clickable { onNavigateToBookDetail(book.id) },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .size(64.dp, 84.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                            ) {
                                                if (!book.coverUrl.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = book.coverUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0xFFEADDFF)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = book.title.take(2),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF21005D)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = book.title,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ONLINE_SEARCH mode results
                    // [단계 2] SearchScreen.kt 내 컴포저블 상태 분기 리팩토링
                    when (val state = searchUiState) {
                        is com.example.ui.viewmodel.SearchUiState.Idle -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "원하는 책 제목이나 저자명을 검색해 보거나,\n'7세 아이가 좋아하는 따뜻한 공룡 책' 등\n자유로운 AI 테마 추천 검색어를 입력해 보세요!",
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        is com.example.ui.viewmodel.SearchUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "AI와 구글 북스가 맞춤 도서를 탐색하는 중...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "잠시만 기다려 주세요.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is com.example.ui.viewmodel.SearchUiState.Empty -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "검색 결과가 없습니다.",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "검색어를 보다 정확히 입력해 보시거나,\n다른 키워드로 검색을 시도해 보세요.",
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        is com.example.ui.viewmodel.SearchUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Error Icon",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "도서 검색 중 오류가 발생했습니다.",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = state.exceptionMessage,
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                        is com.example.ui.viewmodel.SearchUiState.Success -> {
                            val results = state.results
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "온라인 검색 추천 (${results.size}권)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(results) { result ->
                                        OnlineBookGridCard(
                                            result = result,
                                            onRegisterClick = {
                                                // [요구사항 3] 가짜 ISBN 오염 방지: 빈 문자열 ""로 안전하게 저장
                                                val safeIsbn = result.isbn
                                                viewModel.insertBook(
                                                    title = result.title,
                                                    author = result.author,
                                                    publisher = result.publisher.ifEmpty { "출판사 정보 없음" },
                                                    publishDate = result.publishDate.ifEmpty { "연도 미상" },
                                                    isbn = safeIsbn,
                                                    category = result.category.ifEmpty { "창작그림책" },
                                                    coverUrl = result.coverUrl,
                                                    status = Book.STATUS_READING,
                                                    onSuccess = { insertedId ->
                                                        onNavigateToBookDetail(insertedId)
                                                    }
                                                )
                                            }
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
}

@Composable
fun BookGridCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Book cover / Placeholder container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!book.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Floating Status Tag
                val statusLabel = when (book.status) {
                    Book.STATUS_WANT_TO_READ -> "읽고싶은"
                    Book.STATUS_READING -> "읽는중"
                    else -> "완독✨"
                }
                val statusColor = when (book.status) {
                    Book.STATUS_WANT_TO_READ -> Color(0xFF90A4AE)
                    Book.STATUS_READING -> Color(0xFF64B5F6)
                    else -> Color(0xFF81C784)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = book.author,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            SuggestionChip(
                onClick = {},
                label = { Text(book.category, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.height(22.dp),
                border = null,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFFEADDFF),
                    labelColor = Color(0xFF21005D)
                )
            )
        }
    }
}

@Composable
fun OnlineBookGridCard(
    result: BookSearchResult,
    onRegisterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRegisterClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Book cover / Placeholder container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!result.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = result.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // AI / Google Books label badge
                val isAiResult = result.description.contains("AI", ignoreCase = true) || result.isbn.isEmpty()
                val badgeLabel = if (isAiResult) "AI 추천" else "Google 도서"
                val badgeBgColor = if (isAiResult) Color(0xFFE0F2FE) else Color(0xFFF1F5F9)
                val badgeTextColor = if (isAiResult) Color(0xFF0369A1) else Color(0xFF475569)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 9.sp,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // [요구사항 5] CloudDownload 버튼 배치
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .shadow(4.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { onRegisterClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "내 서재에 저장",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = result.author,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(result.category.ifEmpty { "창작그림책" }, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.height(22.dp),
                    border = null,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFFF3E8FF),
                        labelColor = Color(0xFF6B21A8)
                    )
                )
            }
        }
    }
}
