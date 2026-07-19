package com.aniwavestream.app.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aniwavestream.app.data.api.AniwavesResolver
import com.aniwavestream.app.data.api.ResolvedServer
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.player.MediaCache
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceDark
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

// Crunchyroll's exact brand orange for active progress / accents.
private val CrunchyrollOrange = Color(0xFFFF6600)
private const val CONTROLS_TIMEOUT_MS = 3_000L

/** Bridges "an episode is actively playing" to MainActivity for auto-PiP. */
internal object PlayerPipState {
    var active = false
}

private enum class Menu { NONE, SPEED, SUBS, AUDIO, SERVER }

@Composable
fun PlayerScreen(
    animeId: Int,
    episode: Int,
    title: String = "",
    repository: AnimeRepository,
    library: UserLibraryStore,
    onBack: () -> Unit,
    onEpisodeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var displayTitle by remember { mutableStateOf(title) }
    var maxEp by remember { mutableStateOf(12) }
    val scope = rememberCoroutineScope()

    var streamState by remember(animeId, episode) { mutableStateOf<StreamState>(StreamState.Loading) }
    var currentUrl by remember { mutableStateOf<String?>(null) }

    // Global cache + aggressive load control (per spec).
    val exoPlayer = remember { MediaCache.buildPlayer(context) }
    var isPlaying by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var tracksVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(s: Int) {
                buffering = s == Player.STATE_BUFFERING
            }
            override fun onTracksChanged(t: Tracks) { tracksVersion++ }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    // Drop every allocation instantly on screen exit (no memory leaks).
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            PlayerPipState.active = false
        }
    }

    // Resolve the real stream from the backend (no demo fallback).
    LaunchedEffect(animeId, episode) {
        streamState = StreamState.Loading
        currentUrl = null
        val resolver = AniwavesResolver()
        val titleToUse = displayTitle.takeIf { it.isNotBlank() }
            ?: repository.cached(animeId)?.title
            ?: repository.detail(animeId).getOrNull()?.title
            ?: ""
        runCatching { resolver.resolve(animeId, titleToUse, episode) }
            .onSuccess { url -> currentUrl = url; streamState = StreamState.Ready }
            .onFailure { t -> streamState = StreamState.Error(t.message ?: "Failed to load stream") }
    }

    // Load the resolved URL into the player.
    LaunchedEffect(currentUrl) {
        val url = currentUrl ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Metadata (best-effort, does not block playback).
    LaunchedEffect(animeId) {
        repository.detail(animeId).onSuccess {
            if (displayTitle.isBlank()) displayTitle = it.title
            maxEp = (it.episodes ?: 12).coerceIn(1, 24)
        }
    }

    // Position / duration polling loop.
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            position = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Persist rough progress while watching.
    LaunchedEffect(animeId, episode) {
        while (isActive) {
            delay(5000)
            val dur = exoPlayer.duration
            val pos = exoPlayer.currentPosition
            if (dur > 0) library.saveProgress(animeId, episode, pos.toFloat() / dur.toFloat())
        }
    }

    // --- UI state ---
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(Menu.NONE) }
    var servers by remember { mutableStateOf<List<ResolvedServer>>(emptyList()) }
    var serverLoading by remember { mutableStateOf(false) }
    var doubleTap by remember { mutableStateOf<DoubleTapIndicator?>(null) }

    // Auto-hide controls while playing (unless locked or a menu is open).
    LaunchedEffect(controlsVisible, isPlaying, locked, menu) {
        if (controlsVisible && isPlaying && !locked && menu == Menu.NONE) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    PlayerPipState.active = isPlaying && currentUrl != null

    BackHandler(onBack = onBack)

    val tracks = remember(tracksVersion) { exoPlayer.currentTracks }
    val subOptions = remember(tracks) { collectTracks(tracks, C.TRACK_TYPE_TEXT) }
    val audioOptions = remember(tracks) { collectTracks(tracks, C.TRACK_TYPE_AUDIO) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Video canvas — generic controller disabled, custom overlay drawn on top.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer (tap toggles controls, double-tap seeks, drag scrubs / volume / brightness).
        if (!locked) {
            GestureLayer(
                modifier = Modifier.fillMaxSize(),
                onToggleControls = { controlsVisible = !controlsVisible },
                onDoubleTapSide = { right ->
                    val delta = if (right) 10_000L else -10_000L
                    val target = (exoPlayer.currentPosition + delta)
                        .coerceIn(0, duration.coerceAtLeast(0))
                    exoPlayer.seekTo(target)
                    doubleTap = DoubleTapIndicator(right, System.currentTimeMillis())
                },
                onScrub = { fraction ->
                    if (duration > 0) exoPlayer.seekTo((fraction * duration.toFloat()).toLong())
                },
                onVolume = { level -> setVolume(context, level) },
                onBrightness = { level -> setBrightness(context, level) }
            )
        }

        // Top bar.
        AnimatedVisibility(visible = controlsVisible && !locked, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Background.copy(alpha = 0.65f)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpringIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back") { onBack() }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        displayTitle.ifBlank { "Episode $episode" },
                        color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text("Episode $episode", color = TextSecondary, fontSize = 12.sp)
                }
                SpringIcon(Icons.Filled.PictureInPicture, "Picture in Picture") { enterPip(context) }
                SpringIcon(Icons.Filled.Lock, "Lock controls") { locked = true }
            }
        }

        // Bottom controls.
        AnimatedVisibility(
            visible = controlsVisible && !locked,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Column(
                Modifier.fillMaxWidth().background(Background.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Seek bar — Crunchyroll orange active segment on dark-gray track.
                Slider(
                    value = if (duration > 0) (position.toFloat() / duration.toFloat()) else 0f,
                    onValueChange = { if (duration > 0) exoPlayer.seekTo((it * duration.toFloat()).toLong()) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = CrunchyrollOrange,
                        activeTrackColor = CrunchyrollOrange,
                        inactiveTrackColor = Color(0x33FFFFFF),
                        disabledThumbColor = CrunchyrollOrange,
                        disabledActiveTrackColor = CrunchyrollOrange,
                        disabledInactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(position), color = TextSecondary, fontSize = 12.sp)
                    Text(" / ", color = TextSecondary, fontSize = 12.sp)
                    Text(formatTime(duration), color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    SpringIcon(Icons.Filled.Speed, "Playback speed") { menu = Menu.SPEED }
                    SpringIcon(Icons.Filled.Subtitles, "Subtitles") { menu = Menu.SUBS }
                    SpringIcon(Icons.Filled.Audiotrack, "Audio track") { menu = Menu.AUDIO }
                    SpringIcon(Icons.Filled.Tune, "Server / quality") { menu = Menu.SERVER }
                }

                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    SpringIcon(Icons.Filled.SkipPrevious, "Back 10s", size = 28) {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                    }
                    SpringIcon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        size = 36
                    ) { exoPlayer.playWhenReady = !exoPlayer.playWhenReady }
                    SpringIcon(Icons.Filled.SkipNext, "Forward 10s", size = 28) {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(duration))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { if (episode < maxEp) onEpisodeChange(episode + 1) }, enabled = episode < maxEp) {
                        Text("Next episode", color = OrangePrimary)
                    }
                }
            }
        }

        // Locked affordance.
        AnimatedVisibility(visible = locked, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                SpringIcon(Icons.Filled.Lock, "Unlock controls", modifier = Modifier.padding(12.dp)) {
                    locked = false
                }
            }
        }

        // Menu sheets (slide up + fade).
        AnimatedVisibility(
            visible = menu != Menu.NONE,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (menu) {
                                Menu.SPEED -> "Playback speed"
                                Menu.SUBS -> "Subtitles"
                                Menu.AUDIO -> "Audio track"
                                Menu.SERVER -> "Server / quality"
                                else -> ""
                            },
                            color = TextPrimary, fontSize = 16.sp
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { menu = Menu.NONE }) { Text("Close", color = OrangePrimary) }
                    }
                    Spacer(Modifier.height(8.dp))
                    when (menu) {
                        Menu.SPEED -> {
                            val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                            speeds.forEach { sp ->
                                MenuRow(
                                    if (sp == 1f) "Normal" else "${sp}x",
                                    exoPlayer.playbackParameters.speed == sp
                                ) { exoPlayer.setPlaybackSpeed(sp) }
                            }
                        }
                        Menu.SUBS -> {
                            MenuRow("Off", subOptions.none { isSelected(tracks, it) }) {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon().clearOverrideOfType(C.TRACK_TYPE_TEXT).build()
                            }
                            subOptions.forEach { opt ->
                                MenuRow(opt.label, isSelected(tracks, opt)) { selectTrack(exoPlayer, opt) }
                            }
                            if (subOptions.isEmpty()) Text("No subtitle tracks", color = TextSecondary)
                        }
                        Menu.AUDIO -> {
                            audioOptions.forEach { opt ->
                                MenuRow(opt.label, isSelected(tracks, opt)) { selectTrack(exoPlayer, opt) }
                            }
                            if (audioOptions.isEmpty()) Text("No alternate audio", color = TextSecondary)
                        }
                        Menu.SERVER -> {
                            if (servers.isEmpty() && !serverLoading) {
                                LaunchedEffect(Unit) {
                                    serverLoading = true
                                    runCatching {
                                        AniwavesResolver().listServers(animeId, displayTitle, episode)
                                    }.onSuccess { servers = it }.onFailure { }
                                    serverLoading = false
                                }
                            }
                            if (serverLoading) Text("Loading servers…", color = TextSecondary)
                            servers.forEach { srv ->
                                MenuRow(srv.name, currentUrl == srv.url) {
                                    currentUrl = srv.url
                                    menu = Menu.NONE
                                }
                            }
                            if (!serverLoading && servers.isEmpty()) Text("No servers found", color = TextSecondary)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Loading / buffering overlay.
        if (streamState is StreamState.Loading || buffering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (streamState is StreamState.Loading) "Resolving stream…" else "Buffering…",
                    color = TextSecondary
                )
            }
        }

        // Error overlay with retry.
        if (streamState is StreamState.Error) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("Couldn't load this episode.", color = TextPrimary)
                    Text((streamState as StreamState.Error).message, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        scope.launch {
                            streamState = StreamState.Loading
                            currentUrl = null
                            runCatching { AniwavesResolver().resolve(animeId, displayTitle, episode) }
                                .onSuccess { url -> currentUrl = url; streamState = StreamState.Ready }
                                .onFailure { t -> streamState = StreamState.Error(t.message ?: "Failed") }
                        }
                    }) { Text("Retry", color = OrangePrimary) }
                }
            }
        }

        // Double-tap seek indicator.
        doubleTap?.let { dt ->
            if (System.currentTimeMillis() - dt.at < 600) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (dt.right) "10»" else "«10", color = CrunchyrollOrange, fontSize = 20.sp)
                        }
                    }
                }
            } else {
                doubleTap = null
            }
        }
    }
}

// ---------- gesture layer ----------

private data class DoubleTapIndicator(val right: Boolean, val at: Long)

private enum class DragMode { SCRUB, VERTICAL }

@Composable
private fun GestureLayer(
    modifier: Modifier,
    onToggleControls: () -> Unit,
    onDoubleTapSide: (right: Boolean) -> Unit,
    onScrub: (fraction: Float) -> Unit,
    onVolume: (level: Float) -> Unit,
    onBrightness: (level: Float) -> Unit
) {
    val context = LocalContext.current
    var dragMode by remember { mutableStateOf<DragMode?>(null) }
    var startVolume by remember { mutableFloatStateOf(0f) }
    var startBrightness by remember { mutableFloatStateOf(0f) }

    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset -> onDoubleTapSide(offset.x > size.width / 2) }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragMode = null
                        startVolume = currentVolume(context)
                        startBrightness = currentBrightness(context)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragMode == null) {
                            dragMode = if (abs(dragAmount.x) > abs(dragAmount.y)) DragMode.SCRUB else DragMode.VERTICAL
                        }
                        when (dragMode) {
                            DragMode.SCRUB -> {
                                val w = size.width.coerceAtLeast(1)
                                val frac = (change.position.x / w).coerceIn(0f, 1f)
                                onScrub(frac)
                            }
                            DragMode.VERTICAL -> {
                                val right = change.position.x > size.width / 2
                                val delta = -(dragAmount.y) / size.height
                                if (right) {
                                    onVolume((startVolume + delta).coerceIn(0f, 1f))
                                } else {
                                    onBrightness((startBrightness + delta).coerceIn(0.05f, 1f))
                                }
                            }
                            null -> {}
                        }
                    },
                    onDragEnd = { dragMode = null }
                )
            }
    )
}

// ---------- hardware / PiP helpers ----------

private fun setVolume(context: Context, level: Float) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).toInt().coerceIn(0, max), 0)
}

private fun currentVolume(context: Context): Float {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 1f
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun setBrightness(context: Context, level: Float) {
    val act = context as? Activity ?: return
    try {
        val attrs = act.window.attributes
        attrs.screenBrightness = level
        act.window.attributes = attrs
    } catch (_: Exception) {
    }
}

private fun currentBrightness(context: Context): Float {
    val act = context as? Activity ?: return 1f
    return act.window.attributes.screenBrightness.takeIf { it >= 0f } ?: 1f
}

private fun enterPip(context: Context) {
    val act = context as? ComponentActivity ?: return
    val rational = Rational(16, 9)
    val params = android.app.PictureInPictureParams.Builder()
        .setAspectRatio(rational)
        .build()
    try {
        act.enterPictureInPictureMode(params)
    } catch (_: Exception) {
    }
}

// ---------- track helpers ----------

private data class TrackOption(val groupIndex: Int, val trackIndex: Int, val label: String)

private fun collectTracks(tracks: Tracks, type: Int): List<TrackOption> {
    val out = mutableListOf<TrackOption>()
    tracks.groups.forEachIndexed { gi, group ->
        if (group.length == 0) return@forEachIndexed
        val fmt = group.getTrackFormat(0)
        if (fmt.trackType != type) return@forEachIndexed
        for (i in 0 until group.length) {
            val f = group.getTrackFormat(i)
            out.add(TrackOption(gi, i, formatLabel(f, type)))
        }
    }
    return out
}

private fun formatLabel(f: Format, type: Int): String {
    val lang = f.language?.takeIf { it.isNotBlank() } ?: f.label?.toString()
    if (lang != null) return lang
    return if (type == C.TRACK_TYPE_AUDIO) {
        val ch = f.channelCount ?: 0
        if (ch > 0) "Audio ($ch ch)" else "Audio"
    } else "Track"
}

private fun isSelected(tracks: Tracks, opt: TrackOption): Boolean {
    val group = tracks.groups.getOrNull(opt.groupIndex) ?: return false
    return group.isTrackSelected(opt.trackIndex)
}

private fun selectTrack(exoPlayer: ExoPlayer, opt: TrackOption) {
    val params = exoPlayer.trackSelectionParameters.buildUpon()
        .setOverrideForType(TrackSelectionOverride(opt.groupIndex, listOf(opt.trackIndex)))
        .build()
    exoPlayer.trackSelectionParameters = params
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------- small UI atoms ----------

@Composable
private fun SpringIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: Int = 24,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Icon(imageVector, contentDescription = contentDescription, tint = TextPrimary, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun MenuRow(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (selected) CrunchyrollOrange else TextPrimary)
            Spacer(Modifier.weight(1f))
            if (selected) Icon(Icons.Filled.Check, null, tint = CrunchyrollOrange)
        }
    }
}

private sealed interface StreamState {
    data object Loading : StreamState
    data object Ready : StreamState
    data class Error(val message: String) : StreamState
}
