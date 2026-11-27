package com.northeast.launcher.ui

import android.view.KeyEvent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.R
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.data.HomeRules
import com.northeast.launcher.data.RowPosition
import com.northeast.launcher.data.Season
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.LocalTileStyle
import com.northeast.launcher.ui.theme.Woodland
import kotlinx.coroutines.launch

/** Stable key for the trailing "Add / Edit apps" tile in the row. */
const val ADD_TILE_KEY = "__add__"

/** Stable key for the settings gear. */
const val GEAR_KEY = "__gear__"

/**
 * The home screen: one row of pinned apps (+ an "Edit apps" tile), clock and info top-right,
 * settings gear top-left.
 *
 * Remote controls:
 * - OK opens an app; hold OK (or press Menu) for Move / Remove (unless the layout is locked).
 * - In move mode, ◀ ▶ move the tile and OK or Back puts it down.
 * - ▲ from the row reaches the settings gear.
 */
@Composable
fun HomeScreen(
    state: LauncherState,
    nowMillis: Long,
    season: Season,
    movingKey: AppKey?,
    focusTarget: String?,
    focusRequestId: Int,
    onFocused: (String) -> Unit,
    onOpen: (LaunchableApp) -> Unit,
    onOptions: (LaunchableApp) -> Unit,
    onMoveDone: () -> Unit,
    onAddApps: () -> Unit,
    onSettings: () -> Unit,
) {
    val settings = state.settings
    val tile = LocalTileStyle.current
    val pinned = state.pinned
    val locked = settings.layoutLocked
    val showEditTile = settings.showEditTile && !locked && movingKey == null
    val rowKeys: List<String> = pinned.map { it.key.flatten() } + if (showEditTile) listOf(ADD_TILE_KEY) else emptyList()

    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val requesters = remember { HashMap<String, FocusRequester>() }
    fun requesterFor(key: String) = requesters.getOrPut(key) { FocusRequester() }

    suspend fun focusKey(key: String) {
        val index = rowKeys.indexOf(key)
        if (index >= 0) {
            rowState.scrollToItem(index)
            withFrameNanos { } // let the item compose before focusing it
        }
        runCatching { requesterFor(key).requestFocus() }
    }

    /** Wrap-around: jump from one end of the row to the other. Returns true if handled. */
    fun wrapFrom(key: String, delta: Int): Boolean {
        if (!settings.wrapAround) return false
        val index = rowKeys.indexOf(key)
        val next = HomeRules.step(index, rowKeys.size, delta, wrap = true) ?: return false
        if (next == index + delta) return false // ordinary step: let normal focus movement handle it
        scope.launch { focusKey(rowKeys[next]) }
        return true
    }

    // Put focus somewhere sensible whenever asked (first load, Home press, back from a sub-screen).
    LaunchedEffect(focusRequestId, state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        val target = focusTarget?.takeIf { it in rowKeys || it == GEAR_KEY } ?: rowKeys.firstOrNull() ?: GEAR_KEY
        focusKey(target)
    }
    // Keep focus on a tile while it is being moved.
    LaunchedEffect(movingKey, rowKeys) {
        movingKey?.let { focusKey(it.flatten()) }
    }

    Scenery {
        // Settings gear, top-left.
        WoodlandCard(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp, start = 40.dp)
                .size(56.dp)
                .focusRequester(requesterFor(GEAR_KEY))
                .onFocusChanged { if (it.isFocused) onFocused(GEAR_KEY) },
        ) {
            Image(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                colorFilter = ColorFilter.tint(Woodland.Birch),
                modifier = Modifier.size(28.dp),
            )
        }

        TopInfo(
            nowMillis = nowMillis,
            settings = settings,
            season = season,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 48.dp),
        )

        val rowAlignment = when (settings.rowPosition) {
            RowPosition.UPPER -> Alignment.TopCenter
            RowPosition.CENTER -> Alignment.Center
            RowPosition.LOWER -> Alignment.BottomCenter
        }
        val rowPadding = when (settings.rowPosition) {
            RowPosition.UPPER -> Modifier.padding(top = 150.dp)
            RowPosition.CENTER -> Modifier
            RowPosition.LOWER -> Modifier.padding(bottom = 120.dp)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(rowAlignment)
                .then(rowPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.loaded) {
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 56.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.height(tile.height + 40.dp),
                ) {
                    items(pinned, key = { it.key.flatten() }) { app ->
                        val key = app.key.flatten()
                        val moving = app.key == movingKey
                        WoodlandCard(
                            onClick = { if (moving) onMoveDone() else onOpen(app) },
                            onLongClick = { if (!moving && !locked) onOptions(app) },
                            highlighted = moving,
                            modifier = Modifier
                                .size(tile.width, tile.height)
                                .focusRequester(requesterFor(key))
                                .onFocusChanged { if (it.isFocused) onFocused(key) }
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
                                        code == KeyEvent.KEYCODE_MENU -> { if (!locked) onOptions(app); true }
                                        code == KeyEvent.KEYCODE_DPAD_LEFT -> wrapFrom(key, -1)
                                        code == KeyEvent.KEYCODE_DPAD_RIGHT -> wrapFrom(key, +1)
                                        else -> false
                                    }
                                },
                        ) { focused ->
                            AppTileContent(
                                app = app,
                                catalog = state.catalog,
                                focused = focused,
                                badge = if (moving) "◀  move  ▶" else null,
                            )
                        }
                    }
                    if (showEditTile) {
                        item(key = ADD_TILE_KEY) {
                            WoodlandCard(
                                onClick = onAddApps,
                                modifier = Modifier
                                    .size(tile.width, tile.height)
                                    .focusRequester(requesterFor(ADD_TILE_KEY))
                                    .onFocusChanged { if (it.isFocused) onFocused(ADD_TILE_KEY) }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_LEFT -> wrapFrom(ADD_TILE_KEY, -1)
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> wrapFrom(ADD_TILE_KEY, +1)
                                            else -> false
                                        }
                                    },
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
                Box(Modifier.height(tile.height + 40.dp))
            }

            if (settings.showHint || pinned.isEmpty() || movingKey != null) {
                Text(
                    text = when {
                        movingKey != null -> "◀ ▶ to move  ·  OK to place"
                        pinned.isEmpty() && locked -> "The home screen is locked. ▲ for settings."
                        pinned.isEmpty() -> "Pick the apps you want on your home screen."
                        locked -> "▲ for settings"
                        else -> "Hold OK on an app to move or remove it  ·  ▲ for settings"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Woodland.BirchFaint,
                )
            }
        }
    }
}
