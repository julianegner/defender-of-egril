package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnPointType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpawnPointTest {
    @Test
    fun testEditorEnemySpawnWithSpawnPoint() {
        val spawnPoint = Position(5, 10)
        val spawn =
            EditorEnemySpawn(
                attackerType = AttackerType.GOBLIN,
                level = 1,
                spawnTurn = 1,
                spawnPoint = spawnPoint,
            )

        assertEquals(spawnPoint, spawn.spawnPoint)
        assertEquals(AttackerType.GOBLIN, spawn.attackerType)
        assertEquals(1, spawn.level)
        assertEquals(1, spawn.spawnTurn)
    }

    @Test
    fun testEditorEnemySpawnWithoutSpawnPoint() {
        // Test backward compatibility - spawn point is optional
        val spawn =
            EditorEnemySpawn(
                attackerType = AttackerType.ORK,
                level = 2,
                spawnTurn = 5,
            )

        assertEquals(null, spawn.spawnPoint)
        assertEquals(AttackerType.ORK, spawn.attackerType)
        assertEquals(2, spawn.level)
        assertEquals(5, spawn.spawnTurn)
    }

    @Test
    fun testSerializationWithSpawnPoint() {
        val spawnPoint = Position(3, 7)
        val spawn =
            EditorEnemySpawn(
                attackerType = AttackerType.SKELETON,
                level = 3,
                spawnTurn = 2,
                spawnPoint = spawnPoint,
            )

        val level =
            EditorLevel(
                id = "test_level",
                mapId = "test_map",
                title = "Test Level",
                subtitle = "Testing",
                startCoins = 100,
                startHealthPoints = 10,
                enemySpawns = listOf(spawn),
                availableTowers = emptySet(),
            )

        val json = EditorJsonSerializer.serializeLevel(level)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(deserialized)
        assertEquals(1, deserialized.enemySpawns.size)
        assertEquals(spawnPoint, deserialized.enemySpawns[0].spawnPoint)
    }

    @Test
    fun testSerializationWithoutSpawnPoint() {
        // Test backward compatibility
        val spawn =
            EditorEnemySpawn(
                attackerType = AttackerType.GOBLIN,
                level = 1,
                spawnTurn = 1,
                spawnPoint = null,
            )

        val level =
            EditorLevel(
                id = "test_level_2",
                mapId = "test_map",
                title = "Test Level 2",
                subtitle = "Testing",
                startCoins = 100,
                startHealthPoints = 10,
                enemySpawns = listOf(spawn),
                availableTowers = emptySet(),
            )

        val json = EditorJsonSerializer.serializeLevel(level)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(deserialized)
        assertEquals(1, deserialized.enemySpawns.size)
        assertEquals(null, deserialized.enemySpawns[0].spawnPoint)
    }

    // ─── spawnPointInfoMap serialization ─────────────────────────────────────────

    @Test
    fun testMapSerializationWithSpawnPointInfo() {
        val landPos = "0,0"
        val waterPos = "0,4"
        val tiles = mutableMapOf<String, TileType>()
        tiles[landPos] = TileType.SPAWN_POINT
        tiles[waterPos] = TileType.SPAWN_POINT
        tiles["5,0"] = TileType.TARGET
        for (x in 0..5) tiles["$x,0"] = tiles["$x,0"] ?: TileType.PATH

        val map =
            EditorMap(
                id = "test_water_map",
                name = "Water Spawn Map",
                width = 6,
                height = 5,
                tiles = tiles,
                spawnPointInfoMap = mapOf(
                    landPos to SpawnPointType.LAND,
                    waterPos to SpawnPointType.WATER,
                ),
            )

        val json = EditorJsonSerializer.serializeMap(map)
        assertTrue(json.contains("spawnPointInfo"), "Serialized map must contain spawnPointInfo section")
        assertTrue(json.contains("\"WATER\""), "Serialized map must contain WATER type")
        assertTrue(json.contains("\"LAND\""), "Serialized map must contain LAND type")

        val restored = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(restored)
        assertEquals(2, restored.spawnPointInfoMap.size)
        assertEquals(SpawnPointType.LAND, restored.spawnPointInfoMap[landPos])
        assertEquals(SpawnPointType.WATER, restored.spawnPointInfoMap[waterPos])
    }

    @Test
    fun testMapDeserializationWithoutSpawnPointInfoIsBackwardCompatible() {
        // Older map JSON without spawnPointInfo must deserialize to empty map
        val legacyJson = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "legacy_map",
    "name": "Legacy Map",
    "width": 6,
    "height": 5,
    "readyToUse": false,
    "isOfficial": false,
    "mapToolingInfo": "procedural generation",
    "tiles": {
      "0,0": "SPAWN_POINT",
      "5,0": "TARGET"
    }
  }
}"""
        val map = EditorJsonSerializer.deserializeMap(legacyJson)
        assertNotNull(map)
        assertTrue(map.spawnPointInfoMap.isEmpty(), "Legacy map without spawnPointInfo must have empty spawn point info map")
    }

    @Test
    fun testEditorMapGetCompatibleSpawnPointsFiltersByType() {
        val map =
            EditorMap(
                id = "compat_map",
                width = 4,
                height = 2,
                tiles =
                    mapOf(
                        "0,0" to TileType.SPAWN_POINT,
                        "0,1" to TileType.SPAWN_POINT,
                    ),
                spawnPointInfoMap =
                    mapOf(
                        "0,0" to SpawnPointType.LAND,
                        "0,1" to SpawnPointType.WATER,
                    ),
            )

        val krakenPoints = map.getCompatibleSpawnPoints(AttackerType.THE_KRAKEN)
        val goblinPoints = map.getCompatibleSpawnPoints(AttackerType.GOBLIN)

        assertEquals(listOf(Position(0, 1)), krakenPoints)
        assertEquals(listOf(Position(0, 0)), goblinPoints)
    }
}
