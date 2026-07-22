package com.aniwavestream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = OrangePrimary)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = TextSecondary)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = OrangePrimary)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", color = OrangePrimary)
            }
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showScore: Boolean = true
) {
    Column(
        modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalDensity.current)
                    .data(anime.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = OrangePrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            anime.title.firstOrNull()?.toString() ?: "?",
                            color = TextSecondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
            if (showScore && anime.score != null) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        String.format("%.1f", anime.score),
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            anime.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
fun AnimeRow(
    items: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { anime ->
            AnimePosterCard(anime = anime, onClick = { onAnimeClick(anime) })
        }
    }
}

@Composable
fun ContinueCard(
    anime: Anime,
    episode: Int,
    progress: Float,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Background)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalDensity.current)
                    .data(anime.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                anime.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("E$episode · Continue", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = OrangePrimary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
fun HeroBanner(
    anime: Anime,
    onPlay: () -> Unit,
    onDetails: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalDensity.current)
                .data(anime.bannerUrl ?: anime.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        0.45f to Color.Transparent,
                        1f to Background
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                "FEATURED",
                color = OrangePrimary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                anime.title,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 0.7f),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            val meta = listOfNotNull(
                anime.score?.let { "★ ${String.format("%.1f", it)}" },
                anime.year?.toString(),
                anime.type,
                anime.episodes?.let { "$it eps" }
            ).joinToString("  ·  ")
            Text(meta, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                anime.synopsis,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryPillButton(text = "▶  Play", onClick = onPlay)
                SecondaryPillButton(text = "Details", onClick = onDetails)
            }
        }
    }
}

@Composable
fun PrimaryPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(OrangePrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(text, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
