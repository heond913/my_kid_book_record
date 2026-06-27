package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class BookRepository(private val database: AppDatabase) {
    private val bookDao = database.bookDao()
    private val sessionDao = database.sessionDao()
    private val photoDao = database.photoDao()
    private val historyDao = database.historyDao()
    private val goalDao = database.goalDao()

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val allSessions: Flow<List<ReadingSession>> = sessionDao.getAllSessions()
    val allGoals: Flow<List<ReadingGoal>> = goalDao.getAllGoals()

    suspend fun getBookById(id: Int): Book? = bookDao.getBookById(id)

    suspend fun insertBook(book: Book): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: Book) = bookDao.updateBook(book)

    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)

    fun getSessionsForBook(bookId: Int): Flow<List<ReadingSession>> = 
        sessionDao.getSessionsForBook(bookId)

    suspend fun insertSession(session: ReadingSession): Long = 
        sessionDao.insertSession(session)

    suspend fun deleteSession(session: ReadingSession) = 
        sessionDao.deleteSession(session)

    fun getPhotosForBook(bookId: Int): Flow<List<BookPhoto>> = 
        photoDao.getPhotosForBook(bookId)

    fun getPhotosForSession(sessionId: Int): Flow<List<BookPhoto>> = 
        photoDao.getPhotosForSession(sessionId)

    suspend fun insertPhoto(photo: BookPhoto): Long = 
        photoDao.insertPhoto(photo)

    suspend fun updatePhoto(photo: BookPhoto) = 
        photoDao.updatePhoto(photo)

    suspend fun deletePhoto(photo: BookPhoto) = 
        photoDao.deletePhoto(photo)

    fun getHistoryForBook(bookId: Int): Flow<List<StatusHistory>> = 
        historyDao.getHistoryForBook(bookId)

    suspend fun insertHistory(history: StatusHistory): Long = 
        historyDao.insertHistory(history)

    suspend fun deleteHistory(history: StatusHistory) = 
        historyDao.deleteHistory(history)

    fun getGoalForPeriod(type: String, value: String): Flow<ReadingGoal?> = 
        goalDao.getGoalForPeriod(type, value)

    suspend fun insertGoal(goal: ReadingGoal): Long = 
        goalDao.insertGoal(goal)

    suspend fun deleteGoal(goal: ReadingGoal) = 
        goalDao.deleteGoal(goal)

    /**
     * Updates a book's status and automatically registers status-change history logs.
     */
    suspend fun updateBookStatus(bookId: Int, newStatus: String, changeDate: String) {
        val book = bookDao.getBookById(bookId) ?: return
        val oldStatus = book.status
        if (oldStatus == newStatus) return

        val updatedBook = book.copy(status = newStatus)
        bookDao.updateBook(updatedBook)

        val history = StatusHistory(
            bookId = bookId,
            fromStatus = oldStatus,
            toStatus = newStatus,
            changeDate = changeDate
        )
        historyDao.insertHistory(history)
    }

    /**
     * Inserts a book and its starting history log/reading session in a single database transaction.
     */
    suspend fun insertBookWithSessionAndHistory(
        book: Book,
        status: String,
        selectedDateStr: String
    ): Long {
        return database.withTransaction {
            val bookId = bookDao.insertBook(book)
            
            val history = StatusHistory(
                bookId = bookId.toInt(),
                fromStatus = "등록",
                toStatus = status,
                changeDate = selectedDateStr
            )
            historyDao.insertHistory(history)
            
            if (status == Book.STATUS_READING || status == Book.STATUS_COMPLETED) {
                val memo = if (status == Book.STATUS_COMPLETED) {
                    "완독하여 독서 포트폴리오에 첫 발자국을 남겼어요! 🥳"
                } else {
                    "책을 읽기 시작했어요! 🌱"
                }
                val session = ReadingSession(
                    bookId = bookId.toInt(),
                    startDate = selectedDateStr,
                    endDate = selectedDateStr,
                    title = if (status == Book.STATUS_COMPLETED) "완독 축하!" else "독서 시작",
                    memo = memo,
                    rating = 5,
                    tags = if (status == Book.STATUS_COMPLETED) "완독,성공" else "시작,독서"
                )
                sessionDao.insertSession(session)
            }
            bookId
        }
    }
}
