package de.egril.defender.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*

/**
 * Difficulty level chooser dropdown component
 * Displays the current difficulty and allows switching between difficulty levels
 */
@Composable
fun DifficultyChooser(
    modifier: Modifier = Modifier,
    onDifficultyChanged: (DifficultyLevel) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDifficulty = AppSettings.difficulty.value

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectableText(
                text = getDifficultyDisplayName(currentDifficulty),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Dropdown arrow icon
        SelectableText(
            text = if (expanded) "▲" else "▼",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
                        onDifficultyChanged(level)
                        expanded = false
                    }
                )
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
