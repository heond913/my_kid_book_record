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
        BookSearchResult("강아지똥", "권정생", "길벗어린이", "1996-04", "9788986621136", "동화", "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=200", "아무짝에도 쓸모없다고 천대받던 강아지똥이 아름다운 민들레 꽃을 피우기 위해 자신의 온 몸을 바치는 눈물겨운 사랑และ 희생의 명작입니다."),
        BookSearchResult("누가 내 머리에 똥 쌌어?", "베르너 홀츠바르트", "사계절", "1993-01", "9788971966563", "동화", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&q=80&w=200", "머리에 똥을 맞은 꼬마 두더지가 범인을 찾아 나서는 재미있는 유머러스 탐정 이야기로, 아이들에게 배변에 대한 흥미를 주는 전설적인 그림책입니다."),
        BookSearchResult("마당을 나온 암탉", "황선미", "사계절", "2000-05", "9788971968710", "동화", "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&q=80&w=200", "양계장을 탈출해 야생에서 청둥오리 새끼를 지극정성으로 키워내는 암탉 잎싹의 고결하고도 숭고한 모성애와 자유를 향한 눈부신 여정입니다."),
        BookSearchResult("설민석의 한국사 대모험 1", "설민석", "아이휴먼", "2017-01", "9791195977932", "역사", "https://images.unsplash.com/photo-1474932430478-367db2683cec?auto=format&fit=crop&q=80&w=200", "흥미진진한 만화 속 모험을 통해 초등학생이 꼭 알아야 할 필수 한국사 핵심 인물과 유적을 자연스럽고 재미있게 배우는 최고의 역사 학습만화입니다."),
        BookSearchResult("용선생의 시끌벅적 한국사 1", "우지현", "사회평론어린이", "2012-12", "9788964356401", "역사", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&q=80&w=200", "초등 역사 최고 권위의 입체식 해설서로, 개성 넘치는 캐릭터들과 함께 역사적 배경과 인과관계를 스토리텔링으로 완벽하게 이해하게 도와줍니다."),
        BookSearchResult("Why? 과학 공룡", "허순봉", "예림당", "2009-06", "9788930231367", "과학", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&q=80&w=200", "지구 최고의 포식자였던 공룡들의 탄생과 종류, 멸종의 비밀까지 흥미로운 삽화와 탄탄한 초등 과학 교과 연계 스토리를 통해 밝혀냅니다."),
        BookSearchResult("이상한 과자 가게 전천당 1", "히로시마 레이코", "길벗스쿨", "2019-07", "9791164060894", "동화", "https://images.unsplash.com/photo-1509021436665-8f37df706533?auto=format&fit=crop&q=80&w=200", "행운을 가진 사람에게만 나타나는 신비한 과자가게 전천당의 주인이 고민을 해결해주는 마법 같은 간식을 파는 기묘하고 환상적인 판타지 시리즈입니다."),
        BookSearchResult("어린 왕자", "생텍쥐페리", "더스토리", "2015-09", "9791158430344", "문학", "https://images.unsplash.com/photo-1495640388908-05fa85288e61?auto=format&fit=crop&q=80&w=200", "사막에 불시착한 조종사가 우주 여행을 하던 순수한 영혼의 어린 왕자를 만나 세상을 살아가는 참된 가치와 우정, 사랑을 배워가는 인류의 영원한 고전입니다.")
    )

    fun getLocalFallbackResults(query: String): List<BookSearchResult> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase().trim()
        val matched = fallbackBooks.filter { book ->
            book.title.lowercase().contains(lowerQuery) ||
            book.author.lowercase().contains(lowerQuery) ||
            book.publisher.lowercase().contains(lowerQuery) ||
            book.category.lowercase().contains(lowerQuery) ||
            book.description.lowercase().contains(lowerQuery)
        }
        
        if (matched.isNotEmpty()) {
            return matched
        }
        
        return when {
            lowerQuery.contains("공룡") || lowerQuery.contains("디노") -> {
                fallbackBooks.filter { it.category == "과학" || it.description.contains("공룡") }
            }
            lowerQuery.contains("역사") || lowerQuery.contains("한국") || lowerQuery.contains("대모험") -> {
                fallbackBooks.filter { it.category == "역사" }
            }
            lowerQuery.contains("동화") || lowerQuery.contains("그림") || lowerQuery.contains("상상") || lowerQuery.contains("빵") || lowerQuery.contains("사탕") -> {
                fallbackBooks.filter { it.category == "동화" }
            }
            lowerQuery.contains("책") || lowerQuery.contains("추천") || lowerQuery.contains("아이") || lowerQuery.contains("조카") -> {
                fallbackBooks.take(5)
            }
            else -> {
                fallbackBooks.filter { 
                    it.title.contains(lowerQuery.take(1)) || it.author.contains(lowerQuery.take(1)) 
                }.ifEmpty { fallbackBooks.take(4) }
            }
        }
    }

    /**
     * Search books via Google Books API
     */
    suspend fun searchGoogleBooks(query: String): List<BookSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<BookSearchResult>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=8"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "searchGoogleBooks response code: ${response.code}")
                if (!response.isSuccessful) {
                    Log.e(TAG, "searchGoogleBooks failed: ${response.code} ${response.message}")
                    return@withContext emptyList()
                }
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyString)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val volumeInfo = item.optJSONObject("volumeInfo") ?: continue
                    
                    val title = volumeInfo.optString("title", "알 수 없는 제목")
                    
                    val authorsArray = volumeInfo.optJSONArray("authors")
                    val author = if (authorsArray != null && authorsArray.length() > 0) {
                        authorsArray.getString(0)
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
            Log.e(TAG, "Google Books Search Error", e)
        }
        return@withContext results
    }

    private fun mapToKidsCategory(rawCategory: String, title: String): String {
        // Simple heuristic map to standard children's categories: "동화", "과학", "역사", "문학", "기타"
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
     * Search books via Gemini API for smart kids catalog resolution (Excellent for Korean Children's Books)
     */
    suspend fun searchWithGemini(query: String): List<BookSearchResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or is placeholder.")
            return@withContext emptyList()
        }

        val results = mutableListOf<BookSearchResult>()
        try {
            val systemInstruction = """
                당신은 어린이 도서 추천 및 카탈로그 관리 전문가입니다. 
                사용자가 도서 검색어(제목, 저자, 출판사, 키워드, ISBN 등)를 입력하면 해당 도서와 가장 연관성 높은 실제 한국어/글로벌 어린이 도서(유아~초등 고학년 대상) 정보를 최대 5개 검색하여 구조화된 JSON 배열로 반환해야 합니다.
                
                중요 규칙:
                1. 카테고리는 반드시 다음 5개 항목 중 하나로 엄격하게 할당해야 합니다: "동화", "과학", "역사", "문학", "기타".
                2. 반환 포맷은 정확히 JSON 배열 포맷이어야 합니다. 마크다운 기호 없이 순수 JSON만 응답하세요.
            """.trimIndent()

            val prompt = """
                사용자 검색어: "$query"
                
                위 검색어에 부합하는 도서 2~3개 정보를 아래 JSON 스키마에 맞게 응답해줘.
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

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API Request Failed: ${response.code} ${response.message}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates") ?: return@withContext emptyList()
                val contentObj = candidates.optJSONObject(0)?.optJSONObject("content") ?: return@withContext emptyList()
                val parts = contentObj.optJSONArray("parts") ?: return@withContext emptyList()
                val textResponse = parts.optJSONObject(0)?.optString("text") ?: return@withContext emptyList()

                // Parse the inner JSON Array returned by Gemini
                val bookListJson = JSONArray(textResponse)
                for (i in 0 until bookListJson.length()) {
                    val bookObj = bookListJson.getJSONObject(i)
                    val rawCategory = bookObj.optString("category", "기타")
                    
                    // Normalize category to our standard set
                    val category = when {
                        rawCategory.contains("동화") -> "동화"
                        rawCategory.contains("과학") -> "과학"
                        rawCategory.contains("역사") -> "역사"
                        rawCategory.contains("문학") -> "문학"
                        else -> "기타"
                    }

                    results.add(
                        BookSearchResult(
                            title = bookObj.optString("title", "알 수 없는 제목"),
                            author = bookObj.optString("author", "알 수 없는 저자"),
                            publisher = bookObj.optString("publisher", "알 수 없는 출판사"),
                            publishDate = bookObj.optString("publishDate", ""),
                            isbn = bookObj.optString("isbn", ""),
                            category = category,
                            coverUrl = null, // Will use placeholder or we can query search online
                            description = bookObj.optString("description", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Catalog Search Error", e)
        }
        return@withContext results
    }
}
