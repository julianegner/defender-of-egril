package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MorvathShadowmasterTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 30).flatMap { x -> (0 until 30).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Morvath Test Level",
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

    @Test
    fun morvathIsConfiguredAsVillain() {
        val type = AttackerType.MORVATH_THE_SHADOWMASTER
        assertTrue(type.isVillain, "Morvath should be a villain")
        assertTrue(type.isBoss, "Morvath should be a boss")
        assertEquals("Morvath", type.villainName, "Morvath short name should be present")
    }

    @Test
    fun morvathCreatesShadowFogOnOwnAndNearbyTilesPlusOneRangedTile() {
        val state = GameState(createOpenLevel())
        val morvathPos = Position(15, 15)
        val morvath =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORVATH_THE_SHADOWMASTER,
                position = mutableStateOf(morvathPos),
            )
        state.attackers.add(morvath)

        val abilitySystem = EnemyAbilitySystem(state, PathfindingSystem(state))
        abilitySystem.processEnemyAbilities()
        abilitySystem.applyPendingMorvathFog()

        val fogTiles = state.fieldEffects.filter { it.type == FieldEffectType.SHADOW_FOG }
        val guaranteedTiles = (listOf(morvathPos) + morvathPos.getHexNeighbors()).toSet()

        guaranteedTiles.forEach { pos ->
            assertTrue(
                fogTiles.any { it.position == pos },
                "Morvath should always fog his own tile and adjacent tile $pos",
            )
        }

        val rangedFogTiles =
            fogTiles.filter {
                it.position !in guaranteedTiles && morvathPos.hexDistanceTo(it.position) <= 10
            }
        assertEquals(
            1,
            rangedFogTiles.size,
            "Morvath should add exactly one additional ranged fog tile each enemy turn",
        )
        assertTrue(fogTiles.all { it.turnsRemaining == 3 }, "Newly created Morvath fog should last 3 turns")
    }

    @Test
    fun morvathRefreshesFogDurationToThreeTurns() {
        val state = GameState(createOpenLevel())
        val morvathPos = Position(15, 15)
        val morvath =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORVATH_THE_SHADOWMASTER,
                position = mutableStateOf(morvathPos),
            )
        state.attackers.add(morvath)
        state.fieldEffects.add(
            FieldEffect(
                position = morvathPos,
                type = FieldEffectType.SHADOW_FOG,
                damage = 0,
                turnsRemaining = 1,
                defenderId = 0,
                attackerId = morvath.id,
            ),
        )

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        val refreshed = state.fieldEffects.first { it.type == FieldEffectType.SHADOW_FOG && it.position == morvathPos }
        assertEquals(3, refreshed.turnsRemaining, "Morvath should refresh existing shadow fog back to 3 turns")
    }
}
