package com.aniwavestream.app.data.repository

import com.aniwavestream.app.data.api.JikanApi
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.ScheduleDay
import com.aniwavestream.app.data.model.toAnime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AnimeRepository(
    private val api: JikanApi = JikanApi.create()
) {
    private val cache = mutableMapOf<Int, Anime>()
    private var lastCallMs = 0L

    private suspend fun throttle() {
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

    suspend fun schedule(day: ScheduleDay, limit: Int = 50): Result<List<Anime>> = withContext(Dispatchers.IO) {
        runCatching {
            throttle()
            remember(api.schedules(day = day.jikanParam, limit = limit).data.map { it.toAnime() })
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

    fun cached(id: Int): Anime? = cache[id]
}
