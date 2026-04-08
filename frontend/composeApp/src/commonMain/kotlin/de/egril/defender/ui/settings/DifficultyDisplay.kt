package de.egril.defender.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*

/**
 * Displays the current difficulty level in italic text.
 * Can optionally be made clickable to open the difficulty chooser dropdown.
 */
@Composable
fun DifficultyDisplay(
    modifier: Modifier = Modifier,
    isClickable: Boolean = false
) {
    val currentDifficulty = AppSettings.difficulty.value
    var showDropdown by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        SelectableText(
            text = getDifficultyDisplayName(currentDifficulty),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (isClickable) {
                Modifier.clickable { showDropdown = !showDropdown }
            } else {
                Modifier
            }
        )
        
        // Show dropdown if clickable and opened
        if (isClickable && showDropdown) {
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                DifficultyLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = getDifficultyDisplayName(level),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = getDifficultyDescription(level),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            AppSettings.saveDifficulty(level)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Get the localized display name for a difficulty level
 */
@Composable
private fun getDifficultyDisplayName(level: DifficultyLevel): String {
    return when (level) {
        DifficultyLevel.BABY -> stringResource(Res.string.difficulty_baby)
        DifficultyLevel.EASY -> stringResource(Res.string.difficulty_easy)
        DifficultyLevel.MEDIUM -> stringResource(Res.string.difficulty_medium)
        DifficultyLevel.HARD -> stringResource(Res.string.difficulty_hard)
        DifficultyLevel.NIGHTMARE -> stringResource(Res.string.difficulty_nightmare)
    }
}

/**
 * Get the localized description for a difficulty level
 */
@Composable
private fun getDifficultyDescription(level: DifficultyLevel): String {
    return when (level) {
        DifficultyLevel.BABY -> stringResource(Res.string.difficulty_baby_desc)
        DifficultyLevel.EASY -> stringResource(Res.string.difficulty_easy_desc)
        DifficultyLevel.MEDIUM -> stringResource(Res.string.difficulty_medium_desc)
        DifficultyLevel.HARD -> stringResource(Res.string.difficulty_hard_desc)
        DifficultyLevel.NIGHTMARE -> stringResource(Res.string.difficulty_nightmare_desc)
    }
}
