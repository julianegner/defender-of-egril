package de.egril.defender.editor

import de.egril.defender.model.Position

/**
 * A point on the world map used for curved paths.
 * Coordinates are in permille (0-1000) of the world map dimensions.
 */
data class WorldMapPoint(
    val x: Int,
    val y: Int
) {
    /**
     * Convert to normalized coordinates (0.0-1.0)
     */
    fun toNormalized(): Pair<Float, Float> = (x / 1000f) to (y / 1000f)
    
    companion object {
        /**
         * Create from normalized coordinates (0.0-1.0)
         */
        fun fromNormalized(x: Float, y: Float): WorldMapPoint {
            return WorldMapPoint(
                (x * 1000).toInt().coerceIn(0, 1000),
                (y * 1000).toInt().coerceIn(0, 1000)
            )
        }
        
        /**
         * Create from Position
         */
        fun fromPosition(position: Position): WorldMapPoint {
            return WorldMapPoint(position.x, position.y)
        }
    }
    
    /**
     * Convert to Position
     */
    fun toPosition(): Position = Position(x, y)
}

/**
 * Type of connection between locations on the world map
 */
enum class ConnectionType {
    ROAD,       // Land route - displayed as light brown curved line
    SEA_ROUTE   // Sea route - displayed as dark blue dashed curved line
}

/**
 * A location on the world map that can contain multiple levels.
 * The location is only visible if at least one of its levels is ready to play.
 */
data class WorldMapLocationData(
    val id: String,                      // Unique identifier for this location
    val name: String,                    // Display name shown on the map
    val nameKey: String? = null,         // Optional string resource key for translation (e.g., "location_starting_village")
    val position: WorldMapPoint,         // Position on the world map
    val levelIds: List<String>,          // List of level IDs at this location
    val iconResourceName: String? = null // Optional icon resource name (without extension, e.g., "emoji_map", "emoji_pushpin")
)

/**
 * A path between two locations on the world map.
 * The path can be a straight line (empty controlPoints) or a curve (with control points).
 * Paths remain visible even when source/destination locations are hidden.
 * 
 * For mixed-type paths (partly road, partly sea), use segmentTypes to specify the type for each segment.
 * A segment is the portion between consecutive points:
 * - Segment 0: fromLocation to first waypoint (or toLocation if no waypoints)
 * - Segment 1: first waypoint to second waypoint
 * - Segment N: last waypoint to toLocation
 */
data class WorldMapPathData(
    val fromLocationId: String,          // Source location ID
    val toLocationId: String,            // Destination location ID
    val controlPoints: List<WorldMapPoint> = emptyList(),  // Optional control points for curved paths
    val type: ConnectionType = ConnectionType.ROAD,  // Default type for entire path (used when segmentTypes is empty)
    val segmentTypes: List<ConnectionType> = emptyList()  // Optional per-segment types for mixed paths
) {
    /**
     * Get the connection type for a specific segment index.
     * If segmentTypes is empty or too short, returns the default type.
     */
    fun getSegmentType(segmentIndex: Int): ConnectionType {
        return segmentTypes.getOrNull(segmentIndex) ?: type
    }
    
    /**
     * Get the number of segments in this path.
     * A path with N waypoints has N+1 segments.
     */
    fun getSegmentCount(): Int {
        return controlPoints.size + 1
    }
    /**
     * Check if this path represents a valid connection based on level prerequisites.
     * A path is valid if any level at the destination has a prerequisite at the source.
     */
    fun isValidConnection(
        locations: List<WorldMapLocationData>,
        levels: List<EditorLevel>
    ): Boolean {
        val fromLocation = locations.find { it.id == fromLocationId } ?: return false
        val toLocation = locations.find { it.id == toLocationId } ?: return false
        
        // Check if any level at destination has a prerequisite at source
        val sourceLevelIds = fromLocation.levelIds.toSet()
        return toLocation.levelIds.any { destLevelId ->
            val destLevel = levels.find { it.id == destLevelId }
            destLevel?.prerequisites?.any { it in sourceLevelIds } == true
        }
    }
}

/**
 * A rectangular sea area on the world map, used for wave animations.
 * Coordinates are in permille (0-1000) of the world map dimensions.
 */
data class WorldMapSeaArea(
    val x: Int,      // Left edge (permille 0-1000)
    val y: Int,      // Top edge (permille 0-1000)
    val width: Int,  // Width (permille)
    val height: Int  // Height (permille)
)

/**
 * A river path on the world map, used for flowing water animations.
 * Coordinates are in permille (0-1000) of the world map dimensions.
 * Points are ordered from source (upstream) to destination (towards sea).
 */
data class WorldMapRiver(
    val points: List<WorldMapPoint>  // River path points (ordered towards sea)
)

/**
 * Complete world map data containing all locations and paths.
 * This is stored in a separate JSON file for easy editing.
 */
data class WorldMapData(
    val locations: List<WorldMapLocationData> = emptyList(),
    val paths: List<WorldMapPathData> = emptyList(),
    val seaAreas: List<WorldMapSeaArea> = emptyList(),
    val rivers: List<WorldMapRiver> = emptyList()
) {
    /**
     * Get all locations that have at least one playable level.
     */
    fun getVisibleLocations(
        levels: List<EditorLevel>,
        maps: List<EditorMap>,
        storage: EditorStorage
    ): List<WorldMapLocationData> {
        return locations.filter { location ->
            location.levelIds.any { levelId ->
                storage.isLevelReadyToPlay(levelId)
            }
        }
    }
    
    /**
     * Get all paths that connect locations based on level prerequisites.
     * Paths are visible even if source/destination locations are hidden.
     */
    fun getValidPaths(levels: List<EditorLevel>): List<WorldMapPathData> {
        return paths.filter { path ->
            path.isValidConnection(locations, levels)
        }
    }
    
    /**
     * Find a location by its ID.
     */
    fun findLocation(id: String): WorldMapLocationData? {
        return locations.find { it.id == id }
    }
    
    /**
     * Find the location containing a specific level.
     */
    fun findLocationByLevelId(levelId: String): WorldMapLocationData? {
        return locations.find { levelId in it.levelIds }
    }
    
    /**
     * Get all paths connected to a location (either as source or destination).
     */
    fun getPathsForLocation(locationId: String): List<WorldMapPathData> {
        return paths.filter { it.fromLocationId == locationId || it.toLocationId == locationId }
    }
    
    /**
     * Check if a location has any level whose prerequisites are not fulfilled by other levels at that location.
     * This indicates a potential configuration issue in the editor.
     */
    fun hasLocationWithUnfulfilledPrerequisites(locationId: String, levels: List<EditorLevel>): Boolean {
        val location = findLocation(locationId) ?: return false
        val levelIdsAtLocation = location.levelIds.toSet()
        
        // Check if any level at this location has prerequisites not in the same location
        return location.levelIds.any { levelId ->
            val level = levels.find { it.id == levelId }
            val prerequisites = level?.prerequisites ?: emptyList()
            prerequisites.isNotEmpty() && !prerequisites.all { it in levelIdsAtLocation }
        }
    }
}
