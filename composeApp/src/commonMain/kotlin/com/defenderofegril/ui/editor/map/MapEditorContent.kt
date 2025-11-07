package com.defenderofegril.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.ui.editor.CreateMapDialog
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

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
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                // Generate ID from name with underscores
                val sanitizedName = name.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedName.isNotEmpty()) {
                    "map_$sanitizedName"
                } else {
                    "map_custom_${kotlin.random.Random.nextInt(10000, 99999)}"
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
