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
    fun rearAttacksAreBlockedWhenFreyaTurnsShieldToMostDangerousTower() {
        // Freya moves East, but the only threat is behind her, so she turns the shield West to face it.
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val rearTower = defender(1, DefenderType.BOW_TOWER, Position(1, 3))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.defenders.add(rearTower)
        state.attackers.add(freya)

        assertTrue(engine.defenderAttack(rearTower.id, freya.id), "Rear tower should be allowed to attack Freya")

        assertEquals(freya.maxHealth, freya.currentHealth.value, "Shield should turn toward the most dangerous tower")
    }

    @Test
    fun shieldKeepsMovementDirectionWhenItBlocksAsMuchDamageAsTheDangerousTower() {
        // Eastern tower is equally dangerous and listed first, so the tie keeps Freya facing her movement direction.
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val eastTower = defender(1, DefenderType.BOW_TOWER, Position(6, 3))
        val rearTower = defender(2, DefenderType.BOW_TOWER, Position(1, 3))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.defenders.addAll(listOf(eastTower, rearTower))
        state.attackers.add(freya)

        assertTrue(engine.defenderAttack(rearTower.id, freya.id))
        assertEquals(freya.maxHealth - rearTower.type.baseDamage, freya.currentHealth.value, "Tie should keep the movement-facing shield")
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
    fun northAndSouthTowersAreBlockedSymmetrically() {
        // Freya is moving East on row 3 (odd row). Her NW flank is (4,2) and SW flank is (4,4).
        // Towers placed "due north" (same column, rows above) and "due south" (same column, rows
        // below) are side attackers and must behave the same regardless of hex tie-breaking.
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        // Place towers far enough to have range but clearly north/south of Freya.
        val northTower = defender(1, DefenderType.BOW_TOWER, Position(4, 1), actions = 3)
        val southTower = defender(2, DefenderType.BOW_TOWER, Position(4, 5), actions = 3)
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.defenders.addAll(listOf(northTower, southTower))
        state.attackers.add(freya)

        // Both towers may attack Freya. Freya turns her shield toward one equally dangerous tower,
        // so exactly one of the two hits go through.
        val northDamaged = engine.defenderAttack(northTower.id, freya.id)
        val southDamaged = engine.defenderAttack(southTower.id, freya.id)

        assertTrue(northDamaged, "North tower should be allowed to attack Freya")
        assertTrue(southDamaged, "South tower should be allowed to attack Freya")

        assertEquals(
            freya.maxHealth - northTower.type.baseDamage,
            freya.currentHealth.value,
            "Exactly one of the equally dangerous side towers should be blocked by the turned shield",
        )
    }

    @Test
    fun northTowerIsBlockedFromAttackingGoblinBehindShield() {
        // Freya at (4,3) moving East.  Her NW-flank tile is (4,2) — directly above her on the same
        // column (odd-row hex grid).  A tower to the north at (4,1) tries to attack a goblin at
        // (3,3) which is directly WEST of Freya.  The hex line from the tower to the goblin passes
        // through the NW-flank shield tile, so the attack must be blocked.
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val northTower = defender(1, DefenderType.BOW_TOWER, Position(4, 1))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(3, 3))

        state.defenders.add(northTower)
        state.attackers.addAll(listOf(freya, goblin))

        assertTrue(engine.defenderAttack(northTower.id, goblin.id), "Tower should be allowed to target the goblin")
        assertEquals(
            goblin.maxHealth,
            goblin.currentHealth.value,
            "Goblin west of Freya should be shielded from north tower (shield line of sight)",
        )
    }

    @Test
    fun southTowerIsAlsoBlockedFromAttackingGoblinBehindShield() {
        // Mirror of the north case: south tower at (4,5) → goblin at (3,3).
        // The hex line passes through the SW-flank at (4,4), blocking the attack.
        // An equally dangerous east tower keeps the shield facing the movement direction (tie-break).
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val eastTower = defender(2, DefenderType.BOW_TOWER, Position(6, 3))
        val southTower = defender(1, DefenderType.BOW_TOWER, Position(4, 5))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(3, 3))

        state.defenders.addAll(listOf(eastTower, southTower))
        state.attackers.addAll(listOf(freya, goblin))

        assertTrue(engine.defenderAttack(southTower.id, goblin.id), "Tower should be allowed to target the goblin")
        assertEquals(
            goblin.maxHealth,
            goblin.currentHealth.value,
            "Goblin west of Freya should be shielded from south tower (shield line of sight)",
        )
    }

    @Test
    fun autoAttackSkipsShieldedFreyaAndHitsAnotherTarget() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val frontTower = defender(1, DefenderType.BOW_TOWER, Position(6, 3))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val goblin = attacker(2, AttackerType.GOBLIN, Position(7, 3))

        state.defenders.add(frontTower)
        state.attackers.addAll(listOf(freya, goblin))

        assertEquals(
            goblin.position.value,
            engine.getNextAutoAttackTargetPosition(frontTower),
            "Auto-attack should ignore Freya when the shield wall blocks all damage",
        )
        assertTrue(engine.performOneAutoAttack(frontTower.id), "Auto-attack should still fire at another valid target")
        assertEquals(freya.maxHealth, freya.currentHealth.value, "Shielded Freya should remain unharmed")
        assertEquals(
            goblin.maxHealth - frontTower.type.baseDamage,
            goblin.currentHealth.value,
            "Auto-attack should damage the alternate unshielded target",
        )
    }

    @Test
    fun autoAttackDoesNothingWhenOnlyShieldedTargetsAreAvailable() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val frontTower = defender(1, DefenderType.BOW_TOWER, Position(6, 3))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))

        state.defenders.add(frontTower)
        state.attackers.add(freya)

        assertEquals(null, engine.getNextAutoAttackTargetPosition(frontTower))
        assertFalse(engine.performOneAutoAttack(frontTower.id), "Auto-attack should not waste an action on a fully blocked target")
        assertEquals(1, frontTower.actionsRemaining.value, "Blocked auto-attacks should keep the tower action available")
        assertEquals(freya.maxHealth, freya.currentHealth.value, "Shielded Freya should take no damage")
    }

    @Test
    fun areaAutoAttackSkipsShieldedClusterAndTargetsDamageableEnemy() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val wizard = defender(1, DefenderType.WIZARD_TOWER, Position(6, 3))
        val freya = attacker(1, AttackerType.FALLEN_SHIELDMAIDEN_FREYA, Position(4, 3))
        val shieldedGoblin = attacker(2, AttackerType.GOBLIN, Position(3, 3))
        val openGoblin = attacker(3, AttackerType.GOBLIN, Position(7, 3))

        state.defenders.add(wizard)
        state.attackers.addAll(listOf(freya, shieldedGoblin, openGoblin))

        assertEquals(
            openGoblin.position.value,
            engine.getNextAutoAttackTargetPosition(wizard),
            "Area auto-attack should skip shielded targets when they would take no damage",
        )
        assertTrue(engine.performOneAutoAttack(wizard.id), "Wizard should auto-attack a damageable target")
        assertEquals(freya.maxHealth, freya.currentHealth.value, "Shielded Freya should remain unharmed")
        assertEquals(shieldedGoblin.maxHealth, shieldedGoblin.currentHealth.value, "Enemies behind the shield should remain unharmed")
        assertEquals(
            openGoblin.maxHealth - wizard.type.baseDamage,
            openGoblin.currentHealth.value,
            "Area auto-attack should damage the unshielded enemy instead",
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
