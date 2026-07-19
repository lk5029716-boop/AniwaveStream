package com.aniwavestream.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Real playback backend (Aniwavesapi). Resolves aniwaves.ru streams.
 *
 * IMPORTANT: the backend serves JSON ONLY under the `/api/` prefix. The bare
 * paths (`/search`, `/servers`, `/stream`) return an HTML "Endpoint Tester"
 * page, which makes Retrofit's JSON converter throw and the player fail on
 * every single episode. Always hit the api-prefixed paths.
 */
interface AniwavesApi {

    @GET("api/search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("api/servers")
    suspend fun servers(
        @Query("episodeId") episodeId: String,
        @Query("type") type: String = "sub"
    ): ServersResponse

    @GET("api/stream")
    suspend fun stream(@Query("serverId") serverId: String): StreamResponse

    companion object {
        const val BASE = "https://aniwavesapis.onrender.com/"

        fun create(): AniwavesApi {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AniwavesApi::class.java)
        }
    }
}

@Serializable
data class SearchResponse(
    val results: List<SearchResult> = emptyList()
)

@Serializable
data class SearchResult(
    val id: String = "",          // slug, e.g. "naruto-76396"
    val title: String = "",
    val poster: String = "",
    val type: String = "",
    val episodes: EpisodeCounts = EpisodeCounts()
)

@Serializable
data class EpisodeCounts(
    val sub: Int = 0,
    val dub: Int = 0
)

@Serializable
data class ServersResponse(
    val servers: List<Server> = emptyList()
)

@Serializable
data class Server(
    val id: String = "",          // base64 server link id, opaque to the client
    val name: String = "",        // "Vidplay" | "BYFMS" | "DGHG"
    val type: String = "sub"
)

@Serializable
data class StreamResponse(
    val m3u8: String? = null,
    val proxiedM3u8: String? = null,
    val type: String? = null,
    val provider: String? = null,
    val error: String? = null
)
