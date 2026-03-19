package de.egril.defender.save

/**
 * Service for uploading and downloading player settings to/from the backend.
 *
 * Settings are stored in a dedicated `player_settings` table, completely separate
 * from save files ([BackendSaveService]) and general user data ([BackendUserDataService]).
 * This allows settings to be read and written independently of other player data.
 */
expect object BackendSettingsService {

    /**
     * Upload the player's settings to the backend.
     * Returns true on success, false on failure (e.g. not authenticated, network error).
     *
     * @param settingsJson Raw JSON content representing the settings map
     * @param token        Bearer token for authentication
     */
    suspend fun uploadSettings(settingsJson: String, token: String): Boolean

    /**
     * Fetch the player's settings from the backend.
     * Returns null on failure (not authenticated, network error, no settings stored yet).
     *
     * @param token Bearer token for authentication
     */
    suspend fun fetchSettings(token: String): Map<String, String>?
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

/** Builds the JSON payload for a settings upload request. */
internal fun buildSettingsUploadJson(settingsJson: String): String =
    """{"data":"${escapeJsonString(settingsJson)}"}"""

/**
 * Parses a settings map from the JSON response returned by GET /api/settings.
 * The response wraps the settings JSON in a `data` field alongside an `updatedAt` timestamp.
 *
 * Expected outer shape:
 * ```json
 * { "data": "<escaped inner JSON>", "updatedAt": "..." }
 * ```
 */
internal fun parseSettingsResponseJson(responseJson: String): Map<String, String>? {
    val outerTrimmed = responseJson.trim()
    if (!outerTrimmed.startsWith("{")) return null

    val innerJson = extractJsonStringFieldInternal(outerTrimmed, "data") ?: return null
    return parseSettingsJson(innerJson)
}

/**
 * Serialises a settings map to a flat JSON object string (the inner payload, without envelope).
 */
internal fun serializeSettingsJson(settings: Map<String, String>): String {
    val entries = settings.entries.joinToString(",\n  ") { (k, v) ->
        "\"${escapeJsonString(k)}\": \"${escapeJsonString(v)}\""
    }
    return "{\n  $entries\n}"
}

/**
 * Parses a `Map<String, String>` from a flat JSON object string.
 * Reuses the same parsing logic as level-progress maps.
 */
internal fun parseSettingsJson(json: String): Map<String, String>? {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{")) return null
    return try {
        val map = mutableMapOf<String, String>()
        val inner = if (trimmed.endsWith("}")) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed.substring(1)
        }
        var pos = 0
        while (pos < inner.length) {
            val keyStart = inner.indexOf('"', pos)
            if (keyStart < 0) break
            val keyEnd = inner.indexOf('"', keyStart + 1)
            if (keyEnd < 0) break
            val key = inner.substring(keyStart + 1, keyEnd)
            val colonPos = inner.indexOf(':', keyEnd + 1)
            if (colonPos < 0) break
            var valStart = colonPos + 1
            while (valStart < inner.length && inner[valStart] == ' ') valStart++
            if (valStart >= inner.length || inner[valStart] != '"') break
            val valEnd = inner.indexOf('"', valStart + 1)
            if (valEnd < 0) break
            val value = inner.substring(valStart + 1, valEnd)
            if (key.isNotEmpty()) map[key] = value
            pos = valEnd + 1
        }
        map
    } catch (_: Exception) {
        null
    }
}
