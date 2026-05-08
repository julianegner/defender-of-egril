package de.egril.defender.editor

import de.egril.defender.AndroidContextProvider

// Must match the Compose Multiplatform asset-pack path configured for this module's generated
// resources package in the Android AAB asset-pack build setup.
private const val REPOSITORY_ASSET_PREFIX =
    "composeResources/defender_of_egril.composeapp.generated.resources/files/repository/"

actual fun readPlatformRepositoryBytes(path: String): ByteArray? {
    return try {
        AndroidContextProvider.getContext().assets.open("$REPOSITORY_ASSET_PREFIX$path").use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}
