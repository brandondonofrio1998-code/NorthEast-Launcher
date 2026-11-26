package com.northeast.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
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
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.Woodland
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TileShape = RoundedCornerShape(14.dp)

/**
 * A bark-brown card. Focus grows it slightly and draws the seasonal accent outline.
 * [highlighted] keeps the accent outline even without focus (used in move mode and
 * for "already on home" items in the picker).
 */
@Composable
fun WoodlandCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = LocalAccent.current
    val outline = Border(border = BorderStroke(3.dp, accent), shape = TileShape)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (highlighted) Woodland.BarkFocused else Woodland.Bark,
            contentColor = Woodland.Birch,
            focusedContainerColor = Woodland.BarkFocused,
            focusedContentColor = Woodland.Birch,
            pressedContainerColor = Woodland.BarkDim,
            pressedContentColor = Woodland.Birch,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = if (highlighted) outline else Border.None,
            focusedBorder = outline,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/** Icon + name inside a [WoodlandCard]. */
@Composable
fun AppTileContent(
    app: LaunchableApp,
    catalog: AppCatalog,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        AppIcon(app, catalog, iconSize)
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = LocalAccent.current,
                maxLines = 1,
            )
        }
    }
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
