package de.egril.defender.ui.infopage

import de.egril.defender.AppBuildInfo

/**
 * Holds information about a newer version available for download.
 *
 * @param version     The newer version string (e.g. "1.2.3").
 * @param releasePageUrl URL of the GitHub releases page for the user to navigate to.
 */
data class NewVersionInfo(
    val version: String,
    val releasePageUrl: String
)

/**
 * Returns the file-name suffixes that identify release assets matching the current platform.
 * Returns null when the platform should not perform a GitHub update check (e.g. WASM,
 * or Android when the app was installed via the Play Store).
 */
internal expect fun platformAssetExtensions(): List<String>?

/**
 * Checks the GitHub releases list for a version newer than the one currently running.
 *
 * Iterates releases from newest to oldest and stops as soon as a release with a version
 * that is equal to or older than [AppBuildInfo.VERSION_NAME] is encountered.  Returns the
 * first release that (a) is newer and (b) contains an asset for the current platform, or
 * null when no such release exists or the API is unavailable.
 */
suspend fun checkForNewerVersion(): NewVersionInfo? {
    val extensions = platformAssetExtensions() ?: return null
    val currentVersion = AppBuildInfo.VERSION_NAME
    val releases = fetchGithubReleases() ?: return null

    for (release in releases) {
        val releaseVersion = release.tagName.removePrefix("v")
        if (compareVersions(releaseVersion, currentVersion) <= 0) break

        val hasPlatformAsset = release.assets.any { asset ->
            extensions.any { ext -> asset.name.endsWith(ext, ignoreCase = true) }
        }
        if (hasPlatformAsset) {
            return NewVersionInfo(
                version = releaseVersion,
                releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/${release.tagName}"
            )
        }
    }
    return null
}

/**
 * Compares two semantic version strings (e.g. "1.2.3").
 * Returns a positive number when [v1] > [v2], negative when [v1] < [v2], or 0 when equal.
 */
internal fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0..2) {
        val diff = (parts1.getOrElse(i) { 0 }) - (parts2.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}
