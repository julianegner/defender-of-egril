package de.egril.defender

import android.content.Context
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for AndroidContextProvider
 */
class AndroidContextProviderTest {
    
    @Test
    fun `initialize sets application context`() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockAppContext = mockk<Context>(relaxed = true)
        
        io.mockk.every { mockContext.applicationContext } returns mockAppContext
        
        AndroidContextProvider.initialize(mockContext)
        val retrievedContext = AndroidContextProvider.getContext()
        
        assertEquals(mockAppContext, retrievedContext)
    }
    
    @Test
    fun `getContext throws exception when not initialized`() {
        // Create a fresh instance by using reflection to reset the context
        val field = AndroidContextProvider::class.java.getDeclaredField("applicationContext")
        field.isAccessible = true
        field.set(AndroidContextProvider, null)
        
        val exception = assertFailsWith<IllegalStateException> {
            AndroidContextProvider.getContext()
        }
        
        assertNotNull(exception.message)
        assert(exception.message!!.contains("not initialized"))
    }
    
    @Test
    fun `initialize can be called multiple times`() {
        val mockContext1 = mockk<Context>(relaxed = true)
        val mockAppContext1 = mockk<Context>(relaxed = true)
        val mockContext2 = mockk<Context>(relaxed = true)
        val mockAppContext2 = mockk<Context>(relaxed = true)
        
        io.mockk.every { mockContext1.applicationContext } returns mockAppContext1
        io.mockk.every { mockContext2.applicationContext } returns mockAppContext2
        
        AndroidContextProvider.initialize(mockContext1)
        assertEquals(mockAppContext1, AndroidContextProvider.getContext())
        
        AndroidContextProvider.initialize(mockContext2)
        assertEquals(mockAppContext2, AndroidContextProvider.getContext())
    }
}
