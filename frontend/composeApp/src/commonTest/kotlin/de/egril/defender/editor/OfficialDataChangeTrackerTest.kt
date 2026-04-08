package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfficialDataChangeTrackerTest {
    
    @Test
    fun `initially no modifications tracked`() {
        // Clear any previous state
        OfficialDataChangeTracker.clearTracking()
        
        assertFalse(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialLevels().size)
    }
    
    @Test
    fun `track official map modification`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Track modification to an official map
        OfficialDataChangeTracker.trackMapModified("map_tutorial")
        
        assertTrue(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(1, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
        assertEquals("map_tutorial", OfficialDataChangeTracker.getModifiedOfficialMaps()[0])
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialLevels().size)
    }
    
    @Test
    fun `track official level modification`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Track modification to an official level
        OfficialDataChangeTracker.trackLevelModified("welcome_to_defender_of_egril")
        
        assertTrue(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
        assertEquals(1, OfficialDataChangeTracker.getModifiedOfficialLevels().size)
        assertEquals("welcome_to_defender_of_egril", OfficialDataChangeTracker.getModifiedOfficialLevels()[0])
    }
    
    @Test
    fun `track multiple official modifications`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Track multiple modifications
        OfficialDataChangeTracker.trackMapModified("map_tutorial")
        OfficialDataChangeTracker.trackMapModified("map_spiral")
        OfficialDataChangeTracker.trackLevelModified("the_first_wave")
        OfficialDataChangeTracker.trackLevelModified("the_ork_invasion")
        
        assertTrue(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(2, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
        assertEquals(2, OfficialDataChangeTracker.getModifiedOfficialLevels().size)
        assertTrue(OfficialDataChangeTracker.getModifiedOfficialMaps().contains("map_tutorial"))
        assertTrue(OfficialDataChangeTracker.getModifiedOfficialMaps().contains("map_spiral"))
        assertTrue(OfficialDataChangeTracker.getModifiedOfficialLevels().contains("the_first_wave"))
        assertTrue(OfficialDataChangeTracker.getModifiedOfficialLevels().contains("the_ork_invasion"))
    }
    
    @Test
    fun `non-official map not tracked`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Try to track a non-official map
        OfficialDataChangeTracker.trackMapModified("user_custom_map")
        
        assertFalse(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
    }
    
    @Test
    fun `non-official level not tracked`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Try to track a non-official level
        OfficialDataChangeTracker.trackLevelModified("user_custom_level")
        
        assertFalse(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialLevels().size)
    }
    
    @Test
    fun `clear tracking removes all modifications`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Track some modifications
        OfficialDataChangeTracker.trackMapModified("map_tutorial")
        OfficialDataChangeTracker.trackLevelModified("the_first_wave")
        
        assertTrue(OfficialDataChangeTracker.hasModifiedOfficialData())
        
        // Clear tracking
        OfficialDataChangeTracker.clearTracking()
        
        assertFalse(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
        assertEquals(0, OfficialDataChangeTracker.getModifiedOfficialLevels().size)
    }
    
    @Test
    fun `duplicate modifications only tracked once`() {
        OfficialDataChangeTracker.clearTracking()
        
        // Track same map multiple times
        OfficialDataChangeTracker.trackMapModified("map_tutorial")
        OfficialDataChangeTracker.trackMapModified("map_tutorial")
        OfficialDataChangeTracker.trackMapModified("map_tutorial")
        
        assertTrue(OfficialDataChangeTracker.hasModifiedOfficialData())
        assertEquals(1, OfficialDataChangeTracker.getModifiedOfficialMaps().size)
        assertEquals("map_tutorial", OfficialDataChangeTracker.getModifiedOfficialMaps()[0])
    }
}
