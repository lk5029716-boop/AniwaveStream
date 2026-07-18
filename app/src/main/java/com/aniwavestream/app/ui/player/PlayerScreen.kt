package com.aniwavestream.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

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
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // CRASH FIX: ExoPlayer owned by a ViewModel so it survives rotation / fullscreen
    // recreation (the original "app closes itself on rotate" bug).
    val vm: PlayerViewModel = viewModel()
    val exoPlayer = vm.player
    val playbackError by vm.error.collectAsStateWithLifecycle()
    val currentTracks by vm.tracks.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var maxEp by remember { mutableStateOf(12) }
    // Open in landscape fullscreen by default (user asked for auto-rotate).
    var isFullscreen by rememberSaveable { mutableStateOf(true) }
    var isLocked by rememberSaveable { mutableStateOf(false) }

    // Stream-resolution state.
    var streamUrl by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var resolveError by remember(animeId, episode) { mutableStateOf(false) }
    var errorDetail by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var loadKey by remember { mutableStateOf(0) }

    // Server switcher (Dantotsu's exo_source): pick among backend servers.
    var servers by remember(animeId, episode) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var currentServerId by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var currentType by remember(animeId, episode) { mutableStateOf("sub") }

    // ---- Custom controller UI state ----
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }

    // Skip-intro availability (could be wired to AniSkip later).
    var showSkipIntro by remember { mutableStateOf(false) }

    // Brightness / volume gestures (Dantotsu-style).
    var brightness by remember { mutableFloatStateOf(-1f) }   // -1 = follow system
    var volume by remember { mutableFloatStateOf(-1f) }
    var showBrightness by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }

    LaunchedEffect(animeId) {
        repository.detail(animeId)
            .onSuccess {
                title = it.title
                maxEp = (it.episodes ?: 12).coerceIn(1, 24)
            }
            .onFailure { resolveError = true }
    }

    // Resolve servers + first stream. Dantotsu loads a server list then picks one;
    // we do the same so the Server button has real options.
    suspend fun loadStream(targetUrl: String? = null) {
        val url = targetUrl ?: withContext(Dispatchers.IO) {
            runCatching { AniwavesApi.resolveStream(title, episode, currentType) }.getOrNull()
        }
        if (!url.isNullOrBlank()) {
            streamUrl = url
            resolveError = false
        } else {
            streamUrl = null
            resolveError = true
            errorDetail = "Backend returned no stream for \"$title\" ep $episode ($currentType)"
        }
    }

    // Fetch server list for the current episode/type (for the server switcher).
    LaunchedEffect(animeId, episode, title, currentType) {
        if (title.isBlank()) return@LaunchedEffect
        val list = withContext(Dispatchers.IO) {
            runCatching { AniwavesApi.servers(resolveSlugSafe(title), episode, currentType) }.getOrNull()
                ?: runCatching { AniwavesApi.servers(fallbackSlug(title), episode, currentType) }.getOrNull()
                ?: emptyList()
        }
        servers = list
        if (currentServerId == null && list.isNotEmpty()) currentServerId = list.first().first
        // Initial resolve (preferred server order handled inside resolveStream).
        if (streamUrl == null && !resolveError) loadStream()
    }

    // Real stream load with retry + backoff (cold backend self-heal).
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
                exoPlayer.setMediaItem(PlayerModule.buildMediaItem(url))
                exoPlayer.prepare()
                exoPlayer.play()
            }
            val deadline = System.currentTimeMillis() + 25_000L
            while (System.currentTimeMillis() < deadline) {
                if (exoPlayer.playbackState == Player.STATE_READY) { ok = true; break }
                if (vm.error.value != null) break
                kotlinx.coroutines.delay(500)
            }
            if (!ok && vm.error.value == null) kotlinx.coroutines.delay(2500L * attempt)
        }
        if (!ok) {
            resolveError = true
            errorDetail = vm.error.value?.message
                ?: "ExoPlayer couldn't load the stream (timed out after $maxAttempts attempts)"
        }
    }

    // Apply playback speed whenever it changes.
    LaunchedEffect(speed) {
        runCatching { exoPlayer.setPlaybackSpeed(speed) }
    }

    // Lifecycle pause + keep-screen-on + restore orientation.
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

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            runCatching {
                val dur = exoPlayer.duration
                val pos = exoPlayer.currentPosition
                if (dur > 0) library.saveProgress(animeId, episode, pos.toFloat() / dur.toFloat())
            }
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Rotate on fullscreen (player kept alive).
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sync controller state from ExoPlayer (throttled to 300ms to stay cheap).
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

    fun toggleLock() {
        isLocked = !isLocked
        touchControls()
    }

    // Swipe up/down on left half = brightness, right half = volume (Dantotsu gesture).
    fun applyBrightness(v: Float) {
        val lp = activity?.window?.attributes ?: return
        lp.screenBrightness = v.coerceIn(0.05f, 1f)
        activity.window.attributes = lp
        brightness = v
        showBrightness = true
        scope.launch { delay(1500); showBrightness = false }
    }
    fun applyVolume(v: Float) {
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).roundToInt(), 0)
        volume = v
        showVolume = true
        scope.launch { delay(1500); showVolume = false }
    }

    BackHandler {
        when {
            isLocked -> Unit
            showEpisodeSheet -> showEpisodeSheet = false
            showAudioSheet -> showAudioSheet = false
            showSubSheet -> showSubSheet = false
            showServerSheet -> showServerSheet = false
            showSpeedSheet -> showSpeedSheet = false
            isFullscreen -> isFullscreen = false
            else -> onBack()
        }
    }

    // PiP (Dantotsu exo_pip).
    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder().build()
                activity.enterPictureInPictureMode(params)
            } else {
                @Suppress("DEPRECATION")
                activity.enterPictureInPictureMode()
            }
        }
    }

    val trackState = currentTracks?.toTrackSelectionUiState() ?: TrackSelectionUiState()
    val pipAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        (activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) ?: false)

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

        // Gesture surface: tap toggles controls; left/right swipe = brightness/volume.
        // Horizontal drag = seek (Dantotsu double-tap/scroll seek), vertical drag on
        // left half = brightness, right half = volume.
        if (!isLocked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { touchControls(); controlsVisible = !controlsVisible }
                        )
                    }
                    .pointerInput(Unit) {
                        val scopeWidth = size.width
                        var startB = brightness
                        var startV = volume
                        detectDragGestures(
                            onDragStart = { startB = brightness; startV = volume },
                            onDrag = { change, dragAmount ->
                                val x = change.position.x
                                // Horizontal drag => seek.
                                if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                    val new = (exoPlayer.currentPosition + dragAmount.x * 1000L)
                                        .coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
                                    runCatching { exoPlayer.seekTo(new) }
                                    touchControls()
                                } else {
                                    // Vertical drag => brightness (left) or volume (right).
                                    val dy = -dragAmount.y / 1000f
                                    if (startB < 0f) startB = (activity?.window?.attributes?.screenBrightness ?: 0.5f)
                                        .coerceIn(0.05f, 1f)
                                    if (startV < 0f) startV = (audioManager
                                        ?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f) /
                                        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f)
                                    if (x < scopeWidth / 2f) {
                                        applyBrightness((startB + dy).coerceIn(0.05f, 1f))
                                    } else {
                                        applyVolume((startV + dy).coerceIn(0f, 1f))
                                    }
                                }
                            }
                        )
                    }
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { touchControls(); controlsVisible = !controlsVisible }
            )
        }

        // Skip-intro pill (Dantotsu exo_skip_op_ed).
        AnimatedVisibility(
            visible = showSkipIntro && controlsVisible,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 16.dp),
            enter = fadeIn(), exit = fadeOut()
        ) {
            Box(
                Modifier
                    .background(OrangePrimary, RoundedCornerShape(50))
                    .clickable { runCatching { exoPlayer.seekTo(exoPlayer.currentPosition + 85_000L) } }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) { Text(stringResource(R.string.skip_intro), color = Color.White, fontWeight = FontWeight.Bold) }
        }

        // Gesture indicators (brightness / volume), Dantotsu-style vertical bars.
        if (showBrightness) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 48.dp)) {
                GestureIndicator(stringResource(R.string.brightness), brightness.coerceIn(0.05f, 1f))
            }
        }
        if (showVolume) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)) {
                GestureIndicator(stringResource(R.string.volume), volume.coerceIn(0f, 1f))
            }
        }

        // ---- Top bar: back / episodes / server / audio / subtitles / settings / speed / rotate / pip / lock ----
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(), exit = fadeOut()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { touchControls() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { touchControls(); onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
                Column(
                    Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text(title.ifBlank { stringResource(R.string.loading) }, color = Color.White, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.episode_label, episode), color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .background(
                                    if (currentType == "dub") OrangePrimary else Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    touchControls()
                                    currentType = if (currentType == "sub") "dub" else "sub"
                                    currentServerId = null
                                    streamUrl = null
                                    resolveError = false
                                    loadKey++
                                }
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                if (currentType == "dub") stringResource(R.string.dub_type) else stringResource(R.string.sub_type),
                                color = Color.White, style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                // Action buttons in a horizontal-scroll row so they never overflow /
                // clip on narrow landscape screens (the "action tabs failed" symptom).
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                IconButton(onClick = { touchControls(); showEpisodeSheet = true }) {
                    Icon(Icons.Filled.List, contentDescription = stringResource(R.string.episodes), tint = Color.White)
                }
                IconButton(onClick = { touchControls(); showServerSheet = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.server), tint = Color.White)
                }
                IconButton(onClick = { touchControls(); showAudioSheet = true }) {
                    Icon(Icons.Filled.Audiotrack, contentDescription = stringResource(R.string.audio_tracks), tint = Color.White)
                }
                IconButton(onClick = { touchControls(); showSubSheet = true }) {
                    Icon(Icons.Filled.Subtitles, contentDescription = stringResource(R.string.subtitles), tint = Color.White)
                }
                IconButton(onClick = { touchControls(); showSpeedSheet = true }) {
                    Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.playback_speed), tint = Color.White)
                }
                IconButton(onClick = { touchControls(); isFullscreen = !isFullscreen }) {
                    Icon(
                        if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (isFullscreen) stringResource(R.string.exit_fullscreen) else stringResource(R.string.enter_fullscreen),
                        tint = Color.White
                    )
                }
                if (pipAvailable) {
                    IconButton(onClick = { touchControls(); enterPip() }) {
                        Icon(Icons.Filled.PictureInPicture, contentDescription = stringResource(R.string.picture_in_picture), tint = Color.White)
                    }
                }
                IconButton(onClick = { touchControls(); toggleLock() }) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.lock_controls), tint = Color.White)
                }
                }
            }
        }

        // ---- Bottom bar: prev / play-pause / next / seek ----
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(), exit = fadeOut()
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
                        val prev = (episode - 1).coerceAtLeast(1)
                        if (prev != episode) onEpisodeChange(prev)
                    }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous), tint = Color.White)
                    }
                    IconButton(onClick = {
                        touchControls()
                        runCatching { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp)
                        )
                    }
                    IconButton(onClick = {
                        touchControls()
                        val next = (episode + 1).coerceAtMost(maxEp)
                        if (next != episode) onEpisodeChange(next)
                    }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next), tint = Color.White)
                    }
                    // Double-tap-style quick skip buttons (Dantotsu exo_fast_rewind/forward).
                    IconButton(onClick = { touchControls(); runCatching { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0)) } }) {
                        Icon(Icons.Filled.FastRewind, contentDescription = "Rewind 10s", tint = Color.White)
                    }
                    IconButton(onClick = { touchControls(); runCatching { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000) } }) {
                        Icon(Icons.Filled.FastForward, contentDescription = "Forward 10s", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${formatTime(positionMs)} / ${formatTime(durationMs)}", color = Color.White, style = MaterialTheme.typography.labelMedium)
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
                }
            )
        }
    }

    if (showAudioSheet) {
        AudioSheet(
            state = trackState,
            onSelect = { track ->
                runCatching { currentTracks?.let { exoPlayer.selectTrack(track, it) } }
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
        )
    }

    if (showSubSheet) {
        SubtitleSheet(
            state = trackState,
            onSelect = { track ->
                runCatching { currentTracks?.let { exoPlayer.selectTrack(track, it) } }
                showSubSheet = false
            },
            onSubtitlesOff = {
                runCatching { exoPlayer.disableSubtitles() }
                showSubSheet = false
            },
            onDismiss = { showSubSheet = false }
        )
    }

    if (showServerSheet) {
        ServerSheet(
            servers = servers,
            currentServerId = currentServerId,
            onSelect = { id ->
                currentServerId = id
                showServerSheet = false
                scope.launch {
                    val url = withContext(Dispatchers.IO) {
                        runCatching { AniwavesApi.streamUrl(id) }.getOrNull()
                    }
                    loadStream(url)
                }
            },
            onDismiss = { showServerSheet = false }
        )
    }

    if (showSpeedSheet) {
        SpeedSheet(
            current = speed,
            onSelect = { s -> speed = s; showSpeedSheet = false },
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

// ---- helpers for server slug (kept local to avoid touching AniwavesApi signature) ----
private suspend fun resolveSlugSafe(title: String): String? =
    withContext(Dispatchers.IO) { runCatching { AniwavesApi.resolveSlug(title) }.getOrNull() }

private fun fallbackSlug(title: String): String =
    title.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun GestureIndicator(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .height(120.dp)
                .width(6.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height((120 * value).dp)
                    .background(OrangePrimary, RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PlaybackErrorOverlay(detail: String? = null, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.playback_error_title), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.playback_error_message), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.width(12.dp))
            Text(detail, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier.padding(top = 20.dp).background(OrangePrimary, RoundedCornerShape(50))
                .clickable(onClick = onRetry).padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(stringResource(R.string.retry), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AudioSheet(state: TrackSelectionUiState, onSelect: (SelectableTrack) -> Unit, onDismiss: () -> Unit) {
    BottomSheetBase(title = stringResource(R.string.audio_tracks), onDismiss = onDismiss) {
        if (state.hasAudioOptions) {
            state.audioTracks.forEach { track ->
                TrackRow(track.label, track.isSelected) { onSelect(track) }
            }
        } else {
            Text("No alternate audio tracks for this source", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SubtitleSheet(state: TrackSelectionUiState, onSelect: (SelectableTrack) -> Unit, onSubtitlesOff: () -> Unit, onDismiss: () -> Unit) {
    BottomSheetBase(title = stringResource(R.string.subtitles), onDismiss = onDismiss) {
        TrackRow(stringResource(R.string.subtitles_off), false, onSubtitlesOff)
        if (state.hasSubtitleOptions) {
            state.subtitleTracks.forEach { track -> TrackRow(track.label, track.isSelected) { onSelect(track) } }
        }
    }
}

@Composable
private fun ServerSheet(servers: List<Pair<String, String>>, currentServerId: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    BottomSheetBase(title = stringResource(R.string.switch_server), onDismiss = onDismiss) {
        if (servers.isEmpty()) {
            Text(stringResource(R.string.no_servers), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            servers.forEach { (id, name) ->
                TrackRow(name, id == currentServerId) { onSelect(id) }
            }
        }
    }
}

@Composable
private fun SpeedSheet(current: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    BottomSheetBase(title = stringResource(R.string.playback_speed), onDismiss = onDismiss) {
        speeds.forEach { s ->
            TrackRow("${if (s == 1f) "Normal" else s}x", s == current) { onSelect(s) }
        }
    }
}

@Composable
private fun EpisodeSheet(current: Int, total: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    BottomSheetBase(title = stringResource(R.string.episodes), onDismiss = onDismiss) {
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 48.dp), modifier = Modifier.heightIn(max = 360.dp)) {
            items(total) { index ->
                val ep = index + 1
                Box(
                    Modifier.padding(4.dp)
                        .background(if (ep == current) OrangePrimary else Background, RoundedCornerShape(8.dp))
                        .clickable { onSelect(ep) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ep.toString(), color = if (ep == current) Color.White else TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun BottomSheetBase(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(SurfaceElevated, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(20.dp)
        ) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            content()
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
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) OrangePrimary else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Filled.Subtitles, contentDescription = null, tint = OrangePrimary)
    }
}
