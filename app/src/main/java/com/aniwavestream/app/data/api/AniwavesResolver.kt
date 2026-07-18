package com.aniwavestream.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves a real playback URL for an episode using the AniwavesApi backend.
 *
 * Steps:
 *  1. Resolve the aniwaves slug from the anime title (/api/search).
 *  2. Pick a server for the episode (/api/servers). Tries Vidplay first,
 *     then BYFMS, then DGHG.
 *  3. Resolve the stream (/api/stream) and return its proxiedM3u8, prefixed
 *     with the API base so ExoPlayer can fetch it same-origin.
 *
 * Returns the absolute proxied m3u8 URL, or throws if resolution fails at any
 * step (caller is responsible for surfacing the error — no demo fallback).
 */
class AniwavesResolver(private val api: AniwavesApi = AniwavesApi.create()) {

    // animeId (Jikan mal_id) -> aniwaves slug. Cheap in-memory cache.
    private val slugCache = mutableMapOf<Int, String>()

    suspend fun resolve(animeId: Int, title: String, episode: Int): String =
        withContext(Dispatchers.IO) {
            val slug = slugCache[animeId] ?: run {
                val results = api.search(title).results
                val best = results.firstOrNull { it.id.isNotBlank() }
                    ?: throw IllegalStateException("No anime match on backend for \"$title\"")
                slugCache[animeId] = best.id
                best.id
            }

            val episodeId = "${slug}-ep-${episode}"
            val servers = api.servers(episodeId = episodeId, type = "sub").servers
            if (servers.isEmpty()) {
                throw IllegalStateException("No servers returned for $episodeId")
            }

            // Preference order: Vidplay -> BYFMS -> DGHG -> anything.
            val ordered = servers.sortedBy { s ->
                when (s.name.lowercase()) {
                    "vidplay" -> 0
                    "byfms" -> 1
                    "dghg" -> 2
                    else -> 3
                }
            }

            var lastError: Throwable? = null
            for (server in ordered) {
                try {
                    val resp = api.stream(serverId = server.id)
                    val proxied = resp.proxiedM3u8
                    if (!proxied.isNullOrBlank()) {
                        return@withContext absolutize(proxied)
                    }
                    lastError = IllegalStateException(
                        resp.error ?: "Stream resolution returned no proxiedM3u8 (${server.name})"
                    )
                } catch (t: Throwable) {
                    lastError = t
                }
            }
            throw lastError ?: IllegalStateException("All servers failed for $episodeId")
        }

    private fun absolutize(path: String): String =
        if (path.startsWith("http")) path else AniwavesApi.BASE.trimEnd('/') + path
}
