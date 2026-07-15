package com.aniwavestream.app.data.api

import com.aniwavestream.app.data.model.AnimeDetailResponse
import com.aniwavestream.app.data.model.AnimeListResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface JikanApi {
    @GET("top/anime")
    suspend fun topAnime(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("filter") filter: String? = null
    ): AnimeListResponse

    @GET("seasons/now")
    suspend fun seasonalNow(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): AnimeListResponse

    @GET("anime")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sfw") sfw: Boolean = true
    ): AnimeListResponse

    @GET("anime")
    suspend fun byGenre(
        @Query("genres") genreId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("order_by") orderBy: String = "popularity",
        @Query("sort") sort: String = "asc",
        @Query("sfw") sfw: Boolean = true
    ): AnimeListResponse

    @GET("anime/{id}/full")
    suspend fun animeFull(@Path("id") id: Int): AnimeDetailResponse

    @GET("anime/{id}/recommendations")
    suspend fun recommendations(@Path("id") id: Int): AnimeListResponse

    companion object {
        private const val BASE = "https://api.jikan.moe/v4/"

        fun create(): JikanApi {
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
                .addInterceptor { chain ->
                    // Jikan free tier: be polite with User-Agent
                    val req = chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "AniwaveStream/1.0 (Android educational demo)")
                        .build()
                    chain.proceed(req)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(JikanApi::class.java)
        }
    }
}
