package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Gargoyle
import de.egril.defender.model.GargoyleRules
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.isImmuneToAttackFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GargoyleCombatTest {
    private fun createState(): GameState {
        val level =
            Level(
                id = 1,
                name = "Gargoyle Combat Test",
                gridWidth = 10,
                gridHeight = 10,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(9, 9)),
                pathCells = (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
                availableTowers = emptySet(),
            )
        return GameState(level = level)
    }

    private fun addDefender(state: GameState, type: DefenderType, position: Position): Defender {
        val defender =
            Defender(
                id = state.nextDefenderId.value++,
                type = type,
                position = mutableStateOf(position),
                actionsRemaining = mutableStateOf(1),
                buildTimeRemaining = mutableStateOf(0),
            )
        state.defenders.add(defender)
        return defender
    }

    private fun addGargoyle(state: GameState, position: Position, health: Int = GargoyleRules.HEALTH_PER_LEVEL): Gargoyle {
        val gargoyle =
            Gargoyle(
                id = state.nextGargoyleId.value++,
                position = mutableStateOf(position),
                level = mutableStateOf(1),
                currentHealth = mutableStateOf(health),
            )
        state.gargoyles.add(gargoyle)
        return gargoyle
    }

    @Test
    fun manualBallistaHitReducesInactiveGargoyleHealthWithoutCoins() {
        val state = createState()
        val combat = CombatSystem(state, BridgeSystem(state))
        val ballista = addDefender(state, DefenderType.BALLISTA_TOWER, Position(0, 0))
        val gargoyle = addGargoyle(state, Position(4, 0))
        val coinsBefore = state.coins.value

        val hit = combat.defenderAttackPosition(ballista.id, Position(4, 0)) {}

        assertTrue(hit)
        assertEquals(GargoyleRules.HEALTH_PER_LEVEL - DefenderType.BALLISTA_TOWER.baseDamage, gargoyle.currentHealth.value)
        assertTrue(state.gargoyles.contains(gargoyle))
        assertEquals(coinsBefore, state.coins.value)
        assertEquals(0, state.pendingCoinGains.value)
    }

    @Test
    fun destroyingInactiveGargoyleGrantsXpButNoCoins() {
        val state = createState()
        val combat = CombatSystem(state, BridgeSystem(state))
        val ballista = addDefender(state, DefenderType.BALLISTA_TOWER, Position(0, 0))
        addGargoyle(state, Position(4, 0), health = 1)

        combat.defenderAttackPosition(ballista.id, Position(4, 0)) {}

        assertTrue(state.gargoyles.isEmpty())
        assertEquals(AttackerType.GARGOYLE.xp * 1, state.xpEarnedThisLevel.value)
        assertEquals(0, state.pendingCoinGains.value)
        assertEquals(0, state.enemiesKilledTotal.value)
    }

    @Test
    fun spearAndBowConsumeActionWithoutDamagingInactiveGargoyle() {
        for (type in listOf(DefenderType.SPEAR_TOWER, DefenderType.BOW_TOWER)) {
            val state = createState()
            val combat = CombatSystem(state, BridgeSystem(state))
            val tower = addDefender(state, type, Position(0, 0))
            val gargoyle = addGargoyle(state, Position(2, 0))

            val hit = combat.defenderAttackPosition(tower.id, Position(2, 0)) {}

            assertTrue(hit, "$type should still consume its action")
            assertEquals(GargoyleRules.HEALTH_PER_LEVEL, gargoyle.currentHealth.value, "$type must not damage gargoyles")
            assertEquals(0, tower.actionsRemaining.value)
        }
    }

    @Test
    fun destroyedActiveGargoyleGrantsXpButNoCoins() {
        val state = createState()
        val combat = CombatSystem(state, BridgeSystem(state))
        val gargoyle =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GARGOYLE,
                position = mutableStateOf(Position(5, 5)),
                currentHealth = mutableStateOf(0),
                level = mutableStateOf(2),
            )
        state.attackers.add(gargoyle)
        gargoyle.isDefeated.value = true

        combat.processDefeatedAttackers()

        assertFalse(state.attackers.contains(gargoyle))
        assertEquals(AttackerType.GARGOYLE.xp * 2, state.xpEarnedThisLevel.value)
        assertEquals(0, state.pendingCoinGains.value)
        assertEquals(0, state.enemiesKilledTotal.value)
    }

    @Test
    fun fireballHitsInactiveGargoyleEvenWhenNoTargetIsSelected() {
        val state = createState()
        val combat = CombatSystem(state, BridgeSystem(state))
        val wizard = addDefender(state, DefenderType.WIZARD_TOWER, Position(0, 0))
        val gargoyle = addGargoyle(state, Position(3, 0))

        combat.defenderAttackPosition(wizard.id, Position(3, 0)) {}

        assertTrue(gargoyle.currentHealth.value < GargoyleRules.HEALTH_PER_LEVEL, "fireball must damage stone gargoyles")
    }

    @Test
    fun acidHitsInactiveGargoyle() {
        val state = createState()
        val combat = CombatSystem(state, BridgeSystem(state))
        val alchemy = addDefender(state, DefenderType.ALCHEMY_TOWER, Position(0, 0))
        val gargoyle = addGargoyle(state, Position(2, 0))

        combat.defenderAttackPosition(alchemy.id, Position(2, 0)) {}

        assertTrue(gargoyle.currentHealth.value < GargoyleRules.HEALTH_PER_LEVEL, "acid must damage stone gargoyles")
    }

    @Test
    fun gargoyleXpMatchesDragonXp() {
        assertEquals(AttackerType.DRAGON.xp, AttackerType.GARGOYLE.xp)
    }

    @Test
    fun activeGargoyleIsImmuneToNoDamageTowers() {
        val gargoyle =
            Attacker(
                id = 1,
                type = AttackerType.GARGOYLE,
                position = mutableStateOf(Position(1, 1)),
                currentHealth = mutableStateOf(GargoyleRules.HEALTH_PER_LEVEL),
                level = mutableStateOf(1),
            )

        for (type in GargoyleRules.NO_DAMAGE_DEFENDERS) {
            assertTrue(gargoyle.isImmuneToAttackFrom(type), "$type should show the immunity shield")
        }
        assertFalse(gargoyle.isImmuneToAttackFrom(DefenderType.WIZARD_TOWER))
    }
}
