package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.AltarLink
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopStep
import de.egril.defender.model.EventMapImage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.INDEFINITE_SUPPORT_COUNT
import de.egril.defender.model.Level
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObject
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.SupportSpell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [EventScriptSystem]: scripted level events (conditions, actions, story messages).
 */
class EventScriptSystemTest {
    @Test
    fun altarThresholdCountsOnlySimultaneouslyChannelingReadyEnabledAltars() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "altars",
                                condition = EventCondition(EventConditionType.ALTARS_ACTIVATED, threshold = 2),
                                actions = listOf(EventAction(EventActionType.GIVE_COINS, amount = 25)),
                            ),
                        ),
                    ),
                ),
            )
        val first = Defender(id = 1, type = DefenderType.ALTAR, position = mutableStateOf(Position(2, 2)))
        val second = Defender(id = 2, type = DefenderType.ALTAR, position = mutableStateOf(Position(3, 2)))
        val inactive = Defender(id = 3, type = DefenderType.ALTAR, position = mutableStateOf(Position(4, 2)))
        val building = Defender(id = 4, type = DefenderType.ALTAR, position = mutableStateOf(Position(5, 2)))
        val disabled = Defender(id = 5, type = DefenderType.ALTAR, position = mutableStateOf(Position(6, 2)))
        val otherTower = Defender(id = 6, type = DefenderType.SPIKE_TOWER, position = mutableStateOf(Position(7, 2)))
        state.defenders.addAll(listOf(first, second, inactive, building, disabled, otherTower))
        first.isChanneling.value = true
        building.isChanneling.value = true
        building.buildTimeRemaining.value = 1
        disabled.isChanneling.value = true
        disabled.isDisabled.value = true
        otherTower.isChanneling.value = true
        val system = EventScriptSystem(state)
        val coins = state.coins.value
        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(coins, state.coins.value)

        // Activations on different turns do not accumulate.
        first.isChanneling.value = false
        second.isChanneling.value = true
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(coins, state.coins.value)

        first.isChanneling.value = true
        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(coins + 25, state.coins.value)
        assertTrue("altars" in state.triggeredEventIds)
        system.evaluate(EventTrigger.ENEMY_TURN_START)
        assertEquals(coins + 25, state.coins.value)
    }

    @Test
    fun altarVictoryFiresImmediatelyButRespectsFromTurn() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "victory",
                                condition = EventCondition(EventConditionType.ALTARS_ACTIVATED, fromTurn = 3, threshold = 1),
                                actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                                messageKey = "event_msg_rune_network_taken_over",
                            ),
                        ),
                    ),
                ),
            )
        val altar = Defender(id = 1, type = DefenderType.ALTAR, position = mutableStateOf(Position(2, 2)))
        altar.isChanneling.value = true
        state.defenders.add(altar)
        state.attackers.add(Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(0, 0))))
        val system = EventScriptSystem(state)
        state.turnNumber.value = 2
        system.evaluate(EventTrigger.IMMEDIATE)
        assertFalse(state.scriptedVictory.value)
        assertFalse(state.isLevelWon())
        assertTrue(state.pendingMessages.isEmpty())
        state.turnNumber.value = 3
        system.evaluate(EventTrigger.IMMEDIATE)
        assertTrue(state.scriptedVictory.value)
        assertTrue(state.isLevelWon())
        assertEquals("event_msg_rune_network_taken_over", state.pendingMessages.single().name)
        altar.isChanneling.value = false
        assertEquals(1, state.pendingMessages.single().eventMessageAmount)
        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(1, state.pendingMessages.size)
    }

    @Test
    fun winLevelActionWorksSilentlyFromLoopSteps() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "victory-loop",
                                condition = EventCondition(EventConditionType.TURN_START),
                                loop =
                                    EventLoop(
                                        repeatCount = 1,
                                        steps =
                                            listOf(
                                                EventLoopStep(
                                                    waitTurns = 1,
                                                    actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                    ),
                ),
            )
        state.attackers.add(Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(0, 0))))
        val system = EventScriptSystem(state)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertFalse(state.isLevelWon())
        state.turnNumber.value++
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.scriptedVictory.value)
        assertTrue(state.isLevelWon())
        assertTrue(state.pendingMessages.isEmpty())
    }

    @Test
    fun runeNetworkLoopMessageCapturesActivatedCountBeforeLaterReset() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "rune-message-loop",
                                condition = EventCondition(EventConditionType.TURN_START),
                                loop =
                                    EventLoop(
                                        repeatCount = 1,
                                        steps =
                                            listOf(
                                                EventLoopStep(
                                                    waitTurns = 0,
                                                    messageKey = "event_msg_rune_network_taken_over",
                                                ),
                                                EventLoopStep(waitTurns = 1),
                                            ),
                                    ),
                            ),
                        ),
                    ),
                ),
            )
        val altar =
            Defender(
                id = 1,
                type = DefenderType.ALTAR,
                position = mutableStateOf(Position(2, 2)),
                isChanneling = mutableStateOf(true),
            )
        state.defenders.add(altar)
        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)
        altar.isChanneling.value = false
        assertEquals(1, state.pendingMessages.single().eventMessageAmount)
    }

    @Test
    fun mapImagesAppearOnlyWhenTriggeredAndRemainUntilHidden() {
        val image = EventMapImage("kraken", "kraken.png", 2.5f, 3f, 4f, 2f)
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "show",
                                condition = EventCondition(EventConditionType.TURN_START, fromTurn = 2),
                                actions = listOf(EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = image)),
                            ),
                            LevelEvent(
                                id = "hide",
                                condition = EventCondition(EventConditionType.TURN_START, fromTurn = 4),
                                actions = listOf(EventAction(EventActionType.HIDE_MAP_IMAGE, imageId = image.id)),
                            ),
                        ),
                    ),
                ),
            )
        val system = EventScriptSystem(state)
        state.turnNumber.value = 1
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeEventMapImages.isEmpty())
        state.turnNumber.value = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(listOf(image), state.activeEventMapImages.toList())
        state.turnNumber.value = 3
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(listOf(image), state.activeEventMapImages.toList())
        state.turnNumber.value = 4
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeEventMapImages.isEmpty())
    }

    @Test
    fun showingSameImageIdReplacesGeometryAndHideLeavesOtherImagesVisible() {
        val first = EventMapImage("first", "first.png")
        val second = EventMapImage("second", "second.png")
        val replaced = first.copy(x = -1.5f, width = 3.5f)
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "images",
                                condition = EventCondition(EventConditionType.TURN_START),
                                actions =
                                    listOf(
                                        EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = first),
                                        EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = second),
                                        EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = replaced),
                                        EventAction(EventActionType.HIDE_MAP_IMAGE, imageId = "second"),
                                        EventAction(EventActionType.HIDE_MAP_IMAGE, imageId = "absent"),
                                    ),
                            ),
                        ),
                    ),
                ),
            )
        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(listOf(replaced), state.activeEventMapImages.toList())
    }

    @Test
    fun invalidMapImagesAreNotActivated() {
        val invalid =
            listOf(
                EventMapImage("", "test.png"),
                EventMapImage("test", "../test.png"),
                EventMapImage("test", "test.png", width = 0f),
                EventMapImage("test", "test.png", height = -1f),
                EventMapImage("test", "test.png", x = Float.NaN),
                EventMapImage("test", "test.png", width = Float.POSITIVE_INFINITY),
            )
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "invalid",
                                condition = EventCondition(EventConditionType.TURN_START),
                                actions = invalid.map { EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = it) },
                            ),
                        ),
                    ),
                ),
            )
        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeEventMapImages.isEmpty())
    }

    @Test
    fun eventLoopCanShowAndHideMapImages() {
        val image = EventMapImage("loop_image", "image.png")
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "loop",
                                condition = EventCondition(EventConditionType.TURN_START),
                                loop =
                                    EventLoop(
                                        repeatCount = 1,
                                        steps =
                                            listOf(
                                                EventLoopStep(waitTurns = 0, actions = listOf(EventAction(EventActionType.SHOW_MAP_IMAGE, mapImage = image))),
                                                EventLoopStep(waitTurns = 1, actions = listOf(EventAction(EventActionType.HIDE_MAP_IMAGE, imageId = image.id))),
                                            ),
                                    ),
                            ),
                        ),
                    ),
                ),
            )
        val system = EventScriptSystem(state)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(listOf(image), state.activeEventMapImages.toList())
        state.turnNumber.value++
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeEventMapImages.isEmpty())
    }

    @Test
    fun altarLinkConnectsDesignatedAltarsOnlyWhenBothAreActivated() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "show",
                                condition = EventCondition(EventConditionType.TURN_START),
                                repeatable = true,
                                actions =
                                    listOf(
                                        EventAction(
                                            EventActionType.SHOW_ALTAR_LINK,
                                            linkId = "ab",
                                            altarFrom = Position(2, 2),
                                            altarTo = Position(3, 2),
                                        ),
                                    ),
                            ),
                            LevelEvent(
                                id = "hide",
                                condition = EventCondition(EventConditionType.TURN_START, fromTurn = 3),
                                actions = listOf(EventAction(EventActionType.HIDE_ALTAR_LINK, linkId = "ab")),
                            ),
                        ),
                    ),
                ),
            )
        val first = Defender(id = 1, type = DefenderType.ALTAR, position = mutableStateOf(Position(2, 2)))
        val second = Defender(id = 2, type = DefenderType.ALTAR, position = mutableStateOf(Position(3, 2)))
        val other = Defender(id = 3, type = DefenderType.ALTAR, position = mutableStateOf(Position(5, 5)))
        state.defenders.addAll(listOf(first, second, other))
        val system = EventScriptSystem(state)

        // Only the designated first altar is active: no line.
        first.isChanneling.value = true
        state.turnNumber.value = 1
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeAltarLinks.isEmpty())

        // An unrelated altar does not complete the designated pair.
        other.isChanneling.value = true
        state.turnNumber.value = 1
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeAltarLinks.isEmpty())

        second.isChanneling.value = true
        state.turnNumber.value = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(listOf(AltarLink("ab", Position(2, 2), Position(3, 2))), state.activeAltarLinks.toList())

        // Showing the same id again replaces the existing line instead of adding a second one.
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(1, state.activeAltarLinks.size)

        state.turnNumber.value = 3
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeAltarLinks.isEmpty())
    }

    @Test
    fun altarLinkInvalidWithoutTwoDistinctTiles() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "show",
                                condition = EventCondition(EventConditionType.TURN_START),
                                actions =
                                    listOf(
                                        EventAction(EventActionType.SHOW_ALTAR_LINK, linkId = "same", altarFrom = Position(2, 2), altarTo = Position(2, 2)),
                                        EventAction(EventActionType.SHOW_ALTAR_LINK, linkId = "missing", altarFrom = Position(2, 2)),
                                    ),
                            ),
                        ),
                    ),
                ),
            )
        state.defenders.add(Defender(id = 1, type = DefenderType.ALTAR, position = mutableStateOf(Position(2, 2))).also { it.isChanneling.value = true })
        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeAltarLinks.isEmpty())
    }

    @Test
    fun sameLinkIdWithDifferentPairsShowsSeveralLinesAtOnce() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "links",
                                condition = EventCondition(EventConditionType.TURN_START),
                                repeatable = true,
                                actions =
                                    listOf(
                                        EventAction(EventActionType.SHOW_ALTAR_LINK, linkId = "altar_link", altarFrom = Position(2, 2), altarTo = Position(3, 2)),
                                        EventAction(EventActionType.SHOW_ALTAR_LINK, linkId = "altar_link", altarFrom = Position(3, 2), altarTo = Position(2, 4)),
                                    ),
                            ),
                            LevelEvent(
                                id = "hide",
                                condition = EventCondition(EventConditionType.TURN_START, fromTurn = 2),
                                actions = listOf(EventAction(EventActionType.HIDE_ALTAR_LINK, linkId = "altar_link")),
                            ),
                        ),
                    ),
                ),
            )
        listOf(Position(2, 2), Position(3, 2), Position(2, 4)).forEachIndexed { i, pos ->
            state.defenders.add(Defender(id = i + 1, type = DefenderType.ALTAR, position = mutableStateOf(pos)).also { it.isChanneling.value = true })
        }
        val system = EventScriptSystem(state)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(2, state.activeAltarLinks.size)

        // Showing the same pair again (in either order) does not add a duplicate line.
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(2, state.activeAltarLinks.size)

        state.turnNumber.value = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.activeAltarLinks.isEmpty())
    }

    @Test
    fun oneAltarCanBeLinkedToSeveralAltars() {
        val state =
            GameState(
                createLevel(
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "links",
                                condition = EventCondition(EventConditionType.TURN_START),
                                actions =
                                    listOf(
                                        EventAction(EventActionType.SHOW_ALTAR_LINK, linkId = "ab", altarFrom = Position(2, 2), altarTo = Position(3, 2)),
                                        EventAction(EventActionType.SHOW_ALTAR_LINK, linkId = "ac", altarFrom = Position(2, 2), altarTo = Position(2, 4)),
                                    ),
                            ),
                        ),
                    ),
                ),
            )
        listOf(Position(2, 2), Position(3, 2), Position(2, 4)).forEachIndexed { i, pos ->
            state.defenders.add(Defender(id = i + 1, type = DefenderType.ALTAR, position = mutableStateOf(pos)).also { it.isChanneling.value = true })
        }
        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(
            listOf(
                AltarLink("ab", Position(2, 2), Position(3, 2)),
                AltarLink("ac", Position(2, 2), Position(2, 4)),
            ),
            state.activeAltarLinks.toList(),
        )
    }

    private fun createLevel(events: LevelEvents): Level =
        Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = (0..5).map { Position(it, 0) }.toSet(),
            buildAreas = setOf(Position(2, 2), Position(3, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            events = events,
        )

    @Test
    fun testTurnStartConditionFires() {
        val event =
            LevelEvent(
                id = "e1",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        // Enemy trigger should NOT fire a player-turn-start event.
        system.evaluate(EventTrigger.ENEMY_TURN_START)
        assertEquals(100, state.coins.value, "Enemy turn should not fire a TURN_START event")

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(150, state.coins.value, "Player turn start should grant coins")
    }

    @Test
    fun testNonRepeatableEventFiresOnce() {
        val event =
            LevelEvent(
                id = "once",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 10)),
                repeatable = false,
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(110, state.coins.value, "Non-repeatable event should fire only once")
        assertTrue(state.triggeredEventIds.contains("once"))
    }

    @Test
    fun testRepeatableEventFiresEachTime() {
        val event =
            LevelEvent(
                id = "rep",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 10)),
                repeatable = true,
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(120, state.coins.value, "Repeatable event should fire every time")
    }

    @Test
    fun testFromTurnGate() {
        val event =
            LevelEvent(
                id = "gated",
                condition = EventCondition(type = EventConditionType.COINS_AT_OR_BELOW, fromTurn = 5, threshold = 50),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.coins.value = 20
        val system = EventScriptSystem(state)

        state.turnNumber.value = 3
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(20, state.coins.value, "Event should not fire before its fromTurn gate")

        state.turnNumber.value = 5
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(70, state.coins.value, "Event should fire once the fromTurn gate is reached")
    }

    @Test
    fun testEnemiesKilledCondition() {
        val event =
            LevelEvent(
                id = "kills",
                condition = EventCondition(type = EventConditionType.ENEMIES_KILLED, threshold = 3),
                actions = listOf(EventAction(type = EventActionType.GIVE_MANA, amount = 5)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        state.maxMana.value = 100
        val system = EventScriptSystem(state)

        state.enemiesKilledTotal.value = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(0, state.currentMana.value, "Event should not fire below the kill threshold")

        state.enemiesKilledTotal.value = 3
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(5, state.currentMana.value, "Event should fire once the kill threshold is met")
    }

    @Test
    fun testEnemyTypeKilledCondition() {
        val event =
            LevelEvent(
                id = "orkkills",
                condition =
                    EventCondition(
                        type = EventConditionType.ENEMY_TYPE_KILLED,
                        threshold = 2,
                        attackerType = AttackerType.ORK,
                    ),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 25)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        state.enemiesKilledByType[AttackerType.GOBLIN] = 5
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(100, state.coins.value, "Kills of a different type should not count")

        state.enemiesKilledByType[AttackerType.ORK] = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(125, state.coins.value, "Reaching the type-specific kill threshold should fire the event")
    }

    @Test
    fun testUnitReachedCondition() {
        val target = Position(3, 0)
        val event =
            LevelEvent(
                id = "reached",
                condition = EventCondition(type = EventConditionType.UNIT_REACHED, position = target),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 30)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        val attacker = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(1, 0)))
        state.attackers.add(attacker)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(100, state.coins.value, "Event should not fire while no unit is on the target tile")

        attacker.position.value = target
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(130, state.coins.value, "Event should fire when a unit reaches the target tile")
    }

    @Test
    fun testUnitReachedConditionIsTypeSpecificWhenTypeSet() {
        val target = Position(3, 0)
        val event =
            LevelEvent(
                id = "reached_ork",
                condition =
                    EventCondition(
                        type = EventConditionType.UNIT_REACHED,
                        position = target,
                        attackerType = AttackerType.ORK,
                    ),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 30)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        // A goblin on the target tile must NOT fire an ork-specific UNIT_REACHED event.
        val goblin = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(target))
        state.attackers.add(goblin)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(100, state.coins.value, "A different unit type on the tile should not fire a type-specific event")

        // An ork on the target tile fires it.
        val ork = Attacker(id = 2, type = AttackerType.ORK, position = mutableStateOf(target))
        state.attackers.add(ork)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(130, state.coins.value, "The specified unit type reaching the tile should fire the event")
    }

    @Test
    fun testGiveSupportActions() {
        val event =
            LevelEvent(
                id = "support",
                condition = EventCondition(type = EventConditionType.TURN_START),
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
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(2, state.supportObjectsRemaining[SupportObjectType.BARRICADE])
        assertEquals(1, state.supportSpellsRemaining[SpellType.FREEZE_SPELL])
    }

    @Test
    fun testDestroyMineAction() {
        val minePos = Position(2, 2)
        val event =
            LevelEvent(
                id = "destroy",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.DESTROY_MINE, position = minePos)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val mine = Defender(id = 1, type = DefenderType.DWARVEN_MINE, position = mutableStateOf(minePos))
        state.defenders.add(mine)
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertFalse(state.defenders.contains(mine), "Mine should be removed")
        assertTrue(state.destroyedMinePositions.contains(minePos), "Destroyed mine position should be recorded")
    }

    @Test
    fun testGiveSupportActionsFallBackToFirstEntryWhenTypeMissing() {
        // Events authored before a type was persisted (null type) should still grant a support,
        // matching the first entry the editor displayed as selected.
        val event =
            LevelEvent(
                id = "support_no_type",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions =
                    listOf(
                        EventAction(type = EventActionType.GIVE_SUPPORT_OBJECT, amount = 1),
                        EventAction(type = EventActionType.GIVE_SUPPORT_SPELL, amount = 1),
                    ),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(1, state.supportObjectsRemaining[SupportObjectType.entries.first()])
        assertEquals(1, state.supportSpellsRemaining[SpellType.entries.first()])
    }

    @Test
    fun testGiveSupportActionsPreserveIndefiniteCounts() {
        val event =
            LevelEvent(
                id = "support_infinite",
                condition = EventCondition(type = EventConditionType.TURN_START),
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
            )
        val state =
            GameState(
                createLevel(LevelEvents(listOf(event))).copy(
                    supports =
                        LevelSupports(
                            objects =
                                listOf(
                                    SupportObject(
                                        type = SupportObjectType.BARRICADE,
                                        count = INDEFINITE_SUPPORT_COUNT,
                                    ),
                                ),
                            spells =
                                listOf(
                                    SupportSpell(
                                        spell = SpellType.FREEZE_SPELL,
                                        count = INDEFINITE_SUPPORT_COUNT,
                                    ),
                                ),
                        ),
                ),
            )
        state.initializePrePlacedElements()
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(INDEFINITE_SUPPORT_COUNT, state.supportObjectsRemaining[SupportObjectType.BARRICADE])
        assertEquals(INDEFINITE_SUPPORT_COUNT, state.supportSpellsRemaining[SpellType.FREEZE_SPELL])
    }

    @Test
    fun testMessageIsQueued() {
        val event =
            LevelEvent(
                id = "msg",
                condition = EventCondition(type = EventConditionType.TURN_START),
                messageKey = "event_msg_reinforcements",
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(1, state.pendingMessages.size, "A message should be queued")
        assertEquals(GameMessageType.EVENT_MESSAGE, state.pendingMessages.first().type)
        assertEquals("event_msg_reinforcements", state.pendingMessages.first().name)
    }

    @Test
    fun testNoMessageIsQueuedWithoutMessageKey() {
        val event =
            LevelEvent(
                id = "no_msg",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
                messageKey = null,
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.pendingMessages.isEmpty(), "'No message' events must not show a popup")
        assertEquals(150, state.coins.value, "The actions are still applied")
    }

    @Test
    fun testMessageFrameIsCarriedOnMessage() {
        val event =
            LevelEvent(
                id = "framed",
                condition = EventCondition(type = EventConditionType.TURN_START),
                messageKey = "event_msg_high_tide",
                messageFrame = "kraken",
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals("kraken", state.pendingMessages.first().eventMessageFrame)
    }

    @Test
    fun testLoopStepWithoutMessageKeyShowsNoPopup() {
        val event =
            LevelEvent(
                id = "loop_no_msg",
                condition = EventCondition(type = EventConditionType.TURN_START),
                messageKey = null,
                loop =
                    EventLoop(
                        repeatCount = 1,
                        steps =
                            listOf(
                                EventLoopStep(
                                    waitTurns = 0,
                                    actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 10)),
                                    messageKey = null,
                                ),
                                // A loop needs at least one waiting step per pass to be valid.
                                EventLoopStep(waitTurns = 3),
                            ),
                    ),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertTrue(state.pendingMessages.isEmpty(), "Silent loop steps must not show a popup")
        assertEquals(110, state.coins.value, "The loop step actions are still applied")
    }

    @Test
    fun testLoopStepMessageUsesItsFrame() {
        val event =
            LevelEvent(
                id = "loop_msg",
                condition = EventCondition(type = EventConditionType.TURN_START),
                messageKey = null,
                loop =
                    EventLoop(
                        repeatCount = 1,
                        steps =
                            listOf(
                                EventLoopStep(
                                    waitTurns = 0,
                                    messageKey = "event_msg_low_tide",
                                    messageFrame = "sylvanas",
                                ),
                                EventLoopStep(waitTurns = 3),
                            ),
                    ),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(1, state.pendingMessages.size)
        val message = state.pendingMessages.first()
        assertEquals("event_msg_low_tide", message.name)
        assertEquals("sylvanas", message.eventMessageFrame)
    }

    @Test
    fun testImmediateTriggerFiresThresholdConditions() {
        val killEvent =
            LevelEvent(
                id = "kills",
                condition = EventCondition(type = EventConditionType.ENEMIES_KILLED, threshold = 5),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val coinsEvent =
            LevelEvent(
                id = "lowcoins",
                condition = EventCondition(type = EventConditionType.COINS_AT_OR_BELOW, threshold = 30),
                actions = listOf(EventAction(type = EventActionType.GIVE_MANA, amount = 5)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(coinsEvent, killEvent))))
        state.turnNumber.value = 1
        state.coins.value = 20
        state.maxMana.value = 100
        val system = EventScriptSystem(state)

        // Threshold-based events fire mid-turn on the IMMEDIATE trigger.
        state.enemiesKilledTotal.value = 5
        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(70, state.coins.value, "Enemies-killed event should fire immediately")
        assertEquals(5, state.currentMana.value, "Coins-at-or-below event should fire immediately")
    }

    @Test
    fun testImmediateTriggerDoesNotFireTurnStartConditions() {
        val event =
            LevelEvent(
                id = "turnstart",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(100, state.coins.value, "Turn-start events must not fire on the IMMEDIATE trigger")
    }

    @Test
    fun testImmediateTriggerDoesNotFireEnemyTurnStartConditions() {
        val event =
            LevelEvent(
                id = "enemyturnstart",
                condition = EventCondition(type = EventConditionType.ENEMY_TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(100, state.coins.value, "Enemy-turn-start events must not fire on the IMMEDIATE trigger")
    }
}
