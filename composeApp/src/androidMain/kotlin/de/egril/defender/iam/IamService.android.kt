package de.egril.defender.iam

actual fun getIamBaseUrl(): String = readIamBaseUrlFromJvmEnv()

internal actual fun startPlatformLogin() {
    // On Android, open the Keycloak auth page in the system browser.
    // Full AppAuth-style integration can be added in a future iteration.
    try {
        val authUrl = buildString {
            append(IamConfig.authUrl)
            append("?client_id=").append(IamConfig.CLIENT_ID)
            append("&response_type=code")
            append("&scope=openid")
        }
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(authUrl)
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        de.egril.defender.AndroidContextProvider.getContext().startActivity(intent)
    } catch (_: Exception) {
        // Ignore – login errors must never disrupt gameplay
    }
}

internal actual fun performPlatformLogout() {
    // Local state is cleared by IamService.logout(); no server-side action for now
}

actual suspend fun initPlatformIam() {
    // Android login is triggered manually; nothing to restore on startup
}
