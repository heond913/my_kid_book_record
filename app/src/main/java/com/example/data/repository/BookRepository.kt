package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [요구사항 1] BookRepository (도서 핵심 영속성 및 세션 라이프사이클)
 * - 책임: 도서 정보의 CRUD, 상세조회, 독서 세션(ReadingSession)의 추가/삭제 및 목표(ReadingGoal) 영속성 전담.
 * - 설계 안전성 (시니어 아키텍트 코멘트):
 *   1. 트랜잭션 원자성 보장: 복합 상태 변경 작업 시 Room Database의 [withTransaction] 블록을 활용하여
 *      도서 상태 수정과 이력 기록([StatusHistory]), 초기 독서 세션 발급을 단일 원자적 단위(Atomic Unit)로 묶습니다.
 *      중간에 예외가 발생하더라도 자동 롤백되어 데이터 불일치(Data Inconsistency)를 사전에 차단합니다.
 *   2. Main-Safety 보장: 모든 DB 디스크 입출력은 [Dispatchers.IO] 스레드 풀에서 안전하게 구동되어
 *      UI 스레드의 Block(화면 멈춤/Jank) 현상을 원천 방지합니다.
 */
class BookRepository(private val database: AppDatabase) {
    private val bookDao = database.bookDao()
    private val sessionDao = database.sessionDao()
    private val historyDao = database.historyDao()
    private val goalDao = database.goalDao()

    // --- State Streams (Data Observation Flow) ---
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val allSessions: Flow<List<ReadingSession>> = sessionDao.getAllSessions()
    val allGoals: Flow<List<ReadingGoal>> = goalDao.getAllGoals()

    // --- Core Book DB Operations ---
    suspend fun getBookById(id: Int): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)
    }

    suspend fun insertBook(book: BookEntity): Long = withContext(Dispatchers.IO) {
        bookDao.insertBook(book)
    }

    suspend fun updateBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(book)
    }

    // --- Reading Session Operations ---
    fun getSessionsForBook(bookId: Int): Flow<List<ReadingSession>> = 
        sessionDao.getSessionsForBook(bookId)

    suspend fun insertSession(session: ReadingSession): Long = withContext(Dispatchers.IO) {
        sessionDao.insertSession(session)
    }

    suspend fun deleteSession(session: ReadingSession) = withContext(Dispatchers.IO) {
        sessionDao.deleteSession(session)
    }

    // --- History Log Operations ---
    fun getHistoryForBook(bookId: Int): Flow<List<StatusHistory>> = 
        historyDao.getHistoryForBook(bookId)

    suspend fun insertHistory(history: StatusHistory): Long = withContext(Dispatchers.IO) {
        historyDao.insertHistory(history)
    }

    suspend fun deleteHistory(history: StatusHistory) = withContext(Dispatchers.IO) {
        historyDao.deleteHistory(history)
    }

    // --- Reading Goal Operations ---
    fun getGoalForPeriod(type: String, value: String): Flow<ReadingGoal?> = 
        goalDao.getGoalForPeriod(type, value)

    suspend fun insertGoal(goal: ReadingGoal): Long = withContext(Dispatchers.IO) {
        goalDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: ReadingGoal) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(goal)
    }

    /**
     * [트랜잭션 복합 연산 1] 도서 상태 변경 및 이력 데이터 트랜잭션 동기화
     */
    suspend fun updateBookStatus(bookId: Int, newStatus: String, changeDate: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val book = bookDao.getBookById(bookId) ?: return@withTransaction
            val oldStatus = book.status
            if (oldStatus == newStatus) return@withTransaction

            val updatedBook = book.copy(status = newStatus)
            bookDao.updateBook(updatedBook)

            val history = StatusHistory(
                bookId = bookId,
                fromStatus = oldStatus,
                toStatus = newStatus,
                changeDate = parseDate(changeDate)
            )
            historyDao.insertHistory(history)
        }
    }

    /**
     * [트랜잭션 복합 연산 2] 도서 등록, 초기 상태 로그 및 독서 첫 세션 원자적 통합 추가
     */
    suspend fun insertBookWithSessionAndHistory(
        book: BookEntity,
        status: String,
        selectedDateStr: String
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val bookId = bookDao.insertBook(book)
            
            val history = StatusHistory(
                bookId = bookId.toInt(),
                fromStatus = "등록",
                toStatus = status,
                changeDate = parseDate(selectedDateStr)
            )
            historyDao.insertHistory(history)
            
            if (status == BookEntity.STATUS_READING || status == BookEntity.STATUS_COMPLETED) {
                val memo = if (status == BookEntity.STATUS_COMPLETED) {
                    "완독하여 독서 포트폴리오에 첫 발자국을 남겼어요! 🥳"
                } else {
                    "책을 읽기 시작했어요! 🌱"
                }
                val session = ReadingSession(
                    bookId = bookId.toInt(),
                    startDate = parseDate(selectedDateStr),
                    endDate = parseDate(selectedDateStr),
                    title = if (status == BookEntity.STATUS_COMPLETED) "완독 축하!" else "독서 시작",
                    memo = memo,
                    rating = 5,
                    tags = if (status == BookEntity.STATUS_COMPLETED) "완독,성공" else "시작,독서"
                )
                sessionDao.insertSession(session)
            }
            bookId
        }
    }
}
