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
    val releasePageUrl: String,
    val isBetaRelease: Boolean,
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
suspend fun checkForNewerVersion(): NewVersionInfo? = checkForNewerVersions().firstOrNull()

suspend fun checkForNewerVersions(): List<NewVersionInfo> {
    val extensions = platformAssetExtensions() ?: return emptyList()
    val currentVersion = AppBuildInfo.VERSION_NAME
    val releases = fetchGithubReleases() ?: return emptyList()
    return findNewerVersions(currentVersion, releases, extensions)
}

internal fun findNewerVersions(
    currentVersion: String,
    releases: List<GithubRelease>,
    extensions: List<String>,
): List<NewVersionInfo> {
    val currentIsBeta = isBetaVersion(currentVersion)
    var newerStable: NewVersionInfo? = null
    var newerBeta: NewVersionInfo? = null

    for (release in releases) {
        val releaseVersion = release.tagName.removePrefix("v")
        if (compareVersions(releaseVersion, currentVersion) <= 0) {
            continue
        }

        val hasPlatformAsset =
            release.assets.any { asset ->
                extensions.any { ext -> asset.name.endsWith(ext, ignoreCase = true) }
            }
        if (!hasPlatformAsset) {
            continue
        }

        val isBetaRelease = release.prerelease || isBetaVersion(releaseVersion)
        val info =
            NewVersionInfo(
                version = releaseVersion,
                releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/${release.tagName}",
                isBetaRelease = isBetaRelease,
            )

        if (currentIsBeta) {
            if (isBetaRelease) {
                newerBeta = selectNewerVersionInfo(newerBeta, info)
            } else {
                newerStable = selectNewerVersionInfo(newerStable, info)
            }
        } else if (!isBetaRelease) {
            newerStable = selectNewerVersionInfo(newerStable, info)
        }
    }

    return if (currentIsBeta) {
        listOfNotNull(newerBeta, newerStable)
    } else {
        listOfNotNull(newerStable)
    }
}

/**
 * Compares two semantic version strings (e.g. "1.2.3").
 * Returns a positive number when [v1] > [v2], negative when [v1] < [v2], or 0 when equal.
 */
internal fun compareVersions(
    v1: String,
    v2: String,
): Int {
    val parts1 = v1.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0..2) {
        val diff = (parts1.getOrElse(i) { 0 }) - (parts2.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return comparePrereleaseSuffixes(v1, v2)
}

private val betaVersionRegex = Regex("^(\\d+(?:\\.\\d+){0,2})-beta(?:[.-](.*))?$", RegexOption.IGNORE_CASE)

internal fun isBetaVersion(version: String): Boolean = betaVersionRegex.matches(version)

private fun selectNewerVersionInfo(
    current: NewVersionInfo?,
    candidate: NewVersionInfo,
): NewVersionInfo {
    if (current == null) {
        return candidate
    }

    return if (compareVersions(candidate.version, current.version) > 0) {
        candidate
    } else {
        current
    }
}

private fun comparePrereleaseSuffixes(
    v1: String,
    v2: String,
): Int {
    val suffix1 = prereleaseSuffix(v1)
    val suffix2 = prereleaseSuffix(v2)
    if (suffix1 == null && suffix2 == null) {
        return 0
    }
    if (suffix1 == null) {
        return 1
    }
    if (suffix2 == null) {
        return -1
    }

    val tokens1 = prereleaseTokens(suffix1)
    val tokens2 = prereleaseTokens(suffix2)
    val maxSize = maxOf(tokens1.size, tokens2.size)
    for (index in 0 until maxSize) {
        val token1 = tokens1.getOrNull(index) ?: return -1
        val token2 = tokens2.getOrNull(index) ?: return 1
        val numeric1 = token1.toIntOrNull()
        val numeric2 = token2.toIntOrNull()
        val diff =
            when {
                numeric1 != null && numeric2 != null -> numeric1 - numeric2
                numeric1 != null -> -1
                numeric2 != null -> 1
                else -> token1.compareTo(token2, ignoreCase = true)
            }
        if (diff != 0) {
            return diff
        }
    }
    return 0
}

private fun prereleaseSuffix(version: String): String? = version.substringAfter('-', "").ifEmpty { null }

private fun prereleaseTokens(suffix: String): List<String> {
    return if (suffix.isEmpty()) {
        emptyList()
    } else {
        suffix.split('.', '-').filter { it.isNotEmpty() }
    }
}
