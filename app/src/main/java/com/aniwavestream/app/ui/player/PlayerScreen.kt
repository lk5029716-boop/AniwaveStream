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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import com.aniwavestream.app.R
import com.aniwavestream.app.data.model.DemoStreams
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.player.PlayerModule
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(context.getString(R.string.loading)) }
    var maxEp by remember { mutableStateOf(12) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var trackState by remember { mutableStateOf(TrackSelectionUiState()) }
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }
    var playbackError by remember { mutableStateOf<PlaybackException?>(null) }

    LaunchedEffect(animeId) {
        repository.detail(animeId).onSuccess {
            title = it.title
            maxEp = (it.episodes ?: 12).coerceIn(1, 24)
        }
    }

    val streamUrl = remember(animeId, episode) {
        DemoStreams.forEpisode(animeId, episode)
    }

    // Rule 9: cache-backed, load-control-tuned player from the central module.
    val exoPlayer = remember {
        PlayerModule.buildPlayer(context).apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // --- CRASH FIX: attach a listener so playback/network errors surface as UI
    //     state instead of propagating and killing the app. Also keep track and
    //     tracks state in sync for the selection menus (Rule 10).
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
                trackState = tracks.toTrackSelectionUiState()
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(streamUrl) {
        playbackError = null
        // DRM-ready item builder (Rule 12); demo streams pass no license URL.
        exoPlayer.setMediaItem(PlayerModule.buildMediaItem(streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Rule 9c: lifecycle-aware pause/release so backgrounding never crashes.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Persist rough progress while watching (guarded against released player).
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
            exoPlayer.release()
            // Restore orientation + system bars on exit.
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Keep screen on during playback.
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    // Rule 9c: react to fullscreen toggle by rotating natively (no player rebuild).
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar hidden in fullscreen for immersion.
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

            // Fullscreen toggle overlay (Rule 9c).
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

            // Rule 4-style consumer error overlay (also the crash fix in action).
            playbackError?.let {
                PlaybackErrorOverlay(
                    onRetry = {
                        playbackError = null
                        exoPlayer.setMediaItem(PlayerModule.buildMediaItem(streamUrl))
                        exoPlayer.prepare()
                        exoPlayer.play()
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
                Text(
                    stringResource(R.string.demo_stream_notice),
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    if (episode < maxEp) onEpisodeChange(episode + 1)
                }) {
                    Text(stringResource(R.string.next_episode), color = OrangePrimary)
                }
            }
        }
    }

    // Rule 10: audio + subtitle selection sheet overlaid on the player.
    if (showTrackSheet) {
        TrackSelectionSheet(
            state = trackState,
            onSelect = { track ->
                currentTracks?.let { exoPlayer.selectTrack(track, it) }
                showTrackSheet = false
            },
            onSubtitlesOff = {
                exoPlayer.disableSubtitles()
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
