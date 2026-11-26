package com.northeast.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.northeast.launcher.data.Season

/** Northeast woodland palette. All colors are original choices for this project. */
object Woodland {
    // Sky / background: deep spruce, darkening toward the top.
    val SkyTop = Color(0xFF142319)
    val SkyBottom = Color(0xFF1B3024)

    // Tiles: bark brown cards.
    val Bark = Color(0xFF4A3728)
    val BarkFocused = Color(0xFF5C4533)
    val BarkDim = Color(0xFF3A2C20)

    // Text: birch whites.
    val Birch = Color(0xFFF1EDE3)
    val BirchMuted = Color(0xFFB9C2B4)
    val BirchFaint = Color(0xFF7E8A7E)

    /** Focus accent for each New England season (see [Season]). */
    fun accentFor(season: Season): Color = when (season) {
        Season.WINTER -> Color(0xFFDCE8EF)  // frost on birch
        Season.SPRING -> Color(0xFFB79AD1)  // lilac, NH's state flower
        Season.SUMMER -> Color(0xFF8CCB3F)  // bright summer leaf green
        Season.FOLIAGE -> Color(0xFFC8442F) // red maple
        Season.STICK -> Color(0xFFB8733A)   // copper oak leaves
    }

    /** Tree colors for the pine ridge, far → mid → near, in the season's accent hue. */
    data class Ridge(val far: Color, val mid: Color, val near: Color)

    /**
     * Each season's accent blended into the spruce background at increasing strength
     * (far 28%, mid 62%), with the nearest trees darkened so the layers read as depth.
     * Precomputed (tools/gen_pine_ridge.py documents the art) so there's no per-frame math.
     */
    fun ridgeColors(season: Season): Ridge = when (season) {
        Season.WINTER -> Ridge(Color(0xFF51645D), Color(0xFF93A2A2), Color(0xFF747B7E))
        Season.SPRING -> Ridge(Color(0xFF474E54), Color(0xFF7C728F), Color(0xFF60526E))
        Season.SUMMER -> Ridge(Color(0xFF3B5B2C), Color(0xFF619035), Color(0xFF4A6B22))
        Season.FOLIAGE -> Ridge(Color(0xFF4B3627), Color(0xFF863C2B), Color(0xFF692519))
        Season.STICK -> Ridge(Color(0xFF47432A), Color(0xFF7C5A32), Color(0xFF613E1F))
    }
}

/** The current pine-ridge colors; change with the season. */
val LocalRidge = staticCompositionLocalOf { Woodland.ridgeColors(Season.FOLIAGE) }

/** The current focus accent; changes with the season. */
val LocalAccent = staticCompositionLocalOf { Woodland.accentFor(Season.FOLIAGE) }

@Composable
fun NorthEastTheme(season: Season, content: @Composable () -> Unit) {
    val accent = Woodland.accentFor(season)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF101810),
            surface = Woodland.Bark,
            onSurface = Woodland.Birch,
            background = Woodland.SkyBottom,
            onBackground = Woodland.Birch,
            border = accent,
        ),
    ) {
        CompositionLocalProvider(
            LocalAccent provides accent,
            LocalRidge provides Woodland.ridgeColors(season),
            content = content,
        )
    }
}
