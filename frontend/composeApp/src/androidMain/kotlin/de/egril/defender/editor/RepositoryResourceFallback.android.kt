package de.egril.defender.editor

import de.egril.defender.AndroidContextProvider
import de.egril.defender.config.LogConfig

// Must match the Compose Multiplatform asset-pack path configured for this module's generated
// resources package in frontend/assetPack/build.gradle.kts and the compose-app CMP package wiring
// in frontend/composeApp/build.gradle.kts.
private const val REPOSITORY_ASSET_PREFIX =
    "composeResources/defender_of_egril.composeapp.generated.resources/files/repository/"

private fun buildAssetPath(path: String): String = "$REPOSITORY_ASSET_PREFIX$path"

actual fun readPlatformRepositoryBytes(path: String): ByteArray? {
    return try {
        AndroidContextProvider.getContext().assets.open(buildAssetPath(path)).use { it.readBytes() }
    } catch (e: Exception) {
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Android AssetManager fallback could not load repository file $path: ${e.message}")
        }
        null
    }
}
