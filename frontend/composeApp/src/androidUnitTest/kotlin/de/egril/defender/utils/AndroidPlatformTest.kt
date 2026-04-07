package de.egril.defender.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import de.egril.defender.AndroidContextProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidPlatform
 */
class AndroidPlatformTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        
        every { mockContext.packageManager } returns mockPackageManager
        
        mockkObject(AndroidContextProvider)
        every { AndroidContextProvider.getContext() } returns mockContext
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    @Test
    fun `getPlatform returns AndroidPlatform instance`() {
        val platform = getPlatform()
        assertNotNull(platform)
        assertTrue(platform is AndroidPlatform)
    }
    
    @Test
    fun `platform name contains Android version`() {
        val platform = getPlatform()
        assertTrue(platform.name.contains("Android"))
        assertTrue(platform.name.contains(Build.VERSION.SDK_INT.toString()))
    }
    
    @Test
    fun `isAndroidTV returns true when leanback feature is present`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns true
        
        val platform = AndroidPlatform()
        assertTrue(platform.isAndroidTV)
        assertTrue(platform.name.contains("Android TV"))
    }
    
    @Test
    fun `isAndroidTV returns false when leanback feature is not present`() {
        every { mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) } returns false
        
        val platform = AndroidPlatform()
        assertFalse(platform.isAndroidTV)
        assertFalse(platform.name.contains("Android TV"))
    }
    
    @Test
    fun `isAndroidTV returns false when context throws exception`() {
        every { AndroidContextProvider.getContext() } throws RuntimeException("Context not available")
        
        val platform = AndroidPlatform()
        assertFalse(platform.isAndroidTV)
    }
    
    @Test
    fun `platform name falls back to base info when context unavailable`() {
        every { AndroidContextProvider.getContext() } throws RuntimeException("Context not available")
        
        val platform = AndroidPlatform()
        assertEquals("Android ${Build.VERSION.SDK_INT}", platform.name)
    }
    
    @Test
    fun `getSystemLanguageCode returns lowercase language code`() {
        val languageCode = getSystemLanguageCode()
        assertNotNull(languageCode)
        assertEquals(languageCode, languageCode.lowercase())
    }
}
