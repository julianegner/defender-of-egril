package de.egril.defender.ui.infopage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun fetchLatestReleaseAssets(): List<GithubReleaseAsset>? = null

actual suspend fun fetchGithubReleases(): List<GithubRelease>? {
    if (isInstalledFromPlayStore()) return null
    return withContext(Dispatchers.IO) { jvmFetchGithubReleases() }
}
