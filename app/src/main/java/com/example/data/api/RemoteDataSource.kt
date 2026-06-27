package com.example.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object RemoteDataSource {
    private const val TAG = "RemoteDataSource"
    
    val baseClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun buildCustomClient(timeoutMs: Long): OkHttpClient {
        return baseClient.newBuilder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Executes a GET request and returns the response body string.
     * Throws IOException on error.
     */
    @Throws(IOException::class)
    fun get(url: String, client: OkHttpClient): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            Log.d(TAG, "GET Request code: $code to url: $url")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "GET Request Failed: Code=$code, ErrorBody=$errorBody")
                throw IOException("Google Books API Error: Code=$code, Message=${response.message}")
            }
            return response.body?.string() ?: ""
        }
    }

    /**
     * Executes a POST request and returns the response body string.
     * Throws IOException on failure.
     */
    @Throws(IOException::class)
    fun post(url: String, requestBody: RequestBody, client: OkHttpClient, modelName: String): String {
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            Log.d(TAG, "POST Request code: $code for model: $modelName")
            
            if (code == 503 || code == 429 || code == 404) {
                Log.w(TAG, "[$modelName] 실패 (Status $code). 예비 모델로 즉시 로테이션 우회합니다.")
                throw IOException("Retryable failure: Code=$code")
            }

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Gemini API ($modelName) Request Failed: Code=$code, ErrorBody=$errorBody")
                throw IOException("Gemini API Error: Code=$code, Message=${response.message}")
            }
            return response.body?.string() ?: ""
        }
    }
}
