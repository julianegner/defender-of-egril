package de.egril.defender.game

import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test level filtering logic to ensure only ready-to-play levels are shown
 */
class LevelFilteringTest {
    
    @Test
    fun testMapReadyValidation() {
        // Map with valid path from spawn to target should be ready
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.PATH
        tiles["3,0"] = TileType.TARGET
        
        val validMap = EditorMap(
            id = "valid_map",
            name = "Valid Map",
            width = 10,
            height = 5,
            tiles = tiles
        )
        
        assertTrue(validMap.validateReadyToUse(), "Map with valid path should be ready")
        
        // Map without target should not be ready
        val noTargetTiles = mutableMapOf<String, TileType>()
        noTargetTiles["0,0"] = TileType.SPAWN_POINT
        noTargetTiles["1,0"] = TileType.PATH
        noTargetTiles["2,0"] = TileType.PATH
        
        val noTargetMap = EditorMap(
            id = "no_target",
            name = "No Target",
            width = 10,
            height = 5,
            tiles = noTargetTiles
        )
        
        assertTrue(!noTargetMap.validateReadyToUse(), "Map without target should not be ready")
        
        // Map without spawn should not be ready
        val noSpawnTiles = mutableMapOf<String, TileType>()
        noSpawnTiles["0,0"] = TileType.PATH
        noSpawnTiles["1,0"] = TileType.PATH
        noSpawnTiles["2,0"] = TileType.TARGET
        
        val noSpawnMap = EditorMap(
            id = "no_spawn",
            name = "No Spawn",
            width = 10,
            height = 5,
            tiles = noSpawnTiles
        )
        
        assertTrue(!noSpawnMap.validateReadyToUse(), "Map without spawn should not be ready")
    }
    
    @Test
    fun testLevelReadyValidation() {
        // Complete level - should be ready
        val completeLevel = EditorLevel(
            id = "complete",
            mapId = "test_map",
            title = "Complete",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertTrue(completeLevel.isReadyToPlay(), "Level with towers and spawns should be ready")
        
        // Level without towers - should not be ready
        val noTowersLevel = EditorLevel(
            id = "no_towers",
            mapId = "test_map",
            title = "No Towers",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = emptySet()
        )
        assertTrue(!noTowersLevel.isReadyToPlay(), "Level without towers should not be ready")
        
        // Level without spawns - should not be ready
        val noSpawnsLevel = EditorLevel(
            id = "no_spawns",
            mapId = "test_map",
            title = "No Spawns",
            startCoins = 100,
            enemySpawns = emptyList(),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertTrue(!noSpawnsLevel.isReadyToPlay(), "Level without spawns should not be ready")
        
        // Level with multiple towers - should be ready
        val multipleTowersLevel = EditorLevel(
            id = "multi_towers",
            mapId = "test_map",
            title = "Multiple Towers",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(
                DefenderType.SPIKE_TOWER,
                DefenderType.BOW_TOWER,
                DefenderType.WIZARD_TOWER
            )
        )
        assertTrue(multipleTowersLevel.isReadyToPlay(), "Level with multiple towers should be ready")
        
        // Level with multiple spawns - should be ready
        val multipleSpawnsLevel = EditorLevel(
            id = "multi_spawns",
            mapId = "test_map",
            title = "Multiple Spawns",
            startCoins = 100,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1),
                EditorEnemySpawn(AttackerType.ORK, 1, 2),
                EditorEnemySpawn(AttackerType.OGRE, 1, 3)
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertTrue(multipleSpawnsLevel.isReadyToPlay(), "Level with multiple spawns should be ready")
    }
}
