package com.aniwavestream.app.ui.components

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * Rule 4 — Consumer-grade error states.
 * Maps raw backend exceptions (HTTP 504, 404, SocketTimeoutException, ...) into
 * friendly, on-brand copy. The UI layer must NEVER surface a raw stack trace or
 * server code — it renders a [FriendlyError] instead.
 */
data class FriendlyError(
    val title: String,
    val message: String,
    val icon: FriendlyErrorIcon
)

enum class FriendlyErrorIcon { OFFLINE, TIMEOUT, NOT_FOUND, RATE_LIMIT, GENERIC }

fun Throwable.toFriendlyError(): FriendlyError = when (this) {
    is UnknownHostException -> FriendlyError(
        title = "You're offline",
        message = "We couldn't reach the anime network. Check your connection and try again.",
        icon = FriendlyErrorIcon.OFFLINE
    )
    is SocketTimeoutException -> FriendlyError(
        title = "Transmission interrupted",
        message = "The anime transmission was interrupted. Let's try again!",
        icon = FriendlyErrorIcon.TIMEOUT
    )
    is HttpException -> when (code()) {
        404 -> FriendlyError(
            title = "Nothing here yet",
            message = "We couldn't find this title. It may have moved or is no longer available.",
            icon = FriendlyErrorIcon.NOT_FOUND
        )
        429 -> FriendlyError(
            title = "Slow down a sec",
            message = "You're moving fast! Give the servers a moment to catch their breath.",
            icon = FriendlyErrorIcon.RATE_LIMIT
        )
        in 500..599 -> FriendlyError(
            title = "Our side hiccuped",
            message = "The anime servers are having a moment. We'll be back shortly — try again.",
            icon = FriendlyErrorIcon.TIMEOUT
        )
        else -> genericError()
    }
    is IOException -> FriendlyError(
        title = "Connection trouble",
        message = "Something got tangled on the way. Give it another shot.",
        icon = FriendlyErrorIcon.OFFLINE
    )
    else -> genericError()
}

/** Convenience for legacy call sites that only have a message string. */
fun String?.toFriendlyError(): FriendlyError = when {
    this == null -> genericError()
    contains("timeout", true) || contains("timed out", true) -> FriendlyError(
        "Transmission interrupted",
        "The anime transmission was interrupted. Let's try again!",
        FriendlyErrorIcon.TIMEOUT
    )
    contains("unable to resolve host", true) || contains("no address", true) -> FriendlyError(
        "You're offline",
        "We couldn't reach the anime network. Check your connection and try again.",
        FriendlyErrorIcon.OFFLINE
    )
    contains("404", true) -> FriendlyError(
        "Nothing here yet",
        "We couldn't find this title. It may have moved or is no longer available.",
        FriendlyErrorIcon.NOT_FOUND
    )
    contains("429", true) -> FriendlyError(
        "Slow down a sec",
        "You're moving fast! Give the servers a moment to catch their breath.",
        FriendlyErrorIcon.RATE_LIMIT
    )
    else -> genericError()
}

private fun genericError() = FriendlyError(
    title = "Something went sideways",
    message = "We hit an unexpected snag. Tap retry and we'll give it another go.",
    icon = FriendlyErrorIcon.GENERIC
)
