package com.aniwavestream.app.data.repository

import com.aniwavestream.app.data.api.AniListApi
import com.aniwavestream.app.data.model.AlMedia
import com.aniwavestream.app.data.model.AiringSchedule
import com.aniwavestream.app.data.model.AlResponse
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.Character
import com.aniwavestream.app.data.model.toAnime
import com.aniwavestream.app.data.model.toCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AnimeRepository(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
) {
    private val cache = mutableMapOf<Int, Anime>()
    private val requestTimes = ArrayDeque<Long>()
    private val rateMutex = Mutex()
    /** Per-filter result cache for Browse (letter/year/genre) so re-selecting a
     *  filter reuses the prior network result instead of re-hitting AniList. */
    private val cacheResults = mutableMapOf<String, List<Anime>>()

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
            val out = retryQuery { page(AniListApi.query(GENRE_Q, { str("genre", genre); int("perPage", 24) })).map { it.toAnime() } }
            remember(out)
        }
    }

    /** Retry a query up to 3x with backoff. AniList's unauthenticated limit is 90
     *  req/min; rapid filter tapping on the year/letter grid can trip HTTP 429 or
     *  transient 5xx. We retry those here so the Browse screen surfaces data instead
     *  of a fatal "Something went sideways" error on the first hit. */
    private suspend inline fun <T> retryQuery(block: () -> T): T {
        var lastErr: Throwable? = null
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: RateLimitException) {
                lastErr = e
                val wait = (1500L * (attempt + 1)).coerceAtMost(9000L)
                delay(wait)
            } catch (e: RuntimeException) {
                // AniListApi throws RuntimeException for non-429 HTTP errors; retry once.
                lastErr = e
                if (attempt < 1) delay(1000L) else throw e
            }
        }
        throw lastErr ?: RuntimeException("Browse query failed")
    }

    /** A–Z browse.
     *
     * AniList has no "starts-with" filter, and the obvious approaches are broken:
     *  - Sorting by TITLE_ROMAJI + fetching page 1 returns only symbol/punctuation
     *    titles (the first real "A" sits ~index 220), so a 50-item fetch is empty.
     *  - `search: "A"`/`"O"` return nothing (AniList drops single-letter stopwords),
     *    and raw search matches substrings, not starts-with.
     *
     * Strategy: use `search` (sort SEARCH_MATCH) for every letter; it returns
     * starts-with titles for B–Z. For the two stopword letters A and O — where
     * search yields nothing — fall back to paging the TITLE_ROMAJI-sorted catalogue
     * and collecting titles that actually start with the letter.
     * "All"/"ALL"/blank returns the popular list. */
    suspend fun byLetter(letter: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = ("letter:" + letter.lowercase())
            cacheResults[key]?.let { return@runCatching it }
            val out = retryQuery {
                loadByLetterUncached(letter)
            }
            cacheResults[key] = out
            remember(out)
        }
    }

    /** Core letter load (no caching/retry wrapper). */
    private suspend fun loadByLetterUncached(letter: String): List<Anime> {
        throttle()
        if (letter.isBlank() || letter.equals("All", ignoreCase = true)) {
            return page(AniListApi.query(POPULAR_Q, { int("perPage", 50) })).map { it.toAnime() }
        }
        val target = letter.first().uppercaseChar()
        // Search works for B–Z. A and O are stopwords → paging fallback.
        if (target !in setOf('A', 'O')) {
            val all = page(
                AniListApi.query(SEARCH_Q, { str("q", letter); int("perPage", 50) })
            ).map { it.toAnime() }
            val filtered = all.filter { a ->
                a.title.firstOrNull()?.equals(letter.first(), ignoreCase = true) == true
            }
            if (filtered.isNotEmpty()) return filtered
            // fall through to paging if search returned nothing unexpected
        }
        // Paging fallback (covers A, O, and any search miss).
        val out = mutableListOf<Anime>()
        val maxPages = 80
        for (pageNum in 1..maxPages) {
            val media = page(
                AniListApi.query(BY_LETTER_Q, { int("page", pageNum); int("perPage", 50) })
            )
            if (media.isEmpty()) break
            var passed = false
            for (m in media) {
                val anime = m.toAnime()
                val first = anime.title.firstOrNull() ?: continue
                if (!first.isLetterOrDigit()) continue
                val fu = first.uppercaseChar()
                if (fu < target) continue
                if (fu > target) { passed = true; break }
                if (fu == target) out += anime
            }
            if (passed || out.size >= 60) break
        }
        return out
    }

    /** Release year browse: AniList media filtered by seasonYear. Results are cached
     *  per year so re-tapping a decade/year doesn't re-hit the network (and re-risks 429). */
    suspend fun byYear(year: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = "year:$year"
            cacheResults[key]?.let { return@runCatching it }
            val out = retryQuery {
                page(AniListApi.query(BY_YEAR_Q, { int("year", year); int("perPage", 24) })).map { it.toAnime() }
            }
            cacheResults[key] = out
            remember(out)
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

    /**
     * REAL weekly schedule from AniList AiringSchedule. We fetch the next 7 days of
     * airing episodes (time-windowed) and hand the flat list back; the ViewModel
     * groups it by weekday + derives "EP x" + status. No mock data.
     */
    suspend fun schedule(perPage: Int = 80): Result<List<AiringSchedule>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            val now = System.currentTimeMillis() / 1000L
            val from = now              // only UPCOMING airings (this week onward) — not 1970-era
            val to = now + (7L * 24 * 3600) // next 7 days, in AniList unix-seconds
            val text = AniListApi.query(AIRING_Q) {
                int("per", perPage)
                int("from", from.toInt())
                int("to", to.toInt())
            }
            val resp = parse(text)
            resp.data?.Page?.airingSchedules?.mapNotNull { it.toAiring() } ?: emptyList()
        }
    }

    fun cached(id: Int): Anime? = cache[id]
}

/* ===================== GraphQL queries ===================== */

private const val MEDIA_FIELDS = """
    id idMal title { romaji english native }
    coverImage { extraLarge large medium color }
    bannerImage averageScore episodes nextAiringEpisode { episode } seasonYear format status description
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
query(${'$'}page: Int, ${'$'}perPage: Int) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
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

private const val AIRING_Q = """
query(${'$'}per: Int, ${'$'}from: Int, ${'$'}to: Int) {
  Page(page: 1, perPage: ${'$'}per) {
    airingSchedules(sort: TIME, airingAt_greater: ${'$'}from, airingAt_lesser: ${'$'}to) {
      episode
      airingAt
      media { id title { romaji english } coverImage { medium } episodes status }
    }
  }
}
"""

private fun com.aniwavestream.app.data.model.AlAiring.toAiring(): AiringSchedule? {
    val m = media ?: return null
    val t = m.title
    val name = (t?.english ?: t?.romaji).orEmpty().ifBlank { return null }
    return AiringSchedule(
        id = m.id,
        title = name,
        cover = m.coverImage?.extraLarge ?: m.coverImage?.large ?: m.coverImage?.medium,
        episode = episode ?: 0,
        totalEpisodes = m.episodes,
        airingAt = airingAt ?: 0L,
        status = m.status
    )
}

/** Maps the numeric BrowseGenres ids used by the UI to AniList genre names. */
private val GENRE_NAMES = mapOf(
    1 to "Action", 2 to "Adventure", 4 to "Comedy", 8 to "Drama",
    10 to "Fantasy", 14 to "Horror", 7 to "Mystery", 22 to "Romance",
    24 to "Sci-Fi", 36 to "Slice of Life", 30 to "Sports", 37 to "Supernatural"
)