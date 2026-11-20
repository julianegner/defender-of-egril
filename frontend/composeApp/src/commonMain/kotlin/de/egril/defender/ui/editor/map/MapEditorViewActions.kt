package de.egril.defender.ui.editor.map

import androidx.compose.ui.graphics.painter.BitmapPainter
import de.egril.defender.editor.*
import de.egril.defender.ui.MapImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun MapEditorViewState.refreshMapPreviewImage(
    currentMap: EditorMap,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch {
        mapPreviewError = null
        val preferredIds = listOf(currentMap.id, map.id).distinct().filter { it.isNotBlank() }
        val existingPreviewBytes =
            preferredIds.firstNotNullOfOrNull { id ->
                MapImageProvider.loadMapImageBytes(id)
            }
        var nextPreviewBytes = existingPreviewBytes

        if (nextPreviewBytes == null) {
            try {
                val (pixels, width, height) =
                    withContext(Dispatchers.Default) {
                        EditorStorage.generateMapPixels(currentMap)
                    }
                withContext(Dispatchers.Default) {
                    EditorStorage.compressAndSaveMapImage(currentMap, pixels, width, height)
                }
                nextPreviewBytes =
                    preferredIds.firstNotNullOfOrNull { id ->
                        MapImageProvider.loadMapImageBytes(id)
                    }
            } catch (e: Exception) {
                mapPreviewError = e.message ?: "Failed to generate map preview"
                nextPreviewBytes = null
            }
        }

        val decodedPainter =
            nextPreviewBytes?.let { bytes ->
                val bitmap = MapImageProvider.decodeImageBitmap(bytes)
                bitmap?.let { BitmapPainter(it) }
            }

        mapPreviewBytes = nextPreviewBytes
        mapPreviewPainter = decodedPainter
    }
}

internal suspend fun MapEditorViewState.loadMapPreviewFromDisk(currentMap: EditorMap): BitmapPainter? =
    withContext(Dispatchers.Default) {
        val preferredIds = listOf(currentMap.id, map.id).distinct().filter { it.isNotBlank() }
        val bytes =
            preferredIds.firstNotNullOfOrNull { id ->
                MapImageProvider.loadMapImageBytes(id)
            }
        val painter =
            bytes?.let { imageBytes ->
                val bitmap = MapImageProvider.decodeImageBitmap(imageBytes)
                bitmap?.let { BitmapPainter(it) }
            }
        mapPreviewBytes = bytes
        mapPreviewPainter = painter
        mapImageGenerationSuccess = bytes != null
        if (bytes == null) {
            mapImageGenerationError = "Map image not found"
        } else {
            mapImageGenerationError = null
        }
        painter
    }

internal fun MapEditorViewState.regenerateMapPreview(
    currentMap: EditorMap,
    coroutineScope: CoroutineScope,
) {
    if (mapImageGenerationRunning) return
    mapImageGenerationRunning = true
    mapImageGenerationSuccess = null
    mapImageGenerationError = null
    mapImageProgress = emptyList()

    coroutineScope.launch {
        try {
            val saveResult = withContext(Dispatchers.Default) { EditorStorage.saveMapData(currentMap) }
            processMapImages(saveResult) { mapImageProgress = it }
            mapImagePreviews = loadMapImagePreviews(mapImageFileNames(currentMap))
            mapImagePreviewIndex = 0
            val refreshedPainter = loadMapPreviewFromDisk(currentMap)
            mapPreviewPainter = refreshedPainter
            mapImageGenerationSuccess = refreshedPainter != null
            if (refreshedPainter == null) {
                mapImageGenerationError = "Map image could not be loaded after regeneration"
                mapImageGenerationSuccess = false
            }
        } catch (e: Exception) {
            mapImageGenerationSuccess = false
            mapImageGenerationError = e.message ?: "Failed to regenerate map preview"
        } finally {
            mapImageGenerationRunning = false
        }
    }
}
