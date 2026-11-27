package com.northeast.launcher.data

/** Small pure rules used by the home screen, kept here so they can be unit-tested. */
object HomeRules {

    /** True if night dimming should be on at [minuteOfDay] (0–1439). Windows may cross midnight. */
    fun isDimmed(window: NightDimWindow, minuteOfDay: Int): Boolean {
        if (window == NightDimWindow.OFF) return false
        if (window == NightDimWindow.ALWAYS) return true
        val m = minuteOfDay.mod(24 * 60)
        val start = window.startMinute
        val end = window.endMinute
        return if (start < end) m in start until end else m >= start || m < end
    }

    fun greeting(hour: Int): String = when (hour.mod(24)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    /**
     * Where focus goes when moving [delta] (±1) from [index] in a row of [size] items.
     * Returns null when focus should stay (at an end, without wrap-around).
     */
    fun step(index: Int, size: Int, delta: Int, wrap: Boolean): Int? {
        if (size <= 0 || index !in 0 until size) return null
        val next = index + delta
        return when {
            next in 0 until size -> next
            wrap -> next.mod(size)
            else -> null
        }
    }
}
