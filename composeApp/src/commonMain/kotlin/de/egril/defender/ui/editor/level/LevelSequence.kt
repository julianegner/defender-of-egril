package de.egril.defender.ui.editor.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.PrerequisiteValidationResult
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.LevelSequenceScrollbar
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*

/**
 * Data class to store level card position for drawing arrows
 */
data class LevelCardPosition(
    val levelId: String,
    val center: Offset,
    val size: IntSize
)

/**
 * Keyboard scroll amount in pixels for arrow key navigation
 */
private const val KEYBOARD_SCROLL_AMOUNT = 50

/**
 * Padding for scrollbar spacing to prevent overlap
 */
private const val SCROLLBAR_PADDING = 12

/**
 * Extra padding around tree map content to ensure all items are reachable
 * Increased from 200 to 400 to ensure full scroll range accessibility
 */
private const val TREE_MAP_CONTENT_PADDING = 400

/**
 * Main content for the Level Dependencies tab (formerly Level Sequence)
 * Shows a tree map visualization of levels with prerequisite connections
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LevelSequenceContent() {
    var allLevels by remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var validationResult by remember { mutableStateOf<PrerequisiteValidationResult?>(null) }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }
    var showPrerequisiteEditor by remember { mutableStateOf(false) }
    
    // Track level card positions for drawing arrows
    val levelPositions = remember { mutableStateMapOf<String, LevelCardPosition>() }
    
    // Focus requester for keyboard navigation
    val focusRequester = remember { FocusRequester() }
    
    // Reload data and validate
    LaunchedEffect(Unit) {
        allLevels = EditorStorage.getAllLevels()
        validationResult = EditorStorage.validateAllPrerequisites()
        // Request focus after content loads
        focusRequester.requestFocus()
    }
    
    // Recalculate validation when levels change
    LaunchedEffect(allLevels) {
        validationResult = EditorStorage.validateAllPrerequisites()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Title and validation status
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.level_dependencies),
                style = MaterialTheme.typography.titleMedium
            )
            
            // Validation status
            validationResult?.let { result ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (result.isValid) {
                        CheckmarkIcon(size = 20.dp, tint = Color.Green)
                        Text(
                            text = stringResource(Res.string.validation_passed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    } else {
                        WarningIcon(size = 20.dp)
                        Text(
                            text = stringResource(Res.string.validation_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
        }
        
        // Validation errors (if any)
        validationResult?.let { result ->
            if (!result.isValid) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (result.missingLevelIds.isNotEmpty()) {
                            Text(
                                text = "${stringResource(Res.string.missing_prerequisites)}: ${result.missingLevelIds.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (result.circularDependencies.isNotEmpty()) {
                            Text(
                                text = "${stringResource(Res.string.circular_dependencies)}: ${result.circularDependencies.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (result.disconnectedFromFinal) {
                            Text(
                                text = stringResource(Res.string.final_stand_disconnected),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (result.unreachableLevels.isNotEmpty()) {
                            Text(
                                text = "${stringResource(Res.string.unreachable_levels)}: ${result.unreachableLevels.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Tree map visualization with scrolling
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            val horizontalScrollState = rememberScrollState()
            val verticalScrollState = rememberScrollState()
            
            // Track pending scroll operations
            var pendingScrollX by remember { mutableStateOf<Int?>(null) }
            var pendingScrollY by remember { mutableStateOf<Int?>(null) }
            
            // Handle pending horizontal scroll
            LaunchedEffect(pendingScrollX) {
                pendingScrollX?.let { delta ->
                    // Calculate new scroll position without artificial limits
                    val newValue = horizontalScrollState.value + delta
                    horizontalScrollState.scrollTo(newValue)
                    pendingScrollX = null
                }
            }
            
            // Handle pending vertical scroll
            LaunchedEffect(pendingScrollY) {
                pendingScrollY?.let { delta ->
                    // Calculate new scroll position without artificial limits
                    val newValue = verticalScrollState.value + delta
                    verticalScrollState.scrollTo(newValue)
                    pendingScrollY = null
                }
            }
            
            // Keyboard event handler for arrow key scrolling
            val keyboardHandler: (KeyEvent) -> Boolean = { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            // Scroll left by decreasing scroll value
                            pendingScrollX = -KEYBOARD_SCROLL_AMOUNT
                            true
                        }
                        Key.DirectionRight -> {
                            // Scroll right by increasing scroll value
                            pendingScrollX = KEYBOARD_SCROLL_AMOUNT
                            true
                        }
                        Key.DirectionUp -> {
                            // Scroll up by decreasing scroll value
                            pendingScrollY = -KEYBOARD_SCROLL_AMOUNT
                            true
                        }
                        Key.DirectionDown -> {
                            // Scroll down by increasing scroll value
                            pendingScrollY = KEYBOARD_SCROLL_AMOUNT
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            
            // Show scroll hint if content is scrollable
            val showScrollHint = horizontalScrollState.maxValue > 0 || verticalScrollState.maxValue > 0
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        // Request focus on pointer events to regain focus after clicking cards
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.any { it.pressed }) {
                                        focusRequester.requestFocus()
                                    }
                                }
                            }
                        }
                        .onKeyEvent(keyboardHandler)
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                ) {
                    LevelTreeMap(
                        levels = allLevels,
                        levelPositions = levelPositions,
                        selectedLevelId = selectedLevelId,
                        validationResult = validationResult,
                        onLevelClick = { levelId ->
                            selectedLevelId = levelId
                            showPrerequisiteEditor = true
                        }
                    )
                }

                // Platform-specific scrollbars (actual implementation on desktop, no-op elsewhere)
                LevelSequenceScrollbar(
                    horizontalScrollState = horizontalScrollState,
                    verticalScrollState = verticalScrollState
                )
            }
        }
    }
    
    // Prerequisite editor dialog
    if (showPrerequisiteEditor && selectedLevelId != null) {
        val selectedLevel = allLevels.find { it.id == selectedLevelId }
        if (selectedLevel != null) {
            PrerequisiteEditorDialog(
                level = selectedLevel,
                allLevels = allLevels,
                onDismiss = { showPrerequisiteEditor = false },
                onSave = { updatedLevel ->
                    EditorStorage.saveLevel(updatedLevel)
                    allLevels = EditorStorage.getAllLevels()
                    validationResult = EditorStorage.validateAllPrerequisites()
                    showPrerequisiteEditor = false
                }
            )
        }
    }
}

/**
 * Tree map visualization of levels with dependency arrows
 */
@Composable
fun LevelTreeMap(
    levels: List<EditorLevel>,
    levelPositions: MutableMap<String, LevelCardPosition>,
    selectedLevelId: String?,
    validationResult: PrerequisiteValidationResult?,
    onLevelClick: (String) -> Unit
) {
    val density = LocalDensity.current
    
    // Organize levels into columns based on their depth in the dependency graph
    val levelsByDepth = remember(levels) {
        calculateLevelDepths(levels)
    }
    
    val cardWidth = 200.dp
    val cardHeight = 150.dp
    val columnSpacing = 100.dp
    val rowSpacing = 20.dp
    
    Box(
        modifier = Modifier
            .width(with(density) { ((levelsByDepth.size * (cardWidth.toPx() + columnSpacing.toPx())) + TREE_MAP_CONTENT_PADDING).toDp() })
            .height(with(density) { 
                val maxLevelsInColumn = levelsByDepth.values.maxOfOrNull { it.size } ?: 1
                ((maxLevelsInColumn * (cardHeight.toPx() + rowSpacing.toPx())) + TREE_MAP_CONTENT_PADDING).toDp()
            })
    ) {
        // Draw arrows between levels
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (level in levels) {
                val toPosition = levelPositions[level.id] ?: continue
                
                for (prereqId in level.prerequisites) {
                    val fromPosition = levelPositions[prereqId] ?: continue
                    
                    // Draw arrow from prerequisite to this level
                    val startX = fromPosition.center.x + fromPosition.size.width / 2
                    val startY = fromPosition.center.y
                    val endX = toPosition.center.x - toPosition.size.width / 2
                    val endY = toPosition.center.y
                    
                    // Determine arrow color based on validation
                    val arrowColor = when {
                        validationResult?.circularDependencies?.contains(level.id) == true ||
                        validationResult?.circularDependencies?.contains(prereqId) == true -> Color.Red
                        validationResult?.missingLevelIds?.contains(prereqId) == true -> Color.Gray
                        else -> Color(0xFF4CAF50) // Green
                    }
                    
                    // Draw curved arrow
                    val path = Path().apply {
                        moveTo(startX, startY)
                        val controlX = (startX + endX) / 2
                        cubicTo(
                            controlX, startY,
                            controlX, endY,
                            endX, endY
                        )
                    }
                    
                    drawPath(
                        path = path,
                        color = arrowColor,
                        style = Stroke(width = 2f)
                    )
                    
                    // Draw arrowhead
                    val arrowSize = 10f
                    val arrowPath = Path().apply {
                        moveTo(endX, endY)
                        lineTo(endX - arrowSize, endY - arrowSize / 2)
                        lineTo(endX - arrowSize, endY + arrowSize / 2)
                        close()
                    }
                    drawPath(path = arrowPath, color = arrowColor)
                }
            }
        }
        
        // Draw level cards
        for ((depth, levelsAtDepth) in levelsByDepth) {
            levelsAtDepth.forEachIndexed { index, level ->
                val xOffset = with(density) { (depth * (cardWidth.toPx() + columnSpacing.toPx()) + 25).toDp() }
                val yOffset = with(density) { (index * (cardHeight.toPx() + rowSpacing.toPx()) + 25).toDp() }
                
                LevelTreeCard(
                    level = level,
                    isSelected = level.id == selectedLevelId,
                    hasValidationError = validationResult?.circularDependencies?.contains(level.id) == true ||
                                        validationResult?.unreachableLevels?.contains(level.id) == true,
                    onClick = { onLevelClick(level.id) },
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .width(cardWidth)
                        .height(cardHeight)
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInParent()
                            levelPositions[level.id] = LevelCardPosition(
                                levelId = level.id,
                                center = Offset(
                                    position.x + coordinates.size.width / 2,
                                    position.y + coordinates.size.height / 2
                                ),
                                size = coordinates.size
                            )
                        }
                )
            }
        }
    }
}

/**
 * Calculate the depth of each level in the dependency graph
 * Levels with no prerequisites are at depth 0
 * Note: This function is not thread-safe but is only called from the main/UI thread
 * during Compose recomposition, so thread safety is not a concern.
 */
private fun calculateLevelDepths(levels: List<EditorLevel>): Map<Int, List<EditorLevel>> {
    val levelMap = levels.associateBy { it.id }
    val depths = mutableMapOf<String, Int>()
    
    // Calculate depth for each level using DFS with memoization
    fun getDepth(levelId: String, visited: MutableSet<String>): Int {
        if (levelId in visited) return 0 // Circular dependency, treat as depth 0
        if (levelId in depths) return depths[levelId]!!
        
        val level = levelMap[levelId] ?: return 0
        if (level.prerequisites.isEmpty()) {
            depths[levelId] = 0
            return 0
        }
        
        visited.add(levelId)
        val maxPrereqDepth = level.prerequisites
            .filter { it in levelMap }
            .maxOfOrNull { getDepth(it, visited) } ?: -1
        visited.remove(levelId)
        
        val depth = maxPrereqDepth + 1
        depths[levelId] = depth
        return depth
    }
    
    // Calculate depths for all levels
    for (level in levels) {
        getDepth(level.id, mutableSetOf())
    }
    
    // Group levels by depth
    return levels.groupBy { depths[it.id] ?: 0 }
}

/**
 * Card representing a level in the tree map
 */
@Composable
fun LevelTreeCard(
    level: EditorLevel,
    isSelected: Boolean,
    hasValidationError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = EditorStorage.isLevelReadyToPlay(level)
    
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                hasValidationError -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(8.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title with ready indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (isReady) {
                        CheckmarkIcon(size = 14.dp, tint = Color.Green)
                    } else {
                        Text("X", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Subtitle
                if (level.subtitle.isNotBlank()) {
                    Text(
                        text = level.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                // Stats
                Text(
                    text = "${stringResource(Res.string.enemies)}: ${level.enemySpawns.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${level.startCoins} coins | ${level.startHealthPoints} HP",
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Prerequisites info
                if (level.prerequisites.isNotEmpty()) {
                    val reqCount = level.getEffectiveRequiredCount()
                    val prereqText = if (reqCount < level.prerequisites.size) {
                        "${stringResource(Res.string.requires)} $reqCount/${level.prerequisites.size}"
                    } else {
                        "${stringResource(Res.string.requires)} ${level.prerequisites.size}"
                    }
                    Text(
                        text = prereqText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.entry_point),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            // Test Level badge in lower right corner
            if (level.testingOnly) {
                Text(
                    text = stringResource(Res.string.test_level),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * Dialog for editing level prerequisites
 */
@Composable
fun PrerequisiteEditorDialog(
    level: EditorLevel,
    allLevels: List<EditorLevel>,
    onDismiss: () -> Unit,
    onSave: (EditorLevel) -> Unit
) {
    var selectedPrerequisites by remember { mutableStateOf(level.prerequisites) }
    var requiredCount by remember { mutableStateOf(level.requiredPrerequisiteCount?.toString() ?: "") }
    
    // Available levels for prerequisites (exclude self and levels that would create a cycle)
    val availableLevels = remember(level, allLevels) {
        allLevels.filter { it.id != level.id }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "${stringResource(Res.string.edit_prerequisites)}: ${level.title}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.select_prerequisites),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Checkbox list of available levels
                availableLevels.forEach { availableLevel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPrerequisites = if (availableLevel.id in selectedPrerequisites) {
                                    selectedPrerequisites - availableLevel.id
                                } else {
                                    selectedPrerequisites + availableLevel.id
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = availableLevel.id in selectedPrerequisites,
                            onCheckedChange = { checked ->
                                selectedPrerequisites = if (checked) {
                                    selectedPrerequisites + availableLevel.id
                                } else {
                                    selectedPrerequisites - availableLevel.id
                                }
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = availableLevel.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = availableLevel.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (selectedPrerequisites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Required count input
                    Text(
                        text = stringResource(Res.string.required_count_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = requiredCount,
                        onValueChange = { newValue ->
                            // Only allow digits
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                requiredCount = newValue
                            }
                        },
                        label = { Text(stringResource(Res.string.required_count)) },
                        placeholder = { Text("${selectedPrerequisites.size} (${stringResource(Res.string.all)})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedLevel = level.copy(
                        prerequisites = selectedPrerequisites,
                        requiredPrerequisiteCount = requiredCount.toIntOrNull()
                    )
                    onSave(updatedLevel)
                }
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
