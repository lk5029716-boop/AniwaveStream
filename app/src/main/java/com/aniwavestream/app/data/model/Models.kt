package com.aniwavestream.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeListResponse(
    val data: List<AnimeDto> = emptyList(),
    val pagination: Pagination? = null
)

@Serializable
data class AnimeDetailResponse(
    val data: AnimeDto
)

@Serializable
data class Pagination(
    @SerialName("last_visible_page") val lastVisiblePage: Int = 1,
    @SerialName("has_next_page") val hasNextPage: Boolean = false
)

@Serializable
data class AnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    val synopsis: String? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val score: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val year: Int? = null,
    val season: String? = null,
    val rating: String? = null,
    val duration: String? = null,
    val images: Images? = null,
    val trailer: Trailer? = null,
    val genres: List<NamedEntity> = emptyList(),
    val studios: List<NamedEntity> = emptyList(),
    @SerialName("aired") val aired: Aired? = null
) {
    val displayTitle: String get() = titleEnglish?.takeIf { it.isNotBlank() } ?: title
    val posterUrl: String? get() = images?.webp?.largeImageUrl ?: images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl
    val posterThumbUrl: String? get() = images?.webp?.smallImageUrl ?: images?.jpg?.smallImageUrl ?: posterUrl
    val bannerUrl: String? get() = images?.jpg?.largeImageUrl ?: posterUrl
}

@Serializable
data class Images(val jpg: ImageUrls? = null, val webp: ImageUrls? = null)

@Serializable
data class ImageUrls(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_image_url") val smallImageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
    @SerialName("medium_image_url") val mediumImageUrl: String? = null
)

@Serializable
data class Trailer(
    @SerialName("youtube_id") val youtubeId: String? = null,
    val url: String? = null,
    @SerialName("embed_url") val embedUrl: String? = null
)

@Serializable
data class NamedEntity(
    @SerialName("mal_id") val malId: Int = 0,
    val name: String = ""
)

@Serializable
data class Aired(val string: String? = null)

/** In-app domain models */
data class Anime(
    val id: Int,
    val title: String,
    val synopsis: String,
    val posterUrl: String?,
    val bannerUrl: String?,
    val score: Double?,
    val episodes: Int?,
    val year: Int?,
    val type: String?,
    val status: String?,
    val rating: String?,
    val genres: List<String>,
    val studios: List<String>
)

fun AnimeDto.toAnime() = Anime(
    id = malId,
    title = displayTitle,
    synopsis = synopsis.orEmpty(),
    posterUrl = posterUrl,
    bannerUrl = bannerUrl,
    score = score,
    episodes = episodes,
    year = year,
    type = type,
    status = status,
    rating = rating,
    genres = genres.map { it.name },
    studios = studios.map { it.name }
)

data class ContinueItem(
    val anime: Anime,
    val episode: Int,
    val progressFraction: Float
)

data class GenreChip(val id: Int, val name: String)

val BrowseGenres = listOf(
    GenreChip(1, "Action"),
    GenreChip(2, "Adventure"),
    GenreChip(4, "Comedy"),
    GenreChip(8, "Drama"),
    GenreChip(10, "Fantasy"),
    GenreChip(14, "Horror"),
    GenreChip(7, "Mystery"),
    GenreChip(22, "Romance"),
    GenreChip(24, "Sci-Fi"),
    GenreChip(36, "Slice of Life"),
    GenreChip(30, "Sports"),
    GenreChip(37, "Supernatural")
)

/** Weekly schedule day */
enum class ScheduleDay(val displayName: String, val jikanParam: String) {
    MONDAY("Mon", "monday"),
    TUESDAY("Tue", "tuesday"),
    WEDNESDAY("Wed", "wednesday"),
    THURSDAY("Thu", "thursday"),
    FRIDAY("Fri", "friday"),
    SATURDAY("Sat", "saturday"),
    SUNDAY("Sun", "sunday");

    companion object {
        fun today(): ScheduleDay {
            val cal = java.util.Calendar.getInstance()
            return values()[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }
    }
}
