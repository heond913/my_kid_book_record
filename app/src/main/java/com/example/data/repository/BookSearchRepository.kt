package com.example.data.repository

import android.util.Log
import com.example.data.api.BookSearchCoordinator
import com.example.data.api.BookSearchResult
import com.example.data.api.SearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [요구사항 2] BookSearchRepository (온라인/오프라인 통합 검색 중재자)
 * - 책임: ViewModel이 직접 외부 도서 API 및 AI 서비스를 호출하지 못하도록 통제하고, 검색 정책을 총괄 기획 및 안전하게 조율.
 * - 설계 안전성 (시니어 아키텍트 코멘트):
 *   1. 예외 차단 및 격리 정책(Containment Policy): Google Books, Gemini AI 호출 등 원격 불안정 요소로 인한 네트워크 에러,
 *      타임아웃, 직렬화 실패 등의 예외(Throwable)가 상위 UI 레이어까지 도달해 크래시(Crash)를 유발하지 않도록 Repository 단에서 완벽히 소화(Catch)합니다.
 *   2. 로테이션 체인 및 폴백 전략: 구글 북스 장애 또는 결과가 없을 때 예외를 상위로 전파하지 않고,
 *      즉시 예비 인프라(Gemini AI 3.5-flash -> 2.5-flash 로테이션 체인)를 무중단 가동합니다.
 *      모든 원격 통신이 불통인 최악의 상황에서는 로컬 고성능 캐시 데이터셋(Local Fallback)을 정산하여 안정적인 데이터를 토스해 비즈니스 연속성을 보장합니다.
 *   3. Main-Safety 보장: 대용량 데이터 파싱 및 통신 조율은 [Dispatchers.IO] 컨텍스트 하에 격리 구동됩니다.
 */
class BookSearchRepository {
    private val TAG = "BookSearchRepository"

    /**
     * 외부 API(Google Books) 및 Gemini AI 통합 검색을 수행하고 안전한 도서 검색 결과를 보장합니다.
     */
    suspend fun performUnifiedSearch(query: String, searchMode: SearchMode = SearchMode.ALL): List<BookSearchResult> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Executing unified book search in repository for query: $query, mode: $searchMode")
                BookSearchCoordinator.performUnifiedSearch(query, searchMode)
            } catch (e: Exception) {
                // 상위 UI 레이어로 에러가 유출되어 앱이 중단되지 않도록 차단
                Log.e(TAG, "Unified search encountered an unhandled exception. Gracefully falling back to local dataset.", e)
                try {
                    BookSearchCoordinator.getLocalFallbackResults(query)
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Local fallback dataset retrieval also failed.", fallbackEx)
                    emptyList()
                }
            }
        }

    /**
     * 특정 쿼리에 매핑되는 정적 로컬 폴백 데이터셋을 직접 쿼리합니다.
     */
    fun getLocalFallbackResults(query: String): List<BookSearchResult> {
        return try {
            BookSearchCoordinator.getLocalFallbackResults(query)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve local fallback results", e)
            emptyList()
        }
    }
}
