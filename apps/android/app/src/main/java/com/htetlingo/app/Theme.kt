package com.htetlingo.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vibrant, harmonious & Cute Aesthetic HtetLingo color palette
val TealPrimary = Color(0xFF0F766E)
val TealBright = Color(0xFF14B8A6)
val IndigoAccent = Color(0xFF4F46E5)
val CoralOrange = Color(0xFFEA580C)
val EmeraldGreen = Color(0xFF16A34A)
val RosePink = Color(0xFFE11D48)
val CrimsonRed = Color(0xFFDC2626)
val SkyBlue = Color(0xFF0284C7)
val DeepPurple = Color(0xFF7C3AED)
val WarmGold = Color(0xFFD97706)

// Cute Feminine & Liquid Glass Colors
val SakuraPink = Color(0xFFF472B6)
val SakuraLight = Color(0xFFFDF2F8)
val SakuraDark = Color(0xFFDB2777)
val LilacPurple = Color(0xFFA855F7)
val LilacLight = Color(0xFFFAF5FF)
val PeachPastel = Color(0xFFFDBA74)
val PeachLight = Color(0xFFFFF7ED)
val MintPastel = Color(0xFF34D399)
val MintLight = Color(0xFFECFDF5)
val GlassWhiteBg = Color(0xEBFFFFFF)
val GlassDarkBg = Color(0xCC1E1528)

private val LightColors = lightColorScheme(
    primary = SakuraPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF831843),
    secondary = LilacPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = PeachPastel,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = Color(0xFFFDF8FA),
    onBackground = Color(0xFF1F1728),
    surface = Color.White,
    onSurface = Color(0xFF1F1728),
    surfaceVariant = Color(0xFFF8EFF5),
    onSurfaceVariant = Color(0xFF6B5E74)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF472B6),
    onPrimary = Color(0xFF500724),
    primaryContainer = Color(0xFF831843),
    onPrimaryContainer = Color(0xFFFCE7F3),
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF3B0764),
    secondaryContainer = Color(0xFF581C87),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = Color(0xFFFDBA74),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = Color(0xFF7C2D12),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF160E1D),
    onBackground = Color(0xFFFAF5FF),
    surface = Color(0xFF22162B),
    onSurface = Color(0xFFFAF5FF),
    surfaceVariant = Color(0xFF2D1F37),
    onSurfaceVariant = Color(0xFFC4B5CD)
)

@Composable
fun HtetLingoTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
