package com.example.data.api

data class BookSearchResult(
    val title: String,
    val author: String,
    val publisher: String,
    val publishDate: String,
    val isbn: String,
    val category: String,
    val coverUrl: String? = null,
    val description: String = ""
)

enum class SearchMode {
    ALL,      // Google Books + Gemini 통합 검색
    GOOGLE,   // Google Books 단독 검색
    AI,       // Gemini 문맥 추천 검색
    LOCAL     // 오프라인 Fallback 더미 데이터셋 검색
}

object BookSearchService {
    
    fun getLocalFallbackResults(query: String): List<BookSearchResult> {
        return BookSearchCoordinator.getLocalFallbackResults(query)
    }

    suspend fun searchGoogleBooks(
        query: String,
        startTime: Long = System.currentTimeMillis(),
        timeoutMs: Long = 10000L
    ): List<BookSearchResult> {
        return BookSearchCoordinator.searchGoogleBooks(query, startTime, timeoutMs)
    }

    suspend fun searchWithGemini(
        query: String,
        startTime: Long = System.currentTimeMillis(),
        timeoutMs: Long = 10000L
    ): List<BookSearchResult> {
        return BookSearchCoordinator.searchWithGemini(query, startTime, timeoutMs)
    }

    suspend fun performUnifiedSearch(
        query: String,
        searchMode: SearchMode = SearchMode.ALL
    ): List<BookSearchResult> {
        return BookSearchCoordinator.performUnifiedSearch(query, searchMode)
    }
}
