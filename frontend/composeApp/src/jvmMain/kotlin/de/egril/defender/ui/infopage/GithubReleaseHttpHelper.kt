package de.egril.defender.ui.infopage

import java.net.HttpURLConnection
import java.net.URI

/**
 * Fetches the list of recent GitHub releases via the GitHub REST API.
 * Shared by Desktop and Android platforms (via the jvmMain source set).
 * Returns null on network errors or unexpected response codes.
 */
internal fun jvmFetchGithubReleases(): List<GithubRelease>? {
    return try {
        val url = "$GITHUB_RELEASES_API_URL?per_page=10"
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.disconnect()
            return null
        }
        val json = connection.inputStream.bufferedReader().readText()
        connection.disconnect()
        parseGithubReleasesJson(json)
    } catch (_: Exception) {
        null
    }
}
