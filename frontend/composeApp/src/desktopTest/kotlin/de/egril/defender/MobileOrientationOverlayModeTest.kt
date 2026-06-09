package de.egril.defender

import de.egril.defender.ui.Screen
import de.egril.defender.utils.MobileOrientationOverlayMode
import kotlin.test.Test
import kotlin.test.assertEquals

class MobileOrientationOverlayModeTest {

    @Test
    fun `gameplay requires landscape overlay`() {
        assertEquals(
            MobileOrientationOverlayMode.LANDSCAPE_REQUIRED,
            mobileOrientationOverlayModeForScreen(Screen.GamePlay(levelId = 1))
        )
    }

    @Test
    fun `main menu requires portrait overlay`() {
        assertEquals(
            MobileOrientationOverlayMode.PORTRAIT_REQUIRED,
            mobileOrientationOverlayModeForScreen(Screen.MainMenu)
        )
    }

    @Test
    fun `info screens require portrait overlay`() {
        assertEquals(
            MobileOrientationOverlayMode.PORTRAIT_REQUIRED,
            mobileOrientationOverlayModeForScreen(Screen.InstallationInfo)
        )
        assertEquals(
            MobileOrientationOverlayMode.PORTRAIT_REQUIRED,
            mobileOrientationOverlayModeForScreen(Screen.InstallationInfoAtTab(de.egril.defender.ui.infopage.InfoTab.HOW_TO_PLAY))
        )
    }

    @Test
    fun `world map requires portrait overlay`() {
        assertEquals(
            MobileOrientationOverlayMode.PORTRAIT_REQUIRED,
            mobileOrientationOverlayModeForScreen(Screen.WorldMap)
        )
    }

    @Test
    fun `other screens do not force orientation overlay`() {
        assertEquals(
            MobileOrientationOverlayMode.NONE,
            mobileOrientationOverlayModeForScreen(Screen.Rules)
        )
        assertEquals(
            MobileOrientationOverlayMode.NONE,
            mobileOrientationOverlayModeForScreen(Screen.LoadGame)
        )
        assertEquals(
            MobileOrientationOverlayMode.NONE,
            mobileOrientationOverlayModeForScreen(Screen.LevelComplete(levelId = 1, won = true, isLastLevel = false))
        )
    }
}
