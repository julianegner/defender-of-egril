package de.egril.defender

import kotlinx.serialization.Serializable

@Serializable
data class CommunityFileUploadRequest(
    val fileType: String,  // "MAP" or "LEVEL"
    val fileId: String,
    val data: String
)

@Serializable
data class CommunityFileMetadata(
    val fileType: String,
    val fileId: String,
    val authorUsername: String,
    val authorId: String,
    val updatedAt: String,
    val uploadedAt: String
)

@Serializable
data class CommunityFileData(
    val fileType: String,
    val fileId: String,
    val authorUsername: String,
    val authorId: String,
    val data: String,
    val updatedAt: String,
    val uploadedAt: String
)
