package de.egril.defender.save

import androidx.activity.ComponentActivity
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidFileExportImport initialization
 */
class AndroidFileExportImportTest {
    
    @Test
    fun `initialize accepts ComponentActivity without error`() {
        val mockActivity = mockk<ComponentActivity>(relaxed = true)
        
        // Should initialize without throwing exception
        AndroidFileExportImport.initialize(mockActivity)
    }
    
    @Test
    fun `getInstance returns AndroidFileExportImport instance`() {
        val instance = AndroidFileExportImport.getInstance()
        assertNotNull(instance)
        assertTrue(instance is AndroidFileExportImport)
    }
    
    @Test
    fun `getInstance is singleton`() {
        val instance1 = AndroidFileExportImport.getInstance()
        val instance2 = AndroidFileExportImport.getInstance()
        
        // Should return same instance
        assertTrue(instance1 === instance2)
    }
    
    @Test
    fun `getFileExportImport returns AndroidFileExportImport instance`() {
        val exportImport = getFileExportImport()
        assertNotNull(exportImport)
        assertTrue(exportImport is AndroidFileExportImport)
    }
}
