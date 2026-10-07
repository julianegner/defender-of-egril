package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AltarTest {
    private val buildPosition = Position(2, 1)
    private val pathPosition = Position(2, 3)

    private fun state(events: LevelEvents = LevelEvents()): GameState =
        GameState(
            Level(
                id = 1,
                name = "Altar test",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 3)),
                targetPositions = listOf(Position(9, 3)),
                pathCells = (0..9).map { Position(it, 3) }.toSet(),
                buildAreas = setOf(buildPosition),
                riverTiles = mapOf(Position(3, 1) to RiverTile(Position(3, 1))),
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
                initialCoins = 10000,
                events = events,
            ),
        ).also {
            it.phase.value = GamePhase.PLAYER_TURN
            it.runes.value = 2
        }

    private fun wizard(
        state: GameState,
        level: Int = 10,
    ): Defender =
        Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = mutableStateOf(buildPosition),
            level = mutableStateOf(level),
            actionsRemaining = mutableStateOf(1),
        ).also { state.defenders.add(it) }

    @Test
    fun sanctifyingRequiresLevelTenAndConsumesExactlyOneRune() {
        for (level in listOf(10, 11, 20)) {
            val state = state()
            val wizard = wizard(state, level)
            wizard.trapCooldownRemaining.value = 4
            wizard.hasBeenUsed.value = true
            val manager = TowerManager(state)
            assertTrue(manager.sanctifyDefender(wizard.id))
            val altar = state.defenders.single()
            assertEquals(DefenderType.ALTAR, altar.type)
            assertEquals(wizard.id, altar.id)
            assertSame(wizard.position, altar.position)
            assertSame(wizard.level, altar.level)
            assertSame(wizard.actionsRemaining, altar.actionsRemaining)
            assertSame(wizard.trapCooldownRemaining, altar.trapCooldownRemaining)
            assertEquals(4, altar.trapCooldownRemaining.value)
            assertTrue(altar.hasBeenUsed.value)
            assertEquals(1, state.runes.value)
            assertFalse(manager.sanctifyDefender(wizard.id))
            assertEquals(1, state.runes.value)
        }
    }

    @Test
    fun sanctifyingRejectsLowLevelMissingRuneWrongTypeAndUnreadyTower() {
        val state = state()
        val wizard = wizard(state, 9)
        val manager = TowerManager(state)
        assertFalse(manager.sanctifyDefender(wizard.id))
        wizard.level.value = 10
        state.runes.value = 0
        assertFalse(manager.sanctifyDefender(wizard.id))
        state.runes.value = 2
        wizard.buildTimeRemaining.value = 1
        assertFalse(manager.sanctifyDefender(wizard.id))
        wizard.buildTimeRemaining.value = 0
        wizard.isDisabled.value = true
        assertFalse(manager.sanctifyDefender(wizard.id))
        wizard.isDisabled.value = false
        state.defenders[0] = wizard.copy(type = DefenderType.ALCHEMY_TOWER)
        assertFalse(manager.sanctifyDefender(wizard.id))
        assertEquals(2, state.runes.value)
    }

    @Test
    fun sanctifyingRejectsRaftsBargesAndTowerBasesOnNonBuildGround() {
        val state = state()
        val wizard = wizard(state)
        val manager = TowerManager(state)
        wizard.raftId.value = 1
        assertFalse(manager.sanctifyDefender(wizard.id))
        wizard.position.value = Position(3, 1)
        assertFalse(manager.sanctifyDefender(wizard.id))
        wizard.raftId.value = null
        wizard.towerBaseBarricadeId.value = 1
        for (position in listOf(pathPosition, Position(4, 4), Position(0, 3), Position(9, 3))) {
            wizard.position.value = position
            assertFalse(manager.sanctifyDefender(wizard.id))
        }
        wizard.position.value = buildPosition
        assertTrue(manager.sanctifyDefender(wizard.id))
    }

    @Test
    fun altarsCannotBePlacedUpgradedOrAttackEitherTargetsOrPositions() {
        val state = state()
        val wizard = wizard(state)
        val engine = GameEngine(state)
        assertFalse(engine.placeDefender(DefenderType.ALTAR, buildPosition))
        assertFalse(state.canPlaceDefender(DefenderType.ALTAR))
        assertTrue(engine.sanctifyDefender(wizard.id))
        val altar = state.defenders.single()
        assertFalse(engine.upgradeDefender(altar.id))
        val enemy = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(pathPosition))
        state.attackers.add(enemy)
        val initialHealth = enemy.currentHealth.value
        assertFalse(altar.canAttack(enemy))
        assertFalse(engine.defenderAttack(altar.id, enemy.id))
        assertFalse(engine.defenderAttackPosition(altar.id, pathPosition))
        assertEquals(initialHealth, enemy.currentHealth.value)
        assertEquals(1, altar.actionsRemaining.value)
        assertEquals(0, altar.damage)
        assertEquals(0, altar.previewAttackDamage())
    }

    @Test
    fun activationUsesAnAvailableActionOnlyOnPlayerTurnOncePerTurn() {
        val state = state()
        val wizard = wizard(state)
        val manager = TowerManager(state)
        assertTrue(manager.sanctifyDefender(wizard.id))
        val altar = state.defenders.single()
        state.phase.value = GamePhase.ENEMY_TURN
        assertFalse(manager.activateAltar(altar.id))
        state.phase.value = GamePhase.PLAYER_TURN
        altar.actionsRemaining.value = 0
        assertFalse(manager.activateAltar(altar.id))
        altar.actionsRemaining.value = 1
        altar.isDisabled.value = true
        assertFalse(manager.activateAltar(altar.id))
        altar.isDisabled.value = false
        assertTrue(manager.activateAltar(altar.id))
        assertTrue(altar.isChanneling.value)
        assertTrue(altar.hasBeenUsed.value)
        assertEquals(0, altar.actionsRemaining.value)
        altar.actionsRemaining.value = 2
        assertFalse(manager.activateAltar(altar.id))
        assertEquals(2, altar.actionsRemaining.value)
        altar.resetActions()
        assertFalse(altar.isChanneling.value)
        assertEquals(1, altar.actionsRemaining.value)
        assertTrue(manager.activateAltar(altar.id))
    }

    @Test
    fun altarsRetainMagicalTrapsAndShareTheirActionWithActivation() {
        val state = state()
        val wizard = wizard(state)
        val engine = GameEngine(state)
        assertTrue(engine.sanctifyDefender(wizard.id))
        val altar = state.defenders.single()
        assertTrue(state.canWizardPlaceMagicalTrapAt(altar, pathPosition))
        assertTrue(engine.performWizardPlaceMagicalTrap(altar.id, pathPosition))
        assertEquals(TrapType.MAGICAL, state.traps.single().type)
        assertEquals(altar.id, state.traps.single().defenderId)
        assertEquals(10, altar.trapCooldownRemaining.value)
        assertEquals(0, altar.actionsRemaining.value)
        assertFalse(engine.activateAltar(altar.id))
        altar.resetActions()
        altar.trapCooldownRemaining.value = 0
        assertTrue(engine.activateAltar(altar.id))
        assertFalse(engine.performWizardPlaceMagicalTrap(altar.id, Position(3, 3)))
        state.phase.value = GamePhase.ENEMY_TURN
        engine.completeEnemyTurn()
        assertTrue(engine.performWizardPlaceMagicalTrap(altar.id, Position(3, 3)))
    }

    @Test
    fun nextPlayerTurnResetsChannelingAndTicksMagicalTrapCooldown() {
        val state = state()
        val wizard = wizard(state)
        val engine = GameEngine(state)
        assertTrue(engine.sanctifyDefender(wizard.id))
        val altar = state.defenders.single()
        altar.trapCooldownRemaining.value = 5
        assertTrue(engine.activateAltar(altar.id))
        state.phase.value = GamePhase.ENEMY_TURN
        engine.completeEnemyTurn()
        assertEquals(GamePhase.PLAYER_TURN, state.phase.value)
        assertFalse(altar.isChanneling.value)
        assertEquals(1, altar.actionsRemaining.value)
        assertEquals(4, altar.trapCooldownRemaining.value)
    }

    @Test
    fun activationEvaluatesVictoryEventsImmediatelyWithEnemiesRemaining() {
        val state =
            state(
                LevelEvents(
                    listOf(
                        LevelEvent(
                            id = "altar-victory",
                            condition = EventCondition(EventConditionType.ALTARS_ACTIVATED, threshold = 1),
                            actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                        ),
                    ),
                ),
            )
        val wizard = wizard(state)
        state.attackers.add(Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(pathPosition)))
        val engine = GameEngine(state)
        assertTrue(engine.sanctifyDefender(wizard.id))
        assertFalse(state.isLevelWon())
        assertTrue(engine.activateAltar(wizard.id))
        assertTrue(state.scriptedVictory.value)
        assertTrue(state.isLevelWon())
        assertTrue("altar-victory" in state.triggeredEventIds)
        val enemy = state.attackers.single()
        assertFalse(enemy.isDefeated.value)
    }
}
