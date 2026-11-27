package com.northeast.launcher.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 4-digit settings PIN. Stored as "salt:sha256(salt + pin)" (hex), never as the digits.
 * It keeps family members and residents out of the settings; it is not strong security
 * (anyone with ADB access can clear the app's data).
 */
object Pin {
    const val LENGTH = 4

    fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all { it in '0'..'9' }

    fun create(pin: String, random: SecureRandom = SecureRandom()): String {
        require(isValid(pin)) { "PIN must be $LENGTH digits" }
        val salt = ByteArray(8).also(random::nextBytes).toHex()
        return "$salt:${hash(salt, pin)}"
    }

    fun verify(pin: String, stored: String): Boolean {
        if (!isValid(pin) || !isStoredFormat(stored)) return false
        val (salt, expected) = stored.split(':')
        return MessageDigest.isEqual(hash(salt, pin).toByteArray(), expected.toByteArray())
    }

    fun isStoredFormat(stored: String): Boolean {
        val parts = stored.split(':')
        return parts.size == 2 && parts[0].length == 16 && parts[1].length == 64 &&
            parts.all { part -> part.all { it in '0'..'9' || it in 'a'..'f' } }
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
