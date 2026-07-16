package com.aniwavestream.app.ui.theme

import androidx.compose.ui.graphics.Color

// Anivave "Redux Premium" palette — exact match to the web design system.
val Flame = Color(0xFFFF5A1F)          // primary accent (--flame)
val FlameDim = Color(0x26FF5A1F)       // 15% flame tint (--flame-dim)
val Cool = Color(0xFF3DE3BD)           // secondary accent (--cool)
val CoolDim = Color(0x1F3DE3BD)        // 12% cool tint (--cool-dim)
val Gold = Color(0xFFFFC93C)           // rating chip (--gold)
val Purple = Color(0xFF8B5CF6)         // upcoming badge (--purple)
val PurpleDim = Color(0x268B5CF6)      // 15% purple tint (--purple-dim)

val Void = Color(0xFF08080A)           // app background (--void)
val Surface = Color(0xFF121216)        // card surface (--surface)
val SurfaceRaised = Color(0xFF1A1A22)  // elevated surface (--surface-raised)

// Hairline borders — the signature Anivave 1px translucent stroke.
val Hairline = Color(0x14FFFFFF)       // rgba(255,255,255,0.08)
val HairlineBright = Color(0x28FFFFFF) // rgba(255,255,255,0.16)

val TextPrimary = Color(0xFFF9F9FB)
val TextSecondary = Color(0xFFA19FA8)
val TextMuted = Color(0xFF63616A)

// Backwards-compatible aliases (old names used across the app).
val OrangePrimary = Flame
val OrangeDim = Flame
val OrangeGlow = FlameDim
val Background = Void
val SurfaceDark = Void
val SurfaceElevated = SurfaceRaised
val CardBorder = Hairline
val TextMutedOld = TextMuted
val Success = Cool
val Danger = Color(0xFFFF5A5F)
