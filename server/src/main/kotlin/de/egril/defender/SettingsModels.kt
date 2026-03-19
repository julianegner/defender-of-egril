package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for uploading player settings.
 *
 * @param data Raw JSON content of the settings map
 */
@Serializable
data class SettingsUploadRequest(
    val data: String
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
