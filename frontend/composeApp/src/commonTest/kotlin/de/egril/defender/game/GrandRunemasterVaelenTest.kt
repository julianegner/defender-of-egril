package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.Trap
import de.egril.defender.model.TrapType
import de.egril.defender.model.hexDistanceTo
import de.egril.defender.model.hidesHealthBar
import de.egril.defender.model.isSummoner
import de.egril.defender.model.isUniqueEnemyAlreadyPresent
import de.egril.defender.ui.icon.enemy.enemyAttackPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrandRunemasterVaelenTest {
    @Test
    fun vaelenIsAUniqueBossAndSpawnLoopAnchor() {
        val type = AttackerType.GRAND_RUNEMASTER_VAELEN

        assertTrue(type.isVillain)
        assertTrue(type.isBoss)
        assertTrue(type.showBossHealthBar)
        assertTrue(type.hidesHealthBar)
        assertTrue(type.isSummoner())
        assertEquals("Vaelen", type.villainName)

        val vaelen = Attacker(1, type, mutableStateOf(Position(5, 3)))
        assertTrue(isUniqueEnemyAlreadyPresent(type, listOf(vaelen)))
    }

    @Test
    fun runeGlyphWardProtectsNearbyEnemiesAndStopsWhenVaelenFalls() {
        val state = createState()
        val vaelen = addVaelen(state)
        val nearby = Attacker(2, AttackerType.GOBLIN, mutableStateOf(Position(7, 3)))
        val distant = Attacker(3, AttackerType.GOBLIN, mutableStateOf(Position(10, 3)))
        state.attackers.addAll(listOf(nearby, distant))

        assertEquals(2, vaelen.position.value.hexDistanceTo(nearby.position.value))
        assertEquals(2, state.runeGlyphWardArmor(nearby))
        assertTrue(state.isProtectedByRuneGlyphWard(nearby))
        nearby.movementPenalty.value = 1
        assertEquals(nearby.currentBaseMovementSpeed, calculateEffectiveEnemySpeed(state, nearby, nearby.position.value))
        assertEquals(0, state.runeGlyphWardArmor(distant))
        assertFalse(state.isProtectedByRuneGlyphWard(distant))

        vaelen.isDefeated.value = true
        assertEquals(0, state.runeGlyphWardArmor(nearby))
        assertFalse(state.isProtectedByRuneGlyphWard(nearby))
    }

    @Test
    fun wizardTowerDamageIsHalvedButPhysicalDamageIsNot() {
        val state = createState()
        val vaelen = addVaelen(state)
        val wizard = addTower(state, 1, DefenderType.WIZARD_TOWER, Position(4, 3))
        val nearbyOrk = Attacker(2, AttackerType.ORK, mutableStateOf(Position(6, 3)))
        state.attackers.add(nearbyOrk)
        val combat = CombatSystem(state, BridgeSystem(state))

        assertEquals(15, enemyAttackPreview(vaelen, wizard, hasDoubleLevelBuff = false, gameState = state).damage)
        assertTrue(combat.defenderAttack(wizard.id, vaelen.id) {})
        assertEquals(285, vaelen.currentHealth.value)
        assertEquals(12, nearbyOrk.currentHealth.value)

        val pike = addTower(state, 2, DefenderType.SPIKE_TOWER, Position(4, 3))
        assertEquals(5, enemyAttackPreview(vaelen, pike, hasDoubleLevelBuff = false, gameState = state).damage)
        assertTrue(combat.defenderAttack(pike.id, vaelen.id) {})
        assertEquals(280, vaelen.currentHealth.value)
    }

    @Test
    fun runicSurgeSilencesAtMostTwoNearbyTowersEveryThreeTurns() {
        val state = createState()
        val vaelen = addVaelen(state)
        val first = addTower(state, 1, DefenderType.SPIKE_TOWER, Position(4, 3))
        val second = addTower(state, 2, DefenderType.SPIKE_TOWER, Position(6, 3))
        val distant = addTower(state, 3, DefenderType.SPIKE_TOWER, Position(9, 3))
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))

        abilities.processEnemyAbilities()
        assertTrue(first.isDisabled.value)
        assertTrue(second.isDisabled.value)
        assertFalse(distant.isDisabled.value)
        assertEquals(2, first.disabledTurnsRemaining.value)
        assertEquals(3, vaelen.summonCooldown.value)

        repeat(2) { abilities.processEnemyAbilities() }
        assertEquals(1, vaelen.summonCooldown.value)
        abilities.processEnemyAbilities()
        assertEquals(3, vaelen.summonCooldown.value)
    }

    @Test
    fun vaelenIgnoresMagicalTrapTeleport() {
        val state = createState()
        val vaelen = addVaelen(state)
        val originalPosition = vaelen.position.value
        state.traps.add(Trap(position = originalPosition, damage = 0, defenderId = 1, type = TrapType.MAGICAL))

        GameEngine(state).checkAndActivateTraps()

        assertEquals(originalPosition, vaelen.position.value)
    }

    private fun createState(): GameState {
        val allCells = (0 until 12).flatMap { x -> (0 until 6).map { y -> Position(x, y) } }.toSet()
        val level =
            Level(
                id = 1,
                name = "Vaelen Test",
                gridWidth = 12,
                gridHeight = 6,
                startPositions = listOf(Position(0, 3)),
                targetPositions = listOf(Position(11, 3)),
                pathCells = allCells,
                attackerWaves = emptyList(),
                initialCoins = 1000,
                healthPoints = 10,
            )
        return GameState(level)
    }

    private fun addVaelen(state: GameState): Attacker =
        Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GRAND_RUNEMASTER_VAELEN,
            position = mutableStateOf(Position(5, 3)),
        ).also { state.attackers.add(it) }

    private fun addTower(
        state: GameState,
        id: Int,
        type: DefenderType,
        position: Position,
    ): Defender =
        Defender(
            id = id,
            type = type,
            position = mutableStateOf(position),
            actionsRemaining = mutableStateOf(1),
        ).also { state.defenders.add(it) }
}
