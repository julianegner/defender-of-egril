package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopStep
import de.egril.defender.model.EventMapImage
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventsSerializationTest {
    @Test
    fun altarVictoryEventRoundTripsWithoutAdditionalSerializerFields() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "rune-network",
                        condition = EventCondition(EventConditionType.ALTARS_ACTIVATED, fromTurn = 2, threshold = 4),
                        actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                        messageKey = "event_msg_rune_network_taken_over",
                    ),
                ),
            )
        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        assertTrue(json.contains("\"type\": \"ALTARS_ACTIVATED\""))
        assertTrue(json.contains("\"type\": \"WIN_LEVEL\""))
        assertEquals(events, assertNotNull(EditorJsonSerializer.deserializeLevel(json)).events)
    }

    @Test
    fun mapImageActionsRoundTripThroughEventsAndNestedLoops() {
        val image = EventMapImage("image \"one\", {sea}[0]\\", "tide \"high\", {sea}[0].png", -1.25f, -0.5f, 2.75f, 1.5f)
        val actions =
            listOf(
                EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = image),
                EventAction(EventActionType.HIDE_MAP_IMAGE, imageId = image.id),
            )
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "tide",
                        condition = EventCondition(EventConditionType.TURN_START),
                        actions = actions,
                        loop =
                            EventLoop(
                                repeatCount = 2,
                                steps =
                                    listOf(
                                        EventLoopStep(
                                            waitTurns = 3,
                                            actions = actions,
                                            nestedLoop = EventLoop(steps = listOf(EventLoopStep(actions = actions))),
                                        ),
                                    ),
                            ),
                    ),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        assertTrue(json.contains("\"mapImage\": {"))
        assertTrue(json.contains("\"imageId\":"))
        val loaded = assertNotNull(EditorJsonSerializer.deserializeLevel(json))
        assertEquals(events, loaded.events)
    }

    @Test
    fun oldActionsHaveNoMapImageFields() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "coins",
                        condition = EventCondition(EventConditionType.TURN_START),
                        actions = listOf(EventAction(EventActionType.GIVE_COINS, amount = 50)),
                    ),
                ),
            )
        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        assertTrue(!json.contains("\"mapImage\""))
        assertTrue(!json.contains("\"imageId\""))
        val action =
            assertNotNull(EditorJsonSerializer.deserializeLevel(json))
                .events.events
                .single()
                .actions
                .single()
        assertNull(action.mapImage)
        assertNull(action.imageId)
        assertEquals(50, action.amount)
    }

    @Test
    fun imageGeometryDefaultsAndScientificNotationAreSupported() {
        val level =
            baseLevel(
                LevelEvents(
                    listOf(
                        LevelEvent(
                            id = "image",
                            condition = EventCondition(EventConditionType.TURN_START),
                            actions = listOf(EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = EventMapImage("sea", "sea.png"))),
                        ),
                    ),
                ),
            )
        val json =
            EditorJsonSerializer
                .serializeLevel(level)
                .replace("\"x\": 0.0, \"y\": 0.0, \"width\": 1.0, \"height\": 1.0", "\"x\": -1.25e1, \"width\": 2.5e-1")
        val image =
            assertNotNull(
                assertNotNull(EditorJsonSerializer.deserializeLevel(json))
                    .events.events
                    .single()
                    .actions
                    .single()
                    .mapImage,
            )
        assertEquals(EventMapImage("sea", "sea.png", -12.5f, 0f, 0.25f, 1f), image)
    }

    @Test
    fun invalidMapImageGeometryIsIgnoredWithoutDroppingOtherActions() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "image",
                        condition = EventCondition(EventConditionType.TURN_START),
                        actions =
                            listOf(
                                EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = EventMapImage("sea", "sea.png")),
                                EventAction(EventActionType.GIVE_COINS, amount = 10),
                            ),
                    ),
                ),
            )
        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        for (invalid in listOf("0", "-2.5", "NaN", "1e100")) {
            val loaded = assertNotNull(EditorJsonSerializer.deserializeLevel(json.replace("\"width\": 1.0", "\"width\": $invalid")))
            assertNull(
                loaded.events.events
                    .single()
                    .actions
                    .first()
                    .mapImage,
            )
            assertEquals(
                10,
                loaded.events.events
                    .single()
                    .actions
                    .last()
                    .amount,
            )
        }
    }

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

    @Test
    fun testMessageFrameRoundTrip() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "tide",
                        condition = EventCondition(type = EventConditionType.TURN_START, fromTurn = 3),
                        messageKey = "event_msg_high_tide",
                        messageFrame = "kraken",
                        loop =
                            EventLoop(
                                repeatCount = 0,
                                steps =
                                    listOf(
                                        EventLoopStep(
                                            waitTurns = 3,
                                            messageKey = "event_msg_low_tide",
                                            messageFrame = "sylvanas",
                                        ),
                                    ),
                            ),
                    ),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        assertTrue(json.contains("\"messageFrame\": \"kraken\""), "JSON should contain the event frame")
        assertTrue(json.contains("\"messageFrame\": \"sylvanas\""), "JSON should contain the loop step frame")

        val level = assertNotNull(EditorJsonSerializer.deserializeLevel(json))
        val event = level.events.events.first { it.id == "tide" }
        assertEquals("kraken", event.messageFrame)
        assertEquals("sylvanas", assertNotNull(event.loop).steps.first().messageFrame)
    }

    @Test
    fun testStandardMessageFrameIsNotSerializedAndParsesAsNull() {
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "plain",
                        condition = EventCondition(type = EventConditionType.TURN_START),
                        messageKey = "event_msg_reinforcements",
                        loop = EventLoop(repeatCount = 1, steps = listOf(EventLoopStep(waitTurns = 1))),
                    ),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(events))
        assertTrue(!json.contains("messageFrame"), "The standard frame is omitted to keep level files unchanged")

        // Levels authored before frames were selectable parse as the standard frame (null).
        val level = assertNotNull(EditorJsonSerializer.deserializeLevel(json))
        val event = level.events.events.first { it.id == "plain" }
        assertNull(event.messageFrame)
        assertNull(assertNotNull(event.loop).steps.first().messageFrame)
    }
}
