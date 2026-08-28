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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoAttackDamageabilityTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 10).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Auto Attack Damageability Test",
            gridWidth = 10,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun defender(
        id: Int,
        type: DefenderType,
        position: Position,
        actions: Int = 1,
    ) = Defender(
        id = id,
        type = type,
        position = mutableStateOf(position),
        actionsRemaining = mutableStateOf(actions),
        buildTimeRemaining = mutableStateOf(0),
    )

    private fun attacker(
        id: Int,
        type: AttackerType,
        position: Position,
    ) = Attacker(id, type, mutableStateOf(position), mutableStateOf(1))

    @Test
    fun rangedAutoAttackSkipsImmuneTrollAndHitsDamageableEnemy() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val tower = defender(1, DefenderType.BOW_TOWER, Position(3, 3))
        val troll = attacker(1, AttackerType.TROLL, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(5, 3))

        state.defenders.add(tower)
        state.attackers.addAll(listOf(troll, goblin))

        assertEquals(goblin.position.value, engine.getNextAutoAttackTargetPosition(tower))
        assertTrue(engine.performOneAutoAttack(tower.id))
        assertEquals(troll.maxHealth, troll.currentHealth.value, "Immune troll should not be auto-targeted by ranged attack")
        assertEquals(goblin.maxHealth - tower.type.baseDamage, goblin.currentHealth.value)
    }

    @Test
    fun rangedAutoAttackDoesNothingWhenOnlyTrollIsInRange() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val tower = defender(1, DefenderType.BOW_TOWER, Position(3, 3))
        val troll = attacker(1, AttackerType.TROLL, Position(4, 3))

        state.defenders.add(tower)
        state.attackers.add(troll)

        assertNull(engine.getNextAutoAttackTargetPosition(tower))
        assertFalse(engine.performOneAutoAttack(tower.id))
        assertEquals(1, tower.actionsRemaining.value, "No-damage auto-attack must not consume actions")
        assertEquals(troll.maxHealth, troll.currentHealth.value)
    }

    @Test
    fun areaAutoAttackDoesNothingWhenOnlyFireballImmuneEnemyIsAvailable() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val wizard = defender(1, DefenderType.WIZARD_TOWER, Position(3, 3))
        val redDemon = attacker(1, AttackerType.RED_DEMON, Position(5, 3))

        state.defenders.add(wizard)
        state.attackers.add(redDemon)

        assertNull(engine.getNextAutoAttackTargetPosition(wizard))
        assertFalse(engine.performOneAutoAttack(wizard.id))
        assertEquals(1, wizard.actionsRemaining.value, "No-damage area auto-attack must not consume actions")
        assertEquals(redDemon.maxHealth, redDemon.currentHealth.value)
    }

    @Test
    fun lastingAutoAttackDoesNothingWhenOnlyAcidImmuneEnemyIsAvailable() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val alchemy = defender(1, DefenderType.ALCHEMY_TOWER, Position(3, 3))
        val blueDemon = attacker(1, AttackerType.BLUE_DEMON, Position(4, 3))

        state.defenders.add(alchemy)
        state.attackers.add(blueDemon)

        assertNull(engine.getNextAutoAttackTargetPosition(alchemy))
        assertFalse(engine.performOneAutoAttack(alchemy.id))
        assertEquals(1, alchemy.actionsRemaining.value, "No-damage lasting auto-attack must not consume actions")
        assertEquals(blueDemon.maxHealth, blueDemon.currentHealth.value)
    }

    @Test
    fun autoAttackMayTargetMirrorImageWhenItIsTheOnlyVisibleTarget() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val tower = defender(1, DefenderType.BOW_TOWER, Position(3, 3))
        val mirror = attacker(1, AttackerType.SILAS_MIRROR_IMAGE, Position(4, 3))

        state.defenders.add(tower)
        state.attackers.add(mirror)

        assertEquals(mirror.position.value, engine.getNextAutoAttackTargetPosition(tower))
        assertTrue(engine.performOneAutoAttack(tower.id))
        assertTrue(tower.isDisabled.value, "Hitting a mirror should apply Silas mirror blind effect")
        assertTrue(mirror.isDefeated.value, "Mirror image should vanish when hit")
    }

    @Test
    fun wizardAutoAttackGeneratesManaWhenNoTargetAndNoMagicalTrapAreAvailable() {
        val pathCells = setOf(Position(0, 0), Position(29, 29))
        val level =
            Level(
                id = 1,
                name = "Wizard Auto Mana Test",
                gridWidth = 30,
                gridHeight = 30,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(29, 29)),
                pathCells = pathCells,
                attackerWaves = emptyList(),
                initialCoins = 1000,
                healthPoints = 10,
            )
        val state = GameState(level, phase = mutableStateOf(de.egril.defender.model.GamePhase.PLAYER_TURN))
        state.maxMana.value = 20
        state.currentMana.value = 0

        val engine = GameEngine(state)
        val wizard = defender(1, DefenderType.WIZARD_TOWER, Position(15, 15)).apply { this.level.value = 10 }
        val distantEnemy = attacker(1, AttackerType.GOBLIN, Position(0, 0))

        state.defenders.add(wizard)
        state.attackers.add(distantEnemy)

        assertTrue(state.hasDefendersForAutoAttack(), "Wizard should make auto-attack available when it can auto-generate mana")
        assertFalse(state.canWizardPlaceAnyMagicalTrap(wizard), "Wizard should have no valid magical trap tile in range")
        assertTrue(engine.performOneAutoAttack(wizard.id), "Wizard should spend its action on mana generation")
        assertEquals(7, state.currentMana.value, "Wizard mana generation should use the normal generate-mana action")
        assertEquals(0, wizard.actionsRemaining.value, "Mana generation should consume the wizard action")
        assertTrue(state.getDefenderTypesWithSpecialActions().isEmpty(), "Unavailable magical traps should not be reported as remaining special actions")
        assertEquals(distantEnemy.maxHealth, distantEnemy.currentHealth.value, "No enemy should be damaged when mana generation is used")
    }

    @Test
    fun wizardAutoAttackDoesNotGenerateManaWhenMagicalTrapIsAvailable() {
        val state = GameState(createOpenLevel(), phase = mutableStateOf(de.egril.defender.model.GamePhase.PLAYER_TURN))
        state.maxMana.value = 20
        state.currentMana.value = 0

        val engine = GameEngine(state)
        val wizard = defender(1, DefenderType.WIZARD_TOWER, Position(3, 3)).apply { this.level.value = 10 }
        val distantEnemy = attacker(1, AttackerType.GOBLIN, Position(9, 3))

        state.defenders.add(wizard)
        state.attackers.add(distantEnemy)

        assertTrue(state.canWizardPlaceAnyMagicalTrap(wizard), "Wizard should still have a manual magical trap option")
        assertFalse(engine.performOneAutoAttack(wizard.id), "Auto-attack should not consume the action when a manual trap is available")
        assertEquals(0, state.currentMana.value)
        assertEquals(1, wizard.actionsRemaining.value)
        assertEquals(listOf(DefenderType.WIZARD_TOWER), state.getDefenderTypesWithSpecialActions())
    }
}
