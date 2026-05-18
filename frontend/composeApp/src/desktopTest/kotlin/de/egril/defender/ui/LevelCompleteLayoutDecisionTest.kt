package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LevelCompleteLayoutDecisionTest {

    @Test
    fun nativeMobileUsesMobileLevelCompleteLayout() {
        assertTrue(
            shouldUseMobileLevelCompleteLayout(
                isNativeMobile = true,
                isMobileWeb = false
            )
        )
    }

    @Test
    fun mobileWebUsesMobileLevelCompleteLayout() {
        assertTrue(
            shouldUseMobileLevelCompleteLayout(
                isNativeMobile = false,
                isMobileWeb = true
            )
        )
    }

    @Test
    fun desktopDoesNotUseMobileLevelCompleteLayout() {
        assertFalse(
            shouldUseMobileLevelCompleteLayout(
                isNativeMobile = false,
                isMobileWeb = false
            )
        )
    }

    @Test
    fun mobilePortraitStacksButtons() {
        assertTrue(
            shouldStackLevelCompleteButtons(
                isMobileLayout = true,
                isPortrait = true
            )
        )
    }

    @Test
    fun mobileLandscapeDoesNotStackButtons() {
        assertFalse(
            shouldStackLevelCompleteButtons(
                isMobileLayout = true,
                isPortrait = false
            )
        )
    }

    @Test
    fun desktopPortraitDoesNotStackButtons() {
        assertFalse(
            shouldStackLevelCompleteButtons(
                isMobileLayout = false,
                isPortrait = true
            )
        )
    }
}
