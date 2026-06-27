package com.example.data.api

import com.example.data.model.BookEntity
import com.example.data.model.BookUiModel

data class BookDto(
    val title: String,
    val author: String,
    val publisher: String,
    val publishDate: String,
    val isbn: String,
    val category: String,
    val coverUrl: String? = null,
    val description: String = ""
)

fun BookDto.toUiModel(): BookUiModel {
    return BookUiModel(
        title = title,
        author = author,
        publisher = publisher,
        publishDate = publishDate,
        isbn = isbn,
        category = category,
        coverUrl = coverUrl,
        status = BookEntity.STATUS_READING,
        addedTimestamp = System.currentTimeMillis(),
        childName = ""
    )
}

fun BookDto.toEntity(
    status: String = BookEntity.STATUS_READING,
    addedTimestamp: Long = System.currentTimeMillis(),
    childName: String = ""
): BookEntity {
    return BookEntity(
        title = title,
        author = author,
        publisher = publisher,
        publishDate = publishDate,
        isbn = isbn,
        category = category,
        coverUrl = coverUrl,
        status = status,
        addedTimestamp = addedTimestamp,
        childName = childName
    )
}

enum class SearchMode {
    ALL,      // Google Books + Gemini 통합 검색
    GOOGLE,   // Google Books 단독 검색
    AI,       // Gemini 문맥 추천 검색
    LOCAL     // 오프라인 Fallback 더미 데이터셋 검색
}

object BookSearchService {
    
    fun getLocalFallbackResults(query: String): List<BookDto> {
        return BookSearchCoordinator.getLocalFallbackResults(query)
    }

    suspend fun searchGoogleBooks(
        query: String,
        startTime: Long = System.currentTimeMillis(),
        timeoutMs: Long = 10000L
    ): List<BookDto> {
        return BookSearchCoordinator.searchGoogleBooks(query, startTime, timeoutMs)
    }

    suspend fun searchWithGemini(
        query: String,
        startTime: Long = System.currentTimeMillis(),
        timeoutMs: Long = 10000L
    ): List<BookDto> {
        return BookSearchCoordinator.searchWithGemini(query, startTime, timeoutMs)
    }

    suspend fun performUnifiedSearch(
        query: String,
        searchMode: SearchMode = SearchMode.ALL
    ): List<BookDto> {
        return BookSearchCoordinator.performUnifiedSearch(query, searchMode)
    }
}
