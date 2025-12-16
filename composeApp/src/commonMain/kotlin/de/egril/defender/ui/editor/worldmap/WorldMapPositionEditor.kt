@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.ConnectionType
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.editor.WorldMapPoint
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import org.jetbrains.compose.resources.painterResource
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*

/**
 * World Map Position Editor content - allows placing locations on the world map visually.
 * Uses the new WorldMapData model with locations and curved paths.
 */
@Composable
fun WorldMapPositionEditorContent() {
    var worldMapData by remember { mutableStateOf(EditorStorage.getWorldMapData()) }
    var allLevels by remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var hoveredPosition by remember { mutableStateOf<WorldMapPoint?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showEditPathDialog by remember { mutableStateOf<WorldMapPathData?>(null) }
    var showAddConnectionDialog by remember { mutableStateOf(false) }
    var connectionCreationState by remember { mutableStateOf<Pair<String, String?>?>(null) } // (fromLocationId, toLocationId?)
    
    // Waypoint interaction state
    var selectedPathForEdit by remember { mutableStateOf<WorldMapPathData?>(null) }
    var draggingWaypointIndex by remember { mutableStateOf<Int?>(null) }
    var hoveredPathId by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    val isDarkMode = AppSettings.isDarkMode.value
    
    // Reload data
    LaunchedEffect(Unit) {
        worldMapData = EditorStorage.getWorldMapData()
        allLevels = EditorStorage.getAllLevels()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Title
        Text(
            text = stringResource(Res.string.world_map_positions),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                text = stringResource(Res.string.world_map_position_instructions),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left side: World Map with clickable positions
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { containerSize = it }
                ) {
                    WorldMapCanvas(
                        worldMapData = worldMapData,
                        allLevels = allLevels,
                        selectedLocationId = selectedLocationId,
                        hoveredPosition = hoveredPosition,
                        containerSize = containerSize,
                        isDarkMode = isDarkMode,
                        selectedPathForEdit = selectedPathForEdit,
                        draggingWaypointIndex = draggingWaypointIndex,
                        hoveredPathId = hoveredPathId,
                        onPositionClick = { point ->
                            // Update position for selected location
                            if (selectedLocationId != null) {
                                val location = worldMapData.locations.find { it.id == selectedLocationId }
                                if (location != null) {
                                    val updatedLocation = location.copy(position = point)
                                    EditorStorage.saveWorldMapLocation(updatedLocation)
                                    worldMapData = EditorStorage.getWorldMapData()
                                }
                            }
                        },
                        onPathClick = { path ->
                            // Save the path with the new waypoint
                            EditorStorage.saveWorldMapPath(path)
                            worldMapData = EditorStorage.getWorldMapData()
                            selectedPathForEdit = path
                        },
                        onWaypointDragStart = { pathData, waypointIndex ->
                            selectedPathForEdit = pathData
                            draggingWaypointIndex = waypointIndex
                        },
                        onWaypointDrag = { pathData, waypointIndex, newPosition ->
                            if (selectedPathForEdit == pathData) {
                                val updatedControlPoints = pathData.controlPoints.toMutableList()
                                updatedControlPoints[waypointIndex] = newPosition
                                val updatedPath = pathData.copy(controlPoints = updatedControlPoints)
                                EditorStorage.saveWorldMapPath(updatedPath)
                                worldMapData = EditorStorage.getWorldMapData()
                                selectedPathForEdit = updatedPath
                            }
                        },
                        onWaypointDragEnd = {
                            draggingWaypointIndex = null
                        },
                        onPathHover = { pathId ->
                            hoveredPathId = pathId
                        },
                        onHoverChange = { point ->
                            hoveredPosition = point
                        }
                    )
                    
                    // Hover tooltip
                    if (hoveredPosition != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Position: ${hoveredPosition!!.x}, ${hoveredPosition!!.y}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            
            // Right side: Locations and Paths lists
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Add Location button
                    Button(
                        onClick = { showAddLocationDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text("+ Add Location")
                    }
                    
                    Text(
                        text = "Locations",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(worldMapData.locations) { location ->
                            LocationListItem(
                                location = location,
                                worldMapData = worldMapData,
                                allLevels = allLevels,
                                isSelected = location.id == selectedLocationId,
                                onClick = {
                                    selectedLocationId = if (selectedLocationId == location.id) null else location.id
                                },
                                onDelete = {
                                    EditorStorage.deleteWorldMapLocation(location.id)
                                    worldMapData = EditorStorage.getWorldMapData()
                                    if (selectedLocationId == location.id) {
                                        selectedLocationId = null
                                    }
                                }
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connections (${worldMapData.paths.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Button(
                            onClick = { showAddConnectionDialog = true },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("+")
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(worldMapData.paths) { path ->
                            PathListItem(
                                path = path,
                                locations = worldMapData.locations,
                                allLevels = allLevels,
                                onClick = { showEditPathDialog = path },
                                onDelete = {
                                    EditorStorage.deleteWorldMapPath(path.fromLocationId, path.toLocationId)
                                    worldMapData = EditorStorage.getWorldMapData()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Location Dialog
    if (showAddLocationDialog) {
        AddLocationDialog(
            allLevels = allLevels,
            existingLocations = worldMapData.locations,
            onDismiss = { showAddLocationDialog = false },
            onConfirm = { newLocation ->
                EditorStorage.saveWorldMapLocation(newLocation)
                worldMapData = EditorStorage.getWorldMapData()
                showAddLocationDialog = false
            }
        )
    }
    
    // Edit Path Dialog
    if (showEditPathDialog != null) {
        EditPathDialog(
            path = showEditPathDialog!!,
            onDismiss = { showEditPathDialog = null },
            onConfirm = { updatedPath ->
                EditorStorage.saveWorldMapPath(updatedPath)
                worldMapData = EditorStorage.getWorldMapData()
                showEditPathDialog = null
            }
        )
    }
    
    // Add Connection Dialog
    if (showAddConnectionDialog) {
        AddConnectionDialog(
            locations = worldMapData.locations,
            onDismiss = { showAddConnectionDialog = false },
            onConfirm = { newPath ->
                EditorStorage.saveWorldMapPath(newPath)
                worldMapData = EditorStorage.getWorldMapData()
                showAddConnectionDialog = false
            }
        )
    }
}

/**
 * Canvas showing the world map background with location markers and paths.
 * Supports interactive waypoint editing by clicking on connection lines.
 */
@Composable
private fun WorldMapCanvas(
    worldMapData: WorldMapData,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    selectedLocationId: String?,
    hoveredPosition: WorldMapPoint?,
    containerSize: IntSize,
    isDarkMode: Boolean,
    selectedPathForEdit: WorldMapPathData?,
    draggingWaypointIndex: Int?,
    hoveredPathId: Pair<String, String>?,
    onPositionClick: (WorldMapPoint) -> Unit,
    onPathClick: (WorldMapPathData) -> Unit,
    onWaypointDragStart: (WorldMapPathData, Int) -> Unit,
    onWaypointDrag: (WorldMapPathData, Int, WorldMapPoint) -> Unit,
    onWaypointDragEnd: () -> Unit,
    onPathHover: (Pair<String, String>?) -> Unit,
    onHoverChange: (WorldMapPoint?) -> Unit
) {
    // Get the painter to access image dimensions
    val mapPainter = painterResource(Res.drawable.world_map_background)
    val imageAspectRatio = mapPainter.intrinsicSize.width / mapPainter.intrinsicSize.height
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = mapPainter,
            contentDescription = "World Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Canvas overlay for markers and interaction
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageAspectRatio) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val offset = event.changes.firstOrNull()?.position ?: continue
                            
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> {
                                    // Calculate actual image bounds within container (accounting for ContentScale.Fit)
                                    val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                                    val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                                        val h = size.height.toFloat()
                                        val w = h * imageAspectRatio
                                        val ox = (size.width - w) / 2f
                                        listOf(w, h, ox, 0f)
                                    } else {
                                        val w = size.width.toFloat()
                                        val h = w / imageAspectRatio
                                        val oy = (size.height - h) / 2f
                                        listOf(w, h, 0f, oy)
                                    }
                                    
                                    // Calculate position as permille (0-1000) relative to image bounds
                                    val x = (((offset.x - imageOffsetX) / imageWidth) * 1000).toInt().coerceIn(0, 1000)
                                    val y = (((offset.y - imageOffsetY) / imageHeight) * 1000).toInt().coerceIn(0, 1000)
                                    onHoverChange(WorldMapPoint(x, y))
                                }
                                PointerEventType.Exit -> {
                                    onHoverChange(null)
                                }
                                else -> {}
                            }
                        }
                    }
                }
                .pointerInput(imageAspectRatio, worldMapData) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Calculate actual image bounds
                            val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                            val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                                val h = size.height.toFloat()
                                val w = h * imageAspectRatio
                                val ox = (size.width - w) / 2f
                                listOf(w, h, ox, 0f)
                            } else {
                                val w = size.width.toFloat()
                                val h = w / imageAspectRatio
                                val oy = (size.height - h) / 2f
                                listOf(w, h, 0f, oy)
                            }
                            
                            // Check if drag started on a waypoint
                            val waypointRadius = 20f
                            for (path in worldMapData.paths) {
                                for ((index, waypoint) in path.controlPoints.withIndex()) {
                                    val wpScreenX = imageOffsetX + (waypoint.x / 1000f) * imageWidth
                                    val wpScreenY = imageOffsetY + (waypoint.y / 1000f) * imageHeight
                                    val dist = kotlin.math.sqrt(
                                        (offset.x - wpScreenX) * (offset.x - wpScreenX) +
                                        (offset.y - wpScreenY) * (offset.y - wpScreenY)
                                    )
                                    if (dist < waypointRadius) {
                                        onWaypointDragStart(path, index)
                                        return@detectDragGestures
                                    }
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (selectedPathForEdit != null && draggingWaypointIndex != null) {
                                // Calculate new waypoint position
                                val offset = change.position
                                val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                                val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                                    val h = size.height.toFloat()
                                    val w = h * imageAspectRatio
                                    val ox = (size.width - w) / 2f
                                    listOf(w, h, ox, 0f)
                                } else {
                                    val w = size.width.toFloat()
                                    val h = w / imageAspectRatio
                                    val oy = (size.height - h) / 2f
                                    listOf(w, h, 0f, oy)
                                }
                                
                                val x = (((offset.x - imageOffsetX) / imageWidth) * 1000).toInt().coerceIn(0, 1000)
                                val y = (((offset.y - imageOffsetY) / imageHeight) * 1000).toInt().coerceIn(0, 1000)
                                onWaypointDrag(selectedPathForEdit, draggingWaypointIndex, WorldMapPoint(x, y))
                            }
                        },
                        onDragEnd = {
                            onWaypointDragEnd()
                        },
                        onDragCancel = {
                            onWaypointDragEnd()
                        }
                    )
                }
                .pointerInput(imageAspectRatio, worldMapData) {
                    detectTapGestures { offset ->
                        // Calculate actual image bounds within container (accounting for ContentScale.Fit)
                        val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                        val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                            val h = size.height.toFloat()
                            val w = h * imageAspectRatio
                            val ox = (size.width - w) / 2f
                            listOf(w, h, ox, 0f)
                        } else {
                            val w = size.width.toFloat()
                            val h = w / imageAspectRatio
                            val oy = (size.height - h) / 2f
                            listOf(w, h, 0f, oy)
                        }
                        
                        // Calculate position as permille (0-1000) relative to image bounds
                        val x = (((offset.x - imageOffsetX) / imageWidth) * 1000).toInt().coerceIn(0, 1000)
                        val y = (((offset.y - imageOffsetY) / imageHeight) * 1000).toInt().coerceIn(0, 1000)
                        val clickPoint = WorldMapPoint(x, y)
                        
                        // Check if click is near any waypoint (for existing waypoints)
                        val waypointRadius = 20f // pixels
                        var foundWaypoint = false
                        for (path in worldMapData.paths) {
                            for ((index, waypoint) in path.controlPoints.withIndex()) {
                                val wpScreenX = imageOffsetX + (waypoint.x / 1000f) * imageWidth
                                val wpScreenY = imageOffsetY + (waypoint.y / 1000f) * imageHeight
                                val dist = kotlin.math.sqrt(
                                    (offset.x - wpScreenX) * (offset.x - wpScreenX) +
                                    (offset.y - wpScreenY) * (offset.y - wpScreenY)
                                )
                                if (dist < waypointRadius) {
                                    foundWaypoint = true
                                    break
                                }
                            }
                            if (foundWaypoint) break
                        }
                        
                        // Check if click is near any connection line
                        if (!foundWaypoint) {
                            val lineClickThreshold = 15f // pixels
                            for (path in worldMapData.paths) {
                                val fromLoc = worldMapData.locations.find { it.id == path.fromLocationId }
                                val toLoc = worldMapData.locations.find { it.id == path.toLocationId }
                                if (fromLoc != null && toLoc != null) {
                                    // Build list of line segments
                                    val points = mutableListOf<Pair<Float, Float>>()
                                    points.add(
                                        (imageOffsetX + (fromLoc.position.x / 1000f) * imageWidth) to
                                        (imageOffsetY + (fromLoc.position.y / 1000f) * imageHeight)
                                    )
                                    for (cp in path.controlPoints) {
                                        points.add(
                                            (imageOffsetX + (cp.x / 1000f) * imageWidth) to
                                            (imageOffsetY + (cp.y / 1000f) * imageHeight)
                                        )
                                    }
                                    points.add(
                                        (imageOffsetX + (toLoc.position.x / 1000f) * imageWidth) to
                                        (imageOffsetY + (toLoc.position.y / 1000f) * imageHeight)
                                    )
                                    
                                    // Check distance to each segment
                                    for (i in 0 until points.size - 1) {
                                        val (x1, y1) = points[i]
                                        val (x2, y2) = points[i + 1]
                                        val dist = distanceToLineSegment(offset.x, offset.y, x1, y1, x2, y2)
                                        if (dist < lineClickThreshold) {
                                            // Add waypoint at click position and save
                                            val updatedControlPoints = path.controlPoints.toMutableList()
                                            // Insert at the appropriate position (after segment i-1)
                                            val insertIndex = if (i == 0) 0 else i
                                            updatedControlPoints.add(insertIndex, clickPoint)
                                            val updatedPath = path.copy(controlPoints = updatedControlPoints)
                                            onPathClick(updatedPath)
                                            return@detectTapGestures
                                        }
                                    }
                                }
                            }
                        }
                        
                        // If no path was clicked, handle as location position click
                        onPositionClick(clickPoint)
                    }
                }
        ) {
            val markerRadius = 15f
            
            // Calculate actual image bounds within container (accounting for ContentScale.Fit)
            val containerAspectRatio = size.width / size.height
            val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                val h = size.height
                val w = h * imageAspectRatio
                val ox = (size.width - w) / 2f
                listOf(w, h, ox, 0f)
            } else {
                val w = size.width
                val h = w / imageAspectRatio
                val oy = (size.height - h) / 2f
                listOf(w, h, 0f, oy)
            }
            
            // Draw paths first (so they appear behind locations)
            val locationById = worldMapData.locations.associateBy { it.id }
            
            for (path in worldMapData.paths) {
                val fromLocation = locationById[path.fromLocationId] ?: continue
                val toLocation = locationById[path.toLocationId] ?: continue
                
                // Check if path is valid
                val isValid = path.isValidConnection(worldMapData.locations, allLevels)
                
                // Determine color and dash pattern based on connection type and validity
                val (lineColor, dashPattern) = when (path.type) {
                    ConnectionType.ROAD -> {
                        val color = if (isValid) {
                            if (isDarkMode) Color(0xFFD2691E) else Color(0xFFA0522D)
                        } else {
                            Color(0xFFFFA500) // Orange for invalid
                        }
                        color to null
                    }
                    ConnectionType.SEA_ROUTE -> {
                        val color = if (isValid) {
                            if (isDarkMode) Color(0xFF1E90FF) else Color(0xFF00008B)
                        } else {
                            Color(0xFFFFA500) // Orange for invalid
                        }
                        color to PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    }
                }
                
                val startX = imageOffsetX + (fromLocation.position.x / 1000f) * imageWidth
                val startY = imageOffsetY + (fromLocation.position.y / 1000f) * imageHeight
                val endX = imageOffsetX + (toLocation.position.x / 1000f) * imageWidth
                val endY = imageOffsetY + (toLocation.position.y / 1000f) * imageHeight
                
                if (path.controlPoints.isEmpty()) {
                    // Draw straight line
                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = dashPattern
                    )
                } else {
                    // Draw curved path through control points
                    var prevX = startX
                    var prevY = startY
                    
                    for (cp in path.controlPoints) {
                        val cpX = imageOffsetX + (cp.x / 1000f) * imageWidth
                        val cpY = imageOffsetY + (cp.y / 1000f) * imageHeight
                        
                        drawLine(
                            color = lineColor,
                            start = Offset(prevX, prevY),
                            end = Offset(cpX, cpY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                            pathEffect = dashPattern
                        )
                        
                        prevX = cpX
                        prevY = cpY
                    }
                    
                    // Draw final segment
                    drawLine(
                        color = lineColor,
                        start = Offset(prevX, prevY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = dashPattern
                    )
                }
            }
            
            // Draw all location markers
            for (location in worldMapData.locations) {
                val x = imageOffsetX + (location.position.x / 1000f) * imageWidth
                val y = imageOffsetY + (location.position.y / 1000f) * imageHeight
                
                val isSelected = location.id == selectedLocationId
                val hasPlayableLevels = location.levelIds.any { levelId ->
                    EditorStorage.isLevelReadyToPlay(levelId)
                }
                
                val markerColor = when {
                    isSelected -> Color(0xFF2196F3) // Blue for selected
                    hasPlayableLevels -> if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF2E7D32) // Green for playable
                    else -> if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFF757575) // Grey for not playable
                }
                
                // Draw marker circle
                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) markerRadius * 1.3f else markerRadius,
                    center = Offset(x, y)
                )
                
                // Draw border
                drawCircle(
                    color = if (isDarkMode) Color.White else Color.Black,
                    radius = if (isSelected) markerRadius * 1.3f else markerRadius,
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            
            // Draw hover indicator for selected location
            if (selectedLocationId != null && hoveredPosition != null) {
                val x = imageOffsetX + (hoveredPosition.x / 1000f) * imageWidth
                val y = imageOffsetY + (hoveredPosition.y / 1000f) * imageHeight
                
                drawCircle(
                    color = Color(0x802196F3), // Semi-transparent blue
                    radius = markerRadius * 1.5f,
                    center = Offset(x, y)
                )
            }
            
            // Draw waypoint handles for all connections
            val waypointRadius = 8f
            for (path in worldMapData.paths) {
                for ((index, waypoint) in path.controlPoints.withIndex()) {
                    val wpX = imageOffsetX + (waypoint.x / 1000f) * imageWidth
                    val wpY = imageOffsetY + (waypoint.y / 1000f) * imageHeight
                    
                    // Determine if this waypoint is being dragged
                    val isDragging = selectedPathForEdit == path && draggingWaypointIndex == index
                    
                    // Draw waypoint handle
                    drawCircle(
                        color = if (isDragging) Color(0xFFFF9800) else Color(0xFF2196F3), // Orange when dragging, blue otherwise
                        radius = if (isDragging) waypointRadius * 1.5f else waypointRadius,
                        center = Offset(wpX, wpY)
                    )
                    
                    // Draw white border
                    drawCircle(
                        color = Color.White,
                        radius = if (isDragging) waypointRadius * 1.5f else waypointRadius,
                        center = Offset(wpX, wpY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

/**
 * List item for a location
 */
@Composable
private fun LocationListItem(
    location: WorldMapLocationData,
    worldMapData: WorldMapData,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Check if location has unfulfilled prerequisites
    val hasUnfulfilledPrereqs = worldMapData.hasLocationWithUnfulfilledPrerequisites(location.id, allLevels)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Warning icon if location has unfulfilled prerequisites
                    if (hasUnfulfilledPrereqs) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFA500) // Orange
                        )
                    }
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Position: ${location.position.x}, ${location.position.y} | ${location.levelIds.size} levels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Delete button
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

/**
 * List item for a path
 */
@Composable
private fun PathListItem(
    path: WorldMapPathData,
    locations: List<WorldMapLocationData>,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val fromName = locations.find { it.id == path.fromLocationId }?.name ?: path.fromLocationId
    val toName = locations.find { it.id == path.toLocationId }?.name ?: path.toLocationId
    
    // Check if connection is valid based on level prerequisites
    val isValid = path.isValidConnection(locations, allLevels)
    
    // Determine connection type display
    val connectionTypeText = when (path.type) {
        ConnectionType.ROAD -> "Road"
        ConnectionType.SEA_ROUTE -> "Sea"
    }
    val connectionTypeColor = when (path.type) {
        ConnectionType.ROAD -> Color(0xFFA0522D) // Brown
        ConnectionType.SEA_ROUTE -> Color(0xFF00008B) // Dark blue
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Warning icon if connection is invalid
                    if (!isValid) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFA500) // Orange
                        )
                    }
                    Text(
                        text = "$fromName → $toName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${path.controlPoints.size} waypoints",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = connectionTypeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = connectionTypeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = connectionTypeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // Delete button
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

/**
 * Dialog for adding a new location
 */
@Composable
private fun AddLocationDialog(
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    existingLocations: List<WorldMapLocationData>,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapLocationData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLevelIds by remember { mutableStateOf(setOf<String>()) }
    
    // Get levels not yet assigned to any location
    val assignedLevelIds = existingLocations.flatMap { it.levelIds }.toSet()
    val availableLevels = allLevels.filter { it.id !in assignedLevelIds }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Location") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select Levels:", style = MaterialTheme.typography.bodyMedium)
                
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(availableLevels) { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLevelIds = if (level.id in selectedLevelIds) {
                                        selectedLevelIds - level.id
                                    } else {
                                        selectedLevelIds + level.id
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = level.id in selectedLevelIds,
                                onCheckedChange = {
                                    selectedLevelIds = if (it) {
                                        selectedLevelIds + level.id
                                    } else {
                                        selectedLevelIds - level.id
                                    }
                                }
                            )
                            Text(level.title.ifEmpty { level.id })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedLevelIds.isNotEmpty()) {
                        val locationId = name.lowercase().replace(" ", "_")
                        onConfirm(WorldMapLocationData(
                            id = locationId,
                            name = name,
                            position = WorldMapPoint(500, 500), // Center by default
                            levelIds = selectedLevelIds.toList()
                        ))
                    }
                },
                enabled = name.isNotBlank() && selectedLevelIds.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for editing path control points
 */
@Composable
private fun EditPathDialog(
    path: WorldMapPathData,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapPathData) -> Unit
) {
    var controlPoints by remember { mutableStateOf(path.controlPoints.toMutableList()) }
    var connectionType by remember { mutableStateOf(path.type) }
    var newX by remember { mutableStateOf("") }
    var newY by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Connection") },
        text = {
            Column {
                Text(
                    "Connection: ${path.fromLocationId} → ${path.toLocationId}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Connection type selector
                Text("Connection Type:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { connectionType = ConnectionType.ROAD },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connectionType == ConnectionType.ROAD) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Road")
                    }
                    Button(
                        onClick = { connectionType = ConnectionType.SEA_ROUTE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connectionType == ConnectionType.SEA_ROUTE) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sea")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Waypoints (0-1000):", style = MaterialTheme.typography.bodySmall)
                
                LazyColumn(
                    modifier = Modifier.height(150.dp)
                ) {
                    items(controlPoints.size) { index ->
                        val cp = controlPoints[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}: (${cp.x}, ${cp.y})")
                            TextButton(onClick = {
                                controlPoints = controlPoints.toMutableList().apply { removeAt(index) }
                            }) {
                                Text("X", color = Color.Red)
                            }
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Add Waypoint:", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newX,
                        onValueChange = { newX = it.filter { c -> c.isDigit() } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newY,
                        onValueChange = { newY = it.filter { c -> c.isDigit() } },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val x = newX.toIntOrNull()?.coerceIn(0, 1000)
                            val y = newY.toIntOrNull()?.coerceIn(0, 1000)
                            if (x != null && y != null) {
                                controlPoints = controlPoints.toMutableList().apply { add(WorldMapPoint(x, y)) }
                                newX = ""
                                newY = ""
                            }
                        }
                    ) {
                        Text("+")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(path.copy(controlPoints = controlPoints, type = connectionType))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for adding a new connection between two locations
 */
@Composable
private fun AddConnectionDialog(
    locations: List<WorldMapLocationData>,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapPathData) -> Unit
) {
    var fromLocationId by remember { mutableStateOf<String?>(null) }
    var toLocationId by remember { mutableStateOf<String?>(null) }
    var connectionType by remember { mutableStateOf(ConnectionType.ROAD) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Connection") },
        text = {
            Column {
                Text("Select two locations to connect:", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // From location selector
                Text("From:", style = MaterialTheme.typography.bodySmall)
                LazyColumn(
                    modifier = Modifier.height(120.dp).padding(bottom = 8.dp)
                ) {
                    items(locations) { location ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fromLocationId = location.id }
                                .padding(8.dp)
                                .background(
                                    if (fromLocationId == location.id) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fromLocationId == location.id,
                                onClick = { fromLocationId = location.id }
                            )
                            Text(location.name)
                        }
                    }
                }
                
                // To location selector
                Text("To:", style = MaterialTheme.typography.bodySmall)
                LazyColumn(
                    modifier = Modifier.height(120.dp).padding(bottom = 8.dp)
                ) {
                    items(locations) { location ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toLocationId = location.id }
                                .padding(8.dp)
                                .background(
                                    if (toLocationId == location.id) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        Color.Transparent
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = toLocationId == location.id,
                                onClick = { toLocationId = location.id }
                            )
                            Text(location.name)
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Connection type selector
                Text("Connection Type:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { connectionType = ConnectionType.ROAD },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connectionType == ConnectionType.ROAD) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Road")
                    }
                    Button(
                        onClick = { connectionType = ConnectionType.SEA_ROUTE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connectionType == ConnectionType.SEA_ROUTE) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sea")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fromLocationId != null && toLocationId != null && fromLocationId != toLocationId) {
                        onConfirm(WorldMapPathData(
                            fromLocationId = fromLocationId!!,
                            toLocationId = toLocationId!!,
                            controlPoints = emptyList(),
                            type = connectionType
                        ))
                    }
                },
                enabled = fromLocationId != null && toLocationId != null && fromLocationId != toLocationId
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Calculate the perpendicular distance from a point to a line segment.
 */
private fun distanceToLineSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val lengthSquared = dx * dx + dy * dy
    
    if (lengthSquared == 0f) {
        // Line segment is actually a point
        return kotlin.math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1))
    }
    
    // Calculate projection of point onto line segment (clamped to segment)
    val t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared
    val tClamped = t.coerceIn(0f, 1f)
    
    // Calculate closest point on segment
    val closestX = x1 + tClamped * dx
    val closestY = y1 + tClamped * dy
    
    // Return distance to closest point
    return kotlin.math.sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY))
}

