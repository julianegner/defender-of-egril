package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for uploading general user data (abilities, level progress, local username).
 *
 * @param data Raw JSON content of the user data
 * @param platform The short frontend platform identifier (e.g. WEB, DESKTOP, ANDROID, IOS), optional
 * @param platformLong The full platform name including user agent for web, optional
 * @param versionName The version name of the frontend (e.g. "1.0"), optional
 * @param commitHash The short git commit hash of the frontend build, optional
 */
@Serializable
data class UserDataUploadRequest(
    val data: String,
    val platform: String? = null,
    val platformLong: String? = null,
    val versionName: String? = null,
    val commitHash: String? = null
)

/**
 * Response returned when fetching user data.
 *
 * @param data      Raw JSON content of the user data
 * @param updatedAt ISO-8601 timestamp of the last update (UTC)
 */
@Serializable
data class UserDataResponse(
    val data: String,
    val updatedAt: String
)
