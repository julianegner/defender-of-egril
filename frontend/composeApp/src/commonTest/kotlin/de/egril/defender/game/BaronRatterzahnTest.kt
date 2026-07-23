package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.isSwarmUnit
import de.egril.defender.model.isSummoner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaronRatterzahnTest {
    private fun createLevel(): Level {
        val path = (0..11).map { Position(it, 3) }.toSet()
        return Level(
            id = 1,
            name = "Baron Test",
            gridWidth = 12,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(11, 3)),
            pathCells = path,
            buildAreas = setOf(Position(4, 2), Position(7, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    @Test
    fun baronAndRoboticGoblinAreConfigured() {
        val baron = AttackerType.BARON_RATTERZAHN
        assertTrue(baron.isVillain)
        assertTrue(baron.isSummoner())
        assertEquals("Ratterzahn", baron.villainName)
        assertEquals(AttackerType.GOBLIN.health * 2, AttackerType.ROBOTIC_GOBLIN.health)
        assertTrue(AttackerType.ROBOTIC_GOBLIN.isRobotic)
        assertFalse(AttackerType.ROBOTIC_GOBLIN.isSwarmUnit())
    }

    @Test
    fun scrapPilesDropAndHatchNextRound() {
        val state = GameState(createLevel())
        val baron =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.BARON_RATTERZAHN,
                position = mutableStateOf(Position(5, 3)),
                level = mutableStateOf(2),
                currentTarget = mutableStateOf(Position(11, 3)),
            )
        state.attackers.add(baron)

        val abilities = EnemyAbilitySystem(state)
        abilities.processEnemyAbilities()

        assertEquals(2, state.scrapPiles.size, "Baron should drop two scrap piles")
        assertEquals(0, state.attackers.count { it.type == AttackerType.ROBOTIC_GOBLIN }, "Scraps hatch one round later")
        assertTrue(
            state.scrapPiles.map { it.position }.distinct().size == 2,
            "Scrap piles should be on different tiles",
        )
        assertTrue(
            state.scrapPiles.all { pile ->
                state.level.isOnPath(pile.position) || pile.position.getHexNeighbors().any { state.level.isOnPath(it) }
            },
            "Scrap piles should be on or next to the used path",
        )

        state.turnNumber.value = 1
        abilities.processEnemyAbilities()

        val roboticGoblins = state.attackers.filter { it.type == AttackerType.ROBOTIC_GOBLIN }
        assertEquals(2, roboticGoblins.size, "Both scrap piles should hatch into robotic goblins")
        assertTrue(state.scrapPiles.isEmpty(), "Scrap piles should be consumed after hatching")
        assertTrue(roboticGoblins.all { it.level.value == 2 }, "Robotic goblins should inherit Baron's level")
    }

    @Test
    fun rocketStunsTowerAndUsesCooldown() {
        val state = GameState(createLevel())
        val baron =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.BARON_RATTERZAHN,
                position = mutableStateOf(Position(5, 3)),
                level = mutableStateOf(1),
            )
        val firstTower =
            Defender(
                id = 1,
                type = DefenderType.BOW_TOWER,
                position = mutableStateOf(Position(4, 2)),
                level = mutableStateOf(1),
                buildTimeRemaining = mutableStateOf(0),
            )
        val secondTower =
            Defender(
                id = 2,
                type = DefenderType.SPEAR_TOWER,
                position = mutableStateOf(Position(7, 2)),
                level = mutableStateOf(1),
                buildTimeRemaining = mutableStateOf(0),
            )
        state.attackers.add(baron)
        state.defenders.addAll(listOf(firstTower, secondTower))

        val abilities = EnemyAbilitySystem(state)
        abilities.processEnemyAbilities()
        assertTrue(firstTower.isDisabled.value || secondTower.isDisabled.value, "Rocket should disable a tower immediately")
        assertEquals(4, (if (firstTower.isDisabled.value) firstTower else secondTower).disabledTurnsRemaining.value)
        assertEquals(1, state.rocketAttackEffects.size, "Rocket attack should create an animation effect")

        val disabledBeforeSecondTurn = state.defenders.count { it.isDisabled.value }
        state.turnNumber.value = 1
        abilities.processEnemyAbilities()
        assertEquals(disabledBeforeSecondTurn, state.defenders.count { it.isDisabled.value }, "Rocket should respect cooldown")
    }
}
