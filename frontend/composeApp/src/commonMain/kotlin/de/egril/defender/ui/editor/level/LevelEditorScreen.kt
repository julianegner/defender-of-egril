@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import de.egril.defender.ui.icon.LeftArrowIcon
import de.egril.defender.ui.editor.EditorTab
import de.egril.defender.ui.editor.EditorHowToContent
import de.egril.defender.ui.editor.map.MapEditorContent
import de.egril.defender.ui.editor.worldmap.WorldMapPositionEditorContent
import de.egril.defender.ui.settings.SettingsButton
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Main screen for level editing with tabs for Map Editor, Level Editor, Level Sequence, and World Map Positions
 */
@Composable
fun LevelEditorScreen(
    onBack: () -> Unit,
    remoteCommunityMaps: List<de.egril.defender.save.CommunityFileInfo> = emptyList(),
    downloadingMapId: String? = null,
    onDownloadRemoteMap: ((de.egril.defender.save.CommunityFileInfo) -> Unit)? = null
) {
    var currentTab by remember { mutableStateOf(EditorTab.LEVEL_EDITOR) }
    var showHowToDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                EditorTab.MAP_EDITOR -> MapEditorContent(
                    remoteCommunityMaps = remoteCommunityMaps,
                    downloadingMapId = downloadingMapId,
                    onDownloadRemoteMap = onDownloadRemoteMap
                )
                EditorTab.LEVEL_EDITOR -> LevelEditorContent()
                EditorTab.LEVEL_SEQUENCE -> LevelSequenceContent()
                EditorTab.WORLD_MAP_POSITIONS -> WorldMapPositionEditorContent()
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
                        text = stringResource(Res.string.level_editor),
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsButton()
                        
                        Button(
                            onClick = { showHowToDialog = true }
                        ) {
                            Text(stringResource(Res.string.info))
                        }
                        
                        Button(onClick = onBack) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LeftArrowIcon(size = 16.dp, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(Res.string.back_to_world_map))
                            }
                        }
                    }
                }
                
                // Tab navigation
                val tabs = listOf(
                    EditorTab.MAP_EDITOR to stringResource(Res.string.map_editor),
                    EditorTab.LEVEL_EDITOR to stringResource(Res.string.level_editor),
                    EditorTab.LEVEL_SEQUENCE to stringResource(Res.string.level_dependencies),
                    EditorTab.WORLD_MAP_POSITIONS to stringResource(Res.string.world_map_positions)
                )
                
                val selectedTabIndex = tabs.indexOfFirst { it.first == currentTab }.let { index ->
                    if (index == -1) tabs.indexOfFirst { it.first == EditorTab.LEVEL_EDITOR } else index
                }
                
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, (tab, label) ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = { currentTab = tab },
                            text = { Text(label) }
                        )
                    }
                }
            }
        }

        // How-To dialog shown when the Info button is clicked
        if (showHowToDialog) {
            Dialog(onDismissRequest = { showHowToDialog = false }) {
                Card(
                    modifier = Modifier
                        .widthIn(min = 700.dp, max = 900.dp)
                        .heightIn(max = 620.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(Res.string.editor_howto_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            EditorHowToContent()
                        }
                        Button(
                            onClick = { showHowToDialog = false },
                            modifier = Modifier.align(Alignment.End).padding(top = 12.dp)
                        ) {
                            Text(stringResource(Res.string.editor_howto_close))
                        }
                    }
                }
            }
        }
        }
    }
}
