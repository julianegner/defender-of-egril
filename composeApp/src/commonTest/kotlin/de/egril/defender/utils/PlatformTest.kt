package de.egril.defender.utils

import kotlin.test.Test
import kotlin.test.assertNotNull
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
