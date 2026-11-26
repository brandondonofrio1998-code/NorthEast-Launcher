package com.northeast.launcher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.data.Season
import com.northeast.launcher.ui.theme.NorthEastTheme

private enum class Screen { Home, Apps }

/**
 * Root of the launcher UI.
 *
 * @param refreshRequests bumped by the activity on start and when apps are installed/removed.
 * @param homePresses bumped when the Home button is pressed while the launcher is already open.
 */
@Composable
fun LauncherApp(
    state: LauncherState,
    season: Season,
    refreshRequests: Int,
    homePresses: Int,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Home) }
    var optionsFor by remember { mutableStateOf<LaunchableApp?>(null) }
    var movingKey by remember { mutableStateOf<AppKey?>(null) }
    var focusTarget by remember { mutableStateOf<AppKey?>(null) }
    var focusRequestId by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshRequests) { state.refresh() }

    // Home pressed while we're already in front: drop back to a clean home screen.
    LaunchedEffect(homePresses) {
        if (homePresses == 0) return@LaunchedEffect
        screen = Screen.Home
        optionsFor = null
        movingKey = null
        focusTarget = null
        focusRequestId++
    }

    // Back never leaves the launcher; it only steps out of modes and sub-screens.
    BackHandler {
        when {
            movingKey != null -> { focusTarget = movingKey; movingKey = null; focusRequestId++ }
            screen == Screen.Apps -> { screen = Screen.Home; focusRequestId++ }
            else -> Unit
        }
    }

    NorthEastTheme(season) {
        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                movingKey = movingKey,
                focusTarget = focusTarget,
                focusRequestId = focusRequestId,
                onOpen = { app ->
                    focusTarget = app.key
                    if (!state.catalog.launch(context, app)) {
                        Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                },
                onOptions = { optionsFor = it },
                onMoveDone = { focusTarget = movingKey; movingKey = null; focusRequestId++ },
                onAddApps = { screen = Screen.Apps },
            )
            Screen.Apps -> AddAppsScreen(
                state = state,
                onToggle = { app, pin -> if (pin) state.pin(app) else state.unpin(app) },
            )
        }

        optionsFor?.let { app ->
            TileOptionsDialog(
                app = app,
                onMove = { optionsFor = null; movingKey = app.key },
                onRemove = {
                    optionsFor = null
                    val index = state.pinned.indexOfFirst { it.key == app.key }
                    state.unpin(app)
                    // Keep focus near where the removed tile was.
                    focusTarget = state.pinned.getOrNull(index)?.key ?: state.pinned.lastOrNull()?.key
                    focusRequestId++
                },
                onDismiss = { optionsFor = null; focusTarget = app.key; focusRequestId++ },
            )
        }
    }
}
