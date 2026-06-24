package de.egril.defender.ui.infopage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InfoPageBrowserNavigationTest {
    @Test
    fun infoRouteSelectsRequestedTab() {
        val tabs =
            buildVisibleInfoTabs(
                showDownloadTab = true,
                showInstallationTab = true,
                showEditorHowToTab = false,
            )

        val navigation = resolveInfoPageBrowserNavigation("/info/keyboard-shortcuts", tabs)

        assertIs<InfoPageBrowserNavigation.SelectTab>(navigation)
        assertEquals(InfoTab.KEYBOARD_SHORTCUTS, navigation.tab)
    }

    @Test
    fun dataPrivacyRouteSelectsBackendTab() {
        val tabs =
            buildVisibleInfoTabs(
                showDownloadTab = true,
                showInstallationTab = true,
                showEditorHowToTab = false,
            )

        val navigation = resolveInfoPageBrowserNavigation("/data-privacy/en", tabs)

        assertIs<InfoPageBrowserNavigation.SelectTab>(navigation)
        assertEquals(InfoTab.BACKEND, navigation.tab)
    }

    @Test
    fun unknownInfoSlugFallsBackToFirstVisibleTab() {
        val tabs =
            buildVisibleInfoTabs(
                showDownloadTab = false,
                showInstallationTab = false,
                showEditorHowToTab = false,
            )

        val navigation = resolveInfoPageBrowserNavigation("/info/not-a-tab", tabs)

        assertIs<InfoPageBrowserNavigation.SelectTab>(navigation)
        assertEquals(tabs.first(), navigation.tab)
    }

    @Test
    fun rootPathNavigatesBack() {
        val tabs =
            buildVisibleInfoTabs(
                showDownloadTab = true,
                showInstallationTab = true,
                showEditorHowToTab = false,
            )

        val navigation = resolveInfoPageBrowserNavigation("/", tabs)

        assertEquals(InfoPageBrowserNavigation.NavigateBack, navigation)
    }
}
