package de.egril.defender.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementNotificationDialogLayoutTest {
    @Test
    fun mobileWebLandscapeUsesCompactHalfSizedDialog() {
        val layout =
            calculateAchievementNotificationLayout(
                availableWidth = 800.dp,
                availableHeight = 400.dp,
                isPlatformMobileDevice = false,
                isMobileWeb = true,
            )

        assertEquals(AchievementNotificationTypographyMode.VERY_COMPACT, layout.typographyMode)
        assertTrue(layout.cardWidth <= 320.dp)
        assertTrue(layout.maxHeight <= 260.dp)
        assertTrue(layout.buttonFillFraction < 1f)
    }

    @Test
    fun mobileWebPortraitUsesCompactDialog() {
        val layout =
            calculateAchievementNotificationLayout(
                availableWidth = 400.dp,
                availableHeight = 700.dp,
                isPlatformMobileDevice = false,
                isMobileWeb = true,
            )

        assertEquals(AchievementNotificationTypographyMode.COMPACT, layout.typographyMode)
        assertTrue(layout.iconSize <= 40.dp)
        assertTrue(layout.maxHeight < 700.dp * 0.6f)
    }

    @Test
    fun desktopKeepsLargerDialogLayout() {
        val layout =
            calculateAchievementNotificationLayout(
                availableWidth = 1200.dp,
                availableHeight = 900.dp,
                isPlatformMobileDevice = false,
                isMobileWeb = false,
            )

        assertEquals(AchievementNotificationTypographyMode.DESKTOP, layout.typographyMode)
        assertEquals(64.dp, layout.iconSize)
        assertEquals(1f, layout.buttonFillFraction)
    }
}
