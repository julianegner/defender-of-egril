package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.TileType
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Barricade
import de.egril.defender.model.Bridge
import de.egril.defender.model.BridgeType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Fief
import de.egril.defender.model.FiefType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Mushroom
import de.egril.defender.model.Position
import de.egril.defender.model.Raft
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TileZone
import de.egril.defender.model.Trap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [TileZoneSystem]: switching map areas between path and river at runtime (tides,
 * shifting river courses) and the consequences for units and objects on the changed tiles.
 */
class TileZoneSystemTest {
    private val floodA = Position(2, 0)
    private val floodB = Position(3, 0)
    private val riverA = Position(2, 1)
    private val riverB = Position(3, 1)

    /**
     * Row 0: spawn, path x5, target. Row 1: path, river x2, path.
     * Zone "tide" floods (2,0), (3,0); zone "drought" dries (2,1), (3,1).
     */
    private fun createLevel(): Level {
        val riverTiles =
            listOf(riverA, riverB).associateWith { RiverTile(position = it, flowDirection = RiverFlow.EAST, flowSpeed = 1) }
        val tide =
            TileZone(
                id = "tide",
                name = "High tide",
                tiles = mapOf(floodA to TileType.RIVER, floodB to TileType.RIVER),
                riverTiles =
                    mapOf(
                        floodA to RiverTile(position = floodA, flowDirection = RiverFlow.SOUTH_EAST, flowSpeed = 1),
                        floodB to RiverTile(position = floodB, flowDirection = RiverFlow.SOUTH_EAST, flowSpeed = 1),
                    ),
            )
        val drought = TileZone(id = "drought", tiles = mapOf(riverA to TileType.PATH, riverB to TileType.PATH))
        return Level(
            id = 1,
            name = "Tide Level",
            gridWidth = 8,
            gridHeight = 3,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(6, 0)),
            pathCells = (1..5).map { Position(it, 0) }.toSet() + setOf(Position(1, 1), Position(4, 1)),
            buildAreas = setOf(Position(1, 2), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            riverTiles = riverTiles,
            tileZones = listOf(tide, drought),
        )
    }

    private fun attacker(
        id: Int,
        type: AttackerType,
        position: Position,
    ) = Attacker(id = id, type = type, position = mutableStateOf(position))

    @Test
    fun applyAndRevertZoneSwitchesTiles() {
        val state = GameState(createLevel())
        val system = TileZoneSystem(state)

        assertTrue(system.applyZone("tide"))
        assertTrue(state.level.isRiverTile(floodA))
        assertFalse(state.level.isOnPath(floodA))
        assertEquals(RiverFlow.SOUTH_EAST, state.level.getRiverTile(floodA)?.flowDirection)
        assertEquals(TileType.RIVER, state.paintedTileTypeAt(floodA))
        assertFalse(system.applyZone("tide"), "Applying an active zone again is a no-op")

        assertTrue(system.revertZone("tide"))
        assertFalse(state.level.isRiverTile(floodA))
        assertTrue(state.level.isOnPath(floodA))
        assertNull(state.paintedTileTypeAt(floodA))
        assertTrue(state.activeTileZoneIds.isEmpty())
    }

    @Test
    fun toggleZoneAlternates() {
        val state = GameState(createLevel())
        val system = TileZoneSystem(state)

        system.toggleZone("drought")
        assertTrue(state.level.isOnPath(riverA))
        system.toggleZone("drought")
        assertTrue(state.level.isRiverTile(riverA))
        assertEquals(RiverFlow.EAST, state.level.getRiverTile(riverA)?.flowDirection, "Original river flow is restored")
    }

    @Test
    fun unknownZoneIsIgnored() {
        val state = GameState(createLevel())
        assertFalse(TileZoneSystem(state).applyZone("missing"))
        assertTrue(state.activeTileZoneIds.isEmpty())
    }

    @Test
    fun floodingDrownsLandUnitsWithoutReward() {
        val state = GameState(createLevel())
        state.attackers.add(attacker(1, AttackerType.GOBLIN, floodA))
        state.attackers.add(attacker(2, AttackerType.GOBLIN, Position(5, 0)))

        TileZoneSystem(state).applyZone("tide")

        assertEquals(listOf(2), state.attackers.map { it.id }, "Only the goblin on the flooded tile drowns")
        assertEquals(100, state.coins.value, "Drowning grants no reward")
        assertEquals(0, state.enemiesKilledTotal.value, "Drowned units are not counted as kills")
    }

    @Test
    fun floodingSparesUnitsThatCanStayOnWater() {
        val state = GameState(createLevel())
        val swimmer = AttackerType.entries.first { it.canTraverseRiver && !it.survivesSubmersion }
        val flyer = AttackerType.entries.first { it.canFlyOverTerrain }
        state.attackers.add(attacker(1, swimmer, floodA))
        state.attackers.add(attacker(2, flyer, floodB))

        TileZoneSystem(state).applyZone("tide")

        assertEquals(setOf(1, 2), state.attackers.map { it.id }.toSet())
    }

    @Test
    fun trollSubmergesAndResurfacesWhenWaterRecedes() {
        val state = GameState(createLevel())
        val troll = attacker(1, AttackerType.TROLL, floodA)
        state.attackers.add(troll)
        val system = TileZoneSystem(state)

        system.applyZone("tide")
        assertTrue(state.attackers.isEmpty(), "Submerged troll is not on the map")
        assertEquals(listOf(troll), state.submergedAttackers.toList())
        assertTrue(state.isLevelWon(), "Submerged units do not count towards winning")

        system.revertZone("tide")
        assertTrue(state.submergedAttackers.isEmpty())
        assertEquals(listOf(troll), state.attackers.toList())
        assertEquals(floodA, troll.position.value)
    }

    @Test
    fun resurfacingUnitMovesToFreeNeighborWhenTileIsOccupied() {
        val state = GameState(createLevel())
        val troll = attacker(1, AttackerType.TROLL, floodA)
        state.attackers.add(troll)
        val system = TileZoneSystem(state)
        system.applyZone("tide")
        // Another unit occupies the troll's tile by the time the water recedes.
        state.attackers.add(attacker(2, AttackerType.SKELETON, floodA))

        system.revertZone("tide")

        assertTrue(state.submergedAttackers.isEmpty())
        assertTrue(troll.position.value != floodA, "Resurfaced unit must not share a tile")
        assertTrue(state.level.isOnPath(troll.position.value), "Resurfaced unit lands on a walkable tile")
    }

    @Test
    fun floodingDestroysTrapsFiefsAndMushrooms() {
        val state = GameState(createLevel())
        state.traps.add(Trap(position = floodA, damage = 10, defenderId = 1))
        state.fiefs.add(Fief(position = floodB, type = FiefType.FISHER))
        state.mushrooms.add(Mushroom(position = floodA))
        state.traps.add(Trap(position = Position(5, 0), damage = 10, defenderId = 1))

        TileZoneSystem(state).applyZone("tide")

        assertEquals(listOf(Position(5, 0)), state.traps.map { it.position })
        assertTrue(state.fiefs.isEmpty())
        assertTrue(state.mushrooms.isEmpty())
    }

    @Test
    fun floodedTowerBaseBecomesRaftWithBarricadeHealth() {
        val state = GameState(createLevel())
        val barricade = Barricade(id = 5, position = floodA, healthPoints = mutableStateOf(130), defenderId = 1)
        val tower = Defender(id = 1, type = DefenderType.BOW_TOWER, position = mutableStateOf(floodA))
        barricade.supportedTowerId.value = tower.id
        tower.towerBaseBarricadeId.value = barricade.id
        state.barricades.add(barricade)
        state.defenders.add(tower)

        TileZoneSystem(state).applyZone("tide")

        val raft = state.rafts.single()
        assertEquals(tower.id, raft.defenderId)
        assertEquals(130, raft.healthPoints.value)
        assertEquals(raft.id, tower.raftId.value)
        assertNull(tower.towerBaseBarricadeId.value)
        assertTrue(state.barricades.isEmpty(), "The flooded barricade is replaced by the raft")
    }

    @Test
    fun floodingDestroysDwarvenMine() {
        val state = GameState(createLevel())
        state.defenders.add(Defender(id = 1, type = DefenderType.DWARVEN_MINE, position = mutableStateOf(floodA)))

        TileZoneSystem(state).applyZone("tide")

        assertTrue(state.defenders.isEmpty())
        assertTrue(state.rafts.isEmpty())
    }

    @Test
    fun noPlayDestroysEverythingExceptFlyingOrHoveringEnemies() {
        val buildArea = Position(1, 2)
        val noPlayZone =
            TileZone(
                id = "void",
                tiles =
                    mapOf(
                        floodA to TileType.NO_PLAY,
                        buildArea to TileType.NO_PLAY,
                        riverA to TileType.NO_PLAY,
                    ),
            )
        val level = createLevel()
        val state = GameState(level.copy(tileZones = level.tileZones + noPlayZone))
        val flyingEnemyType = AttackerType.entries.first { it.canFlyOverTerrain }
        val flyingVillainType = AttackerType.entries.first { it.isVillain && it.canFlyOverTerrain }
        val dragon = attacker(1, AttackerType.DRAGON, floodA)
        val flyingEnemy = attacker(2, flyingEnemyType, buildArea)
        val flyingVillain = attacker(6, flyingVillainType, riverA)
        state.attackers.add(attacker(3, AttackerType.GOBLIN, floodA))
        state.attackers.add(attacker(4, AttackerType.EWHAD, buildArea))
        state.attackers.add(dragon)
        state.attackers.add(flyingEnemy)
        state.attackers.add(flyingVillain)
        state.submergedAttackers.add(attacker(5, AttackerType.TROLL, riverA))

        val towerOnBarricade = Defender(id = 1, type = DefenderType.BOW_TOWER, position = mutableStateOf(floodA))
        val towerBase = Barricade(id = 1, position = floodA, healthPoints = mutableStateOf(120), defenderId = towerOnBarricade.id)
        towerBase.supportedTowerId.value = towerOnBarricade.id
        towerOnBarricade.towerBaseBarricadeId.value = towerBase.id
        val bargeTower = Defender(id = 2, type = DefenderType.SPEAR_TOWER, position = mutableStateOf(riverA))
        val raft = Raft(id = 1, defenderId = bargeTower.id, currentPosition = mutableStateOf(riverA))
        bargeTower.raftId.value = raft.id
        state.defenders.addAll(listOf(towerOnBarricade, bargeTower))
        state.barricades.add(towerBase)
        state.barricades.add(Barricade(id = 2, position = buildArea, healthPoints = mutableStateOf(80), defenderId = 99))
        state.rafts.add(raft)
        state.traps.add(Trap(position = floodA, damage = 10, defenderId = 1))
        state.fiefs.add(Fief(position = buildArea, type = FiefType.MARKETPLACE))
        state.mushrooms.add(Mushroom(position = floodA))
        state.bridges.add(Bridge(id = 1, type = BridgeType.WOODEN, positions = listOf(riverA), createdByAttackerId = 99, createdOnTurn = 1))

        TileZoneSystem(state).applyZone("void")

        assertEquals(setOf(dragon, flyingEnemy, flyingVillain), state.attackers.toSet())
        assertTrue(state.submergedAttackers.isEmpty())
        assertTrue(state.defenders.isEmpty())
        assertTrue(state.barricades.isEmpty())
        assertTrue(state.rafts.isEmpty())
        assertTrue(raft.isDestroyed.value)
        assertTrue(state.traps.isEmpty())
        assertTrue(state.fiefs.isEmpty())
        assertTrue(state.mushrooms.isEmpty())
        assertTrue(state.bridges.isEmpty())
        assertEquals(100, state.coins.value, "Destroyed enemies grant no coins")
        assertEquals(0, state.enemiesKilledTotal.value, "Destroyed enemies are not counted as kills")
        assertEquals(TileType.NO_PLAY, state.currentTileTypeAt(floodA))
        assertEquals(TileType.NO_PLAY, state.currentTileTypeAt(buildArea))
        assertEquals(TileType.NO_PLAY, state.currentTileTypeAt(riverA))
    }

    private fun stateWithRaft(raftHealth: Int): Pair<GameState, Defender> {
        val state = GameState(createLevel())
        val tower = Defender(id = 1, type = DefenderType.BOW_TOWER, position = mutableStateOf(riverA))
        val raft = Raft(id = 1, defenderId = tower.id, currentPosition = mutableStateOf(riverA), healthPoints = mutableStateOf(raftHealth))
        tower.raftId.value = raft.id
        state.defenders.add(tower)
        state.rafts.add(raft)
        return state to tower
    }

    @Test
    fun strandedRaftBecomesBarricadeCarryingTower() {
        val (state, tower) = stateWithRaft(Raft.RAFT_MAX_HEALTH)

        TileZoneSystem(state).applyZone("drought")

        assertTrue(state.rafts.isEmpty())
        val barricade = state.barricades.single()
        assertEquals(riverA, barricade.position)
        assertEquals(Raft.RAFT_MAX_HEALTH - Raft.STRANDING_DAMAGE, barricade.healthPoints.value)
        assertEquals(tower.id, barricade.supportedTowerId.value)
        assertEquals(barricade.id, tower.towerBaseBarricadeId.value)
        assertNull(tower.raftId.value)
        assertTrue(state.defenders.contains(tower))
    }

    @Test
    fun strandedWeakRaftLosesTowerButKeepsBarricade() {
        val (state, tower) = stateWithRaft(120)

        TileZoneSystem(state).applyZone("drought")

        val barricade = state.barricades.single()
        assertEquals(90, barricade.healthPoints.value)
        assertNull(barricade.supportedTowerId.value)
        assertFalse(state.defenders.contains(tower), "Tower is destroyed when the barricade drops below 100 HP")
        assertEquals(100, state.coins.value, "No refund for the lost tower")
    }

    @Test
    fun strandedRaftWithoutHealthLeftLeavesNothing() {
        val (state, _) = stateWithRaft(Raft.STRANDING_DAMAGE)

        TileZoneSystem(state).applyZone("drought")

        assertTrue(state.barricades.isEmpty())
        assertTrue(state.defenders.isEmpty())
    }

    @Test
    fun dryingKillsWaterOnlyUnitsAndRemovesBridges() {
        val state = GameState(createLevel())
        val waterOnly = AttackerType.entries.first { it.canOnlyMoveOnWater }
        state.attackers.add(attacker(1, waterOnly, riverA))
        state.bridges.add(Bridge(id = 1, type = BridgeType.WOODEN, positions = listOf(riverB), createdByAttackerId = 9, createdOnTurn = 1))

        TileZoneSystem(state).applyZone("drought")

        assertTrue(state.attackers.isEmpty())
        assertEquals(0, state.enemiesKilledTotal.value)
        assertTrue(state.bridges.isEmpty())
    }

    @Test
    fun restoreActiveZonesReappliesTilesOnly() {
        val state = GameState(createLevel())
        state.traps.add(Trap(position = floodA, damage = 10, defenderId = 1))
        state.activeTileZoneIds.add("tide")

        TileZoneSystem(state).restoreActiveZones()

        assertTrue(state.level.isRiverTile(floodA))
        assertEquals(1, state.traps.size, "Restoring a saved zone state does not re-run flood consequences")
    }

    @Test
    fun laterZoneTakesPrecedenceAndRevertFallsBackToEarlierZone() {
        val level = createLevel()
        val dryTide = TileZone(id = "dry_tide", tiles = mapOf(floodA to TileType.PATH))
        val state = GameState(level.copy(tileZones = level.tileZones + dryTide))
        val system = TileZoneSystem(state)

        system.applyZone("tide")
        system.applyZone("dry_tide")
        assertTrue(state.level.isOnPath(floodA))
        assertTrue(state.level.isRiverTile(floodB))

        system.revertZone("dry_tide")
        assertTrue(state.level.isRiverTile(floodA), "Tile falls back to the still active earlier zone")
        assertNotNull(state.paintedTileTypeAt(floodA))
    }
}
