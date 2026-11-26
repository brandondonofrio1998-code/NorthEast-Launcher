package com.northeast.launcher.data

/** Minimal persistence seam so storage logic is unit-testable without Android. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** In-memory store, used by unit tests and previews. */
class InMemoryKeyValueStore : KeyValueStore {
    private val map = HashMap<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) {
        map[key] = value
    }
}
