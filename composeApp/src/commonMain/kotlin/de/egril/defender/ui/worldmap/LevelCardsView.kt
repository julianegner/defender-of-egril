package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import defender_of_egril.composeapp.generated.resources.*

/**
 * Grid view of level cards - alternative to the image-based world map.
 * Displays all levels as clickable cards in a responsive grid layout.
 * 
 * @param worldLevels List of all levels to display
 * @param onLevelSelected Callback when a level is selected
 * @param showUserLevelsTab If true, shows tabs to filter between Official and User Levels
 * @param filterToUserLevelsOnly If true, only shows user levels (ignores showUserLevelsTab)
 * @param modifier Modifier for the layout
 */
@Composable
fun LevelCardsView(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    showUserLevelsTab: Boolean = false,
    filterToUserLevelsOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Filter levels based on tab selection or direct filter
    val filteredLevels = remember(worldLevels, selectedTabIndex, showUserLevelsTab, filterToUserLevelsOnly) {
        if (filterToUserLevelsOnly) {
            // Direct filter: only user levels
            worldLevels.filter { worldLevel ->
                val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                editorLevel?.isOfficial == false
            }
        } else if (showUserLevelsTab) {
            // Tab-based filter
            if (selectedTabIndex == 0) {
                // Official tab
                worldLevels.filter { worldLevel ->
                    val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                    editorLevel?.isOfficial == true
                }
            } else {
                // User Levels tab
                worldLevels.filter { worldLevel ->
                    val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                    editorLevel?.isOfficial == false
                }
            }
        } else {
            // No filtering
            worldLevels
        }
    }
    
    Column(modifier = modifier) {
        // Show tabs if requested
        if (showUserLevelsTab) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(Res.string.official)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(Res.string.user_levels)) }
                )
            }
        }
        
        // Level cards grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 350.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredLevels) { worldLevel ->
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
}
