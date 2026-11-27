package com.northeast.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.ui.theme.Woodland

/**
 * Every installed app; OK toggles whether it's on the home screen. Items never disappear,
 * so focus never jumps and a mistaken press is undone with another press.
 */
@Composable
fun AddAppsScreen(
    state: LauncherState,
    onToggle: (LaunchableApp, pinnedNow: Boolean) -> Unit,
) {
    val pinnedKeys = state.pinned.map { it.key }.toSet()
    val firstItem = remember { FocusRequester() }

    LaunchedEffect(state.loaded) {
        if (state.loaded) runCatching { firstItem.requestFocus() }
    }

    Scenery {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp, end = 56.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Your home screen apps", style = MaterialTheme.typography.headlineSmall, color = Woodland.Birch)
            Text(
                "OK adds or removes an app. Back when you're done.",
                style = MaterialTheme.typography.bodySmall,
                color = Woodland.BirchFaint,
            )
            val apps = state.pickerApps
            if (apps.isEmpty() && state.loaded) {
                Text(
                    "No launchable apps found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Woodland.BirchMuted,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(top = 20.dp, bottom = 140.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(apps, key = { it.key.flatten() }) { app ->
                    val isPinned = app.key in pinnedKeys
                    val modifier = Modifier.height(116.dp).let {
                        if (app == apps.first()) it.focusRequester(firstItem) else it
                    }
                    WoodlandCard(
                        onClick = { onToggle(app, !isPinned) },
                        highlighted = isPinned,
                        modifier = modifier,
                    ) { focused ->
                        AppTileContent(
                            app = app,
                            catalog = state.catalog,
                            focused = focused,
                            iconSize = 40.dp,
                            badge = if (isPinned) "✓ On home" else null,
                            forceIconAndName = true,
                        )
                    }
                }
            }
        }
    }
}
