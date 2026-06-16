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

    data class GenerationResult(
        val title: String,
        val description: String,
        val content: String,
        val category: String,
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

    suspend fun autoGeneratePrompt(
        userInput: String,
        mode: String, // "generate" or "improve"
        existingCategories: List<String>
    ): GenerationResult? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            Log.e(TAG, "GEMINI_API_KEY not found in BuildConfig", e)
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder!")
            return@withContext GenerationResult(
                title = "AI Gen Offline",
                description = "Placeholder for local draft",
                content = "AI Generation is offline because the Gemini API Key is missing. Try inserting your API key in the Secrets panel inside AI Studio.",
                category = "General",
                explanation = "Authentication missing."
            )
        }

        val categoriesListStr = existingCategories.joinToString(", ")

        val prompt = if (mode == "improve") {
            """
                Optimize and improve the following user draft prompt to make it a professional, high-performing AI system prompt.
                Focus on:
                1. Adding role/persona definitions and clear structural context.
                2. Defining explicit variables or placeholders using square brackets (e.g. [insert text here]).
                3. Listing step-by-step rules and formatting guidelines for the model.
                
                Existing Draft Prompt to Improve:
                ${userInput}

                In addition, suggest:
                1. A matching short, punchy category name. Try to choose from these existing categories if applicable: [${categoriesListStr}]. If none are suitable, suggest a new suitable capitalized category.
                2. A catchy, clean Title.
                3. A very short reference description of this improved prompt.
                4. A brief explanation of the improvements made to this prompt.

                You MUST return a JSON object in exactly the following format (no other text, markers, or markdown tags like ```json around it):
                {
                  "title": "Improved Catchy Title",
                  "description": "Short explanation of the prompt purpose.",
                  "content": "Fully-optimized, deep-structured prompt body with variables like [topic] etc.",
                  "category": "Selected Category",
                  "explanation": "Brief breakdown of the optimization techniques applied."
                }
            """.trimIndent()
        } else {
            """
                Create a professional, highly effective AI system prompt from scratch based on the following short idea or description.
                Generate:
                1. A comprehensive prompt body with detailed instructions, role/persona, step-by-step logic, and placeholders in square brackets (e.g. [insert text here]) where user input is expected.
                2. A catchy, clean Title.
                3. A very short reference description.
                4. A matching category name. Try to choose from these existing categories if applicable: [${categoriesListStr}]. If none fit, suggest a new capitalized category.
                5. A brief explanation of the concept design.

                User descriptive idea:
                ${userInput}

                You MUST return a JSON object in exactly the following format (no other text, markers, or markdown tags like ```json around it):
                {
                  "title": "Generated Catchy Title",
                  "description": "Short explanation of the prompt purpose.",
                  "content": "Professional, deep-structured prompt body with variables.",
                  "category": "Selected Category",
                  "explanation": "Brief description of how to use this generated prompt."
                }
            """.trimIndent()
        }

        val systemInstruction = "You are an expert AI Prompt Engineer and Taxonomy architect. Always return a valid JSON object containing exactly 'title', 'description', 'content', 'category', and 'explanation' fields."

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
                put("temperature", 0.7)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${apiKey}")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Request failed: Code=${response.code}, Body=${errorBody}")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Raw Response: ${responseBodyStr}")

                val rootObj = JSONObject(responseBodyStr)
                val candidates = rootObj.getJSONArray("candidates")
                if (candidates.length() == 0) return@withContext null

                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.getJSONObject("content")
                val parts = contentObj.getJSONArray("parts")
                if (parts.length() == 0) return@withContext null

                val responseText = parts.getJSONObject(0).getString("text")
                Log.d(TAG, "Extracted JSON text: ${responseText}")

                val resultObj = JSONObject(responseText.trim())
                GenerationResult(
                    title = resultObj.optString("title", "Generated Prompt"),
                    description = resultObj.optString("description", "A prompt generated by Gemini"),
                    content = resultObj.optString("content", ""),
                    category = resultObj.optString("category", "General"),
                    explanation = resultObj.optString("explanation", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing autoGeneratePrompt API call", e)
            null
        }
    }
}
