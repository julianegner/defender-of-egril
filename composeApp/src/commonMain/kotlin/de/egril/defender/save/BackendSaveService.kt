package de.egril.defender.save

/**
 * Metadata about a savefile stored on the backend.
 *
 * @param saveId    Unique identifier matching the local save ID
 * @param data      Raw JSON content of the savefile
 * @param updatedAt ISO-8601 timestamp of the last update on the server (UTC)
 */
data class RemoteSavefileInfo(
    val saveId: String,
    val data: String,
    val updatedAt: String
)

/**
 * Service for uploading and downloading savefiles to/from the backend.
 *
 * Implementations are platform-specific but the interface is shared across all platforms.
 */
expect object BackendSaveService {

    /**
     * Upload a savefile to the backend.
     * Returns true on success, false on failure (e.g. not authenticated, network error).
     *
     * @param saveId  Unique identifier for this save (e.g. "savegame_1234567890")
     * @param jsonData Raw JSON content of the savefile
     * @param token   Bearer token for authentication (from [de.egril.defender.iam.IamService.getToken])
     */
    suspend fun uploadSavefile(saveId: String, jsonData: String, token: String): Boolean

    /**
     * Fetch the list of savefiles for the authenticated user from the backend.
     * Returns null on failure (not authenticated, network error, etc.).
     *
     * @param token Bearer token for authentication
     */
    suspend fun fetchSavefiles(token: String): List<RemoteSavefileInfo>?
}

// ---------------------------------------------------------------------------
// Shared helpers available to all platform implementations
// ---------------------------------------------------------------------------

/** Escapes a string for safe embedding as a JSON string value. */
internal fun escapeJsonString(s: String): String = buildString {
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

/** Builds the JSON payload for a savefile upload request. */
internal fun buildUploadJson(saveId: String, jsonData: String): String =
    """{"saveId":"${escapeJsonString(saveId)}","data":"${escapeJsonString(jsonData)}"}"""

/**
 * Parses a JSON array of savefile metadata objects returned by GET /api/savefiles.
 *
 * Expected format (produced by kotlinx.serialization on the server):
 * ```json
 * [
 *   {"saveId":"savegame_123","data":"{...escaped...}","updatedAt":"2024-01-01T00:00:00Z"},
 *   ...
 * ]
 * ```
 */
internal fun parseRemoteSavefilesJson(json: String): List<RemoteSavefileInfo> {
    val result = mutableListOf<RemoteSavefileInfo>()
    // Split on object boundaries: find each { … } block inside the array
    val trimmed = json.trim()
    if (trimmed.isEmpty() || trimmed == "[]") return result

    // Walk through the JSON array and extract individual object strings
    var depth = 0
    var objStart = -1
    var i = 0
    var inString = false
    var escape = false

    while (i < trimmed.length) {
        val c = trimmed[i]
        when {
            escape -> escape = false
            c == '\\' && inString -> escape = true
            c == '"' -> inString = !inString
            !inString && c == '{' -> {
                if (depth == 0) objStart = i
                depth++
            }
            !inString && c == '}' -> {
                depth--
                if (depth == 0 && objStart >= 0) {
                    val objStr = trimmed.substring(objStart, i + 1)
                    parseRemoteSavefileObject(objStr)?.let { result.add(it) }
                    objStart = -1
                }
            }
        }
        i++
    }
    return result
}

/** Parses a single JSON object into a [RemoteSavefileInfo], or returns null on failure. */
private fun parseRemoteSavefileObject(obj: String): RemoteSavefileInfo? {
    return try {
        val saveId = extractJsonStringField(obj, "saveId") ?: return null
        val data = extractJsonStringField(obj, "data") ?: return null
        val updatedAt = extractJsonStringField(obj, "updatedAt") ?: ""
        RemoteSavefileInfo(saveId = saveId, data = data, updatedAt = updatedAt)
    } catch (_: Exception) {
        null
    }
}

/**
 * Extracts the string value for [key] from a JSON object string, correctly handling escape sequences.
 * Returns null if the key is not found.
 */
private fun extractJsonStringField(json: String, key: String): String? {
    val keyPattern = "\"$key\""
    val keyIndex = json.indexOf(keyPattern)
    if (keyIndex < 0) return null

    // Find the colon after the key
    val colonIndex = json.indexOf(':', keyIndex + keyPattern.length)
    if (colonIndex < 0) return null

    // Find the opening quote of the value
    val openQuoteIndex = json.indexOf('"', colonIndex + 1)
    if (openQuoteIndex < 0) return null

    // Read the string value, respecting escape sequences
    val sb = StringBuilder()
    var i = openQuoteIndex + 1
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
            break
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}
