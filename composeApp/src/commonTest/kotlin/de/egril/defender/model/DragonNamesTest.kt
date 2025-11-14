package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DragonNamesTest {
    
    @Test
    fun testDragonNamesListHas200Names() {
        assertEquals(200, DragonNames.names.size, "Dragon names list should contain exactly 200 names")
    }
    
    @Test
    fun testDragonNamesAreNotEmpty() {
        DragonNames.names.forEach { name ->
            assertTrue(name.isNotBlank(), "Dragon name should not be blank")
        }
    }
    
    @Test
    fun testGetRandomNameReturnsValidName() {
        val randomName = DragonNames.getRandomName()
        assertNotNull(randomName, "getRandomName should return a non-null name")
        assertTrue(randomName.isNotBlank(), "Random dragon name should not be blank")
        assertTrue(DragonNames.names.contains(randomName), "Random name should be from the names list")
    }
    
    @Test
    fun testGetRandomNameReturnsDifferentNames() {
        // Get 10 random names and check that at least some are different
        val names = (1..10).map { DragonNames.getRandomName() }.toSet()
        // With 200 names, getting 10 should give us at least 3 unique ones (very high probability)
        assertTrue(names.size >= 3, "Multiple calls to getRandomName should return different names")
    }
}
