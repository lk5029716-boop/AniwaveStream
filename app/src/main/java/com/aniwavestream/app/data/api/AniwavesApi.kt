package com.aniwavestream.app.data.api

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Client for the self-hosted Aniwaves scraping API (Aniwavesapi).
 *
 * The API is keyed by the aniwaves.ru *slug* (e.g. "naruto-76396"), NOT the
 * AniList id, so to play episode N of an anime we:
 *   1) /api/search?q=<title>  -> slug
 *   2) /api/servers?episodeId=<slug>-ep-<N>&type=sub -> list of server ids
 *   3) /api/stream?serverId=<id> -> proxiedM3u8 (CORS-safe, same-origin)
 *
 * /api/stream returns a [proxiedM3u8] URL already rewritten through the backend's
 * own /api/proxy, so the Android player can load it directly without hitting the
 * CDN's CORS/referer lock. We prefer "Vidplay", then "BYFMS"/"bymas".
 *
 * Any failure returns null -- the caller shows a real error, never a demo video.
 */
object AniwavesApi {

    // Your deployed backend. No trailing slash.
    private const val BASE = "https://aniwavesapis.onrender.com"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Ping /api/health once. The backend is hosted on Render's free tier, which
     * spins down after inactivity; the first call can take several seconds to
     * wake it. Hitting health first means the real (multi-step) resolve chain
     * doesn't blow past ExoPlayer's 30s timeout on its very first request.
     */
    fun warmUp() {
        try { get("/api/health") } catch (_: Exception) { /* best-effort only */ }
    }

    private fun get(path: String): String {
        val req = Request.Builder()
            .url("$BASE$path")
            .header("Accept", "application/json")
            .header("User-Agent", "AniwaveStream/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("AniwavesApi HTTP ${resp.code}: ${resp.body?.string()?.take(120)}")
            return resp.body?.string().orEmpty()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Search aniwaves by title, return the best-matching slug (or null). */
    fun resolveSlug(title: String): String? {
        val q = title.trim()
        if (q.isBlank()) return null
        val root: JsonObject = json.parseToJsonElement(get("/api/search?q=${enc(q)}")).jsonObject
        val results = root["results"]?.jsonArray ?: return null
        if (results.isEmpty()) return null
        val exact = results.firstOrNull {
            it.jsonObject["title"]?.jsonPrimitive?.content?.equals(q, true) == true
        } ?: results.first()
        return exact.jsonObject["id"]?.jsonPrimitive?.content
    }

    /** Server ids available for an episode, with their display names. */
    fun servers(slug: String, episode: Int, type: String = "sub"): List<Pair<String, String>> {
        val epId = "$slug-ep-$episode"
        val root: JsonObject = json.parseToJsonElement(get("/api/servers?episodeId=${enc(epId)}&type=$type")).jsonObject
        val list = root["servers"]?.jsonArray ?: return emptyList()
        return list.mapNotNull { s ->
            val o = s.jsonObject
            val id = o["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = o["name"]?.jsonPrimitive?.content ?: "unknown"
            id to name
        }
    }

    /** Resolve a playable .m3u8 URL for a server id, or null. */
    fun streamUrl(serverId: String): String? {
        val root: JsonObject = json.parseToJsonElement(get("/api/stream?serverId=${enc(serverId)}")).jsonObject
        // Prefer proxiedM3u8: the backend rewrites EVERY child path (variant + .ts
        // segments) back through /api/proxy, so the whole playlist tree stays routed
        // through our server and survives the CDN's per-token host rotation. The raw
        // m3u8 has relative /cdn/ children bound to a short-lived host that 400s once
        // the token expires (verified live: fresh=200, ~2min later=400). Raw is only a
        // last-resort fallback if the backend omits the proxied field.
        root["proxiedM3u8"]?.jsonPrimitive?.content?.let { return "$BASE$it" }
        return root["m3u8"]?.jsonPrimitive?.content
    }

    /**
     * Full resolution: title + episode -> first working .m3u8.
     * Preference order: Vidplay, then BYFMS/bymas, then any other server.
     * Returns null if nothing resolves (caller shows a real error, not a demo).
     */
    fun resolveStream(title: String, episode: Int, type: String = "sub"): String? {
        val slug = resolveSlug(title) ?: return null
        val all = servers(slug, episode, type)
        if (all.isEmpty()) return null
        val preferred = all.sortedBy { rank(it.second) }
        for ((id, name) in preferred) {
            try {
                streamUrl(id)?.let { return it }
                Log.w("AniwavesApi", "server $name returned no stream, trying next")
            } catch (e: Exception) {
                Log.w("AniwavesApi", "server $name failed: ${e.message}")
            }
        }
        return null
    }

    private fun rank(name: String): Int = when {
        name.equals("Vidplay", true) -> 0
        name.equals("BYFMS", true) || name.equals("bymas", true) -> 1
        else -> 2
    }
}
