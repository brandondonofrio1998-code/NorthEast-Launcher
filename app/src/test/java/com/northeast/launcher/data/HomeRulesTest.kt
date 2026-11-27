package com.northeast.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRulesTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun dimWindowCrossingMidnight() {
        val w = NightDimWindow.TEN_TO_SEVEN
        assertFalse(HomeRules.isDimmed(w, at(21, 59)))
        assertTrue(HomeRules.isDimmed(w, at(22)))
        assertTrue(HomeRules.isDimmed(w, at(0)))
        assertTrue(HomeRules.isDimmed(w, at(6, 59)))
        assertFalse(HomeRules.isDimmed(w, at(7)))
        assertFalse(HomeRules.isDimmed(w, at(12)))
    }

    @Test
    fun dimOffAndAlways() {
        (0 until 24).forEach { h ->
            assertFalse(HomeRules.isDimmed(NightDimWindow.OFF, at(h)))
            assertTrue(HomeRules.isDimmed(NightDimWindow.ALWAYS, at(h)))
        }
    }

    @Test
    fun greetings() {
        assertEquals("Good morning", HomeRules.greeting(5))
        assertEquals("Good morning", HomeRules.greeting(11))
        assertEquals("Good afternoon", HomeRules.greeting(12))
        assertEquals("Good evening", HomeRules.greeting(21))
        assertEquals("Good night", HomeRules.greeting(23))
        assertEquals("Good night", HomeRules.greeting(3))
    }

    @Test
    fun stepWithoutWrapStopsAtEnds() {
        assertEquals(1, HomeRules.step(0, 3, +1, wrap = false))
        assertNull(HomeRules.step(2, 3, +1, wrap = false))
        assertNull(HomeRules.step(0, 3, -1, wrap = false))
    }

    @Test
    fun stepWithWrapCycles() {
        assertEquals(0, HomeRules.step(2, 3, +1, wrap = true))
        assertEquals(2, HomeRules.step(0, 3, -1, wrap = true))
    }

    @Test
    fun stepRejectsBadInput() {
        assertNull(HomeRules.step(0, 0, +1, wrap = true))
        assertNull(HomeRules.step(5, 3, +1, wrap = true))
    }
}
