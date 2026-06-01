package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object BackendCrashService {
    actual suspend fun submitCrashReport(request: CrashReportSubmitRequest, token: String?): Int? =
        withContext(Dispatchers.IO) {
            val status = jvmHttpPostOptionalAuth("/api/crash", buildCrashReportUploadJson(request), token)
            if (status in 200..299) null else status
        }
}
