package com.northeast.launcher.home

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One remote button press, for the Remote button test screen. Kept in memory only. */
data class KeyRecord(
    val keyCode: Int,
    val scanCode: Int,
    val name: String,
    /** When the button went down; the same press seen by the service and the app shares it. */
    val downTime: Long,
    /** True if the Home-button service saw it (keys the system never delivers to apps). */
    val viaService: Boolean,
)

/**
 * In-memory log of recent key presses while the Remote button test is open.
 * Nothing is recorded, stored or sent anywhere when the test screen is closed.
 */
object KeyLog {
    private const val MAX = 12

    private val _records = MutableStateFlow<List<KeyRecord>>(emptyList())
    val records: StateFlow<List<KeyRecord>> = _records

    /** Set while the test screen is open; the service then reports keys instead of acting on them. */
    @Volatile
    var testing: Boolean = false
        private set

    fun startTest() {
        _records.value = emptyList()
        testing = true
    }

    fun stopTest() {
        testing = false
        _records.value = emptyList()
    }

    fun record(event: KeyEvent, viaService: Boolean) {
        if (!testing || event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return
        val record = KeyRecord(
            keyCode = event.keyCode,
            scanCode = event.scanCode,
            name = nameOf(event.keyCode),
            downTime = event.downTime,
            viaService = viaService,
        )
        // With the service on, a press reaches both the service and the app: list it once.
        if (_records.value.any { it.keyCode == record.keyCode && it.downTime == record.downTime }) return
        _records.value = (listOf(record) + _records.value).take(MAX)
    }

    fun nameOf(keyCode: Int): String = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")

    /** Navigation keys can't be used as an extra Home key (they're needed to move around). */
    fun isReserved(keyCode: Int): Boolean = keyCode in setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
    )
}
