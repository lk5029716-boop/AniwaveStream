package com.aniwavestream.app.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aniwavestream.app.data.model.DemoStreams
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    var title by remember { mutableStateOf("Loading…") }
    var maxEp by remember { mutableStateOf(12) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(animeId) {
        repository.detail(animeId).onSuccess {
            title = it.title
            maxEp = (it.episodes ?: 12).coerceIn(1, 24)
        }
    }

    val streamUrl = remember(animeId, episode) {
        DemoStreams.forEpisode(animeId, episode)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(streamUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Persist rough progress while watching
    LaunchedEffect(animeId, episode, exoPlayer) {
        while (isActive) {
            delay(5000)
            val dur = exoPlayer.duration
            val pos = exoPlayer.currentPosition
            if (dur > 0) {
                library.saveProgress(animeId, episode, pos.toFloat() / dur.toFloat())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val dur = exoPlayer.duration
            val pos = exoPlayer.currentPosition
            if (dur > 0) {
                scope.launch {
                    library.saveProgress(animeId, episode, pos.toFloat() / dur.toFloat())
                }
            }
            exoPlayer.release()
        }
    }

    BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, maxLines = 1)
                Text("Episode $episode  ·  Demo stream", color = TextSecondary)
            }
            IconButton(
                onClick = { if (episode > 1) onEpisodeChange(episode - 1) },
                enabled = episode > 1
            ) {
                Icon(Icons.AutoMirrored.Filled.SkipPrevious, null, tint = OrangePrimary)
            }
            IconButton(
                onClick = { if (episode < maxEp) onEpisodeChange(episode + 1) },
                enabled = episode < maxEp
            ) {
                Icon(Icons.AutoMirrored.Filled.SkipNext, null, tint = OrangePrimary)
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
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Public sample video for demo playback only.",
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                if (episode < maxEp) onEpisodeChange(episode + 1)
            }) {
                Text("Next episode", color = OrangePrimary)
            }
        }
    }
}
