package com.northeast.launcher.data

/**
 * New England's seasons as they look outside, used to pick the focus accent color.
 * Based only on the device's own date — no network, no location.
 */
enum class Season(val displayName: String) {
    /** December – February: snow, frost, bare birches. */
    WINTER("Winter"),

    /** March – May: mud season into lilac time. */
    SPRING("Spring"),

    /** June – August: long sunny days. */
    SUMMER("Summer"),

    /** September – October: peak foliage, red maples. */
    FOLIAGE("Foliage"),

    /** November: stick season — bare branches, last copper oak leaves. */
    STICK("Stick season");

    companion object {
        /** [month] is 1–12 (January = 1). Out-of-range values wrap around. */
        fun forMonth(month: Int): Season =
            when (((month - 1).mod(12)) + 1) {
                12, 1, 2 -> WINTER
                3, 4, 5 -> SPRING
                6, 7, 8 -> SUMMER
                9, 10 -> FOLIAGE
                else -> STICK // 11
            }
    }
}
