package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.*
import de.egril.defender.ui.MapImageProvider
import de.egril.defender.ui.constrainMapOffsets
import de.egril.defender.ui.hexagon.HexagonMinimapFromEditorMap
import de.egril.defender.ui.hexagon.MinimapConfig

/**
 * View for editing a map
 */
@Composable
fun MapEditorView(
    map: EditorMap,
    onSave: (EditorMap, String?, ByteArray?) -> Unit,
    onCancel: () -> Unit,
) {
    val state = remember { MapEditorViewState(map) }
    with(state) {
        val coroutineScope = rememberCoroutineScope()
        val backgroundImagePainter =
            remember(backgroundImageBytes) {
                backgroundImageBytes?.let { bytes ->
                    val bitmap = MapImageProvider.decodeImageBitmap(bytes)
                    if (bitmap != null) BitmapPainter(bitmap) else null
                }
            }

        // Create updated map for minimap that reflects current tiles state
        val currentMap =
            remember(mapWidth, mapHeight, tiles, riverTiles, targetInfoMap, spawnPointInfoMap, mapToolingInfo, allowNoBuildableTiles, allowNoDirectPath, tileZones) {
                map.copy(
                    tileZones = tileZones,
                    width = mapWidth,
                    height = mapHeight,
                    tiles = tiles.toMap(),
                    riverTiles = riverTiles.toMap(),
                    targetInfoMap = targetInfoMap.toMap(),
                    spawnPointInfoMap = spawnPointInfoMap.toMap(),
                    mapToolingInfo = mapToolingInfo,
                    allowNoBuildableTiles = allowNoBuildableTiles,
                    allowNoDirectPath = allowNoDirectPath,
                )
            }
        val pathPreviews = remember(currentMap) { currentMap.previewPaths() }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp),
                    ) {
                        MapEditorViewGrid(currentMap, backgroundImagePainter, pathPreviews)
                        MapControls(
                            mapControlState =
                                MapControlState(
                                    zoomLevel = zoomLevel,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                ),
                            onStateChange = { newState ->
                                val newScale = newState.zoomLevel
                                val (constrainedX, constrainedY) =
                                    constrainMapOffsets(
                                        newState.offsetX,
                                        newState.offsetY,
                                        newScale,
                                        containerSize,
                                        actualContentSize,
                                    )
                                zoomLevel = newScale
                                offsetX = constrainedX
                                offsetY = constrainedY
                            },
                        ) {
                            // Minimap
                            HexagonMinimapFromEditorMap(
                                map = currentMap,
                                modifier = Modifier.size(150.dp),
                                config =
                                    MinimapConfig(
                                        showViewport = true,
                                        minimapSizeDp = 150f,
                                    ),
                                scale = zoomLevel,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                containerSize = containerSize,
                                contentSize = actualContentSize,
                                onViewportDrag = { newOffsetX, newOffsetY ->
                                    offsetX = newOffsetX
                                    offsetY = newOffsetY
                                },
                            )
                        }
                    }

                    MapEditorViewSidePanel(currentMap, backgroundImagePainter, coroutineScope, pathPreviews)
                }

                MapEditorViewFooter(coroutineScope, onSave, onCancel)
            }
        }

        MapEditorViewDialogs(currentMap, coroutineScope, onSave)
    }
}
