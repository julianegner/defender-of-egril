package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObsidianProtectorAuraTest {
    private fun createLevel(): Level {
        val cells = (0 until 10).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Obsidian Protector Aura Test",
            gridWidth = 10,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = cells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun attacker(id: Int, type: AttackerType, position: Position) =
        Attacker(id, type, mutableStateOf(position), mutableStateOf(1))

    private fun defender(position: Position) =
        Defender(
            id = 1,
            type = DefenderType.BOW_TOWER,
            position = mutableStateOf(position),
            actionsRemaining = mutableStateOf(2),
            buildTimeRemaining = mutableStateOf(0),
        )

    @Test
    fun protectsUnitsWithinTwoHexesButNotProtectorOrDistantUnits() {
        val state = GameState(createLevel())
        val protector = attacker(1, AttackerType.OBSIDIAN_PROTECTOR, Position(4, 3))
        val nearby = attacker(2, AttackerType.GOBLIN, Position(6, 3))
        val distant = attacker(3, AttackerType.GOBLIN, Position(7, 3))
        state.attackers.addAll(listOf(protector, nearby, distant))

        assertFalse(state.isProtectedByObsidianProtector(protector))
        assertTrue(state.canManuallyTargetAttacker(protector))
        assertTrue(state.isProtectedByObsidianProtector(nearby))
        assertFalse(state.canManuallyTargetAttacker(nearby))
        assertFalse(state.isProtectedByObsidianProtector(distant))
        assertTrue(state.canManuallyTargetAttacker(distant))
        assertTrue(Position(6, 3) in state.obsidianProtectorAuraPositions())
        assertFalse(Position(7, 3) in state.obsidianProtectorAuraPositions())
    }

    @Test
    fun auraMovesAndEndsWhenProtectorIsDefeated() {
        val state = GameState(createLevel())
        val protector = attacker(1, AttackerType.OBSIDIAN_PROTECTOR, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(6, 3))
        state.attackers.addAll(listOf(protector, goblin))

        assertTrue(state.isProtectedByObsidianProtector(goblin))
        protector.position.value = Position(1, 3)
        assertFalse(state.isProtectedByObsidianProtector(goblin))
        protector.position.value = Position(4, 3)
        protector.isDefeated.value = true
        assertFalse(state.isProtectedByObsidianProtector(goblin))
    }

    @Test
    fun towersCanAttackProtectorButCannotDamageProtectedUnits() {
        val state = GameState(createLevel())
        val engine = GameEngine(state)
        val tower = defender(Position(3, 3))
        val protector = attacker(1, AttackerType.OBSIDIAN_PROTECTOR, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(5, 3))
        state.defenders.add(tower)
        state.attackers.addAll(listOf(protector, goblin))

        assertTrue(engine.defenderAttack(tower.id, protector.id))
        assertTrue(engine.defenderAttack(tower.id, goblin.id))

        assertEquals(protector.maxHealth - tower.type.baseDamage, protector.currentHealth.value)
        assertEquals(goblin.maxHealth, goblin.currentHealth.value)
    }

    @Test
    fun areaAttacksDamageProtectorButNotProtectedUnits() {
        val state = GameState(createLevel())
        val engine = GameEngine(state)
        val wizard =
            Defender(
                id = 1,
                type = DefenderType.WIZARD_TOWER,
                position = mutableStateOf(Position(3, 3)),
                actionsRemaining = mutableStateOf(1),
                buildTimeRemaining = mutableStateOf(0),
            )
        val protector = attacker(1, AttackerType.OBSIDIAN_PROTECTOR, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(5, 3))
        state.defenders.add(wizard)
        state.attackers.addAll(listOf(protector, goblin))

        assertTrue(engine.defenderAttack(wizard.id, protector.id))

        assertEquals(protector.maxHealth - wizard.type.baseDamage, protector.currentHealth.value)
        assertEquals(goblin.maxHealth, goblin.currentHealth.value)
    }

    @Test
    fun autoAttackPrioritizesProtectorOverProtectedAllies() {
        val state = GameState(createLevel())
        val protector = attacker(1, AttackerType.OBSIDIAN_PROTECTOR, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(5, 3))
        state.attackers.addAll(listOf(protector, goblin))
        val selector =
            AutoAttackSelector(
                state = state,
                getEffectiveRange = { it.range },
                findClosestTargetPosition = { Position(9, 3) },
            )

        val selected = selector.selectAutoTargetForDefender(defender(Position(3, 3)), state.attackers)

        assertEquals(protector.id, selected?.id)
    }
}
