package de.egril.defender.ui.infopage

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfoPageLayoutDecisionTest {

    @Test
    fun mobileWebLandscapeUsesCompactHeader() {
        assertTrue(
            shouldUseCompactInfoHeaderLayout(
                isMobileWeb = true,
                isLandscape = true
            )
        )
    }

    @Test
    fun mobileWebPortraitDoesNotUseCompactHeader() {
        assertFalse(
            shouldUseCompactInfoHeaderLayout(
                isMobileWeb = true,
                isLandscape = false
            )
        )
    }

    @Test
    fun desktopLandscapeDoesNotUseCompactHeader() {
        assertFalse(
            shouldUseCompactInfoHeaderLayout(
                isMobileWeb = false,
                isLandscape = true
            )
        )
    }
}
