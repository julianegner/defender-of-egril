package de.egril.defender.editor

import de.egril.defender.AndroidContextProvider

private const val REPOSITORY_ASSET_PREFIX =
    "composeResources/defender_of_egril.composeapp.generated.resources/files/repository/"

actual fun readPlatformRepositoryBytes(path: String): ByteArray? {
    return try {
        AndroidContextProvider.getContext().assets.open("$REPOSITORY_ASSET_PREFIX$path").use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}
