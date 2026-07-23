package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreyaShieldWallTest {
    private fun createTestLevel(): Level =
        Level(
            id = 1,
            name = "Freya Shield Wall Test",
            gridWidth = 10,
            gridHeight = 7,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.FALLEN_SHIELDMAIDEN_FREYA))),
            initialCoins = 1000,
            healthPoints = 10,
        )

    private fun attacker(
        id: Int,
        type: AttackerType,
        position: Position,
    ) = Attacker(id, type, mutableStateOf(position), mutableStateOf(1))

    private fun defender(
        id: Int,
        type: DefenderType,
        position: Position,
        actions: Int = 1,
    ) = Defender(
        id = id,
        type = type,
        position = mutableStateOf(position),
        level = mutableStateOf(1),
        buildTimeRemaining = mutableStateOf(0),
        actionsRemaining = mutableStateOf(actions),
    )

    @Test
    fun frontAttacksAreBlockedForFreyaAndShieldFlanks() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val frontTower = defender(1, DefenderType.BOW_TOWER, Position(6, 3), actions = 2)
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val upperSkeleton = attacker(2, AttackerType.SKELETON, Position(4, 2))

        state.defenders.add(frontTower)
        state.attackers.addAll(listOf(freya, upperSkeleton))

        assertTrue(engine.defenderAttack(frontTower.id, freya.id), "Front tower should be allowed to attack Freya")
        assertTrue(engine.defenderAttack(frontTower.id, upperSkeleton.id), "Front tower should be allowed to attack the protected flank unit")

        assertEquals(freya.maxHealth, freya.currentHealth.value, "Freya should block frontal damage on herself")
        assertEquals(upperSkeleton.maxHealth, upperSkeleton.currentHealth.value, "Freya should block frontal damage for the flank tile")
    }

    @Test
    fun rearAttacksStillDamageFreya() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val rearTower = defender(1, DefenderType.BOW_TOWER, Position(1, 3))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.defenders.add(rearTower)
        state.attackers.add(freya)

        assertTrue(engine.defenderAttack(rearTower.id, freya.id), "Rear tower should be allowed to attack Freya")

        assertEquals(freya.maxHealth - rearTower.type.baseDamage, freya.currentHealth.value, "Rear attacks should not be blocked")
    }

    @Test
    fun sideAttacksStillDamageShieldFlank() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val sideTower = defender(1, DefenderType.BOW_TOWER, Position(1, 2))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val upperSkeleton = attacker(2, AttackerType.SKELETON, Position(4, 2))

        state.defenders.add(sideTower)
        state.attackers.addAll(listOf(freya, upperSkeleton))

        assertTrue(sideTower.canAttack(upperSkeleton), "Side tower should be in range of the protected flank tile")
        assertTrue(engine.defenderAttack(sideTower.id, upperSkeleton.id), "Side tower should be allowed to attack the protected flank tile")

        assertEquals(
            upperSkeleton.maxHealth - sideTower.type.baseDamage,
            upperSkeleton.currentHealth.value,
            "Attacks from the side should not be blocked for the flank tile",
        )
    }

    @Test
    fun ballistaBypassesShieldWallFromTheFront() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val ballista = defender(1, DefenderType.BALLISTA_TOWER, Position(7, 3), actions = 2)
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val lowerSkeleton = attacker(2, AttackerType.SKELETON, Position(4, 4))

        state.defenders.add(ballista)
        state.attackers.addAll(listOf(freya, lowerSkeleton))

        assertTrue(engine.defenderAttack(ballista.id, freya.id), "Ballista should be allowed to attack Freya")
        assertTrue(engine.defenderAttack(ballista.id, lowerSkeleton.id), "Ballista should be allowed to attack the protected flank tile")

        assertEquals(freya.maxHealth - ballista.type.baseDamage, freya.currentHealth.value, "Ballista shots should bypass the shield wall")
        assertEquals(lowerSkeleton.maxHealth - ballista.type.baseDamage, lowerSkeleton.currentHealth.value, "Ballista shots should bypass the shield wall for flank tiles too")
    }

    @Test
    fun visibleShieldWallMarksBothFlankTiles() {
        val state = GameState(createTestLevel())
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.attackers.add(freya)

        assertEquals(
            setOf(Position(4, 2), Position(4, 4)),
            state.freyaShieldWallVisiblePositions(),
            "Freya should render shield wall indicators on both flank tiles",
        )
    }

    @Test
    fun visibleShieldWallOverlaysUseFreyaFrontDirectionForEachFlankTile() {
        val state = GameState(createTestLevel())
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.attackers.add(freya)

        assertEquals(
            mapOf(
                Position(4, 2) to FreyaShieldWallOverlay(frontDirection = 0),
                Position(4, 4) to FreyaShieldWallOverlay(frontDirection = 0),
            ),
            state.freyaShieldWallVisibleOverlays(),
            "Freya's visible wall overlays should carry the same front direction for both flank tiles",
        )
    }

    @Test
    fun defeatedFreyaDoesNotRenderShieldWallTiles() {
        val state = GameState(createTestLevel())
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        freya.isDefeated.value = true

        state.attackers.add(freya)

        assertFalse(
            state.freyaShieldWallVisiblePositions().isNotEmpty(),
            "Defeated Freya should no longer render shield wall indicators",
        )
    }
}
