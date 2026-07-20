package com.aniwavestream.app.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
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
    val posterUrl: String? get() =
        images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl ?: images?.webp?.largeImageUrl ?: images?.webp?.imageUrl
    val bannerUrl: String? get() = posterUrl
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

/* ===================== AniList GraphQL models ===================== */

@Serializable
data class AlResponse(
    val data: AlData? = null,
    val errors: List<AlError>? = null
)

@Serializable
data class AlError(val message: String = "")

@Serializable
data class AlData(
    val Page: AlPage? = null,
    val Media: AlMedia? = null
)

@Serializable
data class AlPage(
    val media: List<AlMedia> = emptyList(),
    val airingSchedules: List<AlAiring> = emptyList(),
    val pageInfo: AlPageInfo? = null
)

@Serializable
data class AlAiring(
    val episode: Int? = null,
    val airingAt: Long? = null,
    val media: AlAiringMedia? = null
)

@Serializable
data class AlAiringMedia(
    val id: Int = 0,
    val title: AlTitle? = null,
    val coverImage: AlCoverImage? = null,
    val episodes: Int? = null,
    val status: String? = null
)

@Serializable
data class AlPageInfo(@SerialName("hasNextPage") val hasNextPage: Boolean = false)

@Serializable
data class AlMedia(
    val id: Int = 0,
    @SerialName("idMal") val idMal: Int? = null,
    val title: AlTitle? = null,
    val coverImage: AlCoverImage? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val seasonYear: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val studios: AlStudioConnection? = null,
    val characters: AlCharacterConnection? = null,
    val recommendations: AlRecommendationConnection? = null
)

@Serializable
data class AlTitle(
    val romaji: String = "",
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AlCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null
)

@Serializable
data class AlStudioConnection(val nodes: List<AlStudio> = emptyList())

@Serializable
data class AlStudio(val name: String = "")

@Serializable
data class AlCharacterConnection(val edges: List<AlCharacterEdge> = emptyList())

@Serializable
data class AlCharacterEdge(
    val role: String? = null,
    val node: AlCharacterNode? = null
)

@Serializable
data class AlCharacterNode(
    val id: Int = 0,
    val name: AlName? = null,
    val image: AlImage? = null
)

@Serializable
data class AlName(val full: String = "")

@Serializable
data class AlImage(val large: String? = null)

@Serializable
data class AlRecommendationConnection(val nodes: List<AlRecommendationNode> = emptyList())

@Serializable
data class AlRecommendationNode(val media: AlMedia? = null)

/** Strip HTML tags from AniList descriptions and fall back to romaji title. */
fun AlMedia.toAnime(): Anime {
    val poster = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium
    val title = this.title?.english ?: this.title?.romaji ?: this.title?.native ?: ""
    val synopsis = description
        ?.replace(Regex("<br\\s*/?>"), "\n")
        ?.replace(Regex("<[^>]*>"), "")
        ?.trim()
        .orEmpty()
    return Anime(
        id = idMal ?: id,
        title = title,
        synopsis = synopsis,
        posterUrl = poster,
        bannerUrl = bannerImage ?: poster,
        score = averageScore?.toDouble()?.div(10.0),
        episodes = episodes,
        year = seasonYear,
        type = format,
        status = status,
        rating = null,
        genres = genres,
        studios = studios?.nodes?.map { it.name } ?: emptyList()
    )
}

fun AlCharacterEdge.toCharacter(): Character? {
    val n = node ?: return null
    return Character(
        id = n.id,
        name = n.name?.full ?: "",
        imageUrl = n.image?.large,
        role = role
    )
}

/** In-app domain models */
@Immutable
data class Anime(
    val id: Int = 0,
    val title: String = "",
    val synopsis: String = "",
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val score: Double? = null,
    val episodes: Int? = null,
    val year: Int? = null,
    val type: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList()
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

/** Combined detail payload fetched from AniList in a SINGLE GraphQL call (media + characters + recommendations). */
data class DetailData(
    val anime: Anime,
    val characters: List<Character>,
    val recommendations: List<Anime>
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

/** One real airing entry from AniList AiringSchedule, grouped by weekday in-app. */
@Serializable
data class AiringSchedule(
    val id: Int = 0,
    val title: String = "",
    val cover: String? = null,
    val episode: Int = 0,
    val totalEpisodes: Int? = null,
    val airingAt: Long = 0L,
    val status: String? = null
)

/** Sort airing entries by their broadcast time, earliest first. */
fun List<AiringSchedule>.sortedByAiring(): List<AiringSchedule> =
    sortedBy { it.airingAt }

/** Airing anime for a weekday (Jikan schedules). */
val ScheduleDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

/** A single timed broadcast for the weekly schedule card. */
data class DayAiring(
    val time: String,      // "23:00"
    val title: String,
    val status: String,    // "Hype Airing", "New", "Final"
    val episode: Int = 0,  // released episode number (0 = unknown)
    val cover: String? = null,
    val posterFocal: Alignment = Alignment.TopCenter // crop bias for the slanted art
)

/**
 * REAL weekly schedule, grouped by weekday from live AniList AiringSchedule data.
 * Each AiringSchedule already carries its exact broadcast time + episode number,
 * so we can show "EP x" and a status badge per show — no mock data.
 */
fun buildRealSchedule(shows: List<AiringSchedule>): Map<String, List<DayAiring>> {
    val cal = java.util.Calendar.getInstance()
    val byDay = ScheduleDays.associateWith { mutableListOf<DayAiring>() }
    for (s in shows.sortedByAiring()) {
        cal.timeInMillis = s.airingAt * 1000L
        val dayName = ScheduleDays.getOrNull(cal.get(java.util.Calendar.DAY_OF_WEEK) - 2) ?: continue
        val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE)
        val time = "%02d:%02d".format(hh, mm)
        val status = when {
            s.status.equals("FINISHED", true) -> "Final"
            s.status.equals("RELEASING", true) && (s.totalEpisodes == null || s.episode < (s.totalEpisodes ?: Int.MAX_VALUE)) -> "Hype Airing"
            else -> "New"
        }
        byDay[dayName]?.add(DayAiring(time, s.title, status, s.episode, s.cover))
    }
    return byDay
}

/** Demo anime used to fill New Releases / Upcoming when the live API is empty. */
val DemoNewReleases: List<Anime> = listOf(
    Anime(91001, "Solo Leveling", "https://cdn.myanimelist.net/images/anime/1500/110741.jpg", score = 8.9, type = "TV", episodes = 25, status = "Finished Airing", year = 2024, genres = listOf("Action", "Adventure", "Fantasy"), studios = listOf("A-1 Pictures")),
    Anime(91002, "Frieren: Beyond Journey's End", "https://cdn.myanimelist.net/images/anime/1015/138006.jpg", score = 9.3, type = "TV", episodes = 28, status = "Finished Airing", year = 2023, genres = listOf("Adventure", "Drama", "Fantasy"), studios = listOf("Madhouse")),
    Anime(91003, "Jujutsu Kaisen", "https://cdn.myanimelist.net/images/anime/1173/110255.jpg", score = 8.7, type = "TV", episodes = 47, status = "Finished Airing", year = 2023, genres = listOf("Action", "Supernatural"), studios = listOf("MAPPA")),
    Anime(91004, "Chainsaw Man", "https://cdn.myanimelist.net/images/anime/1364/138324.jpg", score = 8.6, type = "TV", episodes = 12, status = "Finished Airing", year = 2022, genres = listOf("Action", "Supernatural"), studios = listOf("MAPPA")),
    Anime(91005, "Demon Slayer: Kimetsu no Yaiba", "https://cdn.myanimelist.net/images/anime/1286/99889.jpg", score = 8.5, type = "TV", episodes = 55, status = "Finished Airing", year = 2024, genres = listOf("Action", "Fantasy"), studios = listOf("ufotable")),
    Anime(91006, "Spy x Family", "https://cdn.myanimelist.net/images/anime/1554/131518.jpg", score = 8.4, type = "TV", episodes = 37, status = "Finished Airing", year = 2023, genres = listOf("Action", "Comedy", "Slice of Life"), studios = listOf("Wit Studio"))
)

val DemoUpcoming: List<Anime> = listOf(
    Anime(92001, "Re:ZERO -Starting Life in Another World- Season 3", "https://cdn.myanimelist.net/images/anime/1764/138002.jpg", score = 8.4, type = "TV", episodes = 16, status = "Currently Airing", year = 2025, genres = listOf("Drama", "Fantasy", "Psychological"), studios = listOf("White Fox")),
    Anime(92002, "One Punch Man Season 3", "https://cdn.myanimelist.net/images/anime/1384/138003.jpg", score = 7.9, type = "TV", episodes = 12, status = "Upcoming", year = 2025, genres = listOf("Action", "Comedy", "Superhero"), studios = listOf("J.C.Staff")),
    Anime(92003, "Blue Lock Season 2", "https://cdn.myanimelist.net/images/anime/1432/138004.jpg", score = 7.7, type = "TV", episodes = 14, status = "Upcoming", year = 2025, genres = listOf("Sports")),
    Anime(92004, "Mushoku Tensei: Jobless Reincarnation Season 2", "https://cdn.myanimelist.net/images/anime/1315/138005.jpg", score = 8.3, type = "TV", episodes = 13, status = "Upcoming", year = 2025, genres = listOf("Adventure", "Drama", "Fantasy"), studios = listOf("Studio Bind")),
    Anime(92005, "Kaiju No. 8 Season 2", "https://cdn.myanimelist.net/images/anime/1543/138006.jpg", score = 8.0, type = "TV", episodes = 12, status = "Upcoming", year = 2025, genres = listOf("Action", "Sci-Fi")),
    Anime(92006, "Oshi no Ko Season 2", "https://cdn.myanimelist.net/images/anime/1492/138007.jpg", score = 8.2, type = "TV", episodes = 13, status = "Upcoming", year = 2025, genres = listOf("Drama", "Mystery"))
)

/** A new-release episode tile (landscape) for the Home "New Releases" row. */
data class NewReleaseEpisode(
    val anime: Anime,
    val epNum: String,     // "Ep 3"
    val epTitle: String,   // "Across the Pass"
    val timeAgo: String,   // "2 hrs ago"
    val duration: String,  // "24m"
    val progress: Float    // 0..1 watched fraction
)

/** Demo New Releases — mirrors anivave.html NEW_EPISODES_RELEASED. */
val DemoNewReleaseEpisodes: List<NewReleaseEpisode> = listOf(
    NewReleaseEpisode(DemoNewReleases[0], "Ep 3", "Across the Pass", "2 hrs ago", "24m", 0.30f),
    NewReleaseEpisode(DemoNewReleases[2], "Ep 17", "Thunderclap", "Yesterday", "25m", 0f),
    NewReleaseEpisode(DemoNewReleases[3], "Ep 5", "Gun Devil Fragment", "2 days ago", "24m", 0.95f),
    NewReleaseEpisode(DemoNewReleases[4], "Ep 8", "Hashira Training", "3 days ago", "24m", 0.60f),
    NewReleaseEpisode(DemoNewReleases[5], "Ep 11", "Mission Complete", "4 days ago", "24m", 0.45f)
)

/** Calendar pills for the Weekly Schedule card (day name + date number), per anivave.html. */
val SchedulePills = listOf(
    "Mon" to 13, "Tue" to 14, "Wed" to 15, "Thu" to 16, "Fri" to 17, "Sat" to 18, "Sun" to 19
)

