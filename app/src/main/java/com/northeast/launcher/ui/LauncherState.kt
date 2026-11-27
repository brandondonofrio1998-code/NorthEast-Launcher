package com.northeast.launcher.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.northeast.launcher.apps.AppCatalog
import com.northeast.launcher.apps.LaunchableApp
import com.northeast.launcher.data.AppKey
import com.northeast.launcher.data.LauncherSettings
import com.northeast.launcher.data.PickerFilter
import com.northeast.launcher.data.PickerSort
import com.northeast.launcher.data.PinnedAppsRepository
import com.northeast.launcher.data.PinnedOrder
import com.northeast.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** UI state for the launcher. All mutations happen on the main thread. */
@Stable
class LauncherState(
    val catalog: AppCatalog,
    private val repository: PinnedAppsRepository,
    private val settingsStore: SettingsStore,
) {
    /**
     * Read synchronously at startup (SharedPreferences, a few ms) so the very first frame
     * already has the right colors and layout.
     */
    var settings: LauncherSettings by mutableStateOf(settingsStore.load())
        private set

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

    /**
     * Apps for the "Your home screen apps" list, per the app-list settings. Apps already on
     * the home screen are always listed so they can be removed, even if filtered out otherwise.
     */
    val pickerApps: List<LaunchableApp>
        get() {
            val pinnedSet = pinnedKeys.toHashSet()
            val filtered = installed.filter {
                settings.pickerFilter == PickerFilter.ALL || it.isTvApp || it.key in pinnedSet
            }
            return when (settings.pickerSort) {
                PickerSort.NAME -> filtered
                PickerSort.TV_FIRST -> filtered.sortedByDescending { it.isTvApp } // stable: keeps A–Z within groups
            }
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

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val updated = transform(settings)
        if (updated == settings) return
        settings = updated
        settingsStore.save(updated)
    }

    /** Resets every setting except the PIN (so a reset can't be used to bypass it). */
    fun resetSettings() = updateSettings { LauncherSettings(pinHash = it.pinHash) }

    fun resetPinnedToDefaults() {
        pinnedKeys = repository.resetToDefaults(installed.map { it.key })
    }

    fun pin(app: LaunchableApp) = mutate { PinnedOrder.add(it, app.key) }

    fun unpin(app: LaunchableApp) = mutate { PinnedOrder.remove(it, app.key) }

    /** Sorts the home screen apps by name (one-off; manual order resumes afterwards). */
    fun sortPinnedByName() {
        val order = pinned.sortedBy { it.label.lowercase() }.map { it.key }
        mutate { full -> order + full.filterNot { it in order } }
    }

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
