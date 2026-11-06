package de.egril.defender.ui.editor.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.ui.MapImageProvider
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.LeftArrowIcon
import de.egril.defender.ui.icon.RightArrowIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.map_image_generate_step
import defender_of_egril.composeapp.generated.resources.map_image_next
import defender_of_egril.composeapp.generated.resources.map_image_optimize_step
import defender_of_egril.composeapp.generated.resources.map_image_previous
import defender_of_egril.composeapp.generated.resources.map_image_step_skipped
import defender_of_egril.composeapp.generated.resources.map_preview
import defender_of_egril.composeapp.generated.resources.movable_zone_image_hint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class ImageStep { WAITING, RUNNING, DONE, SKIPPED }

internal data class ImageGenerationProgress(
    val fileName: String,
    val generation: ImageStep = ImageStep.WAITING,
    val optimization: ImageStep = ImageStep.WAITING,
)

internal fun mapImageFileNames(map: EditorMap): List<String> =
    listOf("${map.id}.png") + map.tileZones.indices.map { index -> MapImageProvider.tileZoneImageFileName(map.id, index) }

internal fun initialImageProgress(result: EditorStorage.MapSaveResult): List<ImageGenerationProgress> =
    mapImageFileNames(result.validatedMap).map { ImageGenerationProgress(it) }

/** A generated background image shown in the preview carousel, identified by its file name. */
internal class MapImagePreview(
    val fileName: String,
    val painter: Painter,
)

internal suspend fun loadMapImagePreviews(fileNames: List<String>): List<MapImagePreview> =
    withContext(Dispatchers.Default) {
        fileNames.mapNotNull { fileName ->
            MapImageProvider.loadMapImageFileBytes(fileName)?.let { bytes ->
                MapImageProvider.decodeImageBitmap(bytes)?.let { MapImagePreview(fileName, BitmapPainter(it)) }
            }
        }
    }

/** Shows one image at a time with previous/next arrows and the file name below it. */
@Composable
internal fun MapImageCarousel(
    previews: List<MapImagePreview>,
    index: Int,
    onIndexChange: (Int) -> Unit,
) {
    val current = previews[index]
    val previousLabel = stringResource(Res.string.map_image_previous)
    val nextLabel = stringResource(Res.string.map_image_next)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { onIndexChange((index - 1 + previews.size) % previews.size) },
                enabled = previews.size > 1,
                modifier = Modifier.semantics { contentDescription = previousLabel },
            ) {
                LeftArrowIcon(size = 20.dp)
            }
            Image(
                painter = current.painter,
                contentDescription = stringResource(Res.string.map_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(1f).heightIn(min = 180.dp, max = 420.dp),
            )
            IconButton(
                onClick = { onIndexChange((index + 1) % previews.size) },
                enabled = previews.size > 1,
                modifier = Modifier.semantics { contentDescription = nextLabel },
            ) {
                RightArrowIcon(size = 20.dp)
            }
        }
        Text(
            text = "${current.fileName} (${index + 1}/${previews.size})",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun MovableZoneImageHint() {
    Text(
        text = stringResource(Res.string.movable_zone_image_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Runs and reports both stages for each image independently. Files whose source has not changed
 * keep their previous PNG; the save result determines precisely which zone indices need work.
 */
internal suspend fun processMapImages(
    result: EditorStorage.MapSaveResult,
    providedBackground: ByteArray? = null,
    onProgress: (List<ImageGenerationProgress>) -> Unit,
) {
    val map = result.validatedMap
    var progress = initialImageProgress(result)
    onProgress(progress)

    fun update(
        index: Int,
        generation: ImageStep? = null,
        optimization: ImageStep? = null,
    ) {
        progress =
            progress.toMutableList().apply {
                val current = get(index)
                set(index, current.copy(generation = generation ?: current.generation, optimization = optimization ?: current.optimization))
            }
        onProgress(progress)
    }

    if (providedBackground != null) {
        update(0, ImageStep.SKIPPED, ImageStep.RUNNING)
        withContext(Dispatchers.Default) { EditorStorage.saveProvidedMapImage(map, providedBackground) }
        update(0, optimization = ImageStep.DONE)
    } else if (result.imageNeedsRegeneration) {
        update(0, generation = ImageStep.RUNNING)
        val (pixels, width, height) = withContext(Dispatchers.Default) { EditorStorage.generateMapPixels(map) }
        update(0, generation = ImageStep.DONE, optimization = ImageStep.RUNNING)
        val size = withContext(Dispatchers.Default) { EditorStorage.compressAndSaveMapImage(map, pixels, width, height) }
        check(size >= 0) { "Could not save map image for ${map.id}" }
        update(0, optimization = ImageStep.DONE)
    } else {
        update(0, ImageStep.SKIPPED, ImageStep.SKIPPED)
    }

    for (index in map.tileZones.indices) {
        val progressIndex = index + 1
        if (index !in result.zoneIndicesToRegenerate) {
            update(progressIndex, ImageStep.SKIPPED, ImageStep.SKIPPED)
            continue
        }
        update(progressIndex, generation = ImageStep.RUNNING)
        val (pixels, width, height) = withContext(Dispatchers.Default) { EditorStorage.generateTileZonePixels(map, index) }
        update(progressIndex, generation = ImageStep.DONE, optimization = ImageStep.RUNNING)
        withContext(Dispatchers.Default) { EditorStorage.compressAndSaveTileZoneImage(map, index, pixels, width, height) }
        update(progressIndex, optimization = ImageStep.DONE)
    }
}

@Composable
internal fun MapImageProgressRows(progress: List<ImageGenerationProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        progress.forEach { image ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(image.fileName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                ImageProgressStep(stringResource(Res.string.map_image_generate_step), image.generation)
                ImageProgressStep(stringResource(Res.string.map_image_optimize_step), image.optimization)
            }
        }
    }
}

@Composable
private fun ImageProgressStep(
    label: String,
    step: ImageStep,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        when (step) {
            ImageStep.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            ImageStep.DONE -> CheckmarkIcon(size = 14.dp, tint = MaterialTheme.colorScheme.primary)
            ImageStep.SKIPPED -> Text("-", style = MaterialTheme.typography.bodySmall)
            ImageStep.WAITING -> Text(" ", style = MaterialTheme.typography.bodySmall)
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
        if (step == ImageStep.SKIPPED) {
            Text(stringResource(Res.string.map_image_step_skipped), style = MaterialTheme.typography.bodySmall)
        }
    }
}
