package com.northeast.launcher.data

/**
 * Pure list operations for the pinned-app order. Every function returns a new list
 * and never throws for out-of-range input, because remote-control input can arrive
 * faster than the UI updates.
 */
object PinnedOrder {

    /** Appends [key] unless it is already pinned. */
    fun add(list: List<AppKey>, key: AppKey): List<AppKey> =
        if (key in list) list else list + key

    fun remove(list: List<AppKey>, key: AppKey): List<AppKey> = list - key

    /** Moves the item at [from] to index [to]; both are clamped to the list bounds. */
    fun move(list: List<AppKey>, from: Int, to: Int): List<AppKey> {
        if (list.isEmpty() || from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        if (target == from) return list
        val result = list.toMutableList()
        val item = result.removeAt(from)
        result.add(target, item)
        return result
    }

    /** Moves [key] one step earlier. No-op if it is first or not pinned. */
    fun moveEarlier(list: List<AppKey>, key: AppKey): List<AppKey> {
        val index = list.indexOf(key)
        return if (index <= 0) list else move(list, index, index - 1)
    }

    /** Moves [key] one step later. No-op if it is last or not pinned. */
    fun moveLater(list: List<AppKey>, key: AppKey): List<AppKey> {
        val index = list.indexOf(key)
        return if (index < 0 || index == list.lastIndex) list else move(list, index, index + 1)
    }

    /**
     * Pinned apps that are currently installed, in pinned order. Uninstalled apps are hidden,
     * not deleted, so a reinstall (or an app update that briefly removes it) restores the tile
     * in its old spot.
     */
    fun visible(list: List<AppKey>, installed: Set<AppKey>): List<AppKey> =
        list.filter { it in installed }

    /** Removes duplicates, keeping the first occurrence. */
    fun normalize(list: List<AppKey>): List<AppKey> = list.distinct()
}
