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
        searchMode: String = "ALL"
    ): List<BookSearchResult> {
        return BookSearchCoordinator.performUnifiedSearch(query, searchMode)
    }
}
