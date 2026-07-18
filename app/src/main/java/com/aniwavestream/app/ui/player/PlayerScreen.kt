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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import com.aniwavestream.app.R
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.api.AniwavesApi
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.player.PlayerModule
import com.aniwavestream.app.player.PlayerViewModel
import com.aniwavestream.app.player.SelectableTrack
import com.aniwavestream.app.player.TrackSelectionUiState
import com.aniwavestream.app.player.disableSubtitles
import com.aniwavestream.app.player.selectTrack
import com.aniwavestream.app.player.toTrackSelectionUiState
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
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
    val currentTracks by vm.tracks.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var maxEp by remember { mutableStateOf(12) }
    var isFullscreen by rememberSaveable { mutableStateOf(true) }
    var showTrackSheet by remember { mutableStateOf(false) }
    // Sub/Dub chooser (Dantotsu-style): re-resolves the stream for the chosen type.
    var currentType by remember(animeId, episode) { mutableStateOf("sub") }

    var streamUrl by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var resolveError by remember(animeId, episode) { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var errorDetail by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var loadKey by remember { mutableStateOf(0) }

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }

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
                ?: "Backend returned no stream for \"$title\" ep $episode"
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
                val p = exoPlayer
                if (p.playbackState == androidx.media3.common.Player.STATE_READY) { ok = true; break }
                if (vm.error.value != null) break
                kotlinx.coroutines.delay(500)
            }
            if (!ok && vm.error.value == null) {
                kotlinx.coroutines.delay(2500L * attempt)
            }
        }
        if (!ok) {
            resolveError = true
            errorDetail = vm.error.value?.message
                ?: "ExoPlayer couldn't load the stream (timed out after $maxAttempts attempts)"
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> runCatching { exoPlayer.pause() }
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

    BackHandler {
        when {
            showEpisodeSheet -> showEpisodeSheet = false
            showAudioSheet -> showAudioSheet = false
            showTrackSheet -> showTrackSheet = false
            isFullscreen -> isFullscreen = false
            else -> onBack()
        }
    }

    val trackState = currentTracks?.toTrackSelectionUiState() ?: TrackSelectionUiState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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

        Box(
            Modifier
                .fillMaxSize()
                .clickable {
                    lastInteraction = System.currentTimeMillis()
                    controlsVisible = !controlsVisible
                }
        ) {}

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
                    .clickable { touchControls() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
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
                    Text(title, color = Color.White, maxLines = 1)
                    Text(
                        stringResource(R.string.episode_label, episode),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                // SUB / DUB chooser (Dantotsu-style).
                Box(
                    Modifier
                        .background(
                            if (currentType == "dub") OrangePrimary else Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            touchControls()
                            currentType = if (currentType == "sub") "dub" else "sub"
                            streamUrl = null
                            resolveError = false
                            loadKey++
                        }
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        if (currentType == "dub") stringResource(R.string.dub_type) else stringResource(R.string.sub_type),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = { touchControls(); showEpisodeSheet = true }) {
                    Icon(Icons.Filled.List, contentDescription = "Episodes", tint = Color.White)
                }
                IconButton(onClick = { touchControls(); showAudioSheet = true }) {
                    Icon(
                        Icons.Filled.Audiotrack,
                        contentDescription = if (currentType == "dub") "Audio (DUB)" else "Audio (SUB)",
                        tint = if (currentType == "dub") OrangePrimary else Color.White
                    )
                }
                IconButton(onClick = { touchControls(); showTrackSheet = true }) {
                    Icon(
                        Icons.Filled.Subtitles,
                        contentDescription = stringResource(R.string.audio_and_subtitles),
                        tint = Color.White
                    )
                }
                IconButton(onClick = { touchControls(); isFullscreen = !isFullscreen }) {
                    Icon(
                        if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (isFullscreen) "Portrait" else "Landscape",
                        tint = Color.White
                    )
                }
                }
            }
        }

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
                    .clickable { touchControls() }
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
                        thumbColor = OrangePrimary,
                        activeTrackColor = OrangePrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        touchControls()
                        runCatching { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

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
            },
            onSelect = { track ->
                runCatching { currentTracks?.let { exoPlayer.selectTrack(track, it) } }
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
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
    detail: String? = null,
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
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.playback_error_message),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.width(12.dp))
            Text(
                detail,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier
                .padding(top = 20.dp)
                .background(OrangePrimary, RoundedCornerShape(50))
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
                .background(SurfaceElevated, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.subtitles),
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            TrackRow(stringResource(R.string.subtitles_off), false, onSubtitlesOff)
            if (state.hasSubtitleOptions) {
                state.subtitleTracks.forEach { track ->
                    TrackRow(track.label, track.isSelected) { onSelect(track) }
                }
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.close), color = OrangePrimary)
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
                .background(SurfaceElevated, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(20.dp)
        ) {
            Text(
                "Audio",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.audio_tracks),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                SubDubToggle(
                    currentType = currentType,
                    onTypeChange = onTypeChange
                )
            }
            Spacer(Modifier.width(8.dp))
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
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.close), color = OrangePrimary)
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
                    if (currentType != "dub") OrangePrimary else Color.Transparent,
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
                    if (currentType == "dub") OrangePrimary else Color.Transparent,
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
private fun EpisodeSheet(
    current: Int,
    total: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
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
                .background(SurfaceElevated, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(20.dp)
        ) {
            Text(
                "Episodes",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
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
                                if (ep == current) OrangePrimary else Background,
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
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.close), color = OrangePrimary)
            }
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
            color = if (selected) OrangePrimary else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Filled.Subtitles,
                contentDescription = null,
                tint = OrangePrimary
            )
        }
    }
}
