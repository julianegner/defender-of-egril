package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object BackendSettingsService {

    actual suspend fun uploadSettings(settingsJson: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val status = jvmHttpPost("/api/settings", buildSettingsUploadJson(settingsJson), token)
            status in 200..299
        }

    actual suspend fun fetchSettings(token: String): Map<String, String>? =
        withContext(Dispatchers.IO) {
            val json = jvmHttpGet("/api/settings", token) ?: return@withContext null
            parseSettingsResponseJson(json)
        }
}
