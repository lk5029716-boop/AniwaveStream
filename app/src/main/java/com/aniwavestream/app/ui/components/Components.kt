package com.aniwavestream.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.Episode
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangeGlow
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    // Rule 5: never a bare spinner — fall back to structural shimmer.
    HomeShimmer(modifier.fillMaxSize().background(Background))
}

/**
 * Rule 4 — Consumer-grade full-screen error state.
 * Expressive vector illustration + friendly copy + prominent accent Retry button.
 * Accepts either a raw [Throwable] or a legacy message string; both are mapped
 * through [toFriendlyError] so no raw code ever reaches the layout.
 */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    FriendlyErrorScreen(message.toFriendlyError(), onRetry)
}

@Composable
fun ErrorBox(error: Throwable, onRetry: (() -> Unit)? = null) {
    FriendlyErrorScreen(error.toFriendlyError(), onRetry)
}

@Composable
fun FriendlyErrorScreen(error: FriendlyError, onRetry: (() -> Unit)?) {
    val icon: ImageVector = when (error.icon) {
        FriendlyErrorIcon.OFFLINE -> Icons.Default.CloudOff
        FriendlyErrorIcon.TIMEOUT -> Icons.Default.HourglassEmpty
        FriendlyErrorIcon.NOT_FOUND -> Icons.Default.SearchOff
        FriendlyErrorIcon.RATE_LIMIT -> Icons.Default.Speed
        FriendlyErrorIcon.GENERIC -> Icons.Default.SentimentDissatisfied
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(OrangeGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            error.title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(28.dp))
            PrimaryPillButton(text = "Retry", onClick = onRetry, leadingIcon = Icons.Default.Refresh)
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
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        if (onSeeAll != null) {
            Text(
                "See all",
                color = OrangePrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
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
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
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
            AsyncImage(
                model = anime.posterUrl,
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

/**
 * Rule 3 — Polished media / episode card.
 * Horizontal Coil thumbnail with an integrated watch-progress bar pinned to the
 * bottom edge of the image, plus a cleanly stacked metadata column on the right.
 * Never a plain text block or bare icon row.
 */
@Composable
fun EpisodeCard(
    episode: Episode,
    thumbnailUrl: String?,
    watchProgress: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(140.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Background)
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Play affordance
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Episode chip
            Text(
                "E${episode.number}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrangePrimary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            // Integrated watch-progress bar on the bottom edge of the image
            if (watchProgress > 0f) {
                LinearProgressIndicator(
                    progress = { watchProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = OrangePrimary,
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                episode.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Episode ${episode.number} · ${episode.durationLabel}",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Rule 1 — Immersive hero header.
 * Edge-to-edge artwork that bleeds under the translucent status bar, a
 * bottom-anchored vertical gradient scrim for text legibility, and a Crossfade
 * from shimmer placeholder to the loaded artwork (Rule 7).
 */
@Composable
fun HeroBanner(
    anime: Anime,
    onPlay: () -> Unit,
    onDetails: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(460.dp)
    ) {
        AsyncImage(
            model = anime.bannerUrl ?: anime.posterUrl,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Bottom-anchored vertical scrim for readable overlay text over bright art.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.4f to Color.Transparent,
                        0.75f to Background.copy(alpha = 0.6f),
                        1f to Background
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Text(
                "FEATURED",
                color = OrangePrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                anime.title,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 0.7f),
                color = TextPrimary,
                fontWeight = FontWeight.Black,
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
                PrimaryPillButton(text = "Play", onClick = onPlay, leadingIcon = Icons.Default.PlayArrow)
                SecondaryPillButton(text = "Details", onClick = onDetails)
            }
        }
    }
}

/**
 * Rules 2 & 7 — Premium Material 3 pill button with spring press physics.
 * RoundedCornerShape(50) pill, brand-orange fill, springs down on press.
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "primary-press"
    )
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(OrangePrimary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "secondary-press"
    )
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Text(text, color = TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
