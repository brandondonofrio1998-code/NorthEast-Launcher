package com.northeast.launcher.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.data.BackgroundStyle
import com.northeast.launcher.data.ClockFormat
import com.northeast.launcher.data.ColorSeason
import com.northeast.launcher.data.CornerStyle
import com.northeast.launcher.data.DimStrength
import com.northeast.launcher.data.EdgePadding
import com.northeast.launcher.data.FocusZoom
import com.northeast.launcher.data.HomePressAction
import com.northeast.launcher.data.LabelMode
import com.northeast.launcher.data.LauncherSettings
import com.northeast.launcher.data.NightDimWindow
import com.northeast.launcher.data.OutlineWidth
import com.northeast.launcher.data.PickerFilter
import com.northeast.launcher.data.PickerSort
import com.northeast.launcher.data.RowPosition
import com.northeast.launcher.data.SceneryMode
import com.northeast.launcher.data.TextSize
import com.northeast.launcher.data.TileArtwork
import com.northeast.launcher.data.TileSize
import com.northeast.launcher.data.TreeColor
import com.northeast.launcher.home.HomeButtonService
import com.northeast.launcher.home.KeyLog
import com.northeast.launcher.ui.LauncherState
import com.northeast.launcher.ui.Scenery
import com.northeast.launcher.ui.theme.LocalAccent
import com.northeast.launcher.ui.theme.LocalTileStyle
import com.northeast.launcher.ui.theme.Woodland

/** What the settings screen can open. */
enum class SettingsDestination { EDIT_APPS, ARRANGE, REMOTE_TEST, HOME_BUTTON, SET_PIN, TV_SETTINGS }

/**
 * Every launcher option in one scrolling list, grouped into sections. Changes apply and save
 * immediately; the panel on the right previews the tile style.
 */
@Composable
fun SettingsScreen(
    state: LauncherState,
    onOpen: (SettingsDestination) -> Unit,
) {
    val s = state.settings
    fun set(transform: (LauncherSettings) -> LauncherSettings) = state.updateSettings(transform)
    val context = LocalContext.current
    val serviceOn = HomeButtonService.isEnabled(context)
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    val firstRow = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }

    Scenery {
        Row(Modifier.fillMaxSize().padding(start = 48.dp, end = 40.dp, top = 32.dp)) {
            Column(Modifier.weight(0.62f).fillMaxHeight()) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Woodland.Birch)
                Text(
                    "OK or ◀ ▶ changes a setting. Changes save right away. Back to leave.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Woodland.BirchFaint,
                )
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    section("Look & color") {
                        item {
                            Box(Modifier.focusRequester(firstRow)) {
                                ChoiceRow(
                                    "Color theme", ColorSeason.entries, s.colorSeason, { it.label },
                                    { v -> set { it.copy(colorSeason = v) } },
                                    description = "Highlight color. Automatic follows New England's seasons by date.",
                                )
                            }
                        }
                        item {
                            ChoiceRow("Tree color", TreeColor.entries, s.treeColor, { it.label }, { v -> set { it.copy(treeColor = v) } })
                        }
                        item {
                            ChoiceRow("Scenery", SceneryMode.entries, s.scenery, { it.label }, { v -> set { it.copy(scenery = v) } })
                        }
                        item {
                            ChoiceRow("Background", BackgroundStyle.entries, s.background, { it.label }, { v -> set { it.copy(background = v) } })
                        }
                        item {
                            ChoiceRow(
                                "Night dimming", NightDimWindow.entries, s.nightDim, { it.label },
                                { v -> set { it.copy(nightDim = v) } },
                                description = "Darkens the home screen at night. Apps aren't affected.",
                            )
                        }
                        item {
                            ChoiceRow("Dimming strength", DimStrength.entries, s.dimStrength, { it.label }, { v -> set { it.copy(dimStrength = v) } })
                        }
                    }
                    section("App tiles") {
                        item { ChoiceRow("Tile size", TileSize.entries, s.tileSize, { it.label }, { v -> set { it.copy(tileSize = v) } }) }
                        item {
                            ChoiceRow(
                                "Tile artwork", TileArtwork.entries, s.tileArtwork, { it.label },
                                { v -> set { it.copy(tileArtwork = v) } },
                                description = "App banner uses each app's own wide TV artwork when it has one.",
                            )
                        }
                        item { ChoiceRow("App names", LabelMode.entries, s.labelMode, { it.label }, { v -> set { it.copy(labelMode = v) } }) }
                        item { ChoiceRow("Highlight zoom", FocusZoom.entries, s.focusZoom, { it.label }, { v -> set { it.copy(focusZoom = v) } }) }
                        item { ChoiceRow("Highlight outline", OutlineWidth.entries, s.outlineWidth, { it.label }, { v -> set { it.copy(outlineWidth = v) } }) }
                        item { ChoiceRow("Corners", CornerStyle.entries, s.cornerStyle, { it.label }, { v -> set { it.copy(cornerStyle = v) } }) }
                        item { ChoiceRow("Row position", RowPosition.entries, s.rowPosition, { it.label }, { v -> set { it.copy(rowPosition = v) } }) }
                        item {
                            ToggleRow(
                                "\"Edit apps\" tile", s.showEditTile, { v -> set { it.copy(showEditTile = v) } },
                                description = "When off, edit apps from here instead.",
                            )
                        }
                    }
                    section("Clock & text") {
                        item { ToggleRow("Clock", s.showClock, { v -> set { it.copy(showClock = v) } }) }
                        item { ChoiceRow("Clock format", ClockFormat.entries, s.clockFormat, { it.label }, { v -> set { it.copy(clockFormat = v) } }) }
                        item { ToggleRow("Date", s.showDate, { v -> set { it.copy(showDate = v) } }) }
                        item { ToggleRow("Greeting", s.showGreeting, { v -> set { it.copy(showGreeting = v) } }, description = "\"Good morning\", \"Good evening\"…") }
                        item { ToggleRow("Season name", s.showSeasonName, { v -> set { it.copy(showSeasonName = v) } }) }
                        item { ToggleRow("Tips line", s.showHint, { v -> set { it.copy(showHint = v) } }, description = "The hint under the apps.") }
                        item { ChoiceRow("Text size", TextSize.entries, s.textSize, { it.label }, { v -> set { it.copy(textSize = v) } }) }
                        item {
                            ChoiceRow(
                                "Screen edge padding", EdgePadding.entries, s.edgePadding, { it.label },
                                { v -> set { it.copy(edgePadding = v) } },
                                description = "Use this if your TV cuts off the edges of the picture.",
                            )
                        }
                    }
                    section("Navigation & apps") {
                        item {
                            ToggleRow(
                                "Wrap around", s.wrapAround, { v -> set { it.copy(wrapAround = v) } },
                                description = "▶ on the last app jumps back to the first.",
                            )
                        }
                        item {
                            ToggleRow(
                                "Return to last app", s.rememberFocus, { v -> set { it.copy(rememberFocus = v) } },
                                description = "Coming back home highlights the app you last used. Off: always the first app.",
                            )
                        }
                        item {
                            ChoiceRow(
                                "Home button on the home screen", HomePressAction.entries, s.homePressAction, { it.label },
                                { v -> set { it.copy(homePressAction = v) } },
                            )
                        }
                        item { ActionRow("Edit home screen apps", onClick = { onOpen(SettingsDestination.EDIT_APPS) }) }
                        item { ActionRow("Arrange home screen apps", onClick = { onOpen(SettingsDestination.ARRANGE) }, description = "Reorder with ▲ ▼.") }
                        item { ActionRow("Sort home screen apps A – Z", onClick = state::sortPinnedByName, value = null, confirm = true) }
                        item { ChoiceRow("App list shows", PickerFilter.entries, s.pickerFilter, { it.label }, { v -> set { it.copy(pickerFilter = v) } }) }
                        item { ChoiceRow("App list order", PickerSort.entries, s.pickerSort, { it.label }, { v -> set { it.copy(pickerSort = v) } }) }
                    }
                    section("Home button & remote") {
                        item {
                            ActionRow(
                                "Home button setup",
                                onClick = { onOpen(SettingsDestination.HOME_BUTTON) },
                                value = if (serviceOn) "Service on" else "Service off",
                                description = "Lets the remote's Home button open NorthEast on Google TV.",
                            )
                        }
                        item {
                            ToggleRow(
                                "Home button takeover", s.homeTakeover, { v -> set { it.copy(homeTakeover = v) } },
                                description = "Off lets the Home button work the stock way, without turning the service off.",
                            )
                        }
                        item {
                            SettingRow(
                                title = "Extra Home button",
                                value = if (s.extraHomeKey == 0) "None" else KeyLog.nameOf(s.extraHomeKey),
                                description = if (s.extraHomeKey == 0) "Choose one in Remote button test." else "OK clears it.",
                                onClick = { set { it.copy(extraHomeKey = 0) } },
                            )
                        }
                        item { ActionRow("Remote button test", onClick = { onOpen(SettingsDestination.REMOTE_TEST) }, description = "See what each remote button sends.") }
                        item { ActionRow("Open Android TV settings", onClick = { onOpen(SettingsDestination.TV_SETTINGS) }, description = "Network, display, accounts, apps…") }
                    }
                    section("Lock") {
                        item {
                            ToggleRow(
                                "Lock home screen layout", s.layoutLocked, { v -> set { it.copy(layoutLocked = v) } },
                                description = "Stops apps from being added, removed or moved by accident.",
                            )
                        }
                        if (s.hasPin) {
                            item { ActionRow("Change settings PIN", onClick = { onOpen(SettingsDestination.SET_PIN) }) }
                            item { ActionRow("Remove settings PIN", onClick = { set { it.copy(pinHash = "") } }, value = "On", confirm = true) }
                        } else {
                            item {
                                ActionRow(
                                    "Set a settings PIN", onClick = { onOpen(SettingsDestination.SET_PIN) }, value = "Off",
                                    description = "Asks for a 4-digit PIN before settings open.",
                                )
                            }
                        }
                    }
                    section("Reset & about") {
                        item { ActionRow("Reset home screen apps", onClick = state::resetPinnedToDefaults, value = null, confirm = true, description = "Back to YouTube, Crunchyroll, Spotify, HBO Max (if installed).") }
                        item { ActionRow("Reset all settings", onClick = state::resetSettings, value = null, confirm = true, description = "Keeps your apps and your PIN.") }
                        item {
                            SettingRow(
                                title = "About NorthEast",
                                value = "v$version",
                                description = "No ads, no tracking, no internet access. Everything stays on this device.",
                                onClick = {},
                            )
                        }
                    }
                }
            }
            TilePreview(Modifier.weight(0.38f).padding(start = 32.dp, top = 72.dp))
        }
    }
}

private fun LazyListScope.section(title: String, rows: LazyListScope.() -> Unit) {
    item { SectionHeader(title) }
    rows()
}

/** A non-interactive preview of two tiles in the current style: one highlighted, one not. */
@Composable
private fun TilePreview(modifier: Modifier = Modifier) {
    val style = LocalTileStyle.current
    val accent = LocalAccent.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Preview", style = MaterialTheme.typography.titleMedium, color = Woodland.BirchMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            PreviewTile(highlighted = true, label = "Highlighted", accent = accent, scale = style.focusedScale)
            PreviewTile(highlighted = false, label = "App", accent = accent, scale = 1f)
        }
    }
}

@Composable
private fun PreviewTile(highlighted: Boolean, label: String, accent: androidx.compose.ui.graphics.Color, scale: Float) {
    val style = LocalTileStyle.current
    val shape = RoundedCornerShape(style.corner)
    // Preview at 70% size so two fit beside the list.
    val w = style.width * 0.7f
    val h = style.height * 0.7f
    val showLabel = when (style.labels) {
        LabelMode.ALWAYS -> true
        LabelMode.ON_FOCUS -> highlighted
        LabelMode.NEVER -> false
    }
    Box(
        modifier = Modifier
            .scale(scale)
            .size(w, h)
            .background(if (highlighted) Woodland.BarkFocused else Woodland.Bark, shape)
            .then(if (highlighted) Modifier.border(BorderStroke(style.outline, accent), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (style.artwork == TileArtwork.BANNER) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .background(Woodland.BarkDim, RoundedCornerShape(style.corner / 2)),
                contentAlignment = Alignment.Center,
            ) { Text("Banner", style = MaterialTheme.typography.labelSmall, color = Woodland.BirchFaint) }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(style.iconSize * 0.7f).background(accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp)))
                if (showLabel) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Woodland.Birch, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
