package com.aniwavestream.app.data.repository

import com.aniwavestream.app.data.api.AniListApi
import com.aniwavestream.app.data.model.AlMedia
import com.aniwavestream.app.data.model.AlResponse
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.Character
import com.aniwavestream.app.data.model.toAnime
import com.aniwavestream.app.data.model.toCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AnimeRepository(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
) {
    private val cache = mutableMapOf<Int, Anime>()
    private val requestTimes = ArrayDeque<Long>()
    private val rateMutex = Mutex()

    // AniList unauthenticated limit is 90 req/min. Serialize ALL requests through one
    // mutex + a rolling 60s window so parallel calls can't burst past the limit and get
    // HTTP 429 (which previously dropped characters/recommendations on detail screens).
    private val MIN_GAP_MS = 750L
    private val WINDOW_MS = 60_000L
    private val MAX_PER_WINDOW = 85

    private suspend fun throttle() {
        rateMutex.withLock {
            val now = System.currentTimeMillis()
            while (requestTimes.isNotEmpty() && now - requestTimes.first() > WINDOW_MS) {
                requestTimes.removeFirst()
            }
            if (requestTimes.size >= MAX_PER_WINDOW) {
                val wait = (requestTimes.first() + WINDOW_MS - now).coerceAtLeast(0L) + 50L
                delay(wait)
            } else {
                val since = requestTimes.lastOrNull()?.let { now - it } ?: Long.MAX_VALUE
                val wait = (MIN_GAP_MS - since).coerceAtLeast(0L)
                if (wait > 0) delay(wait)
            }
            requestTimes.addLast(System.currentTimeMillis())
        }
    }

    private fun remember(list: List<Anime>): List<Anime> {
        list.forEach { cache[it.id] = it }
        return list
    }

    private fun parse(text: String): AlResponse {
        val resp = json.decodeFromString<AlResponse>(text)
        if (resp.errors?.isNotEmpty() == true) {
            throw RuntimeException(resp.errors.first().message)
        }
        return resp
    }

    private fun page(text: String): List<AlMedia> =
        parse(text).data?.Page?.media ?: emptyList()

    // ---- Home / browse lists (all AniList GraphQL) ----

    suspend fun trending(perPage: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(page(AniListApi.query(TRENDING_Q, { int("perPage", perPage) })).map { it.toAnime() })
        }
    }

    suspend fun topRated(perPage: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(page(AniListApi.query(POPULAR_Q, { int("perPage", perPage) })).map { it.toAnime() })
        }
    }

    /** Top 100 by score — AniList caps perPage at 50, so fetch two pages. */
    suspend fun top100(): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            val out = mutableListOf<Anime>()
            for (page in 1..2) {
                throttle()
                out += page(AniListApi.query(TOP100_Q, { int("page", page); int("perPage", 50) }))
                    .map { it.toAnime() }
            }
            remember(out.distinctBy { it.id })
        }
    }

    suspend fun seasonal(perPage: Int = 25): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            val (season, year) = currentSeasonYear()
            remember(
                page(
                    AniListApi.query(SEASONAL_Q) {
                        str("season", season)
                        int("year", year)
                        int("perPage", perPage)
                    }
                ).map { it.toAnime() }
            )
        }
    }

    /** Paged variant for the "See All" screen — fetches a server page of the current season
     *  without depending on HomeViewModel's shared state (so opening See All never triggers a
     *  home reload). */
    suspend fun seasonalPage(pageNum: Int, perPage: Int = 50): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            val (season, year) = currentSeasonYear()
            remember(
                page(
                    AniListApi.query(SEASONAL_Q) {
                        str("season", season)
                        int("year", year)
                        int("perPage", perPage)
                    }
                ).map { it.toAnime() }
            )
        }
    }

    /** Same for Top 100 / Popular — let See All fetch its own page instead of relying on the
     *  home snapshot (which can be empty/transient right after a refresh). */
    suspend fun popularPage(pageNum: Int, perPage: Int = 50): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(
                page(AniListApi.query(POPULAR_Q) {
                    int("page", pageNum); int("perPage", perPage)
                }).map { it.toAnime() }
            )
        }
    }

    /** AniList's Media.season arg requires an enum (WINTER/SPRING/SUMMER/FALL); CURRENT is
     *  invalid there (it only works on AiringSchedule). Derive the real current season. */
    private fun currentSeasonYear(): Pair<String, Int> {
        val now = java.util.Calendar.getInstance()
        val month = now.get(java.util.Calendar.MONTH) + 1
        val year = now.get(java.util.Calendar.YEAR)
        val season = when (month) {
            in 3..5 -> "SPRING"
            in 6..8 -> "SUMMER"
            in 9..11 -> "FALL"
            else -> "WINTER"
        }
        // Northern-hemisphere anime seasons: Winter = Jan–Mar (year), Spring = Apr–Jun, etc.
        return season to year
    }

    suspend fun newReleases(perPage: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(page(AniListApi.query(RELEASING_Q, { int("perPage", perPage) })).map { it.toAnime() })
        }
    }

    suspend fun upcoming(perPage: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(page(AniListApi.query(UPCOMING_Q, { int("perPage", perPage) })).map { it.toAnime() })
        }
    }

    suspend fun search(query: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.isBlank()) return@runCatching emptyList()
            throttle()
            remember(page(AniListApi.query(SEARCH_Q, { str("q", query); int("perPage", 20) })).map { it.toAnime() })
        }
    }

    suspend fun byGenre(genreId: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            val genre = GENRE_NAMES[genreId] ?: return@runCatching emptyList()
            throttle()
            remember(page(AniListApi.query(GENRE_Q, { str("genre", genre); int("perPage", 24) })).map { it.toAnime() })
        }
    }

    /** A-Z browse: fetch a title-sorted page and client-filter by the first letter.
     *  Pass "0-9" to match titles starting with a digit. */
    suspend fun byLetter(letter: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            val all = page(AniListApi.query(BY_LETTER_Q, { int("perPage", 100) })).map { it.toAnime() }
            val filtered = all.filter { a ->
                val t = a.title.firstOrNull()?.toString().orEmpty()
                if (letter == "0-9") t.any { it.isDigit() } else t.equals(letter, ignoreCase = true)
            }
            remember(filtered)
        }
    }

    /** Release year browse: AniList media filtered by seasonYear. */
    suspend fun byYear(year: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(page(AniListApi.query(BY_YEAR_Q, { int("year", year); int("perPage", 24) })).map { it.toAnime() })
        }
    }

    // ---- Detail + extras ----

    suspend fun detail(id: Int): Result<Anime> = withContext(Dispatchers.IO) {
        runCatching {
            cache[id]?.let { return@runCatching it }
            throttle()
            val media = parse(AniListApi.query(DETAIL_Q, { int("id", id) })).data?.Media
                ?: throw RuntimeException("AniList: no media for id $id")
            media.toAnime().also { cache[it.id] = it }
        }
    }

    suspend fun characters(id: Int): Result<List<Character>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            val media = parse(AniListApi.query(CHAR_Q, { int("id", id) })).data?.Media
            media?.characters?.edges
                ?.mapNotNull { it.toCharacter() }
                ?.take(12)
                ?: emptyList()
        }
    }

    suspend fun recommendations(id: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            val media = parse(AniListApi.query(REC_Q, { int("id", id) })).data?.Media
            media?.recommendations?.nodes
                ?.mapNotNull { it.media }
                ?.distinctBy { it.id }
                ?.take(12)
                ?.map { it.toAnime() }
                ?: emptyList()
        }
    }

    /** Weekly schedule — AniList has no curated weekly slate; keep the demo list. */
    suspend fun schedule(day: String, perPage: Int = 14): Result<List<Anime>> =
        Result.success(emptyList())

    fun cached(id: Int): Anime? = cache[id]
}

/* ===================== GraphQL queries ===================== */

private const val MEDIA_FIELDS = """
    id idMal title { romaji english native }
    coverImage { extraLarge large medium color }
    bannerImage averageScore episodes seasonYear format status description
    genres studios(isMain: true) { nodes { name } }
"""

private const val TRENDING_Q = """
query(${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(sort: TRENDING_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val POPULAR_Q = """
query(${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(sort: POPULARITY_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val TOP100_Q = """
query(${'$'}page: Int, ${'$'}perPage: Int) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    media(sort: SCORE_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val SEASONAL_Q = """
query(${'$'}season: MediaSeason, ${'$'}year: Int, ${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(season: ${'$'}season, seasonYear: ${'$'}year, sort: POPULARITY_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val RELEASING_Q = """
query(${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(status: RELEASING, sort: POPULARITY_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val UPCOMING_Q = """
query(${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(status: NOT_YET_RELEASED, sort: POPULARITY_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val SEARCH_Q = """
query(${'$'}q: String, ${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(search: ${'$'}q, sort: SEARCH_MATCH, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val GENRE_Q = """
query(${'$'}genre: String, ${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(genre: ${'$'}genre, sort: POPULARITY_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}"""

private const val BY_LETTER_Q = """\
query(${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(sort: TITLE_ROMAJI, type: ANIME) { ${MEDIA_FIELDS} }
  }
}
"""

private const val BY_YEAR_Q = """\
query(${'$'}year: Int, ${'$'}perPage: Int) {
  Page(page: 1, perPage: ${'$'}perPage) {
    media(seasonYear: ${'$'}year, sort: POPULARITY_DESC, type: ANIME) { ${MEDIA_FIELDS} }
  }
}
"""

private const val DETAIL_Q = """
query(${'$'}id: Int) {
  Media(id: ${'$'}id, type: ANIME) { ${MEDIA_FIELDS} }
}"""

private const val CHAR_Q = """
query(${'$'}id: Int) {
  Media(id: ${'$'}id, type: ANIME) {
    characters(sort: ROLE, perPage: 12) {
      edges { role node { id name { full } image { large } } }
    }
  }
}"""

private const val REC_Q = """
query(${'$'}id: Int) {
  Media(id: ${'$'}id, type: ANIME) {
    recommendations(sort: RATING_DESC, perPage: 12) {
      nodes { media { ${MEDIA_FIELDS} } }
    }
  }
}"""

/** Maps the numeric BrowseGenres ids used by the UI to AniList genre names. */
private val GENRE_NAMES = mapOf(
    1 to "Action", 2 to "Adventure", 4 to "Comedy", 8 to "Drama",
    10 to "Fantasy", 14 to "Horror", 7 to "Mystery", 22 to "Romance",
    24 to "Sci-Fi", 36 to "Slice of Life", 30 to "Sports", 37 to "Supernatural"
)