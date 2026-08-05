package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SplitBuildTowerButtonSerializationTest {
    private fun sampleLevel(splitBuildTowerButton: Boolean): EditorLevel =
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
            splitBuildTowerButton = splitBuildTowerButton,
        )

    @Test
    fun splitBuildTowerButtonDefaultsToTrueWhenAbsent() {
        val json = EditorJsonSerializer.serializeLevel(sampleLevel(splitBuildTowerButton = true))
        assertFalse(json.contains("splitBuildTowerButton"))

        val parsed = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(parsed)
        assertTrue(parsed.splitBuildTowerButton)
    }

    @Test
    fun splitBuildTowerButtonRoundTripsWhenFalse() {
        val json = EditorJsonSerializer.serializeLevel(sampleLevel(splitBuildTowerButton = false))
        assertTrue(json.contains("\"splitBuildTowerButton\": false"))

        val parsed = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(parsed)
        assertFalse(parsed.splitBuildTowerButton)
    }
}
