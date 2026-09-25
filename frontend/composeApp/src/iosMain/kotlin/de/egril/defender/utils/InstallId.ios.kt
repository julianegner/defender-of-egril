package de.egril.defender.utils

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

private const val KEY_INSTALL_UUID = "install_uuid"
private val installIdSettings = Settings()

actual fun readPersistedInstallUuid(): String? = installIdSettings[KEY_INSTALL_UUID, ""].ifBlank { null }

actual fun persistInstallUuid(uuid: String) {
    installIdSettings[KEY_INSTALL_UUID] = uuid
}
