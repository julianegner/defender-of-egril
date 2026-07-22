package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Morguk Bonewhisper (issue #652): Goblin Shaman villain with three abilities:
 *  - War Totem Aura: passive speed buff to nearby Horde units (via villainAbility)
 *  - Hex of Silence: disables an adjacent tower each turn
 *  - Spirit Summon: spawns goblins on adjacent path tiles every 3 turns
 */
class MorgukBonewhisperTest {
    /**
     * A wide-open level where every cell is a path tile so spawn positions are unrestricted.
     */
    private fun createOpenLevel(): Level {
        val allCells = (0 until 12).flatMap { x -> (0 until 6).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Test Level",
            gridWidth = 12,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(11, 3)),
            pathCells = allCells,
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.MORGUK_BONEWHISPER))),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    // ─── Stat / config tests ──────────────────────────────────────────────────

    @Test
    fun morgukIsConfiguredAsVillain() {
        val type = AttackerType.MORGUK_BONEWHISPER
        assertTrue(type.isVillain, "Morguk should be a villain")
        assertTrue(type.hidesHealthBar, "Villain must not show health bar")
        assertTrue(type.isBoss, "Morguk is a boss villain")
        assertEquals(EnemyFaction.HORDE, type.faction, "Morguk belongs to the Horde")
        assertEquals("Morguk", type.villainName, "Villain proper name lives in the enum")
    }

    @Test
    fun morgukHasAllThreeAbilityFlags() {
        val type = AttackerType.MORGUK_BONEWHISPER
        assertTrue(type.canSummon, "Morguk can summon via Spirit Summon")
        assertTrue(type.canDisableTowers, "Morguk can disable towers via Hex of Silence")
        val ability = type.villainAbility
        assertTrue(ability != null, "Morguk must have a villain aura ability")
        assertEquals(VillainAuraEffect.SPEED, ability.effect, "War Totem Aura grants speed")
        assertTrue(ability.range > 0, "Aura must have a positive range")
    }

    @Test
    fun morgukIsUniqueBattlefield() {
        val morguk = Attacker(id = 1, type = AttackerType.MORGUK_BONEWHISPER, position = mutableStateOf(Position(5, 3)))
        val attackers = listOf(morguk)
        assertTrue(isUniqueEnemyAlreadyPresent(AttackerType.MORGUK_BONEWHISPER, attackers))
        // A defeated Morguk clears the way for a second spawn
        morguk.isDefeated.value = true
        assertFalse(isUniqueEnemyAlreadyPresent(AttackerType.MORGUK_BONEWHISPER, attackers))
    }

    @Test
    fun morgukCountsAsSummoner() {
        assertTrue(AttackerType.MORGUK_BONEWHISPER.isSummoner(), "isSummoner() must be true for Spirit Summon")
    }

    // ─── War Totem Aura ───────────────────────────────────────────────────────

    @Test
    fun warTotemAuraBuffsNearbyGoblins() {
        val level = createOpenLevel()
        val state = GameState(level)
        val abilities = EnemyAbilitySystem(state)

        val morguk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORGUK_BONEWHISPER,
                position = mutableStateOf(Position(5, 3)),
            )
        val nearbyGoblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(6, 3)),
            )
        val farGoblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(11, 3)),
            )
        state.attackers.addAll(listOf(morguk, nearbyGoblin, farGoblin))

        abilities.processEnemyAbilities()

        assertEquals(1, nearbyGoblin.speedBonus.value, "Nearby goblin should be buffed by War Totem Aura")
        assertEquals(0, farGoblin.speedBonus.value, "Out-of-range goblin must not be buffed")
    }

    @Test
    fun warTotemAuraDoesNotBuffNonHordeUnits() {
        val level = createOpenLevel()
        val state = GameState(level)
        val abilities = EnemyAbilitySystem(state)

        val morguk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORGUK_BONEWHISPER,
                position = mutableStateOf(Position(5, 3)),
            )
        val skeleton =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SKELETON,
                position = mutableStateOf(Position(6, 3)),
            )
        state.attackers.addAll(listOf(morguk, skeleton))

        abilities.processEnemyAbilities()

        assertEquals(0, skeleton.speedBonus.value, "Non-Horde units must not receive the War Totem Aura bonus")
    }

    // ─── Spirit Summon ────────────────────────────────────────────────────────

    @Test
    fun spiritSummonSpawnsGoblinsOnAdjacentPathTiles() {
        val level = createOpenLevel()
        val state = GameState(level)
        val morgukPos = Position(5, 3)

        val morguk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORGUK_BONEWHISPER,
                position = mutableStateOf(morgukPos),
                level = mutableStateOf(2),
            )
        state.attackers.add(morguk)

        EnemyAbilitySystem(state).processEnemyAbilities()

        val summoned = state.attackers.filter { it.type == AttackerType.GOBLIN && !it.isDefeated.value }
        assertTrue(summoned.isNotEmpty(), "Spirit Summon should have spawned at least one goblin")

        summoned.forEach { goblin ->
            val distance = goblin.position.value.distanceTo(morgukPos)
            assertEquals(1, distance, "Spirit Summon goblins must appear on adjacent tiles (distance 1)")
            assertEquals(2, goblin.level.value, "Summoned goblins inherit Morguk's level")
        }

        assertEquals(3, morguk.summonCooldown.value, "Spirit Summon cooldown should be 3 after activation")
    }

    @Test
    fun spiritSummonRespectsCooldown() {
        val level = createOpenLevel()
        val state = GameState(level)

        val morguk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORGUK_BONEWHISPER,
                position = mutableStateOf(Position(5, 3)),
            )
        state.attackers.add(morguk)

        val abilities = EnemyAbilitySystem(state)
        abilities.processEnemyAbilities()
        val firstCount = state.attackers.count { it.type == AttackerType.GOBLIN }
        assertTrue(firstCount > 0, "First Spirit Summon should produce goblins")

        // Immediately processing again (cooldown at 3 → decrements to 2) must not summon more
        abilities.processEnemyAbilities()
        val secondCount = state.attackers.count { it.type == AttackerType.GOBLIN }
        assertEquals(firstCount, secondCount, "Spirit Summon on cooldown must not spawn extra goblins")
        assertEquals(2, morguk.summonCooldown.value, "Cooldown decrements each enemy turn")
    }

    @Test
    fun spiritSummonDoesNotSpawnOnOccupiedTile() {
        val level = createOpenLevel()
        val state = GameState(level)
        val morgukPos = Position(5, 3)
        // Pre-occupy all adjacent tiles with living enemies
        val adjacentTiles = morgukPos.getHexNeighbors()

        val morguk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORGUK_BONEWHISPER,
                position = mutableStateOf(morgukPos),
            )
        state.attackers.add(morguk)

        adjacentTiles.forEach { pos ->
            if (pos.x >= 0 && pos.x < level.gridWidth && pos.y >= 0 && pos.y < level.gridHeight) {
                state.attackers.add(
                    Attacker(
                        id = state.nextAttackerId.value++,
                        type = AttackerType.GOBLIN,
                        position = mutableStateOf(pos),
                    ),
                )
            }
        }

        val goblinsBefore = state.attackers.count { it.type == AttackerType.GOBLIN }
        EnemyAbilitySystem(state).processEnemyAbilities()
        val goblinsAfter = state.attackers.count { it.type == AttackerType.GOBLIN }

        assertEquals(goblinsBefore, goblinsAfter, "Spirit Summon must not spawn on an occupied tile")
    }

    // ─── Hex of Silence ───────────────────────────────────────────────────────

    @Test
    fun hexOfSilenceDisablesAdjacentTower() {
        // Narrow path level so the tower can be adjacent to the path
        val pathCells = (0..11).map { Position(it, 3) }.toSet()
        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 12,
                gridHeight = 6,
                startPositions = listOf(Position(0, 3)),
                targetPositions = listOf(Position(11, 3)),
                pathCells = pathCells,
                buildAreas = setOf(Position(5, 2)),
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.MORGUK_BONEWHISPER))),
                initialCoins = 1000,
                healthPoints = 10,
            )
        val state = GameState(level)

        val tower =
            Defender(
                id = 1,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(5, 2)),
                level = mutableStateOf(1),
            )
        tower.buildTimeRemaining.value = 0 // Fully built
        state.defenders.add(tower)

        val morguk =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.MORGUK_BONEWHISPER,
                position = mutableStateOf(Position(5, 3)), // adjacent to the tower
                level = mutableStateOf(1),
            )
        state.attackers.add(morguk)

        EnemyAbilitySystem(state).processEnemyAbilities()

        assertTrue(tower.isDisabled.value, "Hex of Silence should disable the adjacent tower")
        assertTrue(tower.disabledTurnsRemaining.value > 0, "Disabled tower must have turns remaining")
    }
}
