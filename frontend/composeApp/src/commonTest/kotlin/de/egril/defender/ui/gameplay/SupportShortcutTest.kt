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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the keyboard-shortcut helpers backing the support bar boxes.
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
    fun testShortcutBindings() {
        assertEquals("Shift+1", supportSlotShortcutBinding(0))
        assertEquals("Shift+9", supportSlotShortcutBinding(8))
        assertNull(supportSlotShortcutBinding(9))
        assertNull(supportSlotShortcutBinding(-1))
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
}
