package de.egril.defender

import kotlinx.serialization.Serializable

@Serializable
data class CommunityFileUploadRequest(
    val fileType: String,  // "MAP" or "LEVEL"
    val fileId: String,
    val data: String,
    val description: String = ""  // Optional short description (used for LEVEL files)
)

@Serializable
data class CommunityFileMetadata(
    val fileType: String,
    val fileId: String,
    val authorUsername: String,
    val authorId: String,
    val updatedAt: String,
    val uploadedAt: String,
    val description: String = ""  // Optional description (only for LEVEL files)
)

@Serializable
data class CommunityFileData(
    val fileType: String,
    val fileId: String,
    val authorUsername: String,
    val authorId: String,
    val data: String,
    val updatedAt: String,
    val uploadedAt: String,
    val description: String = ""  // Optional description (only for LEVEL files)
)
