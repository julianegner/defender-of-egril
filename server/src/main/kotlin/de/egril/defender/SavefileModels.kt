package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for uploading a savefile.
 *
 * @param saveId Unique identifier of the save (e.g. "savegame_1234567890")
 * @param data   Raw JSON content of the savefile
 */
@Serializable
data class SavefileUploadRequest(
    val saveId: String,
    val data: String
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
