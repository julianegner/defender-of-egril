@file:OptIn(ExperimentalMaterial3Api::class)

package de.egril.defender.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.iam.IamState
import de.egril.defender.ui.editor.level.EditorLevelTemplate
import de.egril.defender.utils.getCurrentUsername
import defender_of_egril.composeapp.generated.resources.*

/**
 * Returns the default author name to pre-fill in create dialogs.
 * If the user is authenticated, uses their first and last name from the IAM token.
 * Falls back to the OS username when not authenticated.
 */
fun getDefaultAuthorName(iamState: IamState): String =
    if (iamState.isAuthenticated) {
        listOfNotNull(iamState.firstName, iamState.lastName).joinToString(" ")
    } else {
        getCurrentUsername()
    }

/**
 * Generic "Save As" dialog that can be used for both maps and levels
 */
@Composable
fun SaveAsDialog(
    title: String,
    label: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var newValue by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newValue.isNotBlank()) onSave(newValue) },
                enabled = newValue.isNotBlank(),
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Dialog for creating a new map
 */
@Composable
internal fun CreateMapDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, Int, String, MapTemplateDefinition?) -> Unit,
    mapTemplates: List<MapTemplateDefinition>,
    defaultAuthor: String = "",
) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("30") }
    var height by remember { mutableStateOf("8") }
    var author by remember { mutableStateOf(defaultAuthor) }
    var selectedTemplate by remember { mutableStateOf<MapTemplateDefinition?>(null) }
    var templateExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTemplate?.id) {
        selectedTemplate?.templateMap?.let { templateMap ->
            width = templateMap.width.toString()
            height = templateMap.height.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_new_map_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.map_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = width,
                    onValueChange = { if (it.all { c -> c.isDigit() }) width = it },
                    enabled = selectedTemplate?.templateMap == null,
                    label = { Text(stringResource(Res.string.width)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                    enabled = selectedTemplate?.templateMap == null,
                    label = { Text(stringResource(Res.string.height)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(Res.string.author_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = templateExpanded,
                    onExpandedChange = { templateExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedTemplate?.name ?: stringResource(Res.string.template_blank),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.map_templates)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = templateExpanded,
                        onDismissRequest = { templateExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.template_blank)) },
                            onClick = {
                                selectedTemplate = null
                                templateExpanded = false
                            },
                        )
                        mapTemplates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    selectedTemplate = template
                                    templateExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toIntOrNull() ?: 30
                    val h = height.toIntOrNull() ?: 8
                    onCreate(name, w, h, author, selectedTemplate)
                },
            ) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Dialog for creating a new level
 */
@Composable
internal fun CreateLevelDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, EditorLevelTemplate) -> Unit,
    defaultAuthor: String = "",
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf(defaultAuthor) }
    var selectedTemplate by remember { mutableStateOf(EditorLevelTemplate.TUTORIAL) }
    var templateExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_new_level_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.level_title)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(Res.string.author_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = templateExpanded,
                    onExpandedChange = { templateExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedTemplate.localizedLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.apply_level_template)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = templateExpanded,
                        onDismissRequest = { templateExpanded = false },
                    ) {
                        EditorLevelTemplate.entries.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.localizedLabel()) },
                                onClick = {
                                    selectedTemplate = template
                                    templateExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(title, author, selectedTemplate) }) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun EditorLevelTemplate.localizedLabel(): String =
    when (this) {
        EditorLevelTemplate.TUTORIAL -> stringResource(Res.string.template_tutorial)
        EditorLevelTemplate.STEADY_PRESSURE -> stringResource(Res.string.template_steady_pressure)
        EditorLevelTemplate.VILLAIN_DUEL -> stringResource(Res.string.template_villain_duel)
        EditorLevelTemplate.RIVER_PRESSURE -> stringResource(Res.string.template_river_pressure)
        EditorLevelTemplate.ENDURANCE -> stringResource(Res.string.template_endurance)
    }

/**
 * Generic confirmation dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.yes))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
