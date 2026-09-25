package de.egril.defender.ui.infopage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfoPageLayoutDecisionTest {
    @Test
    fun mobileWebLandscapeUsesCompactHeader() {
        assertTrue(
            shouldUseCompactInfoHeaderLayout(
                isMobileWeb = true,
                isLandscape = true,
            ),
        )
    }

    @Test
    fun mobileWebPortraitDoesNotUseCompactHeader() {
        assertFalse(
            shouldUseCompactInfoHeaderLayout(
                isMobileWeb = true,
                isLandscape = false,
            ),
        )
    }

    @Test
    fun desktopLandscapeDoesNotUseCompactHeader() {
        assertFalse(
            shouldUseCompactInfoHeaderLayout(
                isMobileWeb = false,
                isLandscape = true,
            ),
        )
    }

    @Test
    fun installationTabIsHiddenWhenPlatformIsNotWeb() {
        val tabs =
            buildVisibleInfoTabs(
                showDownloadTab = false,
                showInstallationTab = false,
                showEditorHowToTab = false,
            )

        assertFalse(InfoTab.INSTALLATION in tabs)
    }

    @Test
    fun installationTabIsShownOnWeb() {
        val tabs =
            buildVisibleInfoTabs(
                showDownloadTab = true,
                showInstallationTab = true,
                showEditorHowToTab = false,
            )

        assertTrue(InfoTab.INSTALLATION in tabs)
    }

    @Test
    fun streamerInfoTabIsAlwaysLast() {
        val tabConfigurations =
            listOf(
                buildVisibleInfoTabs(showDownloadTab = true, showInstallationTab = true, showEditorHowToTab = true),
                buildVisibleInfoTabs(showDownloadTab = true, showInstallationTab = false, showEditorHowToTab = false),
                buildVisibleInfoTabs(showDownloadTab = false, showInstallationTab = true, showEditorHowToTab = false),
            )

        tabConfigurations.forEach { tabs ->
            assertEquals(InfoTab.STREAMER_INFO, tabs.last())
        }
    }
}
