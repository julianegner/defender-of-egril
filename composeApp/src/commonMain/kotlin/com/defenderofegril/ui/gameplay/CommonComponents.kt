package com.defenderofegril.ui.gameplay

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
import com.defenderofegril.ui.*

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
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isExpanded) {
                    TriangleDownIcon(size = 20.dp)
                } else {
                    TriangleLeftIcon(size = 20.dp)
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
    iconSize: Dp = 12.dp,
    spacerWidth: Dp = 4.dp,
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
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onCoinsClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
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
            Spacer(modifier = Modifier.width(4.dp))
            Text("$coins", style = textStyle)
        }
        
        // Health
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeartIcon(size = iconSize)
            Spacer(modifier = Modifier.width(4.dp))
            Text("$health", style = textStyle)
        }
        
        // Turn
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReloadIcon(size = iconSize - 2.dp) // Slightly smaller reload icon
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (textStyle == MaterialTheme.typography.bodyLarge) "Turn $turn" else "$turn",
                style = if (textStyle == MaterialTheme.typography.bodyLarge) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Compact stats display for header - just the numbers in a row.
 *
 * @param coins Current coin count
 * @param health Current health points  
 * @param turn Current turn number
 * @param onCoinsClick Optional callback when coins are clicked (for cheat codes)
 * @param modifier Modifier for the row
 */
@Composable
fun CompactStatsRow(
    coins: Int,
    health: Int,
    turn: Int,
    modifier: Modifier = Modifier,
    onCoinsClick: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Clickable coins
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (onCoinsClick != null) {
                Modifier.clickable(onClick = onCoinsClick)
            } else {
                Modifier
            }
        ) {
            MoneyIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("$coins", style = MaterialTheme.typography.bodyMedium)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeartIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("$health", style = MaterialTheme.typography.bodyMedium)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReloadIcon(size = 14.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("$turn", style = MaterialTheme.typography.bodySmall)
        }
    }
}
