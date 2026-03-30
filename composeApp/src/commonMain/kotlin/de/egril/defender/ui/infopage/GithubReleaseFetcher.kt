package de.egril.defender.ui.infopage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val GITHUB_RELEASES_API_URL =
    "https://api.github.com/repos/julianegner/defender-of-egril/releases"

/**
 * Represents a single downloadable asset from a GitHub release.
 */
data class GithubReleaseAsset(
    val name: String,
    val downloadUrl: String
)

/**
 * Represents a single GitHub release with its tag name and downloadable assets.
 */
data class GithubRelease(
    val tagName: String,
    val assets: List<GithubReleaseAsset>
)

/**
 * Fetches the list of assets from the latest GitHub release.
 * Returns null when the API is unreachable or the response cannot be parsed.
 * Only the wasmJs platform provides a real implementation; all other platforms return null.
 */
expect suspend fun fetchLatestReleaseAssets(): List<GithubReleaseAsset>?

/**
 * Fetches the list of recent GitHub releases (newest first) for version checking.
 * Returns null when the API is unreachable or the response cannot be parsed.
 * Implemented on Desktop and Android; returns null on iOS and WASM.
 */
expect suspend fun fetchGithubReleases(): List<GithubRelease>?

/**
 * Parses a GitHub releases JSON array string into a list of [GithubRelease].
 * Shared by all JVM-based platform implementations.
 */
internal fun parseGithubReleasesJson(json: String): List<GithubRelease>? {
    return try {
        Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val assets = obj["assets"]?.jsonArray?.mapNotNull { assetElement ->
                val assetObj = assetElement.jsonObject
                val name = assetObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val url = assetObj["browser_download_url"]?.jsonPrimitive?.content
                    ?: return@mapNotNull null
                GithubReleaseAsset(name, url)
            } ?: emptyList()
            GithubRelease(tagName, assets)
        }
    } catch (_: Exception) {
        null
    }
}
