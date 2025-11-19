package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.EditorStorage
import de.egril.defender.ui.icon.DownArrowIcon
import de.egril.defender.ui.icon.UpArrowIcon
import de.egril.defender.ui.editor.ConfirmationDialog
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*
import kotlin.math.abs

/**
 * Drag state to track which level is being dragged
 */
data class DragState(
    val levelId: String,
    val isFromSequence: Boolean,
    val currentPosition: Offset = Offset.Zero
)

/**
 * Item bounds for drop target detection
 */
data class ItemBounds(
    val index: Int,
    val position: Offset,
    val size: IntSize
)

/**
 * Main content for the Level Sequence tab
 */
@Composable
fun LevelSequenceContent() {
    val sequence = remember { mutableStateOf(EditorStorage.getLevelSequence()) }
    val allLevels = remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var levelToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var isDropTargetAvailableArea by remember { mutableStateOf(false) }
    
    // Track bounds of items and available area
    val itemBounds = remember { mutableStateMapOf<Int, ItemBounds>() }
    var availableAreaBounds by remember { mutableStateOf<Pair<Offset, IntSize>?>(null) }
    var sequenceAreaBounds by remember { mutableStateOf<Pair<Offset, IntSize>?>(null) }
    
    // Get levels in sequence that are ready to play
    val levelsInSequence = sequence.value.sequence.mapNotNull { levelId ->
        val level = EditorStorage.getLevel(levelId)
        if (level != null && EditorStorage.isLevelReadyToPlay(level)) {
            levelId to level
        } else null
    }
    
    // Get all levels not in sequence that are ready to play
    val availableLevels = allLevels.value.filter { level ->
        EditorStorage.isLevelReadyToPlay(level) && !sequence.value.sequence.contains(level.id)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.level_sequence),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = stringResource(Res.string.arrange_level_order),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Section: Levels in Sequence
        Text(
            text = stringResource(Res.string.levels_in_sequence),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (levelsInSequence.isEmpty()) {
            Text(
                text = "No levels in sequence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .onGloballyPositioned { coordinates ->
                        sequenceAreaBounds = coordinates.positionInRoot() to 
                            IntSize(coordinates.size.width, coordinates.size.height)
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levelsInSequence.size) { index ->
                    val (levelId, level) = levelsInSequence[index]
                    
                    // Show drop indicator before this item
                    if (dropTargetIndex == index && !isDropTargetAvailableArea) {
                        DropIndicator()
                    }
                    
                    LevelSequenceItem(
                        index = index,
                        levelId = levelId,
                        levelTitle = level.title,
                        isInSequence = true,
                        isDragging = dragState?.levelId == levelId,
                        canMoveUp = index > 0,
                        canMoveDown = index < levelsInSequence.size - 1,
                        onMoveUp = {
                            EditorStorage.moveLevelUp(levelId)
                            sequence.value = EditorStorage.getLevelSequence()
                        },
                        onMoveDown = {
                            EditorStorage.moveLevelDown(levelId)
                            sequence.value = EditorStorage.getLevelSequence()
                        },
                        onRemove = {
                            levelToRemove = levelId to level.title
                        },
                        onDragStart = { offset ->
                            dragState = DragState(levelId, true, offset)
                            dropTargetIndex = null
                            isDropTargetAvailableArea = false
                            itemBounds.clear()
                        },
                        onDrag = { change, dragAmount ->
                            dragState?.let { state ->
                                val newPosition = state.currentPosition + Offset(dragAmount.x, dragAmount.y)
                                dragState = state.copy(currentPosition = newPosition)
                                
                                // Check if over available area
                                availableAreaBounds?.let { (position, size) ->
                                    val isOverAvailable = newPosition.x >= position.x && 
                                                         newPosition.x <= position.x + size.width &&
                                                         newPosition.y >= position.y && 
                                                         newPosition.y <= position.y + size.height
                                    isDropTargetAvailableArea = isOverAvailable
                                    if (isOverAvailable) {
                                        dropTargetIndex = null
                                    } else {
                                        // Calculate drop position based on Y coordinate
                                        // Find which items the drag position is between
                                        // Exclude the item being dragged from calculations
                                        val sortedBounds = itemBounds.values
                                            .filter { it.index != levelsInSequence.indexOfFirst { pair -> pair.first == state.levelId } }
                                            .sortedBy { it.index }
                                        var targetIndex: Int? = null
                                        
                                        if (sortedBounds.isNotEmpty()) {
                                            // Check if before first item (above its midpoint)
                                            val firstItem = sortedBounds.first()
                                            val firstMidpoint = firstItem.position.y + firstItem.size.height / 2
                                            if (newPosition.y < firstMidpoint) {
                                                targetIndex = 0
                                            } else {
                                                // Check each item to find insertion point
                                                for (i in 0 until sortedBounds.size) {
                                                    val currentItem = sortedBounds[i]
                                                    val currentMidpoint = currentItem.position.y + currentItem.size.height / 2
                                                    
                                                    if (i == sortedBounds.size - 1) {
                                                        // Last item - check if below its midpoint
                                                        if (newPosition.y >= currentMidpoint) {
                                                            targetIndex = sortedBounds.size
                                                        } else {
                                                            // Above the last item's midpoint, insert before it
                                                            targetIndex = i
                                                        }
                                                    } else {
                                                        val nextItem = sortedBounds[i + 1]
                                                        val nextMidpoint = nextItem.position.y + nextItem.size.height / 2
                                                        
                                                        // Check if between current midpoint and next midpoint
                                                        if (newPosition.y >= currentMidpoint && newPosition.y < nextMidpoint) {
                                                            targetIndex = i + 1
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // No other items, insert at position 0
                                            targetIndex = 0
                                        }
                                        
                                        dropTargetIndex = targetIndex
                                    }
                                }
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            dragState?.let { state ->
                                if (isDropTargetAvailableArea && state.isFromSequence) {
                                    // Remove from sequence
                                    EditorStorage.removeLevelFromSequence(state.levelId)
                                } else if (dropTargetIndex != null) {
                                    if (state.isFromSequence) {
                                        // Moving within sequence
                                        val currentSequence = EditorStorage.getLevelSequence().sequence.toMutableList()
                                        val fromIndex = currentSequence.indexOf(state.levelId)
                                        
                                        if (fromIndex >= 0 && fromIndex != dropTargetIndex) {
                                            currentSequence.removeAt(fromIndex)
                                            // No adjustment needed - dropTargetIndex is already correct
                                            // since we excluded the dragged item from calculations
                                            val insertIndex = dropTargetIndex!!.coerceIn(0, currentSequence.size)
                                            currentSequence.add(insertIndex, state.levelId)
                                            EditorStorage.updateLevelSequence(de.egril.defender.editor.LevelSequence(currentSequence))
                                        }
                                    } else {
                                        // Adding from available levels
                                        EditorStorage.addLevelToSequence(state.levelId, dropTargetIndex)
                                    }
                                }
                                sequence.value = EditorStorage.getLevelSequence()
                                allLevels.value = EditorStorage.getAllLevels()
                            }
                            dragState = null
                            dropTargetIndex = null
                            isDropTargetAvailableArea = false
                            itemBounds.clear()
                        },
                        onPositionChanged = { position, size ->
                            // Track item bounds for drop target detection
                            itemBounds[index] = ItemBounds(index, position, size)
                        }
                    )
                    
                    // Show drop indicator after last item
                    if (index == levelsInSequence.size - 1 && dropTargetIndex == levelsInSequence.size && !isDropTargetAvailableArea) {
                        DropIndicator()
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Section: Available Levels
        Text(
            text = stringResource(Res.string.available_levels),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (availableLevels.isEmpty()) {
            // Show empty drop area that can receive items
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        if (isDropTargetAvailableArea) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        availableAreaBounds = coordinates.positionInRoot() to 
                            IntSize(coordinates.size.width, coordinates.size.height)
                    }
            ) {
                Text(
                    text = "Drag levels here to remove from sequence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .background(
                        if (isDropTargetAvailableArea) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        availableAreaBounds = coordinates.positionInRoot() to 
                            IntSize(coordinates.size.width, coordinates.size.height)
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableLevels.size) { index ->
                        val level = availableLevels[index]
                        
                        AvailableLevelCard(
                            level = level,
                            isDragging = dragState?.levelId == level.id,
                            onAddToSequence = {
                                EditorStorage.addLevelToSequence(level.id)
                                sequence.value = EditorStorage.getLevelSequence()
                                allLevels.value = EditorStorage.getAllLevels()
                            },
                            onDragStart = { offset ->
                                dragState = DragState(level.id, false, offset)
                                dropTargetIndex = null  // Don't preset - only set if over sequence
                                isDropTargetAvailableArea = false
                            },
                            onDrag = { change, dragAmount ->
                                dragState?.let { state ->
                                    val newPosition = state.currentPosition + Offset(dragAmount.x, dragAmount.y)
                                    dragState = state.copy(currentPosition = newPosition)
                                    
                                    // Check if over sequence area
                                    var isOverSequence = false
                                    sequenceAreaBounds?.let { (position, size) ->
                                        isOverSequence = newPosition.x >= position.x && 
                                                        newPosition.x <= position.x + size.width &&
                                                        newPosition.y >= position.y && 
                                                        newPosition.y <= position.y + size.height
                                    }
                                    
                                    if (isOverSequence) {
                                        // Calculate drop position based on Y coordinate
                                        val sortedBounds = itemBounds.values.sortedBy { it.index }
                                        var targetIndex: Int? = null
                                        
                                        if (sortedBounds.isNotEmpty()) {
                                            // Check if before first item (above its midpoint)
                                            val firstItem = sortedBounds.first()
                                            val firstMidpoint = firstItem.position.y + firstItem.size.height / 2
                                            if (newPosition.y < firstMidpoint) {
                                                targetIndex = firstItem.index
                                            } else {
                                                // Check each item to find insertion point
                                                for (i in 0 until sortedBounds.size) {
                                                    val currentItem = sortedBounds[i]
                                                    val currentMidpoint = currentItem.position.y + currentItem.size.height / 2
                                                    
                                                    if (i == sortedBounds.size - 1) {
                                                        // Last item - check if below its midpoint
                                                        if (newPosition.y >= currentMidpoint) {
                                                            targetIndex = currentItem.index + 1
                                                        } else {
                                                            // Above the last item's midpoint, insert before it
                                                            targetIndex = currentItem.index
                                                        }
                                                    } else {
                                                        val nextItem = sortedBounds[i + 1]
                                                        val nextMidpoint = nextItem.position.y + nextItem.size.height / 2
                                                        
                                                        // Check if between current midpoint and next midpoint
                                                        if (newPosition.y >= currentMidpoint && newPosition.y < nextMidpoint) {
                                                            targetIndex = currentItem.index + 1
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // No items yet, insert at position 0
                                            targetIndex = 0
                                        }
                                        
                                        dropTargetIndex = targetIndex
                                    } else {
                                        // Not over sequence area - clear drop target
                                        dropTargetIndex = null
                                    }
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                dragState?.let { state ->
                                    if (!state.isFromSequence && dropTargetIndex != null) {
                                        // Add to sequence at specific position
                                        EditorStorage.addLevelToSequence(state.levelId, dropTargetIndex)
                                        sequence.value = EditorStorage.getLevelSequence()
                                        allLevels.value = EditorStorage.getAllLevels()
                                    }
                                }
                                dragState = null
                                dropTargetIndex = null
                                isDropTargetAvailableArea = false
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Confirmation dialog for removing level from sequence
    levelToRemove?.let { (levelId, levelTitle) ->
        ConfirmationDialog(
            title = stringResource(Res.string.remove_level_confirmation_title),
            message = stringResource(Res.string.remove_level_confirmation_message).replace("%s", levelTitle),
            onDismiss = { levelToRemove = null },
            onConfirm = {
                EditorStorage.removeLevelFromSequence(levelId)
                sequence.value = EditorStorage.getLevelSequence()
                allLevels.value = EditorStorage.getAllLevels()
                levelToRemove = null
            }
        )
    }
}

/**
 * Visual indicator for drop target position
 */
@Composable
fun DropIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 2.dp)
    )
}

/**
 * Level item in the sequence list with drag support
 */
@Composable
fun LevelSequenceItem(
    index: Int,
    levelId: String,
    levelTitle: String,
    isInSequence: Boolean,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onPositionChanged: (Offset, IntSize) -> Unit
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInRoot()
                itemSize = coordinates.size
                onPositionChanged(itemPosition, itemSize)
            }
            .pointerInput(levelId) {  // Key by level ID to ensure stable drag handling
                detectDragGestures(
                    onDragStart = { offset ->
                        onDragStart(itemPosition + offset)
                    },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${index + 1}. $levelTitle",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Drag to reorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onMoveUp,
                    enabled = canMoveUp && !isDragging
                ) {
                    UpArrowIcon(size = 16.dp, tint = Color.White)
                }
                
                Button(
                    onClick = onMoveDown,
                    enabled = canMoveDown && !isDragging
                ) {
                    DownArrowIcon(size = 16.dp, tint = Color.White)
                }
                
                Button(
                    onClick = onRemove,
                    enabled = !isDragging,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.remove_from_sequence))
                }
            }
        }
    }
}

/**
 * Available level card in grid layout with drag support
 */
@Composable
fun AvailableLevelCard(
    level: de.egril.defender.editor.EditorLevel,
    isDragging: Boolean,
    onAddToSequence: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var cardPosition by remember { mutableStateOf(Offset.Zero) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardPosition = coordinates.positionInRoot()
            }
            .pointerInput(level.id) {  // Key by level ID to ensure stable drag handling
                detectDragGestures(
                    onDragStart = { offset ->
                        onDragStart(cardPosition + offset)
                    },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = level.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
            if (level.subtitle.isNotBlank()) {
                Text(
                    text = level.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onAddToSequence,
                enabled = !isDragging,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.add_to_sequence))
            }
            
            Text(
                text = "Drag to add",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
