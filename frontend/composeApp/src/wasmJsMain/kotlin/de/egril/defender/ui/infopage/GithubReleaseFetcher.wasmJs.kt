@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.ui.infopage

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume

private const val GITHUB_API_URL =
    "https://api.github.com/repos/julianegner/defender-of-egril/releases/latest"

/**
 * Uses the browser's JSON parser via JS interop to extract name and browser_download_url
 * from each entry in the GitHub release assets array.
 * Returns a string where entries are separated by \u0001 (SOH) and the name/url within
 * each entry are separated by \u0000 (NUL). These control characters are used as delimiters
 * because they are guaranteed not to appear in asset filenames or HTTPS URLs.
 */
@JsFun("""
    (json) => {
        try {
            const data = JSON.parse(json);
            if (!data.assets) return "";
            return data.assets
                .filter(a => a.name && a.browser_download_url)
                .map(a => a.name + "\u0000" + a.browser_download_url)
                .join("\u0001");
        } catch(e) {
            return "";
        }
    }
""")
private external fun extractAssetsFromJson(json: String): String

actual suspend fun fetchLatestReleaseAssets(): List<GithubReleaseAsset>? =
    suspendCancellableCoroutine { continuation ->
        try {
            val xhr = XMLHttpRequest()
            xhr.open("GET", GITHUB_API_URL, async = true)
            xhr.setRequestHeader("Accept", "application/vnd.github.v3+json")
            xhr.onload = {
                if (xhr.status.toInt() in 200..299) {
                    val raw = extractAssetsFromJson(xhr.responseText)
                    val assets = if (raw.isEmpty()) {
                        emptyList()
                    } else {
                        raw.split("\u0001").mapNotNull { entry ->
                            val parts = entry.split("\u0000")
                            if (parts.size == 2) {
                                GithubReleaseAsset(name = parts[0], downloadUrl = parts[1])
                            } else {
                                null
                            }
                        }
                    }
                    continuation.resume(assets)
                } else {
                    continuation.resume(null)
                }
            }
            xhr.onerror = {
                continuation.resume(null)
            }
            xhr.send()
        } catch (_: Exception) {
            continuation.resume(null)
        }
    }

actual suspend fun fetchGithubReleases(): List<GithubRelease>? = null
