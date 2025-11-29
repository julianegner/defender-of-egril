package de.egril.defender.ui.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.mouseWheelZoom
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.editor.EditorStorage
import org.jetbrains.compose.resources.painterResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.world_map_background

/**
 * World map location data for placing level markers on the map
 */
data class WorldMapLocation(
    val x: Float,  // X position as percentage of map width (0.0 to 1.0)
    val y: Float,  // Y position as percentage of map height (0.0 to 1.0)
    val levelIds: List<String>,  // Editor level IDs at this location
    val name: String  // Display name for this location
)

/**
 * Road connection between two locations
 */
data class WorldMapRoad(
    val fromLocation: WorldMapLocation,
    val toLocation: WorldMapLocation
)

/**
 * Image-based World Map View that displays a PNG background with location markers
 */
@Composable
fun ImageWorldMapView(
    worldLevels: List<WorldLevel>,
    onLocationClicked: (WorldMapLocation, List<WorldLevel>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = AppSettings.isDarkMode.value
    
    // Generate location data and road connections from level maps
    val (locations, roads) = remember(worldLevels) {
        generateWorldMapLocationsAndRoads(worldLevels)
    }
    
    // Pan and zoom state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize(1200, 800)) } // Default map size
    
    // Use rememberUpdatedState to avoid capturing stale offset values in gesture handlers
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentScale by rememberUpdatedState(scale)
    
    // Helper function to find location at screen position
    fun findLocationAtPosition(screenX: Float, screenY: Float): WorldMapLocation? {
        // Transform screen coordinates to map coordinates
        val mapX = (screenX - containerSize.width / 2 - currentOffsetX) / currentScale + containerSize.width / 2
        val mapY = (screenY - containerSize.height / 2 - currentOffsetY) / currentScale + containerSize.height / 2
        
        // Calculate actual image position within the container
        val aspectRatio = imageSize.width.toFloat() / imageSize.height.toFloat()
        val containerAspect = containerSize.width.toFloat() / containerSize.height.toFloat()
        
        val (imageDisplayWidth, imageDisplayHeight) = if (containerAspect > aspectRatio) {
            // Container is wider - fit to height
            val h = containerSize.height.toFloat()
            val w = h * aspectRatio
            w to h
        } else {
            // Container is taller - fit to width
            val w = containerSize.width.toFloat()
            val h = w / aspectRatio
            w to h
        }
        
        val imageOffsetX = (containerSize.width - imageDisplayWidth) / 2
        val imageOffsetY = (containerSize.height - imageDisplayHeight) / 2
        
        // Convert to normalized coordinates (0-1)
        val normalizedX = (mapX - imageOffsetX) / imageDisplayWidth
        val normalizedY = (mapY - imageOffsetY) / imageDisplayHeight
        
        // Find location within click radius (5% of map size)
        val clickRadius = 0.05f
        for (location in locations) {
            val dx = normalizedX - location.x
            val dy = normalizedY - location.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < clickRadius) {
                return location
            }
        }
        return null
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF1A1A2E) else Color(0xFF87CEEB))
            .onSizeChanged { containerSize = it }
            .mouseWheelZoom(
                containerSize = containerSize,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                onScaleChange = { scale = it.coerceIn(0.5f, 3f) },
                onOffsetChange = { x, y -> offsetX = x; offsetY = y }
            )
            .pointerInput(locations) {
                detectTapGestures { offset ->
                    val location = findLocationAtPosition(offset.x, offset.y)
                    if (location != null) {
                        val levelsAtLocation = worldLevels.filter { 
                            it.level.editorLevelId in location.levelIds 
                        }
                        onLocationClicked(location, levelsAtLocation)
                    }
                }
            }
            .pointerInput(Unit) {
                var dragStartOffsetX = 0f
                var dragStartOffsetY = 0f
                var cumulativeX = 0f
                var cumulativeY = 0f
                
                detectDragGestures(
                    onDragStart = {
                        dragStartOffsetX = currentOffsetX
                        dragStartOffsetY = currentOffsetY
                        cumulativeX = 0f
                        cumulativeY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        cumulativeX += dragAmount.x
                        cumulativeY += dragAmount.y
                        offsetX = dragStartOffsetX + cumulativeX
                        offsetY = dragStartOffsetY + cumulativeY
                    }
                )
            }
            .then(
                if (isPlatformMobile) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                scale = (scale * zoom).coerceIn(0.5f, 3f)
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Draw the map image and overlay locations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            // Background map image
            Image(
                painter = painterResource(Res.drawable.world_map_background),
                contentDescription = "World Map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Draw road connections between locations
            RoadConnectionsOverlay(
                roads = roads,
                containerSize = containerSize,
                isDarkMode = isDarkMode
            )
            
            // Location markers overlay
            LocationMarkersOverlay(
                locations = locations,
                worldLevels = worldLevels,
                containerSize = containerSize,
                isDarkMode = isDarkMode
            )
        }
    }
}

/**
 * Overlay that draws road connections between locations
 */
@Composable
private fun BoxScope.RoadConnectionsOverlay(
    roads: List<WorldMapRoad>,
    containerSize: IntSize,
    isDarkMode: Boolean
) {
    val roadColor = if (isDarkMode) Color(0xFF8B4513) else Color(0xFFA0522D)  // Brown road color
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (road in roads) {
            val startX = road.fromLocation.x * containerSize.width
            val startY = road.fromLocation.y * containerSize.height
            val endX = road.toLocation.x * containerSize.width
            val endY = road.toLocation.y * containerSize.height
            
            // Draw road as a dashed line
            drawLine(
                color = roadColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
            )
        }
    }
}

/**
 * Overlay that draws location markers on top of the map
 */
@Composable
private fun BoxScope.LocationMarkersOverlay(
    locations: List<WorldMapLocation>,
    worldLevels: List<WorldLevel>,
    containerSize: IntSize,
    isDarkMode: Boolean
) {
    // Draw each location marker
    for (location in locations) {
        val levelsAtLocation = worldLevels.filter { 
            it.level.editorLevelId in location.levelIds 
        }
        
        // Determine location status based on contained levels
        val hasWonLevel = levelsAtLocation.any { it.status == LevelStatus.WON }
        val hasUnlockedLevel = levelsAtLocation.any { it.status == LevelStatus.UNLOCKED }
        val allLocked = levelsAtLocation.all { it.status == LevelStatus.LOCKED }
        
        val markerColor = when {
            hasWonLevel -> if (isDarkMode) Color(0xFF2ECC71) else Color(0xFF27AE60)  // Green
            hasUnlockedLevel -> if (isDarkMode) Color(0xFF3498DB) else Color(0xFF2196F3)  // Blue
            allLocked -> if (isDarkMode) Color(0xFF7F8C8D) else Color(0xFF95A5A6)  // Grey
            else -> if (isDarkMode) Color(0xFF3498DB) else Color(0xFF2196F3)  // Blue default
        }
        
        // Calculate marker position in pixels (pre-compute to avoid division on each frame)
        val markerSize = 30.dp
        val halfMarkerSize = 15  // in pixels, approximate center offset
        val xPx = (location.x * containerSize.width).toInt() - halfMarkerSize
        val yPx = (location.y * containerSize.height).toInt() - halfMarkerSize
        
        // Position the marker based on normalized coordinates
        Box(
            modifier = Modifier
                .offset(x = xPx.dp, y = yPx.dp)
                .size(markerSize)
        ) {
            // Marker circle
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = markerColor,
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = levelsAtLocation.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Generate world map locations and road connections from the available levels.
 * Groups levels by their map ID and assigns positions based on prerequisites.
 * Creates road connections based on level prerequisite relationships.
 */
private fun generateWorldMapLocationsAndRoads(worldLevels: List<WorldLevel>): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
    if (worldLevels.isEmpty()) return emptyList<WorldMapLocation>() to emptyList()
    
    // Get all editor levels for prerequisite info
    val editorLevels = EditorStorage.getAllLevels().associateBy { it.id }
    
    // Group levels by map ID
    val levelsByMap = worldLevels.groupBy { 
        it.level.mapId ?: it.level.editorLevelId ?: "unknown"
    }
    
    // Calculate depth of each level based on prerequisites (with memoization)
    val levelDepths = mutableMapOf<String, Int>()
    val currentPath = mutableSetOf<String>()
    for (worldLevel in worldLevels) {
        val levelId = worldLevel.level.editorLevelId ?: continue
        levelDepths[levelId] = calculateLevelDepth(levelId, editorLevels, levelDepths, currentPath)
    }
    
    val maxDepth = levelDepths.values.maxOrNull() ?: 0
    val locations = mutableListOf<WorldMapLocation>()
    val levelIdToLocation = mutableMapOf<String, WorldMapLocation>()
    
    // Create locations for each map group
    var locationIndex = 0
    for ((mapId, levels) in levelsByMap) {
        // Try to get custom position from the map file
        val editorMap = EditorStorage.getMap(mapId)
        val customPosition = editorMap?.worldMapPosition
        
        // Use custom position if available, otherwise calculate based on depth
        val x = if (customPosition != null) {
            // Position x is stored as percentage (0-100), convert to 0.0-1.0
            customPosition.x / 100f
        } else {
            // Calculate average depth for this map's levels
            val avgDepth = levels.mapNotNull { 
                levelDepths[it.level.editorLevelId] 
            }.average().takeIf { !it.isNaN() } ?: 0.0
            
            // Position based on depth (left to right)
            if (maxDepth > 0) {
                0.15f + ((avgDepth / maxDepth.toDouble()) * 0.7).toFloat()
            } else {
                0.5f
            }
        }
        
        val y = if (customPosition != null) {
            // Position y is stored as percentage (0-100), convert to 0.0-1.0
            customPosition.y / 100f
        } else {
            // Distribute locations vertically with some variation
            0.25f + (locationIndex % 3) * 0.25f + ((locationIndex / 3) % 2) * 0.1f
        }
        
        // Get location name from map or first level
        val mapName = editorMap?.name?.takeIf { it.isNotEmpty() } 
            ?: levels.firstOrNull()?.level?.name 
            ?: "Location"
        
        val location = WorldMapLocation(
            x = x.coerceIn(0.1f, 0.9f),
            y = y.coerceIn(0.15f, 0.85f),
            levelIds = levels.mapNotNull { it.level.editorLevelId },
            name = mapName
        )
        
        locations.add(location)
        
        // Map each level ID to its location
        for (levelId in location.levelIds) {
            levelIdToLocation[levelId] = location
        }
        
        locationIndex++
    }
    
    // Generate road connections based on prerequisites
    val roads = mutableListOf<WorldMapRoad>()
    val addedRoads = mutableSetOf<Pair<WorldMapLocation, WorldMapLocation>>()
    
    for (level in editorLevels.values) {
        val levelLocation = levelIdToLocation[level.id] ?: continue
        
        for (prereqId in level.prerequisites) {
            val prereqLocation = levelIdToLocation[prereqId] ?: continue
            
            // Avoid duplicate roads (in either direction)
            val roadPair = if (prereqLocation.x < levelLocation.x) {
                prereqLocation to levelLocation
            } else {
                levelLocation to prereqLocation
            }
            
            if (roadPair !in addedRoads && prereqLocation != levelLocation) {
                addedRoads.add(roadPair)
                roads.add(WorldMapRoad(
                    fromLocation = prereqLocation,
                    toLocation = levelLocation
                ))
            }
        }
    }
    
    return locations to roads
}

/**
 * Calculate the depth of a level in the prerequisite tree using memoization.
 * This is optimized to avoid redundant calculations and handle cycles.
 */
private fun calculateLevelDepth(
    levelId: String, 
    editorLevels: Map<String, de.egril.defender.editor.EditorLevel>,
    depthCache: MutableMap<String, Int>,
    currentPath: MutableSet<String>
): Int {
    // Return cached depth if available
    if (levelId in depthCache) return depthCache[levelId]!!
    
    // Avoid infinite loops - if we've seen this level in the current path, treat as cycle
    if (levelId in currentPath) return 0
    currentPath.add(levelId)
    
    val level = editorLevels[levelId]
    if (level == null || level.prerequisites.isEmpty()) {
        depthCache[levelId] = 0
        currentPath.remove(levelId)
        return 0
    }
    
    val maxPrereqDepth = level.prerequisites.maxOfOrNull { prereqId ->
        calculateLevelDepth(prereqId, editorLevels, depthCache, currentPath)
    } ?: 0
    
    val depth = maxPrereqDepth + 1
    depthCache[levelId] = depth
    currentPath.remove(levelId)
    
    return depth
}
