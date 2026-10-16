package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SplitBuildTowerButtonSerializationTest {
    private fun sampleLevel(): EditorLevel =
        EditorLevel(
            id = "split_button_test",
            mapId = "test_map",
            title = "Split Button Test",
            startCoins = 100,
            enemySpawns =
                listOf(
                    EditorEnemySpawn(attackerType = AttackerType.GOBLIN, level = 1, spawnTurn = 1),
                ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
        )

    @Test
    fun splitBuildTowerButtonNotSerializedInLevelJson() {
        val json = EditorJsonSerializer.serializeLevel(sampleLevel())
        assertFalse(json.contains("splitBuildTowerButton"))
    }

    @Test
    fun levelWithLegacySplitBuildTowerButtonFieldDeserializesCorrectly() {
        // Verify that a JSON with legacy splitBuildTowerButton field can still be parsed (backward compat)
        val json =
            EditorJsonSerializer
                .serializeLevel(sampleLevel())
                .replace(
                    "\"prerequisites\": []",
                    "\"prerequisites\": [],\n  \"splitBuildTowerButton\": false",
                )
        val parsed = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(parsed)
        assertEquals("split_button_test", parsed.id)
    }
}
