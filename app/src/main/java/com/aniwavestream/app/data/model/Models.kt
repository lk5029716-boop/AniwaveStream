package com.aniwavestream.app.data.model

import androidx.compose.runtime.Immutable
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
data class CharacterListResponse(
    val data: List<CharacterEdgeDto> = emptyList()
)

@Serializable
data class CharacterEdgeDto(
    val character: CharacterDto? = null,
    val role: String? = null
)

@Serializable
data class CharacterDto(
    @SerialName("mal_id") val malId: Int = 0,
    val name: String = "",
    val images: Images? = null,
    val url: String? = null
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
    val posterUrl: String? get() = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl
    val bannerUrl: String? get() = images?.jpg?.largeImageUrl ?: posterUrl
}

@Serializable
data class Images(val jpg: ImageUrls? = null, val webp: ImageUrls? = null)

@Serializable
data class ImageUrls(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_image_url") val smallImageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null
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
@Immutable
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

@Immutable
data class Episode(
    val number: Int,
    val title: String,
    val durationLabel: String,
    /** Public demo stream (not licensed anime content). */
    val streamUrl: String
)

@Immutable
data class ContinueItem(
    val anime: Anime,
    val episode: Int,
    val progressFraction: Float
)

object DemoStreams {
    // Public Big Buck Bunny / open sample media for player demos only.
    private val samples = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    )

    fun forEpisode(animeId: Int, episode: Int): String =
        samples[(animeId + episode) % samples.size]

    fun buildEpisodes(anime: Anime): List<Episode> {
        val count = (anime.episodes ?: 12).coerceIn(1, 24)
        return (1..count).map { n ->
            Episode(
                number = n,
                title = "Episode $n",
                durationLabel = "24m",
                streamUrl = forEpisode(anime.id, n)
            )
        }
    }
}

@Immutable
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

@Immutable
data class Character(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val role: String?
)

fun CharacterEdgeDto.toCharacter(): Character? {
    val c = character ?: return null
    return Character(
        id = c.malId,
        name = c.name,
        imageUrl = c.images?.jpg?.largeImageUrl ?: c.images?.jpg?.imageUrl,
        role = role
    )
}

/** Airing anime for a weekday (Jikan schedules). */
val ScheduleDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

