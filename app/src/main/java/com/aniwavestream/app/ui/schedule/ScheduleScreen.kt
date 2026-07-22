package com.aniwavestream.app.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.DpSize
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.ScheduleDay
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.ui.components.ErrorBox
import com.aniwavestream.app.ui.components.LoadingBox
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary

@Composable
fun ScheduleScreen(
    repository: AnimeRepository,
    onAnimeClick: (Anime) -> Unit
) {
    var selectedDay by remember { mutableStateOf(ScheduleDay.today()) }
    var items by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDay) {
        loading = true
        error = null
        repository.schedule(selectedDay)
            .onSuccess {
                items = it
                loading = false
            }
            .onFailure {
                error = it.message ?: "Failed to load schedule"
                loading = false
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Text(
            "Schedule",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // Day tabs — horizontal scrollable row of pill-shaped day chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ScheduleDay.entries, key = { it.name }) { day ->
                DayTab(
                    day = day,
                    selected = day == selectedDay,
                    onClick = { selectedDay = day }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Content
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!) { selectedDay = selectedDay }
            items.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No anime scheduled for ${selectedDay.displayName}.",
                        color = TextSecondary
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { anime ->
                    ScheduleCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTab(
    day: ScheduleDay,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) OrangePrimary else SurfaceElevated,
        animationSpec = tween(300)
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = tween(300)
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.displayName,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

/**
 * A schedule card with a diagonal clip-path design on the poster image
 * and a subtle gradient overlay for text readability.
 *
 * The poster uses WebP (preferred) for crisp, high-quality images,
 * with crossfade for smooth loading.
 */
@Composable
private fun ScheduleCard(
    anime: Anime,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Poster with diagonal clip-path
            Box(
                Modifier
                    .width(90.dp)
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .background(Background)
            ) {
                SubcomposeAsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalDensity.current)
                        .data(anime.posterUrl)
                        .crossfade(true)
                        .size(DpSize(90.dp, 120.dp))
                        .build(),
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                bottomStart = 16.dp
                            )
                        ),
                    loading = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Background),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = OrangePrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                anime.title.firstOrNull()?.toString() ?: "?",
                                color = TextSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                // Diagonal overlay gradient
                Canvas(Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                }
            }

            // Info column
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    anime.title,
                    color = TextPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val meta = listOfNotNull(
                    anime.score?.let { "★ ${"%.1f".format(it)}" },
                    anime.year?.toString(),
                    anime.type,
                    anime.episodes?.let { "$it eps" }
                ).joinToString("  ·  ")

                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status badge
                anime.status?.let { status ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrangePrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            status.take(8),
                            color = OrangePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
