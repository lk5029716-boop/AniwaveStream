package com.aniwavestream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aniwavestream.app.R

// Anivave signature typefaces (downloaded into res/font).
val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.W400),
    Font(R.font.bricolage_grotesque, FontWeight.S600),
    Font(R.font.bricolage_grotesque, FontWeight.W700),
    Font(R.font.bricolage_grotesque, FontWeight.W800)
)
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.W500),
    Font(R.font.ibm_plex_mono_medium, FontWeight.W600)
)

private val AniwaveColors = darkColorScheme(
    primary = Flame,
    onPrimary = TextPrimary,
    secondary = Cool,
    background = Void,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = Hairline,
    outlineVariant = HairlineBright
)

private val AniwaveTypography = Typography(
    // Big display headers use Bricolage Grotesque (anivave signature).
    displayLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.W800,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.W800,
        fontSize = 22.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.W700,
        fontSize = 19.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp
    ),
    // Meta / labels / chips use IBM Plex Mono for that technical anivave feel.
    labelLarge = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
)

@Composable
fun AniwaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AniwaveColors,
        typography = AniwaveTypography,
        content = content
    )
}
