package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class SuggestionResult(
        val category: String,
        val tags: List<String>,
        val explanation: String
    )

    suspend fun suggestCategoryAndTags(
        title: String,
        description: String,
        content: String,
        existingCategories: List<String>
    ): SuggestionResult? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            Log.e(TAG, "GEMINI_API_KEY not found in BuildConfig", e)
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder!")
            return@withContext SuggestionResult(
                category = "General",
                tags = listOf("draft", "uncategorized"),
                explanation = "AI suggest is offline (API Key is blank or placeholder)."
            )
        }

        val categoriesListStr = existingCategories.joinToString(", ")

        val prompt = """
            Analyze the following AI prompt to suggest:
            1. A highly matching 'category'. Pick from the following existing categories if possible: [$categoriesListStr]. If none fit, suggest a new suitable, single-word category (capitalized).
            2. 3 to 5 lowercase descriptive 'tags' (e.g. "outreach", "writing", "blog", "code"). Do not include '#' symbol in the tags array itself.
            3. A short, helpful 'explanation' of why this category and those tags were selected.

            Prompt Details:
            Title: $title
            Description: $description
            Content: $content

            You MUST return a JSON object in exactly the following format (no other text around it):
            {
              "category": "Suggested Category",
              "tags": ["tag1", "tag2", "tag3"],
              "explanation": "Why this category/tags match."
            }
        """.trimIndent()

        val systemInstruction = "You are a specialized taxonomy assistant. Return JSON containing 'category', 'tags' array, and 'explanation' fields."

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Request failed: Code=${response.code}, Body=$errorBody")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Raw Response: $responseBodyStr")

                val rootObj = JSONObject(responseBodyStr)
                val candidates = rootObj.getJSONArray("candidates")
                if (candidates.length() == 0) return@withContext null

                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.getJSONObject("content")
                val parts = contentObj.getJSONArray("parts")
                if (parts.length() == 0) return@withContext null

                val responseText = parts.getJSONObject(0).getString("text")
                Log.d(TAG, "Extracted JSON text: $responseText")

                val resultObj = JSONObject(responseText.trim())
                val category = resultObj.optString("category", "General")
                val tagsArr = resultObj.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) {
                        tagsList.add(tagsArr.getString(i))
                    }
                }
                val explanation = resultObj.optString("explanation", "")

                SuggestionResult(category, tagsList, explanation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing API call", e)
            null
        }
    }
}
