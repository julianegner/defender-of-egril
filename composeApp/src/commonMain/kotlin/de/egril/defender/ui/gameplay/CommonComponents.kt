package de.egril.defender.ui.gameplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.ReloadIcon
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon

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
        
    // Health
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeartIcon(size = iconSize)
        Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
        Text("$health", style = textStyle)
    }
    
    // Mana (only show if mana values are provided)
    if (currentMana != null && maxMana != null && maxMana > 0) {
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
        }
    }
        
    // Turn
    Row(verticalAlignment = Alignment.CenterVertically) {
        ReloadIcon(size = iconSize - 2.dp) // Slightly smaller reload icon
        Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
        Text("$turn", style = textStyle)
    }

    // Enemy count (clickable if callback provided)
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
    }
}
