package com.northeast.launcher.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.northeast.launcher.apps.AppCatalog
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.data.PinnedAppsRepository
import com.northeast.launcher.data.PinnedOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** UI state for the home screen. All mutations happen on the main thread. */
@Stable
class LauncherState(
    val catalog: AppCatalog,
    private val repository: PinnedAppsRepository,
) {
    /** Every launchable app on the device, sorted by name. */
    var installed: List<LaunchableApp> by mutableStateOf(emptyList())
        private set

    /** Saved pin order, including apps that are currently uninstalled (they stay hidden). */
    private var pinnedKeys: List<AppKey> by mutableStateOf(emptyList())

    /** False until the first load finishes, so the UI can avoid flashing an empty state. */
    var loaded: Boolean by mutableStateOf(false)
        private set

    /** Pinned apps that are installed right now, in the user's order. */
    val pinned: List<LaunchableApp>
        get() {
            val byKey = installed.associateBy { it.key }
            return PinnedOrder.visible(pinnedKeys, byKey.keys).mapNotNull(byKey::get)
        }

    /** Installed apps not on the home screen yet, sorted by name. */
    val addable: List<LaunchableApp>
        get() {
            val byKey = installed.associateBy { it.key }
            return PinnedOrder.unpinned(installed.map { it.key }, pinnedKeys).mapNotNull(byKey::get)
        }

    /** Re-reads installed apps (and, on first run, seeds the default pins). */
    suspend fun refresh() {
        val firstLoad = !loaded
        val (apps, keys) = withContext(Dispatchers.IO) {
            val apps = catalog.loadInstalled()
            // Pins are read from disk once; after that the in-memory list is the source of truth,
            // so a refresh finishing late can never undo a move the user just made.
            apps to if (firstLoad) repository.loadOrSeed(apps.map { it.key }) else null
        }
        installed = apps
        if (keys != null && !loaded) pinnedKeys = keys
        loaded = true
    }

    fun pin(app: LaunchableApp) = mutate { PinnedOrder.add(it, app.key) }

    fun unpin(app: LaunchableApp) = mutate { PinnedOrder.remove(it, app.key) }

    /**
     * Moves [app] one visible slot earlier/later. Works on the visible list so hidden
     * (uninstalled) pins never make a move press look like it did nothing.
     */
    fun moveEarlier(app: LaunchableApp) = moveVisible(app, -1)

    fun moveLater(app: LaunchableApp) = moveVisible(app, +1)

    private fun moveVisible(app: LaunchableApp, delta: Int) {
        val visible = pinned.map { it.key }
        val index = visible.indexOf(app.key)
        val neighborIndex = index + delta
        if (index < 0 || neighborIndex !in visible.indices) return
        val neighbor = visible[neighborIndex]
        mutate { full ->
            val from = full.indexOf(app.key)
            val to = full.indexOf(neighbor)
            if (from < 0 || to < 0) full else PinnedOrder.move(full, from, to)
        }
    }

    private fun mutate(transform: (List<AppKey>) -> List<AppKey>) {
        pinnedKeys = PinnedOrder.normalize(transform(pinnedKeys))
        repository.save(pinnedKeys)
    }
}
