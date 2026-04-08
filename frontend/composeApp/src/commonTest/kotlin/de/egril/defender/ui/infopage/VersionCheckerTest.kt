package de.egril.defender.ui.infopage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
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

    // ---------------------------------------------------------------------------
    // parseGithubReleasesJson
    // ---------------------------------------------------------------------------

    @Test
    fun `parseGithubReleasesJson parses single release with assets`() {
        val json = """
            [
              {
                "tag_name": "v1.2.3",
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
        assertEquals(1, releases[0].assets.size)
        assertEquals("defender_1.2.3.deb", releases[0].assets[0].name)
        assertEquals("https://example.com/file.deb", releases[0].assets[0].downloadUrl)
    }

    @Test
    fun `parseGithubReleasesJson parses multiple releases`() {
        val json = """
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
        val json = """
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
        val json = """
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
}
