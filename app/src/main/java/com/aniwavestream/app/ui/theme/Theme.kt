package com.aniwavestream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AniwaveColors = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = TextPrimary,
    secondary = OrangeDim,
    background = Background,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val AniwaveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun AniwaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AniwaveColors,
        typography = AniwaveTypography,
        content = content
    )
}
