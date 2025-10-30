package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.EditorStorage

enum class EditorTab {
    MAP_EDITOR,
    LEVEL_EDITOR,
    LEVEL_SEQUENCE
}

@Composable
fun LevelEditorScreen(
    onBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf(EditorTab.LEVEL_EDITOR) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Title
        Text(
            text = "Level Editor",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Tab buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { currentTab = EditorTab.MAP_EDITOR },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == EditorTab.MAP_EDITOR) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Map Editor")
            }
            
            Button(
                onClick = { currentTab = EditorTab.LEVEL_EDITOR },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == EditorTab.LEVEL_EDITOR) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Level Editor")
            }
            
            Button(
                onClick = { currentTab = EditorTab.LEVEL_SEQUENCE },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == EditorTab.LEVEL_SEQUENCE) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Level Sequence")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (currentTab) {
            EditorTab.MAP_EDITOR -> MapEditorContent()
            EditorTab.LEVEL_EDITOR -> LevelEditorContent()
            EditorTab.LEVEL_SEQUENCE -> LevelSequenceContent()
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button
        Button(onClick = onBack) {
            Text("Back to World Map")
        }
    }
}

@Composable
fun MapEditorContent() {
    val maps = remember { EditorStorage.getAllMaps() }
    var selectedMapId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Map Editor",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Select a map to edit:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(maps) { map ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMapId = map.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMapId == map.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = map.name.ifEmpty { "Map ${map.id}" },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Size: ${map.width}x${map.height}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        if (selectedMapId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Map editing functionality will be implemented here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LevelEditorContent() {
    val levels = remember { EditorStorage.getAllLevels() }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Level Editor",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Select a level to edit:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(levels) { level ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLevelId = level.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedLevelId == level.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = level.title,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (level.subtitle.isNotEmpty()) {
                            Text(
                                text = level.subtitle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Map: ${level.mapId} | Coins: ${level.startCoins} | HP: ${level.startHealthPoints}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Enemies: ${level.enemySpawns.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        if (selectedLevelId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Level editing functionality will be implemented here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LevelSequenceContent() {
    val sequence = remember { mutableStateOf(EditorStorage.getLevelSequence()) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Level Sequence",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Arrange level order:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sequence.value.sequence.size) { index ->
                val levelId = sequence.value.sequence[index]
                val level = EditorStorage.getLevel(levelId)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                text = "${index + 1}. ${level?.title ?: levelId}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    EditorStorage.moveLevelUp(levelId)
                                    sequence.value = EditorStorage.getLevelSequence()
                                },
                                enabled = index > 0
                            ) {
                                Text("↑")
                            }
                            
                            Button(
                                onClick = {
                                    EditorStorage.moveLevelDown(levelId)
                                    sequence.value = EditorStorage.getLevelSequence()
                                },
                                enabled = index < sequence.value.sequence.size - 1
                            ) {
                                Text("↓")
                            }
                        }
                    }
                }
            }
        }
    }
}
