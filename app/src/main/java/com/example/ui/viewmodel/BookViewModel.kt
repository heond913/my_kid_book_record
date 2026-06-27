package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.BookSearchResult
import com.example.data.model.*
import com.example.data.db.AppDatabase
import com.example.data.repository.BookRepository
import com.example.data.repository.BookSearchRepository
import com.example.data.repository.PhotoRepository
import com.example.data.repository.StatisticsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 로딩, 성공, 에러, 결과 없음 상태를 정밀하게 표현하는 검색 상태 머신 정의
sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<BookSearchResult>) : SearchUiState
    data class Error(val exceptionMessage: String) : SearchUiState
    object Empty : SearchUiState
}

/**
 * [최종 리팩토링 리비전] BookViewModel
 * - 책임: UI 상태 제어(UI State Management) 및 사용자 흐름 생명주기 관리 전담.
 * - 설계 안전성 (시니어 아키텍트 코멘트):
 *   1. 관심사 분리(SoC): 비대한 비즈니스 연산과 영속성, 외부 API 쿼리 책임을 4대 전용 레포지토리로 완전 이관했습니다.
 *      이제 뷰모델은 UI의 프리젠테이션 로직과 비즈니스 흐름 중재(Orchestration)라는 본연의 단일 책임(SRP)에만 충실합니다.
 *   2. 메인 스레드 안전성(Main-Safety): UI 스레드 상에서의 중복 캐싱이나 계산을 지양하고,
 *      모든 독서 통계 및 장르 수식 분석은 [StatisticsRepository]를 통해 격리 수행됩니다.
 *   3. 검색 트랜잭션 보호: [BookSearchRepository]를 활용해 구글 API 및 백업 Gemini 로테이션 예외를 안전하게 캡슐화합니다.
 */
class BookViewModel(
    application: Application,
    private val bookRepository: BookRepository,
    private val searchRepository: BookSearchRepository,
    private val statsRepository: StatisticsRepository,
    private val photoRepository: PhotoRepository
) : AndroidViewModel(application) {

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

    // --- Dynamic Data Observations (Reactive UI streams) ---
    val books: StateFlow<List<Book>> = bookRepository.allBooks
        .combine(childNameState) { allBooks, currentChildName ->
            allBooks.filter { book ->
                book.childName == currentChildName || book.childName.isEmpty()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ReadingSession>> = bookRepository.allSessions
        .combine(books) { allSessions, currentBooks ->
            val bookIds = currentBooks.map { it.id }.toSet()
            allSessions.filter { session ->
                bookIds.contains(session.bookId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<ReadingGoal>> = bookRepository.allGoals
        .combine(childNameState) { allGoals, currentChildName ->
            allGoals.filter { goal ->
                goal.childName == currentChildName || goal.childName.isEmpty()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search States ---
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

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
        list.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRecentSearches()
    }

    // --- Core Operations (Delegated to specialized repositories) ---

    fun selectBook(bookId: Int) {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            _selectedBook.value = book
            if (book != null) {
                bookRepository.getSessionsForBook(bookId).collect {
                    _selectedBookSessions.value = it
                }
            }
        }
        viewModelScope.launch {
            photoRepository.getPhotosForBook(bookId).collect {
                _selectedBookPhotos.value = it
            }
        }
        viewModelScope.launch {
            bookRepository.getHistoryForBook(bookId).collect {
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
            val id = bookRepository.insertBookWithSessionAndHistory(book, status, selectedDateStr).toInt()
            onSuccess(id)
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            bookRepository.updateBook(book)
            if (_selectedBook.value?.id == book.id) {
                _selectedBook.value = book
            }
        }
    }

    fun updateBookStatus(bookId: Int, newStatus: String) {
        viewModelScope.launch {
            val today = getFormattedToday()
            bookRepository.updateBookStatus(bookId, newStatus, today)
            val updated = bookRepository.getBookById(bookId)
            _selectedBook.value = updated
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            bookRepository.deleteBook(book)
            if (_selectedBook.value?.id == book.id) {
                _selectedBook.value = null
            }
        }
    }

    fun deleteHistory(history: StatusHistory) {
        viewModelScope.launch {
            bookRepository.deleteHistory(history)
        }
    }

    fun insertHistory(history: StatusHistory) {
        viewModelScope.launch {
            bookRepository.insertHistory(history)
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
            val sessionId = bookRepository.insertSession(session).toInt()

            photos.forEach { (uri, purpose) ->
                photoRepository.insertPhoto(
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
            bookRepository.deleteSession(session)
            _selectedBookSessions.value = _selectedBookSessions.value.filter { it.id != session.id }
        }
    }

    // --- Book Photos Operations ---

    fun addGeneralBookPhoto(bookId: Int, uri: String, purpose: String, memo: String = "") {
        viewModelScope.launch {
            photoRepository.insertPhoto(
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
            photoRepository.updatePhoto(updated)
            _selectedBookPhotos.value = _selectedBookPhotos.value.map {
                if (it.id == photoId) updated else it
            }
        }
    }

    fun deletePhoto(photo: BookPhoto) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
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
            bookRepository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: ReadingGoal) {
        viewModelScope.launch {
            bookRepository.deleteGoal(goal)
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
                val unifiedResults = searchRepository.performUnifiedSearch(query, searchMode)
                if (unifiedResults.isEmpty()) {
                    _searchUiState.value = SearchUiState.Empty
                } else {
                    searchCache[cacheKey] = unifiedResults
                    _searchUiState.value = SearchUiState.Success(unifiedResults)
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Search failure", e)
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

    // --- Statistics and Math Calculations (Delegated to StatisticsRepository for Main-Safety) ---

    suspend fun getMonthStats(monthValue: String): MonthStats {
        return statsRepository.getMonthStats(monthValue, books.value, sessions.value)
    }

    suspend fun getCategoryPercentages(monthValue: String): Map<String, Float> {
        return statsRepository.getCategoryPercentages(monthValue, books.value, sessions.value)
    }

    suspend fun getMonthlyTrendData(): List<TrendItem> {
        return statsRepository.getMonthlyTrendData(books.value, sessions.value)
    }

    // Helpers
    private fun getFormattedToday(): String {
        val sdf = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
        return sdf.format(Date())
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
            val database = AppDatabase.getDatabase(application)
            val bookRepository = BookRepository(database)
            val searchRepository = BookSearchRepository()
            val statsRepository = StatisticsRepository()
            val photoRepository = PhotoRepository(database)
            
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(
                application,
                bookRepository,
                searchRepository,
                statsRepository,
                photoRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
