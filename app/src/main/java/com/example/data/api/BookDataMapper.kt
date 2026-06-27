package com.example.data.api

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object BookDataMapper {
    private const val TAG = "BookDataMapper"

    fun mapToKidsCategory(rawCategory: String, title: String): String {
        val titleLower = title.lowercase()
        return when {
            rawCategory.contains("fiction") || rawCategory.contains("juvenile fiction") || titleLower.contains("동화") || titleLower.contains("이야기") -> "동화"
            rawCategory.contains("science") || rawCategory.contains("nature") || rawCategory.contains("math") || titleLower.contains("과학") || titleLower.contains("수학") || titleLower.contains("동물") || titleLower.contains("식물") -> "과학"
            rawCategory.contains("history") || rawCategory.contains("biography") || titleLower.contains("역사") || titleLower.contains("위인") || titleLower.contains("삼국유사") -> "역사"
            rawCategory.contains("poetry") || rawCategory.contains("literature") || rawCategory.contains("language") || titleLower.contains("시") || titleLower.contains("문학") || titleLower.contains("한자") -> "문학"
            else -> "기타"
        }
    }

    fun parseGoogleBooksResponse(bodyString: String): List<BookDto> {
        val results = mutableListOf<BookDto>()
        try {
            val json = JSONObject(bodyString)
            val items = json.optJSONArray("items") ?: return emptyList()
            
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
                    BookDto(
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Google Books response JSON", e)
        }
        return results
    }

    fun parseGeminiResponse(bodyString: String, modelName: String): List<BookDto> {
        val results = mutableListOf<BookDto>()
        try {
            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates") ?: return emptyList()
            val contentObj = candidates.optJSONObject(0)?.optJSONObject("content") ?: return emptyList()
            val parts = contentObj.optJSONArray("parts") ?: return emptyList()
            val textResponse = parts.optJSONObject(0)?.optString("text") ?: return emptyList()

            val bookListJson = JSONArray(textResponse)
            for (i in 0 until bookListJson.length()) {
                val bookObj = bookListJson.getJSONObject(i)
                val rawCategory = bookObj.optString("category", "기타")
                
                val category = when {
                    rawCategory.contains("동화") -> "동화"
                    rawCategory.contains("과학") -> "과학"
                    rawCategory.contains("역사") -> "역사"
                    rawCategory.contains("문학") -> "문학"
                    else -> "기타"
                }

                // Handle multi-author and String types
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
                    BookDto(
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini ($modelName) response JSON", e)
        }
        return results
    }
}
