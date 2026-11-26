package com.northeast.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {

    @Test
    fun flattenAndUnflattenRoundTrip() {
        val key = AppKey("com.spotify.tv.android", "com.spotify.tv.android.SpotifyTVActivity")
        assertEquals(key, AppKey.unflatten(key.flatten()))
    }

    @Test
    fun shortActivityNameIsExpandedAgainstPackage() {
        assertEquals(
            AppKey("com.example", "com.example.Main"),
            AppKey.unflatten("com.example/.Main"),
        )
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        assertEquals(AppKey("a.b", "a.b.C"), AppKey.unflatten("  a.b/a.b.C \t"))
    }

    @Test
    fun malformedValuesReturnNull() {
        listOf("", "noslash", "/onlyActivity", "onlyPackage/", "a/b/c", "   ").forEach {
            assertNull("expected null for '$it'", AppKey.unflatten(it))
        }
    }
}
