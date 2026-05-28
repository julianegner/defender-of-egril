package de.egril.defender.ui.gameplay

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.ReloadIcon
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.coins
import defender_of_egril.composeapp.generated.resources.health
import defender_of_egril.composeapp.generated.resources.spells
import defender_of_egril.composeapp.generated.resources.turn
import defender_of_egril.composeapp.generated.resources.tooltip_enemies_on_map_and_planned

/**
 * Reusable expandable card component with collapse/expand functionality.
 * Displays a header with title and collapse icon, with expandable content below.
 *
 * @param title Card title text
 * @param subtitle Optional subtitle text shown below title (even when collapsed)
 * @param modifier Modifier for the card
 * @param defaultExpanded Whether the card starts expanded (default: false)
 * @param content Composable content shown when expanded
 */
@Composable
fun ExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    defaultExpanded: Boolean = false,
    forceExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    
    // Force expansion when forceExpanded is true
    LaunchedEffect(forceExpanded) {
        if (forceExpanded) {
            isExpanded = true
        }
    }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(GamePlayConstants.Spacing.Sections)) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isExpanded) {
                    TriangleDownIcon(size = GamePlayConstants.IconSizes.Large, tint = if (isDarkMode) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black)
                } else {
                    TriangleLeftIcon(size = GamePlayConstants.IconSizes.Large, tint = if (isDarkMode) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black)
                }
            }
            
            // Optional subtitle (always visible)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Expandable content
            if (isExpanded) {
                content()
            }
        }
    }
}

/**
 * Reusable row with an icon, spacer, and text.
 * Common pattern used throughout the app for stats display.
 *
 * @param icon Composable icon to display
 * @param text Text to display after the icon
 * @param iconSize Size of the icon (default: 12.dp)
 * @param spacerWidth Width of spacer between icon and text (default: 4.dp)
 * @param textStyle Text style to apply (default: bodySmall)
 * @param modifier Modifier for the row
 */
@Composable
fun IconTextRow(
    icon: @Composable (Dp) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = GamePlayConstants.IconSizes.Small,
    spacerWidth: Dp = GamePlayConstants.Spacing.IconText,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        icon(iconSize)
        Spacer(modifier = Modifier.width(spacerWidth))
        Text(text, style = textStyle)
    }
}

/**
 * Display game statistics (coins, health, turn) with icons.
 * Used in both expanded and compact headers.
 *
 * @param coins Current coin count
 * @param health Current health points
 * @param turn Current turn number
 * @param iconSize Size for the icons (default: 20.dp for expanded, 16.dp for compact)
 * @param textStyle Text style to use
 * @param onCoinsClick Optional callback when coins are clicked (for cheat codes)
 * @param modifier Modifier for the column
 */
@Composable
fun GameStatsDisplay(
    coins: Int,
    health: Int,
    turn: Int,
    activeEnemyCount: Int,
    remainingEnemyCount: Int,
    currentMana: Int? = null,  // Optional mana display (null if not using mana)
    maxMana: Int? = null,  // Optional max mana (null if not using mana)
    iconSize: Dp = GamePlayConstants.IconSizes.Large,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onCoinsClick: (() -> Unit)? = null,
    onEnemyCountClick: (() -> Unit)? = null,
    onManaClick: (() -> Unit)? = null  // Optional callback when mana is clicked
) {

    // Coins (clickable if callback provided)
    TooltipWrapper(text = stringResource(Res.string.coins)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (onCoinsClick != null) {
                Modifier.clickable(onClick = onCoinsClick)
            } else {
                Modifier
            }
        ) {
            MoneyIcon(size = iconSize)
            Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
            Text("$coins", style = textStyle)
        }
    }
        
    // Health
    TooltipWrapper(text = stringResource(Res.string.health)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeartIcon(size = iconSize)
            Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
            Text("$health", style = textStyle)
        }
    }
    
    // Mana (only show if mana values are provided)
    if (currentMana != null && maxMana != null && maxMana > 0) {
        TooltipWrapper(text = stringResource(Res.string.spells)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onManaClick != null) {
                    Modifier.clickable(onClick = onManaClick)
                } else {
                    Modifier
                }
            ) {
                de.egril.defender.ui.icon.PentagramIcon(
                    size = iconSize,
                    color = Color(0xFF9C27B0)  // Purple for mana
                )
                Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
                Text("$currentMana/$maxMana", style = textStyle)
                if (AppSettings.showButtonShortcutHints.value && onManaClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ShortcutKeyChip(
                        text = formatShortcutBindingForDisplay(AppSettings.shortcutToggleSpellMenu.value)
                    )
                }
            }
        }
    }
        
    // Turn
    TooltipWrapper(text = stringResource(Res.string.turn)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReloadIcon(size = iconSize - 2.dp) // Slightly smaller reload icon
            Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
            Text("$turn", style = textStyle)
        }
    }

    // Enemy count (clickable if callback provided)
    TooltipWrapper(text = stringResource(Res.string.tooltip_enemies_on_map_and_planned)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (onEnemyCountClick != null) {
                Modifier.clickable(onClick = onEnemyCountClick)
            } else {
                Modifier
            }
        ) {
            EnemyTypeIcon(AttackerType.GOBLIN, modifier = Modifier.size(iconSize + 4.dp))
            Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
            Text("$activeEnemyCount | $remainingEnemyCount", style = textStyle)
            if (AppSettings.showButtonShortcutHints.value && onEnemyCountClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                ShortcutKeyChip(
                    text = formatShortcutBindingForDisplay(AppSettings.shortcutToggleEnemyList.value)
                )
            }
        }
    }
}

/**
 * Displays a shortcut key label styled to look like a physical keyboard key:
 * square border (no corner rounding), small padding, monospace-style text.
 *
 * Used wherever button-shortcut hints are shown (action buttons, dialogs).
 * Arrow unicode characters (\u2190\u2191\u2192\u2193\u25C0) are rendered as
 * Material Symbol icons to ensure compatibility on all platforms including wasm.
 */
@Composable
fun ShortcutKeyChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    if (!AppSettings.showButtonShortcutHints.value) return
    Box(
        modifier = modifier
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        if (containsArrowUnicode(text)) {
            ArrowChipContent(text = text, color = color)
        } else {
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/**
 * Set of arrow unicode characters that need material symbol rendering for wasm compatibility.
 */
private val ARROW_UNICODE_CHARS = setOf('\u2190', '\u2191', '\u2192', '\u2193', '\u25C0')

/**
 * Returns true if the text contains any arrow unicode characters that need
 * to be rendered as material symbol icons for wasm compatibility.
 */
private fun containsArrowUnicode(text: String): Boolean {
    return text.any { it in ARROW_UNICODE_CHARS }
}

/**
 * Renders chip content that may contain arrow unicode characters as Material Symbol icons.
 * Non-arrow characters (like "/" separators) are rendered as text.
 */
@Composable
private fun ArrowChipContent(text: String, color: Color) {
    val iconSize = with(LocalDensity.current) { 10.sp.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (char in text) {
            val arrowIcon = charToArrowIcon(char)
            if (arrowIcon != null) {
                dev.vicart.compose.material.symbols.FilledSymbol(
                    icon = arrowIcon,
                    size = iconSize,
                    tint = color
                )
            } else {
                Text(
                    text = char.toString(),
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Maps a unicode arrow character to its corresponding Material Symbol icon,
 * or null if the character is not an arrow.
 */
private fun charToArrowIcon(char: Char): String? = when (char) {
    '\u2190' -> dev.vicart.compose.material.symbols.MaterialSymbols.ARROW_BACK
    '\u2191' -> dev.vicart.compose.material.symbols.MaterialSymbols.ARROW_UPWARD
    '\u2192' -> dev.vicart.compose.material.symbols.MaterialSymbols.ARROW_FORWARD
    '\u2193' -> dev.vicart.compose.material.symbols.MaterialSymbols.ARROW_DOWNWARD
    '\u25C0' -> dev.vicart.compose.material.symbols.MaterialSymbols.ARROW_BACK
    else -> null
}
