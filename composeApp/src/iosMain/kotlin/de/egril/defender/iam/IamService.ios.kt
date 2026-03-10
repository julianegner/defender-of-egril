package de.egril.defender.iam

actual fun getIamBaseUrl(): String = "http://localhost:8081"

internal actual fun startPlatformLogin() {
    // On iOS, open the Keycloak auth page in Safari.
    // Full ASWebAuthenticationSession integration can be added in a future iteration.
    try {
        val authUrl = buildString {
            append(IamConfig.authUrl)
            append("?client_id=").append(IamConfig.CLIENT_ID)
            append("&response_type=code")
            append("&scope=openid")
        }
        val nsUrl = platform.Foundation.NSURL.URLWithString(authUrl) ?: return
        platform.UIKit.UIApplication.sharedApplication.openURL(
            nsUrl,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null
        )
    } catch (_: Exception) {
        // Ignore – login errors must never disrupt gameplay
    }
}

internal actual fun performPlatformLogout() {
    // Local state is cleared by IamService.logout(); no server-side action for now
}

actual suspend fun initPlatformIam() {
    // iOS login is triggered manually; nothing to restore on startup
}
