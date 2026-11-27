package com.northeast.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.northeast.launcher.apps.AppCatalog
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.LabelMode
import com.northeast.launcher.data.TileArtwork
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.LocalTileStyle
import com.northeast.launcher.ui.theme.Woodland
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A bark-brown card styled by the tile settings (corners, outline, focus zoom).
 * [highlighted] keeps the accent outline without focus (move mode, "on home" in the picker).
 * [content] receives whether the card currently has focus.
 */
@Composable
fun WoodlandCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable (focused: Boolean) -> Unit,
) {
    val style = LocalTileStyle.current
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(style.corner)
    val outline = Border(border = BorderStroke(style.outline, accent), shape = shape)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (highlighted) Woodland.BarkFocused else Woodland.Bark,
            contentColor = Woodland.Birch,
            focusedContainerColor = Woodland.BarkFocused,
            focusedContentColor = Woodland.Birch,
            pressedContainerColor = Woodland.BarkDim,
            pressedContentColor = Woodland.Birch,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = style.focusedScale),
        border = ClickableSurfaceDefaults.border(
            border = if (highlighted) outline else Border.None,
            focusedBorder = outline,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content(focused) }
    }
}

/**
 * What's drawn inside an app's card: its banner, or its icon + name, per the tile settings.
 * [forceIconAndName] is used by the app list, where names always matter.
 */
@Composable
fun AppTileContent(
    app: LaunchableApp,
    catalog: AppCatalog,
    focused: Boolean,
    modifier: Modifier = Modifier,
    badge: String? = null,
    forceIconAndName: Boolean = false,
    iconSize: Dp = LocalTileStyle.current.iconSize,
) {
    val style = LocalTileStyle.current
    val showLabel = forceIconAndName || when (style.labels) {
        LabelMode.ALWAYS -> true
        LabelMode.ON_FOCUS -> focused
        LabelMode.NEVER -> false
    }
    if (!forceIconAndName && style.artwork == TileArtwork.BANNER) {
        val banner = rememberBanner(app, catalog)
        if (banner != null) {
            Box(modifier = modifier.fillMaxSize()) {
                Image(
                    bitmap = banner,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (showLabel || badge != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0xB3101810))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (showLabel) Label(app.label)
                        if (badge != null) Badge(badge)
                    }
                }
            }
            return
        }
        // No banner: fall through to icon + name.
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        AppIcon(app, catalog, iconSize)
        if (showLabel) Label(app.label)
        if (badge != null) Badge(badge)
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Badge(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = LocalAccent.current, maxLines = 1)
}

@Composable
private fun rememberBanner(app: LaunchableApp, catalog: AppCatalog): ImageBitmap? {
    val style = LocalTileStyle.current
    val density = LocalDensity.current
    val w = with(density) { style.width.roundToPx() }
    val h = with(density) { style.height.roundToPx() }
    val banner by produceState<ImageBitmap?>(initialValue = null, app.key, w, h) {
        value = withContext(Dispatchers.IO) { catalog.loadBanner(app.key, w, h) }
    }
    return banner
}

/** Loads the icon off the main thread; reserves its space meanwhile so nothing jumps. */
@Composable
private fun AppIcon(app: LaunchableApp, catalog: AppCatalog, size: Dp) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, app.key, sizePx) {
        value = withContext(Dispatchers.IO) { catalog.loadIcon(app.key, sizePx) }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Spacer(Modifier.size(size))
    }
}
