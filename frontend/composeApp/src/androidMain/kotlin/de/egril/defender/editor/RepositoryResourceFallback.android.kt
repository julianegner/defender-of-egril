package de.egril.defender.editor

import de.egril.defender.AndroidContextProvider
import de.egril.defender.config.LogConfig

// Must match the Compose Multiplatform asset-pack path configured for this module's generated
// resources package in frontend/assetPack/build.gradle.kts and the compose-app CMP package wiring
// in frontend/composeApp/build.gradle.kts.
private const val REPOSITORY_ASSET_PREFIX =
    "composeResources/defender_of_egril.composeapp.generated.resources/files/repository/"

private fun buildAssetPath(path: String): String = "$REPOSITORY_ASSET_PREFIX$path"

// JVM unit tests have no Android context, so they read the repository files from the source tree.
private const val SOURCE_REPOSITORY_DIR = "src/commonMain/composeResources/files/repository/"

actual fun readPlatformRepositoryBytes(path: String): ByteArray? =
    try {
        AndroidContextProvider
            .getContext()
            .assets
            .open(buildAssetPath(path))
            .use { it.readBytes() }
    } catch (e: Exception) {
        readSourceRepositoryBytes(path) ?: run {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Android AssetManager fallback could not load repository file $path: ${e.message}")
            }
            null
        }
    }

private fun readSourceRepositoryBytes(path: String): ByteArray? {
    val projectDir = System.getProperty("composeApp.projectDir") ?: return null
    val file = java.io.File(projectDir, SOURCE_REPOSITORY_DIR + path)
    return if (file.isFile) file.readBytes() else null
}
