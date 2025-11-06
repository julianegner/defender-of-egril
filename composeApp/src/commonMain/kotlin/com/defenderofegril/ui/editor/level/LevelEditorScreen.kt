package com.defenderofegril.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.defenderofegril.ui.icon.LeftArrowIcon
import com.defenderofegril.ui.editor.EditorTab
import com.defenderofegril.ui.editor.map.MapEditorContent

/**
 * Main screen for level editing with tabs for Map Editor, Level Editor, and Level Sequence
 */
@Composable
fun LevelEditorScreen(
    onBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf(EditorTab.LEVEL_EDITOR) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Content area (below header)
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Spacer for header
            Spacer(modifier = Modifier.height(140.dp))
            
            // Content based on selected tab
            when (currentTab) {
                EditorTab.MAP_EDITOR -> MapEditorContent()
                EditorTab.LEVEL_EDITOR -> LevelEditorContent()
                EditorTab.LEVEL_SEQUENCE -> LevelSequenceContent()
            }
        }
        
        // Main header (on top with elevated z-index)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                // Title and Back button row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Level Editor",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Button(onClick = onBack) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LeftArrowIcon(size = 16.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back to World Map")
                        }
                    }
                }
                
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
            }
        }
    }
}
