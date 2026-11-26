package com.northeast.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonTest {

    @Test
    fun everyMonthMapsToExpectedSeason() {
        val expected = mapOf(
            1 to Season.WINTER, 2 to Season.WINTER, 3 to Season.SPRING, 4 to Season.SPRING,
            5 to Season.SPRING, 6 to Season.SUMMER, 7 to Season.SUMMER, 8 to Season.SUMMER,
            9 to Season.FOLIAGE, 10 to Season.FOLIAGE, 11 to Season.STICK, 12 to Season.WINTER,
        )
        expected.forEach { (month, season) -> assertEquals("month $month", season, Season.forMonth(month)) }
    }

    @Test
    fun outOfRangeMonthsWrap() {
        assertEquals(Season.WINTER, Season.forMonth(0))   // = December
        assertEquals(Season.WINTER, Season.forMonth(13))  // = January
        assertEquals(Season.STICK, Season.forMonth(-1))   // = November
    }
}
