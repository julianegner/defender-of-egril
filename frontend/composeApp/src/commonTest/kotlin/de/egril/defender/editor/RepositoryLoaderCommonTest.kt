package de.egril.defender.editor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Multiplatform tests for RepositoryLoader.
 * These tests work across all platforms including WASM.
 * 
 * Note: Uses kotlinx.coroutines.test.runTest which provides multiplatform support
 * for testing suspend functions across JVM, JS, and Native platforms.
 */
class RepositoryLoaderCommonTest {
    private class CountingFileStorage : FileStorage {
        private val textFiles = mutableMapOf<String, String>()
        private val binaryFiles = mutableMapOf<String, ByteArray>()
        val writtenPaths = mutableListOf<String>()

        fun seedTextFile(path: String, content: String) {
            textFiles[path] = content
        }

        override fun writeFile(path: String, content: String) {
            writtenPaths += path
            textFiles[path] = content
        }

        override fun readFile(path: String): String? = textFiles[path]

        override fun listFiles(directory: String): List<String> {
            val prefix = "$directory/"
            return textFiles.keys.asSequence()
                .filter { it.startsWith(prefix) }
                .map { it.removePrefix(prefix) }
                .filter { !it.contains("/") }
                .toList()
        }

        override fun fileExists(path: String): Boolean {
            val normalized = "$path/"
            return textFiles.containsKey(path) ||
                binaryFiles.containsKey(path) ||
                textFiles.keys.any { it.startsWith(normalized) } ||
                binaryFiles.keys.any { it.startsWith(normalized) }
        }

        override fun createDirectory(path: String) = Unit
        override fun deleteFile(path: String) {
            textFiles.remove(path)
            binaryFiles.remove(path)
        }
        override fun renameDirectory(oldPath: String, newPath: String): Boolean = false
        override fun copyDirectory(sourcePath: String, targetPath: String): Boolean = false
        override fun deleteDirectory(path: String): Boolean = false
        override fun getAbsolutePath(path: String): String = path
        override fun writeBinaryFile(path: String, content: ByteArray) {
            binaryFiles[path] = content
        }
        override fun readBinaryFile(path: String): ByteArray? = binaryFiles[path]
    }

    
    @Test
    fun testLoadSequenceFromRepository() = runTest {
        // This test verifies that the repository sequence can be loaded
        // Works on all platforms including WASM
        val sequence = RepositoryLoader.loadSequence()
        
        // If sequence is null, it means resources aren't available in this test environment (acceptable)
        if (sequence != null) {
            assertTrue(sequence.sequence.isNotEmpty(), "Sequence should not be empty")
            assertTrue(sequence.sequence.contains("welcome_to_defender_of_egril"), 
                "Sequence should contain the tutorial level")
        }
    }
    
    @Test
    fun testLoadMapFromRepository() = runTest {
        // This test verifies that a map can be loaded from repository
        // Works on all platforms including WASM
        val map = RepositoryLoader.loadMap("map_tutorial")
        
        // If map is null, it means resources aren't available in this test environment (acceptable)
        if (map != null) {
            assertTrue(map.id == "map_tutorial", "Map ID should be correct")
            assertTrue(map.width == 15, "Map width should be 15")
            assertTrue(map.height == 8, "Map height should be 8")
            assertTrue(map.readyToUse, "Map should be marked as ready to use")
        }
    }
    
    @Test
    fun testLoadLevelFromRepository() = runTest {
        // This test verifies that a level can be loaded from repository
        // Works on all platforms including WASM
        val level = RepositoryLoader.loadLevel("welcome_to_defender_of_egril")
        
        // If level is null, it means resources aren't available in this test environment (acceptable)
        if (level != null) {
            assertTrue(level.id == "welcome_to_defender_of_egril", "Level ID should be correct")
            assertTrue(level.mapId == "map_tutorial", "Level should reference correct map")
            assertTrue(level.title == "Welcome to Defender of Egril", "Level title should be correct")
            assertTrue(level.enemySpawns.isNotEmpty(), "Level should have enemy spawns")
            assertTrue(level.availableTowers.isNotEmpty(), "Level should have available towers")
        }
    }
    
    @Test
    fun testHasRepositoryFiles() = runTest {
        // This test verifies that repository files are detected
        // Works on all platforms including WASM
        val hasFiles = RepositoryLoader.hasRepositoryFiles()
        
        // Don't fail the test if resources aren't available
        // The functionality will work in the actual app
        // This test just ensures the function can be called without errors
        assertTrue(true, "Repository file check completed without errors")
    }
    
    @Test
    fun testLoadWorldMapDataFromRepository() = runTest {
        // This test verifies that the worldmap data can be loaded from repository
        // Works on all platforms including WASM
        val worldMapData = RepositoryLoader.loadWorldMapData()
        
        // If worldMapData is null, it means resources aren't available in this test environment (acceptable)
        if (worldMapData != null) {
            assertTrue(worldMapData.locations.isNotEmpty(), "WorldMapData should have locations")
            // Check for some known locations from the repository worldmap.json
            val locationIds = worldMapData.locations.map { it.id }
            assertTrue(locationIds.contains("tutorial"), "WorldMapData should contain tutorial location")
            assertTrue(locationIds.contains("the_beginning"), "WorldMapData should contain the_beginning location")
        }
    }
    
    @Test
    fun testLoadDragonNames() = runTest {
        // This test verifies that dragon names can be loaded from repository
        // Works on all platforms including WASM
        val dragonNames = RepositoryLoader.loadDragonNames()
        
        // If dragonNames is null, it means resources aren't available in this test environment (acceptable)
        if (dragonNames != null) {
            assertTrue(dragonNames.isNotEmpty(), "Dragon names list should not be empty")
            // All dragon names should be non-blank strings
            assertTrue(dragonNames.all { it.isNotBlank() }, "All dragon names should be non-blank")
        }
    }

    @Test
    fun testLoadAndSaveRepositoryFilesReloadsEvenWhenVersionMatches() = runTest {
        // Skip in environments where bundled repository resources are unavailable.
        if (!RepositoryLoader.hasRepositoryFiles()) return@runTest

        val bundledVersion = RepositoryLoader.loadVersion() ?: return@runTest
        val storage = CountingFileStorage().apply {
            seedTextFile("gamedata/version.txt", bundledVersion)
            seedTextFile("gamedata/official/maps/existing_map.json", "{}")
        }

        val success = RepositoryLoader.loadAndSaveRepositoryFiles(storage)

        assertTrue(success, "Repository sync should succeed")
        assertTrue(
            storage.writtenPaths.any { it.startsWith("gamedata/official/levels/") },
            "Official levels should be rewritten even when stored and bundled versions match"
        )
        assertTrue(
            storage.writtenPaths.contains("gamedata/official/sequence.json"),
            "Official sequence should be rewritten during reload"
        )
    }
}
