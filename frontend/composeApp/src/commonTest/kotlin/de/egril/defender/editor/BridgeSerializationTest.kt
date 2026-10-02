package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.BridgeType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BridgeSerializationTest {
    @Test
    fun testRoundTripLevelWithInitialBridges() {
        val level =
            EditorLevel(
                id = "bridge_init_level",
                mapId = "test_map",
                title = "Bridge Init",
                startCoins = 100,
                startHealthPoints = 10,
                enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
                initialData =
                    InitialData(
                        bridges =
                            listOf(
                                InitialBridge(
                                    position = Position(2, 3),
                                    type = BridgeType.WOODEN,
                                    healthPoints = 50,
                                ),
                                InitialBridge(
                                    position = Position(4, 5),
                                    type = BridgeType.STONE,
                                    healthPoints = 100,
                                    isIndestructible = true,
                                ),
                            ),
                    ),
            )

        val json = EditorJsonSerializer.serializeLevel(level)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(deserialized)
        val bridges = deserialized.getEffectiveInitialData().bridges
        assertEquals(2, bridges.size)
        assertEquals(Position(2, 3), bridges[0].position)
        assertEquals(BridgeType.WOODEN, bridges[0].type)
        assertEquals(50, bridges[0].healthPoints)
        assertTrue(!bridges[0].isIndestructible)

        assertEquals(Position(4, 5), bridges[1].position)
        assertEquals(BridgeType.STONE, bridges[1].type)
        assertEquals(100, bridges[1].healthPoints)
        assertTrue(bridges[1].isIndestructible)
    }
}
