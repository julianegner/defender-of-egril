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

    @Test
    fun narrowDesktopUsesCompactMainMenuLayout() {
        assertTrue(
            shouldUseCompactMainMenuLayout(
                isNativeMobile = false,
                isMobileWeb = false,
                isNarrowWindow = true
            )
        )
    }

    @Test
    fun narrowDesktopUsesStackedLayout() {
        assertTrue(
            shouldUseStackedMainMenuLayout(
                isNativeMobile = false,
                isMobileWeb = false,
                isPortrait = false,
                isNarrowWindow = true
            )
        )
    }

    @Test
    fun stackedNativeMobileShowsInlineVersionInfo() {
        assertTrue(
            shouldShowInlineMainMenuVersionInfo(
                usesStackedLayout = true,
                isMobileWeb = false
            )
        )
        assertFalse(
            shouldShowOverlayMainMenuVersionInfo(
                usesStackedLayout = true,
                isMobileWeb = false
            )
        )
    }

    @Test
    fun stackedMobileWebKeepsBottomVersionInfo() {
        assertFalse(
            shouldShowInlineMainMenuVersionInfo(
                usesStackedLayout = true,
                isMobileWeb = true
            )
        )
        assertTrue(
            shouldShowOverlayMainMenuVersionInfo(
                usesStackedLayout = true,
                isMobileWeb = true
            )
        )
    }

    @Test
    fun desktopKeepsBottomVersionInfo() {
        assertFalse(
            shouldShowInlineMainMenuVersionInfo(
                usesStackedLayout = false,
                isMobileWeb = false
            )
        )
        assertTrue(
            shouldShowOverlayMainMenuVersionInfo(
                usesStackedLayout = false,
                isMobileWeb = false
            )
        )
    }
}
