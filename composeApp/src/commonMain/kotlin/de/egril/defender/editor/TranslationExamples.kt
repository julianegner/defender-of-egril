package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position

/**
 * Example demonstrating how to create levels with translation support.
 * 
 * This file is for documentation purposes only and shows how to use
 * the translation key system for maps, levels, and locations.
 */

/**
 * Example 1: Creating a level with translation keys (built-in content)
 */
fun createTranslatableLevel(): EditorLevel {
    return EditorLevel(
        id = "level_tutorial",
        mapId = "map_tutorial",
        
        // English name as fallback
        title = "Welcome to Defender of Egril",
        // Translation key for looking up localized title
        titleKey = "level_tutorial_title",
        
        // Subtitle (can be empty)
        subtitle = "Tutorial",
        subtitleKey = "level_tutorial_subtitle",
        
        startCoins = 150,
        startHealthPoints = 20,
        enemySpawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(0, 1)),
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 3, Position(0, 4))
        ),
        availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER)
    )
}

/**
 * Example 2: Creating a user-made level without translation
 */
fun createUserLevel(): EditorLevel {
    return EditorLevel(
        id = "custom_level_123",
        mapId = "custom_map_456",
        
        // User's chosen name (no translation)
        title = "Bob's Amazing Challenge",
        titleKey = null,  // No translation key
        
        subtitle = "Custom Level by Bob",
        subtitleKey = null,
        
        startCoins = 100,
        startHealthPoints = 10,
        enemySpawns = listOf(
            EditorEnemySpawn(AttackerType.ORK, 1, 1)
        ),
        availableTowers = DefenderType.entries.toSet()
    )
}

/**
 * Example 3: Creating a map with translation key
 */
fun createTranslatableMap(): EditorMap {
    return EditorMap(
        id = "map_tutorial",
        name = "Tutorial Map",  // English fallback
        nameKey = "map_tutorial_name",  // Translation key
        width = 15,
        height = 8,
        tiles = mapOf(
            "0,1" to TileType.SPAWN_POINT,
            "0,4" to TileType.SPAWN_POINT,
            "14,4" to TileType.TARGET,
            // ... more tiles
        ),
        readyToUse = true
    )
}

/**
 * Example 4: Creating a world map location with translation
 */
fun createTranslatableLocation(): WorldMapLocationData {
    return WorldMapLocationData(
        id = "starting_village",
        name = "Starting Village",  // English fallback
        nameKey = "location_starting_village",  // Translation key
        position = WorldMapPoint(200, 300),
        levelIds = listOf("level_tutorial", "level_first_wave")
    )
}

/**
 * Example 5: Using the localized names in UI
 * 
 * Import: import de.egril.defender.ui.getLocalizedTitle
 * Import: import de.egril.defender.ui.getLocalizedName
 */
fun displayLevelInUI(level: EditorLevel) {
    // Before (hardcoded English):
    // Text(level.title)
    
    // After (with translation):
    // Text(level.getLocalizedTitle())
    // Text(level.getLocalizedSubtitle())
    
    println("Displaying level: ${level.getLocalizedTitle()}")
}

/**
 * Example 6: The system automatically handles missing translations
 */
fun demonstrateFallback() {
    val level = EditorLevel(
        id = "test",
        mapId = "test",
        title = "Test Level",
        titleKey = "nonexistent_key",  // This key doesn't exist in strings.xml
        subtitle = "",
        startCoins = 100,
        startHealthPoints = 10,
        enemySpawns = emptyList(),
        availableTowers = emptySet()
    )
    
    // Will try to look up "nonexistent_key", fail, and fall back to "Test Level"
    println(level.getLocalizedTitle())  // Outputs: "Test Level"
}

/**
 * Example 7: JSON structure with translation keys
 * 
 * When saved to JSON, levels with translation keys look like this:
 * 
 * {
 *   "id": "level_tutorial",
 *   "mapId": "map_tutorial",
 *   "title": "Welcome to Defender of Egril",
 *   "titleKey": "level_tutorial_title",
 *   "subtitle": "Tutorial",
 *   "subtitleKey": "level_tutorial_subtitle",
 *   "startCoins": 150,
 *   "startHealthPoints": 20,
 *   ...
 * }
 * 
 * User-created levels without translation:
 * 
 * {
 *   "id": "custom_level_123",
 *   "mapId": "custom_map_456",
 *   "title": "Bob's Amazing Challenge",
 *   "subtitle": "Custom Level by Bob",
 *   "startCoins": 100,
 *   "startHealthPoints": 10,
 *   ...
 * }
 * 
 * Note: titleKey and subtitleKey are omitted when null (backward compatible)
 */
