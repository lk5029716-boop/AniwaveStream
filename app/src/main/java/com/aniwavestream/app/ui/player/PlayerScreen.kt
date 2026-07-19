package com.aniwavestream.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aniwavestream.app.R
import com.aniwavestream.app.data.api.AniwavesApi
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.player.PlayerModule
import com.aniwavestream.app.player.PlayerViewModel
import com.aniwavestream.app.player.SelectableTrack
import com.aniwavestream.app.player.TrackSelectionUiState
import com.aniwavestream.app.player.disableSubtitles
import com.aniwavestream.app.player.selectTrack
import com.aniwavestream.app.player.toTrackSelectionUiState
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.SurfaceRaised
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-featured video player UI modeled on Dantotsu's ExoplayerView layout,
 * rebuilt in Compose on top of AniwaveStream's existing media3 engine
 * ([PlayerViewModel] / [PlayerModule]) and Aniwaves backend resolver.
 *
 * Recreates Dantotsu's control surface:
 *  - Top bar: back, title + episode, SUB/DUB pill, episode picker, lock, orientation.
 *  - Center: prev-ep / -10s / play-pause / +10s / next-ep.
 *  - Bottom: scrubber + position/duration, speed, subtitles, audio, PiP, resize (fit/zoom/fill).
 *  - Gestures: tap toggles controls, double-tap left/right seeks -/+10s, lock hides everything.
 *  - Sheets: subtitle picker, audio (with SUB/DUB toggle), speed, episode grid.
 * Real streams only (no demo fallback); signature kept identical so Nav call site is untouched.
 */
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
    val currentTracks by vm.tracks.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var maxEp by remember { mutableStateOf(12) }
    var isFullscreen by rememberSaveable { mutableStateOf(true) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var currentType by remember(animeId, episode) { mutableStateOf("sub") }

    var streamUrl by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var resolveError by remember(animeId, episode) { mutableStateOf(false) }
    var errorDetail by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var loadKey by remember { mutableStateOf(0) }
    var reloadKey by remember { mutableStateOf(0) }

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    var showTrackSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var resizeMode by rememberSaveable { mutableStateOf(0) } // 0 fit, 1 zoom, 2 fill

    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    var speed by remember { mutableFloatStateOf(1f) }

    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

    LaunchedEffect(animeId) {
        repository.detail(animeId)
            .onSuccess {
                title = it.title
                maxEp = (it.episodes ?: 12).coerceIn(1, 24)
            }
            .onFailure { resolveError = true }
    }

    LaunchedEffect(animeId, episode, title, reloadKey) {
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
                    setPlaybackSpeed(speed)
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

    fun applyResize() {
        val mode = when (resizeMode) {
            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
        runCatching { playerViewRef.value?.resizeMode = mode }
    }

    LaunchedEffect(resizeMode) { applyResize() }

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

    // Periodic progress save (kept identical to the working bare implementation).
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
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    // UI ticker: refresh play state / position and auto-hide idle controls.
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(300)
            runCatching {
                isPlaying = exoPlayer.isPlaying
                positionMs = exoPlayer.currentPosition
                durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            }
            if (isPlaying && controlsVisible &&
                System.currentTimeMillis() - lastInteraction > 3500L
            ) {
                controlsVisible = false
            }
        }
    }

    fun touchControls() {
        lastInteraction = System.currentTimeMillis()
        controlsVisible = true
    }

    fun seekBy(deltaMs: Long) {
        runCatching { exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs).coerceAtLeast(0)) }
    }

    BackHandler {
        when {
            showEpisodeSheet -> showEpisodeSheet = false
            showAudioSheet -> showAudioSheet = false
            showTrackSheet -> showTrackSheet = false
            showSpeedSheet -> showSpeedSheet = false
            locked -> locked = false
            isFullscreen -> isFullscreen = false
            else -> onBack()
        }
    }

    val trackState = currentTracks?.toTrackSelectionUiState() ?: TrackSelectionUiState()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!locked) {
                            lastInteraction = System.currentTimeMillis()
                            controlsVisible = !controlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        if (locked) return@detectTapGestures
                        val w = this.size.width
                        if (offset.x < w / 2) seekBy(-10_000) else seekBy(10_000)
                        lastInteraction = System.currentTimeMillis()
                        controlsVisible = true
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    playerViewRef.value = this
                    resizeMode = when (resizeMode) {
                        0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!locked) {
            // ---- Top bar --------------------------------------------------
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { touchControls(); onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(title, color = Color.White, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.episode_label, episode),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(
                                    if (currentType == "dub") Flame else Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    touchControls()
                                    currentType = if (currentType == "sub") "dub" else "sub"
                                    streamUrl = null
                                    resolveError = false
                                    loadKey++
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (currentType == "dub") stringResource(R.string.dub_type) else stringResource(R.string.sub_type),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { touchControls(); showEpisodeSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_source_24),
                                contentDescription = stringResource(R.string.episodes),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            touchControls()
                            locked = true
                            controlsVisible = false
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_lock_open_24),
                                contentDescription = stringResource(R.string.lock_controls),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { touchControls(); isFullscreen = !isFullscreen }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_fullscreen_24),
                                contentDescription = if (isFullscreen) stringResource(R.string.exit_fullscreen) else stringResource(R.string.enter_fullscreen),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ---- Center transport ----------------------------------------
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        touchControls()
                        if (episode > 1) onEpisodeChange(episode - 1)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_skip_previous_24),
                            contentDescription = stringResource(R.string.previous_episode),
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = { touchControls(); seekBy(-10_000) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_fast_rewind_24),
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = {
                        touchControls()
                        runCatching { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    IconButton(onClick = { touchControls(); seekBy(10_000) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_fast_forward_24),
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = {
                        touchControls()
                        if (episode < maxEp) onEpisodeChange(episode + 1)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_skip_next_24),
                            contentDescription = stringResource(R.string.next_episode),
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // ---- Bottom bar ----------------------------------------------
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Slider(
                        value = positionMs.toFloat().coerceAtLeast(0f),
                        onValueChange = { v ->
                            touchControls()
                            runCatching { exoPlayer.seekTo(v.toLong()) }
                        },
                        valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Flame,
                            activeTrackColor = Flame,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "• ${speed}x",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable { touchControls(); showSpeedSheet = true }
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { touchControls(); showTrackSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_subtitles_24),
                                contentDescription = stringResource(R.string.subtitles),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { touchControls(); showAudioSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_audiotrack_24),
                                contentDescription = stringResource(R.string.audio_tracks),
                                tint = if (currentType == "dub") Flame else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = {
                            touchControls()
                            runCatching { activity?.enterPictureInPictureMode() }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_picture_in_picture_alt_24),
                                contentDescription = stringResource(R.string.picture_in_picture),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = {
                            touchControls()
                            resizeMode = (resizeMode + 1) % 3
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_round_screen_rotation_alt_24),
                                contentDescription = "Resize",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---- Locked state: floating unlock ------------------------------
        if (locked) {
            IconButton(
                onClick = { locked = false; touchControls() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_round_lock_24),
                    contentDescription = stringResource(R.string.lock_controls),
                    tint = Color.White
                )
            }
        }

        // ---- Error overlay ----------------------------------------------
        if (playbackError != null || resolveError) {
            PlaybackErrorOverlay(
                detail = errorDetail,
                onRetry = {
                    vm.clearError()
                    resolveError = false
                    errorDetail = null
                    loadKey++
                    reloadKey++
                }
            )
        }
    }

    if (showTrackSheet) {
        SubtitleSheet(
            state = trackState,
            onSelect = { track ->
                runCatching { currentTracks?.let { exoPlayer.selectTrack(track, it) } }
                showTrackSheet = false
            },
            onSubtitlesOff = {
                runCatching { exoPlayer.disableSubtitles() }
                showTrackSheet = false
            },
            onDismiss = { showTrackSheet = false }
        )
    }

    if (showAudioSheet) {
        AudioSheet(
            state = trackState,
            currentType = currentType,
            onTypeChange = { next ->
                currentType = next
                streamUrl = null
                resolveError = false
                loadKey++
                showAudioSheet = false
            },
            onSelect = { track ->
                runCatching { currentTracks?.let { exoPlayer.selectTrack(track, it) } }
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
        )
    }

    if (showSpeedSheet) {
        SpeedSheet(
            speeds = speeds,
            current = speed,
            onSelect = { s ->
                speed = s
                runCatching { exoPlayer.setPlaybackSpeed(s) }
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showEpisodeSheet) {
        EpisodeSheet(
            current = episode,
            total = maxEp,
            onSelect = { ep ->
                if (ep != episode) onEpisodeChange(ep)
                showEpisodeSheet = false
            },
            onDismiss = { showEpisodeSheet = false }
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun PlaybackErrorOverlay(
    detail: String?,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.playback_error_title),
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.playback_error_message),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                detail,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .background(Flame, RoundedCornerShape(50))
                .clickable(onClick = onRetry)
                .padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(stringResource(R.string.retry), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SubtitleSheet(
    state: TrackSelectionUiState,
    onSelect: (SelectableTrack) -> Unit,
    onSubtitlesOff: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(title = stringResource(R.string.subtitles), onDismiss = onDismiss) {
        TrackRow(stringResource(R.string.subtitles_off), false, onSubtitlesOff)
        if (state.hasSubtitleOptions) {
            state.subtitleTracks.forEach { track ->
                TrackRow(track.label, track.isSelected) { onSelect(track) }
            }
        }
    }
}

@Composable
private fun AudioSheet(
    state: TrackSelectionUiState,
    currentType: String,
    onTypeChange: (String) -> Unit,
    onSelect: (SelectableTrack) -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(title = stringResource(R.string.audio_tracks), onDismiss = onDismiss) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.audio_tracks),
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            SubDubToggle(currentType = currentType, onTypeChange = onTypeChange)
        }
        Spacer(Modifier.height(8.dp))
        if (state.hasAudioOptions) {
            state.audioTracks.forEach { track ->
                TrackRow(track.label, track.isSelected) { onSelect(track) }
            }
        } else {
            Text(
                "No alternate audio tracks for this source",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SpeedSheet(
    speeds: List<Float>,
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(title = stringResource(R.string.playback_speed), onDismiss = onDismiss) {
        speeds.forEach { s ->
            TrackRow("${s}x", s == current) { onSelect(s) }
        }
    }
}

@Composable
private fun EpisodeSheet(
    current: Int,
    total: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(title = stringResource(R.string.episodes), onDismiss = onDismiss) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            items(total) { index ->
                val ep = index + 1
                Box(
                    Modifier
                        .padding(4.dp)
                        .background(
                            if (ep == current) Flame else Color.Black,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(ep) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        ep.toString(),
                        color = if (ep == current) Color.White else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(SurfaceRaised, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(20.dp)
        ) {
            Text(
                title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            content()
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.close), color = Flame)
            }
        }
    }
}

@Composable
private fun SubDubToggle(
    currentType: String,
    onTypeChange: (String) -> Unit
) {
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .background(
                    if (currentType != "dub") Flame else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .clickable { onTypeChange("sub") }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(stringResource(R.string.sub_type), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Box(
            Modifier
                .background(
                    if (currentType == "dub") Flame else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .clickable { onTypeChange("dub") }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(stringResource(R.string.dub_type), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) Flame else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Flame)
        }
    }
}
