package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Book
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

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
    private const val TAG = "BookSearchService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val fallbackBooks = listOf(
        BookSearchResult("알사탕", "백희나", "책읽는곰", "2017-03", "9791183267563", "동화", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=200", "상대방의 마음을 들을 수 있는 신비한 알사탕을 통해 타인을 이해하고 위로하게 되는 따뜻한 이야기입니다."),
        BookSearchResult("구름빵", "백희나", "한솔수북", "2004-10", "9788953531235", "동화", "https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&q=80&w=200", "비 오는 아침, 구름으로 만든 빵을 먹고 하늘로 두둥실 날아올라 아침을 굶고 출근한 아빠에게 빵을 전해주는 감동적인 동화입니다."),
        BookSearchResult("강아지똥", "권정생", "길벗어린이", "1996-04", "9788986621136", "동화", "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=200", "아무짝에도 쓸모없다고 천대받던 강아지똥이 아름다운 민들레 꽃을 피우기 위해 자신의 온 몸을 바치는 눈물겨운 사랑과 희생의 명작입니다."),
        BookSearchResult("누가 내 머리에 똥 쌌어?", "베르너 홀츠바르트", "사계절", "1993-01", "9788971966563", "동화", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&q=80&w=200", "머리에 똥을 맞은 꼬마 두더지가 범인을 찾아 나서는 재미있는 유머러스 탐정 이야기로, 아이들에게 배변에 대한 흥미를 주는 전설적인 그림책입니다."),
        BookSearchResult("마당을 나온 암탉", "황선미", "사계절", "2000-05", "9788971968710", "동화", "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&q=80&w=200", "양계장을 탈출해 야생에서 청둥오리 새끼를 지극정성으로 키워내는 암탉 잎싹의 고결하고도 숭고한 모성애와 자유를 향한 눈부신 여정입니다."),
        BookSearchResult("설민석의 한국사 대모험 1", "설민석", "아이휴먼", "2017-01", "9791195977932", "역사", "https://images.unsplash.com/photo-1474932430478-367db2683cec?auto=format&fit=crop&q=80&w=200", "흥미진진한 만화 속 모험을 통해 초등학생이 꼭 알아야 할 필수 한국사 핵심 인물과 유적을 자연스럽고 재미있게 배우는 최고의 역사 학습만화입니다."),
        BookSearchResult("용선생의 시끌벅적 한국사 1", "우지현", "사회평론어린이", "2012-12", "9788964356401", "역사", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&q=80&w=200", "초등 역사 최고 권위의 입체식 해설서로, 개성 넘치는 캐릭터들과 함께 역사적 배경과 인과관계를 스토리텔링으로 완벽하게 이해하게 도와줍니다."),
        BookSearchResult("Why? 과학 공룡", "허순봉", "예림당", "2009-06", "9788930231367", "과학", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&q=80&w=200", "지구 최고의 포식자였던 공룡들의 탄생과 종류, 멸종의 비밀까지 흥미로운 삽화와 탄탄한 초등 과학 교과 연계 스토리를 통해 밝혀냅니다."),
        BookSearchResult("이상한 과자 가게 전천당 1", "히로시마 레이코", "길벗스쿨", "2019-07", "9791164060894", "동화", "https://images.unsplash.com/photo-1509021436665-8f37df706533?auto=format&fit=crop&q=80&w=200", "행운을 가진 사람에게만 나타나는 신비한 과자가게 전천당의 주인이 고민을 해결해주는 마법 같은 간식을 파는 기묘하고 환상적인 판타지 시리즈입니다."),
        BookSearchResult("어린 왕자", "생텍쥐페리", "더스토리", "2015-09", "9791158430344", "문학", "https://images.unsplash.com/photo-1495640388908-05fa85288e61?auto=format&fit=crop&q=80&w=200", "사막에 불시착한 조종사가 우주 여행을 하던 순수한 영혼의 어린 왕자를 만나 세상을 살아가는 참된 가치와 우정, 사랑을 배워가는 인류의 영원한 고전입니다."),
        BookSearchResult("소공녀", "프랜시스 호지슨 버넷", "계림북스", "2005-12", "9788953538425", "동화", "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=200", "부유한 특별 대우생에서 하루아침에 다락방 하녀로 전락한 세라가 고결한 품위와 따뜻한 마음을 잃지 않고 역경을 극복하는 감동 명작입니다."),
        BookSearchResult("소공자", "프랜시스 호지슨 버넷", "웅진씽크하우스", "2007-06", "9788901067346", "동화", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=200", "뉴욕의 가난한 소년 세드릭이 완고하고 차가운 영국의 할아버지 에를 백작을 만나, 천사 같은 순수함과 사랑으로 마음을 변화시키는 이야기입니다."),
        BookSearchResult("비밀의 화원", "프랜시스 호지슨 버넷", "시공주니어", "2010-03", "9788952757531", "동화", "https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&q=80&w=200", "고집스럽고 외로운 소녀 메리가 버려진 비밀의 정원을 발견하고, 자연과 친구들의 사랑 속에서 몸과 마음의 건강과 행복을 되찾아가는 마법 같은 이야기입니다."),
        BookSearchResult("해리 포터와 마법사의 돌 1", "J.K. 롤링", "문학수첩", "2019-11", "9788914022417", "동화", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=200", "자신이 마법사임을 알게 된 해리 포터가 호그와트 마법학교에 입학하여 친구들과 함께 어둠의 마법을 마주하며 펼치는 위대한 마법 판타지의 서막입니다."),
        BookSearchResult("백설공주", "그림 형제", "비룡소", "2011-04", "9788949110011", "동화", "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&q=80&w=200", "질투 많은 왕비를 피해 숲속 일곱 난쟁이들과 살아가던 백설공주가 독사과를 먹고 잠들었다가 왕자님의 진실한 사랑의 힘으로 깨어나는 세계적인 전래 동화입니다."),
        BookSearchResult("신데렐라", "샤를 페로", "비룡소", "2013-05", "9788949110035", "동화", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&q=80&w=200", "계모와 언니들의 구박 속에서도 희망을 잃지 않던 신데렐라가 요정 대모의 마법 도움으로 무도회에 참석해 유리구두 한 짝을 남겨두고 온 로맨틱 동화입니다.")
    )

    fun getLocalFallbackResults(query: String): List<BookSearchResult> {
        if (query.isBlank()) return emptyList()
        val lower = query.lowercase().trim()
        return fallbackBooks.filter { 
            it.title.lowercase().contains(lower) || 
            it.author.lowercase().contains(lower) || 
            it.category.lowercase().contains(lower) ||
            it.description.lowercase().contains(lower)
        }.take(4)
    }

    /**
     * Search books via Google Books API with explicit API key authentication
     */
    suspend fun searchGoogleBooks(
        query: String,
        startTime: Long = System.currentTimeMillis(),
        timeoutMs: Long = 10000L
    ): List<BookSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<BookSearchResult>()
        try {
            val remainingTime = timeoutMs - (System.currentTimeMillis() - startTime)
            if (remainingTime <= 500) {
                Log.w(TAG, "searchGoogleBooks: No time remaining ($remainingTime ms). Skipping.")
                return@withContext emptyList()
            }

            val customClient = client.newBuilder()
                .connectTimeout(remainingTime, TimeUnit.MILLISECONDS)
                .readTimeout(remainingTime, TimeUnit.MILLISECONDS)
                .writeTimeout(remainingTime, TimeUnit.MILLISECONDS)
                .build()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
            // [요구사항 1] 구글 북스 API 키/권한 분리
            val url = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=30&key=$apiKey"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()

            customClient.newCall(request).execute().use { response ->
                println("GOOGLE BOOKS RESPONSE CODE: ${response.code}")
                Log.d(TAG, "searchGoogleBooks response code: ${response.code}")
                
                // [요구사항 1] 단순히 빈 리스트를 리턴하고 끝내지 말고 에러 바디 원문 로그캣 표출 및 예외 처리
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    println("GOOGLE BOOKS RESPONSE FAILED: ${response.code} ${response.message}, ErrorBody: $errorBody")
                    Log.e(TAG, "구글 서버 에러 바디 원문: $errorBody")
                    throw java.io.IOException("Google Books API Error: Code=${response.code}, Message=${response.message}")
                }
                
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                println("GOOGLE BOOKS BODY LENGTH: ${bodyString.length}")
                
                val json = JSONObject(bodyString)
                val items = json.optJSONArray("items")
                if (items == null) {
                    println("GOOGLE BOOKS ITEMS IS NULL")
                    return@withContext emptyList()
                }
                
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val volumeInfo = item.optJSONObject("volumeInfo") ?: continue
                    
                    val title = volumeInfo.optString("title", "알 수 없는 제목")
                    
                    val authorsArray = volumeInfo.optJSONArray("authors")
                    val author = if (authorsArray != null && authorsArray.length() > 0) {
                        val authorList = mutableListOf<String>()
                        for (idx in 0 until authorsArray.length()) {
                            authorList.add(authorsArray.getString(idx))
                        }
                        authorList.joinToString(", ")
                    } else {
                        "알 수 없는 저자"
                    }
                    
                    val publisher = volumeInfo.optString("publisher", "알 수 없는 출판사")
                    val publishDate = volumeInfo.optString("publishedDate", "")
                    
                    // Extract ISBN
                    var isbn = ""
                    val industryIdentifiers = volumeInfo.optJSONArray("industryIdentifiers")
                    if (industryIdentifiers != null) {
                        for (j in 0 until industryIdentifiers.length()) {
                            val idObj = industryIdentifiers.getJSONObject(j)
                            val type = idObj.optString("type")
                            if (type == "ISBN_13") {
                                isbn = idObj.optString("identifier", "")
                                break
                            } else if (type == "ISBN_10" && isbn.isEmpty()) {
                                isbn = idObj.optString("identifier", "")
                            }
                        }
                    }
                    
                    // Cover Image
                    val imageLinks = volumeInfo.optJSONObject("imageLinks")
                    var coverUrl = imageLinks?.optString("thumbnail") ?: imageLinks?.optString("smallThumbnail")
                    if (coverUrl != null && coverUrl.startsWith("http://")) {
                        coverUrl = coverUrl.replace("http://", "https://")
                    }
                    
                    // Parse category dynamically based on Google Book categories
                    val categoriesArray = volumeInfo.optJSONArray("categories")
                    val rawCategory = categoriesArray?.optString(0)?.lowercase() ?: ""
                    val category = mapToKidsCategory(rawCategory, title)
                    
                    val description = volumeInfo.optString("description", "")

                    results.add(
                        BookSearchResult(
                            title = title,
                            author = author,
                            publisher = publisher,
                            publishDate = publishDate,
                            isbn = isbn,
                            category = category,
                            coverUrl = coverUrl,
                            description = description
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("REAL GOOGLE SEARCH EXCEPTION: ${e.message}")
            e.printStackTrace()
            try {
                Log.e(TAG, "Google Books Search Error", e)
            } catch (logEx: Throwable) {
                // Ignore log mocking exception in JUnit tests
            }
            throw e
        }
        return@withContext results
    }

    private fun mapToKidsCategory(rawCategory: String, title: String): String {
        val titleLower = title.lowercase()
        return when {
            rawCategory.contains("fiction") || rawCategory.contains("juvenile fiction") || titleLower.contains("동화") || titleLower.contains("이야기") -> "동화"
            rawCategory.contains("science") || rawCategory.contains("nature") || rawCategory.contains("math") || titleLower.contains("과학") || titleLower.contains("수학") || titleLower.contains("동물") || titleLower.contains("식물") -> "과학"
            rawCategory.contains("history") || rawCategory.contains("biography") || titleLower.contains("역사") || titleLower.contains("위인") || titleLower.contains("삼국유사") -> "역사"
            rawCategory.contains("poetry") || rawCategory.contains("literature") || rawCategory.contains("language") || titleLower.contains("시") || titleLower.contains("문학") || titleLower.contains("한자") -> "문학"
            else -> "기타"
        }
    }

    /**
     * Search books via Gemini API for smart kids catalog resolution.
     */
    suspend fun searchWithGemini(
        query: String,
        startTime: Long = System.currentTimeMillis(),
        timeoutMs: Long = 10000L
    ): List<BookSearchResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or is placeholder.")
            return@withContext emptyList()
        }

        val systemInstruction = """
            당신은 어린이 도서 추천 및 카탈로그 관리 전문가입니다. 
            사용자가 도서 검색어(제목, 저자, 출판사, 키워드, ISBN 등)를 입력하면 해당 도서와 가장 연관성 높은 실제 한국어/글로벌 어린이 도서(유아~초등 고학년 대상) 정보를 최대 15개 검색하여 구조화된 JSON 배열로 반환해야 합니다.
            
            중요 규칙:
            1. 카테고리는 반드시 다음 5개 항목 중 하나로 엄격하게 할당해야 합니다: "동화", "과학", "역사", "문학", "기타".
            2. 반환 포맷은 정확히 JSON 배열 포맷이어야 합니다. 마크다운 기호 없이 순수 JSON만 응답하세요.
        """.trimIndent()

        val prompt = """
            사용자 검색어: "$query"
            
            위 검색어에 부합하는 도서 정보를 최대 15개 정도 아래 JSON 스키마에 맞게 응답해줘.
            스팩에 없는 내용은 상상하지 말고 최대한 실제 정보에 기반해 채워줘.
            
            [
              {
                "title": "책 제목",
                "author": "저자 이름",
                "publisher": "출판사명",
                "publishDate": "출간년도 (예: 2018-05)",
                "isbn": "13자리 ISBN 또는 공백",
                "category": "동화 / 과학 / 역사 / 문학 / 기타 중 하나 택일",
                "description": "어린이와 부모를 위한 책에 대한 간단한 1~2문장 요약 및 추천 포인트"
              }
            ]
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val modelChain = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-3.1-flash-lite")
        val results = mutableListOf<BookSearchResult>()

        for (modelName in modelChain) {
            try {
                val remainingTime = timeoutMs - (System.currentTimeMillis() - startTime)
                if (remainingTime <= 500) {
                    Log.w(TAG, "searchWithGemini [$modelName]: No time remaining ($remainingTime ms). Breaking loop.")
                    break
                }

                val customClient = client.newBuilder()
                    .connectTimeout(remainingTime, TimeUnit.MILLISECONDS)
                    .readTimeout(remainingTime, TimeUnit.MILLISECONDS)
                    .writeTimeout(remainingTime, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                    .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                customClient.newCall(request).execute().use { response ->
                    println("GEMINI API ($modelName) RESPONSE CODE: ${response.code}")
                    
                    if (response.code == 503 || response.code == 429 || response.code == 404) {
                        Log.w(TAG, "[$modelName] 실패. 예비 모델로 즉시 로테이션 우회합니다. Code: ${response.code}")
                        return@use
                    }

                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        println("GEMINI API ($modelName) FAILED: ${response.code} ${response.message}, ErrorBody: $errorBody")
                        Log.e(TAG, "Gemini API ($modelName) Request Failed: Code=${response.code}, Message=${response.message}, ErrorBody=$errorBody")
                        return@use
                    }

                    val bodyString = response.body?.string() ?: return@use
                    println("GEMINI API ($modelName) SUCCESS BODY LENGTH: ${bodyString.length}")
                    
                    val responseJson = try {
                        JSONObject(bodyString)
                    } catch (e: org.json.JSONException) {
                        Log.e(TAG, "Failed to parse Gemini root response JSON for $modelName. Body: $bodyString", e)
                        return@use
                    }

                    val candidates = responseJson.optJSONArray("candidates") ?: return@use
                    val contentObj = candidates.optJSONObject(0)?.optJSONObject("content") ?: return@use
                    val parts = contentObj.optJSONArray("parts") ?: return@use
                    val textResponse = parts.optJSONObject(0)?.optString("text") ?: return@use

                    val bookListJson = try {
                        JSONArray(textResponse)
                    } catch (e: org.json.JSONException) {
                        Log.e(TAG, "Failed to parse book list JSON array from Gemini ($modelName) response text: $textResponse", e)
                        return@use
                    }

                    for (i in 0 until bookListJson.length()) {
                        val bookObj = try {
                            bookListJson.getJSONObject(i)
                        } catch (e: org.json.JSONException) {
                            Log.e(TAG, "Failed to parse individual book object at index $i from $modelName", e)
                            continue
                        }
                        val rawCategory = bookObj.optString("category", "기타")
                        
                        val category = when {
                            rawCategory.contains("동화") -> "동화"
                            rawCategory.contains("과학") -> "과학"
                            rawCategory.contains("역사") -> "역사"
                            rawCategory.contains("문학") -> "문학"
                            else -> "기타"
                        }

                        // 다중 저자 정보가 뭉개지지 않도록 JSONArray와 String 타입을 모두 정밀하게 처리 및 joinToString
                        val authorVal = bookObj.opt("author")
                        val authorStr = when (authorVal) {
                            is JSONArray -> {
                                val list = mutableListOf<String>()
                                for (j in 0 until authorVal.length()) {
                                    list.add(authorVal.optString(j))
                                }
                                list.joinToString(", ")
                            }
                            else -> bookObj.optString("author", "알 수 없는 저자")
                        }

                        results.add(
                            BookSearchResult(
                                title = bookObj.optString("title", "알 수 없는 제목"),
                                author = authorStr,
                                publisher = bookObj.optString("publisher", "알 수 없는 출판사"),
                                publishDate = bookObj.optString("publishDate", ""),
                                isbn = bookObj.optString("isbn", ""),
                                category = category,
                                coverUrl = null,
                                description = bookObj.optString("description", "")
                            )
                        )
                    }
                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                try {
                    Log.e(TAG, "[$modelName] 통신 중 돌발 예외 발생. 다음 예비 체인으로 우회합니다.", e)
                } catch (logEx: Throwable) {
                    // Ignore log mocking exception in JUnit tests
                }
            }
        }
        return@withContext emptyList()
    }

    /**
     * [요구사항 4] 통합 검색 및 에러 자동 폴백(Fallback) 파이프라인 함수
     */
    suspend fun performUnifiedSearch(query: String, searchMode: String = "ALL"): List<BookSearchResult> {
        Log.d(TAG, "performUnifiedSearch query: $query, mode: $searchMode")
        
        val startTime = System.currentTimeMillis()
        
        if (searchMode == "AI") {
            val results = mutableListOf<BookSearchResult>()
            val timeoutMs = 10000L // AI 검색은 10초 타임아웃
            try {
                kotlinx.coroutines.withTimeout(timeoutMs) {
                    val geminiRes = searchWithGemini(query, startTime = startTime, timeoutMs = timeoutMs)
                    results.addAll(geminiRes)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "performUnifiedSearch [AI Mode] timed out after ${timeoutMs}ms. Returning collected results so far: ${results.size}")
            } catch (e: Exception) {
                Log.e(TAG, "performUnifiedSearch: Gemini AI search failed.", e)
            }
            
            return if (results.isNotEmpty()) {
                results
            } else {
                Log.w(TAG, "performUnifiedSearch [AI Mode] returned empty results. Using local fallback.")
                getLocalFallbackResults(query)
            }
        }
        
        // ALL Mode (도서 검색): 5초 타임아웃
        val timeoutMs = 5000L
        val combinedResults = mutableListOf<BookSearchResult>()
        
        try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                // 1. Google Books API 검색 시도
                val googleResults = try {
                    searchGoogleBooks(query, startTime, timeoutMs)
                } catch (e: Exception) {
                    Log.e(TAG, "performUnifiedSearch: Google Books API call failed. Proceeding with fallback pipeline.", e)
                    emptyList()
                }
                combinedResults.addAll(googleResults)
                
                // 2. 만약 결과가 30개 미만이면, Google Books 결과와 Gemini AI 결과를 결합하여 최대 30개에 수렴시킴
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = timeoutMs - elapsed
                
                if (combinedResults.size < 30 && remaining > 500) {
                    Log.d(TAG, "performUnifiedSearch: Google Books returned ${combinedResults.size} results (< 30). Triggering Gemini AI to supplement. Remaining budget: $remaining ms.")
                    val geminiResults = try {
                        searchWithGemini(query, startTime, timeoutMs)
                    } catch (e: Exception) {
                        Log.e(TAG, "performUnifiedSearch: Gemini supplement search failed or timed out.", e)
                        emptyList()
                    }
                    
                    val processedTitles = combinedResults.map { it.title.trim().lowercase() }.toMutableSet()
                    val processedIsbns = combinedResults.map { it.isbn.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                    
                    for (book in geminiResults) {
                        if (combinedResults.size >= 30) break
                        val titleKey = book.title.trim().lowercase()
                        val isbnKey = book.isbn.trim()
                        
                        val isDuplicateTitle = processedTitles.contains(titleKey)
                        val isDuplicateIsbn = isbnKey.isNotEmpty() && processedIsbns.contains(isbnKey)
                        
                        if (!isDuplicateTitle && !isDuplicateIsbn) {
                            combinedResults.add(book)
                            processedTitles.add(titleKey)
                            if (isbnKey.isNotEmpty()) {
                                processedIsbns.add(isbnKey)
                            }
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "performUnifiedSearch [ALL Mode] timed out after ${timeoutMs}ms. Returning collected results so far: ${combinedResults.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performUnifiedSearch: Search failed.", e)
        }
        
        if (combinedResults.isNotEmpty()) {
            Log.d(TAG, "performUnifiedSearch: Returning ${combinedResults.size} combined results.")
            return combinedResults.take(30)
        }
        
        // 3. 둘 다 없거나 통신 장애 시 최후의 보루인 로컬 데이터셋으로 Fallback
        Log.w(TAG, "performUnifiedSearch: Both APIs failed or returned empty. Falling back to Local Fallback Dataset.")
        return getLocalFallbackResults(query)
    }
}
