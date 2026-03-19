package de.egril.defender.save

/**
 * Metadata for a community file (map or level) from the backend.
 */
data class CommunityFileInfo(
    val fileType: String,       // "MAP" or "LEVEL"
    val fileId: String,
    val authorUsername: String,
    val authorId: String,
    val updatedAt: String,
    val uploadedAt: String
)

/**
 * Full data for a community file including its JSON content.
 */
data class CommunityFileData(
    val fileType: String,
    val fileId: String,
    val authorUsername: String,
    val authorId: String,
    val data: String,
    val updatedAt: String,
    val uploadedAt: String
)

/**
 * Cross-platform service for community maps/levels backend operations.
 * All methods are suspend functions that run asynchronously.
 */
expect object BackendCommunityService {
    /**
     * Upload or update a community map/level.
     * @param fileType "MAP" or "LEVEL"
     * @param fileId The unique ID of the map or level
     * @param jsonData The full JSON data to upload
     * @param token Bearer token for authentication
     * @return true on success, false on failure
     */
    suspend fun uploadCommunityFile(fileType: String, fileId: String, jsonData: String, token: String): Boolean

    /**
     * Fetch the list of community files metadata (without data).
     * @param fileType Optional filter: "MAP", "LEVEL", or null for all
     * @return List of CommunityFileInfo or null on failure
     */
    suspend fun fetchCommunityFileList(fileType: String?): List<CommunityFileInfo>?

    /**
     * Download the full data for a specific community file.
     * @param fileType "MAP" or "LEVEL"
     * @param fileId The unique ID of the map or level
     * @return CommunityFileData or null if not found or on error
     */
    suspend fun fetchCommunityFile(fileType: String, fileId: String): CommunityFileData?
}

// ---------------------------------------------------------------------------
// Shared helper functions
// ---------------------------------------------------------------------------

fun buildCommunityUploadJson(fileType: String, fileId: String, data: String): String {
    val escapedData = escapeJsonString(data)
    val escapedFileType = escapeJsonString(fileType)
    val escapedFileId = escapeJsonString(fileId)
    return """{"fileType":"$escapedFileType","fileId":"$escapedFileId","data":"$escapedData"}"""
}

fun parseCommunityFileListJson(json: String): List<CommunityFileInfo> {
    val result = mutableListOf<CommunityFileInfo>()
    val trimmed = json.trim()
    if (!trimmed.startsWith("[")) return result

    // Simple JSON array parser for community file metadata
    var pos = 1 // skip '['
    while (pos < trimmed.length) {
        val objStart = trimmed.indexOf('{', pos)
        if (objStart < 0) break
        val objEnd = findMatchingBrace(trimmed, objStart)
        if (objEnd < 0) break

        val obj = trimmed.substring(objStart, objEnd + 1)
        val fileType = extractJsonStringField(obj, "fileType") ?: ""
        val fileId = extractJsonStringField(obj, "fileId") ?: ""
        val authorUsername = extractJsonStringField(obj, "authorUsername") ?: ""
        val authorId = extractJsonStringField(obj, "authorId") ?: ""
        val updatedAt = extractJsonStringField(obj, "updatedAt") ?: ""
        val uploadedAt = extractJsonStringField(obj, "uploadedAt") ?: ""

        if (fileType.isNotEmpty() && fileId.isNotEmpty()) {
            result.add(CommunityFileInfo(fileType, fileId, authorUsername, authorId, updatedAt, uploadedAt))
        }
        pos = objEnd + 1
    }
    return result
}

fun parseCommunityFileDataJson(json: String): CommunityFileData? {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{")) return null

    val fileType = extractJsonStringField(trimmed, "fileType") ?: return null
    val fileId = extractJsonStringField(trimmed, "fileId") ?: return null
    val authorUsername = extractJsonStringField(trimmed, "authorUsername") ?: ""
    val authorId = extractJsonStringField(trimmed, "authorId") ?: ""
    val updatedAt = extractJsonStringField(trimmed, "updatedAt") ?: ""
    val uploadedAt = extractJsonStringField(trimmed, "uploadedAt") ?: ""
    val data = extractJsonStringField(trimmed, "data") ?: return null

    return CommunityFileData(fileType, fileId, authorUsername, authorId, data, updatedAt, uploadedAt)
}

private fun extractJsonStringField(json: String, key: String): String? {
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

private fun findMatchingBrace(s: String, start: Int): Int {
    var depth = 0
    var i = start
    while (i < s.length) {
        when (s[i]) {
            '{' -> depth++
            '}' -> { depth--; if (depth == 0) return i }
        }
        i++
    }
    return -1
}
