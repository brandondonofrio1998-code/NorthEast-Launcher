package com.northeast.launcher.data

/**
 * Loads and saves the user's pinned apps, in order.
 *
 * Storage format (one string): a version header line, then one "package/activity" per line.
 * Decoding is tolerant: malformed lines and duplicates are dropped rather than failing, so a
 * corrupted file degrades to a shorter list instead of a crash on the home screen.
 */
class PinnedAppsRepository(
    private val store: KeyValueStore,
    private val defaultPackages: List<String> = DefaultApps.PACKAGES,
) {

    /** The saved list, or null if nothing has ever been saved (first run). */
    fun load(): List<AppKey>? = store.getString(KEY_PINNED)?.let(::decode)

    fun save(list: List<AppKey>) {
        store.putString(KEY_PINNED, encode(PinnedOrder.normalize(list)))
    }

    /**
     * Returns the saved list. On first run only, seeds it from [defaultPackages] that are
     * actually installed (in that order) and saves it. If the user later unpins everything,
     * the empty list is respected and nothing is re-seeded.
     */
    fun loadOrSeed(installed: List<AppKey>): List<AppKey> {
        load()?.let { return it }
        return resetToDefaults(installed)
    }

    /** Replaces the saved list with the installed default apps, as on first run. */
    fun resetToDefaults(installed: List<AppKey>): List<AppKey> {
        val seeded = defaultPackages.mapNotNull { pkg ->
            installed.firstOrNull { it.packageName == pkg }
        }
        save(seeded)
        return seeded
    }

    fun update(transform: (List<AppKey>) -> List<AppKey>): List<AppKey> {
        val updated = PinnedOrder.normalize(transform(load().orEmpty()))
        save(updated)
        return updated
    }

    companion object {
        const val KEY_PINNED = "pinned_apps"
        private const val HEADER = "v1"

        fun encode(list: List<AppKey>): String =
            buildString {
                append(HEADER)
                list.forEach { append('\n').append(it.flatten()) }
            }

        fun decode(raw: String): List<AppKey> =
            raw.lineSequence()
                .map(String::trim)
                .filter { it.isNotEmpty() && !isHeader(it) }
                .mapNotNull(AppKey::unflatten)
                .distinct()
                .toList()

        private fun isHeader(line: String): Boolean =
            line.length >= 2 && line[0] == 'v' && line.substring(1).all(Char::isDigit)
    }
}
