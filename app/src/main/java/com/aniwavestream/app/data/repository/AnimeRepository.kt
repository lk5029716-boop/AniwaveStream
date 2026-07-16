package com.aniwavestream.app.data.repository

import com.aniwavestream.app.data.api.JikanApi
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.Character
import com.aniwavestream.app.data.model.CharacterEdgeDto
import com.aniwavestream.app.data.model.toAnime
import com.aniwavestream.app.data.model.toCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AnimeRepository(
    private val api: JikanApi = JikanApi.create()
) {
    private val cache = mutableMapOf<Int, Anime>()
    private var lastCallMs = 0L

    private suspend fun throttle() {
        // Jikan free tier ~3 req/s; keep a small gap between calls
        val now = System.currentTimeMillis()
        val wait = 400 - (now - lastCallMs)
        if (wait > 0) delay(wait)
        lastCallMs = System.currentTimeMillis()
    }

    private fun remember(list: List<Anime>): List<Anime> {
        list.forEach { cache[it.id] = it }
        return list
    }

    suspend fun topRated(limit: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.topAnime(limit = limit).data.map { it.toAnime() })
        }
    }

    suspend fun trending(limit: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.topAnime(limit = limit, filter = "bypopularity").data.map { it.toAnime() })
        }
    }

    suspend fun seasonal(limit: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.seasonalNow(limit = limit).data.map { it.toAnime() })
        }
    }

    suspend fun search(query: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.isBlank()) return@runCatching emptyList()
            throttle()
            remember(api.search(query = query).data.map { it.toAnime() })
        }
    }

    suspend fun byGenre(genreId: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.byGenre(genreId = genreId).data.map { it.toAnime() })
        }
    }

    suspend fun detail(id: Int): Result<Anime> = withContext(Dispatchers.IO) {
        runCatching {
            cache[id]?.let { return@runCatching it }
            throttle()
            api.animeFull(id).data.toAnime().also { cache[it.id] = it }
        }
    }

    suspend fun recommendations(id: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            api.recommendations(id).data.map { it.toAnime() }
        }
    }

    suspend fun characters(id: Int): Result<List<Character>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            api.characters(id).data.mapNotNull { it.toCharacter() }.take(10)
        }
    }

    suspend fun upcoming(limit: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.animeList(status = "upcoming", orderBy = "popularity", sort = "desc", limit = limit).data.map { it.toAnime() })
        }
    }

    suspend fun newReleases(limit: Int = 20): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.animeList(status = "complete", orderBy = "score", sort = "desc", limit = limit).data.map { it.toAnime() })
        }
    }

    suspend fun schedule(day: String, limit: Int = 14): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            api.schedules(day, limit = limit).data.map { it.toAnime() }
        }
    }

    fun cached(id: Int): Anime? = cache[id]
}
