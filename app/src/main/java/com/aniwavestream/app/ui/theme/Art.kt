package com.aniwavestream.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aniwavestream.app.data.model.Anime

/**
 * Anivave "Redux Premium" art-wash engine.
 * Each title is assigned one of 8 signature radial gradients (ember / abyss /
 * void / bloom / gilded / verdant / azure / rose) derived deterministically
 * from its id, then the real poster is layered on top with a bottom scrim —
 * faithfully reproducing anivave.html's gradient + figure treatment in Compose.
 */
enum class ArtWash(
    val stops: List<Color>,
    val centerX: Float,  // fraction of width
    val centerY: Float   // fraction of height (can be < 0 for top bleed)
) {
    Ember(listOf(Color(0xFFFF8A3D), Color(0xFF7A2C10), Color(0xFF2A0F08), Color(0xFF080302)), 0.2f, -0.1f),
    Abyss(listOf(Color(0xFF3FB8C9), Color(0xFF124A52), Color(0xFF0A2226), Color(0xFF020708)), 0.8f, -0.1f),
    Void(listOf(Color(0xFFA259E6), Color(0xFF3D1A5C), Color(0xFF1A0B2E), Color(0xFF030107)), 0.5f, -0.1f),
    Bloom(listOf(Color(0xFFFF5F8F), Color(0xFF7A1F3D), Color(0xFF2E0D19), Color(0xFF070104)), 0.25f, -0.1f),
    Gilded(listOf(Color(0xFFFFD23F), Color(0xFF7A5C0F), Color(0xFF2E2308), Color(0xFF070501)), 0.75f, -0.1f),
    Verdant(listOf(Color(0xFF4DFF9E), Color(0xFF106B3A), Color(0xFF0A2E1C), Color(0xFF010704)), 0.3f, -0.1f),
    Azure(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF1E3A8A), Color(0xFF020617)), 0.5f, -0.1f),
    Rose(listOf(Color(0xFFF43F5E), Color(0xFF9F1239), Color(0xFF4C0519), Color(0xFF0F0005)), 0.4f, -0.1f);

    companion object {
        fun forAnime(anime: Anime): ArtWash = entries[Math.abs(anime.id) % entries.size]
    }
}

@Composable
fun AnivaveArt(
    anime: Anime,
    modifier: Modifier = Modifier,
    showImage: Boolean = true
) {
    val wash = ArtWash.forAnime(anime)
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val brush = Brush.radialGradient(
            colorStops = wash.stops.mapIndexed { i, c -> i.toFloat() / (wash.stops.size - 1) to c }.toTypedArray(),
            center = androidx.compose.ui.geometry.Offset(w * wash.centerX, h * wash.centerY),
            radius = h * 1.3f
        )
        Box(Modifier.fillMaxSize().background(brush)) {
            if (showImage && anime.posterUrl != null) {
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(Color.Transparent),
                    error = ColorPainter(Color.Transparent),
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Bottom scrim for legibility (anivave art-scrim-b)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = h * 0.4f,
                        endY = h
                    )
                )
            )
        }
    }
}

/** Compact poster-tile variant used inside rows. */
@Composable
fun AnivavePosterArt(anime: Anime, modifier: Modifier = Modifier) {
    AnivaveArt(anime = anime, modifier = modifier.fillMaxWidth().height(160.dp))
}
