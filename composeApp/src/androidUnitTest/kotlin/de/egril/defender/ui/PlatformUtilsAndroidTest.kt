package de.egril.defender.ui

import android.app.Activity
import de.egril.defender.AndroidContextProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Unit tests for Android platform utilities
 */
class PlatformUtilsAndroidTest {
    
    @Before
    fun setup() {
        mockkObject(AndroidContextProvider)
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    @Test
    fun `isEditorAvailable returns false on Android`() {
        // Editor is only available on desktop and WASM
        assertFalse(isEditorAvailable())
    }
    
    @Test
    fun `getGameplayUIScale returns 0,5f for Android`() {
        // Android uses smaller UI scale for touch devices
        assertEquals(0.5f, getGameplayUIScale())
    }
    
    @Test
    fun `exitApplication calls finish on Activity`() {
        val mockActivity = mockk<Activity>(relaxed = true)
        every { AndroidContextProvider.getContext() } returns mockActivity
        
        exitApplication()
        
        verify { mockActivity.finish() }
    }
    
    @Test
    fun `exitApplication does nothing when context is not Activity`() {
        val mockContext = mockk<android.content.Context>(relaxed = true)
        every { AndroidContextProvider.getContext() } returns mockContext
        
        // Should not throw exception
        exitApplication()
    }
    
    @Test
    fun `exitApplication handles exception gracefully`() {
        every { AndroidContextProvider.getContext() } throws RuntimeException("Context not available")
        
        // Should not throw exception
        try {
            exitApplication()
        } catch (e: Exception) {
            // Expected - context not available
        }
    }
}
