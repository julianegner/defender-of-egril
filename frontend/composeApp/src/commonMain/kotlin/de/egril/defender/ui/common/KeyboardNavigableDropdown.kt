package de.egril.defender.ui.common

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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleUpIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.dropdown_keyboard_hint

/**
 * Data class representing an item in a keyboard-navigable dropdown.
 *
 * @param T The type of the value associated with this item.
 * @property value The actual value this item represents.
 * @property content Composable content to display for this item in the dropdown menu.
 */
data class DropdownItem<T>(
    val value: T,
    val content: @Composable () -> Unit
)

/**
 * A reusable dropdown composable with full keyboard navigation support.
 *
 * Features:
 * - Enter/Space to open the dropdown
 * - Arrow Up/Down to navigate between options (highlighted with primary border)
 * - Enter to select the highlighted option
 * - Esc to close without selecting
 * - Navigation hint text shown below when shortcut chips setting is ON
 * - External trigger to programmatically open the dropdown
 *
 * @param items The list of dropdown items to display.
 * @param selectedValue The currently selected value (used to set initial highlight position).
 * @param onItemSelected Callback when an item is selected.
 * @param selectedContent Composable content to display in the closed dropdown (showing current selection).
 * @param modifier Modifier for the dropdown container.
 * @param triggerOpen When set to true, opens the dropdown programmatically.
 * @param onTriggerOpenHandled Callback after the trigger has been handled.
 * @param showHint Whether to show the keyboard navigation hint below the dropdown.
 */
@Composable
fun <T> KeyboardNavigableDropdown(
    items: List<DropdownItem<T>>,
    selectedValue: T,
    onItemSelected: (T) -> Unit,
    selectedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    triggerOpen: Boolean = false,
    onTriggerOpenHandled: () -> Unit = {},
    showHint: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf(-1) }

    // Handle external trigger to open dropdown
    LaunchedEffect(triggerOpen) {
        if (triggerOpen) {
            expanded = true
            onTriggerOpenHandled()
        }
    }

    // Reset highlighted index when dropdown opens
    LaunchedEffect(expanded) {
        if (expanded) {
            highlightedIndex = items.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
        }
    }

    Column {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = modifier
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    RoundedCornerShape(8.dp)
                )
                .clickable { expanded = !expanded }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when {
                            expanded && event.key == Key.DirectionDown -> {
                                highlightedIndex =
                                    (highlightedIndex + 1).coerceAtMost(items.size - 1)
                                true
                            }

                            expanded && event.key == Key.DirectionUp -> {
                                highlightedIndex =
                                    (highlightedIndex - 1).coerceAtLeast(0)
                                true
                            }

                            expanded && event.key == Key.Enter -> {
                                val item = items.getOrNull(highlightedIndex)
                                if (item != null) {
                                    onItemSelected(item.value)
                                }
                                expanded = false
                                true
                            }

                            expanded && event.key == Key.Escape -> {
                                expanded = false
                                true
                            }

                            !expanded && (event.key == Key.Enter || event.key == Key.Spacebar) -> {
                                expanded = true
                                true
                            }

                            else -> false
                        }
                    } else false
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectedContent()
            }

            // Dropdown arrow icon
            if (expanded) {
                TriangleUpIcon(
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                )
            } else {
                TriangleDownIcon(
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { item.content() },
                        onClick = {
                            onItemSelected(item.value)
                            expanded = false
                        },
                        colors = if (index == highlightedIndex) {
                            MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            MenuDefaults.itemColors()
                        },
                        modifier = if (index == highlightedIndex) {
                            Modifier.border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                RoundedCornerShape(4.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }

        // Navigation hint
        if (showHint && AppSettings.showButtonShortcutHints.value) {
            Text(
                text = stringResource(Res.string.dropdown_keyboard_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
