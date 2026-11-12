package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Barricade
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RazzikRocketTest {
    private fun createLevel(): Level {
        val path = (0..15).map { Position(it, 3) }.toSet()
        return Level(
            id = 1,
            name = "Razzik Test",
            gridWidth = 16,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(15, 3)),
            pathCells = path,
            buildAreas = setOf(Position(4, 2), Position(7, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun createRazzik(state: GameState, at: Position): Attacker =
        Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.RAZZIK_THE_ROCKETEER,
            position = mutableStateOf(at),
            level = mutableStateOf(1),
            currentTarget = mutableStateOf(Position(15, 3)),
        ).also { state.attackers.add(it) }

    @Test
    fun razzikIsConfiguredAsVillainBoss() {
        val razzik = AttackerType.RAZZIK_THE_ROCKETEER
        assertTrue(razzik.isVillain)
        assertEquals("Razzik", razzik.villainName)
        assertEquals(150, razzik.health)
        assertTrue(razzik.useLightNarrativeText)
    }

    @Test
    fun rocketsDamageBarricadeTowerBaseWhenNotCrashing() {
        val state = GameState(createLevel())
        val razzik = createRazzik(state, Position(5, 3))
        val base =
            Barricade(
                id = 1,
                position = Position(6, 2),
                healthPoints = mutableStateOf(500),
                defenderId = 1,
            )
        state.barricades.add(base)

        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))
        abilities.processEnemyAbilities()

        assertTrue(state.rocketAttackEffects.isNotEmpty(), "Razzik should launch rockets")
        assertTrue(base.healthPoints.value < 500, "Some rocket should hit the tower base")
        assertTrue(base.healthPoints.value >= 500 - 5 * 60, "Five rockets deal at most 6x10 each")
        assertEquals(150, razzik.currentHealth.value, "Razzik himself takes no self-damage")
    }

    @Test
    fun rocketsIgnoreBarricadesOutOfRange() {
        val state = GameState(createLevel())
        createRazzik(state, Position(0, 3))
        val far =
            Barricade(
                id = 1,
                position = Position(15, 2),
                healthPoints = mutableStateOf(500),
                defenderId = 1,
            )
        state.barricades.add(far)

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        assertEquals(500, far.healthPoints.value, "Barricade beyond 8 tiles must not be targeted")
        assertTrue(state.rocketAttackEffects.isEmpty(), "No target in range means no rocket")
    }

    @Test
    fun barricadesWithZeroHealthAreNotTargeted() {
        val state = GameState(createLevel())
        createRazzik(state, Position(5, 3))
        val dead =
            Barricade(
                id = 1,
                position = Position(6, 2),
                healthPoints = mutableStateOf(0),
                defenderId = 1,
            )
        state.barricades.add(dead)

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        assertTrue(state.rocketAttackEffects.isEmpty(), "Destroyed barricades are not valid targets")
    }
}
