package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AutoAttackAvailability
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that wizard towers auto-generate mana during auto-attack when:
 * - there are no enemies in range, AND
 * - the wizard cannot place a magical trap.
 *
 * When the wizard CAN place a magical trap, mana is NOT auto-generated and the wizard
 * should appear in the "special actions remaining" warning list instead.
 */
class WizardAutoManaTest {
    private fun createLevel(): Level {
        val allCells = (0 until 10).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Wizard Auto Mana Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 5)),
            targetPositions = listOf(Position(9, 5)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun createLargeOpenLevel(): Level {
        val allCells = (0 until 30).flatMap { x -> (0 until 30).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 2,
            name = "Wizard Auto Mana Large Test",
            gridWidth = 30,
            gridHeight = 30,
            startPositions = listOf(Position(0, 15)),
            targetPositions = listOf(Position(29, 15)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun wizard(
        id: Int,
        position: Position,
        actions: Int = 1,
        level: Int = 1,
        trapCooldown: Int = 1,
    ) = Defender(
        id = id,
        type = DefenderType.WIZARD_TOWER,
        position = mutableStateOf(position),
        actionsRemaining = mutableStateOf(actions),
        buildTimeRemaining = mutableStateOf(0),
        level = mutableStateOf(level),
        trapCooldownRemaining = mutableStateOf(trapCooldown),
    )

    private fun goblin(
        id: Int,
        position: Position,
    ) = Attacker(id, AttackerType.GOBLIN, mutableStateOf(position), mutableStateOf(20))

    // -----------------------------------------------------------------------

    @Test
    fun wizardAutoGeneratesManaWhenNoEnemiesInRangeAndCannotPlaceTrap() {
        val level = createLevel()
        val state = GameState(level, currentMana = mutableStateOf(0), maxMana = mutableStateOf(50))
        val engine = GameEngine(state)

        // Wizard at (5,5), no enemies anywhere — trapCooldown > 0 so cannot place trap
        val wiz = wizard(id = 1, position = Position(5, 5), trapCooldown = 3)
        state.defenders.add(wiz)
        // One enemy far outside wizard range
        val far = goblin(id = 1, position = Position(0, 0))
        state.attackers.add(far)

        val manaBefore = state.currentMana.value
        state.phase.value = GamePhase.PLAYER_TURN
        engine.autoDefenderAttacks()

        assertTrue(state.currentMana.value > manaBefore, "Mana should increase when wizard auto-generates with no enemies in range")
        assertEquals(0, wiz.actionsRemaining.value, "Wizard action should be consumed")
    }

    @Test
    fun wizardAutoGeneratesManaWhenNoEnemiesAreAlive() {
        val level = createLevel()
        val state = GameState(level, currentMana = mutableStateOf(0), maxMana = mutableStateOf(50))
        val engine = GameEngine(state)

        val wiz = wizard(id = 1, position = Position(5, 5), trapCooldown = 3)
        state.defenders.add(wiz)

        assertEquals(
            AutoAttackAvailability.MANA_ONLY,
            state.getAutoAttackAvailability(),
            "Auto-action should be available as mana-only when no enemies are alive",
        )
        assertTrue(state.hasDefendersForAutoAttack(), "Mana-only auto-action should still enable the button state")

        val manaBefore = state.currentMana.value
        state.phase.value = GamePhase.PLAYER_TURN
        engine.autoDefenderAttacks()

        assertTrue(state.currentMana.value > manaBefore, "Mana should increase even without any living enemies")
        assertEquals(0, wiz.actionsRemaining.value, "Wizard action should be consumed")
    }

    @Test
    fun wizardManaOnlyActionCountsAsUnusedActionForEndTurnConfirmation() {
        val level = createLevel()
        val state = GameState(level, currentMana = mutableStateOf(0), maxMana = mutableStateOf(50))
        val wiz = wizard(id = 1, position = Position(5, 5), trapCooldown = 3)
        state.defenders.add(wiz)

        assertTrue(
            state.hasDefendersWithUnusedActions(),
            "Wizard mana generation must trigger end-turn confirmation when no enemy is attackable",
        )
    }

    @Test
    fun wizardDoesNotAutoGenerateManaWhenManaIsFull() {
        val level = createLevel()
        val state = GameState(level, currentMana = mutableStateOf(50), maxMana = mutableStateOf(50))
        val engine = GameEngine(state)

        val wiz = wizard(id = 1, position = Position(5, 5), trapCooldown = 3)
        state.defenders.add(wiz)
        val far = goblin(id = 1, position = Position(0, 0))
        state.attackers.add(far)

        val actionsBefore = wiz.actionsRemaining.value
        state.phase.value = GamePhase.PLAYER_TURN
        engine.autoDefenderAttacks()

        // Mana stays full; action is NOT consumed because generate-mana returned false
        assertEquals(50, state.currentMana.value, "Mana should stay at max")
        assertEquals(actionsBefore, wiz.actionsRemaining.value, "Action should not be consumed when mana is already full")
    }

    @Test
    fun wizardAutoGeneratesManaEvenWhenItCanPlaceMagicalTrap() {
        val level = createLargeOpenLevel()
        val state = GameState(level, currentMana = mutableStateOf(0), maxMana = mutableStateOf(50))
        val engine = GameEngine(state)

        // Level 10+ wizard with trap cooldown 0 (can place trap), far from any enemy
        val wiz = wizard(id = 1, position = Position(5, 5), level = 10, trapCooldown = 0)
        state.defenders.add(wiz)
        val far = goblin(id = 1, position = Position(29, 29))
        state.attackers.add(far)

        val manaBefore = state.currentMana.value
        val actionsBefore = wiz.actionsRemaining.value
        state.phase.value = GamePhase.PLAYER_TURN
        engine.autoDefenderAttacks()

        assertTrue(state.currentMana.value > manaBefore, "Mana should increase when no enemy is in range")
        assertTrue(wiz.actionsRemaining.value < actionsBefore, "Wizard action should be consumed")
    }

    @Test
    fun wizardAppearsInSpecialActionsWarningWhenItCanPlaceMagicalTrap() {
        val level = createLevel()
        val state = GameState(level, currentMana = mutableStateOf(0), maxMana = mutableStateOf(50))

        // Level 10+ wizard with trap cooldown 0 (can place trap)
        val wiz = wizard(id = 1, position = Position(5, 5), level = 10, trapCooldown = 0)
        state.defenders.add(wiz)
        // No active enemies needed for the special-actions check
        val special = state.getDefenderTypesWithSpecialActions()

        assertTrue(
            DefenderType.WIZARD_TOWER in special,
            "Wizard should appear in special actions warning when it can place a magical trap",
        )
    }

    @Test
    fun wizardDoesNotAppearInSpecialActionsWarningWhenItCannotPlaceTrap() {
        val level = createLevel()
        val state = GameState(level, currentMana = mutableStateOf(0), maxMana = mutableStateOf(50))

        // Level < 10 wizard — cannot place trap
        val wiz = wizard(id = 1, position = Position(5, 5), level = 5, trapCooldown = 0)
        state.defenders.add(wiz)

        val special = state.getDefenderTypesWithSpecialActions()

        assertFalse(
            DefenderType.WIZARD_TOWER in special,
            "Wizard should NOT appear in special actions warning when it cannot place a magical trap",
        )
    }
}
