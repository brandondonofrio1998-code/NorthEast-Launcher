package com.northeast.launcher.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {

    @Test
    fun correctPinVerifies() {
        assertTrue(Pin.verify("0420", Pin.create("0420")))
    }

    @Test
    fun wrongPinFails() {
        val stored = Pin.create("1111")
        listOf("1112", "0000", "111", "11111", "abcd", "").forEach {
            assertFalse("'$it' should fail", Pin.verify(it, stored))
        }
    }

    @Test
    fun storedValueNeverContainsTheDigitsAsIs() {
        val stored = Pin.create("9876")
        assertTrue(Pin.isStoredFormat(stored))
        assertFalse(stored.split(':')[1] == "9876")
    }

    @Test
    fun saltMakesEachStoredValueDifferent() {
        assertNotEquals(Pin.create("1234"), Pin.create("1234"))
    }

    @Test
    fun validation() {
        assertTrue(Pin.isValid("0000"))
        assertFalse(Pin.isValid("12a4"))
        assertFalse(Pin.isValid("123"))
        assertFalse(Pin.isStoredFormat("garbage"))
        assertFalse(Pin.verify("1234", "garbage"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun createRejectsInvalidPin() {
        Pin.create("12")
    }
}
