package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObject
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.SupportSpell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SupportsSerializationTest {
    private fun baseLevel(supports: LevelSupports): EditorLevel =
        EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            supports = supports,
        )

    @Test
    fun testSerializeLevelWithSupports() {
        val supports =
            LevelSupports(
                objects =
                    listOf(
                        SupportObject(SupportObjectType.DWARVEN_TRAP, count = 2, damage = 15),
                        SupportObject(SupportObjectType.BARRICADE, count = 1, healthPoints = 80),
                    ),
                spells = listOf(SupportSpell(SpellType.FREEZE_SPELL, count = 3)),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(supports))

        assertTrue(json.contains("\"supports\""), "JSON should contain supports field")
        assertTrue(json.contains("\"objects\""), "JSON should contain objects array")
        assertTrue(json.contains("\"spells\""), "JSON should contain spells array")
        assertTrue(json.contains("DWARVEN_TRAP"), "JSON should contain the object type")
        assertTrue(json.contains("FREEZE_SPELL"), "JSON should contain the spell type")
    }

    @Test
    fun testSerializeDeserializeRoundTrip() {
        val supports =
            LevelSupports(
                objects =
                    listOf(
                        SupportObject(SupportObjectType.DWARVEN_TRAP, count = 2, damage = 15),
                        SupportObject(SupportObjectType.MAGICAL_TRAP, count = 1),
                        SupportObject(SupportObjectType.BARRICADE, count = 4, healthPoints = 80),
                    ),
                spells =
                    listOf(
                        SupportSpell(SpellType.FREEZE_SPELL, count = 3),
                        SupportSpell(SpellType.HEAL, count = 1),
                    ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(supports))
        val level = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(level, "Level should be deserialized")
        assertEquals(3, level.supports.objects.size, "Should have 3 support objects")
        assertEquals(2, level.supports.spells.size, "Should have 2 support spells")

        val dwarven = level.supports.objects.first { it.type == SupportObjectType.DWARVEN_TRAP }
        assertEquals(2, dwarven.count)
        assertEquals(15, dwarven.damage)

        val barricade = level.supports.objects.first { it.type == SupportObjectType.BARRICADE }
        assertEquals(4, barricade.count)
        assertEquals(80, barricade.healthPoints)

        val freeze = level.supports.spells.first { it.spell == SpellType.FREEZE_SPELL }
        assertEquals(3, freeze.count)
    }

    @Test
    fun testDeserializeLevelWithoutSupportsBackwardCompatibility() {
        val json =
            """
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
        assertTrue(level.supports.isEmpty(), "Level should have no supports")
    }
}
