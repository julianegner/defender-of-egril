package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for bridge building functionality
 */
class BridgeBuildingTest {
    /**
     * Test that an Ork can build a wooden bridge over a river
     */
    @Test
    fun testOrkBuildsWoodenBridge() {
        // Create a simple map with a river
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET

        val map =
            EditorMap(
                id = "test-ork-bridge",
                name = "Test Ork Bridge",
                width = 5,
                height = 1,
                tiles = tiles,
            )

        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = map.getPathCells(),
                attackerWaves = emptyList(),
                riverTiles = map.getRiverCells().associateWith { RiverTile(it) },
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)

        // Create an ork adjacent to the river
        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
                currentHealth = mutableStateOf(40),
            )
        state.attackers.add(ork)

        // Check that ork can build bridge
        val bridgeablePositions = bridgeSystem.canBuildBridge(ork)
        assertTrue(bridgeablePositions.isNotEmpty(), "Ork should be able to build bridge")
        assertEquals(1, bridgeablePositions.size, "Ork should bridge 1 tile")
        assertTrue(bridgeablePositions.contains(Position(2, 0)), "Ork should bridge river at (2,0)")

        // Build the bridge
        val success = bridgeSystem.buildBridge(ork, bridgeablePositions)
        assertTrue(success, "Bridge building should succeed")

        // Verify bridge was created
        assertEquals(1, state.bridges.size, "Should have 1 bridge")
        val bridge = state.bridges[0]
        assertEquals(BridgeType.WOODEN, bridge.type, "Bridge should be wooden")
        assertEquals(40, bridge.currentHealth.value, "Bridge should have ork's HP")
        assertTrue(bridge.coversPosition(Position(2, 0)), "Bridge should cover river position")

        // Verify ork was destroyed
        assertTrue(ork.isDefeated.value, "Ork should be defeated")
        assertTrue(ork.isBuildingBridge.value, "Ork should be marked as building bridge")
    }

    @Test
    fun testBridgeSacrificeDoesNotGrantCoinsOrCoinEffects() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET

        val map =
            EditorMap(
                id = "test-bridge-reward",
                name = "Test Bridge Reward",
                width = 5,
                height = 1,
                tiles = tiles,
            )

        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = map.getPathCells(),
                attackerWaves = emptyList(),
                riverTiles = map.getRiverCells().associateWith { RiverTile(it) },
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)

        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
                level = mutableStateOf(3),
                currentHealth = mutableStateOf(120),
            )
        state.attackers.add(ork)

        val bridgeablePositions = bridgeSystem.canBuildBridge(ork)
        assertTrue(bridgeablePositions.isNotEmpty(), "Ork should be able to build a bridge")
        assertTrue(bridgeSystem.buildBridge(ork, bridgeablePositions), "Bridge building should succeed")

        combatSystem.processDefeatedAttackers()

        assertEquals(0, state.pendingCoinGains.value, "Bridge sacrifice must not stage coin rewards")
        assertTrue(state.coinGainEffects.isEmpty(), "Bridge sacrifice must not create coin gain effects")
    }

    @Test
    fun testBridgeRevenueRequiresReachingOppositeRiverbank() {
        val crossingLevel =
            Level(
                id = 1,
                name = "Crossing Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = setOf(Position(1, 0), Position(3, 0)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
            )
        val crossingState = GameState(level = crossingLevel)
        val crossingBridgeSystem = BridgeSystem(crossingState)
        val crossingEngine = GameEngine(crossingState)
        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
                currentHealth = mutableStateOf(40),
            )
        crossingState.attackers.add(ork)
        assertTrue(crossingBridgeSystem.buildBridge(ork, listOf(Position(2, 0))))

        crossingState.phase.value = GamePhase.ENEMY_TURN
        crossingEngine.completeEnemyTurn()

        assertEquals(103, crossingState.coins.value, "A 1-tile bridge that reaches the far bank should add 3 coins")

        val partialLevel =
            Level(
                id = 2,
                name = "Partial Bridge Level",
                gridWidth = 6,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(5, 0)),
                pathCells = setOf(Position(1, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                riverTiles =
                    mapOf(
                        Position(2, 0) to RiverTile(Position(2, 0)),
                        Position(3, 0) to RiverTile(Position(3, 0)),
                    ),
            )
        val partialState = GameState(level = partialLevel)
        val partialBridgeSystem = BridgeSystem(partialState)
        val partialEngine = GameEngine(partialState)
        val partialOrk =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
                currentHealth = mutableStateOf(40),
            )
        partialState.attackers.add(partialOrk)
        assertTrue(partialBridgeSystem.buildBridge(partialOrk, listOf(Position(2, 0))))

        partialState.phase.value = GamePhase.ENEMY_TURN
        partialEngine.completeEnemyTurn()

        assertEquals(100, partialState.coins.value, "A bridge that does not reach the far bank must not add income")
    }

    @Test
    fun testCrossingBridgeRevenueIsThreeCoinsPerTile() {
        val level =
            Level(
                id = 1,
                name = "Stone Bridge Revenue",
                gridWidth = 6,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(5, 0)),
                pathCells = setOf(Position(1, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                riverTiles =
                    mapOf(
                        Position(2, 0) to RiverTile(Position(2, 0)),
                        Position(3, 0) to RiverTile(Position(3, 0)),
                    ),
            )
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val engine = GameEngine(state)
        val ogre =
            Attacker(
                id = 1,
                type = AttackerType.OGRE,
                position = mutableStateOf(Position(1, 0)),
                currentHealth = mutableStateOf(80),
            )
        state.attackers.add(ogre)
        assertTrue(bridgeSystem.buildBridge(ogre, listOf(Position(2, 0), Position(3, 0))))

        state.phase.value = GamePhase.ENEMY_TURN
        engine.completeEnemyTurn()

        assertEquals(106, state.coins.value, "A 2-tile bridge that crosses the river should add 6 coins")
    }

    @Test
    fun testSingleTileBridgeRevenueNeedsOppositeBanks() {
        val level =
            Level(
                id = 1,
                name = "Single Tile Same Bank",
                gridWidth = 4,
                gridHeight = 4,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(3, 3)),
                pathCells = setOf(Position(0, 1), Position(1, 0), Position(3, 3)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(1, 1) to RiverTile(Position(1, 1))),
            )
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val engine = GameEngine(state)
        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(0, 1)),
                currentHealth = mutableStateOf(40),
            )
        state.attackers.add(ork)
        assertTrue(bridgeSystem.buildBridge(ork, listOf(Position(1, 1))))

        state.phase.value = GamePhase.ENEMY_TURN
        engine.completeEnemyTurn()

        assertEquals(100, state.coins.value, "A 1-tile bridge with bank tiles on the same side must not add income")
    }

    @Test
    fun testBridgeRevenueIsAppliedOncePerCompletedEnemyTurn() {
        val level =
            Level(
                id = 1,
                name = "Repeated Bridge Revenue",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = setOf(Position(1, 0), Position(3, 0)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
            )
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val engine = GameEngine(state)
        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
                currentHealth = mutableStateOf(40),
            )
        state.attackers.add(ork)
        assertTrue(bridgeSystem.buildBridge(ork, listOf(Position(2, 0))))

        state.phase.value = GamePhase.ENEMY_TURN
        engine.completeEnemyTurn()
        assertEquals(103, state.coins.value, "Bridge revenue should be applied at player-turn start")

        engine.completeEnemyTurn()
        assertEquals(103, state.coins.value, "Calling completeEnemyTurn outside ENEMY_TURN must not add income again")

        state.phase.value = GamePhase.ENEMY_TURN
        engine.completeEnemyTurn()
        assertEquals(106, state.coins.value, "Bridge revenue should be applied again on the next completed enemy turn")
    }

    /**
     * Test that an Ogre can build a stone bridge over 1-2 river tiles
     */
    @Test
    fun testOgreBuildsStonebridge() {
        // Create a map with 2 adjacent river tiles
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.RIVER
        tiles["4,0"] = TileType.PATH
        tiles["5,0"] = TileType.TARGET

        val map =
            EditorMap(
                id = "test-ogre-bridge",
                name = "Test Ogre Bridge",
                width = 6,
                height = 1,
                tiles = tiles,
            )

        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 6,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(5, 0)),
                pathCells = map.getPathCells(),
                attackerWaves = emptyList(),
                riverTiles = map.getRiverCells().associateWith { RiverTile(it) },
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)

        // Create an ogre adjacent to the river
        val ogre =
            Attacker(
                id = 1,
                type = AttackerType.OGRE,
                position = mutableStateOf(Position(1, 0)),
                currentHealth = mutableStateOf(80),
            )
        state.attackers.add(ogre)

        // Check that ogre can build bridge
        val bridgeablePositions = bridgeSystem.canBuildBridge(ogre)
        assertTrue(bridgeablePositions.isNotEmpty(), "Ogre should be able to build bridge")
        assertTrue(bridgeablePositions.size in 1..2, "Ogre should bridge 1-2 tiles")

        // Build the bridge
        val success = bridgeSystem.buildBridge(ogre, bridgeablePositions)
        assertTrue(success, "Bridge building should succeed")

        // Verify bridge was created
        assertEquals(1, state.bridges.size, "Should have 1 bridge")
        val bridge = state.bridges[0]
        assertEquals(BridgeType.STONE, bridge.type, "Bridge should be stone")
        assertEquals(80, bridge.currentHealth.value, "Bridge should have ogre's HP")

        // Verify ogre was destroyed
        assertTrue(ogre.isDefeated.value, "Ogre should be defeated")
        assertTrue(ogre.isBuildingBridge.value, "Ogre should be marked as building bridge")
    }

    /**
     * Test that Evil Wizard can build a magical bridge (costs 1 level)
     */
    @Test
    fun testEvilWizardBuildsMagicalBridge() {
        // Create a simple map with a river
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET

        val map =
            EditorMap(
                id = "test-wizard-bridge",
                name = "Test Wizard Bridge",
                width = 5,
                height = 1,
                tiles = tiles,
            )

        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = map.getPathCells(),
                attackerWaves = emptyList(),
                riverTiles = map.getRiverCells().associateWith { RiverTile(it) },
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)

        // Create an evil wizard adjacent to the river (level 3)
        val wizard =
            Attacker(
                id = 1,
                type = AttackerType.EVIL_WIZARD,
                position = mutableStateOf(Position(1, 0)),
                level = mutableStateOf(3),
                currentHealth = mutableStateOf(90), // 30 HP * 3 levels
            )
        state.attackers.add(wizard)

        // Check that wizard can build bridge
        val bridgeablePositions = bridgeSystem.canBuildBridge(wizard)
        assertTrue(bridgeablePositions.isNotEmpty(), "Evil Wizard should be able to build bridge")
        assertEquals(1, bridgeablePositions.size, "Evil Wizard should bridge 1 tile")

        // Build the bridge
        val success = bridgeSystem.buildBridge(wizard, bridgeablePositions)
        assertTrue(success, "Bridge building should succeed")

        // Verify bridge was created
        assertEquals(1, state.bridges.size, "Should have 1 bridge")
        val bridge = state.bridges[0]
        assertEquals(BridgeType.MAGICAL, bridge.type, "Bridge should be magical")
        assertEquals(0, bridge.currentHealth.value, "Magical bridge should have no HP")
        assertEquals(3, bridge.turnsRemaining.value, "Magical bridge should last 3 turns")

        // Verify wizard lost 1 level but is not defeated
        assertEquals(2, wizard.level.value, "Wizard should lose 1 level")
        assertFalse(wizard.isDefeated.value, "Wizard should not be defeated")
        assertTrue(wizard.isBuildingBridge.value, "Wizard should be marked as building bridge")
        assertEquals(60, wizard.currentHealth.value, "Wizard HP should be capped to new max (30 * 2)")
    }

    /**
     * Test that Evil Wizard with level 1 cannot build bridge
     */
    @Test
    fun testEvilWizardLevel1CannotBuildBridge() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.TARGET

        val map =
            EditorMap(
                id = "test-wizard-bridge",
                name = "Test Wizard Bridge",
                width = 4,
                height = 1,
                tiles = tiles,
            )

        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 4,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(3, 0)),
                pathCells = map.getPathCells(),
                attackerWaves = emptyList(),
                riverTiles = map.getRiverCells().associateWith { RiverTile(it) },
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)

        // Create level 1 evil wizard
        val wizard =
            Attacker(
                id = 1,
                type = AttackerType.EVIL_WIZARD,
                position = mutableStateOf(Position(1, 0)),
                level = mutableStateOf(1),
                currentHealth = mutableStateOf(30),
            )
        state.attackers.add(wizard)

        // Check that wizard cannot build bridge (needs level 2+)
        val bridgeablePositions = bridgeSystem.canBuildBridge(wizard)
        assertTrue(bridgeablePositions.isEmpty(), "Level 1 wizard should not be able to build bridge")
    }

    /**
     * Test that magical bridge expires after 3 turns
     */
    @Test
    fun testMagicalBridgeExpires() {
        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = setOf(Position(0, 0), Position(1, 0), Position(3, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)

        // Create a magical bridge manually
        val bridge =
            Bridge(
                id = 1,
                type = BridgeType.MAGICAL,
                positions = listOf(Position(2, 0)),
                currentHealth = mutableStateOf(0),
                turnsRemaining = mutableStateOf(3),
                createdByAttackerId = 1,
                createdOnTurn = 1,
            )
        state.bridges.add(bridge)

        // Create a unit on the bridge
        val goblin =
            Attacker(
                id = 2,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(2, 0)),
            )
        state.attackers.add(goblin)

        // Process bridges for 2 turns
        bridgeSystem.processBridges()
        assertEquals(2, bridge.turnsRemaining.value, "Bridge should have 2 turns remaining")
        assertFalse(goblin.isDefeated.value, "Unit should not be defeated yet")

        bridgeSystem.processBridges()
        assertEquals(1, bridge.turnsRemaining.value, "Bridge should have 1 turn remaining")
        assertFalse(goblin.isDefeated.value, "Unit should not be defeated yet")

        // Process one more turn - bridge should expire
        bridgeSystem.processBridges()
        assertEquals(0, state.bridges.size, "Bridge should be removed after expiring")
        assertTrue(goblin.isDefeated.value, "Unit on bridge should be defeated when bridge expires")
    }

    /**
     * Test that units on bridge are destroyed when bridge is destroyed by damage
     */
    @Test
    fun testUnitsDestroyedWhenBridgeDestroyed() {
        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = setOf(Position(0, 0), Position(1, 0), Position(3, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)

        // Create a wooden bridge
        val bridge =
            Bridge(
                id = 1,
                type = BridgeType.WOODEN,
                positions = listOf(Position(2, 0)),
                currentHealth = mutableStateOf(40),
                createdByAttackerId = 1,
                createdOnTurn = 1,
            )
        state.bridges.add(bridge)

        // Create units on the bridge
        val goblin1 =
            Attacker(
                id = 2,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(2, 0)),
            )
        val goblin2 =
            Attacker(
                id = 3,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(2, 0)),
            )
        state.attackers.add(goblin1)
        state.attackers.add(goblin2)

        // Damage the bridge to destroy it
        bridgeSystem.damageBridge(Position(2, 0), 40)

        // Process bridges
        bridgeSystem.processBridges()

        // Verify bridge is destroyed and units are defeated
        assertEquals(0, state.bridges.size, "Bridge should be removed")
        assertTrue(goblin1.isDefeated.value, "Goblin 1 should be defeated")
        assertTrue(goblin2.isDefeated.value, "Goblin 2 should be defeated")
    }

    @Test
    fun testIndestructibleBridgeIgnoresDamage() {
        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = setOf(Position(0, 0), Position(1, 0), Position(3, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
            )

        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val bridge =
            Bridge(
                id = 1,
                type = BridgeType.STONE,
                positions = listOf(Position(2, 0)),
                currentHealth = mutableStateOf(100),
                createdByAttackerId = 1,
                createdOnTurn = 1,
                isIndestructible = true,
            )
        state.bridges.add(bridge)

        bridgeSystem.damageBridge(Position(2, 0), 1000)
        bridgeSystem.processBridges()

        assertEquals(1, state.bridges.size, "Bridge should remain")
        assertEquals(100, bridge.currentHealth.value, "Bridge health must not change")
    }

    /**
     * Test that bridges are walkable for pathfinding
     */
    @Test
    fun testBridgesAreWalkable() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET

        val map =
            EditorMap(
                id = "test-bridge-walkable",
                name = "Test Bridge Walkable",
                width = 5,
                height = 1,
                tiles = tiles,
            )

        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = map.getPathCells(),
                attackerWaves = emptyList(),
                riverTiles = map.getRiverCells().associateWith { RiverTile(it) },
            )

        val state = GameState(level = level)
        val pathfinding = PathfindingSystem(state)

        // Without bridge, no path should exist from spawn to target
        val pathWithoutBridge = pathfinding.findPath(Position(1, 0), Position(3, 0))
        // Path will try to go around or return minimal path
        assertTrue(pathWithoutBridge.size <= 2, "Without bridge, should not find full path")

        // Add a bridge
        val bridge =
            Bridge(
                id = 1,
                type = BridgeType.WOODEN,
                positions = listOf(Position(2, 0)),
                currentHealth = mutableStateOf(40),
                createdByAttackerId = 1,
                createdOnTurn = 1,
            )
        state.bridges.add(bridge)

        // With bridge, path should exist
        val pathWithBridge = pathfinding.findPath(Position(1, 0), Position(3, 0))
        assertTrue(pathWithBridge.size >= 3, "With bridge, should find full path")
        assertTrue(pathWithBridge.contains(Position(2, 0)), "Path should go through bridge")
    }

    @Test
    fun testBallistaCanAttackBridgeTile() {
        val level =
            Level(
                id = 1,
                name = "Test Ballista Bridge Attack",
                gridWidth = 6,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(5, 0)),
                pathCells = setOf(Position(2, 0), Position(3, 0), Position(5, 0)),
                buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
                attackerWaves = emptyList(),
                initialCoins = 200,
                riverTiles =
                    mapOf(
                        Position(4, 0) to RiverTile(position = Position(4, 0), flowDirection = RiverFlow.EAST, flowSpeed = 1),
                    ),
                availableTowers = setOf(DefenderType.BALLISTA_TOWER),
            )

        val state = GameState(level = level)
        val engine = GameEngine(state)
        state.bridges.add(
            Bridge(
                id = 1,
                type = BridgeType.STONE,
                positions = listOf(Position(4, 0)),
                currentHealth = mutableStateOf(100),
                createdByAttackerId = 1,
                createdOnTurn = 1,
            ),
        )

        assertTrue(engine.placeDefender(DefenderType.BALLISTA_TOWER, Position(1, 0)))
        val bridge = state.bridges.first()
        val damaged = engine.defenderAttackPosition(state.defenders.first().id, Position(4, 0))

        assertTrue(damaged, "Ballista should be able to target bridge tiles")
        assertEquals(100, bridge.currentHealth.value, "Bridge damage should be deferred until the attack animation finishes")
        assertEquals(1, state.pendingBridgeDamage.size, "Ballista bridge damage should be queued until after the animation")
        engine.processPendingBridgeDamage()
        assertTrue(bridge.currentHealth.value < 100, "Ballista attack should damage bridge HP after the queued animation")
    }

    @Test
    fun testAcidCanAttackBridgeTile() {
        val level =
            Level(
                id = 1,
                name = "Test Acid Bridge Attack",
                gridWidth = 6,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(5, 0)),
                pathCells = setOf(Position(1, 0), Position(3, 0), Position(5, 0)),
                buildAreas = setOf(Position(2, 0), Position(3, 0)),
                attackerWaves = emptyList(),
                initialCoins = 200,
                riverTiles =
                    mapOf(
                        Position(4, 0) to RiverTile(position = Position(4, 0), flowDirection = RiverFlow.EAST, flowSpeed = 1),
                    ),
                availableTowers = setOf(DefenderType.ALCHEMY_TOWER),
            )

        val state = GameState(level = level)
        val engine = GameEngine(state)
        state.bridges.add(
            Bridge(
                id = 1,
                type = BridgeType.STONE,
                positions = listOf(Position(4, 0)),
                currentHealth = mutableStateOf(100),
                createdByAttackerId = 1,
                createdOnTurn = 1,
            ),
        )

        assertTrue(engine.placeDefender(DefenderType.ALCHEMY_TOWER, Position(2, 0)))
        val bridge = state.bridges.first()
        val damaged = engine.defenderAttackPosition(state.defenders.first().id, Position(4, 0))

        assertTrue(damaged, "Alchemy tower should be able to target bridge tiles")
        assertEquals(100, bridge.currentHealth.value, "Acid bridge damage should be deferred until the attack animation finishes")
        assertEquals(1, state.pendingBridgeDamage.size, "Alchemy bridge damage should be queued until after the animation")
        engine.processPendingBridgeDamage()
        assertTrue(bridge.currentHealth.value < 100, "Acid attack should damage bridge HP after the queued animation")
    }

    /**
     * Test that bridge-building units don't count toward enemy count for winning
     */
    @Test
    fun testBridgeBuildersNotCountedForWinning() {
        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 5,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = setOf(Position(0, 0), Position(1, 0), Position(3, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
                directSpawnPlan =
                    listOf(
                        PlannedEnemySpawn(AttackerType.ORK, 1),
                        PlannedEnemySpawn(AttackerType.GOBLIN, 1),
                    ),
            )

        val state = GameState(level = level)

        // Create ork and goblin
        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
            )
        val goblin =
            Attacker(
                id = 2,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(3, 0)),
            )
        state.attackers.add(ork)
        state.attackers.add(goblin)
        state.nextAttackerId.value = 3

        // Initial active enemy count should be 2
        assertEquals(2, state.getActiveEnemyCount(), "Should have 2 active enemies initially")

        // Ork builds bridge
        ork.isBuildingBridge.value = true
        ork.isDefeated.value = true

        // Active enemy count should now be 1 (only goblin, ork is building bridge)
        assertEquals(1, state.getActiveEnemyCount(), "Should have 1 active enemy after ork builds bridge")

        // Defeat goblin
        goblin.isDefeated.value = true

        // No active enemies remain
        assertEquals(0, state.getActiveEnemyCount(), "Should have 0 active enemies")

        // Level should be won (all spawns occurred, all non-bridge enemies defeated)
        state.turnNumber.value = 10 // Ensure all spawns have occurred
        assertTrue(state.isLevelWon(), "Level should be won when only bridge-building enemies remain")
    }
}
