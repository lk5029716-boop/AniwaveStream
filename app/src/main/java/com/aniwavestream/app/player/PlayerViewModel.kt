package com.aniwavestream.app.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the [Player] instance so it SURVIVES Activity recreation on rotation
 * (Rule 9c done correctly). Previously the player lived in a Composable
 * `remember` and was released on every dispose — including the configuration
 * change triggered by `requestedOrientation` for fullscreen. Releasing a
 * player mid-playback then touching it again threw and force-closed the app.
 *
 * Lifecycle is now: build once, pause on background, release only when the
 * ViewModel is truly cleared (navigating away from the player).
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val player: Player = PlayerModule.buildPlayer(app.applicationContext)

    private val _error = MutableStateFlow<androidx.media3.common.PlaybackException?>(null)
    val error: StateFlow<androidx.media3.common.PlaybackException?> = _error.asStateFlow()

    private val _tracks = MutableStateFlow<androidx.media3.common.Tracks?>(null)
    val tracks: StateFlow<androidx.media3.common.Tracks?> = _tracks.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
            _error.value = e
        }

        override fun onTracksChanged(t: androidx.media3.common.Tracks) {
            _tracks.value = t
        }
    }

    init {
        player.addListener(listener)
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { player.removeListener(listener) }
        runCatching { player.release() }
    }
}
