package com.aniwavestream.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.api.AniwavesApi
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.player.PlayerModule
import com.aniwavestream.app.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    animeId: Int,
    episode: Int,
    repository: AnimeRepository,
    library: UserLibraryStore,
    onBack: () -> Unit,
    onEpisodeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val vm: PlayerViewModel = viewModel()
    val exoPlayer = vm.player
    val playbackError by vm.error.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var isFullscreen by remember { mutableStateOf(true) }
    var currentType by remember(animeId, episode) { mutableStateOf("sub") }

    var streamUrl by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var resolveError by remember(animeId, episode) { mutableStateOf(false) }
    var errorDetail by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var loadKey by remember { mutableStateOf(0) }

    LaunchedEffect(animeId) {
        repository.detail(animeId)
            .onSuccess { title = it.title }
            .onFailure { resolveError = true }
    }

    LaunchedEffect(animeId, episode, title) {
        if (title.isBlank()) return@LaunchedEffect
        resolveError = false
        errorDetail = null
        val result = withContext(Dispatchers.IO) {
            runCatching { AniwavesApi.resolveStream(title, episode, currentType) }
        }
        if (result.isSuccess && !result.getOrNull().isNullOrBlank()) {
            streamUrl = result.getOrNull()
            resolveError = false
        } else {
            streamUrl = null
            resolveError = true
            errorDetail = result.exceptionOrNull()?.message
                ?: "Backend returned no stream for '${title}' ep $episode"
        }
    }

    fun safePlayer(block: Player.() -> Unit) {
        runCatching { exoPlayer.block() }
    }

    LaunchedEffect(streamUrl, loadKey) {
        val url = streamUrl ?: return@LaunchedEffect
        vm.clearError()
        errorDetail = null
        var attempt = 0
        val maxAttempts = 3
        var ok = false
        while (attempt < maxAttempts && !ok) {
            attempt++
            runCatching {
                safePlayer {
                    setMediaItem(PlayerModule.buildMediaItem(url))
                    prepare()
                    play()
                }
            }
            val deadline = System.currentTimeMillis() + 25_000L
            while (System.currentTimeMillis() < deadline) {
                // Guard every player read: if the player is released mid-poll
                // (episode switch / back nav), reading playbackState throws
                // IllegalStateException. Swallow it and bail instead of
                // propagating the exception up the coroutine.
                val ready = runCatching {
                    exoPlayer.playbackState == Player.STATE_READY
                }.getOrElse { false }
                if (ready) { ok = true; break }
                if (vm.error.value != null) break
                delay(500)
            }
            if (!ok && vm.error.value == null) {
                delay(2500L * attempt)
            }
        }
        if (!ok) {
            resolveError = true
            errorDetail = vm.error.value?.message
                ?: "ExoPlayer couldn't load the stream (timed out after $maxAttempts attempts)"
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> runCatching { exoPlayer.pause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(animeId, episode, exoPlayer) {
        while (isActive) {
            delay(5000)
            runCatching {
                val dur = exoPlayer.duration
                val pos = exoPlayer.currentPosition
                if (dur > 0) {
                    library.saveProgress(animeId, episode, pos.toFloat() / dur.toFloat())
                }
            }
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            runCatching {
                val dur = exoPlayer.duration
                val pos = exoPlayer.currentPosition
                if (dur > 0) {
                    scope.launch {
                        library.saveProgress(animeId, episode, pos.toFloat() / dur.toFloat())
                    }
                }
            }
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler { onBack() }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackError != null || resolveError) {
            androidx.compose.material3.Text(
                errorDetail ?: "Playback error",
                color = Color.White,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            )
        }
    }
}