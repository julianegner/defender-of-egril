package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainMenuLayoutDecisionTest {

    @Test
    fun mobileWebUsesStackedLayoutInPortrait() {
        assertTrue(
            shouldUseStackedMainMenuLayout(
                isNativeMobile = false,
                isMobileWeb = true,
                isPortrait = true
            )
        )
    }

    @Test
    fun mobileWebDoesNotUseStackedLayoutInLandscape() {
        assertFalse(
            shouldUseStackedMainMenuLayout(
                isNativeMobile = false,
                isMobileWeb = true,
                isPortrait = false
            )
        )
    }

    @Test
    fun nativeMobileStillUsesStackedLayoutOutsidePortrait() {
        assertTrue(
            shouldUseStackedMainMenuLayout(
                isNativeMobile = true,
                isMobileWeb = false,
                isPortrait = false
            )
        )
    }

    @Test
    fun mobileWebUsesCompactMainMenuLayoutInLandscape() {
        assertTrue(
            shouldUseCompactMainMenuLayout(
                isNativeMobile = false,
                isMobileWeb = true
            )
        )
    }

    @Test
    fun desktopDoesNotUseCompactMainMenuLayout() {
        assertFalse(
            shouldUseCompactMainMenuLayout(
                isNativeMobile = false,
                isMobileWeb = false
            )
        )
    }
}
