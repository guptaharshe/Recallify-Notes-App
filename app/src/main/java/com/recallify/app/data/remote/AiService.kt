package com.recallify.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Response

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    val message: ChatMessage?
)

interface AiApi {
    @POST("chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") token: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

class AiService(private val apiKey: String) {
    
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val api: AiApi = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AiApi::class.java)

    // Using the latest Gemini 2.0 Flash model via OpenRouter
    private val modelName = "meta-llama/llama-3.2-3b-instruct:free"
    private val referer = "https://recallify.app"
    private val title = "Recallify"

    suspend fun summarize(content: String): Result<String?> {
        val request = ChatRequest(
            model = modelName,
            messages = listOf(
                ChatMessage("user", "Summarize the following note content concisely:\n\n$content")
            )
        )
        return executeRequest(request)
    }

    suspend fun generateQuiz(content: String): Result<String?> {
        val request = ChatRequest(
            model = modelName,
            messages = listOf(
                ChatMessage("user", "Generate 3 multiple choice questions based on the following content. Provide the question, 4 options, and the correct answer for each.\n\nContent:\n$content")
            )
        )
        return executeRequest(request)
    }

    suspend fun explainLikeImFive(content: String): Result<String?> {
        val request = ChatRequest(
            model = modelName,
            messages = listOf(
                ChatMessage("user", "Explain the following content like I'm five years old (ELI5):\n\n$content")
            )
        )
        return executeRequest(request)
    }

    private suspend fun executeRequest(request: ChatRequest): Result<String?> {
        return try {
            val response = api.getCompletion(
                token = "Bearer $apiKey",
                referer = referer,
                title = title,
                request = request
            )
            if (response.isSuccessful) {
                val body = response.body()
                val resultText = body?.choices?.firstOrNull()?.message?.content
                Result.success(resultText)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("API Error: ${response.code()}\n$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
