package com.example.todolist

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateSuggestions(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ---------- REQUEST ----------

data class GeminiRequest(
    val contents: List<Content>,
    val safetySettings: List<SafetySetting>? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class SafetySetting(
    val category: String,
    val threshold: String
)

// ---------- RESPONSE ----------

data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content? = null
)
