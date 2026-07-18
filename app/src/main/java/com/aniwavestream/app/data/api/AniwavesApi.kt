package com.aniwavestream.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
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
 * The app keys anime by Jikan `mal_id` (Int), but this backend keys by its own
 * slug string (e.g. "naruto-76396"). We therefore resolve the slug from the
 * title via /api/search and cache it per anime id.
 *
 * Flow: title -> /api/search (slug) -> /api/servers (Vidplay/BYFMS/DGHG) ->
 * /api/stream (proxiedM3u8). The proxiedM3u8 path is same-origin to the API
 * base, so we prefix it with the base URL before handing it to ExoPlayer.
 */
interface AniwavesApi {

    @GET("search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("servers")
    suspend fun servers(
        @Query("episodeId") episodeId: String,
        @Query("type") type: String = "sub"
    ): ServersResponse

    @GET("stream")
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
