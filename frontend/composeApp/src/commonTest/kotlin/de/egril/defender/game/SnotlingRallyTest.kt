package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Gribnak the Squealer's "Snotling Rally" summon ability.
 * Gribnak summons weak 5-HP snotlings on every free path tile within distance 2 of himself,
 * then goes on cooldown so he stays a light early-game threat.
 */
class SnotlingRallyTest {
    /**
     * Wide-open test level where every cell is on the path so summon positions are unrestricted.
     */
    private fun createOpenLevel(): Level {
        val allCells = (0 until 10).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 8,
            startPositions = listOf(Position(0, 4)),
            targetPositions = listOf(Position(9, 4)),
            pathCells = allCells,
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.SNOTLING_BOSS))),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    @Test
    fun testSnotlingBossStats() {
        assertEquals(30, AttackerType.SNOTLING_BOSS.health, "Gribnak should have 30 HP")
        assertEquals(3, AttackerType.SNOTLING_BOSS.speed, "Gribnak should have speed 3")
        assertEquals(25, AttackerType.SNOTLING_BOSS.reward, "Gribnak should reward 25 coins")
        assertEquals(15, AttackerType.SNOTLING_BOSS.xp, "Gribnak should reward 15 XP")
        assertTrue(AttackerType.SNOTLING_BOSS.canSummon, "Gribnak should be able to summon")
        assertTrue(AttackerType.SNOTLING_BOSS.isSummoner(), "Gribnak counts as a summoner")

        assertEquals(5, AttackerType.SNOTLING.health, "Snotlings should have 5 HP")
        assertEquals(5, AttackerType.SNOTLING.speed, "Snotlings should move 5 tiles per turn")
    }

    @Test
    fun testSnotlingRallySpawnsSnotlingsWithinDistanceTwo() {
        val level = createOpenLevel()
        val state = GameState(level)
        val bossPos = Position(5, 4)

        val boss =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING_BOSS,
                position = mutableStateOf(bossPos),
                level = mutableStateOf(1),
            )
        state.attackers.add(boss)

        EnemyAbilitySystem(state).processEnemyAbilities()

        val snotlings = state.attackers.filter { it.type == AttackerType.SNOTLING }
        assertTrue(snotlings.isNotEmpty(), "Gribnak should have summoned snotlings")

        snotlings.forEach { snotling ->
            assertEquals(5, snotling.currentHealth.value, "Snotlings should spawn with 5 HP")
            assertEquals(1, snotling.level.value, "Snotlings should spawn at level 1")
            val distance = snotling.position.value.distanceTo(bossPos)
            assertTrue(distance in 1..2, "Snotling should spawn within distance 2 (was $distance)")
        }

        // Cooldown should now be active
        assertEquals(3, boss.summonCooldown.value, "Snotling Rally should set a cooldown")
    }

    @Test
    fun testSnotlingRallyRespectsCooldown() {
        val level = createOpenLevel()
        val state = GameState(level)

        val boss =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING_BOSS,
                position = mutableStateOf(Position(5, 4)),
                level = mutableStateOf(1),
            )
        state.attackers.add(boss)

        val abilities = EnemyAbilitySystem(state)
        abilities.processEnemyAbilities()
        val countAfterFirst = state.attackers.count { it.type == AttackerType.SNOTLING }
        assertTrue(countAfterFirst > 0, "First rally should summon snotlings")

        // Immediately processing again decrements cooldown (3 -> 2) but must not summon more
        abilities.processEnemyAbilities()
        val countAfterSecond = state.attackers.count { it.type == AttackerType.SNOTLING }
        assertEquals(countAfterFirst, countAfterSecond, "Rally on cooldown should not summon more snotlings")
        assertEquals(2, boss.summonCooldown.value, "Cooldown should decrement each enemy turn")
    }

    @Test
    fun testSnotlingRallyDoesNotSpawnOnOccupiedTiles() {
        val level = createOpenLevel()
        val state = GameState(level)
        val bossPos = Position(5, 4)
        val occupied = bossPos.getHexNeighbors().first()

        val boss =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING_BOSS,
                position = mutableStateOf(bossPos),
                level = mutableStateOf(1),
            )
        state.attackers.add(boss)

        val blocker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(occupied),
                level = mutableStateOf(1),
            )
        state.attackers.add(blocker)

        EnemyAbilitySystem(state).processEnemyAbilities()

        val snotlingOnOccupied =
            state.attackers.any { it.type == AttackerType.SNOTLING && it.position.value == occupied }
        assertFalse(snotlingOnOccupied, "Snotlings must not spawn on an already-occupied tile")
    }

    @Test
    fun testSnotlingRallyAvoidsBarricadesAndRedirectsToSnotlingStacks() {
        val level = createOpenLevel()
        val state = GameState(level)
        val bossPos = Position(5, 4)
        val neighbors = bossPos.getHexNeighbors()
        val stackTile = neighbors.first()
        val barricadeTile = neighbors[1]

        val boss =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING_BOSS,
                position = mutableStateOf(bossPos),
                level = mutableStateOf(1),
            )
        state.attackers.add(boss)
        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(stackTile),
            ),
        )
        state.barricades.add(
            Barricade(
                id = 1,
                position = barricadeTile,
                healthPoints = mutableStateOf(120),
                defenderId = 0,
            ),
        )
        neighbors.drop(2).forEach { blocked ->
            state.attackers.add(
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.GOBLIN,
                    position = mutableStateOf(blocked),
                ),
            )
        }
        barricadeTile
            .getHexNeighbors()
            .filter { it != bossPos && it != stackTile }
            .forEach { blocked ->
                state.attackers.add(
                    Attacker(
                        id = state.nextAttackerId.value++,
                        type = AttackerType.GOBLIN,
                        position = mutableStateOf(blocked),
                    ),
                )
            }

        EnemyAbilitySystem(state).processEnemyAbilities()

        assertFalse(
            state.attackers.any {
                it.type == AttackerType.SNOTLING && !it.isDefeated.value && it.position.value == barricadeTile
            },
            "Snotlings must not spawn on barricade tiles",
        )
        assertTrue(
            state.attackers.count {
                it.type == AttackerType.SNOTLING && !it.isDefeated.value && it.position.value == stackTile
            } > 1,
            "Blocked snotling spawns should be redirected to valid snotling stacks",
        )
    }
}
