package com.northeast.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.northeast.launcher.data.BackgroundStyle
import com.northeast.launcher.data.LabelMode
import com.northeast.launcher.data.LauncherSettings
import com.northeast.launcher.data.SceneryMode
import com.northeast.launcher.data.Season
import com.northeast.launcher.data.TileArtwork
import com.northeast.launcher.data.TileSize
import com.northeast.launcher.data.TreeColor

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

    /** Tree colors for the pine ridge, far → mid → near. */
    @Immutable
    data class Ridge(val far: Color, val mid: Color, val near: Color)

    /** The original dark-green silhouettes ("Classic dark green" tree setting). */
    val ClassicRidge = Ridge(Color(0xFF17291E), Color(0xFF122119), Color(0xFF0D1812))

    /**
     * Each season's accent blended into the spruce background at increasing strength
     * (far 28%, mid 62%), with the nearest trees darkened so the layers read as depth.
     * Precomputed so there's no per-frame math.
     */
    fun ridgeColors(season: Season): Ridge = when (season) {
        Season.WINTER -> Ridge(Color(0xFF51645D), Color(0xFF93A2A2), Color(0xFF747B7E))
        Season.SPRING -> Ridge(Color(0xFF474E54), Color(0xFF7C728F), Color(0xFF60526E))
        Season.SUMMER -> Ridge(Color(0xFF3B5B2C), Color(0xFF619035), Color(0xFF4A6B22))
        Season.FOLIAGE -> Ridge(Color(0xFF4B3627), Color(0xFF863C2B), Color(0xFF692519))
        Season.STICK -> Ridge(Color(0xFF47432A), Color(0xFF7C5A32), Color(0xFF613E1F))
    }

    fun backgroundBrush(style: BackgroundStyle): Brush = when (style) {
        BackgroundStyle.SPRUCE -> Brush.verticalGradient(listOf(SkyTop, SkyBottom))
        BackgroundStyle.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF080E0B), Color(0xFF101C15)))
        BackgroundStyle.BLACK -> SolidColor(Color.Black)
    }
}

/** Everything the backdrop needs. */
@Immutable
data class Look(
    val background: Brush,
    val scenery: SceneryMode,
    val ridge: Woodland.Ridge,
    val edgePadding: Dp,
)

/** Everything a tile needs, derived from settings. */
@Immutable
data class TileStyle(
    val width: Dp,
    val height: Dp,
    val iconSize: Dp,
    val corner: Dp,
    val outline: Dp,
    val focusedScale: Float,
    val artwork: TileArtwork,
    val labels: LabelMode,
)

fun tileStyleFor(s: LauncherSettings): TileStyle {
    val (w, h, icon) = when (s.tileSize) {
        TileSize.SMALL -> Triple(160.dp, 100.dp, 40.dp)
        TileSize.MEDIUM -> Triple(196.dp, 124.dp, 52.dp)
        TileSize.LARGE -> Triple(240.dp, 152.dp, 64.dp)
    }
    return TileStyle(
        width = w,
        height = h,
        iconSize = icon,
        corner = s.cornerStyle.dp.dp,
        outline = s.outlineWidth.dp.dp,
        focusedScale = s.focusZoom.scale,
        artwork = s.tileArtwork,
        labels = s.labelMode,
    )
}

/** The current focus accent; changes with the season. */
val LocalAccent = staticCompositionLocalOf { Woodland.accentFor(Season.FOLIAGE) }

val LocalLook = staticCompositionLocalOf {
    Look(Woodland.backgroundBrush(BackgroundStyle.SPRUCE), SceneryMode.FULL, Woodland.ridgeColors(Season.FOLIAGE), 0.dp)
}

val LocalTileStyle = staticCompositionLocalOf { tileStyleFor(LauncherSettings()) }

@Composable
fun NorthEastTheme(settings: LauncherSettings, season: Season, content: @Composable () -> Unit) {
    val accent = Woodland.accentFor(season)
    val look = remember(settings.background, settings.scenery, settings.treeColor, settings.edgePadding, season) {
        Look(
            background = Woodland.backgroundBrush(settings.background),
            scenery = settings.scenery,
            ridge = if (settings.treeColor == TreeColor.CLASSIC) Woodland.ClassicRidge else Woodland.ridgeColors(season),
            edgePadding = settings.edgePadding.dp.dp,
        )
    }
    val tileStyle = remember(settings) { tileStyleFor(settings) }
    val density = LocalDensity.current
    val scaledDensity = remember(density, settings.textSize) {
        Density(density.density, density.fontScale * settings.textSize.scale)
    }
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
            LocalLook provides look,
            LocalTileStyle provides tileStyle,
            LocalDensity provides scaledDensity,
            content = content,
        )
    }
}
