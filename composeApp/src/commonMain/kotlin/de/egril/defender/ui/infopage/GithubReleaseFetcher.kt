package de.egril.defender.ui.infopage

/**
 * Represents a single downloadable asset from a GitHub release.
 */
data class GithubReleaseAsset(
    val name: String,
    val downloadUrl: String
)

/**
 * Fetches the list of assets from the latest GitHub release.
 * Returns null when the API is unreachable or the response cannot be parsed.
 * Only the wasmJs platform provides a real implementation; all other platforms return null.
 */
expect suspend fun fetchLatestReleaseAssets(): List<GithubReleaseAsset>?
