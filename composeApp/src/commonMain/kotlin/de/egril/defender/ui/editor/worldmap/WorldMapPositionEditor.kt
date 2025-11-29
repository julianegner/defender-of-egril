@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import org.jetbrains.compose.resources.painterResource
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*

/**
 * World Map Position Editor content - allows placing maps on the world map visually
 */
@Composable
fun WorldMapPositionEditorContent() {
    var allMaps by remember { mutableStateOf(EditorStorage.getAllMaps()) }
    var selectedMapId by remember { mutableStateOf<String?>(null) }
    var hoveredPosition by remember { mutableStateOf<Position?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    val isDarkMode = AppSettings.isDarkMode.value
    
    // Reload data
    LaunchedEffect(Unit) {
        allMaps = EditorStorage.getAllMaps()
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
                        maps = allMaps,
                        selectedMapId = selectedMapId,
                        hoveredPosition = hoveredPosition,
                        containerSize = containerSize,
                        isDarkMode = isDarkMode,
                        onPositionClick = { position ->
                            // Set position for selected map
                            if (selectedMapId != null) {
                                val map = allMaps.find { it.id == selectedMapId }
                                if (map != null) {
                                    val updatedMap = map.copy(worldMapPosition = position)
                                    EditorStorage.saveMap(updatedMap)
                                    allMaps = EditorStorage.getAllMaps()
                                }
                            }
                        },
                        onHoverChange = { position ->
                            hoveredPosition = position
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
            
            // Right side: Map list
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.maps),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allMaps) { map ->
                            MapPositionListItem(
                                map = map,
                                isSelected = map.id == selectedMapId,
                                onClick = {
                                    selectedMapId = if (selectedMapId == map.id) null else map.id
                                },
                                onClearPosition = {
                                    val updatedMap = map.copy(worldMapPosition = null)
                                    EditorStorage.saveMap(updatedMap)
                                    allMaps = EditorStorage.getAllMaps()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Canvas showing the world map background with map positions
 */
@Composable
private fun WorldMapCanvas(
    maps: List<EditorMap>,
    selectedMapId: String?,
    hoveredPosition: Position?,
    containerSize: IntSize,
    isDarkMode: Boolean,
    onPositionClick: (Position) -> Unit,
    onHoverChange: (Position?) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(Res.drawable.world_map_background),
            contentDescription = "World Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Canvas overlay for markers and interaction
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val offset = event.changes.firstOrNull()?.position ?: continue
                            
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> {
                                    // Calculate position as percentage (0-100)
                                    val x = ((offset.x / size.width) * 100).toInt().coerceIn(0, 100)
                                    val y = ((offset.y / size.height) * 100).toInt().coerceIn(0, 100)
                                    onHoverChange(Position(x, y))
                                }
                                PointerEventType.Exit -> {
                                    onHoverChange(null)
                                }
                                else -> {}
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Calculate position as percentage (0-100)
                        val x = ((offset.x / size.width) * 100).toInt().coerceIn(0, 100)
                        val y = ((offset.y / size.height) * 100).toInt().coerceIn(0, 100)
                        onPositionClick(Position(x, y))
                    }
                }
        ) {
            val markerRadius = 15f
            
            // Draw all map positions
            for (map in maps) {
                val position = map.worldMapPosition ?: continue
                
                val x = (position.x / 100f) * size.width
                val y = (position.y / 100f) * size.height
                
                val isSelected = map.id == selectedMapId
                val markerColor = when {
                    isSelected -> Color(0xFF2196F3) // Blue for selected
                    else -> if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF2E7D32) // Green
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
            
            // Draw road connections between maps that have prerequisites
            val allLevels = EditorStorage.getAllLevels()
            val mapPositions = maps.filter { it.worldMapPosition != null }
                .associateBy({ it.id }, { it.worldMapPosition!! })
            
            val roadColor = if (isDarkMode) Color(0xFF8B4513) else Color(0xFFA0522D)
            
            for (level in allLevels) {
                val levelMapPos = mapPositions[level.mapId] ?: continue
                
                for (prereqId in level.prerequisites) {
                    val prereqLevel = allLevels.find { it.id == prereqId } ?: continue
                    val prereqMapPos = mapPositions[prereqLevel.mapId] ?: continue
                    
                    if (levelMapPos != prereqMapPos) {
                        val startX = (prereqMapPos.x / 100f) * size.width
                        val startY = (prereqMapPos.y / 100f) * size.height
                        val endX = (levelMapPos.x / 100f) * size.width
                        val endY = (levelMapPos.y / 100f) * size.height
                        
                        drawLine(
                            color = roadColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                        )
                    }
                }
            }
            
            // Draw hover indicator for selected map
            if (selectedMapId != null && hoveredPosition != null) {
                val x = (hoveredPosition.x / 100f) * size.width
                val y = (hoveredPosition.y / 100f) * size.height
                
                drawCircle(
                    color = Color(0x802196F3), // Semi-transparent blue
                    radius = markerRadius * 1.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * List item for a map showing its position status
 */
@Composable
private fun MapPositionListItem(
    map: EditorMap,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClearPosition: () -> Unit
) {
    val hasPosition = map.worldMapPosition != null
    
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
                Text(
                    text = map.name.ifEmpty { map.id },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (hasPosition) {
                        "Position: ${map.worldMapPosition!!.x}, ${map.worldMapPosition!!.y}"
                    } else {
                        stringResource(Res.string.no_position_set)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasPosition) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (hasPosition) Color(0xFF4CAF50) else Color.Gray,
                        shape = CircleShape
                    )
            )
            
            // Clear button (only if has position)
            if (hasPosition) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onClearPosition,
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
}
