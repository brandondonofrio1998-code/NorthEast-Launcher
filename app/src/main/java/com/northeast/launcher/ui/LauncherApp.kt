package com.northeast.launcher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.data.HomePressAction
import com.northeast.launcher.data.HomeRules
import com.northeast.launcher.ui.settings.ArrangeDialog
import com.northeast.launcher.ui.settings.HomeButtonDialog
import com.northeast.launcher.ui.settings.PinSetDialog
import com.northeast.launcher.ui.settings.PinUnlockDialog
import com.northeast.launcher.ui.settings.RemoteTestDialog
import com.northeast.launcher.ui.settings.SettingsDestination
import com.northeast.launcher.ui.settings.SettingsScreen
import com.northeast.launcher.ui.settings.openTvSettings
import com.northeast.launcher.ui.theme.NorthEastTheme
import java.util.Calendar

private enum class Screen { Home, Apps, Settings }

/** Full-screen sub-screens opened from settings (drawn as dialogs over the settings list). */
private enum class Overlay { None, Arrange, RemoteTest, HomeButton, SetPin, UnlockPin }

/**
 * Root of the launcher UI.
 *
 * @param refreshRequests bumped by the activity on start and when apps are installed/removed.
 * @param homePresses bumped when the Home button is pressed while the launcher is already open.
 * @param returns bumped when the user comes back to the launcher from another app.
 */
@Composable
fun LauncherApp(
    state: LauncherState,
    refreshRequests: Int,
    homePresses: Int,
    returns: Int,
) {
    val context = LocalContext.current
    val settings = state.settings
    val now by rememberMinuteTicker()
    val calendar = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val season = settings.colorSeason.resolve(calendar.get(Calendar.MONTH) + 1)
    val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

    var screen by remember { mutableStateOf(Screen.Home) }
    var appsOpenedFromSettings by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(Overlay.None) }
    var optionsFor by remember { mutableStateOf<LaunchableApp?>(null) }
    var movingKey by remember { mutableStateOf<AppKey?>(null) }
    var focusTarget by remember { mutableStateOf<String?>(null) }
    var lastFocused by remember { mutableStateOf<String?>(null) }
    var focusRequestId by remember { mutableIntStateOf(0) }

    fun backToHome(focus: String?) {
        screen = Screen.Home
        overlay = Overlay.None
        focusTarget = focus
        focusRequestId++
    }

    LaunchedEffect(refreshRequests) { state.refresh() }

    // Home pressed while we're already in front: drop back to a clean home screen.
    LaunchedEffect(homePresses) {
        if (homePresses == 0) return@LaunchedEffect
        optionsFor = null
        movingKey = null
        val stay = settings.homePressAction == HomePressAction.STAY && screen == Screen.Home
        backToHome(if (stay) lastFocused else null)
    }

    // Back from another app: highlight the app last used, or the first app.
    LaunchedEffect(returns) {
        if (returns == 0 || screen != Screen.Home) return@LaunchedEffect
        focusTarget = if (settings.rememberFocus) lastFocused else null
        focusRequestId++
    }

    // Back never leaves the launcher; it only steps out of modes and sub-screens.
    BackHandler {
        when {
            movingKey != null -> { focusTarget = movingKey?.flatten(); movingKey = null; focusRequestId++ }
            screen == Screen.Apps && appsOpenedFromSettings -> screen = Screen.Settings
            screen == Screen.Apps -> backToHome(ADD_TILE_KEY)
            screen == Screen.Settings -> backToHome(GEAR_KEY)
            else -> Unit
        }
    }

    NorthEastTheme(settings, season) {
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    state = state,
                    nowMillis = now,
                    season = season,
                    movingKey = movingKey,
                    focusTarget = focusTarget,
                    focusRequestId = focusRequestId,
                    onFocused = { lastFocused = it },
                    onOpen = { app ->
                        if (!state.catalog.launch(context, app)) {
                            Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOptions = { optionsFor = it },
                    onMoveDone = { focusTarget = movingKey?.flatten(); movingKey = null; focusRequestId++ },
                    onAddApps = { appsOpenedFromSettings = false; screen = Screen.Apps },
                    onSettings = {
                        if (settings.hasPin) overlay = Overlay.UnlockPin else screen = Screen.Settings
                    },
                )
                Screen.Apps -> AddAppsScreen(
                    state = state,
                    onToggle = { app, pin -> if (pin) state.pin(app) else state.unpin(app) },
                )
                Screen.Settings -> SettingsScreen(
                    state = state,
                    onOpen = { destination ->
                        when (destination) {
                            SettingsDestination.EDIT_APPS -> { appsOpenedFromSettings = true; screen = Screen.Apps }
                            SettingsDestination.ARRANGE -> overlay = Overlay.Arrange
                            SettingsDestination.REMOTE_TEST -> overlay = Overlay.RemoteTest
                            SettingsDestination.HOME_BUTTON -> overlay = Overlay.HomeButton
                            SettingsDestination.SET_PIN -> overlay = Overlay.SetPin
                            SettingsDestination.TV_SETTINGS -> if (!openTvSettings(context)) {
                                Toast.makeText(context, "Couldn't open TV settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }

            // Night dimming: a translucent layer over the launcher only (apps are unaffected).
            if (HomeRules.isDimmed(settings.nightDim, minuteOfDay)) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.dimStrength.alpha)))
            }
        }

        when (overlay) {
            Overlay.None -> Unit
            Overlay.Arrange -> ArrangeDialog(state, onDone = { overlay = Overlay.None })
            Overlay.RemoteTest -> RemoteTestDialog(state, onDone = { overlay = Overlay.None })
            Overlay.HomeButton -> HomeButtonDialog(state, onDone = { overlay = Overlay.None })
            Overlay.SetPin -> PinSetDialog(
                onSet = { stored -> state.updateSettings { it.copy(pinHash = stored) }; overlay = Overlay.None },
                onCancel = { overlay = Overlay.None },
            )
            Overlay.UnlockPin -> PinUnlockDialog(
                storedHash = settings.pinHash,
                onUnlocked = { overlay = Overlay.None; screen = Screen.Settings },
                onCancel = { overlay = Overlay.None },
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
                    focusTarget = (state.pinned.getOrNull(index) ?: state.pinned.lastOrNull())?.key?.flatten()
                    focusRequestId++
                },
                onDismiss = { optionsFor = null; focusTarget = app.key.flatten(); focusRequestId++ },
            )
        }
    }
}
