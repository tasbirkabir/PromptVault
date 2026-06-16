package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Unified Glassmorphic Style elements
val GlassBackground = Color(0xFF141121).copy(alpha = 0.75f) // Premium semi-transparent base
val GlassBorderTop = Color(0xFFC084FC).copy(alpha = 0.22f) // Glowing violet top border
val GlassBorderBottom = Color(0xFF10B981).copy(alpha = 0.08f) // Deep emerald bottom border

fun Modifier.glassmorphicContainer(
    shape: Shape = RoundedCornerShape(16.dp),
    borderOpacity: Float = 1.0f
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                GlassBackground,
                Color(0xFF0C0916).copy(alpha = 0.88f)
            )
        )
    )
    .border(
        width = 1.1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBorderTop.copy(alpha = GlassBorderTop.alpha * borderOpacity),
                GlassBorderBottom.copy(alpha = GlassBorderBottom.alpha * borderOpacity)
            )
        ),
        shape = shape
    )

// "Artistic Flair" Dark Theme Colors
val ObsidianBg = Color(0xFF1C1B1F)      // Charcoal/Dark violet background
val CardBackground = Color(0xFF2B2930)  // Deep slate-violet for cards/surfaces
val EmeraldAccent = Color(0xFFD0BCFF)   // Artistic Lavender accent
val LightGrayText = Color(0xFFE6E1E5)   // Light lavenderish primary text
val CoolGrayMuted = Color(0xFFCAC4D0)   // Cool muted gray/violet for secondary text

// Artistic palette specific colors
val ArtisticMuted = Color(0xFF938F99)
val ArtisticNavBg = Color(0xFF25232A)
val ArtisticActivePill = Color(0xFF4A4458)
val ArtisticDarkPurple = Color(0xFF381E72)
val ArtisticLightPurple = Color(0xFFE8DEF8)
val ArtisticBorder = Color(0xFF49454F)
val ArtisticHighlightTotal = Color(0xFF332D41)

// Tag Colors matching specific categories from requirements
val CategoryMarketingBg = Color(0xFF381E72)
val CategoryMarketingText = Color(0xFFD0BCFF)
val CategoryMarketingBorder = Color(0xFF4F378B)

val CategoryCodingBg = Color(0xFF005049)
val CategoryCodingText = Color(0xFF80D0C7)
val CategoryCodingBorder = Color(0xFF006A60)

// M3 standard mappings
val Primary = Color(0xFFD0BCFF)
val Secondary = Color(0xFF80D0C7)
val Tertiary = Color(0xFF381E72)
val BackgroundDark = Color(0xFF1C1B1F)
val SurfaceDark = Color(0xFF2B2930)
val ErrorColor = Color(0xFFF2B8B5)

