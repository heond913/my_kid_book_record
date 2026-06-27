package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.BookSearchResult
import com.example.data.api.BookSearchService
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// [요구사항 2] 상태 패러다임 전환 (Sealed Class 도입)
// 로딩, 성공, 에러, 결과 없음 상태를 정밀하게 표현하는 검색 상태 머신 정의
sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<BookSearchResult>) : SearchUiState
    data class Error(val exceptionMessage: String) : SearchUiState
    object Empty : SearchUiState
}

class BookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository
    private val sharedPrefs = application.getSharedPreferences("book_journal_prefs", Context.MODE_PRIVATE)
    private val searchCache = mutableMapOf<Pair<String, String>, List<BookSearchResult>>()
    var lastQuery: String = ""
    var lastSearchMode: String = "ALL"
    var lastSearchQuery: String = ""
    var lastSearchTab: String = "MY_LIBRARY"

    private val _childNameState = MutableStateFlow(getChildName())
    val childNameState: StateFlow<String> = _childNameState.asStateFlow()

    private val _selectedDate = MutableStateFlow<Calendar>(Calendar.getInstance().apply { time = Date() })
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    fun setSelectedDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    fun getFormattedDate(calendar: Calendar): String {
        val sdf = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private val _childGenderState = MutableStateFlow(getChildGender())
    val childGenderState: StateFlow<String> = _childGenderState.asStateFlow()

    private val _childPhotoUriState = MutableStateFlow(getChildPhotoUri())
    val childPhotoUriState: StateFlow<String> = _childPhotoUriState.asStateFlow()

    private val _childColorHexState = MutableStateFlow(getChildColorHex())
    val childColorHexState: StateFlow<String> = _childColorHexState.asStateFlow()

    private val _profilesState = MutableStateFlow(getProfiles())
    val profilesState: StateFlow<List<ChildProfile>> = _profilesState.asStateFlow()

    fun getChildName(): String {
        return sharedPrefs.getString("child_name", "") ?: ""
    }

    fun setChildName(name: String) {
        sharedPrefs.edit().putString("child_name", name).apply()
        _childNameState.value = name
    }

    fun getChildGender(): String {
        return sharedPrefs.getString("child_gender", "") ?: ""
    }

    fun setChildGender(gender: String) {
        sharedPrefs.edit().putString("child_gender", gender).apply()
        _childGenderState.value = gender
    }

    fun getChildPhotoUri(): String {
        return sharedPrefs.getString("child_photo_uri", "") ?: ""
    }

    fun setChildPhotoUri(uri: String) {
        sharedPrefs.edit().putString("child_photo_uri", uri).apply()
        _childPhotoUriState.value = uri
    }

    fun getChildColorHex(): String {
        return sharedPrefs.getString("child_color_hex", "#8B5CF6") ?: "#8B5CF6"
    }

    fun setChildColorHex(colorHex: String) {
        sharedPrefs.edit().putString("child_color_hex", colorHex).apply()
        _childColorHexState.value = colorHex
    }

    fun getProfiles(): List<ChildProfile> {
        val jsonStr = sharedPrefs.getString("child_profiles_list", null)
        if (jsonStr.isNullOrEmpty()) {
            val currentName = getChildName()
            if (currentName.isNotEmpty()) {
                val defaultProfile = ChildProfile(
                    name = currentName,
                    gender = getChildGender(),
                    photoUri = getChildPhotoUri(),
                    colorHex = getChildColorHex()
                )
                val list = listOf(defaultProfile)
                val array = org.json.JSONArray().apply {
                    val obj = org.json.JSONObject().apply {
                        put("name", defaultProfile.name)
                        put("gender", defaultProfile.gender)
                        put("photoUri", defaultProfile.photoUri)
                        put("colorHex", defaultProfile.colorHex)
                        put("birthDate", defaultProfile.birthDate)
                    }
                    put(obj)
                }
                sharedPrefs.edit().putString("child_profiles_list", array.toString()).apply()
                return list
            }
            return emptyList()
        }
        
        val list = mutableListOf<ChildProfile>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChildProfile(
                        name = obj.optString("name", ""),
                        gender = obj.optString("gender", "BOY"),
                        photoUri = obj.optString("photoUri", ""),
                        colorHex = obj.optString("colorHex", "#8B5CF6"),
                        birthDate = obj.optString("birthDate", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveProfiles(profiles: List<ChildProfile>) {
        val array = org.json.JSONArray()
        for (profile in profiles) {
            val obj = org.json.JSONObject().apply {
                put("name", profile.name)
                put("gender", profile.gender)
                put("photoUri", profile.photoUri)
                put("colorHex", profile.colorHex)
                put("birthDate", profile.birthDate)
            }
            array.put(obj)
        }
        sharedPrefs.edit().putString("child_profiles_list", array.toString()).apply()
        _profilesState.value = profiles
    }

    fun addProfile(profile: ChildProfile) {
        val currentList = getProfiles().toMutableList()
        currentList.removeAll { it.name == profile.name }
        currentList.add(profile)
        saveProfiles(currentList)
        switchProfile(profile)
    }

    fun switchProfile(profile: ChildProfile) {
        sharedPrefs.edit().apply {
            putString("child_name", profile.name)
            putString("child_gender", profile.gender)
            putString("child_photo_uri", profile.photoUri)
            putString("child_color_hex", profile.colorHex)
            apply()
        }
        _childNameState.value = profile.name
        _childGenderState.value = profile.gender
        _childPhotoUriState.value = profile.photoUri
        _childColorHexState.value = profile.colorHex
    }

    fun switchProfileByName(name: String) {
        val profile = getProfiles().find { it.name == name }
        if (profile != null) {
            switchProfile(profile)
        }
    }

    fun deleteProfile(name: String) {
        val currentList = getProfiles().toMutableList()
        currentList.removeAll { it.name == name }
        saveProfiles(currentList)
        if (_childNameState.value == name) {
            val first = currentList.firstOrNull()
            if (first != null) {
                switchProfile(first)
            } else {
                sharedPrefs.edit().apply {
                    putString("child_name", "")
                    putString("child_gender", "")
                    putString("child_photo_uri", "")
                    putString("child_color_hex", "#8B5CF6")
                    apply()
                }
                _childNameState.value = ""
                _childGenderState.value = ""
                _childPhotoUriState.value = ""
                _childColorHexState.value = "#8B5CF6"
            }
        }
    }

    fun updateProfile(oldName: String, updatedProfile: ChildProfile) {
        val currentList = getProfiles().toMutableList()
        val index = currentList.indexOfFirst { it.name == oldName }
        if (index != -1) {
            currentList[index] = updatedProfile
            saveProfiles(currentList)
            if (_childNameState.value == oldName) {
                switchProfile(updatedProfile)
            }
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BookRepository(database)
    }

    // --- State Streams ---
    val books: StateFlow<List<Book>> = repository.allBooks
        .combine(childNameState) { allBooks, currentChildName ->
            allBooks.filter { book ->
                book.childName == currentChildName || book.childName.isEmpty()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ReadingSession>> = repository.allSessions
        .combine(books) { allSessions, currentBooks ->
            val bookIds = currentBooks.map { it.id }.toSet()
            allSessions.filter { session ->
                bookIds.contains(session.bookId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<ReadingGoal>> = repository.allGoals
        .combine(childNameState) { allGoals, currentChildName ->
            allGoals.filter { goal ->
                goal.childName == currentChildName || goal.childName.isEmpty()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search States ---
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    // 하위 호환성 유지를 위해 searchUiState로부터 성공 결과 리스트를 추출하는 파생 흐름 정의
    val searchResults: StateFlow<List<BookSearchResult>> = _searchUiState
        .map { state ->
            when (state) {
                is SearchUiState.Success -> state.results
                else -> emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // --- Active states for Detail/Add screens ---
    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    private val _selectedBookSessions = MutableStateFlow<List<ReadingSession>>(emptyList())
    val selectedBookSessions: StateFlow<List<ReadingSession>> = _selectedBookSessions.asStateFlow()

    private val _selectedBookPhotos = MutableStateFlow<List<BookPhoto>>(emptyList())
    val selectedBookPhotos: StateFlow<List<BookPhoto>> = _selectedBookPhotos.asStateFlow()

    private val _selectedBookHistory = MutableStateFlow<List<StatusHistory>>(emptyList())
    val selectedBookHistory: StateFlow<List<StatusHistory>> = _selectedBookHistory.asStateFlow()

    // Recent books fast access
    val fastAccessBooks: StateFlow<List<Book>> = books.map { list ->
        list.take(5) // Fast access to last 5 books added
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRecentSearches()
    }

    // --- Repository Operations ---

    fun selectBook(bookId: Int) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            _selectedBook.value = book
            if (book != null) {
                // Collect sessions for this book
                repository.getSessionsForBook(bookId).collect {
                    _selectedBookSessions.value = it
                }
            }
        }
        viewModelScope.launch {
            repository.getPhotosForBook(bookId).collect {
                _selectedBookPhotos.value = it
            }
        }
        viewModelScope.launch {
            repository.getHistoryForBook(bookId).collect {
                _selectedBookHistory.value = it
            }
        }
    }

    fun insertBook(
        title: String,
        author: String,
        publisher: String,
        publishDate: String,
        isbn: String,
        category: String,
        coverUrl: String? = null,
        status: String = Book.STATUS_READING,
        readingDateStr: String? = null,
        onSuccess: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val book = Book(
                title = title,
                author = author,
                publisher = publisher,
                publishDate = publishDate,
                isbn = isbn,
                category = category,
                coverUrl = coverUrl,
                status = status,
                childName = getChildName()
            )
            val selectedDateStr = readingDateStr ?: getFormattedDate(_selectedDate.value)
            val id = repository.insertBookWithSessionAndHistory(book, status, selectedDateStr).toInt()
            onSuccess(id)
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            repository.updateBook(book)
            if (_selectedBook.value?.id == book.id) {
                _selectedBook.value = book
            }
        }
    }

    fun updateBookStatus(bookId: Int, newStatus: String) {
        viewModelScope.launch {
            val today = getFormattedToday()
            repository.updateBookStatus(bookId, newStatus, today)
            // Refresh selection
            val updated = repository.getBookById(bookId)
            _selectedBook.value = updated
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
            if (_selectedBook.value?.id == book.id) {
                _selectedBook.value = null
            }
        }
    }

    fun deleteHistory(history: StatusHistory) {
        viewModelScope.launch {
            repository.deleteHistory(history)
        }
    }

    fun insertHistory(history: StatusHistory) {
        viewModelScope.launch {
            repository.insertHistory(history)
        }
    }

    // --- Reading Session Operations ---

    fun addReadingSession(
        bookId: Int,
        startDate: String,
        endDate: String,
        title: String?,
        memo: String,
        rating: Int,
        tags: String,
        photos: List<Pair<String, String>>, // List of (Uri, Purpose)
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val session = ReadingSession(
                bookId = bookId,
                startDate = parseDate(startDate),
                endDate = parseDate(endDate),
                title = title,
                memo = memo,
                rating = rating,
                tags = tags
            )
            val sessionId = repository.insertSession(session).toInt()

            // Save photos
            photos.forEach { (uri, purpose) ->
                repository.insertPhoto(
                    BookPhoto(
                        bookId = bookId,
                        sessionId = sessionId,
                        uri = uri,
                        purpose = purpose
                    )
                )
            }
            onSuccess()
        }
    }

    fun deleteReadingSession(session: ReadingSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            // Refresh currently selected book sessions
            _selectedBookSessions.value = _selectedBookSessions.value.filter { it.id != session.id }
        }
    }

    // --- Book Photos Operations ---

    fun addGeneralBookPhoto(bookId: Int, uri: String, purpose: String, memo: String = "") {
        viewModelScope.launch {
            repository.insertPhoto(
                BookPhoto(
                    bookId = bookId,
                    uri = uri,
                    purpose = purpose,
                    memo = memo
                )
            )
        }
    }

    fun updatePhotoMemoAndRotation(photoId: Int, memo: String, rotation: Int) {
        viewModelScope.launch {
            val targetPhoto = _selectedBookPhotos.value.find { it.id == photoId } ?: return@launch
            val updated = targetPhoto.copy(memo = memo, rotation = rotation)
            repository.updatePhoto(updated)
            // Refresh
            _selectedBookPhotos.value = _selectedBookPhotos.value.map {
                if (it.id == photoId) updated else it
            }
        }
    }

    fun deletePhoto(photo: BookPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            _selectedBookPhotos.value = _selectedBookPhotos.value.filter { it.id != photo.id }
        }
    }

    // --- Reading Goal Operations ---

    fun setReadingGoal(periodType: String, periodValue: String, targetCount: Int, reward: String = "") {
        viewModelScope.launch {
            val currentChild = getChildName()
            val existing = goals.value.find { it.periodType == periodType && it.periodValue == periodValue }
            val goal = ReadingGoal(
                id = existing?.id ?: 0,
                periodType = periodType,
                periodValue = periodValue,
                targetCount = targetCount,
                childName = currentChild,
                reward = reward
            )
            repository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: ReadingGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // --- Book Catalog Search ---

    fun performExternalSearch(query: String, searchMode: String = "ALL") {
        if (query.isBlank()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }
        
        lastQuery = query
        lastSearchMode = searchMode
        
        val cacheKey = Pair(query.trim().lowercase(), searchMode)
        val cached = searchCache[cacheKey]
        if (cached != null) {
            Log.d("BookViewModel", "Returning cached search results for query: $query, mode: $searchMode")
            _searchUiState.value = SearchUiState.Success(cached)
            _isSearching.value = false
            return
        }

        _searchUiState.value = SearchUiState.Loading
        _isSearching.value = true
        saveSearchQuery(query)

        viewModelScope.launch {
            try {
                // [요구사항 4] 통합 검색 및 에러 자동 폴백(Fallback) 단일 진입점 파이프라인 호출
                val unifiedResults = BookSearchService.performUnifiedSearch(query, searchMode)
                if (unifiedResults.isEmpty()) {
                    _searchUiState.value = SearchUiState.Empty
                } else {
                    searchCache[cacheKey] = unifiedResults
                    _searchUiState.value = SearchUiState.Success(unifiedResults)
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Search failure", e)
                // [수정이 필요한 핵심 결함 사항 1] 유령 메서드 호출 제거 및 에러 상태 처리
                _searchUiState.value = SearchUiState.Error(e.localizedMessage ?: "검색 중 알 수 없는 에러가 발생했습니다.")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun restoreSearchResults(query: String, searchMode: String = "ALL") {
        if (query.isBlank()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }
        val cacheKey = Pair(query.trim().lowercase(), searchMode)
        val cached = searchCache[cacheKey]
        if (cached != null) {
            _searchUiState.value = SearchUiState.Success(cached)
            _isSearching.value = false
        } else {
            _searchUiState.value = SearchUiState.Idle
        }
    }

    fun clearSearchResults() {
        _searchUiState.value = SearchUiState.Idle
    }

    // --- Search Query History in SharedPrefs ---

    private fun loadRecentSearches() {
        val saved = sharedPrefs.getString("recent_searches", "") ?: ""
        if (saved.isNotEmpty()) {
            _recentSearches.value = saved.split("||")
        } else {
            _recentSearches.value = emptyList()
        }
    }

    private fun saveSearchQuery(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        val updated = current.take(10) // Cache top 10
        _recentSearches.value = updated
        sharedPrefs.edit().putString("recent_searches", updated.joinToString("||")).apply()
    }

    fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            saveSearchQuery(query)
        }
    }

    fun removeRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        _recentSearches.value = current
        sharedPrefs.edit().putString("recent_searches", current.joinToString("||")).apply()
    }

    // --- Utility calculations for Reports & Dashboard ---

    private fun isSameMonth(date: Date, monthValue: String): Boolean {
        // monthValue: "yyyy-MM" (e.g., "2026-06")
        val calDate = Calendar.getInstance().apply { time = date }
        
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val targetDate = sdf.parse(monthValue) ?: return false
            val calTarget = Calendar.getInstance().apply { time = targetDate }
            calDate.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calDate.get(Calendar.MONTH) == calTarget.get(Calendar.MONTH)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMonthStats(monthValue: String): MonthStats = withContext(Dispatchers.Default) {
        // monthValue: "YYYY-MM" (e.g., "2026-06")
        val currentBooks = books.value
        val currentSessions = sessions.value

        // Filter sessions occurring in this month
        val monthSessions = currentSessions.filter {
            isSameMonth(it.startDate, monthValue)
        }

        // 1. Reading days (독서일 수): Number of unique dates read
        val uniqueReadingDays = monthSessions.map { formatDate(it.startDate) }.distinct().size

        // 2. Count of completed books this month
        // A book is counted as completed this month if its current status is COMPLETED and it has a session ending this month, 
        // or was added this month. Let's look at Book's status and completed logs.
        val completedBooksThisMonth = currentBooks.filter { book ->
            book.status == Book.STATUS_COMPLETED && (
                monthSessions.any { it.bookId == book.id } || 
                formatTimestamp(book.addedTimestamp, "yyyy-MM") == monthValue
            )
        }.size

        // 3. Current streak (연속 독서일): Calculate current consecutive reading days up to today
        val streak = calculateStreak(currentSessions)

        MonthStats(
            booksReadCount = completedBooksThisMonth,
            readingDaysCount = uniqueReadingDays,
            currentStreak = streak
        )
    }

    suspend fun getCategoryPercentages(monthValue: String): Map<String, Float> = withContext(Dispatchers.Default) {
        // Find categories for all books read/logged this month
        val currentBooks = books.value
        val currentSessions = sessions.value

        val monthBookIds = currentSessions.filter {
            isSameMonth(it.startDate, monthValue)
        }.map { it.bookId }.toSet()

        val booksToAnalyze = currentBooks.filter { monthBookIds.contains(it.id) }
        if (booksToAnalyze.isEmpty()) {
            return@withContext Book.CATEGORIES.associateWith { 0f }
        }

        val total = booksToAnalyze.size.toFloat()
        val counts = booksToAnalyze.groupBy { it.category }.mapValues { it.value.size / total }
        
        val fullMap = mutableMapOf<String, Float>()
        Book.CATEGORIES.forEach { cat ->
            fullMap[cat] = counts[cat] ?: 0f
        }
        fullMap
    }

    suspend fun getMonthlyTrendData(): List<TrendItem> = withContext(Dispatchers.Default) {
        // Get last 6 months trend
        val currentSessions = sessions.value
        val currentBooks = books.value

        val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        val trendList = mutableListOf<TrendItem>()

        for (i in 5 downTo 0) {
            val tempCal = cal.clone() as Calendar
            tempCal.add(Calendar.MONTH, -i)
            val monthKey = format.format(tempCal.time)
            
            // Sessions in this month
            val monthSessCount = currentSessions.filter {
                isSameMonth(it.startDate, monthKey)
            }.size

            // Completed books in this month
            val monthCompCount = currentBooks.filter { book ->
                book.status == Book.STATUS_COMPLETED && (
                    currentSessions.any { it.bookId == book.id && isSameMonth(it.startDate, monthKey) } ||
                    formatTimestamp(book.addedTimestamp, "yyyy-MM") == monthKey
                )
            }.size

            trendList.add(
                TrendItem(
                    monthLabel = "${tempCal.get(Calendar.MONTH) + 1}월",
                    readingCount = monthSessCount,
                    completedCount = monthCompCount
                )
            )
        }
        trendList
    }

    private fun calculateStreak(allSessions: List<ReadingSession>): Int {
        if (allSessions.isEmpty()) return 0
        
        // Parse all unique session start dates into normalized timestamps
        val dates = allSessions.map {
            val cal = Calendar.getInstance().apply {
                time = it.startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.time
        }.distinct().sortedDescending()

        if (dates.isEmpty()) return 0

        // Check if there's reading today or yesterday to continue streak
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val firstReadDate = dates.first()
        val daysDiffToday = (today.time - firstReadDate.time) / (1000 * 60 * 60 * 24)

        if (daysDiffToday > 1) {
            // Streak broken (last reading was before yesterday)
            return 0
        }

        var streak = 1
        var currentDate = firstReadDate

        for (i in 1 until dates.size) {
            val nextDate = dates[i]
            val diff = (currentDate.time - nextDate.time) / (1000 * 60 * 60 * 24)
            if (diff == 1L) {
                streak++
                currentDate = nextDate
            } else if (diff > 1L) {
                break
            }
        }
        return streak
    }

    // Helpers
    private fun getFormattedToday(): String {
        val sdf = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun parseDateString(dateStr: String): Date? {
        val formats = listOf("yy/MM/dd", "yyyy-MM-dd", "yyyy/MM/dd", "yy-MM-dd")
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault())
                return sdf.parse(dateStr)
            } catch (e: Exception) {}
        }
        return null
    }

    private fun formatTimestamp(timestamp: Long, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

// --- Companion Data Classes ---
data class MonthStats(
    val booksReadCount: Int,
    val readingDaysCount: Int,
    val currentStreak: Int
)

data class TrendItem(
    val monthLabel: String,
    val readingCount: Int,
    val completedCount: Int
)

class BookViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
