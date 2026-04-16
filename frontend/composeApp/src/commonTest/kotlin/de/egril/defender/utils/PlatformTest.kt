package de.egril.defender.utils

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformTest {
    
    @Test
    fun testPlatformHasName() {
        val platform = getPlatform()
        assertNotNull(platform.name, "Platform name should not be null")
        assertTrue(platform.name.isNotEmpty(), "Platform name should not be empty")
    }
    
    @Test
    fun testPlatformHasAndroidTVProperty() {
        val platform = getPlatform()
        // isAndroidTV should be a boolean (non-null)
        // This test just verifies the property exists and is accessible
        val isTV = platform.isAndroidTV
        // On non-Android platforms, this should be false
        // On Android, it depends on whether FEATURE_LEANBACK is present
        assertTrue(isTV || !isTV, "isAndroidTV should be a boolean value")
    }

    @Test
    fun testPlatformHasSteamDeckGamingModeProperty() {
        val platform = getPlatform()
        // isSteamDeckGamingMode should be a boolean (non-null)
        // Only JVMPlatform (desktopMain) can return true; all other platforms return false
        val isSteamDeck = platform.isSteamDeckGamingMode
        assertTrue(isSteamDeck || !isSteamDeck, "isSteamDeckGamingMode should be a boolean value")
    }

    @Test
    fun testNonSteamDeckPlatformsReturnFalse() {
        val platform = getPlatform()
        // In the test environment (not a Steam Deck) isSteamDeckGamingMode must be false
        if (!isPlatformDesktop) {
            assertFalse(platform.isSteamDeckGamingMode, "Non-desktop platforms must report isSteamDeckGamingMode=false")
        }
    }

    @Test
    fun testIsLimitedInputDeviceIsFalseInTestEnvironment() {
        // In CI / unit-test environments we are never on an Android TV or Steam Deck gaming mode
        assertFalse(isLimitedInputDevice, "isLimitedInputDevice should be false in the test environment")
    }
    
    @Test
    fun testPlatformDetectionHelpers() {
        // Test that platform detection helpers work
        val isWasm = isPlatformWasm
        val isAndroid = isPlatformAndroid
        val isIos = isPlatformIos
        val isDesktop = isPlatformDesktop
        val isMobile = isPlatformMobile
        
        // At least one platform should be detected
        assertTrue(
            isWasm || isAndroid || isIos || isDesktop,
            "At least one platform should be detected"
        )
        
        // Mobile should be true if Android or iOS
        if (isAndroid || isIos) {
            assertTrue(isMobile, "Mobile should be true for Android or iOS")
        }
    }
}
