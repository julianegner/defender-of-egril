package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.attackerTargetDamage
import de.egril.defender.model.hexDistanceTo
import de.egril.defender.model.isRealVillain
import de.egril.defender.model.isUniqueEnemyAlreadyPresent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SilasMaskmasterTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 8).flatMap { x -> (0 until 6).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Silas Test",
            gridWidth = 8,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(7, 2)),
            pathCells = allCells,
            buildAreas = setOf(Position(3, 1), Position(4, 1), Position(5, 1)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    @Test
    fun silasIsConfiguredAsRealVillain() {
        val silas = AttackerType.SILAS_THE_MASKMASTER
        assertTrue(silas.isVillain)
        assertTrue(silas.isRealVillain)
        assertTrue(silas.canSummon)
        assertTrue(silas.canDisableTowers)
        assertEquals("Silas", silas.villainName)
        assertEquals(2, silas.mirrorImageCount)
        assertEquals(2, silas.mirrorImageRange)
        assertEquals(3, silas.mirrorImageCooldown)
        assertEquals(2, silas.mirrorBlindDurationTurns)
    }

    @Test
    fun mirrorImagesSpawnAndRemainWithinTwoTiles() {
        val state = GameState(createOpenLevel())
        val silasStart = Position(3, 2)
        val silas =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SILAS_THE_MASKMASTER,
                position = mutableStateOf(silasStart),
                currentTarget = mutableStateOf(Position(7, 2)),
            )
        state.attackers.add(silas)

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        val mirrors = state.attackers.filter { it.type == AttackerType.SILAS_MIRROR_IMAGE }
        assertEquals(2, mirrors.size, "Silas should create two mirror images")

        val occupiedPositions = (mirrors.map { it.position.value } + silas.position.value).toSet()
        assertEquals(3, occupiedPositions.size, "Silas and both mirrors should occupy distinct tiles")
        assertTrue(
            occupiedPositions.all { it == silasStart || it.hexDistanceTo(silasStart) <= 2 },
            "Silas and his new mirrors should stay on the original tile or within the configured range",
        )
        assertEquals(3, silas.summonCooldown.value, "Mirror Image should enter cooldown after use")
    }

    @Test
    fun attackingMirrorImageBlindsTowerAndLeavesRealSilasUnharmed() {
        val state = GameState(createOpenLevel())
        val tower =
            Defender(
                id = 1,
                type = DefenderType.BOW_TOWER,
                position = mutableStateOf(Position(3, 1)),
                actionsRemaining = mutableStateOf(1),
            )
        val realSilas =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SILAS_THE_MASKMASTER,
                position = mutableStateOf(Position(5, 2)),
            )
        val mirror =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SILAS_MIRROR_IMAGE,
                position = mutableStateOf(Position(4, 2)),
            )
        state.defenders.add(tower)
        state.attackers.addAll(listOf(realSilas, mirror))

        val combat = CombatSystem(state, BridgeSystem(state))
        val attacked = combat.defenderAttack(tower.id, mirror.id) { combat.processDefeatedAttackers() }

        assertTrue(attacked, "The tower should be able to attack the mirror image")
        assertTrue(tower.isDisabled.value, "Striking a mirror should blind the tower")
        assertEquals(3, tower.disabledTurnsRemaining.value, "Player-turn blinds need +1 stored turn for the countdown model")
        assertEquals(0, tower.actionsRemaining.value, "A blinded tower should lose the rest of its current actions")
        assertEquals(realSilas.maxHealth, realSilas.currentHealth.value, "The real Silas must not take damage when a mirror is hit")
        assertFalse(state.attackers.any { it.id == mirror.id && !it.isDefeated.value }, "The struck mirror should vanish")
        assertTrue(state.pendingMessages.any { it.type == GameMessageType.SILAS_MIRROR_HIT })
    }

    @Test
    fun mirrorImagesDoNotCountAsRealVillainsAndDoNotLoseTheLevelOnTargetBreach() {
        val level =
            Level(
                id = 1,
                name = "Silas Target Test",
                gridWidth = 4,
                gridHeight = 4,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(3, 1)),
                pathCells = setOf(Position(0, 1), Position(1, 1), Position(2, 1), Position(3, 1)),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 5,
            )
        val state = GameState(level)
        val mirror =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SILAS_MIRROR_IMAGE,
                position = mutableStateOf(Position(2, 1)),
                currentTarget = mutableStateOf(Position(3, 1)),
            )
        state.attackers.add(mirror)

        assertFalse(isUniqueEnemyAlreadyPresent(AttackerType.SILAS_THE_MASKMASTER, listOf(mirror)))
        assertEquals(0, attackerTargetDamage(AttackerType.SILAS_MIRROR_IMAGE, 1))

        GameEngine(state).applyMovement(mirror.id, Position(3, 1))

        assertEquals(5, state.healthPoints.value, "Mirror images should not damage the target")
        assertFalse(state.villainReachedTarget.value, "Mirror images must not trigger the instant-loss villain breach flag")
        assertTrue(mirror.isDefeated.value, "Mirror images should still disappear when they reach the target")
    }
}
