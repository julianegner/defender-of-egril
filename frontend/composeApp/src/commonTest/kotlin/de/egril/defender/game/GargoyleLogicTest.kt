package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.TileType
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Gargoyle
import de.egril.defender.model.GargoyleRules
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GargoyleLogicTest {
    private fun createState(): GameState {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        for (x in 1..9) tiles["$x,0"] = TileType.PATH
        tiles["10,0"] = TileType.TARGET
        val level =
            Level(
                id = 1,
                name = "Gargoyle Test",
                pathCells = (1..9).map { Position(it, 0) }.toSet(),
                buildAreas = emptySet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(10, 0)),
            )
        return GameState(level = level)
    }

    private fun addGargoyle(
        state: GameState,
        position: Position,
        level: Int = 1,
    ): Gargoyle {
        val gargoyle =
            Gargoyle(
                id = state.nextGargoyleId.value++,
                position = mutableStateOf(position),
                level = mutableStateOf(level),
                currentHealth = mutableStateOf(GargoyleRules.HEALTH_PER_LEVEL * level),
            )
        state.gargoyles.add(gargoyle)
        return gargoyle
    }

    private fun addEnemy(
        state: GameState,
        type: AttackerType,
        position: Position,
        health: Int? = null,
    ): Attacker {
        val attacker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(position),
                currentHealth = mutableStateOf(health ?: type.health),
            )
        state.attackers.add(attacker)
        return attacker
    }

    private fun logic(state: GameState, enemyDamage: Int = 1) =
        GargoyleLogic(
            state = state,
            getInitialTarget = { Position(10, 0) },
            getEnemyDamage = { enemyDamage },
        )

    @Test
    fun inactiveGargoyleAttacksTwoNearestEnemiesInRangeForFiftyPerLevel() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0), level = 2)
        val near = addEnemy(state, AttackerType.GOBLIN, Position(6, 0))
        val far = addEnemy(state, AttackerType.GOBLIN, Position(7, 0))
        val outOfRange = addEnemy(state, AttackerType.GOBLIN, Position(9, 0))

        logic(state).performInactiveAttacks()

        assertEquals(AttackerType.GOBLIN.health - 100, near.currentHealth.value)
        assertEquals(AttackerType.GOBLIN.health - 100, far.currentHealth.value)
        assertEquals(AttackerType.GOBLIN.health, outOfRange.currentHealth.value)
        assertEquals(GargoyleRules.HEALTH_PER_LEVEL * 2, gargoyle.currentHealth.value)
    }

    @Test
    fun gargoyleActivatesWhenEvilWizardIsWithinTwoTiles() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0))
        addEnemy(state, AttackerType.EVIL_WIZARD, Position(7, 0))

        logic(state).processEnemyTurnEnd()

        assertTrue(gargoyle.isActive.value)
        assertEquals(GargoyleRules.ACTIVE_TURNS, gargoyle.activeTurnsRemaining.value)
        val activeForm = state.attackers.first { it.id == gargoyle.activeAttackerId.value }
        assertEquals(AttackerType.GARGOYLE, activeForm.type)
        assertEquals(Position(5, 0), activeForm.position.value)
    }

    @Test
    fun gargoyleDoesNotActivateForWizardAtThreeTiles() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0))
        addEnemy(state, AttackerType.EVIL_WIZARD, Position(8, 0))

        logic(state).processEnemyTurnEnd()

        assertFalse(gargoyle.isActive.value)
        assertTrue(state.attackers.none { it.type == AttackerType.GARGOYLE })
    }

    @Test
    fun activeGargoyleReturnsToInactiveAfterThreeEnemyTurnsKeepingHealthAndPosition() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0))
        val wizard = addEnemy(state, AttackerType.EVIL_WIZARD, Position(7, 0))
        val logic = logic(state)
        logic.processEnemyTurnEnd()
        val activeForm = state.attackers.first { it.type == AttackerType.GARGOYLE }
        activeForm.currentHealth.value = 400
        activeForm.position.value = Position(7, 0)
        wizard.isDefeated.value = true
        state.attackers.removeAll { it.isDefeated.value }

        logic.processEnemyTurnEnd()
        logic.processEnemyTurnEnd()
        assertTrue(gargoyle.isActive.value)
        logic.processEnemyTurnEnd()

        assertFalse(gargoyle.isActive.value)
        assertTrue(state.attackers.none { it.type == AttackerType.GARGOYLE })
        assertEquals(400, gargoyle.currentHealth.value)
        assertEquals(Position(7, 0), gargoyle.position.value)
    }

    @Test
    fun gargoyleReactivatesWhileWizardStaysInRange() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0))
        addEnemy(state, AttackerType.EVIL_WIZARD, Position(6, 0))
        val logic = logic(state)

        repeat(4) { logic.processEnemyTurnEnd() }
        assertFalse(gargoyle.isActive.value, "Deactivates after the active turns run out")

        logic.processEnemyTurnEnd()
        assertTrue(gargoyle.isActive.value, "Reactivates on the next turn while a wizard stays in range")
        assertEquals(1, state.attackers.count { it.type == AttackerType.GARGOYLE })
    }

    @Test
    fun adjacentEnemiesDamageInactiveGargoyleAndDestroyingItGivesNoCoins() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0))
        gargoyle.currentHealth.value = 2
        addEnemy(state, AttackerType.GOBLIN, Position(6, 0))
        addEnemy(state, AttackerType.GOBLIN, Position(4, 0))
        val engine = GameEngine(state)
        val coinsBefore = state.coins.value

        logic(state, enemyDamage = 1).processEnemyTurnEnd()
        assertFalse(state.gargoyles.contains(gargoyle))
        engine.processDefeatedAttackers()
        assertEquals(coinsBefore, state.coins.value)
        assertEquals(0, engine.getKillsThisTurn())
    }

    @Test
    fun destroyedActiveGargoyleGivesNoCoinsAndIsRemoved() {
        val state = createState()
        val gargoyle = addGargoyle(state, Position(5, 0))
        addEnemy(state, AttackerType.EVIL_WIZARD, Position(7, 0))
        val logic = logic(state)
        logic.processEnemyTurnEnd()
        val activeForm = state.attackers.first { it.type == AttackerType.GARGOYLE }
        activeForm.isDefeated.value = true
        val engine = GameEngine(state)
        val coinsBefore = state.coins.value

        engine.processDefeatedAttackers()
        logic.processEnemyTurnEnd()

        assertEquals(coinsBefore, state.coins.value)
        assertEquals(0, engine.getKillsThisTurn())
        assertFalse(state.gargoyles.contains(gargoyle))
        assertNull(state.attackers.firstOrNull { it.type == AttackerType.GARGOYLE })
    }
}
