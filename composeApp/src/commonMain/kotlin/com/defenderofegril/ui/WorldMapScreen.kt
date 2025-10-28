package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.model.getEnemyTypeCounts

@Composable
fun WorldMapScreen(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null  // Callback for processing cheat codes, returns true if code was valid
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    // State for pan and zoom
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title text - clickable for cheat code access (less obvious than a button)
        Text(
            text = "World Map - Meadows of Egril",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .then(
                    if (onCheatCode != null) {
                        Modifier.clickable { showCheatDialog = true }
                    } else {
                        Modifier
                    }
                )
        )
        
        // Zoomable and scrollable map container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { containerSize = it }
        ) {
            // Map content with pan and zoom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Apply zoom
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            
                            // Apply pan
                            offsetX += pan.x
                            offsetY += pan.y
                            
                            // Constrain pan to keep content visible
                            val maxOffsetX = (containerSize.width * (scale - 1) / 2).coerceAtLeast(0f)
                            val maxOffsetY = (containerSize.height * (scale - 1) / 2).coerceAtLeast(0f)
                            
                            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(worldLevels) { worldLevel ->
                        LevelCard(
                            worldLevel = worldLevel,
                            onClick = {
                                if (worldLevel.status != LevelStatus.LOCKED) {
                                    onLevelSelected(worldLevel.level.id)
                                }
                            }
                        )
                    }
                }
            }
            
            // Minimap - shown when zoomed in
            if (scale > 1.1f) {
                Minimap(
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    containerSize = containerSize,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onBackToMenu) {
            Text("Back to Menu")
        }
    }
    
    // Cheat code dialog
    if (showCheatDialog && onCheatCode != null) {
        AlertDialog(
            onDismissRequest = {
                showCheatDialog = false
                cheatCodeInput = ""
                errorMessage = ""
            },
            title = { Text("Cheat Code") },
            text = {
                Column {
                    Text("Enter cheat code:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = cheatCodeInput,
                        onValueChange = { 
                            cheatCodeInput = it
                            errorMessage = ""  // Clear error when user types
                        },
                        placeholder = { Text("unlock") },
                        isError = errorMessage.isNotEmpty()
                    )
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onCheatCode(cheatCodeInput)
                        if (success) {
                            showCheatDialog = false
                            cheatCodeInput = ""
                            errorMessage = ""
                        } else {
                            errorMessage = "Invalid cheat code"
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showCheatDialog = false
                        cheatCodeInput = ""
                        errorMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun Minimap(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    containerSize: IntSize,
    modifier: Modifier = Modifier
) {
    val minimapSize = 120.dp
    
    Box(
        modifier = modifier
            .size(minimapSize)
            .background(Color(0xCC000000))  // Semi-transparent black background
            .border(2.dp, Color.White)
            .padding(4.dp)
    ) {
        // Map outline (represents the full map)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF444444))
        )
        
        // Viewport indicator (shows current view)
        if (containerSize.width > 0 && containerSize.height > 0) {
            val viewportWidthRatio = 1f / scale
            val viewportHeightRatio = 1f / scale
            
            // Calculate normalized offset (-1 to 1 range)
            val maxOffsetX = (containerSize.width * (scale - 1) / 2).coerceAtLeast(0.01f)
            val maxOffsetY = (containerSize.height * (scale - 1) / 2).coerceAtLeast(0.01f)
            val normalizedOffsetX = -offsetX / maxOffsetX
            val normalizedOffsetY = -offsetY / maxOffsetY
            
            // Calculate viewport position in minimap
            val viewportX = (normalizedOffsetX * (1f - viewportWidthRatio) / 2f + (1f - viewportWidthRatio) / 2f)
            val viewportY = (normalizedOffsetY * (1f - viewportHeightRatio) / 2f + (1f - viewportHeightRatio) / 2f)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(viewportWidthRatio)
                        .fillMaxHeight(viewportHeightRatio)
                        .align(Alignment.TopStart)
                        .offset(
                            x = minimapSize * viewportX,
                            y = minimapSize * viewportY
                        )
                        .background(Color(0x88FFFFFF))  // Semi-transparent white for viewport
                        .border(1.dp, Color.White)
                )
            }
        }
    }
}

@Composable
fun LevelCard(
    worldLevel: WorldLevel,
    onClick: () -> Unit
) {
    val backgroundColor = when (worldLevel.status) {
        LevelStatus.LOCKED -> Color(0xFF9E9E9E)
        LevelStatus.UNLOCKED -> Color(0xFF2196F3)
        LevelStatus.WON -> Color(0xFF4CAF50)
    }
    
    val statusText = when (worldLevel.status) {
        LevelStatus.LOCKED -> "🔒 Locked"
        LevelStatus.UNLOCKED -> "⚔️ Available"
        LevelStatus.WON -> "✓ Completed"
    }
    
    // Get enemy counts for this level
    val enemyCounts = worldLevel.level.getEnemyTypeCounts()
    val enemyList = enemyCounts.entries.toList()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(enabled = worldLevel.status != LevelStatus.LOCKED, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header row with level number and name
            Column {
                Text(
                    text = "Level ${worldLevel.level.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontSize = 18.sp
                )
                
                Text(
                    text = worldLevel.level.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp
                )
            }
            
            // Enemy units display in two columns
            if (enemyList.isNotEmpty()) {
                val halfSize = (enemyList.size + 1) / 2
                val leftColumn = enemyList.take(halfSize)
                val rightColumn = enemyList.drop(halfSize)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        leftColumn.forEach { (attackerType, count) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Enemy icon - larger size
                                Box(
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    EnemyTypeIcon(attackerType = attackerType)
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                // Enemy name and count - larger text
                                Text(
                                    text = "${attackerType.displayName}: ${count}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    
                    // Right column (if there are items for it)
                    if (rightColumn.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rightColumn.forEach { (attackerType, count) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    // Enemy icon - larger size
                                    Box(
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        EnemyTypeIcon(attackerType = attackerType)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(6.dp))
                                    
                                    // Enemy name and count - larger text
                                    Text(
                                        text = "${attackerType.displayName}: ${count}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Status at the bottom
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp
            )
        }
    }
}
