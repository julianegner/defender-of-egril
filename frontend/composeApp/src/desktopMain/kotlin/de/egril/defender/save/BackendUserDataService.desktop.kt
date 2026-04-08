package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object BackendUserDataService {

    actual suspend fun uploadUserData(jsonData: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val status = jvmHttpPost("/api/userdata", buildUserDataUploadJson(jsonData), token)
            status in 200..299
        }

    actual suspend fun fetchUserData(token: String): RemoteUserData? =
        withContext(Dispatchers.IO) {
            val json = jvmHttpGet("/api/userdata", token) ?: return@withContext null
            parseRemoteUserDataJson(json)
        }
}
