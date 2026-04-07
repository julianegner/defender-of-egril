package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WaypointSerializationTest {
    
    @Test
    fun testSerializeLevelWithWaypoints() {
        val waypoints = listOf(
            EditorWaypoint(Position(5, 5), Position(10, 10)),
            EditorWaypoint(Position(10, 10), Position(15, 15)),
            EditorWaypoint(Position(15, 15), Position(20, 20))
        )
        
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            waypoints = waypoints
        )
        
        val json = EditorJsonSerializer.serializeLevel(level)
        
        // Verify waypoints are in the JSON
        assertTrue(json.contains("\"waypoints\""), "JSON should contain waypoints field")
        assertTrue(json.contains("\"position\""), "JSON should contain position field")
        assertTrue(json.contains("\"nextTargetPosition\""), "JSON should contain nextTargetPosition field")
    }
    
    @Test
    fun testDeserializeLevelWithWaypoints() {
        val json = """
{
  "id": "test_level",
  "mapId": "test_map",
  "title": "Test Level",
  "subtitle": "",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1}
  ],
  "availableTowers": ["SPIKE_TOWER"],
  "waypoints": [
    {"position": {"x": 5, "y": 5}, "nextTargetPosition": {"x": 10, "y": 10}},
    {"position": {"x": 10, "y": 10}, "nextTargetPosition": {"x": 15, "y": 15}},
    {"position": {"x": 15, "y": 15}, "nextTargetPosition": {"x": 20, "y": 20}}
  ]
}
        """.trimIndent()
        
        val level = EditorJsonSerializer.deserializeLevel(json)
        
        assertNotNull(level, "Level should be deserialized")
        assertEquals(3, level.waypoints.size, "Should have 3 waypoints")
        
        // Verify first waypoint
        val wp1 = level.waypoints[0]
        assertEquals(Position(5, 5), wp1.position)
        assertEquals(Position(10, 10), wp1.nextTargetPosition)
        
        // Verify second waypoint
        val wp2 = level.waypoints[1]
        assertEquals(Position(10, 10), wp2.position)
        assertEquals(Position(15, 15), wp2.nextTargetPosition)
        
        // Verify third waypoint
        val wp3 = level.waypoints[2]
        assertEquals(Position(15, 15), wp3.position)
        assertEquals(Position(20, 20), wp3.nextTargetPosition)
    }
    
    @Test
    fun testDeserializeLevelWithoutWaypointsBackwardCompatibility() {
        // Old format without waypoints field
        val json = """
{
  "id": "test_level",
  "mapId": "test_map",
  "title": "Test Level",
  "subtitle": "",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1}
  ],
  "availableTowers": ["SPIKE_TOWER"]
}
        """.trimIndent()
        
        val level = EditorJsonSerializer.deserializeLevel(json)
        
        assertNotNull(level, "Level should be deserialized for backward compatibility")
        assertEquals(0, level.waypoints.size, "Should have no waypoints")
    }
    
    @Test
    fun testSerializeDeserializeRoundTrip() {
        val waypoints = listOf(
            EditorWaypoint(Position(5, 5), Position(10, 10)),
            EditorWaypoint(Position(10, 10), Position(15, 15)),
            EditorWaypoint(Position(15, 15), Position(20, 20))
        )
        
        val originalLevel = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            waypoints = waypoints
        )
        
        // Serialize
        val json = EditorJsonSerializer.serializeLevel(originalLevel)
        
        // Deserialize
        val deserializedLevel = EditorJsonSerializer.deserializeLevel(json)
        
        assertNotNull(deserializedLevel, "Level should be deserialized")
        assertEquals(originalLevel.waypoints.size, deserializedLevel.waypoints.size, "Waypoint count should match")
        
        // Verify all waypoints match
        for (i in originalLevel.waypoints.indices) {
            assertEquals(
                originalLevel.waypoints[i].position,
                deserializedLevel.waypoints[i].position,
                "Waypoint $i position should match"
            )
            assertEquals(
                originalLevel.waypoints[i].nextTargetPosition,
                deserializedLevel.waypoints[i].nextTargetPosition,
                "Waypoint $i nextTargetPosition should match"
            )
        }
    }
}
