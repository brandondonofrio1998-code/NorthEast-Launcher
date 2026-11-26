package com.northeast.launcher.data

/**
 * Identifies one launchable activity, e.g. YouTube's TV activity.
 * Stored as "package/activity" (Android's own ComponentName flattening format).
 */
data class AppKey(val packageName: String, val activityName: String) {

    fun flatten(): String = "$packageName/$activityName"

    companion object {
        /** Parses "package/activity". Returns null for anything malformed. */
        fun unflatten(value: String): AppKey? {
            val trimmed = value.trim()
            val slash = trimmed.indexOf('/')
            if (slash <= 0 || slash == trimmed.lastIndex) return null
            val pkg = trimmed.substring(0, slash)
            var activity = trimmed.substring(slash + 1)
            if (activity.contains('/')) return null
            // Android allows ".ShortName" relative to the package.
            if (activity.startsWith('.')) activity = pkg + activity
            return AppKey(pkg, activity)
        }
    }
}
