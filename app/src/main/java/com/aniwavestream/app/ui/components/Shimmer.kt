package com.aniwavestream.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aniwavestream.app.ui.theme.SurfaceDark
import com.aniwavestream.app.ui.theme.SurfaceElevated

/**
 * Rule 5 — Fluid loading transitions.
 * A reusable canvas-sweep shimmer brush that mirrors Crunchyroll's structural
 * placeholders. Apply [shimmer] to any Box/surface that stands in for real
 * content while it fetches. Structural placeholders below match the exact shapes
 * of the hero, rows and grids so there is never a blank screen or a bare spinner.
 */
@Composable
fun Modifier.shimmer(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-progress"
    )
    val shimmerColors = listOf(
        SurfaceElevated,
        Color.White.copy(alpha = 0.08f),
        SurfaceElevated
    )
    return this
        .clip(shape)
        .drawWithCache {
            val widthPx = size.width
            val travel = widthPx * 2f
            val start = -travel + (travel * 1.5f) * progress
            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(start, 0f),
                end = Offset(start + widthPx, size.height)
            )
            onDrawBehind {
                drawRect(color = SurfaceDark)
                drawRect(brush = brush)
            }
        }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    Box(modifier.shimmer(shape))
}

/** Full home screen placeholder: mirrors hero + two poster rows. */
@Composable
fun HomeShimmer(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(420.dp),
            shape = RoundedCornerShape(0.dp)
        )
        Spacer(Modifier.height(20.dp))
        repeat(2) {
            ShimmerBox(
                Modifier
                    .padding(start = 16.dp)
                    .width(140.dp)
                    .height(20.dp)
            )
            Spacer(Modifier.height(12.dp))
            PosterRowShimmer()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun PosterRowShimmer(count: Int = 5) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count) {
            Column(Modifier.width(120.dp)) {
                ShimmerBox(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(6.dp))
                ShimmerBox(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

/** Grid placeholder matching Browse/Search poster grids. */
@Composable
fun PosterGridShimmer(
    modifier: Modifier = Modifier,
    rows: Int = 4
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Column(Modifier.weight(1f)) {
                        ShimmerBox(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(
                            Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Episode list placeholder matching the polished EpisodeCard shape. */
@Composable
fun EpisodeListShimmer(count: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp)) {
        repeat(count) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                ShimmerBox(
                    Modifier
                        .width(140.dp)
                        .height(80.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShimmerBox(Modifier.fillMaxWidth(0.7f).height(14.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp), RoundedCornerShape(4.dp))
                }
            }
        }
    }
}
