package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that the [EditorLevel.isSandbox] flag survives a JSON serialize/deserialize round-trip.
 */
class SandboxLevelSerializationTest {
    private fun sampleLevel(isSandbox: Boolean): EditorLevel =
        EditorLevel(
            id = "sandbox_test",
            mapId = "test_map",
            title = "Sandbox Test",
            startCoins = 100,
            enemySpawns =
                listOf(
                    EditorEnemySpawn(attackerType = AttackerType.GOBLIN, level = 1, spawnTurn = 1),
                ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            isSandbox = isSandbox,
        )

    @Test
    fun sandboxFlagRoundTripsWhenTrue() {
        val json = EditorJsonSerializer.serializeLevel(sampleLevel(isSandbox = true))
        assertTrue(json.contains("\"isSandbox\": true"))
        val parsed = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(parsed)
        assertTrue(parsed.isSandbox)
    }

    @Test
    fun sandboxFlagDefaultsToFalseWhenAbsent() {
        val json = EditorJsonSerializer.serializeLevel(sampleLevel(isSandbox = false))
        assertFalse(json.contains("isSandbox"))
        val parsed = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(parsed)
        assertFalse(parsed.isSandbox)
    }
}
