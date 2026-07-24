package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.getSoulCallResurrectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValeriusSoulCallTest {
    private fun createTestLevel(): Level =
        Level(
            id = 1,
            name = "Valerius Test",
            gridWidth = 12,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(11, 3)),
            pathCells = (0..11).map { Position(it, 3) }.toSet(),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            initialCoins = 100,
            healthPoints = 10,
        )

    @Test
    fun valeriusIsConfiguredAsUndeadVillain() {
        val type = AttackerType.PRINCE_VALERIUS_THE_SOULREAPER

        assertTrue(type.isVillain)
        assertEquals("Valerius", type.villainName)
        assertEquals(3, type.soulCallRange)
        assertEquals(AttackerType.SKELETON, AttackerType.ZOMBIE.getSoulCallResurrectionType())
        assertEquals(AttackerType.UNDEAD_DRAGON, AttackerType.DRAGON.getSoulCallResurrectionType())
        assertEquals(null, AttackerType.SKELETON.getSoulCallResurrectionType())
        assertEquals(null, AttackerType.ROBOTIC_GOBLIN.getSoulCallResurrectionType())
    }

    @Test
    fun soulCallRaisesDeadGoblinAsZombieAtStartOfNextRound() {
        val state = GameState(createTestLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        state.turnNumber.value = 1
        val combatSystem = CombatSystem(state, BridgeSystem(state))
        val engine = GameEngine(state)

        val valerius =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.PRINCE_VALERIUS_THE_SOULREAPER,
                position = mutableStateOf(Position(4, 3)),
            )
        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(6, 3)),
                currentTarget = mutableStateOf(Position(11, 3)),
            )
        state.attackers.addAll(listOf(valerius, goblin))

        goblin.isDefeated.value = true
        combatSystem.processDefeatedAttackers()

        assertEquals(1, state.pendingSoulCalls.size)
        assertEquals(AttackerType.ZOMBIE, state.pendingSoulCalls.single().attackerType)

        engine.startEnemyTurn()

        val resurrected = state.attackers.single { it.type == AttackerType.ZOMBIE }
        assertEquals(Position(6, 3), resurrected.position.value)
        assertEquals(1, resurrected.level.value)
        assertTrue(state.pendingSoulCalls.isEmpty())
    }

    @Test
    fun soulCallRaisesDragonAsUndeadDragonThatDoesNotEatUnits() {
        val state = GameState(createTestLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        state.turnNumber.value = 1
        val combatSystem = CombatSystem(state, BridgeSystem(state))
        val engine = GameEngine(state)

        val valerius =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.PRINCE_VALERIUS_THE_SOULREAPER,
                position = mutableStateOf(Position(4, 3)),
            )
        val dragon =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.DRAGON,
                position = mutableStateOf(Position(6, 3)),
                level = mutableStateOf(10),
                currentHealth = mutableStateOf(5000),
                dragonName = "Smoulder",
                currentTarget = mutableStateOf(Position(11, 3)),
            )
        state.attackers.addAll(listOf(valerius, dragon))

        dragon.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        engine.startEnemyTurn()

        val undeadDragon = state.attackers.single { it.type == AttackerType.UNDEAD_DRAGON }
        val adjacentGoblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(5, 3)),
                currentHealth = mutableStateOf(20),
            )
        state.attackers.add(adjacentGoblin)

        val healthBefore = undeadDragon.currentHealth.value
        engine.applyMovement(undeadDragon.id, undeadDragon.position.value)

        assertEquals("Smoulder", undeadDragon.dragonName)
        assertEquals(healthBefore, undeadDragon.currentHealth.value)
        assertFalse(adjacentGoblin.isDefeated.value, "Undead dragons must not eat adjacent units")
        assertEquals(0, undeadDragon.greed, "Undead dragons should not use dragon greed mechanics")
    }

    @Test
    fun soulCallSkeletonSpawnSuppressesPortalHighlight() {
        val state = GameState(createTestLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        state.turnNumber.value = 1
        val combatSystem = CombatSystem(state, BridgeSystem(state))
        val engine = GameEngine(state)

        val valerius =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.PRINCE_VALERIUS_THE_SOULREAPER,
                position = mutableStateOf(Position(4, 3)),
            )
        val zombie =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ZOMBIE,
                position = mutableStateOf(Position(6, 3)),
                currentTarget = mutableStateOf(Position(11, 3)),
            )
        state.attackers.addAll(listOf(valerius, zombie))

        zombie.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        engine.startEnemyTurn()

        assertEquals(AttackerType.SKELETON, state.enemySpawnEffects.single().attackerType)
        assertTrue(state.enemySpawnEffects.single().suppressPortalAnimation)
    }
}
