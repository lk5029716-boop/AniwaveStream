package com.aniwavestream.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
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

    // CRASH FIX (Rule 9c done right): the ExoPlayer is owned by a ViewModel,
    // so it is NOT rebuilt/released when the Activity is recreated on rotation
    // or fullscreen toggle. Previously the player was released on every dispose,
    // then touched again -> IllegalStateException -> app auto-closed.
    val vm: PlayerViewModel = viewModel()
    val exoPlayer = vm.player
    val playbackError by vm.error.collectAsStateWithLifecycle()
    val currentTracks by vm.tracks.collectAsStateWithLifecycle()

    // Start blank so the resolve effect below waits for the REAL title from
    // repository.detail() instead of firing immediately with the "Loading"
    // placeholder (which would never match a backend slug and always fail).
    var title by remember { mutableStateOf("") }
    var maxEp by remember { mutableStateOf(12) }
    // Survives rotation so we don't toggle orientation twice and fight the system.
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }

    // Stream-resolution state (declared before the effects that read it).
    var streamUrl by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var resolveError by remember(animeId, episode) { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(animeId) {
        repository.detail(animeId)
            .onSuccess {
                title = it.title
                maxEp = (it.episodes ?: 12).coerceIn(1, 24)
            }
            .onFailure { resolveError = true }   // can't fetch title -> nothing to resolve
    }

    // Real stream resolution: hit the self-hosted Aniwaves API (vidplay / bymas) for
    // this anime's title + episode. NO demo fallback -- if it can't resolve we show
    // a real error overlay (retry), never a fake sample video.
    // CRITICAL FIX: AniwavesApi.resolveStream() performs BLOCKING network I/O
    // (OkHttp .execute()). Calling it on the main thread triggers Android's
    // default NetworkOnMainThread detection (active in debug builds), which
    // throws and is swallowed by runCatching -> null -> "Playback hit a snag"
    // without ExoPlayer ever being touched ("it don't even try"). Move the work
    // to Dispatchers.IO and only then set the resolved URL / error state.
    LaunchedEffect(animeId, episode, title, reloadKey) {
        if (title.isBlank()) return@LaunchedEffect
        resolveError = false
        val resolved = withContext(Dispatchers.IO) {
            runCatching { AniwavesApi.warmUp() }              // wake free-tier backend
            runCatching { AniwavesApi.resolveStream(title, episode) }.getOrNull()
        }
        if (!resolved.isNullOrBlank()) { streamUrl = resolved; resolveError = false }
        else { streamUrl = null; resolveError = true }
    }

    // All player mutations are guarded: if the player was released by a
    // lifecycle/teardown race, touching it must NOT throw and force-close the
    // whole app (the original "app closes itself" bug).
    fun safePlayer(block: Player.() -> Unit) {
        runCatching { exoPlayer.block() }
    }

    // ExoPlayer load with automatic retry+backoff. A cold backend (Render free
    // tier) can take >30s to wake; the first prepare() may snag. Retrying a few
    // times with delay lets it self-heal instead of showing a permanent error.
    LaunchedEffect(streamUrl) {
        val url = streamUrl ?: return@LaunchedEffect
        vm.clearError()
        var attempt = 0
        val maxAttempts = 3
        while (attempt < maxAttempts) {
            attempt++
            val ok = runCatching {
                safePlayer {
                    setMediaItem(PlayerModule.buildMediaItem(url))
                    prepare()
                    play()
                }
            }.isSuccess
            // If ExoPlayer reports an error this soon, wait and retry.
            if (vm.error.value == null) break
            if (attempt < maxAttempts) kotlinx.coroutines.delay(2500L * attempt)
        }
    }

    // Rule 9c: lifecycle-aware pause so backgrounding never crashes.
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

    // Progress persistence (guarded against any transient player state).
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

    // Keep screen on during playback.
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
            // Restore orientation on exit.
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Rule 9c: react to fullscreen toggle by rotating natively (player kept alive).
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        when {
            showTrackSheet -> showTrackSheet = false
            isFullscreen -> isFullscreen = false
            else -> onBack()
        }
    }

    val trackState = currentTracks?.toTrackSelectionUiState() ?: TrackSelectionUiState()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedVisibility(visible = !isFullscreen) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TextPrimary
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, maxLines = 1)
                    Text(
                        stringResource(R.string.episode_label, episode),
                        color = TextSecondary
                    )
                }
                if (trackState.hasAudioOptions || trackState.hasSubtitleOptions) {
                    IconButton(onClick = { showTrackSheet = true }) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = stringResource(R.string.audio_and_subtitles),
                            tint = OrangePrimary
                        )
                    }
                }
                IconButton(
                    onClick = { if (episode > 1) onEpisodeChange(episode - 1) },
                    enabled = episode > 1
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_episode),
                        tint = OrangePrimary
                    )
                }
                IconButton(
                    onClick = { if (episode < maxEp) onEpisodeChange(episode + 1) },
                    enabled = episode < maxEp
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.next_episode),
                        tint = OrangePrimary
                    )
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                        setShowSubtitleButton(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = stringResource(
                        if (isFullscreen) R.string.exit_fullscreen else R.string.enter_fullscreen
                    ),
                    tint = Color.White
                )
            }

            if (playbackError != null || resolveError) {
                PlaybackErrorOverlay(
                    onRetry = {
                        vm.clearError()
                        if (streamUrl != null) {
                            safePlayer {
                                setMediaItem(PlayerModule.buildMediaItem(streamUrl!!))
                                prepare()
                                play()
                            }
                        } else {
                            // resolution had failed: re-run the resolve LaunchedEffect
                            resolveError = false
                            reloadKey++
                        }
                    }
                )
            }
        }

        AnimatedVisibility(visible = !isFullscreen) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    if (episode < maxEp) onEpisodeChange(episode + 1)
                }) {
                    Text(stringResource(R.string.next_episode), color = OrangePrimary)
                }
            }
        }
    }

    if (showTrackSheet) {
        TrackSelectionSheet(
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
}

@Composable
private fun PlaybackErrorOverlay(onRetry: () -> Unit) {
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
private fun TrackSelectionSheet(
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
            if (state.hasAudioOptions) {
                Text(
                    stringResource(R.string.audio_tracks),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                state.audioTracks.forEach { track ->
                    TrackRow(track.label, track.isSelected) { onSelect(track) }
                }
                Spacer(Modifier.width(16.dp))
            }
            if (state.hasSubtitleOptions) {
                Text(
                    stringResource(R.string.subtitles),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                TrackRow(stringResource(R.string.subtitles_off), false, onSubtitlesOff)
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
