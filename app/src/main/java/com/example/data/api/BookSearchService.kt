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
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
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
