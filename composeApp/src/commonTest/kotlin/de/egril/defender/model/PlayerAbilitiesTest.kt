package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for PlayerAbilities XP and ability points logic
 */
class PlayerAbilitiesTest {

    @Test
    fun testAddXPGrantsAbilityPointsOnLevelUp() {
        val abilities = PlayerAbilities()
        assertEquals(0, abilities.totalXP)
        assertEquals(1, abilities.level)
        assertEquals(0, abilities.availableAbilityPoints)

        // Adding 100 XP should reach level 2 and grant 1 ability point
        val updated = abilities.addXP(100)
        assertEquals(100, updated.totalXP)
        assertEquals(2, updated.level)
        assertEquals(1, updated.availableAbilityPoints)
    }

    @Test
    fun testAddXPMultipleLevelUps() {
        val abilities = PlayerAbilities()
        // Adding 250 XP should reach level 3 and grant 2 ability points
        val updated = abilities.addXP(250)
        assertEquals(250, updated.totalXP)
        assertEquals(3, updated.level)
        assertEquals(2, updated.availableAbilityPoints)
    }

    @Test
    fun testAddXPAfterSpendingAbilityPoints() {
        val abilities = PlayerAbilities(totalXP = 100, level = 2, availableAbilityPoints = 0)
        // Add enough XP to reach level 3
        val updated = abilities.addXP(150)
        assertEquals(250, updated.totalXP)
        assertEquals(3, updated.level)
        assertEquals(1, updated.availableAbilityPoints)
    }

    @Test
    fun testCalculateLevel() {
        assertEquals(1, PlayerAbilities.calculateLevel(0))
        assertEquals(1, PlayerAbilities.calculateLevel(99))
        assertEquals(2, PlayerAbilities.calculateLevel(100))
        assertEquals(2, PlayerAbilities.calculateLevel(249))
        assertEquals(3, PlayerAbilities.calculateLevel(250))
    }

    @Test
    fun testLevelSyncAfterXPRemovalThenAddition() {
        // Regression test: removing XP must keep level in sync so subsequent addXP works correctly
        // Step 1: start fresh and add enough XP for level 3
        val initial = PlayerAbilities().addXP(250)
        assertEquals(3, initial.level)
        assertEquals(2, initial.availableAbilityPoints)

        // Step 2: simulate removePlayerXP (correct implementation must recalculate level)
        val newXP = 0
        val newLevel = PlayerAbilities.calculateLevel(newXP)
        val afterRemove = initial.copy(totalXP = newXP, level = newLevel)
        assertEquals(0, afterRemove.totalXP)
        assertEquals(1, afterRemove.level)

        // Step 3: add XP again - should grant new ability points since level went up
        val afterAddAgain = afterRemove.addXP(250)
        assertEquals(250, afterAddAgain.totalXP)
        assertEquals(3, afterAddAgain.level)
        // availableAbilityPoints should include the 2 points kept from before removal
        // plus 2 new points from re-leveling up to level 3 again = 4 total
        assertEquals(4, afterAddAgain.availableAbilityPoints,
            "Should grant ability points again after XP is re-added")
    }
}
