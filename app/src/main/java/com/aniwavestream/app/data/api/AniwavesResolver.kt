package com.aniwavestream.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * The backend is occasionally flaky (transient 502s / "stream extraction
 * failed"), so each /api/stream attempt is retried with a short backoff.
 * No demo fallback — if every server fails the caller surfaces the error.
 */
class AniwavesResolver(private val api: AniwavesApi = AniwavesApi.create()) {

    // animeId (Jikan mal_id) -> aniwaves slug. Cheap in-memory cache.
    private val slugCache = mutableMapOf<Int, String>()

    suspend fun resolve(animeId: Int, title: String, episode: Int): String =
        withContext(Dispatchers.IO) {
            val slug = slugFor(animeId, title)
            val (servers, episodeId) = serversFor(slug, episode)
            val url = firstWorkingUrl(servers)
                ?: throw IllegalStateException("All servers failed for $episodeId")
            url
        }

    /**
     * Resolves and returns every available server's playback URL so the UI can
     * offer a real server/quality picker. Servers that fail extraction are
     * dropped. If none resolve, throws.
     */
    suspend fun listServers(animeId: Int, title: String, episode: Int): List<ResolvedServer> =
        withContext(Dispatchers.IO) {
            val slug = slugFor(animeId, title)
            val (servers, _) = serversFor(slug, episode)
            // NOTE: a suspend call (tryResolveStream) cannot live inside a
            // mapNotNull lambda (non-suspending), so use an explicit loop.
            val resolved = mutableListOf<ResolvedServer>()
            for (s in servers) {
                val url = tryResolveStream(s.id)
                if (url != null) resolved += ResolvedServer(s.id, s.name, url)
            }
            if (resolved.isEmpty()) throw IllegalStateException("No servers resolved")
            resolved
        }

    private suspend fun slugFor(animeId: Int, title: String): String =
        slugCache[animeId] ?: run {
            val results = api.search(title).results
            val best = results.firstOrNull { it.id.isNotBlank() }
                ?: throw IllegalStateException("No anime match on backend for \"$title\"")
            slugCache[animeId] = best.id
            best.id
        }

    private suspend fun serversFor(slug: String, episode: Int): Pair<List<Server>, String> {
        val episodeId = "$slug-ep-$episode"
        val servers = api.servers(episodeId = episodeId, type = "sub").servers
        if (servers.isEmpty()) {
            throw IllegalStateException("No servers returned for $episodeId")
        }
        val ordered = servers.sortedBy { s ->
            when (s.name.lowercase()) {
                "vidplay" -> 0
                "byfms" -> 1
                "dghg" -> 2
                else -> 3
            }
        }
        return ordered to episodeId
    }

    private suspend fun firstWorkingUrl(servers: List<Server>): String? {
        var lastError: Throwable? = null
        for (server in servers) {
            try {
                val url = tryResolveStream(server.id)
                if (url != null) return url
                lastError = IllegalStateException("No proxiedM3u8 (${server.name})")
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("stream resolution failed")
    }

    private suspend fun tryResolveStream(serverId: String): String? {
        var last: Throwable? = null
        repeat(ATTEMPTS) { attempt ->
            try {
                val resp = api.stream(serverId = serverId)
                val proxied = resp.proxiedM3u8
                if (!proxied.isNullOrBlank()) return absolutize(proxied)
                last = IllegalStateException(resp.error ?: "No proxiedM3u8 in response")
            } catch (t: Throwable) {
                last = t
            }
            if (attempt < ATTEMPTS - 1) delay(BACKOFF_MS)
        }
        throw last ?: IllegalStateException("stream resolution failed")
    }

    private fun absolutize(path: String): String =
        if (path.startsWith("http")) path else AniwavesApi.BASE.trimEnd('/') + path

    companion object {
        private const val ATTEMPTS = 3
        private const val BACKOFF_MS = 700L
    }
}

data class ResolvedServer(
    val id: String,
    val name: String,
    val url: String
)
