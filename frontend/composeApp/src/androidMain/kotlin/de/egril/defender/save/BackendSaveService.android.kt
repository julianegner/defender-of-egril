package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object BackendSaveService {

    actual suspend fun uploadSavefile(saveId: String, jsonData: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val status = jvmHttpPost("/api/savefiles", buildUploadJson(saveId, jsonData), token)
            status in 200..299
        }

    actual suspend fun fetchSavefiles(token: String): List<RemoteSavefileInfo>? =
        withContext(Dispatchers.IO) {
            val json = jvmHttpGet("/api/savefiles", token) ?: return@withContext null
            parseRemoteSavefilesJson(json)
        }
}
