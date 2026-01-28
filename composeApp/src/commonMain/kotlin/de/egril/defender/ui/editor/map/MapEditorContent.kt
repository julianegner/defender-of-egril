package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.ui.editor.CreateMapDialog
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import kotlin.random.Random

/**
 * Main content for the Map Editor tab
 */
@Composable
fun MapEditorContent() {
    val maps = remember { mutableStateOf(EditorStorage.getAllMaps()) }
    var selectedMapId by remember { mutableStateOf<String?>(null) }
    var editingMap by remember { mutableStateOf<EditorMap?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    if (editingMap != null) {
        // Map editing view
        MapEditorView(
            map = editingMap!!,
            onSave = { updatedMap ->
                EditorStorage.saveMap(updatedMap)
                maps.value = EditorStorage.getAllMaps()
                editingMap = null
            },
            onCancel = { editingMap = null }
        )
    } else {
        // Map list view
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.maps),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(onClick = { showCreateDialog = true }) {
                    Text(stringResource(Res.string.create_new_map))
                }
            }
            
            Text(
                text = stringResource(Res.string.select_map_to_edit),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 400.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(maps.value) { map ->
                    MapListCard(
                        map = map,
                        isSelected = selectedMapId == map.id,
                        onSelect = {
                            selectedMapId = map.id
                            editingMap = map
                        },
                        onDelete = {
                            EditorStorage.deleteMap(map.id)
                            maps.value = EditorStorage.getAllMaps()
                        },
                        onCopy = {
                            // Create a copy with a new ID
                            val sanitizedName = map.name.trim().lowercase()
                                .replace(" ", "_")
                                .replace(Regex("[^a-z0-9_]"), "")
                                .replace(Regex("_+"), "_")
                            val copyId = if (sanitizedName.isNotEmpty()) {
                                "map_${sanitizedName}_copy_${Random.nextInt(1000, 9999)}"
                            } else {
                                "map_${map.id}_copy_${Random.nextInt(1000, 9999)}"
                            }
                            val copiedMap = map.copy(
                                id = copyId,
                                name = "${map.name} (Copy)",
                                isOfficial = false  // Copied maps are never official
                            )
                            EditorStorage.saveMap(copiedMap)
                            maps.value = EditorStorage.getAllMaps()
                            selectedMapId = copyId
                            editingMap = copiedMap
                        }
                    )
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateMapDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, width, height ->
                // Generate ID from name with underscores (lowercase)
                val sanitizedName = name.trim().lowercase()
                    .replace(" ", "_")
                    .replace(Regex("[^a-z0-9_]"), "")
                    .replace(Regex("_+"), "_")  // Collapse consecutive underscores
                val newId = if (sanitizedName.isNotEmpty()) {
                    "map_$sanitizedName"
                } else {
                    "map_custom_${Random.nextInt(10000, 99999)}"
                }
                val newMap = EditorMap(
                    id = newId,
                    name = name,
                    width = width,
                    height = height,
                    tiles = emptyMap()
                )
                EditorStorage.saveMap(newMap)
                maps.value = EditorStorage.getAllMaps()
                showCreateDialog = false
                editingMap = newMap
            }
        )
    }
}
