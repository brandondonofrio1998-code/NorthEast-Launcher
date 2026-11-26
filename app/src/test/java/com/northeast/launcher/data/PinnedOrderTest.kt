package com.northeast.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PinnedOrderTest {

    private val a = AppKey("pkg.a", "pkg.a.Main")
    private val b = AppKey("pkg.b", "pkg.b.Main")
    private val c = AppKey("pkg.c", "pkg.c.Main")
    private val d = AppKey("pkg.d", "pkg.d.Main")

    @Test
    fun addAppendsToEnd() {
        assertEquals(listOf(a, b, c), PinnedOrder.add(listOf(a, b), c))
    }

    @Test
    fun addIgnoresDuplicates() {
        val list = listOf(a, b)
        assertSame(list, PinnedOrder.add(list, a))
    }

    @Test
    fun removeDropsOnlyThatApp() {
        assertEquals(listOf(a, c), PinnedOrder.remove(listOf(a, b, c), b))
    }

    @Test
    fun removeMissingAppIsNoOp() {
        assertEquals(listOf(a, b), PinnedOrder.remove(listOf(a, b), c))
    }

    @Test
    fun moveForwardAndBackward() {
        val list = listOf(a, b, c, d)
        assertEquals(listOf(b, c, a, d), PinnedOrder.move(list, 0, 2))
        assertEquals(listOf(d, a, b, c), PinnedOrder.move(list, 3, 0))
    }

    @Test
    fun moveClampsTargetToBounds() {
        val list = listOf(a, b, c)
        assertEquals(listOf(b, c, a), PinnedOrder.move(list, 0, 99))
        assertEquals(listOf(c, a, b), PinnedOrder.move(list, 2, -5))
    }

    @Test
    fun moveWithInvalidSourceIsNoOp() {
        val list = listOf(a, b, c)
        assertSame(list, PinnedOrder.move(list, 7, 0))
        assertSame(list, PinnedOrder.move(list, -1, 0))
        assertEquals(emptyList<AppKey>(), PinnedOrder.move(emptyList(), 0, 1))
    }

    @Test
    fun moveEarlierAndLater() {
        val list = listOf(a, b, c)
        assertEquals(listOf(b, a, c), PinnedOrder.moveEarlier(list, b))
        assertEquals(listOf(a, c, b), PinnedOrder.moveLater(list, b))
    }

    @Test
    fun moveEarlierAtStartAndLaterAtEndAreNoOps() {
        val list = listOf(a, b, c)
        assertSame(list, PinnedOrder.moveEarlier(list, a))
        assertSame(list, PinnedOrder.moveLater(list, c))
        assertSame(list, PinnedOrder.moveLater(list, d))
    }

    @Test
    fun visibleHidesUninstalledButKeepsOrder() {
        assertEquals(listOf(a, c), PinnedOrder.visible(listOf(a, b, c), setOf(c, a)))
        assertEquals(listOf(c, a), PinnedOrder.visible(listOf(c, b, a), setOf(a, c)))
    }

    @Test
    fun normalizeRemovesDuplicatesKeepingFirst() {
        assertEquals(listOf(b, a, c), PinnedOrder.normalize(listOf(b, a, b, c, a)))
    }
}
