package com.defenderofegril.ui.worldmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.ui.icon.ToolsIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun EditorButtonCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800)  // Distinctive orange color
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
        ) {
            // Distinctive symbol - wrench/hammer icon
            ToolsIcon(size = 64.dp)
            
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(Res.string.level_editor),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.create_edit_maps_levels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
