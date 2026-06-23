package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val publisher: String,
    val publishDate: String,
    val isbn: String,
    val category: String, // e.g. "동화", "과학", "역사", "문학", "기타"
    val coverUrl: String? = null,
    val status: String = STATUS_WANT_TO_READ,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_WANT_TO_READ = "WANT_TO_READ"
        const val STATUS_READING = "READING"
        const val STATUS_COMPLETED = "COMPLETED"

        val CATEGORIES = listOf("동화", "과학", "역사", "문학", "기타")
    }
}

@Entity(tableName = "reading_sessions")
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val startDate: String, // Format: YY/MM/DD, e.g. 26/06/01
    val endDate: String,   // Format: YY/MM/DD, e.g. 26/06/03
    val title: String? = null, // Optional session title, e.g. "1회차 기록"
    val memo: String,
    val rating: Int = 5,   // 1 to 5 stars
    val tags: String = "", // Comma-separated, e.g. "재밌음, 슬픔"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "book_photos")
data class BookPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val sessionId: Int? = null,
    val uri: String, // Absolute path or Uri representation
    val purpose: String, // "책 표지 인증", "인상 깊은 문장", "중요 내용 기록"
    val memo: String = "",
    val rotation: Int = 0 // Rotation angle (0, 90, 180, 270)
) {
    companion object {
        const val PURPOSE_COVER = "책 표지 인증"
        const val PURPOSE_SENTENCE = "인상 깊은 문장"
        const val PURPOSE_CONTENT = "중요 내용 기록"

        val PURPOSES = listOf(PURPOSE_COVER, PURPOSE_SENTENCE, PURPOSE_CONTENT)
    }
}

@Entity(tableName = "status_histories")
data class StatusHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val fromStatus: String,
    val toStatus: String,
    val changeDate: String, // Format: YY/MM/DD
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_goals")
data class ReadingGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val periodType: String, // "MONTHLY", "QUARTERLY", "YEARLY"
    val periodValue: String, // Format: "YYYY-MM" (Monthly), "YYYY-QX" (Quarterly), "YYYY" (Yearly)
    val targetCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
