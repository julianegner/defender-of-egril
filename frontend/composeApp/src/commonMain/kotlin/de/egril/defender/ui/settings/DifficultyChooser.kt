package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.DropdownItem
import de.egril.defender.ui.common.KeyboardNavigableDropdown
import defender_of_egril.composeapp.generated.resources.*

/**
 * Difficulty level chooser dropdown component
 * Displays the current difficulty and allows switching between difficulty levels.
 * Keyboard navigation: Enter/Space to open, ↑/↓ to navigate, Enter to select, Esc to close.
 */
@Composable
fun DifficultyChooser(
    modifier: Modifier = Modifier,
    onDifficultyChanged: (DifficultyLevel) -> Unit = {},
    triggerOpen: Boolean = false,
    onTriggerOpenHandled: () -> Unit = {},
) {
    val currentDifficulty = AppSettings.difficulty.value

    val items =
        DifficultyLevel.entries.map { level ->
            DropdownItem(
                value = level,
                content = {
                    Column {
                        Text(
                            text = getDifficultyDisplayName(level),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = getDifficultyDescription(level),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }

    KeyboardNavigableDropdown(
        items = items,
        selectedValue = currentDifficulty,
        onItemSelected = { level -> onDifficultyChanged(level) },
        selectedContent = {
            Text(
                text = getDifficultyDisplayName(currentDifficulty),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        modifier = modifier.fillMaxWidth(),
        triggerOpen = triggerOpen,
        onTriggerOpenHandled = onTriggerOpenHandled,
    )
}

/**
 * Get the localized display name for a difficulty level
 */
@Composable
private fun getDifficultyDisplayName(level: DifficultyLevel): String =
    when (level) {
        DifficultyLevel.BABY -> stringResource(Res.string.difficulty_baby)
        DifficultyLevel.EASY -> stringResource(Res.string.difficulty_easy)
        DifficultyLevel.MEDIUM -> stringResource(Res.string.difficulty_medium)
        DifficultyLevel.HARD -> stringResource(Res.string.difficulty_hard)
        DifficultyLevel.NIGHTMARE -> stringResource(Res.string.difficulty_nightmare)
    }

/**
 * Get the localized description for a difficulty level
 */
@Composable
private fun getDifficultyDescription(level: DifficultyLevel): String =
    when (level) {
        DifficultyLevel.BABY -> stringResource(Res.string.difficulty_baby_desc)
        DifficultyLevel.EASY -> stringResource(Res.string.difficulty_easy_desc)
        DifficultyLevel.MEDIUM -> stringResource(Res.string.difficulty_medium_desc)
        DifficultyLevel.HARD -> stringResource(Res.string.difficulty_hard_desc)
        DifficultyLevel.NIGHTMARE -> stringResource(Res.string.difficulty_nightmare_desc)
    }
