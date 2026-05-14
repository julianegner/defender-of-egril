package de.egril.defender.ui.gameplay

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TutorialOverlayLayoutTest {

    @Test
    fun mobileWebTutorialOverlayUsesMoreHeight() {
        val layout = calculateTutorialOverlayLayout(
            availableWidth = 900.dp,
            availableHeight = 480.dp,
            isMobileWeb = true,
            isSideOverlayVisible = false
        )

        assertTrue(layout.minHeight >= 180.dp)
        assertTrue(layout.maxHeight > layout.minHeight)
        assertTrue(layout.compactTypography)
    }

    @Test
    fun desktopTutorialOverlayKeepsExistingDefaultSize() {
        val layout = calculateTutorialOverlayLayout(
            availableWidth = 1400.dp,
            availableHeight = 900.dp,
            isMobileWeb = false,
            isSideOverlayVisible = false
        )

        assertEquals(300.dp, layout.width)
        assertEquals(0.dp, layout.minHeight)
        assertEquals(400.dp, layout.maxHeight)
    }
}
