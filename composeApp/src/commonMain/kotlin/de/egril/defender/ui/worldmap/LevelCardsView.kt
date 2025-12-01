package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel

/**
 * Grid view of level cards - alternative to the image-based world map.
 * Displays all levels as clickable cards in a responsive grid layout.
 */
@Composable
fun LevelCardsView(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 350.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
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
