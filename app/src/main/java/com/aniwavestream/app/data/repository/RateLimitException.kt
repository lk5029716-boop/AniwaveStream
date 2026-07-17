package com.aniwavestream.app.data.repository

/**
 * Thrown by [AniListApi] when the AniList unauthenticated rate limit (90 req/min) is exceeded
 * (HTTP 429). Surfaces to the UI so it can show a friendly "slow down" state with a retry,
 * instead of the repository silently dropping data (e.g. an empty characters row).
 */
class RateLimitException(message: String = "AniList rate limit (HTTP 429) exceeded") : Exception(message)
