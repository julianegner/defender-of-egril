@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.ConnectionType
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.editor.WorldMapPoint
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

/**
 * Canvas showing the world map background with location markers and paths.
 * Supports interactive waypoint editing by clicking on connection lines.
 */
@Composable
internal fun WorldMapCanvas(
    worldMapData: WorldMapData,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    selectedLocationId: String?,
    hoveredPosition: WorldMapPoint?,
    containerSize: IntSize,
    isDarkMode: Boolean,
    selectedPathIds: Pair<String, String>?,
    draggingWaypointIndex: Int?,
    hoveredPathId: Pair<String, String>?,
    connectionCreationMode: Boolean,
    connectionFromLocation: String?,
    waypointEditMode: Boolean,
    onLocationClick: (String) -> Unit,
    onPositionClick: (WorldMapPoint) -> Unit,
    onPathClick: (WorldMapPathData, Boolean) -> Unit, // path, addWaypoint
    onWaypointDragStart: (String, String, Int) -> Unit,
    onWaypointDrag: (String, String, Int, WorldMapPoint) -> Unit,
    onWaypointDragEnd: () -> Unit,
    onPathHover: (Pair<String, String>?) -> Unit,
    onHoverChange: (WorldMapPoint?) -> Unit,
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
            contentScale = ContentScale.Fit,
        )

        // Canvas overlay for markers and interaction
        Canvas(
            modifier =
                Modifier
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
                                        val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) =
                                            if (containerAspectRatio >
                                                imageAspectRatio
                                            ) {
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
                    }.pointerInput(imageAspectRatio, selectedPathIds, draggingWaypointIndex, waypointEditMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Only allow waypoint dragging in waypoint edit mode
                                if (!waypointEditMode) return@detectDragGestures

                                // Calculate actual image bounds
                                val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                                val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) =
                                    if (containerAspectRatio > imageAspectRatio) {
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
                                        val dist =
                                            kotlin.math.sqrt(
                                                (offset.x - wpScreenX) * (offset.x - wpScreenX) +
                                                    (offset.y - wpScreenY) * (offset.y - wpScreenY),
                                            )
                                        if (dist < waypointRadius) {
                                            onWaypointDragStart(path.fromLocationId, path.toLocationId, index)
                                            return@detectDragGestures
                                        }
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (selectedPathIds != null && draggingWaypointIndex != null) {
                                    // Calculate new waypoint position
                                    val offset = change.position
                                    val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                                    val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) =
                                        if (containerAspectRatio > imageAspectRatio) {
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
                                    onWaypointDrag(
                                        selectedPathIds.first,
                                        selectedPathIds.second,
                                        draggingWaypointIndex,
                                        WorldMapPoint(x, y),
                                    )
                                }
                            },
                            onDragEnd = {
                                onWaypointDragEnd()
                            },
                            onDragCancel = {
                                onWaypointDragEnd()
                            },
                        )
                    }.pointerInput(imageAspectRatio, worldMapData) {
                        detectTapGestures { offset ->
                            // Calculate actual image bounds within container (accounting for ContentScale.Fit)
                            val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                            val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) =
                                if (containerAspectRatio > imageAspectRatio) {
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

                            // Check if click is on a location marker (highest priority)
                            val locationRadius = 20f // pixels
                            for (location in worldMapData.locations) {
                                val locScreenX = imageOffsetX + (location.position.x / 1000f) * imageWidth
                                val locScreenY = imageOffsetY + (location.position.y / 1000f) * imageHeight
                                val dist =
                                    kotlin.math.sqrt(
                                        (offset.x - locScreenX) * (offset.x - locScreenX) +
                                            (offset.y - locScreenY) * (offset.y - locScreenY),
                                    )
                                if (dist < locationRadius) {
                                    onLocationClick(location.id)
                                    return@detectTapGestures
                                }
                            }

                            // Check if click is near any waypoint (for existing waypoints)
                            val waypointRadius = 20f // pixels
                            var foundWaypoint = false
                            for (path in worldMapData.paths) {
                                for ((index, waypoint) in path.controlPoints.withIndex()) {
                                    val wpScreenX = imageOffsetX + (waypoint.x / 1000f) * imageWidth
                                    val wpScreenY = imageOffsetY + (waypoint.y / 1000f) * imageHeight
                                    val dist =
                                        kotlin.math.sqrt(
                                            (offset.x - wpScreenX) * (offset.x - wpScreenX) +
                                                (offset.y - wpScreenY) * (offset.y - wpScreenY),
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
                                                (imageOffsetY + (fromLoc.position.y / 1000f) * imageHeight),
                                        )
                                        for (cp in path.controlPoints) {
                                            points.add(
                                                (imageOffsetX + (cp.x / 1000f) * imageWidth) to
                                                    (imageOffsetY + (cp.y / 1000f) * imageHeight),
                                            )
                                        }
                                        points.add(
                                            (imageOffsetX + (toLoc.position.x / 1000f) * imageWidth) to
                                                (imageOffsetY + (toLoc.position.y / 1000f) * imageHeight),
                                        )

                                        // Check distance to each segment
                                        var clickedOnLine = false
                                        var waypointInsertIndex = 0

                                        for (i in 0 until points.size - 1) {
                                            val (x1, y1) = points[i]
                                            val (x2, y2) = points[i + 1]
                                            val dist = distanceToLineSegment(offset.x, offset.y, x1, y1, x2, y2)
                                            if (dist < lineClickThreshold) {
                                                clickedOnLine = true
                                                // In waypoint edit mode, we want to insert waypoint at the correct position in the segment
                                                waypointInsertIndex = if (i == 0) 0 else i
                                                break
                                            }
                                        }

                                        if (clickedOnLine) {
                                            if (waypointEditMode) {
                                                // In waypoint edit mode: add waypoint at click position
                                                val updatedControlPoints = path.controlPoints.toMutableList()
                                                updatedControlPoints.add(waypointInsertIndex, clickPoint)
                                                val updatedPath = path.copy(controlPoints = updatedControlPoints)
                                                onPathClick(updatedPath, true)
                                            } else {
                                                // Not in waypoint edit mode: select the connection
                                                onPathClick(path, false)
                                            }
                                            return@detectTapGestures
                                        }
                                    }
                                }
                            }

                            // If no path was clicked, handle as location position click
                            onPositionClick(clickPoint)
                        }
                    },
        ) {
            val markerRadius = 15f

            // Calculate actual image bounds within container (accounting for ContentScale.Fit)
            val containerAspectRatio = size.width / size.height
            val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) =
                if (containerAspectRatio > imageAspectRatio) {
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
                // Helper function to get color and dash pattern for a segment type
                fun getSegmentStyle(segmentType: ConnectionType): Pair<Color, PathEffect?> =
                    when (segmentType) {
                        ConnectionType.ROAD -> {
                            val color =
                                if (isValid) {
                                    if (isDarkMode) Color(0xFFD2691E) else Color(0xFFA0522D)
                                } else {
                                    Color(0xFFFFA500) // Orange for invalid
                                }
                            color to null
                        }
                        ConnectionType.SEA_ROUTE -> {
                            val color =
                                if (isValid) {
                                    // Lighter cyan/aqua for better visibility against dark blue ocean
                                    if (isDarkMode) Color(0xFF00E5FF) else Color(0xFF00CED1)
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
                    // Draw straight line with segment type
                    val segmentType = path.getSegmentType(0)
                    val (lineColor, dashPattern) = getSegmentStyle(segmentType)
                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = dashPattern,
                    )
                } else {
                    // Draw curved path through control points with per-segment types
                    var prevX = startX
                    var prevY = startY
                    var segmentIndex = 0

                    for (cp in path.controlPoints) {
                        val cpX = imageOffsetX + (cp.x / 1000f) * imageWidth
                        val cpY = imageOffsetY + (cp.y / 1000f) * imageHeight

                        // Get style for this segment
                        val segmentType = path.getSegmentType(segmentIndex)
                        val (lineColor, dashPattern) = getSegmentStyle(segmentType)

                        drawLine(
                            color = lineColor,
                            start = Offset(prevX, prevY),
                            end = Offset(cpX, cpY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                            pathEffect = dashPattern,
                        )

                        prevX = cpX
                        prevY = cpY
                        segmentIndex++
                    }

                    // Draw final segment
                    val segmentType = path.getSegmentType(segmentIndex)
                    val (lineColor, dashPattern) = getSegmentStyle(segmentType)
                    drawLine(
                        color = lineColor,
                        start = Offset(prevX, prevY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = dashPattern,
                    )
                }
            }

            // Draw all location markers
            for (location in worldMapData.locations) {
                val x = imageOffsetX + (location.position.x / 1000f) * imageWidth
                val y = imageOffsetY + (location.position.y / 1000f) * imageHeight

                val isSelected = location.id == selectedLocationId
                val isConnectionFrom = connectionCreationMode && connectionFromLocation == location.id
                val hasPlayableLevels =
                    location.levelIds.any { levelId ->
                        EditorStorage.isLevelReadyToPlay(levelId)
                    }

                val markerColor =
                    when {
                        isConnectionFrom -> Color(0xFFFF9800) // Orange for connection source
                        isSelected -> Color(0xFF2196F3) // Blue for selected
                        hasPlayableLevels -> if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF2E7D32) // Green for playable
                        else -> if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFF757575) // Grey for not playable
                    }

                val radius =
                    when {
                        isConnectionFrom -> markerRadius * 1.5f
                        isSelected -> markerRadius * 1.3f
                        else -> markerRadius
                    }

                // Draw marker circle
                drawCircle(
                    color = markerColor,
                    radius = radius,
                    center = Offset(x, y),
                )

                // Draw border
                drawCircle(
                    color = if (isDarkMode) Color.White else Color.Black,
                    radius = radius,
                    center = Offset(x, y),
                    style =
                        androidx.compose.ui.graphics.drawscope
                            .Stroke(width = 2f),
                )
            }

            // Draw hover indicator for selected location
            if (selectedLocationId != null && hoveredPosition != null) {
                val x = imageOffsetX + (hoveredPosition.x / 1000f) * imageWidth
                val y = imageOffsetY + (hoveredPosition.y / 1000f) * imageHeight

                drawCircle(
                    color = Color(0x802196F3), // Semi-transparent blue
                    radius = markerRadius * 1.5f,
                    center = Offset(x, y),
                )
            }

            // Draw waypoint handles for all connections (only in waypoint edit mode)
            if (waypointEditMode) {
                val waypointRadius = 8f
                for (path in worldMapData.paths) {
                    for ((index, waypoint) in path.controlPoints.withIndex()) {
                        val wpX = imageOffsetX + (waypoint.x / 1000f) * imageWidth
                        val wpY = imageOffsetY + (waypoint.y / 1000f) * imageHeight

                        // Determine if this waypoint is being dragged
                        // Compare by path IDs instead of object reference
                        val isDragging =
                            selectedPathIds != null &&
                                selectedPathIds.first == path.fromLocationId &&
                                selectedPathIds.second == path.toLocationId &&
                                draggingWaypointIndex == index

                        // Draw waypoint handle
                        drawCircle(
                            color = if (isDragging) Color(0xFFFF9800) else Color(0xFF2196F3), // Orange when dragging, blue otherwise
                            radius = if (isDragging) waypointRadius * 1.5f else waypointRadius,
                            center = Offset(wpX, wpY),
                        )

                        // Draw white border
                        drawCircle(
                            color = Color.White,
                            radius = if (isDragging) waypointRadius * 1.5f else waypointRadius,
                            center = Offset(wpX, wpY),
                            style =
                                androidx.compose.ui.graphics.drawscope
                                    .Stroke(width = 2f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculate the perpendicular distance from a point to a line segment.
 */
private fun distanceToLineSegment(
    px: Float,
    py: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
): Float {
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
