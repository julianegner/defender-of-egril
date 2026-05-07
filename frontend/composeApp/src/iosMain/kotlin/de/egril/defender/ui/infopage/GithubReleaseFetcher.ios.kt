package de.egril.defender.ui.infopage

actual suspend fun fetchLatestRelease(): GithubRelease? = null

actual suspend fun fetchGithubReleases(): List<GithubRelease>? = null
