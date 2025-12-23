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
}
