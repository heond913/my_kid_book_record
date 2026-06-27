package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getSessionsForBook(bookId: Int): Flow<List<ReadingSession>>

    @Query("SELECT * FROM reading_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ReadingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSession): Long

    @Delete
    suspend fun deleteSession(session: ReadingSession)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM book_photos WHERE bookId = :bookId")
    fun getPhotosForBook(bookId: Int): Flow<List<BookPhoto>>

    @Query("SELECT * FROM book_photos WHERE sessionId = :sessionId")
    fun getPhotosForSession(sessionId: Int): Flow<List<BookPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: BookPhoto): Long

    @Update
    suspend fun updatePhoto(photo: BookPhoto)

    @Delete
    suspend fun deletePhoto(photo: BookPhoto)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM status_histories WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getHistoryForBook(bookId: Int): Flow<List<StatusHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: StatusHistory): Long

    @Delete
    suspend fun deleteHistory(history: StatusHistory)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM reading_goals ORDER BY timestamp DESC")
    fun getAllGoals(): Flow<List<ReadingGoal>>

    @Query("SELECT * FROM reading_goals WHERE periodType = :type AND periodValue = :value LIMIT 1")
    fun getGoalForPeriod(type: String, value: String): Flow<ReadingGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: ReadingGoal): Long

    @Delete
    suspend fun deleteGoal(goal: ReadingGoal)
}
