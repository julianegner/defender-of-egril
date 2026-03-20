package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for uploading a savefile.
 *
 * @param saveId Unique identifier of the save (e.g. "savegame_1234567890")
 * @param data   Raw JSON content of the savefile
 * @param platform The short frontend platform identifier (e.g. WEB, DESKTOP, ANDROID, IOS), optional
 * @param platformLong The full platform name including user agent for web, optional
 * @param versionName The version name of the frontend (e.g. "1.0"), optional
 * @param commitHash The short git commit hash of the frontend build, optional
 */
@Serializable
data class SavefileUploadRequest(
    val saveId: String,
    val data: String,
    val platform: String? = null,
    val platformLong: String? = null,
    val versionName: String? = null,
    val commitHash: String? = null
)

/**
 * Metadata entry returned when listing savefiles.
 *
 * @param saveId    Unique identifier of the save
 * @param updatedAt ISO-8601 timestamp of the last update (UTC)
 */
@Serializable
data class SavefileMetadata(
    val saveId: String,
    val data: String,
    val updatedAt: String
)
