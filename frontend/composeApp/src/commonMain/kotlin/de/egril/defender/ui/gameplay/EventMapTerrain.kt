package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.TileType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.GradientBlendedTileCell
import de.egril.defender.ui.TileImageProvider
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.sqrt

@Composable
internal fun terrainBackgroundColor(
    level: Level,
    position: Position,
): Color =
    when {
        level.isBuildArea(position) -> GamePlayColors.BuildStrip
        level.isOnPath(position) -> GamePlayColors.Path
        level.isRiverTile(position) -> GamePlayColors.River
        else -> GamePlayColors.NonPlayable
    }

/**
 * When event images are visible, terrain that would otherwise be painted inside interactive cells
 * must be drawn below those images. Reuses the cell renderers, including smooth texture transitions.
 */
@Composable
internal fun EventMapTerrain(
    state: GameState,
    hexSize: Float,
    hasMapImage: Boolean,
    visibleZoneIdsByPosition: Map<Position, String>,
    zoneImageIds: Set<String>,
) {
    fun needsTerrain(position: Position): Boolean =
        !hasMapImage || (
            state.paintedTileTypeAt(position) != null &&
                visibleZoneIdsByPosition[position] !in zoneImageIds
        )

    fun tileType(position: Position): TileType =
        state.paintedTileTypeAt(position) ?: when {
            state.level.isSpawnPoint(position) -> TileType.SPAWN_POINT
            state.level.isTargetPosition(position) -> TileType.TARGET
            state.level.isRiverTile(position) -> TileType.RIVER
            state.level.isOnPath(position) -> TileType.PATH
            state.level.isBuildArea(position) -> TileType.BUILD_AREA
            else -> TileType.NO_PLAY
        }

    val tileWidth = hexSize * sqrt(3f)
    val tileHeight = hexSize * 2f
    for (y in 0 until state.level.gridHeight) {
        for (x in 0 until state.level.gridWidth) {
            val position = Position(x, y)
            val isMaelstrom = state.level.getRiverTile(position)?.flowDirection == RiverFlow.MAELSTROM
            if (!needsTerrain(position) && !isMaelstrom) continue
            val type = tileType(position)
            val painter = TileImageProvider.getTilePainter(type, isMaelstrom)
            val color = if (needsTerrain(position)) terrainBackgroundColor(state.level, position) else Color.Transparent
            Box(
                Modifier.offset(
                    x =
                        (
                            x * (tileWidth + HexagonalGridConstants.HORIZONTAL_SPACING) +
                                if (y % 2 == 1) tileWidth * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO else 0f
                        ).dp,
                    y = (y * (tileHeight * 0.75f + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT - 1f) + 1f).dp,
                ),
            ) {
                if (painter != null && AppSettings.useTileSmoothTransitions.value) {
                    val neighbors =
                        position.getHexNeighbors().associateWith { neighbor ->
                            val neighborType = tileType(neighbor)
                            TileImageProvider.getTilePainter(neighborType)
                        }
                    GradientBlendedTileCell(
                        hexSize = hexSize.dp,
                        position = position,
                        tileType = type,
                        backgroundColor = color,
                        borderColor = Color.Transparent,
                        borderWidth = 0.dp,
                        backgroundPainter = painter,
                        onClick = {},
                        getNeighborTileType = { tileType(it) },
                        getNeighborTilePainter = { neighbor, _ -> neighbors[neighbor] },
                    )
                } else {
                    BaseGridCell(
                        hexSize = hexSize.dp,
                        backgroundColor = color,
                        borderColor = Color.Transparent,
                        borderWidth = 0.dp,
                        backgroundPainter = painter,
                        onClick = {},
                    )
                }
            }
        }
    }
}
