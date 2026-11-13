package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutoTowerBaseSerializationTest {
    private fun baseLevel(
        autoTowerBase: Boolean,
        cost: Int,
    ) = EditorLevel(
        id = "auto_base_level",
        mapId = "test_map",
        title = "Auto Base",
        subtitle = "Testing",
        startCoins = 100,
        startHealthPoints = 10,
        enemySpawns = emptyList(),
        availableTowers = emptySet(),
        autoTowerBase = autoTowerBase,
        autoTowerBaseExtraCost = cost,
    )

    @Test
    fun testRoundTripPreservesAutoTowerBaseSettings() {
        val level = baseLevel(autoTowerBase = true, cost = 35)
        val deserialized = EditorJsonSerializer.deserializeLevel(EditorJsonSerializer.serializeLevel(level))

        assertNotNull(deserialized)
        assertTrue(deserialized.autoTowerBase)
        assertEquals(35, deserialized.autoTowerBaseExtraCost)
    }

    @Test
    fun testDefaultsAreOmittedAndRestored() {
        val level = baseLevel(autoTowerBase = false, cost = 20)
        val json = EditorJsonSerializer.serializeLevel(level)
        assertFalse(json.contains("autoTowerBase"), "Disabled option should not be written")

        val deserialized = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(deserialized)
        assertFalse(deserialized.autoTowerBase)
        assertEquals(20, deserialized.autoTowerBaseExtraCost)
    }
}
