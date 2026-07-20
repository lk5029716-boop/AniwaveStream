package com.aniwavestream.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aniwavestream.app.player.clearVideoOverrides
import com.aniwavestream.app.player.disableSubtitles
import com.aniwavestream.app.player.enableSubtitles
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
import kotlin.math.roundToInt

/**
 * Premium streaming player — Netflix / Crunchyroll inspired.
 *
 * Layout map (research-backed):
 *  TOP (gradient): back · title/ep · SUB|DUB · lock · fullscreen
 *  CENTER: prev ep · −10 · big play/pause · +10 · next ep
 *  BOTTOM (gradient): thin accent scrubber · time · icon row
 *    (subs · audio · quality · speed · episodes · PiP · aspect)
 *  EDGE gestures: left = brightness, right = volume, horizontal = scrub
 *  Double-tap L/R: ±10s with Netflix-style pulse badge
 *  CR-style Skip Intro pill + Next Episode near end
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
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }

    val vm: PlayerViewModel = viewModel()
    val exoPlayer = vm.player
    val playbackError by vm.error.collectAsStateWithLifecycle()
    val currentTracks by vm.tracks.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var maxEp by remember { mutableIntStateOf(12) }
    var isFullscreen by rememberSaveable { mutableStateOf(true) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var currentType by remember(animeId, episode) { mutableStateOf("sub") }

    var streamUrl by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var resolveError by remember(animeId, episode) { mutableStateOf(false) }
    var errorDetail by remember(animeId, episode) { mutableStateOf<String?>(null) }
    var loadKey by remember { mutableIntStateOf(0) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var isBuffering by remember { mutableStateOf(true) }

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }

    var showSettings by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var resizeMode by rememberSaveable { mutableIntStateOf(0) }

    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    var speed by remember { mutableFloatStateOf(1f) }

    // Double-tap seek flash
    var seekFlash by remember { mutableStateOf<SeekFlash?>(null) }

    // Edge gesture HUD
    var gestureHud by remember { mutableStateOf<GestureHud?>(null) }

    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

    // Brightness 0..1 (window)
    var brightness by remember {
        mutableFloatStateOf(
            runCatching {
                val raw = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                (raw / 255f).coerceIn(0.05f, 1f)
            }.getOrDefault(0.5f)
        )
    }

    fun setWindowBrightness(value: Float) {
        brightness = value.coerceIn(0.05f, 1f)
        activity?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = brightness
            w.attributes = lp
        }
    }

    fun volumeFraction(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    fun setVolumeFraction(f: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val v = (f.coerceIn(0f, 1f) * max).roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
    }

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
        isBuffering = true
        val result = withContext(Dispatchers.IO) {
            runCatching { AniwavesApi.resolveStream(title, episode, currentType) }
        }
        if (result.isSuccess && !result.getOrNull().isNullOrBlank()) {
            streamUrl = result.getOrNull()
            resolveError = false
        } else {
            streamUrl = null
            resolveError = true
            isBuffering = false
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
        isBuffering = true
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
                val ready = runCatching {
                    exoPlayer.playbackState == Player.STATE_READY
                }.getOrElse { false }
                if (ready) {
                    ok = true
                    break
                }
                if (vm.error.value != null) break
                delay(500)
            }
            if (!ok && vm.error.value == null) {
                delay(2500L * attempt)
            }
        }
        isBuffering = false
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
            activity?.window?.let { w ->
                val lp = w.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = lp
            }
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
            delay(250)
            runCatching {
                isPlaying = exoPlayer.isPlaying
                positionMs = exoPlayer.currentPosition
                durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                bufferedMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                val state = exoPlayer.playbackState
                isBuffering = state == Player.STATE_BUFFERING ||
                    (state == Player.STATE_IDLE && streamUrl != null && !resolveError)
            }
            val sheetOpen = showSettings || showTrackSheet || showAudioSheet ||
                showQualitySheet || showEpisodeSheet || showSpeedSheet
            if (isPlaying && controlsVisible && !sheetOpen &&
                System.currentTimeMillis() - lastInteraction > 3500L
            ) {
                controlsVisible = false
            }
        }
    }

    // Auto-dismiss seek flash
    LaunchedEffect(seekFlash) {
        if (seekFlash != null) {
            delay(650)
            seekFlash = null
        }
    }

    LaunchedEffect(gestureHud) {
        if (gestureHud != null) {
            delay(900)
            gestureHud = null
        }
    }

    fun touchControls() {
        lastInteraction = System.currentTimeMillis()
        controlsVisible = true
    }

    fun seekBy(deltaMs: Long) {
        runCatching {
            exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs).coerceAtLeast(0))
        }
    }

    fun seekTo(ms: Long) {
        runCatching { exoPlayer.seekTo(ms.coerceAtLeast(0)) }
    }

    val anySheet = showSettings || showTrackSheet || showAudioSheet ||
        showQualitySheet || showEpisodeSheet || showSpeedSheet

    BackHandler {
        when {
            showEpisodeSheet -> showEpisodeSheet = false
            showAudioSheet -> showAudioSheet = false
            showTrackSheet -> showTrackSheet = false
            showQualitySheet -> showQualitySheet = false
            showSpeedSheet -> showSpeedSheet = false
            showSettings -> showSettings = false
            locked -> locked = false
            isFullscreen -> isFullscreen = false
            else -> onBack()
        }
    }

    val trackState = currentTracks?.toTrackSelectionUiState() ?: TrackSelectionUiState()

    // CR-style skip intro: show roughly during early credits window
    val showSkipIntro = durationMs > 0 &&
        positionMs in 5_000L..90_000L &&
        !locked &&
        !resolveError

    // Next episode CTA in last 45s
    val showNextCta = durationMs > 0 &&
        episode < maxEp &&
        positionMs >= (durationMs - 45_000L) &&
        !locked

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ---- Video surface ------------------------------------------------
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
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

        // ---- Gesture layer ------------------------------------------------
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked, anySheet) {
                    if (locked || anySheet) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            lastInteraction = System.currentTimeMillis()
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            val w = size.width.toFloat()
                            val left = offset.x < w / 2f
                            if (left) {
                                seekBy(-10_000)
                                seekFlash = SeekFlash(left = true)
                            } else {
                                seekBy(10_000)
                                seekFlash = SeekFlash(left = false)
                            }
                            touchControls()
                        }
                    )
                }
                .pointerInput(locked, anySheet) {
                    if (locked || anySheet) return@pointerInput
                    // Brightness (left half) / volume (right half)
                    var startValue = 0f
                    var isLeft = true
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isLeft = offset.x < size.width / 2f
                            startValue = if (isLeft) brightness else volumeFraction()
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val h = size.height.coerceAtLeast(1).toFloat()
                            // Drag up increases brightness/volume
                            val next = (startValue - dragAmount / h).coerceIn(0f, 1f)
                            startValue = next
                            if (isLeft) {
                                setWindowBrightness(next)
                                gestureHud = GestureHud.Brightness(next)
                            } else {
                                setVolumeFraction(next)
                                gestureHud = GestureHud.Volume(next)
                            }
                        }
                    )
                }
                .pointerInput(locked, anySheet, durationMs) {
                    if (locked || anySheet || durationMs <= 0) return@pointerInput
                    var startX = 0f
                    var startPos = 0L
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            startPos = positionMs
                            touchControls()
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val w = size.width.coerceAtLeast(1)
                            val frac = (change.position.x - startX) / w
                            // Full width ≈ 90s scrub sensitivity
                            val delta = (frac * 90_000).toLong()
                            val target = (startPos + delta).coerceIn(0L, durationMs)
                            positionMs = target
                            gestureHud = GestureHud.Scrub(target, durationMs)
                        },
                        onDragEnd = {
                            seekTo(positionMs)
                            gestureHud = null
                        },
                        onDragCancel = { gestureHud = null }
                    )
                }
        )

        // Double-tap seek badges (Netflix style)
        SeekFlashOverlay(seekFlash)

        // Brightness / volume / scrub HUD
        GestureHudOverlay(gestureHud)

        // Buffering
        if (isBuffering && playbackError == null && !resolveError) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = Flame,
                strokeWidth = 3.dp
            )
        }

        if (!locked) {
            // Cinematic top + bottom gradients always under chrome
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Top vignette
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.85f),
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    // Bottom vignette
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                }
            }

            // ---- TOP BAR --------------------------------------------------
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                enter = fadeIn() + slideInVertically { -it / 3 },
                exit = fadeOut() + slideOutVertically { -it / 3 }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            title.ifBlank { "…" },
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            )
                        )
                        Text(
                            stringResource(R.string.episode_label, episode),
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    SubDubPill(
                        currentType = currentType,
                        onToggle = {
                            touchControls()
                            currentType = if (currentType == "sub") "dub" else "sub"
                            streamUrl = null
                            resolveError = false
                            loadKey++
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        touchControls()
                        locked = true
                        controlsVisible = false
                    }) {
                        Icon(
                            Icons.Filled.LockOpen,
                            contentDescription = stringResource(R.string.lock_controls),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        touchControls()
                        isFullscreen = !isFullscreen
                    }) {
                        Icon(
                            if (isFullscreen) Icons.Outlined.FullscreenExit
                            else Icons.Outlined.Fullscreen,
                            contentDescription = if (isFullscreen) {
                                stringResource(R.string.exit_fullscreen)
                            } else {
                                stringResource(R.string.enter_fullscreen)
                            },
                            tint = Color.White
                        )
                    }
                }
            }

            // ---- CENTER TRANSPORT (Netflix) -------------------------------
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransportIcon(
                        icon = Icons.Filled.SkipPrevious,
                        size = 36.dp,
                        enabled = episode > 1,
                        contentDescription = stringResource(R.string.previous_episode)
                    ) {
                        touchControls()
                        if (episode > 1) onEpisodeChange(episode - 1)
                    }
                    TransportIcon(
                        icon = Icons.Filled.Replay10,
                        size = 40.dp,
                        contentDescription = stringResource(R.string.rewind_10)
                    ) {
                        touchControls()
                        seekBy(-10_000)
                        seekFlash = SeekFlash(left = true)
                    }
                    // Big play/pause circle
                    Box(
                        Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable {
                                touchControls()
                                runCatching {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    TransportIcon(
                        icon = Icons.Filled.Forward10,
                        size = 40.dp,
                        contentDescription = stringResource(R.string.forward_10)
                    ) {
                        touchControls()
                        seekBy(10_000)
                        seekFlash = SeekFlash(left = false)
                    }
                    TransportIcon(
                        icon = Icons.Filled.SkipNext,
                        size = 36.dp,
                        enabled = episode < maxEp,
                        contentDescription = stringResource(R.string.next_episode)
                    ) {
                        touchControls()
                        if (episode < maxEp) onEpisodeChange(episode + 1)
                    }
                }
            }

            // ---- BOTTOM BAR -----------------------------------------------
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Time row
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatTime(positionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (durationMs > 0) {
                                "−${formatTime((durationMs - positionMs).coerceAtLeast(0))}"
                            } else {
                                "−−:−−"
                            },
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Custom Netflix-thin scrubber
                    PremiumScrubber(
                        position = positionMs.toFloat(),
                        buffered = bufferedMs.toFloat(),
                        duration = (if (durationMs > 0) durationMs else 1L).toFloat(),
                        onScrub = { v ->
                            touchControls()
                            positionMs = v.toLong()
                        },
                        onScrubFinished = { v ->
                            seekTo(v.toLong())
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    // Icon row — CR / Netflix secondary controls
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomAction(
                            icon = Icons.Outlined.ClosedCaption,
                            label = stringResource(R.string.subtitles),
                            tint = if (trackState.subtitleTracks.any { it.isSelected }) Flame else Color.White
                        ) {
                            touchControls()
                            showTrackSheet = true
                        }
                        BottomAction(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = stringResource(R.string.audio_tracks),
                            tint = if (currentType == "dub") Flame else Color.White
                        ) {
                            touchControls()
                            showAudioSheet = true
                        }
                        BottomAction(
                            icon = Icons.Outlined.HighQuality,
                            label = stringResource(R.string.quality)
                        ) {
                            touchControls()
                            showQualitySheet = true
                        }
                        BottomAction(
                            icon = Icons.Outlined.Speed,
                            label = if (speed == 1f) stringResource(R.string.speed) else "${speed}x",
                            tint = if (speed != 1f) Flame else Color.White
                        ) {
                            touchControls()
                            showSpeedSheet = true
                        }
                        BottomAction(
                            icon = Icons.Outlined.ViewAgenda,
                            label = stringResource(R.string.episodes)
                        ) {
                            touchControls()
                            showEpisodeSheet = true
                        }
                        BottomAction(
                            icon = Icons.Outlined.PictureInPictureAlt,
                            label = stringResource(R.string.pip_short)
                        ) {
                            touchControls()
                            runCatching { activity?.enterPictureInPictureMode() }
                        }
                        BottomAction(
                            icon = Icons.Outlined.AspectRatio,
                            label = when (resizeMode) {
                                0 -> stringResource(R.string.fit)
                                1 -> stringResource(R.string.zoom)
                                else -> stringResource(R.string.fill)
                            }
                        ) {
                            touchControls()
                            resizeMode = (resizeMode + 1) % 3
                        }
                        BottomAction(
                            icon = Icons.Filled.Settings,
                            label = stringResource(R.string.settings)
                        ) {
                            touchControls()
                            showSettings = true
                        }
                    }
                }
            }

            // Skip Intro (Crunchyroll orange pill)
            AnimatedVisibility(
                visible = showSkipIntro,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = if (controlsVisible) 118.dp else 28.dp),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Flame)
                        .clickable {
                            touchControls()
                            seekTo(90_000L.coerceAtMost(durationMs))
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        stringResource(R.string.skip_intro),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Next episode CTA
            AnimatedVisibility(
                visible = showNextCta,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = if (controlsVisible) 118.dp else 28.dp),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                if (showNextCta && !showSkipIntro) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable {
                                touchControls()
                                onEpisodeChange(episode + 1)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.next_episode),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = null,
                            tint = Flame,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Locked unlock chip
        if (locked) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .clickable {
                        locked = false
                        touchControls()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.lock_controls),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.tap_to_unlock),
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

    // ---- Sheets -----------------------------------------------------------
    if (showSettings) {
        SettingsSheet(
            speed = speed,
            resizeMode = resizeMode,
            currentType = currentType,
            trackState = trackState,
            onSpeed = { showSettings = false; showSpeedSheet = true },
            onQuality = { showSettings = false; showQualitySheet = true },
            onSubs = { showSettings = false; showTrackSheet = true },
            onAudio = { showSettings = false; showAudioSheet = true },
            onResizeCycle = { resizeMode = (resizeMode + 1) % 3 },
            onType = { next ->
                currentType = next
                streamUrl = null
                resolveError = false
                loadKey++
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showTrackSheet) {
        SubtitleSheet(
            state = trackState,
            onSelect = { track ->
                runCatching {
                    exoPlayer.enableSubtitles()
                    currentTracks?.let { exoPlayer.selectTrack(track, it) }
                }
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

    if (showQualitySheet) {
        QualitySheet(
            state = trackState,
            onAuto = {
                runCatching { exoPlayer.clearVideoOverrides() }
                showQualitySheet = false
            },
            onSelect = { track ->
                runCatching { currentTracks?.let { exoPlayer.selectTrack(track, it) } }
                showQualitySheet = false
            },
            onDismiss = { showQualitySheet = false }
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

// ---------------------------------------------------------------------------
// Models
// ---------------------------------------------------------------------------

private data class SeekFlash(val left: Boolean)

private sealed class GestureHud {
    data class Brightness(val value: Float) : GestureHud()
    data class Volume(val value: Float) : GestureHud()
    data class Scrub(val position: Long, val duration: Long) : GestureHud()
}

// ---------------------------------------------------------------------------
// Overlays
// ---------------------------------------------------------------------------

@Composable
private fun SeekFlashOverlay(flash: SeekFlash?) {
    AnimatedVisibility(
        visible = flash != null,
        enter = fadeIn(tween(80)) + scaleIn(initialScale = 0.7f),
        exit = fadeOut(tween(280)),
        modifier = Modifier.fillMaxSize()
    ) {
        val left = flash?.left == true
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(if (left) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            if (left) {
                                listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.14f)
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (left) Icons.Filled.Replay10 else Icons.Filled.Forward10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        if (left) "−10" else "+10",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.GestureHudOverlay(hud: GestureHud?) {
    AnimatedVisibility(
        visible = hud != null,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val h = hud
        if (h != null) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 22.dp, vertical = 16.dp)
                    .widthIn(min = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (h) {
                    is GestureHud.Brightness -> {
                        Icon(Icons.Outlined.BrightnessHigh, null, tint = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.brightness),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        HudBar(h.value)
                    }
                    is GestureHud.Volume -> {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.volume),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        HudBar(h.value)
                    }
                    is GestureHud.Scrub -> {
                        Text(
                            formatTime(h.position),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "/ ${formatTime(h.duration)}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudBar(fraction: Float) {
    Box(
        Modifier
            .width(120.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.25f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(Flame)
        )
    }
}

// ---------------------------------------------------------------------------
// Scrubber
// ---------------------------------------------------------------------------

@Composable
private fun PremiumScrubber(
    position: Float,
    buffered: Float,
    duration: Float,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(position) }
    val shown = if (dragging) dragValue else position
    val posFrac = (shown / duration).coerceIn(0f, 1f)
    val bufFrac = (buffered / duration).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        dragValue = position
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val w = size.width.coerceAtLeast(1)
                        val f = (change.position.x / w).coerceIn(0f, 1f)
                        dragValue = f * duration
                        onScrub(dragValue)
                    },
                    onDragEnd = {
                        dragging = false
                        onScrubFinished(dragValue)
                    },
                    onDragCancel = { dragging = false }
                )
            }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val w = size.width.coerceAtLeast(1)
                    val f = (offset.x / w).coerceIn(0f, 1f)
                    val v = f * duration
                    onScrub(v)
                    onScrubFinished(v)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (dragging) 5.dp else 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            // Buffered
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bufFrac)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            // Played (Flame — Crunchyroll/Netflix accent energy)
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(posFrac)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Flame.copy(alpha = 0.85f), Flame)
                        )
                    )
            )
        }
        // Thumb
        Box(
            Modifier
                .fillMaxWidth(posFrac)
                .padding(end = 0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                Modifier
                    .size(if (dragging) 16.dp else 12.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Controls bits
// ---------------------------------------------------------------------------

@Composable
private fun TransportIcon(
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.28f),
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .widthIn(min = 44.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = tint.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SubDubPill(currentType: String, onToggle: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(2.dp)
    ) {
        PillSegment("SUB", currentType != "dub")
        PillSegment("DUB", currentType == "dub")
    }
}

@Composable
private fun PillSegment(text: String, active: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Flame else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ---------------------------------------------------------------------------
// Sheets
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSheet(
    speed: Float,
    resizeMode: Int,
    currentType: String,
    trackState: TrackSelectionUiState,
    onSpeed: () -> Unit,
    onQuality: () -> Unit,
    onSubs: () -> Unit,
    onAudio: () -> Unit,
    onResizeCycle: () -> Unit,
    onType: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(title = stringResource(R.string.settings), onDismiss = onDismiss) {
        SettingsRow(
            stringResource(R.string.playback_speed),
            "${speed}x",
            onSpeed
        )
        SettingsRow(
            stringResource(R.string.quality),
            if (trackState.isAutoVideo) stringResource(R.string.auto) else {
                trackState.videoTracks.firstOrNull { it.isSelected }?.label
                    ?: stringResource(R.string.auto)
            },
            onQuality
        )
        SettingsRow(stringResource(R.string.subtitles), "…", onSubs)
        SettingsRow(stringResource(R.string.audio_tracks), currentType.uppercase(), onAudio)
        SettingsRow(
            stringResource(R.string.aspect_ratio),
            when (resizeMode) {
                0 -> stringResource(R.string.fit)
                1 -> stringResource(R.string.zoom)
                else -> stringResource(R.string.fill)
            },
            onResizeCycle
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.audio_tracks),
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(6.dp))
        Row {
            SubDubPill(currentType = currentType, onToggle = {
                onType(if (currentType == "sub") "dub" else "sub")
            })
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(value, color = Flame, fontWeight = FontWeight.SemiBold)
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
        TrackRow(stringResource(R.string.subtitles_off), !state.subtitleTracks.any { it.isSelected }, onSubtitlesOff)
        if (state.hasSubtitleOptions) {
            state.subtitleTracks.forEach { track ->
                TrackRow(track.label, track.isSelected) { onSelect(track) }
            }
        } else {
            Text(
                stringResource(R.string.no_subtitle_tracks),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
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
    SheetScaffold(title = stringResource(R.string.audio_and_subtitles), onDismiss = onDismiss) {
        Text(
            stringResource(R.string.stream_type),
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(8.dp))
        SubDubPill(currentType = currentType, onToggle = {
            onTypeChange(if (currentType == "sub") "dub" else "sub")
        })
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.audio_tracks),
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(4.dp))
        if (state.hasAudioOptions) {
            state.audioTracks.forEach { track ->
                TrackRow(track.label, track.isSelected) { onSelect(track) }
            }
        } else {
            Text(
                stringResource(R.string.no_audio_tracks),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QualitySheet(
    state: TrackSelectionUiState,
    onAuto: () -> Unit,
    onSelect: (SelectableTrack) -> Unit,
    onDismiss: () -> Unit
) {
    SheetScaffold(title = stringResource(R.string.quality), onDismiss = onDismiss) {
        TrackRow(stringResource(R.string.auto), state.isAutoVideo, onAuto)
        if (state.videoTracks.isNotEmpty()) {
            state.videoTracks.forEach { track ->
                TrackRow(track.label, !state.isAutoVideo && track.isSelected) { onSelect(track) }
            }
        } else {
            Text(
                stringResource(R.string.quality_adaptive_only),
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
            val label = if (s == 1f) "1x · ${stringResource(R.string.normal)}" else "${s}x"
            TrackRow(label, s == current) { onSelect(s) }
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
            columns = GridCells.Adaptive(minSize = 56.dp),
            modifier = Modifier.heightIn(max = 320.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..total).toList()) { ep ->
                val selected = ep == current
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Flame else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (selected) Flame else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(ep) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        ep.toString(),
                        color = Color.White,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(SurfaceRaised)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Handle
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.close),
                    color = Flame,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Flame.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) Flame else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Flame)
        }
    }
}

@Composable
private fun PlaybackErrorOverlay(
    detail: String?,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.playback_error_title),
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.playback_error_message),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                detail,
                color = TextSecondary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Flame)
                .clickable(onClick = onRetry)
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                stringResource(R.string.retry),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
