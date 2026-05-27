package de.egril.defender.utils

import com.hyperether.resources.AppLocale
import de.egril.defender.ui.infopage.InfoTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deep link parsing, URL slug mapping, and language code handling.
 *
 * IMPORTANT: When adding new deep links or InfoTab entries, add corresponding tests here.
 */
class DeepLinkHandlerTest {

    // ---------------------------------------------------------------
    // parseDeepLink – data-privacy routes
    // ---------------------------------------------------------------

    @Test
    fun `parseDeepLink data-privacy with english`() {
        val result = parseDeepLink("/data-privacy/en")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.DEFAULT, result.language)
    }

    @Test
    fun `parseDeepLink data-privacy with german`() {
        val result = parseDeepLink("/data-privacy/de")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.DE, result.language)
    }

    @Test
    fun `parseDeepLink data-privacy with french`() {
        val result = parseDeepLink("/data-privacy/fr")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.FR, result.language)
    }

    @Test
    fun `parseDeepLink data-privacy with spanish`() {
        val result = parseDeepLink("/data-privacy/es")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.ES, result.language)
    }

    @Test
    fun `parseDeepLink data-privacy with italian`() {
        val result = parseDeepLink("/data-privacy/it")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.IT, result.language)
    }

    @Test
    fun `parseDeepLink data-privacy without language defaults to detected language`() {
        // On desktop, detectSupportedLanguage() returns "en" → AppLocale.DEFAULT
        val result = parseDeepLink("/data-privacy")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.DEFAULT, result.language)
    }

    @Test
    fun `parseDeepLink data-privacy is case insensitive`() {
        val result = parseDeepLink("/Data-Privacy/DE")
        assertIs<DeepLink.DataPrivacy>(result)
        assertEquals(AppLocale.DE, result.language)
    }

    // ---------------------------------------------------------------
    // parseDeepLink – tutorial route
    // ---------------------------------------------------------------

    @Test
    fun `parseDeepLink tutorial`() {
        val result = parseDeepLink("/tutorial")
        assertIs<DeepLink.Tutorial>(result)
    }

    @Test
    fun `parseDeepLink tutorial is case insensitive`() {
        val result = parseDeepLink("/Tutorial")
        assertIs<DeepLink.Tutorial>(result)
    }

    @Test
    fun `parseDeepLink tutorial with trailing slash`() {
        val result = parseDeepLink("/tutorial/")
        assertIs<DeepLink.Tutorial>(result)
    }

    @Test
    fun `parseDeepLink settings`() {
        val result = parseDeepLink("/settings")
        assertIs<DeepLink.Settings>(result)
    }

    @Test
    fun `parseDeepLink settings is case insensitive`() {
        val result = parseDeepLink("/Settings")
        assertIs<DeepLink.Settings>(result)
    }

    // ---------------------------------------------------------------
    // parseDeepLink – info page routes (all tabs)
    // ---------------------------------------------------------------

    @Test
    fun `parseDeepLink info without tab defaults to INSTALLATION`() {
        val result = parseDeepLink("/info")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.INSTALLATION, result.tab)
    }

    @Test
    fun `parseDeepLink info with trailing slash defaults to INSTALLATION`() {
        val result = parseDeepLink("/info/")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.INSTALLATION, result.tab)
    }

    @Test
    fun `parseDeepLink info installation`() {
        val result = parseDeepLink("/info/installation")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.INSTALLATION, result.tab)
    }

    @Test
    fun `parseDeepLink info how-to-play`() {
        val result = parseDeepLink("/info/how-to-play")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.HOW_TO_PLAY, result.tab)
    }

    @Test
    fun `parseDeepLink info audio-licenses`() {
        val result = parseDeepLink("/info/audio-licenses")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.AUDIO_LICENSES, result.tab)
    }

    @Test
    fun `parseDeepLink info license`() {
        val result = parseDeepLink("/info/license")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.LICENSE, result.tab)
    }

    @Test
    fun `parseDeepLink info keyboard-shortcuts`() {
        val result = parseDeepLink("/info/keyboard-shortcuts")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.KEYBOARD_SHORTCUTS, result.tab)
    }

    @Test
    fun `parseDeepLink info backend`() {
        val result = parseDeepLink("/info/backend")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.BACKEND, result.tab)
    }

    @Test
    fun `parseDeepLink info data-privacy alias maps to BACKEND`() {
        val result = parseDeepLink("/info/data-privacy")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.BACKEND, result.tab)
    }

    @Test
    fun `parseDeepLink info feedback`() {
        val result = parseDeepLink("/info/feedback")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.FEEDBACK, result.tab)
    }

    @Test
    fun `parseDeepLink info editor-howto`() {
        val result = parseDeepLink("/info/editor-howto")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.EDITOR_HOWTO, result.tab)
    }

    @Test
    fun `parseDeepLink info download`() {
        val result = parseDeepLink("/info/download")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.DOWNLOAD, result.tab)
    }

    @Test
    fun `parseDeepLink info tab is case insensitive`() {
        val result = parseDeepLink("/Info/Backend")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.BACKEND, result.tab)
    }

    @Test
    fun `parseDeepLink info unknown tab falls back to INSTALLATION`() {
        val result = parseDeepLink("/info/nonexistent-tab")
        assertIs<DeepLink.InfoPage>(result)
        assertEquals(InfoTab.INSTALLATION, result.tab)
    }

    // ---------------------------------------------------------------
    // parseDeepLink – unrecognized routes
    // ---------------------------------------------------------------

    @Test
    fun `parseDeepLink root path returns None`() {
        val result = parseDeepLink("/")
        assertIs<DeepLink.None>(result)
    }

    @Test
    fun `parseDeepLink empty path returns None`() {
        val result = parseDeepLink("")
        assertIs<DeepLink.None>(result)
    }

    @Test
    fun `parseDeepLink unknown route returns None`() {
        val result = parseDeepLink("/some/unknown/path")
        assertIs<DeepLink.None>(result)
    }

    @Test
    fun `parseDeepLink random text returns None`() {
        val result = parseDeepLink("/game-play")
        assertIs<DeepLink.None>(result)
    }

    // ---------------------------------------------------------------
    // toUrlSlug – every InfoTab maps to a slug
    // ---------------------------------------------------------------

    @Test
    fun `toUrlSlug INSTALLATION`() {
        assertEquals("installation", InfoTab.INSTALLATION.toUrlSlug())
    }

    @Test
    fun `toUrlSlug HOW_TO_PLAY`() {
        assertEquals("how-to-play", InfoTab.HOW_TO_PLAY.toUrlSlug())
    }

    @Test
    fun `toUrlSlug AUDIO_LICENSES`() {
        assertEquals("audio-licenses", InfoTab.AUDIO_LICENSES.toUrlSlug())
    }

    @Test
    fun `toUrlSlug LICENSE`() {
        assertEquals("license", InfoTab.LICENSE.toUrlSlug())
    }

    @Test
    fun `toUrlSlug KEYBOARD_SHORTCUTS`() {
        assertEquals("keyboard-shortcuts", InfoTab.KEYBOARD_SHORTCUTS.toUrlSlug())
    }

    @Test
    fun `toUrlSlug BACKEND`() {
        assertEquals("backend", InfoTab.BACKEND.toUrlSlug())
    }

    @Test
    fun `toUrlSlug FEEDBACK`() {
        assertEquals("feedback", InfoTab.FEEDBACK.toUrlSlug())
    }

    @Test
    fun `toUrlSlug EDITOR_HOWTO`() {
        assertEquals("editor-howto", InfoTab.EDITOR_HOWTO.toUrlSlug())
    }

    @Test
    fun `toUrlSlug DOWNLOAD`() {
        assertEquals("download", InfoTab.DOWNLOAD.toUrlSlug())
    }

    @Test
    fun `every InfoTab has a URL slug`() {
        // Ensures no InfoTab is accidentally left without a slug mapping.
        // If a new InfoTab value is added without a slug, this test forces
        // a compile error or a runtime crash, catching the omission early.
        for (tab in InfoTab.entries) {
            val slug = tab.toUrlSlug()
            assert(slug.isNotBlank()) { "InfoTab.$tab has a blank URL slug" }
        }
    }

    // ---------------------------------------------------------------
    // infoTabFromSlug – round-trip with toUrlSlug
    // ---------------------------------------------------------------

    @Test
    fun `infoTabFromSlug returns correct tab for every slug`() {
        // Verify that every InfoTab round-trips through toUrlSlug → infoTabFromSlug
        for (tab in InfoTab.entries) {
            val slug = tab.toUrlSlug()
            val parsed = infoTabFromSlug(slug)
            assertEquals(tab, parsed, "Round-trip failed for InfoTab.$tab (slug=$slug)")
        }
    }

    @Test
    fun `infoTabFromSlug data-privacy alias returns BACKEND`() {
        assertEquals(InfoTab.BACKEND, infoTabFromSlug("data-privacy"))
    }

    @Test
    fun `infoTabFromSlug is case insensitive`() {
        assertEquals(InfoTab.INSTALLATION, infoTabFromSlug("INSTALLATION"))
        assertEquals(InfoTab.BACKEND, infoTabFromSlug("Backend"))
        assertEquals(InfoTab.FEEDBACK, infoTabFromSlug("FEEDBACK"))
    }

    @Test
    fun `infoTabFromSlug returns null for unknown slug`() {
        assertNull(infoTabFromSlug("nonexistent"))
        assertNull(infoTabFromSlug(""))
        assertNull(infoTabFromSlug("some-random-slug"))
    }

    // ---------------------------------------------------------------
    // parseLanguageFromCode
    // ---------------------------------------------------------------

    @Test
    fun `parseLanguageFromCode en returns DEFAULT`() {
        assertEquals(AppLocale.DEFAULT, parseLanguageFromCode("en"))
    }

    @Test
    fun `parseLanguageFromCode de returns DE`() {
        assertEquals(AppLocale.DE, parseLanguageFromCode("de"))
    }

    @Test
    fun `parseLanguageFromCode fr returns FR`() {
        assertEquals(AppLocale.FR, parseLanguageFromCode("fr"))
    }

    @Test
    fun `parseLanguageFromCode es returns ES`() {
        assertEquals(AppLocale.ES, parseLanguageFromCode("es"))
    }

    @Test
    fun `parseLanguageFromCode it returns IT`() {
        assertEquals(AppLocale.IT, parseLanguageFromCode("it"))
    }

    @Test
    fun `parseLanguageFromCode is case insensitive`() {
        assertEquals(AppLocale.DE, parseLanguageFromCode("DE"))
        assertEquals(AppLocale.FR, parseLanguageFromCode("Fr"))
    }

    @Test
    fun `parseLanguageFromCode unsupported language returns DEFAULT`() {
        assertEquals(AppLocale.DEFAULT, parseLanguageFromCode("ja"))
        assertEquals(AppLocale.DEFAULT, parseLanguageFromCode("zh"))
    }

    @Test
    fun `parseLanguageFromCode null returns DEFAULT`() {
        assertEquals(AppLocale.DEFAULT, parseLanguageFromCode(null))
    }
}
