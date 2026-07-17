package com.aniwavestream.app.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import com.aniwavestream.app.data.repository.RateLimitException
import java.util.concurrent.TimeUnit

/**
 * Minimal AniList GraphQL client (https://graphql.anilist.co).
 *
 * No auth required for public media queries. We use a plain OkHttp POST (not
 * Retrofit) because GraphQL is a single endpoint with a JSON body — this keeps
 * the dependency surface small and reuses the same OkHttp/serialization stack
 * the app already ships.
 */
object AniListApi {
    private const val BASE = "https://graphql.anilist.co"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "AniwaveStream/1.0 (Android educational demo)")
                .build()
            chain.proceed(req)
        }
        .build()

    /** POST a GraphQL [query] with variables built via [buildVars]. */
    fun query(query: String, buildVars: VariableBuilder.() -> Unit = {}): String {
        val varsJson = buildJsonObject { VariableBuilder(this).apply(buildVars) }
        val body = buildJsonObject {
            put("query", JsonPrimitive(query))
            put("variables", varsJson)
        }
        val request = Request.Builder()
            .url(BASE)
            .post(json.encodeToString(JsonObject.serializer(), body).toRequestBody("application/json".toMediaType()))
            .build()
        // 429 = rate limited: do NOT hammer the already-exhausted budget. Throw RateLimitException
        // immediately so the repository returns Result.failure(RateLimitException) and the UI can
        // show a "slow down / retry" state. 5xx are transient server errors, so retry those.
        val maxAttempts = 4
        var attempt = 0
        while (true) {
            attempt++
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val code = resp.code
                if (resp.isSuccessful) return text
                Log.w("AniListApi", "HTTP $code on attempt $attempt: ${text.take(200)}")
                if (code == 429) throw RateLimitException("AniList HTTP 429 (attempt $attempt)")
                if (code >= 500 && attempt < maxAttempts) {
                    val retryAfter = resp.headers["Retry-After"]?.toLongOrNull()
                    val waitMs = ((retryAfter ?: 0L) * 1000L).coerceAtLeast(attempt * 1000L).coerceAtMost(8000L)
                    Thread.sleep(waitMs)
                    return@use
                }
                throw RuntimeException("AniList HTTP $code: ${text.take(200)}")
            }
        }
    }

    /** Fluent builder for GraphQL variables backed by a kotlinx [buildJsonObject]. */
    class VariableBuilder(private val obj: kotlinx.serialization.json.JsonObjectBuilder) {
        fun int(key: String, value: Int) = obj.put(key, value)
        fun str(key: String, value: String) = obj.put(key, value)
        fun strOrNull(key: String, value: String?) = value?.let { obj.put(key, it) }
        fun intOrNull(key: String, value: Int?) = value?.let { obj.put(key, it) }
        fun list(key: String, values: List<String>) = obj.putJsonArray(key) {
            values.forEach { add(JsonPrimitive(it)) }
        }
    }
}
