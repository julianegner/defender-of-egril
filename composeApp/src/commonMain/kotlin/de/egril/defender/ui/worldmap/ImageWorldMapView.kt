package de.egril.defender.ui.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.editor.WorldMapPoint

/**
 * World map location data for placing level markers on the map.
 * This is a view model for display purposes.
 */
data class WorldMapLocation(
    val id: String,  // Location ID from WorldMapLocationData
    val x: Float,  // X position as fraction of map width (0.0 to 1.0)
    val y: Float,  // Y position as fraction of map height (0.0 to 1.0)
    val levelIds: List<String>,  // Editor level IDs at this location
    val name: String  // Display name for this location
)

/**
 * Road connection between two locations with optional curve control points
 */
data class WorldMapRoad(
    val fromLocation: WorldMapLocation,
    val toLocation: WorldMapLocation,
    val controlPoints: List<Pair<Float, Float>> = emptyList()  // Control points as (x, y) fractions
)

/**
 * Filter levels at a location to only include those that are ready to play.
 */
private fun getPlayableLevelsAtLocation(
    worldLevels: List<WorldLevel>,
    location: WorldMapLocation
): List<WorldLevel> {
    return worldLevels.filter { worldLevel ->
        val levelId = worldLevel.level.editorLevelId
        levelId != null && levelId in location.levelIds && EditorStorage.isLevelReadyToPlay(levelId)
    }
}

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
                        // Only show levels that are ready to play (not misconfigured)
                        val levelsAtLocation = getPlayableLevelsAtLocation(worldLevels, location)
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
            
            // Location markers overlay - clickable markers
            LocationMarkersOverlay(
                locations = locations,
                worldLevels = worldLevels,
                containerSize = containerSize,
                isDarkMode = isDarkMode,
                onLocationClicked = onLocationClicked
            )
        }
    }
}

/**
 * Overlay that draws road connections between locations
 * Supports both straight lines and curved paths with control points
 */
@Composable
private fun BoxScope.RoadConnectionsOverlay(
    roads: List<WorldMapRoad>,
    containerSize: IntSize,
    isDarkMode: Boolean
) {
    val roadColor = if (isDarkMode) Color(0xFF8B4513) else Color(0xFFA0522D)  // Brown road color
    
    // Calculate actual image bounds within container (accounting for ContentScale.Fit)
    val imageAspectRatio = 1200f / 800f // Default world map aspect ratio
    val containerAspectRatio = containerSize.width.toFloat() / containerSize.height.toFloat().coerceAtLeast(1f)
    
    val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
        // Container is wider - fit to height, center horizontally
        val h = containerSize.height.toFloat()
        val w = h * imageAspectRatio
        val offsetX = (containerSize.width - w) / 2f
        listOf(w, h, offsetX, 0f)
    } else {
        // Container is taller - fit to width, center vertically
        val w = containerSize.width.toFloat()
        val h = w / imageAspectRatio
        val offsetY = (containerSize.height - h) / 2f
        listOf(w, h, 0f, offsetY)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (road in roads) {
            // Convert normalized coordinates to actual screen position within the image bounds
            val startX = imageOffsetX + road.fromLocation.x * imageWidth
            val startY = imageOffsetY + road.fromLocation.y * imageHeight
            val endX = imageOffsetX + road.toLocation.x * imageWidth
            val endY = imageOffsetY + road.toLocation.y * imageHeight
            
            if (road.controlPoints.isEmpty()) {
                // Draw straight line
                drawLine(
                    color = roadColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                )
            } else {
                // Draw curved path through control points
                val allPoints = mutableListOf(Offset(startX, startY))
                for (cp in road.controlPoints) {
                    allPoints.add(Offset(imageOffsetX + cp.first * imageWidth, imageOffsetY + cp.second * imageHeight))
                }
                allPoints.add(Offset(endX, endY))
                
                // Draw line segments between points
                for (i in 0 until allPoints.size - 1) {
                    drawLine(
                        color = roadColor,
                        start = allPoints[i],
                        end = allPoints[i + 1],
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                    )
                }
            }
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
    isDarkMode: Boolean,
    onLocationClicked: (WorldMapLocation, List<WorldLevel>) -> Unit
) {
    // Calculate actual image bounds within container (accounting for ContentScale.Fit)
    val imageAspectRatio = 1200f / 800f // Default world map aspect ratio
    val containerAspectRatio = containerSize.width.toFloat() / containerSize.height.toFloat().coerceAtLeast(1f)
    
    val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
        // Container is wider - fit to height, center horizontally
        val h = containerSize.height.toFloat()
        val w = h * imageAspectRatio
        val offsetX = (containerSize.width - w) / 2f
        listOf(w, h, offsetX, 0f)
    } else {
        // Container is taller - fit to width, center vertically
        val w = containerSize.width.toFloat()
        val h = w / imageAspectRatio
        val offsetY = (containerSize.height - h) / 2f
        listOf(w, h, 0f, offsetY)
    }
    
    // Draw each location marker
    for (location in locations) {
        // Only consider levels that are ready to play (not misconfigured)
        val levelsAtLocation = getPlayableLevelsAtLocation(worldLevels, location)
        
        // Skip if no playable levels at this location
        if (levelsAtLocation.isEmpty()) continue
        
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
        
        // Calculate marker position accounting for image bounds within container
        val markerSize = 40.dp
        
        // Position the marker using Box alignment offset
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Use fractional positioning within the image bounds
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align { size, space, _ ->
                        // Calculate position based on fraction of image (not container)
                        // Account for image offset within container
                        val xPos = imageOffsetX + (location.x * imageWidth) - size.width / 2
                        val yPos = imageOffsetY + (location.y * imageHeight) - size.height / 2
                        androidx.compose.ui.unit.IntOffset(xPos.toInt(), yPos.toInt())
                    }
            ) {
                // Location name label above the marker - white text on semi-transparent dark gray
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xB3404040),  // Semi-transparent dark gray (70% opacity)
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Circular marker
                Surface(
                    modifier = Modifier
                        .size(markerSize)
                        .clickable {
                            onLocationClicked(location, levelsAtLocation)
                        },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = markerColor,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = levelsAtLocation.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generate world map locations and road connections from the available levels.
 * First tries to use WorldMapData from the worldmap.json file, then falls back
 * to auto-generation based on level prerequisites.
 */
private fun generateWorldMapLocationsAndRoads(worldLevels: List<WorldLevel>): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
    if (worldLevels.isEmpty()) return emptyList<WorldMapLocation>() to emptyList()
    
    // Get all editor levels for prerequisite info
    val editorLevels = EditorStorage.getAllLevels().associateBy { it.id }
    val allMaps = EditorStorage.getAllMaps()
    
    // Try to use WorldMapData first
    val worldMapData = EditorStorage.getWorldMapData()
    
    if (worldMapData.locations.isNotEmpty()) {
        // Use locations from worldmap.json
        return generateFromWorldMapData(worldMapData, worldLevels, editorLevels, allMaps)
    }
    
    // Fall back to auto-generation
    return generateAutoLocationsAndRoads(worldLevels, editorLevels, allMaps)
}

/**
 * Generate locations and roads from the WorldMapData file.
 * Only shows locations that have at least one ready-to-play level.
 * Roads are always shown if they match level prerequisites.
 */
private fun generateFromWorldMapData(
    worldMapData: WorldMapData,
    worldLevels: List<WorldLevel>,
    editorLevels: Map<String, de.egril.defender.editor.EditorLevel>,
    allMaps: List<de.egril.defender.editor.EditorMap>
): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
    val locations = mutableListOf<WorldMapLocation>()
    val locationById = mutableMapOf<String, WorldMapLocation>()
    
    // Convert WorldMapLocationData to WorldMapLocation
    for (locationData in worldMapData.locations) {
        // Check if at least one level at this location is ready to play
        val hasPlayableLevel = locationData.levelIds.any { levelId ->
            EditorStorage.isLevelReadyToPlay(levelId)
        }
        
        // Only add location if it has at least one playable level
        if (hasPlayableLevel) {
            val (x, y) = locationData.position.toNormalized()
            val location = WorldMapLocation(
                id = locationData.id,
                x = x.coerceIn(0.1f, 0.9f),
                y = y.coerceIn(0.15f, 0.85f),
                levelIds = locationData.levelIds,
                name = locationData.name
            )
            locations.add(location)
            locationById[locationData.id] = location
        }
    }
    
    // Generate roads from WorldMapData paths
    // Roads are shown even if source/destination locations are hidden
    val roads = mutableListOf<WorldMapRoad>()
    
    for (pathData in worldMapData.paths) {
        // Check if this path matches level prerequisites
        if (!pathData.isValidConnection(worldMapData.locations, editorLevels.values.toList())) {
            continue
        }
        
        val fromLocation = locationById[pathData.fromLocationId]
        val toLocation = locationById[pathData.toLocationId]
        
        // If both locations are visible, use them directly
        if (fromLocation != null && toLocation != null) {
            val controlPoints = pathData.controlPoints.map { it.toNormalized() }
            roads.add(WorldMapRoad(
                fromLocation = fromLocation,
                toLocation = toLocation,
                controlPoints = controlPoints
            ))
        }
        // If locations are hidden but path should still be visible, create temporary locations
        else {
            val fromLocationData = worldMapData.findLocation(pathData.fromLocationId)
            val toLocationData = worldMapData.findLocation(pathData.toLocationId)
            
            if (fromLocationData != null && toLocationData != null) {
                val (fromX, fromY) = fromLocationData.position.toNormalized()
                val (toX, toY) = toLocationData.position.toNormalized()
                
                val tempFromLocation = fromLocation ?: WorldMapLocation(
                    id = fromLocationData.id,
                    x = fromX.coerceIn(0.1f, 0.9f),
                    y = fromY.coerceIn(0.15f, 0.85f),
                    levelIds = emptyList(),
                    name = fromLocationData.name
                )
                val tempToLocation = toLocation ?: WorldMapLocation(
                    id = toLocationData.id,
                    x = toX.coerceIn(0.1f, 0.9f),
                    y = toY.coerceIn(0.15f, 0.85f),
                    levelIds = emptyList(),
                    name = toLocationData.name
                )
                
                val controlPoints = pathData.controlPoints.map { it.toNormalized() }
                roads.add(WorldMapRoad(
                    fromLocation = tempFromLocation,
                    toLocation = tempToLocation,
                    controlPoints = controlPoints
                ))
            }
        }
    }
    
    return locations to roads
}

/**
 * Auto-generate locations and roads based on level prerequisites.
 * Used when no WorldMapData file exists.
 */
private fun generateAutoLocationsAndRoads(
    worldLevels: List<WorldLevel>,
    editorLevels: Map<String, de.egril.defender.editor.EditorLevel>,
    allMaps: List<de.egril.defender.editor.EditorMap>
): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
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
        val editorMap = allMaps.find { it.id == mapId }
        val customPosition = editorMap?.worldMapPosition
        
        // Use custom position if available, otherwise calculate based on depth
        val x = if (customPosition != null) {
            // Position x is stored as permille (0-1000), convert to 0.0-1.0
            customPosition.x / 1000f
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
            // Position y is stored as permille (0-1000), convert to 0.0-1.0
            customPosition.y / 1000f
        } else {
            // Distribute locations vertically with some variation
            0.25f + (locationIndex % 3) * 0.25f + ((locationIndex / 3) % 2) * 0.1f
        }
        
        // Get location name from map or first level
        val mapName = editorMap?.name?.takeIf { it.isNotEmpty() } 
            ?: levels.firstOrNull()?.level?.name 
            ?: "Location"
        
        val location = WorldMapLocation(
            id = mapId,
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
    val addedRoads = mutableSetOf<Pair<String, String>>()
    
    for (level in editorLevels.values) {
        val levelLocation = levelIdToLocation[level.id] ?: continue
        
        for (prereqId in level.prerequisites) {
            val prereqLocation = levelIdToLocation[prereqId] ?: continue
            
            // Avoid duplicate roads (in either direction)
            val roadPair = if (prereqLocation.id < levelLocation.id) {
                prereqLocation.id to levelLocation.id
            } else {
                levelLocation.id to prereqLocation.id
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
