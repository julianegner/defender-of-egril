package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.FieldEffectType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.isSummoner
import de.egril.defender.model.isUniqueEnemyAlreadyPresent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Araxxa the Giant Spider villain, her spiderlings, and the spreading web area.
 */
class AraxxaTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 12).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Araxxa Test Level",
            gridWidth = 12,
            gridHeight = 8,
            startPositions = listOf(Position(0, 4)),
            targetPositions = listOf(Position(11, 4)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun createCombatLevel(): Level =
        Level(
            id = 1,
            name = "Araxxa Combat Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )

    @Test
    fun araxxaIsConfiguredAsVillainAndSummoner() {
        val type = AttackerType.ARAXXA
        assertTrue(type.isVillain, "Araxxa should be a villain")
        assertTrue(type.isBoss, "Araxxa should be a boss")
        assertTrue(type.canSummon, "Araxxa should summon spiderlings")
        assertTrue(type.isSummoner(), "Araxxa must count as a summoner")
        assertEquals("Araxxa", type.villainName, "Araxxa's short villain name should live in the enum")
        assertTrue(isUniqueEnemyAlreadyPresent(type, listOf(Attacker(1, type, mutableStateOf(Position(5, 4))))))
    }

    @Test
    fun araxxaSpawnsSpiderlingsOnAdjacentTiles() {
        val state = GameState(createOpenLevel())
        val araxxaPos = Position(5, 4)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
                level = mutableStateOf(2),
            )
        state.attackers.add(araxxa)

        EnemyAbilitySystem(state).processEnemyAbilities()

        val spiderlings = state.attackers.filter { it.type == AttackerType.SPIDERLING && !it.isDefeated.value }
        assertTrue(spiderlings.isNotEmpty(), "Araxxa should spawn spiderlings")
        spiderlings.forEach { spiderling ->
            assertEquals(1, spiderling.position.value.distanceTo(araxxaPos), "Spiderlings should spawn adjacent to Araxxa")
            assertEquals(2, spiderling.level.value, "Spiderlings should inherit Araxxa's level")
        }
        assertEquals(3, araxxa.summonCooldown.value, "Spiderling summon should start a cooldown")
    }

    @Test
    fun webSpreadsAndBuffsSpidersInsideIt() {
        val state = GameState(createOpenLevel())
        val araxxaPos = Position(5, 4)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
            )
        val spiderling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(araxxaPos),
            )
        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(araxxaPos),
            )
        state.attackers.addAll(listOf(araxxa, spiderling, goblin))

        val abilities = EnemyAbilitySystem(state)
        abilities.processEnemyAbilities()
        val firstWebCount = state.fieldEffects.count { it.type == FieldEffectType.WEB }

        assertTrue(firstWebCount >= 2, "Araxxa should create a web under herself and spread it")
        assertTrue(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxaPos }, "Araxxa's tile should always be webbed")
        assertEquals(1, araxxa.speedBonus.value, "Araxxa should gain a speed bonus while in her web")
        assertEquals(1, spiderling.speedBonus.value, "Spiderlings inside the web should gain a speed bonus")
        assertEquals(0, goblin.speedBonus.value, "Non-spider enemies must not gain the web bonus")

        abilities.processEnemyAbilities()
        val secondWebCount = state.fieldEffects.count { it.type == FieldEffectType.WEB }
        assertTrue(secondWebCount > firstWebCount, "The web area should expand over time")
    }

    @Test
    fun fireAndAcidDestroySpiderWeb() {
        val level = createCombatLevel()

        run {
            val state = GameState(level)
            val engine = GameEngine(state)
            val araxxa =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.ARAXXA,
                    position = mutableStateOf(Position(4, 3)),
                )
            state.attackers.add(araxxa)
            EnemyAbilitySystem(state).processEnemyAbilities()

            assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 1)))
            val wizard = state.defenders.first()
            wizard.buildTimeRemaining.value = 0
            engine.startFirstPlayerTurn()
            wizard.resetActions()

            assertTrue(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value })
            assertTrue(engine.defenderAttackPosition(wizard.id, araxxa.position.value))
            assertFalse(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value }, "Fire should destroy web tiles it hits")
        }

        run {
            val state = GameState(level)
            val engine = GameEngine(state)
            val araxxa =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.ARAXXA,
                    position = mutableStateOf(Position(3, 3)),
                )
            state.attackers.add(araxxa)
            EnemyAbilitySystem(state).processEnemyAbilities()

            assertTrue(engine.placeDefender(DefenderType.ALCHEMY_TOWER, Position(2, 1)))
            val alchemy = state.defenders.first()
            alchemy.buildTimeRemaining.value = 0
            engine.startFirstPlayerTurn()
            alchemy.resetActions()

            assertTrue(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value })
            assertTrue(engine.defenderAttackPosition(alchemy.id, araxxa.position.value))
            assertFalse(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value }, "Acid should destroy web tiles it hits")
        }
    }

    @Test
    fun spiderlingsMergeLikeSnotlings() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val first =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(Position(3, 4)),
            )
        val second =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(Position(4, 4)),
            )
        state.attackers.addAll(listOf(first, second))

        engine.applyMovement(first.id, second.position.value)

        assertTrue(first.isDefeated.value, "Moving spiderling should be absorbed into the existing stack")
        assertTrue(first.wasMerged.value, "Merged spiderlings should not count as real kills")
        assertEquals(10, second.currentHealth.value, "Spiderling stacks should merge their health like snotlings")
    }
}
