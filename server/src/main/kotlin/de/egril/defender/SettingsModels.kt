package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for uploading player settings.
 *
 * @param data Raw JSON content of the settings map
 * @param platform The frontend platform (e.g. WEB, DESKTOP, ANDROID, IOS), optional
 * @param versionName The version name of the frontend (e.g. "1.0"), optional
 * @param commitHash The short git commit hash of the frontend build, optional
 */
@Serializable
data class SettingsUploadRequest(
    val data: String,
    val platform: String? = null,
    val versionName: String? = null,
    val commitHash: String? = null
)

/**
 * Response returned when fetching player settings.
 *
 * @param data      Raw JSON content of the settings map
 * @param updatedAt ISO-8601 timestamp of the last update (UTC)
 */
@Serializable
data class SettingsResponse(
    val data: String,
    val updatedAt: String
)
