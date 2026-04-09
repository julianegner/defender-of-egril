package de.egril.defender.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Info page for the Level Editor with comprehensive documentation
 */
@Composable
fun EditorInfoPage() {
    val scrollState = rememberScrollState()
    
    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
        // Title
        Text(
            text = stringResource(Res.string.editor_info_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Introduction
        InfoSection(
            title = null,
            content = stringResource(Res.string.editor_info_intro)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Getting Started / Workflow
        InfoSection(
            title = stringResource(Res.string.editor_info_workflow_title),
            content = stringResource(Res.string.editor_info_workflow_text)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Copy hint
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = stringResource(Res.string.editor_info_copy_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Restore hint
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = stringResource(Res.string.editor_info_restore_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // File Location
        InfoSection(
            title = stringResource(Res.string.editor_info_file_location_title),
            content = stringResource(Res.string.editor_info_file_location_text)
        )
        
        // File location details
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(Res.string.editor_info_file_location_linux_mac),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.editor_info_file_location_windows),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Directory Structure
        InfoSection(
            title = stringResource(Res.string.editor_info_directory_structure_title),
            content = null
        )
        
        FeaturesList(stringResource(Res.string.editor_info_directory_structure_text))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Map Editor
        InfoSection(
            title = stringResource(Res.string.editor_info_map_editor_title),
            content = stringResource(Res.string.editor_info_map_editor_text)
        )
        
        FeaturesList(stringResource(Res.string.editor_info_map_editor_features))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Level Editor
        InfoSection(
            title = stringResource(Res.string.editor_info_level_editor_title),
            content = stringResource(Res.string.editor_info_level_editor_text)
        )
        
        FeaturesList(stringResource(Res.string.editor_info_level_editor_features))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Level Sequence
        InfoSection(
            title = stringResource(Res.string.editor_info_level_sequence_title),
            content = stringResource(Res.string.editor_info_level_sequence_text)
        )
        
        FeaturesList(stringResource(Res.string.editor_info_level_sequence_features))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // World Map Positions
        InfoSection(
            title = stringResource(Res.string.editor_info_world_map_title),
            content = stringResource(Res.string.editor_info_world_map_text)
        )
        
        FeaturesList(stringResource(Res.string.editor_info_world_map_features))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tips
        InfoSection(
            title = stringResource(Res.string.editor_info_tips_title),
            content = null
        )
        
        FeaturesList(stringResource(Res.string.editor_info_tips_text))
        
        Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Composable for an info section with optional title and content
 */
@Composable
private fun InfoSection(
    title: String?,
    content: String?
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
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

/**
 * Composable for displaying a bulleted features list
 */
@Composable
private fun FeaturesList(features: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Split features by dash separator (since the string has dashes instead of bullet points)
            // The features string should contain items separated by " - " (space-dash-space)
            val featureItems = features.split(" - ").filter { it.isNotBlank() }
            
            featureItems.forEach { feature ->
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
                        text = feature.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
