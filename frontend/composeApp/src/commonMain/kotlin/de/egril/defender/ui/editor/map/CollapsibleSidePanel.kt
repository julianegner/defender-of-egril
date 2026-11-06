package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.TriangleRightIcon

/**
 * A side panel that can be collapsed to a narrow strip, so overlays and tools
 * do not cover the map corners.
 */
@Composable
fun CollapsibleSidePanel(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) {
        Card(
            modifier = modifier.width(PANEL_WIDTH).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.semantics { contentDescription = title },
                    ) {
                        TriangleRightIcon(size = 16.dp)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content,
                )
            }
        }
    } else {
        Card(
            modifier = modifier.width(COLLAPSED_WIDTH).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.semantics { contentDescription = title },
                    ) {
                    TriangleLeftIcon(size = 16.dp)
                }
            }
        }
    }
}

/**
 * A collapsible section inside the side panel. The expanded state is owned by the caller,
 * so the section can drive the same flags that control the overlays on the map.
 */
@Composable
fun CollapsibleSidePanelSection(
    title: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expanded) {
                TriangleDownIcon(size = 14.dp)
            } else {
                TriangleRightIcon(size = 14.dp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (expanded) {
            content()
        }
    }
}

private val PANEL_WIDTH = 320.dp
private val COLLAPSED_WIDTH = 40.dp
