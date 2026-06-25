package de.egril.defender.utils

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlin.random.Random

private const val KEY_INSTALL_UUID = "install_uuid"
private val installIdSettings = Settings()

fun getOrCreateInstallUuid(): String {
    val existing = installIdSettings[KEY_INSTALL_UUID, ""]
    if (existing.isNotBlank()) {
        return existing
    }
    val created = generateUuidV4()
    installIdSettings[KEY_INSTALL_UUID] = created
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
