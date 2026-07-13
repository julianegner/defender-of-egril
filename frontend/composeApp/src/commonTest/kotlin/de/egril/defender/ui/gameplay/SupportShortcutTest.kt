package de.egril.defender.ui.gameplay

import de.egril.defender.model.CooldownPower
import de.egril.defender.model.CooldownPowerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObject
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.SupportSpell
import de.egril.defender.model.Trap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the helpers backing the support bar's keyboard navigation (focus cursor + activation).
 */
class SupportShortcutTest {
    private fun createLevel(supports: LevelSupports): Level =
        Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = setOf(Position(0, 0), Position(5, 0)),
            buildAreas = setOf(Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            supports = supports,
        )

    @Test
    fun testVisibleSlotsOrderedObjectsThenSpellsThenPowers() {
        val supports =
            LevelSupports(
                objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = 1)),
                spells = listOf(SupportSpell(SpellType.FREEZE_SPELL, count = 1)),
                cooldownPowers = listOf(CooldownPower(CooldownPowerType.COIN_SURGE)),
            )
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()

        val slots = visibleSupportSlots(gameState)
        assertEquals(3, slots.size)
        assertEquals(SupportSlot.ObjectSlot(SupportObjectType.DWARVEN_TRAP), slots[0])
        assertEquals(SupportSlot.SpellSlot(SpellType.FREEZE_SPELL), slots[1])
        assertEquals(SupportSlot.PowerSlot(CooldownPowerType.COIN_SURGE), slots[2])
    }

    @Test
    fun testExhaustedObjectsAndSpellsAreHiddenButPowersStay() {
        val supports =
            LevelSupports(
                objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = 1)),
                spells = listOf(SupportSpell(SpellType.FREEZE_SPELL, count = 1)),
                cooldownPowers = listOf(CooldownPower(CooldownPowerType.COIN_SURGE)),
            )
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()

        // Use up the object and spell tokens.
        gameState.supportObjectsRemaining[SupportObjectType.DWARVEN_TRAP] = 0
        gameState.supportSpellsRemaining[SpellType.FREEZE_SPELL] = 0

        val slots = visibleSupportSlots(gameState)
        assertEquals(listOf<SupportSlot>(SupportSlot.PowerSlot(CooldownPowerType.COIN_SURGE)), slots)
    }

    @Test
    fun testObjectSlotEnabledDuringInitialBuilding() {
        val supports =
            LevelSupports(objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = 1)))
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()
        gameState.phase.value = GamePhase.INITIAL_BUILDING

        val slot = SupportSlot.ObjectSlot(SupportObjectType.DWARVEN_TRAP)
        assertTrue(isSupportSlotEnabled(gameState, slot, barEnabled = true))
    }

    @Test
    fun testSpellAndPowerSlotsDisabledDuringInitialBuilding() {
        val supports =
            LevelSupports(
                spells = listOf(SupportSpell(SpellType.FREEZE_SPELL, count = 1)),
                cooldownPowers = listOf(CooldownPower(CooldownPowerType.COIN_SURGE)),
            )
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()
        gameState.phase.value = GamePhase.INITIAL_BUILDING

        assertFalse(
            isSupportSlotEnabled(gameState, SupportSlot.SpellSlot(SpellType.FREEZE_SPELL), barEnabled = true),
        )
        assertFalse(
            isSupportSlotEnabled(gameState, SupportSlot.PowerSlot(CooldownPowerType.COIN_SURGE), barEnabled = true),
        )
    }

    @Test
    fun testHealSpellDisabledAtFullHealth() {
        val supports = LevelSupports(spells = listOf(SupportSpell(SpellType.HEAL, count = 1)))
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.healthPoints.value = gameState.level.healthPoints

        assertFalse(
            isSupportSlotEnabled(gameState, SupportSlot.SpellSlot(SpellType.HEAL), barEnabled = true),
        )

        gameState.healthPoints.value = gameState.level.healthPoints - 1
        assertTrue(
            isSupportSlotEnabled(gameState, SupportSlot.SpellSlot(SpellType.HEAL), barEnabled = true),
        )
    }

    @Test
    fun testCooldownPowerDisabledWhileRecharging() {
        val supports =
            LevelSupports(cooldownPowers = listOf(CooldownPower(CooldownPowerType.COIN_SURGE)))
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()
        gameState.phase.value = GamePhase.PLAYER_TURN

        val slot = SupportSlot.PowerSlot(CooldownPowerType.COIN_SURGE)
        assertTrue(isSupportSlotEnabled(gameState, slot, barEnabled = true))

        gameState.cooldownPowerReadyIn[CooldownPowerType.COIN_SURGE] = 3
        assertFalse(isSupportSlotEnabled(gameState, slot, barEnabled = true))
    }

    @Test
    fun testFocusCursorNavigation() {
        // First move from no-focus lands on the near end (0 forward, last box backwards).
        assertEquals(0, nextSupportFocusIndex(current = null, slotCount = 3, forward = true))
        assertEquals(2, nextSupportFocusIndex(current = null, slotCount = 3, forward = false))

        // Stepping through the boxes.
        assertEquals(1, nextSupportFocusIndex(current = 0, slotCount = 3, forward = true))
        assertEquals(0, nextSupportFocusIndex(current = 1, slotCount = 3, forward = false))

        // Wrap around at both ends.
        assertEquals(0, nextSupportFocusIndex(current = 2, slotCount = 3, forward = true))
        assertEquals(2, nextSupportFocusIndex(current = 0, slotCount = 3, forward = false))

        // No boxes: nothing to focus.
        assertNull(nextSupportFocusIndex(current = null, slotCount = 0, forward = true))
        assertNull(nextSupportFocusIndex(current = 1, slotCount = 0, forward = false))
    }

    @Test
    fun testSupportObjectPlacementTilesListsPathTiles() {
        val supports =
            LevelSupports(objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = 2)))
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()

        // Both path tiles are valid trap placement tiles, ordered top-to-bottom then left-to-right.
        val tiles = supportObjectPlacementTiles(gameState, SupportObjectType.DWARVEN_TRAP)
        assertEquals(listOf(Position(0, 0), Position(5, 0)), tiles)
    }

    @Test
    fun testSupportObjectPlacementTilesExcludesOccupiedTiles() {
        val supports =
            LevelSupports(objects = listOf(SupportObject(SupportObjectType.DWARVEN_TRAP, count = 2)))
        val gameState = GameState(createLevel(supports))
        gameState.initializePrePlacedElements()

        // A tile that already has a trap is not offered again.
        gameState.traps.add(
            Trap(position = Position(0, 0), damage = 10, defenderId = -1),
        )

        val tiles = supportObjectPlacementTiles(gameState, SupportObjectType.DWARVEN_TRAP)
        assertEquals(listOf(Position(5, 0)), tiles)
    }

    @Test
    fun testSpellTargetPositionsEmptyWhenNotTargeting() {
        val gameState = GameState(createLevel(LevelSupports()))
        gameState.initializePrePlacedElements()
        assertTrue(spellTargetPositions(gameState).isEmpty())
    }

    @Test
    fun testSpellTargetPositionsListsValidPositionTargets() {
        val gameState = GameState(createLevel(LevelSupports()))
        gameState.initializePrePlacedElements()
        gameState.spellTargeting.value =
            de.egril.defender.model.SpellTargetingState(
                activeSpell = SpellType.BOMB,
                validTargets = setOf(Position(5, 0), Position(0, 0)),
            )

        // Returned sorted top-to-bottom then left-to-right regardless of input order.
        assertEquals(listOf(Position(0, 0), Position(5, 0)), spellTargetPositions(gameState))
    }
}
