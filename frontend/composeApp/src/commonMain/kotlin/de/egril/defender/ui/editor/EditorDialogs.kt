package de.egril.defender.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.iam.IamState

import de.egril.defender.utils.getCurrentUsername
import defender_of_egril.composeapp.generated.resources.*

/**
 * Returns the default author name to pre-fill in create dialogs.
 * If the user is authenticated, uses their first and last name from the IAM token.
 * Falls back to the OS username when not authenticated.
 */
fun getDefaultAuthorName(iamState: IamState): String {
    return if (iamState.isAuthenticated) {
        listOfNotNull(iamState.firstName, iamState.lastName).joinToString(" ")
    } else {
        getCurrentUsername()
    }
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
    onSave: (String) -> Unit
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newValue.isNotBlank()) onSave(newValue) },
                enabled = newValue.isNotBlank()
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Dialog for creating a new map
 */
@Composable
fun CreateMapDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, Int, String) -> Unit,
    defaultAuthor: String = ""
) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("30") }
    var height by remember { mutableStateOf("8") }
    var author by remember { mutableStateOf(defaultAuthor) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_new_map_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.map_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = width,
                    onValueChange = { if (it.all { c -> c.isDigit() }) width = it },
                    label = { Text(stringResource(Res.string.width)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                    label = { Text(stringResource(Res.string.height)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(Res.string.author_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toIntOrNull() ?: 30
                    val h = height.toIntOrNull() ?: 8
                    onCreate(name, w, h, author)
                }
            ) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Dialog for creating a new level
 */
@Composable
fun CreateLevelDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    defaultAuthor: String = ""
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf(defaultAuthor) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_new_level_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.level_title)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(Res.string.author_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(title, author) }) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Generic confirmation dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
        }
    )
}
