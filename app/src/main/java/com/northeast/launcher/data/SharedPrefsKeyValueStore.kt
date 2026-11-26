package com.northeast.launcher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed store. Chosen over DataStore for this tiny payload: it is read
 * synchronously on startup (no coroutine hop before first frame) and adds no dependency.
 */
class SharedPrefsKeyValueStore(context: Context) : KeyValueStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private companion object {
        const val FILE_NAME = "launcher"
    }
}
