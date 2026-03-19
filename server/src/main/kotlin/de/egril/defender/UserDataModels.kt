package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for uploading general user data (abilities, level progress, local username).
 *
 * @param data Raw JSON content of the user data
 */
@Serializable
data class UserDataUploadRequest(
    val data: String
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
