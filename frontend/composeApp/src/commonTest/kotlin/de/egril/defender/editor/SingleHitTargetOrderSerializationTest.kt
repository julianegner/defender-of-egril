package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SingleHitTargetOrderSerializationTest {
    @Test
    fun targetOrderSurvivesLevelJsonRoundTrip() {
        val order = listOf(Position(8, 3), Position(4, 1))
        val editorLevel =
            EditorLevel(
                id = "ordered_targets",
                mapId = "test_map",
                title = "Ordered Targets",
                startCoins = 100,
                enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
                singleHitTargetOrder = order,
            )

        val restored = EditorJsonSerializer.deserializeLevel(EditorJsonSerializer.serializeLevel(editorLevel))

        assertNotNull(restored)
        assertEquals(order, restored.singleHitTargetOrder)
    }

    @Test
    fun missingTargetOrderRemainsBackwardCompatible() {
        val json =
            """
{
  "id": "legacy",
  "mapId": "map",
  "title": "Legacy",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [],
  "availableTowers": ["SPIKE_TOWER"]
}
            """.trimIndent()

        val restored = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(restored)
        assertEquals(emptyList(), restored.singleHitTargetOrder)
    }
}
