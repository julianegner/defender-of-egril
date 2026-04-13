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
import de.egril.defender.save.CommunityFileInfo
import defender_of_egril.composeapp.generated.resources.*

/**
 * Grid view of level cards - alternative to the image-based world map.
 * Displays all levels as clickable cards in a responsive grid layout.
 * 
 * @param worldLevels List of all levels to display
 * @param onLevelSelected Callback when a level is selected
 * @param showUserLevelsTab If true, shows tabs to filter between Official, Community, and User Levels
 * @param filterToUserLevelsOnly If true, only shows user levels (ignores showUserLevelsTab)
 * @param filterToCommunityOnly If true, only shows community levels (ignores showUserLevelsTab)
 * @param remoteCommunityLevels List of community level metadata available on the server
 * @param downloadingLevelId The fileId of a community level currently being downloaded, or null
 * @param onDownloadRemoteLevel Callback when a remote-only level card is clicked to trigger download
 * @param modifier Modifier for the layout
 */
@Composable
fun LevelCardsView(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    showUserLevelsTab: Boolean = false,
    filterToUserLevelsOnly: Boolean = false,
    filterToCommunityOnly: Boolean = false,
    remoteCommunityLevels: List<CommunityFileInfo> = emptyList(),
    downloadingLevelId: String? = null,
    onDownloadRemoteLevel: ((CommunityFileInfo) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // IDs of locally-downloaded community levels so we can exclude them from the remote-only list
    val localCommunityLevelIds = remember(worldLevels) {
        worldLevels
            .mapNotNull { it.level.editorLevelId }
            .filter { id ->
                de.egril.defender.editor.EditorStorage.getCommunityLevel(id)?.isCommunity == true
            }
            .toSet()
    }

    // Remote-only levels: server has them but they are not yet downloaded locally
    val remoteOnlyLevels = remember(remoteCommunityLevels, localCommunityLevelIds) {
        remoteCommunityLevels.filter { it.fileId !in localCommunityLevelIds }
    }

    // Filter levels based on tab selection or direct filter
    val filteredLevels = remember(worldLevels, selectedTabIndex, showUserLevelsTab, filterToUserLevelsOnly, filterToCommunityOnly) {
        when {
            filterToCommunityOnly -> {
                // Direct filter: only community levels
                worldLevels.filter { worldLevel ->
                    worldLevel.level.isCommunity
                }
            }
            filterToUserLevelsOnly -> {
                // Direct filter: only user levels (not official, not community)
                worldLevels.filter { worldLevel ->
                    val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                        ?: de.egril.defender.editor.EditorStorage.getCommunityLevel(worldLevel.level.editorLevelId ?: "")
                    editorLevel?.isOfficial == false && editorLevel.isCommunity == false
                }
            }
            showUserLevelsTab -> {
                when (selectedTabIndex) {
                    0 -> {
                        // Official tab
                        worldLevels.filter { worldLevel ->
                            val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                                ?: de.egril.defender.editor.EditorStorage.getCommunityLevel(worldLevel.level.editorLevelId ?: "")
                            editorLevel?.isOfficial == true
                        }
                    }
                    1 -> {
                        // Community tab – locally-downloaded community levels
                        worldLevels.filter { worldLevel ->
                            worldLevel.level.isCommunity
                        }
                    }
                    else -> {
                        // User Levels tab
                        worldLevels.filter { worldLevel ->
                            val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                                ?: de.egril.defender.editor.EditorStorage.getCommunityLevel(worldLevel.level.editorLevelId ?: "")
                            editorLevel?.isOfficial == false && editorLevel.isCommunity == false
                        }
                    }
                }
            }
            else -> {
                // No filtering
                worldLevels
            }
        }
    }

    // Which remote-only levels to show (only in community tab)
    val showRemoteOnly = filterToCommunityOnly ||
        (showUserLevelsTab && selectedTabIndex == 1)
    
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
                    text = { Text(stringResource(Res.string.community_levels)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
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
            // Locally-downloaded levels
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
            // Remote-only community levels (shown in community tab only)
            if (showRemoteOnly) {
                items(remoteOnlyLevels) { fileInfo ->
                    RemoteCommunityLevelCard(
                        fileInfo = fileInfo,
                        isDownloading = fileInfo.fileId == downloadingLevelId,
                        onClick = { onDownloadRemoteLevel?.invoke(fileInfo) }
                    )
                }
            }
        }
    }
}
