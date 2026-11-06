package de.egril.defender.ui.editor.level.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.RepositoryImageCatalog
import de.egril.defender.editor.getFileStorage
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventMapImage
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bundled repository level images plus images stored alongside local level files. */
@Composable
internal fun rememberEventMapImageFiles(): List<String> {
    var files by remember { mutableStateOf(RepositoryImageCatalog.fileNames) }
    LaunchedEffect(Unit) {
        files =
            withContext(Dispatchers.Default) {
                val storage = getFileStorage()
                (
                    RepositoryImageCatalog.fileNames +
                        listOf("official", "user", "community").flatMap {
                            storage.listFiles("gamedata/$it/levels")
                        }
                ).filter(EventMapImage::isValidFileName)
                    .distinct()
                    .sorted()
            }
    }
    return files
}

@Composable
internal fun EventMapImageEditor(
    action: EventAction,
    onActionChange: (EventAction) -> Unit,
    context: EventEditorContext,
) {
    if (action.type == EventActionType.HIDE_MAP_IMAGE) {
        IdDropdown(
            label = stringResource(Res.string.event_map_image_id),
            options = context.imageIds.map { it to it },
            selectedId = action.imageId,
            onSelected = { onActionChange(action.copy(imageId = it)) },
        )
        if (action.imageId !in context.imageIds) {
            SelectableText(
                stringResource(Res.string.event_map_image_missing),
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }
    val image = action.mapImage ?: EventMapImage("map_image", "")

    fun update(updated: EventMapImage) = onActionChange(action.copy(mapImage = updated))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = image.id,
            onValueChange = { update(image.copy(id = it)) },
            label = { Text(stringResource(Res.string.event_map_image_id)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        IdDropdown(
            label = stringResource(Res.string.event_map_image_file),
            options = context.imageFiles.map { it to it },
            selectedId = image.fileName,
            onSelected = { update(image.copy(fileName = it)) },
        )
        SelectableText(
            stringResource(Res.string.event_map_image_help),
            style = MaterialTheme.typography.bodySmall,
        )
        ImageNumberField(stringResource(Res.string.x_coordinate), image.x) { update(image.copy(x = it)) }
        ImageNumberField(stringResource(Res.string.y_coordinate), image.y) { update(image.copy(y = it)) }
        ImageNumberField(stringResource(Res.string.event_map_image_width), image.width, positive = true) { update(image.copy(width = it)) }
        ImageNumberField(stringResource(Res.string.event_map_image_height), image.height, positive = true) { update(image.copy(height = it)) }
        if (!image.isValid() || image.fileName !in context.imageFiles) {
            SelectableText(
                stringResource(Res.string.event_map_image_invalid),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ImageNumberField(
    label: String,
    value: Float,
    positive: Boolean = false,
    onChange: (Float) -> Unit,
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.replace(',', '.').toFloatOrNull() != value) text = value.toString()
    }
    val parsed = text.replace(',', '.').toFloatOrNull()
    val valid = parsed != null && parsed.isFinite() && (!positive || parsed > 0f)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            val number = it.replace(',', '.').toFloatOrNull()
            if (number != null && number.isFinite() && (!positive || number > 0f)) onChange(number)
        },
        label = { Text(label) },
        isError = !valid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
