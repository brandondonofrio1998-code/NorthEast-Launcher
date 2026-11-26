package com.northeast.launcher.data

/**
 * Apps pinned automatically on first run, in this order, if they are installed.
 * Best effort: a package that isn't installed (or has a different ID on some device)
 * is simply skipped, and everything can be added, removed and reordered with the remote.
 */
object DefaultApps {
    val PACKAGES: List<String> = listOf(
        "com.google.android.youtube.tv", // YouTube (TV)
        "com.crunchyroll.crunchyroid",   // Crunchyroll
        "com.spotify.tv.android",        // Spotify (TV)
        "com.wbd.stream",                // HBO Max
    )
}
