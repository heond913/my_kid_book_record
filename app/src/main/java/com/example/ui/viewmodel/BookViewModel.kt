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
import java.text.SimpleDateFormat
import java.util.*

class BookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository
    private val sharedPrefs = application.getSharedPreferences("book_journal_prefs", Context.MODE_PRIVATE)

    private val _childNameState = MutableStateFlow(getChildName())
    val childNameState: StateFlow<String> = _childNameState.asStateFlow()

    private val _childGenderState = MutableStateFlow(getChildGender())
    val childGenderState: StateFlow<String> = _childGenderState.asStateFlow()

    private val _childPhotoUriState = MutableStateFlow(getChildPhotoUri())
    val childPhotoUriState: StateFlow<String> = _childPhotoUriState.asStateFlow()

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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BookRepository(database)
    }

    // --- State Streams ---
    val books: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ReadingSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<ReadingGoal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search States ---
    private val _searchResults = MutableStateFlow<List<BookSearchResult>>(emptyList())
    val searchResults: StateFlow<List<BookSearchResult>> = _searchResults.asStateFlow()

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
        status: String = Book.STATUS_WANT_TO_READ,
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
                status = status
            )
            val id = repository.insertBook(book).toInt()
            
            // Log status history initial state
            val today = getFormattedToday()
            repository.insertHistory(
                StatusHistory(
                    bookId = id,
                    fromStatus = "등록",
                    toStatus = status,
                    changeDate = today
                )
            )
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
                startDate = startDate,
                endDate = endDate,
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

    fun setReadingGoal(periodType: String, periodValue: String, targetCount: Int) {
        viewModelScope.launch {
            val existingFlow = repository.getGoalForPeriod(periodType, periodValue)
            val existing = existingFlow.firstOrNull()
            val goal = ReadingGoal(
                id = existing?.id ?: 0,
                periodType = periodType,
                periodValue = periodValue,
                targetCount = targetCount
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
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        saveSearchQuery(query)

        viewModelScope.launch {
            try {
                val googleResults = BookSearchService.searchGoogleBooks(query)
                val geminiResults = if (searchMode == "AI" || googleResults.isEmpty()) {
                    BookSearchService.searchWithGemini(query)
                } else {
                    emptyList()
                }

                // Merge lists nicely, deduplicating by title/author
                val merged = (googleResults + geminiResults).distinctBy { 
                    it.title.lowercase().trim() + it.author.lowercase().trim() 
                }

                _searchResults.value = merged
            } catch (e: Exception) {
                Log.e("BookViewModel", "Search failure", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
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

    fun getMonthStats(monthValue: String): MonthStats {
        // monthValue: "YYYY-MM" (e.g., "2026-06")
        val currentBooks = books.value
        val currentSessions = sessions.value

        // Filter sessions occurring in this month
        // In session, dates are saved as "26/06/03" or "2026-06-03". Let's parse both formats.
        val monthSessions = currentSessions.filter {
            val dateStr = it.startDate
            dateStr.contains(monthValue.substring(2)) || dateStr.contains(monthValue)
        }

        // 1. Reading days (독서일 수): Number of unique dates read
        val uniqueReadingDays = monthSessions.map { it.startDate }.distinct().size

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

        return MonthStats(
            booksReadCount = completedBooksThisMonth,
            readingDaysCount = uniqueReadingDays,
            currentStreak = streak
        )
    }

    fun getCategoryPercentages(monthValue: String): Map<String, Float> {
        // Find categories for all books read/logged this month
        val currentBooks = books.value
        val currentSessions = sessions.value

        val monthBookIds = currentSessions.filter {
            it.startDate.contains(monthValue.substring(2)) || it.startDate.contains(monthValue)
        }.map { it.bookId }.toSet()

        val booksToAnalyze = currentBooks.filter { monthBookIds.contains(it.id) }
        if (booksToAnalyze.isEmpty()) {
            return Book.CATEGORIES.associateWith { 0f }
        }

        val total = booksToAnalyze.size.toFloat()
        val counts = booksToAnalyze.groupBy { it.category }.mapValues { it.value.size / total }
        
        val fullMap = mutableMapOf<String, Float>()
        Book.CATEGORIES.forEach { cat ->
            fullMap[cat] = counts[cat] ?: 0f
        }
        return fullMap
    }

    fun getMonthlyTrendData(): List<TrendItem> {
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
                it.startDate.contains(monthKey.substring(2)) || it.startDate.contains(monthKey)
            }.size

            // Completed books in this month
            val monthCompCount = currentBooks.filter { book ->
                book.status == Book.STATUS_COMPLETED && (
                    currentSessions.any { it.bookId == book.id && (it.startDate.contains(monthKey.substring(2)) || it.startDate.contains(monthKey)) } ||
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
        return trendList
    }

    private fun calculateStreak(allSessions: List<ReadingSession>): Int {
        if (allSessions.isEmpty()) return 0
        
        // Parse all unique session start dates into timestamps
        val dates = allSessions.mapNotNull {
            try {
                parseDateString(it.startDate)
            } catch (e: Exception) {
                null
            }
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
