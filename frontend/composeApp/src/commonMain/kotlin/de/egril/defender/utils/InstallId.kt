package de.egril.defender.utils

import kotlin.random.Random

/**
 * Reads the previously persisted install UUID, if any exists.
 * Returns null (or blank) when no id has been stored yet.
 */
expect fun readPersistedInstallUuid(): String?

/**
 * Persists the given install UUID so it can be read back on subsequent app launches.
 */
expect fun persistInstallUuid(uuid: String)

fun getOrCreateInstallUuid(): String {
    val existing = readPersistedInstallUuid()
    if (!existing.isNullOrBlank()) {
        return existing
    }
    val created = generateUuidV4()
    persistInstallUuid(created)
    return created
}

private fun generateUuidV4(): String {
    val bytes = ByteArray(16)
    Random.Default.nextBytes(bytes)
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte() // version 4
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // variant 10xx
    val hexChars = "0123456789abcdef"
    return buildString {
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            append(hexChars[value ushr 4])
            append(hexChars[value and 0x0F])
            if (index == 3 || index == 5 || index == 7 || index == 9) append('-')
        }
    }
}
