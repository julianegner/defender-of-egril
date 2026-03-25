package de.egril.defender.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Comprehensive how-to guide for the Level Editor.
 * This composable is used both in the editor info popup dialog and as a tab in the InfoPageScreen.
 */
@Composable
fun EditorHowToContent() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Introduction
        HowToSection(
            title = null,
            content = stringResource(Res.string.editor_howto_intro)
        )

        // Workflow
        HowToSection(
            title = stringResource(Res.string.editor_howto_workflow_title),
            content = null
        )
        HowToItemsList(stringResource(Res.string.editor_howto_workflow_steps))

        HorizontalDivider()

        // Map Editor
        HowToSection(
            title = stringResource(Res.string.editor_howto_map_editor_title),
            content = stringResource(Res.string.editor_howto_map_editor_intro)
        )

        HowToSubSection(title = stringResource(Res.string.editor_howto_tile_types_title))
        HowToItemsList(stringResource(Res.string.editor_howto_tile_types))

        HowToSubSection(title = stringResource(Res.string.editor_howto_map_controls_title))
        HowToItemsList(stringResource(Res.string.editor_howto_map_controls))

        HowToSubSection(title = stringResource(Res.string.editor_howto_river_title))
        HowToSection(title = null, content = stringResource(Res.string.editor_howto_river_text))

        HorizontalDivider()

        // Level Editor
        HowToSection(
            title = stringResource(Res.string.editor_howto_level_editor_title),
            content = stringResource(Res.string.editor_howto_level_editor_intro)
        )

        HowToSubSection(title = stringResource(Res.string.editor_howto_level_info_title))
        HowToItemsList(stringResource(Res.string.editor_howto_level_info_fields))

        HowToSubSection(title = stringResource(Res.string.editor_howto_enemy_spawns_title))
        HowToSection(title = null, content = stringResource(Res.string.editor_howto_enemy_spawns_text))

        HowToSubSection(title = stringResource(Res.string.editor_howto_towers_title))
        HowToSection(title = null, content = stringResource(Res.string.editor_howto_towers_text))

        HowToSubSection(title = stringResource(Res.string.editor_howto_waypoints_title))
        HowToSection(title = null, content = stringResource(Res.string.editor_howto_waypoints_text))

        HowToSubSection(title = stringResource(Res.string.editor_howto_initial_setup_title))
        HowToSection(title = null, content = stringResource(Res.string.editor_howto_initial_setup_text))

        HorizontalDivider()

        // Level Sequence
        HowToSection(
            title = stringResource(Res.string.editor_howto_sequence_title),
            content = stringResource(Res.string.editor_howto_sequence_text)
        )
        HowToItemsList(stringResource(Res.string.editor_howto_sequence_features))

        HorizontalDivider()

        // World Map Positions
        HowToSection(
            title = stringResource(Res.string.editor_howto_worldmap_title),
            content = stringResource(Res.string.editor_howto_worldmap_text)
        )
        HowToItemsList(stringResource(Res.string.editor_howto_worldmap_features))

        HorizontalDivider()

        // Tips
        HowToSection(
            title = stringResource(Res.string.editor_howto_tips_title),
            content = null
        )
        HowToItemsList(stringResource(Res.string.editor_howto_tips))

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HowToSection(title: String?, content: String?) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (content != null) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun HowToSubSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun HowToItemsList(items: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Items are separated by " - " (space-dash-space), matching the pattern used
            // in FeaturesList throughout the editor info strings.
            val itemList = items.split(" - ").filter { it.isNotBlank() }
            itemList.forEach { item ->
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•  ",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = item.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
