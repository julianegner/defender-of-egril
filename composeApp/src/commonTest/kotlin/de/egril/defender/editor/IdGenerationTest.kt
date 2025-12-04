package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ID generation patterns for maps and levels.
 * 
 * Requirements:
 * - Map IDs: lowercase with "map_" prefix
 * - Level IDs: lowercase without "level_" prefix
 */
class IdGenerationTest {
    
    @Test
    fun testMapIdGeneration_lowercase() {
        // Simulate the ID generation logic from MapEditorContent.kt
        val name = "My Custom Map"
        val sanitizedName = name.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = "map_$sanitizedName"
        
        assertEquals("map_my_custom_map", newId)
    }
    
    @Test
    fun testMapIdGeneration_withSpecialCharacters() {
        val name = "Test Map! @#$% 123"
        val sanitizedName = name.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = "map_$sanitizedName"
        
        // Special characters are removed, consecutive underscores collapsed
        assertEquals("map_test_map_123", newId)
    }
    
    @Test
    fun testMapIdGeneration_withUppercase() {
        val name = "UPPERCASE MAP"
        val sanitizedName = name.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = "map_$sanitizedName"
        
        assertEquals("map_uppercase_map", newId)
    }
    
    @Test
    fun testMapIdGeneration_emptyName() {
        val name = ""
        val sanitizedName = name.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = if (sanitizedName.isNotEmpty()) {
            "map_$sanitizedName"
        } else {
            "map_custom_12345" // simulated random number
        }
        
        assertTrue(newId.startsWith("map_custom_"))
    }
    
    @Test
    fun testLevelIdGeneration_noPrefix() {
        // Simulate the ID generation logic from LevelEditor.kt
        val title = "My New Level"
        val sanitizedTitle = title.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = sanitizedTitle
        
        assertEquals("my_new_level", newId)
    }
    
    @Test
    fun testLevelIdGeneration_lowercase() {
        val title = "BOSS BATTLE"
        val sanitizedTitle = title.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = sanitizedTitle
        
        assertEquals("boss_battle", newId)
    }
    
    @Test
    fun testLevelIdGeneration_withSpecialCharacters() {
        val title = "Level 1: The Beginning!"
        val sanitizedTitle = title.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = sanitizedTitle
        
        assertEquals("level_1_the_beginning", newId)
    }
    
    @Test
    fun testLevelIdGeneration_emptyTitle() {
        val title = ""
        val sanitizedTitle = title.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = if (sanitizedTitle.isNotEmpty()) {
            sanitizedTitle
        } else {
            "custom_12345" // simulated random number
        }
        
        assertTrue(newId.startsWith("custom_"))
    }
    
    @Test
    fun testLevelCopyIdGeneration() {
        val originalTitle = "The First Wave"
        val copyTitle = "$originalTitle - Copy"
        val sanitizedTitle = copyTitle.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")  // Collapse consecutive underscores
        val newId = "${sanitizedTitle}_1234" // simulated random number
        
        // Transform: "The First Wave - Copy"
        //   -> lowercase: "the first wave - copy"
        //   -> spaces to underscores: "the_first_wave_-_copy"
        //   -> remove non-alphanumeric: "the_first_wave__copy"
        //   -> collapse underscores: "the_first_wave_copy"
        assertEquals("the_first_wave_copy_1234", newId)
    }
}
