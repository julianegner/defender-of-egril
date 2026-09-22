package de.egril.defender.ui.infopage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionCheckerTest {
    // ---------------------------------------------------------------------------
    // compareVersions
    // ---------------------------------------------------------------------------

    @Test
    fun `compareVersions returns positive when first version is greater`() {
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.1.0", "1.0.9") > 0)
        assertTrue(compareVersions("1.0.1", "1.0.0") > 0)
    }

    @Test
    fun `compareVersions returns negative when first version is smaller`() {
        assertTrue(compareVersions("1.0.0", "2.0.0") < 0)
        assertTrue(compareVersions("1.0.9", "1.1.0") < 0)
        assertTrue(compareVersions("1.0.0", "1.0.1") < 0)
    }

    @Test
    fun `compareVersions returns zero for equal versions`() {
        assertEquals(0, compareVersions("1.0.0", "1.0.0"))
        assertEquals(0, compareVersions("2.3.4", "2.3.4"))
    }

    @Test
    fun `compareVersions handles single-component versions`() {
        assertTrue(compareVersions("2", "1") > 0)
        assertEquals(0, compareVersions("1", "1"))
    }

    @Test
    fun `compareVersions handles missing patch component`() {
        assertTrue(compareVersions("1.1", "1.0.9") > 0)
        assertEquals(0, compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun `compareVersions treats stable release as newer than beta of same base version`() {
        assertTrue(compareVersions("1.0.0", "1.0.0-beta") > 0)
        assertTrue(compareVersions("1.0.0-beta", "1.0.0") < 0)
        assertEquals(0, compareVersions("1.0.0-beta", "1.0.0-beta"))
    }

    @Test
    fun `isBetaVersion recognizes beta suffix variants`() {
        assertTrue(isBetaVersion("1.0.0-beta"))
        assertTrue(isBetaVersion("1.0.0-beta.1"))
        assertTrue(isBetaVersion("1.0.0-BETA-hotfix"))
        assertFalse(isBetaVersion("1.0.0"))
    }

    // ---------------------------------------------------------------------------
    // parseGithubReleasesJson
    // ---------------------------------------------------------------------------

    @Test
    fun `parseGithubReleasesJson parses single release with assets`() {
        val json =
            """
            [
              {
                "tag_name": "v1.2.3",
                "prerelease": true,
                "assets": [
                  { "name": "defender_1.2.3.deb", "browser_download_url": "https://example.com/file.deb" }
                ]
              }
            ]
            """.trimIndent()

        val releases = parseGithubReleasesJson(json)
        assertNotNull(releases)
        assertEquals(1, releases.size)
        assertEquals("v1.2.3", releases[0].tagName)
        assertTrue(releases[0].prerelease)
        assertEquals(1, releases[0].assets.size)
        assertEquals("defender_1.2.3.deb", releases[0].assets[0].name)
        assertEquals("https://example.com/file.deb", releases[0].assets[0].downloadUrl)
    }

    @Test
    fun `parseGithubReleasesJson parses multiple releases`() {
        val json =
            """
            [
              { "tag_name": "v2.0.0", "assets": [] },
              { "tag_name": "v1.9.0", "assets": [] }
            ]
            """.trimIndent()

        val releases = parseGithubReleasesJson(json)
        assertNotNull(releases)
        assertEquals(2, releases.size)
        assertEquals("v2.0.0", releases[0].tagName)
        assertEquals("v1.9.0", releases[1].tagName)
    }

    @Test
    fun `parseGithubReleasesJson returns null for invalid json`() {
        assertNull(parseGithubReleasesJson("not valid json"))
        assertNull(parseGithubReleasesJson(""))
    }

    @Test
    fun `parseGithubReleasesJson skips releases missing tag_name`() {
        val json =
            """
            [
              { "assets": [] },
              { "tag_name": "v1.0.0", "assets": [] }
            ]
            """.trimIndent()

        val releases = parseGithubReleasesJson(json)
        assertNotNull(releases)
        assertEquals(1, releases.size)
        assertEquals("v1.0.0", releases[0].tagName)
    }

    @Test
    fun `parseGithubReleasesJson skips assets missing required fields`() {
        val json =
            """
            [
              {
                "tag_name": "v1.0.0",
                "assets": [
                  { "name": "file.deb" },
                  { "browser_download_url": "https://example.com/file.deb" },
                  { "name": "good.deb", "browser_download_url": "https://example.com/good.deb" }
                ]
              }
            ]
            """.trimIndent()

        val releases = parseGithubReleasesJson(json)
        assertNotNull(releases)
        assertEquals(1, releases.size)
        assertEquals(1, releases[0].assets.size)
        assertEquals("good.deb", releases[0].assets[0].name)
    }

    @Test
    fun `findNewerVersions ignores beta releases for production builds`() {
        val releases =
            listOf(
                GithubRelease(
                    tagName = "v1.0.9-beta",
                    prerelease = true,
                    assets = listOf(GithubReleaseAsset("defender_1.0.9-beta.deb", "https://example.com/beta.deb")),
                ),
                GithubRelease(
                    tagName = "v1.0.8",
                    assets = listOf(GithubReleaseAsset("defender_1.0.8.deb", "https://example.com/stable.deb")),
                ),
            )

        val newerVersions = findNewerVersions("1.0.7", releases, listOf(".deb"))

        assertEquals(
            listOf(
                NewVersionInfo(
                    version = "1.0.8",
                    releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/v1.0.8",
                    isBetaRelease = false,
                ),
            ),
            newerVersions,
        )
    }

    @Test
    fun `findNewerVersions returns beta and stable updates for beta builds`() {
        val releases =
            listOf(
                GithubRelease(
                    tagName = "v1.0.9-beta",
                    prerelease = true,
                    assets = listOf(GithubReleaseAsset("defender_1.0.9-beta.deb", "https://example.com/beta.deb")),
                ),
                GithubRelease(
                    tagName = "v1.0.8",
                    assets = listOf(GithubReleaseAsset("defender_1.0.8.deb", "https://example.com/stable.deb")),
                ),
                GithubRelease(
                    tagName = "v1.0.7",
                    assets = listOf(GithubReleaseAsset("defender_1.0.7.deb", "https://example.com/older.deb")),
                ),
            )

        val newerVersions = findNewerVersions("1.0.7-beta", releases, listOf(".deb"))

        assertEquals(
            listOf(
                NewVersionInfo(
                    version = "1.0.9-beta",
                    releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/v1.0.9-beta",
                    isBetaRelease = true,
                ),
                NewVersionInfo(
                    version = "1.0.8",
                    releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/v1.0.8",
                    isBetaRelease = false,
                ),
            ),
            newerVersions,
        )
    }

    @Test
    fun `findNewerVersions picks the newest stable and beta releases even when input order varies`() {
        val releases =
            listOf(
                GithubRelease(
                    tagName = "v1.0.8",
                    assets = listOf(GithubReleaseAsset("defender_1.0.8.deb", "https://example.com/stable-old.deb")),
                ),
                GithubRelease(
                    tagName = "v1.0.9-beta",
                    prerelease = true,
                    assets = listOf(GithubReleaseAsset("defender_1.0.9-beta.deb", "https://example.com/beta-new.deb")),
                ),
                GithubRelease(
                    tagName = "v1.0.10",
                    assets = listOf(GithubReleaseAsset("defender_1.0.10.deb", "https://example.com/stable-new.deb")),
                ),
                GithubRelease(
                    tagName = "v1.0.8-beta",
                    prerelease = true,
                    assets = listOf(GithubReleaseAsset("defender_1.0.8-beta.deb", "https://example.com/beta-old.deb")),
                ),
            )

        val newerVersions = findNewerVersions("1.0.7-beta", releases, listOf(".deb"))

        assertEquals(
            listOf(
                NewVersionInfo(
                    version = "1.0.9-beta",
                    releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/v1.0.9-beta",
                    isBetaRelease = true,
                ),
                NewVersionInfo(
                    version = "1.0.10",
                    releasePageUrl = "https://github.com/julianegner/defender-of-egril/releases/tag/v1.0.10",
                    isBetaRelease = false,
                ),
            ),
            newerVersions,
        )
    }
}
