package de.egril.defender.game

import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.INDEFINITE_SUPPORT_COUNT
import de.egril.defender.model.Level
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObject
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.SupportSpell
import de.egril.defender.model.TrapType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for player-usable level supports (placable objects + spell tokens).
 */
class SupportPlacementTest {
    private fun createLevel(supports: LevelSupports): Level =
        Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = setOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0), Position(4, 0), Position(5, 0)),
            buildAreas = setOf(Position(2, 2), Position(3, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            supports = supports,
        )

    @Test
    fun testInitializePrePlacedElementsPopulatesRemainingCounts() {
        val supports =
            LevelSupports(
                objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = 2)),
                spells = listOf(SupportSpell(SpellType.FREEZE_SPELL, count = 3)),
            )
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()

        assertEquals(2, gameState.supportObjectsRemaining[SupportObjectType.DWARVEN_TRAP])
        assertEquals(3, gameState.supportSpellsRemaining[SpellType.FREEZE_SPELL])
    }

    @Test
    fun testInitializePrePlacedElementsPreservesIndefiniteCounts() {
        val supports =
            LevelSupports(
                objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = INDEFINITE_SUPPORT_COUNT)),
                spells = listOf(SupportSpell(SpellType.FREEZE_SPELL, count = INDEFINITE_SUPPORT_COUNT)),
            )
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()

        assertEquals(INDEFINITE_SUPPORT_COUNT, gameState.supportObjectsRemaining[SupportObjectType.DWARVEN_TRAP])
        assertEquals(INDEFINITE_SUPPORT_COUNT, gameState.supportSpellsRemaining[SpellType.FREEZE_SPELL])
    }

    @Test
    fun testPlaceSupportTrapOnPath() {
        val gameState = GameState(createLevel(LevelSupports()))
        val engine = GameEngine(gameState)

        val placed = engine.placeSupportTrap(Position(3, 0), damage = 15, type = TrapType.DWARVEN)

        assertTrue(placed, "Support trap should be placed on a path tile")
        val trap = gameState.traps.single()
        assertEquals(Position(3, 0), trap.position)
        assertEquals(15, trap.damage)
        assertEquals(-1, trap.defenderId, "Support-placed traps use defenderId -1")
    }

    @Test
    fun testMagicalSupportTrapHasZeroDamage() {
        val gameState = GameState(createLevel(LevelSupports()))
        val engine = GameEngine(gameState)

        engine.placeSupportTrap(Position(2, 0), damage = 99, type = TrapType.MAGICAL)

        assertEquals(0, gameState.traps.single().damage, "Magical support traps deal no damage")
    }

    @Test
    fun testPlaceSupportTrapRejectedOffPath() {
        val gameState = GameState(createLevel(LevelSupports()))
        val engine = GameEngine(gameState)

        val placed = engine.placeSupportTrap(Position(2, 2), damage = 10, type = TrapType.DWARVEN)

        assertFalse(placed, "Support trap cannot be placed off the path")
        assertTrue(gameState.traps.isEmpty())
    }

    @Test
    fun testPlaceSupportBarricadeOnPath() {
        val gameState = GameState(createLevel(LevelSupports()))
        val engine = GameEngine(gameState)

        val placed = engine.placeSupportBarricade(Position(4, 0), hp = 80)

        assertTrue(placed, "Support barricade should be placed on a path tile")
        val barricade = gameState.barricades.single()
        assertEquals(Position(4, 0), barricade.position)
        assertEquals(80, barricade.healthPoints.value)
        assertEquals(-1, barricade.defenderId, "Support-placed barricades use defenderId -1")
    }
}
