package com.northeast.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedAppsRepositoryTest {

    private val youtube = AppKey("com.google.android.youtube.tv", "com.google.android.apps.youtube.tv.activity.ShellActivity")
    private val spotify = AppKey("com.spotify.tv.android", "com.spotify.tv.android.SpotifyTVActivity")
    private val other = AppKey("org.example.other", "org.example.other.Main")

    private fun repo(
        store: KeyValueStore = InMemoryKeyValueStore(),
        defaults: List<String> = listOf("com.google.android.youtube.tv", "com.missing.app", "com.spotify.tv.android"),
    ) = PinnedAppsRepository(store, defaults)

    @Test
    fun loadReturnsNullBeforeFirstSave() {
        assertNull(repo().load())
    }

    @Test
    fun saveThenLoadPreservesOrder() {
        val r = repo()
        r.save(listOf(spotify, other, youtube))
        assertEquals(listOf(spotify, other, youtube), r.load())
    }

    @Test
    fun emptyListIsSavedAndDistinctFromFirstRun() {
        val r = repo()
        r.save(emptyList())
        assertEquals(emptyList<AppKey>(), r.load())
    }

    @Test
    fun dataSurvivesNewRepositoryInstance() {
        val store = InMemoryKeyValueStore()
        repo(store).save(listOf(other, youtube))
        assertEquals(listOf(other, youtube), repo(store).load())
    }

    @Test
    fun firstRunSeedsInstalledDefaultsInDefaultOrder() {
        val r = repo()
        val seeded = r.loadOrSeed(installed = listOf(other, spotify, youtube))
        assertEquals(listOf(youtube, spotify), seeded)
        assertEquals(listOf(youtube, spotify), r.load())
    }

    @Test
    fun seedingHappensOnlyOnce() {
        val r = repo()
        r.loadOrSeed(installed = listOf(youtube, spotify))
        r.save(emptyList()) // user unpinned everything
        assertEquals(emptyList<AppKey>(), r.loadOrSeed(installed = listOf(youtube, spotify)))
    }

    @Test
    fun resetToDefaultsReplacesCustomList() {
        val r = repo()
        r.save(listOf(other))
        assertEquals(listOf(youtube, spotify), r.resetToDefaults(installed = listOf(spotify, other, youtube)))
        assertEquals(listOf(youtube, spotify), r.load())
    }

    @Test
    fun seedingWithNothingInstalledSavesEmptyList() {
        val r = repo()
        assertTrue(r.loadOrSeed(installed = listOf(other)).isEmpty())
        assertEquals(emptyList<AppKey>(), r.load())
    }

    @Test
    fun updateAppliesTransformAndPersists() {
        val r = repo()
        r.save(listOf(youtube, spotify))
        r.update { PinnedOrder.moveLater(it, youtube) }
        assertEquals(listOf(spotify, youtube), r.load())
        r.update { PinnedOrder.add(it, other) }
        assertEquals(listOf(spotify, youtube, other), r.load())
    }

    @Test
    fun saveDropsDuplicates() {
        val r = repo()
        r.save(listOf(youtube, spotify, youtube))
        assertEquals(listOf(youtube, spotify), r.load())
    }

    @Test
    fun decodeSkipsCorruptLinesAndDuplicates() {
        val raw = "v1\n${youtube.flatten()}\ngarbage\n\n${spotify.flatten()}\n${youtube.flatten()}\n/bad"
        assertEquals(listOf(youtube, spotify), PinnedAppsRepository.decode(raw))
    }

    @Test
    fun decodeToleratesMissingOrFutureHeader() {
        assertEquals(listOf(youtube), PinnedAppsRepository.decode(youtube.flatten()))
        assertEquals(listOf(spotify), PinnedAppsRepository.decode("v7\n${spotify.flatten()}"))
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val list = listOf(other, spotify, youtube)
        assertEquals(list, PinnedAppsRepository.decode(PinnedAppsRepository.encode(list)))
    }
}
