package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.FiefType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FiefSerializationTest {
    @Test
    fun testRoundTripLevelWithOnlyInitialFiefs() {
        val level =
            EditorLevel(
                id = "fief_only_level",
                mapId = "test_map",
                title = "Fief Only",
                startCoins = 100,
                startHealthPoints = 10,
                enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
                initialData =
                    InitialData(
                        fiefs =
                            listOf(
                                InitialFief(Position(2, 3), FiefType.FISHER),
                                InitialFief(Position(4, 5), FiefType.QUARRY),
                            ),
                    ),
            )

        val json = EditorJsonSerializer.serializeLevel(level)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(deserialized)
        val fiefs = deserialized.getEffectiveInitialData().fiefs
        assertEquals(2, fiefs.size)
        assertEquals(Position(2, 3), fiefs[0].position)
        assertEquals(FiefType.FISHER, fiefs[0].type)
        assertEquals(Position(4, 5), fiefs[1].position)
        assertEquals(FiefType.QUARRY, fiefs[1].type)
    }
}
