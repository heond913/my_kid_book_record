package com.example.data.repository

import com.example.data.model.*
import com.example.ui.viewmodel.MonthStats
import com.example.ui.viewmodel.TrendItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * [요구사항 3] StatisticsRepository (통합 독서 통계 및 CPU 연산 격리)
 * - 책임: 캘린더 일수 합산, 연속 독서일(Streak) 트래킹, 월간 분량 카운트, 장르 백분율 산출 등 무거운 비즈니스 연산 수행.
 * - 설계 안전성 (시니어 아키텍트 코멘트):
 *   1. CPU 바운드 작업 격리 (Main-Safety): 날짜 변환(SimpleDateFormat), 그룹핑, 정렬, 대량의 독서 히스토리 비교 등
 *      CPU 연산 강도가 매우 높은 비즈니스 연산들이 UI 메인 스레드를 점유해 프레임 드랍(UI Jank)을 유발하지 않도록,
 *      모든 함수 내부를 [Dispatchers.Default] 스레드 풀로 강제 전환하여 격리 수행합니다.
 *   2. 순수 함수형 데이터 처리: 데이터베이스의 원천 데이터를 인자로 받아 정제된 수식 결과를 연산 및 반환하는 구조로 설계하여
 *      부작용(Side Effect)이 없고 유닛 테스트(Unit Test)에 대단히 유용한 코드를 완성합니다.
 */
class StatisticsRepository {

    /**
     * 특정 월에 해당하는 독서 통계 정보를 계산합니다.
     */
    suspend fun getMonthStats(
        monthValue: String, 
        books: List<BookUiModel>, 
        sessions: List<ReadingSession>
    ): MonthStats = withContext(Dispatchers.Default) {
        // monthValue: "YYYY-MM" (e.g., "2026-06")
        
        // Filter sessions occurring in this month
        val monthSessions = sessions.filter {
            isSameMonth(it.startDate, monthValue)
        }

        // 1. Reading days (독서일 수): Number of unique dates read
        val uniqueReadingDays = monthSessions.map { formatDate(it.startDate) }.distinct().size

        // 2. Count of completed books this month
        val completedBooksThisMonth = books.filter { book ->
            book.status == BookEntity.STATUS_COMPLETED && (
                monthSessions.any { it.bookId == book.id } || 
                formatTimestamp(book.addedTimestamp, "yyyy-MM") == monthValue
            )
        }.size

        // 3. Current streak (연속 독서일): Calculate current consecutive reading days up to today
        val streak = calculateStreak(sessions)

        MonthStats(
            booksReadCount = completedBooksThisMonth,
            readingDaysCount = uniqueReadingDays,
            currentStreak = streak
        )
    }

    /**
     * 특정 월의 도서 장르별 독서 백분율을 산출합니다.
     */
    suspend fun getCategoryPercentages(
        monthValue: String, 
        books: List<BookUiModel>, 
        sessions: List<ReadingSession>
    ): Map<String, Float> = withContext(Dispatchers.Default) {
        val monthBookIds = sessions.filter {
            isSameMonth(it.startDate, monthValue)
        }.map { it.bookId }.toSet()

        val booksToAnalyze = books.filter { monthBookIds.contains(it.id) }
        if (booksToAnalyze.isEmpty()) {
            return@withContext BookEntity.CATEGORIES.associateWith { 0f }
        }

        val total = booksToAnalyze.size.toFloat()
        val counts = booksToAnalyze.groupBy { it.category }.mapValues { it.value.size / total }
        
        val fullMap = mutableMapOf<String, Float>()
        BookEntity.CATEGORIES.forEach { cat ->
            fullMap[cat] = counts[cat] ?: 0f
        }
        fullMap
    }

    /**
     * 최근 6개월간의 월별 세션 수 및 완독 도서량 트렌드 아이템 목록을 산출합니다.
     */
    suspend fun getMonthlyTrendData(
        books: List<BookUiModel>, 
        sessions: List<ReadingSession>
    ): List<TrendItem> = withContext(Dispatchers.Default) {
        val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        val trendList = mutableListOf<TrendItem>()

        for (i in 5 downTo 0) {
            val tempCal = cal.clone() as Calendar
            tempCal.add(Calendar.MONTH, -i)
            val monthKey = format.format(tempCal.time)
            
            // Sessions in this month
            val monthSessCount = sessions.filter {
                isSameMonth(it.startDate, monthKey)
            }.size

            // Completed books in this month
            val monthCompCount = books.filter { book ->
                book.status == BookEntity.STATUS_COMPLETED && (
                    sessions.any { it.bookId == book.id && isSameMonth(it.startDate, monthKey) } ||
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
        trendList
    }

    /**
     * 전체 독서 세션 내역을 바탕으로 오늘/어제 기준 연속 독서일(Streak)을 도출합니다.
     */
    fun calculateStreak(allSessions: List<ReadingSession>): Int {
        if (allSessions.isEmpty()) return 0
        
        // Parse all unique session start dates into normalized timestamps
        val dates = allSessions.map {
            val cal = Calendar.getInstance().apply {
                time = it.startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.time
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

    // --- Private Math Helpers ---
    private fun isSameMonth(date: Date, monthValue: String): Boolean {
        val calDate = Calendar.getInstance().apply { time = date }
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val targetDate = sdf.parse(monthValue) ?: return false
            val calTarget = Calendar.getInstance().apply { time = targetDate }
            calDate.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calDate.get(Calendar.MONTH) == calTarget.get(Calendar.MONTH)
        } catch (e: Exception) {
            false
        }
    }

    private fun formatTimestamp(timestamp: Long, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
