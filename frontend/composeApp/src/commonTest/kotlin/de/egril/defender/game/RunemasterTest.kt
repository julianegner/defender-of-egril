package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Barricade
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.attackerTargetDamage
import de.egril.defender.model.hexDistanceTo
import de.egril.defender.model.isSummoner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunemasterTest {
    @Test
    fun defeatingRunemasterAddsOneRuneToInventory() {
        val state = createState()
        val combatSystem = CombatSystem(state, BridgeSystem(state))
        val runemaster =
            Attacker(
                    id = 1,
                    type = AttackerType.RUNEMASTER,
                    position = mutableStateOf(Position(1, 1)),
            )
        state.attackers.add(runemaster)

        runemaster.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        combatSystem.processDefeatedAttackers()

        assertEquals(1, state.runes.value)
    }

    @Test
    fun runemasterSharesMageMovementAndThreatBehavior() {
        assertEquals(2, AttackerType.RUNEMASTER.speed)
        assertEquals(true, AttackerType.RUNEMASTER.isSummoner())
        assertEquals(true, AttackerType.RUNEMASTER.canBuildBridge)
        assertEquals(3, attackerTargetDamage(AttackerType.RUNEMASTER, level = 3))
    }

    @Test
    fun arcaneBlastDamagesBarricadeFromRange() {
        val state = createState()
        val runemaster = addRunemaster(state)
        val barricadePosition = Position(3, 1)
        val barricade = Barricade(id = 1, position = barricadePosition, healthPoints = mutableStateOf(20), defenderId = 0)
        state.barricades.add(barricade)
        assertTrue(runemaster.position.value.hexDistanceTo(barricadePosition) in 2..3)

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        assertEquals(19, barricade.healthPoints.value)
    }

    @Test
    fun arcaneBlastDisablesTowerFromRange() {
        val state = createState()
        val runemaster = addRunemaster(state)
        val towerPosition = Position(3, 1)
        val tower = Defender(id = 1, type = DefenderType.SPIKE_TOWER, position = mutableStateOf(towerPosition))
        state.defenders.add(tower)
        assertTrue(runemaster.position.value.hexDistanceTo(towerPosition) in 2..3)

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        assertTrue(tower.isDisabled.value)
        assertEquals(2, tower.disabledTurnsRemaining.value)
    }

    private fun createState(): GameState {
        val level =
            Level(
                id = 1,
                name = "Runemaster Test",
                gridWidth = 10,
                gridHeight = 10,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(9, 9)),
                pathCells = (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 0,
                healthPoints = 10,
            )
        return GameState(level = level)
    }

    private fun addRunemaster(state: GameState): Attacker {
        val runemaster =
            Attacker(
            id = 1,
            type = AttackerType.RUNEMASTER,
            position = mutableStateOf(Position(1, 1)),
            )
        state.attackers.add(runemaster)
        return runemaster
    }
}
