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

// Unified Glassmorphic Style elements - now a clean, premium, minimal plate matching Linear/Notion
val GlassBackground = Color(0xFF171A20) // Base Card #171A20
val GlassBorderTop = Color(0xFFA8B7C6).copy(alpha = 0.15f) // Subtle elegant slate-blue border
val GlassBorderBottom = Color(0xFFA8B7C6).copy(alpha = 0.05f) // Ultra-faint bottom border

fun Modifier.glassmorphicContainer(
    shape: Shape = RoundedCornerShape(12.dp),
    borderOpacity: Float = 1.0f
): Modifier = this
    .clip(shape)
    .background(Color(0xFF171A20)) // Clean Solid Card Surface
    .border(
        width = 1.dp,
        color = Color(0xFF272A30).copy(alpha = borderOpacity), // Soft dark-border tone
        shape = shape
    )

// "Artistic Flair" Dark Theme Colors - Redesigned for Premium Minimalist Slate
val ObsidianBg = Color(0xFF09090B)      // Dark background: #09090B
val CardBackground = Color(0xFF171A20)  // Card background: #171A20
val EmeraldAccent = Color(0xFFA8B7C6)   // Slate Blue Primary Brand color #A8B7C6
val LightGrayText = Color(0xFFF4F4F5)   // Zinc-100 Clean Text
val CoolGrayMuted = Color(0xFF71717A)   // Zinc-500 Muted Text

// Artistic palette specific colors - now clean slate/monochrome variants
val ArtisticMuted = Color(0xFF52525B)   // Zinc-600
val ArtisticNavBg = Color(0xFF111317)   // Surface: #111317
val ArtisticActivePill = Color(0xFF27272A) // Zinc-800 Active Pill
val ArtisticDarkPurple = Color(0xFF272A30) // Dark slate-border
val ArtisticLightPurple = Color(0xFFA8B7C6) // Slate Blue Accent #A8B7C6
val ArtisticBorder = Color(0xFF1F2228)   // Very soft subtle card border
val ArtisticHighlightTotal = Color(0xFF0F1115)
val CyanAccent = Color(0xFF8E9AA6)       // Desaturated secondary slate-blue

// Tag Colors matching specific categories from requirements (minimal monochrome tags)
val CategoryMarketingBg = Color(0xFF1F2228)
val CategoryMarketingText = Color(0xFFA8B7C6)
val CategoryMarketingBorder = Color(0xFF2F343F)

val CategoryCodingBg = Color(0xFF1A1D23)
val CategoryCodingText = Color(0xFFECEFF1)
val CategoryCodingBorder = Color(0xFF2A2E38)

// M3 standard mappings
val Primary = Color(0xFFA8B7C6)
val Secondary = Color(0xFF8E9AA6)
val Tertiary = Color(0xFF272A30)
val BackgroundDark = Color(0xFF09090B)
val SurfaceDark = Color(0xFF111317)
val ErrorColor = Color(0xFFEF4444) // Clean material red error

