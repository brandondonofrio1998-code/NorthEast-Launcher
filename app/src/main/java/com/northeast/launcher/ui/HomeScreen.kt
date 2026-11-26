package com.northeast.launcher.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.Woodland

private val TileWidth = 196.dp
private val TileHeight = 124.dp

/** Stable key for the trailing "Add apps" tile in the row. */
private const val ADD_TILE_KEY = "__add__"

/**
 * The home screen: one centered row of pinned apps plus an "Add apps" tile.
 *
 * Remote controls:
 * - OK opens an app; hold OK (or press Menu) for Move / Remove.
 * - In move mode, ◀ ▶ move the tile and OK or Back puts it down.
 */
@Composable
fun HomeScreen(
    state: LauncherState,
    movingKey: AppKey?,
    focusTarget: AppKey?,
    focusRequestId: Int,
    onOpen: (LaunchableApp) -> Unit,
    onOptions: (LaunchableApp) -> Unit,
    onMoveDone: () -> Unit,
    onAddApps: () -> Unit,
) {
    val pinned = state.pinned
    val requesters = remember { HashMap<Any, FocusRequester>() }
    fun requesterFor(key: Any) = requesters.getOrPut(key) { FocusRequester() }

    // Put focus somewhere sensible whenever asked (first load, Home press, back from a sub-screen,
    // after a move). Falls back to the first tile, then the Add tile.
    LaunchedEffect(focusRequestId, state.loaded, pinned.size, movingKey, pinned.map { it.key }) {
        if (!state.loaded) return@LaunchedEffect
        val target: Any = movingKey
            ?: focusTarget?.takeIf { key -> pinned.any { it.key == key } }
            ?: pinned.firstOrNull()?.key
            ?: ADD_TILE_KEY
        runCatching { requesterFor(target).requestFocus() }
    }

    Scenery {
        Clock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 48.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.loaded) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 56.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.height(TileHeight + 40.dp),
                ) {
                    items(pinned, key = { it.key.flatten() }) { app ->
                        val moving = app.key == movingKey
                        WoodlandCard(
                            onClick = { if (moving) onMoveDone() else onOpen(app) },
                            onLongClick = { if (!moving) onOptions(app) },
                            highlighted = moving,
                            modifier = Modifier
                                .size(TileWidth, TileHeight)
                                .focusRequester(requesterFor(app.key))
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val code = event.nativeKeyEvent.keyCode
                                    when {
                                        moving -> when (code) {
                                            KeyEvent.KEYCODE_DPAD_LEFT -> { state.moveEarlier(app); true }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> { state.moveLater(app); true }
                                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> true
                                            else -> false
                                        }
                                        code == KeyEvent.KEYCODE_MENU -> { onOptions(app); true }
                                        else -> false
                                    }
                                },
                        ) {
                            AppTileContent(
                                app = app,
                                catalog = state.catalog,
                                iconSize = 52.dp,
                                badge = if (moving) "◀  move  ▶" else null,
                            )
                        }
                    }
                    if (movingKey == null) {
                        item(key = ADD_TILE_KEY) {
                            WoodlandCard(
                                onClick = onAddApps,
                                modifier = Modifier
                                    .size(TileWidth, TileHeight)
                                    .focusRequester(requesterFor(ADD_TILE_KEY)),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+", style = MaterialTheme.typography.displaySmall, color = LocalAccent.current)
                                    Text(
                                        if (pinned.isEmpty()) "Add apps" else "Edit apps",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.height(TileHeight + 40.dp))
            }

            Text(
                text = when {
                    movingKey != null -> "◀ ▶ to move  ·  OK to place"
                    pinned.isEmpty() -> "Pick the apps you want on your home screen."
                    else -> "Hold OK on an app to move or remove it"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Woodland.BirchFaint,
            )
        }
    }
}
