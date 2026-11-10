package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the level option "auto tower base": towers may be placed on PATH tiles
 * at an extra cost, with a 150 HP tower base barricade placed underneath.
 */
class AutoTowerBaseTest {
    private fun levelWith(
        autoTowerBase: Boolean,
        initialCoins: Int = 100,
    ): Level =
        Level(
            id = 1,
            name = "Auto Tower Base Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            initialCoins = initialCoins,
            healthPoints = 10,
            autoTowerBase = autoTowerBase,
            autoTowerBaseExtraCost = 20,
        )

    @Test
    fun testPathPlacementAllowedOnlyWithOption() {
        val off = GameState(levelWith(autoTowerBase = false))
        assertFalse(GameEngine(off).placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        assertTrue(off.defenders.isEmpty())

        val on = GameState(levelWith(autoTowerBase = true))
        assertTrue(GameEngine(on).placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        assertEquals(1, on.defenders.size)
    }

    @Test
    fun testPathPlacementCostsSurchargeAndCreatesBase() {
        val state = GameState(levelWith(autoTowerBase = true))
        val engine = GameEngine(state)

        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        assertEquals(100 - 15 - 20, state.coins.value, "Base cost plus surcharge should be deducted")

        val tower = state.defenders.first()
        assertEquals(15 + 20, tower.totalCost, "Total cost should include the surcharge for refunds")
        assertTrue(tower.isOnTowerBase, "Tower should be linked to a tower base")

        val base = state.barricades.find { it.position == Position(4, 3) }
        assertNotNull(base, "A tower base barricade should be placed under the tower")
        assertEquals(AUTO_TOWER_BASE_HEALTH, base.healthPoints.value)
        assertEquals(tower.id, base.supportedTowerId.value)
    }

    @Test
    fun testPathPlacementRejectedWhenUnaffordable() {
        // Base cost 15 alone is affordable, but 15 + 20 is not
        val state = GameState(levelWith(autoTowerBase = true, initialCoins = 30))
        assertFalse(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        assertTrue(state.defenders.isEmpty())
        assertEquals(30, state.coins.value)
    }

    @Test
    fun testButtonStaysActiveWithEmptyBuildableTileAndOnlyBaseCost() {
        // 30 coins: base cost 15 affordable, 15 + 20 not, but an empty build area exists
        val state = GameState(levelWith(autoTowerBase = true, initialCoins = 30))
        assertTrue(state.canAffordTowerButton(DefenderType.SPEAR_TOWER))
    }

    @Test
    fun testButtonDisabledWithoutEmptyBuildableTileAndOnlyBaseCost() {
        val state = GameState(levelWith(autoTowerBase = true, initialCoins = 30))
        assertTrue(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        assertFalse(state.canAffordTowerButton(DefenderType.SPEAR_TOWER))
    }

    @Test
    fun testButtonActiveWithoutEmptyTileWhenSurchargeAffordable() {
        val state = GameState(levelWith(autoTowerBase = true, initialCoins = 100))
        assertTrue(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        assertTrue(state.canAffordTowerButton(DefenderType.SPEAR_TOWER))
    }

    @Test
    fun testButtonRuleUnchangedWithoutOption() {
        val state = GameState(levelWith(autoTowerBase = false, initialCoins = 15))
        assertTrue(state.canAffordTowerButton(DefenderType.SPEAR_TOWER))
        assertFalse(state.canAffordTowerButton(DefenderType.BOW_TOWER))
    }

    @Test
    fun testUndoRefundsSurcharge() {
        val state = GameState(levelWith(autoTowerBase = true))
        val engine = GameEngine(state)
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        val tower = state.defenders.first()

        assertTrue(engine.undoTower(tower.id))
        assertEquals(100, state.coins.value, "Undo should refund base cost and surcharge")
    }

    @Test
    fun testSellRefundsSurchargeProportionally() {
        val state = GameState(levelWith(autoTowerBase = true))
        val engine = GameEngine(state)
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        val tower = state.defenders.first()
        val coinsBefore = state.coins.value

        assertTrue(engine.sellTower(tower.id))
        assertEquals(coinsBefore + (35 * 0.75).toInt(), state.coins.value)
    }

    @Test
    fun testUpgradeDoesNotChargeSurcharge() {
        val state = GameState(levelWith(autoTowerBase = true))
        val engine = GameEngine(state)
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(4, 3)))
        val tower = state.defenders.first()
        val coinsBefore = state.coins.value
        val upgradeCost = tower.upgradeCost

        assertTrue(engine.upgradeDefender(tower.id))
        assertEquals(coinsBefore - upgradeCost, state.coins.value, "Upgrades should only cost the upgrade price")
    }

    @Test
    fun testKeyboardCandidatesOfferBuildableTilesBeforePathTiles() {
        val state = GameState(levelWith(autoTowerBase = true))
        val candidates = state.keyboardPlacementCandidates(DefenderType.SPEAR_TOWER)
        assertEquals(Position(2, 1), candidates.first(), "Build area should be offered first")
        assertTrue(candidates.drop(1).isNotEmpty(), "PATH tiles should follow")
        assertTrue(candidates.drop(1).all { it.y == 3 }, "Only PATH tiles should follow the build area")
    }

    @Test
    fun testKeyboardCandidatesWithoutOptionExcludePathTiles() {
        val state = GameState(levelWith(autoTowerBase = false))
        assertEquals(listOf(Position(2, 1)), state.keyboardPlacementCandidates(DefenderType.SPEAR_TOWER))
    }

    @Test
    fun testKeyboardCandidatesSkipOccupiedBuildAreaAndUnaffordablePath() {
        // 30 coins: a PATH tile would need 15 + 20, so only the build area remains if it is free
        val state = GameState(levelWith(autoTowerBase = true, initialCoins = 30))
        assertTrue(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        val candidates = state.keyboardPlacementCandidates(DefenderType.SPEAR_TOWER)
        assertTrue(candidates.none { it == Position(2, 1) }, "Occupied build area must not be offered")
        assertTrue(candidates.isEmpty(), "Unaffordable PATH tiles must not be offered")
    }

    @Test
    fun testKeyboardCandidatesSkipPathTilesWithBarricade() {
        val state = GameState(levelWith(autoTowerBase = true))
        state.barricades.add(Barricade(id = 999, position = Position(4, 3), healthPoints = mutableStateOf(50), defenderId = 0))
        val candidates = state.keyboardPlacementCandidates(DefenderType.SPEAR_TOWER)
        assertFalse(candidates.contains(Position(4, 3)), "PATH tile with a barricade must not be offered")
    }

    @Test
    fun testPathTileWithOccupantIsNotAutoTowerBaseSpot() {
        val pos = Position(5, 3)
        val cases: List<(GameState) -> Unit> =
            listOf(
                { it.traps.add(Trap(position = pos, damage = 5, defenderId = 0)) },
                { it.fiefs.add(Fief(position = pos, type = FiefType.entries.first())) },
                { it.attackers.add(Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(pos))) },
                { it.submergedAttackers.add(Attacker(id = 2, type = AttackerType.GOBLIN, position = mutableStateOf(pos))) },
            )
        cases.forEachIndexed { index, occupy ->
            val state = GameState(levelWith(autoTowerBase = true))
            occupy(state)
            assertFalse(
                state.isAutoTowerBasePathSpot(DefenderType.SPEAR_TOWER, pos),
                "Occupant case $index must block the auto tower base spot",
            )
            assertFalse(state.keyboardPlacementCandidates(DefenderType.SPEAR_TOWER).contains(pos))
            assertFalse(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, pos), "Occupant case $index")
            assertTrue(state.defenders.isEmpty())
        }
    }

    @Test
    fun testPathTileWithinSpawnDistanceIsRejected() {
        // Spawn at (0,3): distance 2 is blocked, distance 3 is allowed
        val state = GameState(levelWith(autoTowerBase = true))
        assertEquals(2, Position(2, 3).distanceTo(Position(0, 3)))
        assertFalse(state.isAutoTowerBasePathSpot(DefenderType.SPEAR_TOWER, Position(2, 3)))
        assertFalse(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, Position(2, 3)))

        assertEquals(3, Position(3, 3).distanceTo(Position(0, 3)))
        assertTrue(state.isAutoTowerBasePathSpot(DefenderType.SPEAR_TOWER, Position(3, 3)))
        assertTrue(GameEngine(state).placeDefender(DefenderType.SPEAR_TOWER, Position(3, 3)))
    }
}
