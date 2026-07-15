package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GroqApiService {

    private val effectiveApiKey: String
        get() = BuildConfig.GROQ_API_KEY

    suspend fun getRoast(
        completedCount: Int,
        totalCount: Int,
        missedHabits: Int,
        streak: Int
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Productivity Stats:
                    - Today's Completed: $completedCount
                    - Total Tasks Today: $totalCount
                    - Missed Habits: $missedHabits
                    - Current Streak: $streak days
                    
                    Roast the user based on these stats. If they did well, be a bit impressed but still funny.
                    If they did poorly, roast them hard. 
                    Be creative, use modern slang, and keep it under 30 words.
                    Use emojis.
                """.trimIndent()

                val url = URL("https://api.groq.com/openai/v1/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000 // 5 seconds timeout
                connection.readTimeout = 5000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $effectiveApiKey")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("model", "llama-3.3-70b-versatile")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a funny, sarcastic AI productivity coach.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("max_tokens", 100)
                    put("temperature", 0.9)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                    it.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    "Error: Code $responseCode"
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    suspend fun parseTaskDetails(input: String): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Extract the task title and priority from this text: "$input"
                    Rules:
                    1. Priority must be one of: HIGH, MEDIUM, LOW. 
                    2. If not specified, default to MEDIUM.
                    3. Return ONLY a JSON object like {"title": "Task name", "priority": "HIGH"}.
                """.trimIndent()

                val url = URL("https://api.groq.com/openai/v1/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000 // 5 seconds timeout
                connection.readTimeout = 5000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $effectiveApiKey")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("model", "llama-3.3-70b-versatile")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a helpful assistant that parses tasks.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("response_format", JSONObject().apply { put("type", "json_object") })
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                    it.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val content = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    
                    val resultJson = JSONObject(content)
                    Pair(resultJson.getString("title"), resultJson.getString("priority"))
                } else {
                    Pair(input, "MEDIUM")
                }
            } catch (e: Exception) {
                Pair(input, "MEDIUM")
            }
        }
    }
}

