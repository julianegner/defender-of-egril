package de.egril.defender.save

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Metadata about a savefile stored on the backend.
 *
 * @param saveId    Unique identifier matching the local save ID
 * @param data      Raw JSON content of the savefile
 * @param updatedAt ISO-8601 timestamp of the last update on the server (UTC)
 */
@Serializable
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

private val json = Json { ignoreUnknownKeys = true }

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
 * Parses a JSON array of [RemoteSavefileInfo] objects returned by GET /api/savefiles.
 */
internal fun parseRemoteSavefilesJson(responseJson: String): List<RemoteSavefileInfo> {
    return try {
        json.decodeFromString<List<RemoteSavefileInfo>>(responseJson)
    } catch (_: Exception) {
        emptyList()
    }
}
