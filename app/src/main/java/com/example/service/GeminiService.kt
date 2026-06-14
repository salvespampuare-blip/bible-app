package com.example.service

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// ============================================================================
// Moshi-Compatible Request/Response Classes for Gemini REST API
// ============================================================================

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// Helper models for parsed Semantic Search results
@JsonClass(generateAdapter = true)
data class SemanticVerseResult(
    val book: String,
    val chapter: Int,
    val verseNum: Int,
    val text: String,
    val relevanceReason: String
)

@JsonClass(generateAdapter = true)
data class RemoteVerse(
    val verse: Int,
    val text: String
)

// ============================================================================
// Retrofit Client & API Definitions
// ============================================================================

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: GeminiApi by lazy {
        retrofit.create(GeminiApi::class.java)
    }

    /**
     * Helper to run queries on Gemini 3.5-flash
     */
    suspend fun queryGemini(prompt: String, systemPrompt: String? = null, isJson: Boolean = false): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return "ERROR_MISSING_API_KEY"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = if (isJson) GenerationConfig(responseMimeType = "application/json", temperature = 0.2f) else null,
            systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) }
        )

        val response = api.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
            ?: throw Exception("No response returned from the AI model.")
    }

    /**
     * Parses semantic search JSON array response
     */
    fun parseSemanticSearchResults(jsonText: String): List<SemanticVerseResult> {
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SemanticVerseResult::class.java)
            val adapter = moshi.adapter<List<SemanticVerseResult>>(type)
            return adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Parses remote verses fetched on demand
     */
    fun parseRemoteVerses(jsonText: String): List<RemoteVerse> {
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, RemoteVerse::class.java)
            val adapter = moshi.adapter<List<RemoteVerse>>(type)
            return adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
