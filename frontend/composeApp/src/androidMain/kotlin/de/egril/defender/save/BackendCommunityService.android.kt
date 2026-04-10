package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object BackendCommunityService {

    actual suspend fun uploadCommunityFile(
        fileType: String,
        fileId: String,
        jsonData: String,
        token: String,
        description: String
    ): Boolean = withContext(Dispatchers.IO) {
        val status = jvmHttpPost(
            "/api/community/files",
            buildCommunityUploadJson(fileType, fileId, jsonData, description),
            token
        )
        status in 200..299
    }

    actual suspend fun fetchCommunityFileList(fileType: String?): List<CommunityFileInfo>? =
        withContext(Dispatchers.IO) {
            val path = if (fileType != null) {
                "/api/community/files?fileType=$fileType"
            } else {
                "/api/community/files"
            }
            val json = jvmHttpGet(path, token = null) ?: return@withContext null
            parseCommunityFileListJson(json)
        }

    actual suspend fun fetchCommunityFile(fileType: String, fileId: String): CommunityFileData? =
        withContext(Dispatchers.IO) {
            val json = jvmHttpGet("/api/community/files/$fileType/$fileId", token = null)
                ?: return@withContext null
            parseCommunityFileDataJson(json)
        }

    actual suspend fun fetchCommunityMapImage(mapId: String): ByteArray? =
        withContext(Dispatchers.IO) {
            jvmHttpGetBytes("/api/community/files/MAP/$mapId/image", token = null)
        }
}
