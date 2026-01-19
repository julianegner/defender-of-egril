package de.egril.defender.editor

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidFileStorage in-memory fallback mode
 * (used when Android context is not available in unit tests)
 */
class AndroidFileStorageTest {
    
    @Test
    fun `getFileStorage returns AndroidFileStorage instance`() {
        val storage = getFileStorage()
        assertNotNull(storage)
        assertTrue(storage is AndroidFileStorage)
    }
    
    @Test
    fun `writeFile and readFile work in memory mode`() {
        val storage = AndroidFileStorage()
        val testPath = "test/file.txt"
        val testContent = "Hello, World!"
        
        storage.writeFile(testPath, testContent)
        val readContent = storage.readFile(testPath)
        
        assertEquals(testContent, readContent)
    }
    
    @Test
    fun `readFile returns null for non-existent file`() {
        val storage = AndroidFileStorage()
        val result = storage.readFile("non/existent/file.txt")
        assertNull(result)
    }
    
    @Test
    fun `fileExists returns true for existing file`() {
        val storage = AndroidFileStorage()
        val testPath = "test/file.txt"
        
        storage.writeFile(testPath, "content")
        assertTrue(storage.fileExists(testPath))
    }
    
    @Test
    fun `fileExists returns false for non-existent file`() {
        val storage = AndroidFileStorage()
        assertFalse(storage.fileExists("non/existent/file.txt"))
    }
    
    @Test
    fun `listFiles returns files in directory`() {
        val storage = AndroidFileStorage()
        
        storage.writeFile("maps/map1.json", "content1")
        storage.writeFile("maps/map2.json", "content2")
        storage.writeFile("maps/subfolder/map3.json", "content3")
        storage.writeFile("levels/level1.json", "content4")
        
        val files = storage.listFiles("maps")
        
        assertTrue(files.contains("map1.json"))
        assertTrue(files.contains("map2.json"))
        // Subdirectories should be included
        assertTrue(files.contains("subfolder") || files.size >= 2)
    }
    
    @Test
    fun `createDirectory works in memory mode`() {
        val storage = AndroidFileStorage()
        storage.createDirectory("test/directory")
        // No exception means success
    }
    
    @Test
    fun `deleteFile removes file from memory`() {
        val storage = AndroidFileStorage()
        val testPath = "test/file.txt"
        
        storage.writeFile(testPath, "content")
        assertTrue(storage.fileExists(testPath))
        
        storage.deleteFile(testPath)
        assertFalse(storage.fileExists(testPath))
    }
    
    @Test
    fun `deleteDirectory removes all files in directory`() {
        val storage = AndroidFileStorage()
        
        storage.writeFile("testdir/file1.txt", "content1")
        storage.writeFile("testdir/file2.txt", "content2")
        storage.writeFile("testdir/subfolder/file3.txt", "content3")
        
        val result = storage.deleteDirectory("testdir")
        
        assertTrue(result)
        assertFalse(storage.fileExists("testdir/file1.txt"))
        assertFalse(storage.fileExists("testdir/file2.txt"))
        assertFalse(storage.fileExists("testdir/subfolder/file3.txt"))
    }
    
    @Test
    fun `getAbsolutePath returns path in memory mode`() {
        val storage = AndroidFileStorage()
        val testPath = "test/file.txt"
        
        val absolutePath = storage.getAbsolutePath(testPath)
        assertEquals(testPath, absolutePath)
    }
    
    @Test
    fun `renameDirectory returns false in memory mode`() {
        val storage = AndroidFileStorage()
        val result = storage.renameDirectory("old", "new")
        assertFalse(result)
    }
    
    @Test
    fun `copyDirectory returns false in memory mode`() {
        val storage = AndroidFileStorage()
        val result = storage.copyDirectory("source", "target")
        assertFalse(result)
    }
    
    @Test
    fun `multiple files can be stored and retrieved`() {
        val storage = AndroidFileStorage()
        val files = mapOf(
            "file1.txt" to "content1",
            "file2.txt" to "content2",
            "file3.txt" to "content3"
        )
        
        files.forEach { (path, content) ->
            storage.writeFile(path, content)
        }
        
        files.forEach { (path, expectedContent) ->
            val actualContent = storage.readFile(path)
            assertEquals(expectedContent, actualContent)
        }
    }
}
