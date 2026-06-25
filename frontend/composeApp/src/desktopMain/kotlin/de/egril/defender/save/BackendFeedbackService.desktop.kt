package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object BackendFeedbackService {
    actual suspend fun submitFeedback(
        request: FeedbackSubmitRequest,
        token: String?,
    ): Int? =
        withContext(Dispatchers.IO) {
            val status = jvmHttpPostOptionalAuth("/api/feedback", buildFeedbackUploadJson(request), token)
            if (status in 200..299) null else status
        }
}
