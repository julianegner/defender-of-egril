package de.egril.defender.save

import de.egril.defender.model.PlayerAbilities
import de.egril.defender.model.SpellType

/**
 * General user data that can be synchronised with the backend.
 *
 * This is kept separate from individual save game files. It contains:
 *  - [localUsername]   The player's local display name
 *  - [abilities]       XP, level, ability points and stat upgrades
 *  - [levelProgress]   Map of editor-level-ID → status (LOCKED/UNLOCKED/WON)
 *
 * @param localUsername  The local player name (as entered in the player creation dialog)
 * @param abilities      The player's ability/XP data (may be null when absent from the response)
 * @param levelProgress  Level unlock/win statuses (may be null when absent from the response)
 * @param updatedAt      Server-side ISO-8601 timestamp of the last update (set by the server)
 */
data class RemoteUserData(
    val localUsername: String,
    val abilities: PlayerAbilities?,
    val levelProgress: Map<String, String>?,
    val updatedAt: String = ""
)

/**
 * Service for uploading and downloading general user data (abilities, level progress,
 * local username) to/from the backend.
 *
 * This is distinct from [BackendSaveService], which handles individual save game files.
 * Implementations are platform-specific but the interface is shared across all platforms.
 */
expect object BackendUserDataService {

    /**
     * Upload the user's general data to the backend.
     * Returns true on success, false on failure (e.g. not authenticated, network error).
     *
     * @param jsonData Raw JSON content representing the user data
     * @param token    Bearer token for authentication
     */
    suspend fun uploadUserData(jsonData: String, token: String): Boolean

    /**
     * Fetch the user's general data from the backend.
     * Returns null on failure (not authenticated, network error, no data stored yet).
     *
     * @param token Bearer token for authentication
     */
    suspend fun fetchUserData(token: String): RemoteUserData?
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

/** Builds the JSON payload for a user-data upload request. */
internal fun buildUserDataUploadJson(jsonData: String): String = buildString {
    val platform = de.egril.defender.utils.getClientPlatformName()
    val platformLong = de.egril.defender.utils.getPlatform().name
    val versionName = de.egril.defender.AppBuildInfo.VERSION_NAME
    val commitHash = de.egril.defender.AppBuildInfo.COMMIT_HASH
    append("{")
    append("\"data\":\"${escapeJsonString(jsonData)}\",")
    appendClientInfo(platform, platformLong, versionName, commitHash)
    append("}")
}

/**
 * Parses a [RemoteUserData] from the JSON response returned by GET /api/userdata.
 * The response wraps the actual user-data JSON in a `data` field alongside an
 * `updatedAt` timestamp.
 *
 * Expected outer shape:
 * ```json
 * { "data": "<escaped inner JSON>", "updatedAt": "..." }
 * ```
 */
internal fun parseRemoteUserDataJson(responseJson: String): RemoteUserData? {
    val outerTrimmed = responseJson.trim()
    if (!outerTrimmed.startsWith("{")) return null

    // Extract the "data" field (escaped inner JSON) and "updatedAt"
    val innerJsonEscaped = extractJsonStringFieldInternal(outerTrimmed, "data") ?: return null
    val updatedAt = extractJsonStringFieldInternal(outerTrimmed, "updatedAt") ?: ""

    // The inner JSON is the actual user-data payload
    return parseUserDataJson(innerJsonEscaped, updatedAt)
}

/**
 * Parses a [RemoteUserData] from the raw user-data JSON payload (the inner content,
 * not the wrapper envelope).
 */
internal fun parseUserDataJson(json: String, updatedAt: String = ""): RemoteUserData? {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{")) return null

    val localUsername = extractJsonStringFieldInternal(trimmed, "localUsername") ?: return null

    // Parse abilities (optional – may not be present in older saves)
    val abilities = try {
        val abilitiesStart = trimmed.indexOf("\"abilities\": {")
            .takeIf { it >= 0 } ?: trimmed.indexOf("\"abilities\":{").takeIf { it >= 0 }
        if (abilitiesStart != null) {
            val jsonAfterKey = trimmed.substring(abilitiesStart)
            val braceStart = jsonAfterKey.indexOf('{')
            if (braceStart >= 0) {
                val sub = jsonAfterKey.substring(braceStart)
                val abilitiesJson = extractBalancedBraces(sub)
                if (abilitiesJson != null) parseAbilitiesJson(abilitiesJson) else null
            } else null
        } else null
    } catch (_: Exception) {
        null
    }

    // Parse levelProgress map (optional)
    val levelProgress = try {
        val lpStart = trimmed.indexOf("\"levelProgress\": {")
            .takeIf { it >= 0 } ?: trimmed.indexOf("\"levelProgress\":{").takeIf { it >= 0 }
        if (lpStart != null) {
            val jsonAfterKey = trimmed.substring(lpStart)
            val braceStart = jsonAfterKey.indexOf('{')
            if (braceStart >= 0) {
                val sub = jsonAfterKey.substring(braceStart)
                val lpJson = extractBalancedBraces(sub)
                if (lpJson != null) parseLevelProgressMap(lpJson) else null
            } else null
        } else null
    } catch (_: Exception) {
        null
    }

    return RemoteUserData(
        localUsername = localUsername,
        abilities = abilities,
        levelProgress = levelProgress,
        updatedAt = updatedAt
    )
}

/** Serialises a [RemoteUserData] to the inner JSON payload (without the upload envelope). */
internal fun serializeUserDataJson(
    localUsername: String,
    abilities: PlayerAbilities,
    levelProgress: Map<String, String>
): String {
    val abilitiesJson = serializeAbilitiesJson(abilities)
    val levelProgressJson = levelProgress.entries.joinToString(",\n    ") { (id, status) ->
        "\"${escapeJsonString(id)}\": \"${escapeJsonString(status)}\""
    }
    return """{
  "localUsername": "${escapeJsonString(localUsername)}",
  "abilities": $abilitiesJson,
  "levelProgress": {
    $levelProgressJson
  }
}"""
}

private fun serializeAbilitiesJson(abilities: PlayerAbilities): String {
    val unlockedSpellsJson = abilities.unlockedSpells.joinToString(", ") { "\"${escapeJsonString(it.name)}\"" }
    return """{
    "totalXP": ${abilities.totalXP},
    "level": ${abilities.level},
    "availableAbilityPoints": ${abilities.availableAbilityPoints},
    "healthAbility": ${abilities.healthAbility},
    "treasuryAbility": ${abilities.treasuryAbility},
    "incomeAbility": ${abilities.incomeAbility},
    "constructionAbility": ${abilities.constructionAbility},
    "manaAbility": ${abilities.manaAbility},
    "unlockedSpells": [$unlockedSpellsJson]
  }"""
}

private fun parseAbilitiesJson(json: String): PlayerAbilities? {
    return try {
        val totalXP = extractJsonIntField(json, "totalXP") ?: return null
        val level = extractJsonIntField(json, "level") ?: PlayerAbilities.calculateLevel(totalXP)
        val availableAbilityPoints = extractJsonIntField(json, "availableAbilityPoints") ?: 0
        val healthAbility = extractJsonIntField(json, "healthAbility") ?: 0
        val treasuryAbility = extractJsonIntField(json, "treasuryAbility") ?: 0
        val incomeAbility = extractJsonIntField(json, "incomeAbility") ?: 0
        val constructionAbility = extractJsonIntField(json, "constructionAbility") ?: 0
        val manaAbility = extractJsonIntField(json, "manaAbility") ?: 0
        val unlockedSpells = parseSpellList(json)
        PlayerAbilities(
            totalXP = totalXP,
            level = level,
            availableAbilityPoints = availableAbilityPoints,
            healthAbility = healthAbility,
            treasuryAbility = treasuryAbility,
            incomeAbility = incomeAbility,
            constructionAbility = constructionAbility,
            manaAbility = manaAbility,
            unlockedSpells = unlockedSpells
        )
    } catch (_: Exception) {
        null
    }
}

private fun parseSpellList(json: String): Set<SpellType> {
    val arrayStart = json.indexOf("\"unlockedSpells\": [")
        .takeIf { it >= 0 } ?: json.indexOf("\"unlockedSpells\":[").takeIf { it >= 0 } ?: return emptySet()
    val bracketStart = json.indexOf('[', arrayStart)
    if (bracketStart < 0) return emptySet()
    val bracketEnd = json.indexOf(']', bracketStart)
    if (bracketEnd < 0) return emptySet()
    val arrayContent = json.substring(bracketStart + 1, bracketEnd).trim()
    if (arrayContent.isEmpty()) return emptySet()
    return arrayContent.split(",").mapNotNull { token ->
        val name = token.trim().removeSurrounding("\"")
        try { SpellType.valueOf(name) } catch (_: Exception) { null }
    }.toSet()
}

private fun parseLevelProgressMap(json: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val trimmed = json.trim()
    // Remove surrounding braces
    val inner = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        trimmed.substring(1, trimmed.length - 1)
    } else {
        trimmed
    }
    // Iterate over "key": "value" pairs
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
        if (key.isNotEmpty() && value.isNotEmpty()) {
            map[key] = value
        }
        pos = valEnd + 1
    }
    return map
}

private fun extractJsonIntField(json: String, key: String): Int? {
    val pattern = "\"$key\""
    val keyIdx = json.indexOf(pattern)
    if (keyIdx < 0) return null
    val colonIdx = json.indexOf(':', keyIdx + pattern.length)
    if (colonIdx < 0) return null
    var valStart = colonIdx + 1
    while (valStart < json.length && json[valStart] == ' ') valStart++
    var valEnd = valStart
    while (valEnd < json.length && (json[valEnd].isDigit() || json[valEnd] == '-')) valEnd++
    return json.substring(valStart, valEnd).toIntOrNull()
}

// Re-export the package-private helper used in BackendCommunityService so we avoid duplication
internal fun extractJsonStringFieldInternal(json: String, key: String): String? {
    val pattern = "\"${key}\""
    val keyIdx = json.indexOf(pattern)
    if (keyIdx < 0) return null
    val colonIdx = json.indexOf(':', keyIdx + pattern.length)
    if (colonIdx < 0) return null
    var valStart = colonIdx + 1
    while (valStart < json.length && json[valStart] == ' ') valStart++
    if (valStart >= json.length || json[valStart] != '"') return null
    val sb = StringBuilder()
    var i = valStart + 1
    while (i < json.length) {
        val c = json[i]
        if (c == '\\' && i + 1 < json.length) {
            when (json[i + 1]) {
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                'n' -> { sb.append('\n'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                else -> { sb.append(json[i + 1]); i += 2 }
            }
        } else if (c == '"') {
            return sb.toString()
        } else {
            sb.append(c)
            i++
        }
    }
    return null
}

private fun extractBalancedBraces(json: String): String? {
    var depth = 0
    var i = 0
    while (i < json.length) {
        when (json[i]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return json.substring(0, i + 1)
            }
        }
        i++
    }
    return null
}
