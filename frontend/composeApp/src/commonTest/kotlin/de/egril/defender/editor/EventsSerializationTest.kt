package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventsSerializationTest {
    private fun baseLevel(events: LevelEvents): EditorLevel =
        EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            events = events,
        )

    @Test
    fun testSerializeLevelWithEvents() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "e1",
                        condition = EventCondition(type = EventConditionType.TURN_START, fromTurn = 5),
                        actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
                        messageKey = "event_msg_reinforcements",
                    ),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))

        assertTrue(json.contains("\"events\""), "JSON should contain events field")
        assertTrue(json.contains("TURN_START"), "JSON should contain the condition type")
        assertTrue(json.contains("GIVE_COINS"), "JSON should contain the action type")
        assertTrue(json.contains("event_msg_reinforcements"), "JSON should contain the message key")
    }

    @Test
    fun testSerializeDeserializeRoundTrip() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "coins_low",
                        condition =
                            EventCondition(
                                type = EventConditionType.COINS_AT_OR_BELOW,
                                fromTurn = 5,
                                threshold = 50,
                            ),
                        actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
                        messageKey = "event_msg_treasure",
                        repeatable = true,
                    ),
                    LevelEvent(
                        id = "ork_wave",
                        condition =
                            EventCondition(
                                type = EventConditionType.ENEMY_TYPE_KILLED,
                                threshold = 3,
                                attackerType = AttackerType.ORK,
                            ),
                        actions =
                            listOf(
                                EventAction(
                                    type = EventActionType.GIVE_SUPPORT_OBJECT,
                                    amount = 2,
                                    supportObjectType = SupportObjectType.BARRICADE,
                                ),
                                EventAction(
                                    type = EventActionType.GIVE_SUPPORT_SPELL,
                                    amount = 1,
                                    spellType = SpellType.FREEZE_SPELL,
                                ),
                            ),
                    ),
                    LevelEvent(
                        id = "reach",
                        condition =
                            EventCondition(
                                type = EventConditionType.UNIT_REACHED,
                                position = Position(4, 7),
                            ),
                        actions = listOf(EventAction(type = EventActionType.DESTROY_MINE, position = Position(2, 3))),
                    ),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        val level = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(level, "Level should be deserialized")
        assertEquals(3, level.events.events.size, "Should have 3 events")

        val coinsLow = level.events.events.first { it.id == "coins_low" }
        assertEquals(EventConditionType.COINS_AT_OR_BELOW, coinsLow.condition.type)
        assertEquals(5, coinsLow.condition.fromTurn)
        assertEquals(50, coinsLow.condition.threshold)
        assertEquals("event_msg_treasure", coinsLow.messageKey)
        assertTrue(coinsLow.repeatable)
        assertEquals(EventActionType.GIVE_COINS, coinsLow.actions.first().type)
        assertEquals(50, coinsLow.actions.first().amount)

        val orkWave = level.events.events.first { it.id == "ork_wave" }
        assertEquals(AttackerType.ORK, orkWave.condition.attackerType)
        assertEquals(2, orkWave.actions.size)
        val objAction = orkWave.actions.first { it.type == EventActionType.GIVE_SUPPORT_OBJECT }
        assertEquals(SupportObjectType.BARRICADE, objAction.supportObjectType)
        assertEquals(2, objAction.amount)
        val spellAction = orkWave.actions.first { it.type == EventActionType.GIVE_SUPPORT_SPELL }
        assertEquals(SpellType.FREEZE_SPELL, spellAction.spellType)

        val reach = level.events.events.first { it.id == "reach" }
        assertEquals(Position(4, 7), reach.condition.position)
        val destroy = reach.actions.first()
        assertEquals(EventActionType.DESTROY_MINE, destroy.type)
        assertEquals(Position(2, 3), destroy.position)
    }

    @Test
    fun testDeserializeLevelWithoutEventsBackwardCompatibility() {
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
        assertTrue(level.events.isEmpty(), "Level should have no events")
    }
}
