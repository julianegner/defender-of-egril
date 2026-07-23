package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the Villain System (issue #538): unique enemy heroes with configurable, range-based
 * aura abilities, hidden health display, and uniqueness on the battlefield.
 */
class VillainSystemTest {
    private fun createTestLevel(): Level =
        Level(
            id = 1,
            name = "Test Level",
            gridWidth = 12,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(11, 3)),
            pathCells = (0..11).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            initialCoins = 1000,
            healthPoints = 10,
        )

    @Test
    fun garokkIsAConfiguredVillain() {
        val type = AttackerType.GAROKK
        assertTrue(type.isVillain, "Garokk should be a villain")
        assertTrue(type.hidesHealthBar, "Villains must not display their health")
        assertEquals(EnemyFaction.HORDE, type.faction, "Garokk leads the Horde")
        val ability = type.villainAbility
        assertNotNull(ability, "Garokk should have a configured ability")
        assertEquals(VillainAuraEffect.SPEED, ability.effect)
        assertTrue(ability.range > 0, "Villain aura should have a positive range")
    }

    @Test
    fun freyaIsAnUndeadVillainWithShieldWall() {
        val type = AttackerType.FALLEN_SHIELDMAIDEN_FREYA
        assertTrue(type.isVillain, "Freya should be a villain")
        assertTrue(type.hidesHealthBar, "Villains must not display their health")
        assertTrue(type.isBoss, "Freya should be a boss villain")
        assertEquals(EnemyFaction.UNDEAD, type.faction, "Freya should lead the undead")
        assertEquals("Freya", type.villainName, "Freya's short name should live in the enum")
        assertEquals(2, type.shieldWallRangeBehind, "Freya should protect the next two tiles behind her")
    }

    @Test
    fun ewhadIsAVillainWithLanguageIndependentName() {
        val type = AttackerType.EWHAD
        assertTrue(type.isVillain, "Ewhad should be a villain")
        assertTrue(type.hidesHealthBar, "Ewhad must not display its health")
        assertTrue(type.isBoss, "Ewhad remains a boss")
        assertEquals("Ewhad", type.villainName, "Ewhad's name lives in the enum, not the translations")
    }

    @Test
    fun villainNamesLiveInTheEnum() {
        assertEquals("Garokk", AttackerType.GAROKK.villainName)
        assertEquals("Ewhad", AttackerType.EWHAD.villainName)
        assertEquals("Freya", AttackerType.FALLEN_SHIELDMAIDEN_FREYA.villainName)
        // Regular enemies have no enum name; their (translated) names come from the string resources.
        assertEquals(null, AttackerType.GOBLIN.villainName)
    }

    @Test
    fun regularEnemiesHideNoHealthButVillainsDo() {
        assertFalse(AttackerType.GOBLIN.hidesHealthBar)
        assertTrue(AttackerType.EWHAD.hidesHealthBar, "Ewhad still hides health")
        assertTrue(AttackerType.GAROKK.hidesHealthBar)
    }

    @Test
    fun villainsAndBossesAreUniqueOnTheBattlefield() {
        val garokk =
            Attacker(
                id = 1,
                type = AttackerType.GAROKK,
                position = mutableStateOf(Position(5, 3)),
            )
        val attackers = listOf(garokk)
        assertTrue(isUniqueEnemyAlreadyPresent(AttackerType.GAROKK, attackers))
        val ewhad = Attacker(2, AttackerType.EWHAD, mutableStateOf(Position(1, 3)))
        assertTrue(isUniqueEnemyAlreadyPresent(AttackerType.EWHAD, listOf(ewhad)))
        assertFalse(isUniqueEnemyAlreadyPresent(AttackerType.GOBLIN, attackers), "Regular enemies are not unique")

        // A defeated villain no longer blocks a new one
        garokk.isDefeated.value = true
        assertFalse(isUniqueEnemyAlreadyPresent(AttackerType.GAROKK, attackers))
    }

    @Test
    fun warCryGrantsSpeedBonusToNearbyHordeUnits() {
        val level = createTestLevel()
        val state = GameState(level)
        val abilitySystem = EnemyAbilitySystem(state)

        val garokk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GAROKK,
                position = mutableStateOf(Position(3, 3)),
                level = mutableStateOf(1),
            )
        val nearbyGoblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(4, 3)),
                level = mutableStateOf(1),
            )
        val farGoblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(11, 3)),
                level = mutableStateOf(1),
            )
        state.attackers.addAll(listOf(garokk, nearbyGoblin, farGoblin))

        // First activation happens immediately (cooldown starts at 0).
        abilitySystem.processEnemyAbilities()

        assertEquals(1, nearbyGoblin.speedBonus.value, "Nearby Horde unit should be buffed by War Cry")
        assertEquals(0, farGoblin.speedBonus.value, "Out-of-range units should not be buffed")
    }

    @Test
    fun warCryOnlyBuffsOwnFaction() {
        val level = createTestLevel()
        val state = GameState(level)
        val abilitySystem = EnemyAbilitySystem(state)

        val garokk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GAROKK,
                position = mutableStateOf(Position(3, 3)),
                level = mutableStateOf(1),
            )
        val skeleton =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SKELETON,
                position = mutableStateOf(Position(4, 3)),
                level = mutableStateOf(1),
            )
        state.attackers.addAll(listOf(garokk, skeleton))

        abilitySystem.processEnemyAbilities()

        assertEquals(0, skeleton.speedBonus.value, "Undead units are not part of the Horde faction")
    }

    @Test
    fun villainReachingTargetLosesTheLevel() {
        // A villain breaching a target loses the level immediately, even with health points left.
        val pathCells = (0..9).map { Position(it, 2) }.toSet()
        val level =
            Level(
                id = 1,
                name = "Villain Target Test",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 2)),
                targetPositions = listOf(Position(9, 2)),
                pathCells = pathCells,
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GAROKK))),
                initialCoins = 100,
                healthPoints = 100,
            )
        val state = GameState(level)
        val engine = GameEngine(state)

        val garokk =
            Attacker(
                id = 1,
                type = AttackerType.GAROKK,
                position = mutableStateOf(Position(8, 2)),
                level = mutableStateOf(1),
            )
        state.attackers.add(garokk)
        assertFalse(state.villainReachedTarget.value)
        assertFalse(state.isLevelLost())

        var turnCount = 0
        while (!garokk.isDefeated.value && turnCount < 20) {
            val movements = engine.calculateEnemyTurnMovements()
            for (movementStep in movements.allMovementSteps) {
                for ((attackerId, newPosition) in movementStep) {
                    engine.applyMovement(attackerId, newPosition)
                }
            }
            turnCount++
        }

        assertTrue(garokk.isDefeated.value, "Villain should have reached the target")
        assertTrue(state.villainReachedTarget.value, "Reaching a target must flag the villain breach")
        assertTrue(state.healthPoints.value > 0, "Health should remain, yet the level is still lost")
        assertTrue(state.isLevelLost(), "A villain reaching a target loses the level")
    }

    @Test
    fun woundedVillainKeepsPermanentSelfSpeedBonus() {
        val level = createTestLevel()
        val state = GameState(level)
        val abilitySystem = EnemyAbilitySystem(state)

        val garokk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GAROKK,
                position = mutableStateOf(Position(3, 3)),
                level = mutableStateOf(1),
            )
        // Below 50% health
        garokk.currentHealth.value = garokk.maxHealth / 3
        state.attackers.add(garokk)

        // Advance a few rounds; even on non-activation rounds a wounded villain keeps its self bonus.
        repeat(2) { abilitySystem.processEnemyAbilities() }

        assertEquals(1, garokk.speedBonus.value, "Wounded villain keeps its permanent movement bonus")
    }
}
