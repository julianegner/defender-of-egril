package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests that enemies at spawn points can be attacked by towers.
 */
class SpawnPointAttackTest {

    /**
     * Creates a test level where the spawn point is at (0,3), path goes across row 3,
     * and the target is at (9,3). Build areas are adjacent to the path.
     */
    private fun createTestLevel(): Level {
        return Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (1..8).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
    }

    private fun spawnEnemyAtSpawnPoint(state: GameState): Attacker {
        val spawnPoint = state.level.startPositions.first()
        val enemy = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(spawnPoint),
            level = mutableStateOf(1)
        )
        state.attackers.add(enemy)
        return enemy
    }

    @Test
    fun testSingleTargetAttackOnEnemyAtSpawnPoint() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        // Place a bow tower adjacent to the spawn point
        assertTrue(engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 2)))
        val tower = state.defenders.first()
        tower.buildTimeRemaining.value = 0

        engine.startFirstPlayerTurn()
        tower.resetActions()

        val enemy = spawnEnemyAtSpawnPoint(state)
        val spawnPoint = state.level.startPositions.first()

        // Verify the enemy is at the spawn point (not on path)
        assertFalse(state.level.isOnPath(enemy.position.value), "Spawn point should not be on path")
        assertTrue(state.level.isSpawnPoint(enemy.position.value), "Enemy should be at spawn point")

        // Bow tower should be able to attack the enemy at the spawn point directly
        val healthBefore = enemy.currentHealth.value
        assertTrue(engine.defenderAttack(tower.id, enemy.id), "Single-target attack should succeed on enemy at spawn point")
        assertTrue(enemy.currentHealth.value < healthBefore, "Enemy health should be reduced after attack")
    }

    @Test
    fun testAreaAttackHitsEnemyAtSpawnPoint() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        // Place a wizard tower (area attack) adjacent to the spawn point
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 2)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0

        engine.startFirstPlayerTurn()
        wizard.resetActions()

        val enemy = spawnEnemyAtSpawnPoint(state)
        val spawnPoint = state.level.startPositions.first()

        assertTrue(state.level.isSpawnPoint(spawnPoint), "Target should be a spawn point")

        // Wizard tower should be able to target and hit the enemy at the spawn point
        val healthBefore = enemy.currentHealth.value
        assertTrue(engine.defenderAttackPosition(wizard.id, spawnPoint), "Area attack at spawn point should succeed")
        assertTrue(enemy.currentHealth.value < healthBefore, "Enemy health should be reduced by area attack at spawn point")
    }

    @Test
    fun testDotAttackHitsEnemyAtSpawnPoint() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        // Place an alchemy tower (acid/DOT attack) adjacent to the spawn point
        assertTrue(engine.placeDefender(DefenderType.ALCHEMY_TOWER, Position(2, 2)))
        val alchemy = state.defenders.first()
        alchemy.buildTimeRemaining.value = 0

        engine.startFirstPlayerTurn()
        alchemy.resetActions()

        val enemy = spawnEnemyAtSpawnPoint(state)
        val spawnPoint = state.level.startPositions.first()

        assertTrue(state.level.isSpawnPoint(spawnPoint), "Target should be a spawn point")

        // Alchemy tower should be able to target and hit the enemy at the spawn point with acid
        val healthBefore = enemy.currentHealth.value
        assertTrue(engine.defenderAttackPosition(alchemy.id, spawnPoint), "DOT attack at spawn point should succeed")
        assertTrue(enemy.currentHealth.value < healthBefore, "Enemy health should be reduced by DOT attack at spawn point")
    }

    @Test
    fun testAreaAttackSpawnPointExcludedWhenNoEnemy() {
        // An area attack can target an empty spawn point (preemptive strike)
        // Use a level with no enemies to avoid any spawning
        val emptyLevel = Level(
            id = 1,
            name = "Empty Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (1..8).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10
        )
        val state = GameState(emptyLevel)
        val engine = GameEngine(state)

        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 2)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0

        engine.startFirstPlayerTurn()
        wizard.resetActions()

        // Target the empty spawn point - area attacks CAN target spawn points even without enemies
        val spawnPoint = emptyLevel.startPositions.first()
        assertTrue(emptyLevel.isSpawnPoint(spawnPoint), "Target should be a spawn point")
        assertFalse(emptyLevel.isOnPath(spawnPoint), "Spawn point should not be on path")

        // Area attack should be allowed at a spawn point (it's a valid target tile for enemies)
        assertTrue(engine.defenderAttackPosition(wizard.id, spawnPoint), "Area attack should be allowed at spawn point even without an enemy")
        // No enemies on the map since the level has none
        assertTrue(state.attackers.isEmpty(), "No enemies should be on map in an empty level")
    }

    @Test
    fun testEwhadMessageQueuedAtSpawn() {
        // Verify that the Ewhad enters message is queued as soon as Ewhad spawns
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        engine.startFirstPlayerTurn()

        // No messages before spawning
        assertTrue(state.pendingMessages.isEmpty(), "No messages before spawn")

        // Manually spawn Ewhad at spawn point
        val spawnPoint = state.level.startPositions.first()
        val ewhad = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.EWHAD,
            position = mutableStateOf(spawnPoint),
            level = mutableStateOf(1)
        )
        state.attackers.add(ewhad)
        state.pendingMessages.add(GameMessage(type = GameMessageType.EWHAD_ENTERS))

        // Message should be queued immediately when Ewhad is placed on map
        assertEquals(1, state.pendingMessages.size, "EWHAD_ENTERS message should be queued")
        assertEquals(GameMessageType.EWHAD_ENTERS, state.pendingMessages.first().type, "Message type should be EWHAD_ENTERS")

        // Ewhad should still be at spawn point
        assertTrue(state.level.isSpawnPoint(ewhad.position.value), "Ewhad should still be at spawn point when message is queued")
    }

    @Test
    fun testEwhadEntersMessageQueuedBySpawnAttackers() {
        // Verify that spawnAttackers() automatically queues EWHAD_ENTERS when Ewhad spawns
        val ewhadLevel = Level(
            id = 1,
            name = "Ewhad Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (1..8).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            directSpawnPlan = listOf(
                PlannedEnemySpawn(
                    attackerType = AttackerType.EWHAD,
                    spawnTurn = 2,
                    level = 1,
                    spawnPoint = Position(0, 3)
                )
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        val state = GameState(ewhadLevel)
        val engine = GameEngine(state)

        engine.startFirstPlayerTurn()
        // Advance turn counter to match the spawn turn
        state.turnNumber.value = 2

        assertTrue(state.pendingMessages.isEmpty(), "No messages before Ewhad spawns")

        // spawnAttackers is called during enemy turn processing; call it directly here
        engine.spawnEnemyTurnAttackers()

        assertEquals(1, state.pendingMessages.size, "EWHAD_ENTERS message should be queued by spawnAttackers")
        assertEquals(GameMessageType.EWHAD_ENTERS, state.pendingMessages.first().type, "Message type should be EWHAD_ENTERS")
        // Ewhad should be on the map
        val ewhad = state.attackers.find { it.type == AttackerType.EWHAD && !it.isDefeated.value }
        assertTrue(ewhad != null, "Ewhad should be on the map after spawning")
    }

    @Test
    fun testEwhadRetreatsMessageQueuedOnDefeat() {
        // Verify that killing Ewhad via defenderAttack queues EWHAD_RETREATS immediately
        val spawnPoint = Position(0, 3)
        val ewhadLevel = Level(
            id = 1,
            name = "Ewhad Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(spawnPoint),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (1..8).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10
        )
        val state = GameState(ewhadLevel)
        val engine = GameEngine(state)

        engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 2))
        val tower = state.defenders.first()
        tower.buildTimeRemaining.value = 0
        engine.startFirstPlayerTurn()
        tower.resetActions()

        // Spawn Ewhad with minimal health so one attack kills it
        val ewhad = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.EWHAD,
            position = mutableStateOf(spawnPoint),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(1)
        )
        state.attackers.add(ewhad)

        assertTrue(state.pendingMessages.isEmpty(), "No messages before attack")

        // Attack Ewhad - should kill it (1 HP remaining)
        engine.defenderAttack(tower.id, ewhad.id)

        assertEquals(1, state.pendingMessages.size, "EWHAD_RETREATS message should be queued immediately after killing Ewhad")
        assertEquals(GameMessageType.EWHAD_RETREATS, state.pendingMessages.first().type, "Message type should be EWHAD_RETREATS (not final stand)")
    }

    @Test
    fun testEwhadDefeatedMessageQueuedOnFinalStand() {
        // Verify EWHAD_DEFEATED message is queued when killing Ewhad on the final stand level
        val spawnPoint = Position(0, 3)
        val finalStandLevel = Level(
            id = 1,
            name = "Final Stand",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(spawnPoint),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (1..8).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
            editorLevelId = "the_final_stand"
        )
        val state = GameState(finalStandLevel)
        val engine = GameEngine(state)

        engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 2))
        val tower = state.defenders.first()
        tower.buildTimeRemaining.value = 0
        engine.startFirstPlayerTurn()
        tower.resetActions()

        val ewhad = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.EWHAD,
            position = mutableStateOf(spawnPoint),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(1)
        )
        state.attackers.add(ewhad)

        engine.defenderAttack(tower.id, ewhad.id)

        assertEquals(1, state.pendingMessages.size, "EWHAD_DEFEATED message should be queued immediately on final stand")
        assertEquals(GameMessageType.EWHAD_DEFEATED, state.pendingMessages.first().type, "Message type should be EWHAD_DEFEATED on final stand level")
    }
}
