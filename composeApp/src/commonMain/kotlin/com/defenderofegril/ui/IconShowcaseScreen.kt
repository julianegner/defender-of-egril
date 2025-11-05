package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.*
import com.defenderofegril.ui.icon.enemy.EnemyIcon

/**
 * Preview/test screen to showcase all tower and enemy icons
 * This can be used for manual testing and visual verification
 */
@Composable
fun IconShowcaseScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFE0E0E0)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Tower and Enemy Icons Showcase",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tower Icons Section
        Text("Tower Icons", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DefenderType.entries.forEach { type ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF2196F3))
                            .border(2.dp, Color.White)
                    ) {
                        TowerIcon(
                            defender = Defender(
                                id = 1,
                                type = type,
                                position = Position(0, 0),
                                level = mutableStateOf(2),
                                actionsRemaining = mutableStateOf(1)
                            )
                        )
                    }
                    Text(
                        type.displayName.split(" ")[0],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tower States
        Text("Tower States", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Ready with actions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF2196F3))
                        .border(2.dp, Color.White)
                ) {
                    TowerIcon(
                        defender = Defender(
                            id = 1,
                            type = DefenderType.BOW_TOWER,
                            position = Position(0, 0),
                            level = mutableStateOf(3),
                            actionsRemaining = mutableStateOf(1)
                        )
                    )
                }
                Text("Ready (L3)", style = MaterialTheme.typography.labelSmall)
            }
            
            // Building
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF9E9E9E))
                        .border(2.dp, Color.Gray)
                ) {
                    TowerIcon(
                        defender = Defender(
                            id = 2,
                            type = DefenderType.WIZARD_TOWER,
                            position = Position(0, 0),
                            level = mutableStateOf(1),
                            buildTimeRemaining = mutableStateOf(2),
                            actionsRemaining = mutableStateOf(0)
                        )
                    )
                }
                Text("Building", style = MaterialTheme.typography.labelSmall)
            }
            
            // No actions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF7986CB))
                        .border(2.dp, Color(0xFF2196F3))
                ) {
                    TowerIcon(
                        defender = Defender(
                            id = 3,
                            type = DefenderType.SPEAR_TOWER,
                            position = Position(0, 0),
                            level = mutableStateOf(1),
                            actionsRemaining = mutableStateOf(0)
                        )
                    )
                }
                Text("No Actions", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Enemy Icons Section
        Text("Enemy Icons", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttackerType.entries.forEach { type ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFF44336))
                            .border(2.dp, Color(0xFFF44336))
                    ) {
                        EnemyIcon(
                            attacker = Attacker(
                                id = 1,
                                type = type,
                                position = mutableStateOf(Position(0, 0)),
                                currentHealth = mutableStateOf(type.health / 2)
                            )
                        )
                    }
                    Text(
                        type.displayName.split(" ")[0],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enemy Health States
        Text("Enemy Health States", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Full health
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFF44336))
                        .border(2.dp, Color(0xFFF44336))
                ) {
                    EnemyIcon(
                        attacker = Attacker(
                            id = 1,
                            type = AttackerType.ORK,
                            position = mutableStateOf(Position(0, 0)),
                            currentHealth = mutableStateOf(40)
                        )
                    )
                }
                Text("Full HP", style = MaterialTheme.typography.labelSmall)
            }
            
            // Half health
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFF44336))
                        .border(2.dp, Color(0xFFF44336))
                ) {
                    EnemyIcon(
                        attacker = Attacker(
                            id = 2,
                            type = AttackerType.ORK,
                            position = mutableStateOf(Position(0, 0)),
                            currentHealth = mutableStateOf(20)
                        )
                    )
                }
                Text("Half HP", style = MaterialTheme.typography.labelSmall)
            }
            
            // Low health
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFF44336))
                        .border(2.dp, Color(0xFFF44336))
                ) {
                    EnemyIcon(
                        attacker = Attacker(
                            id = 3,
                            type = AttackerType.ORK,
                            position = mutableStateOf(Position(0, 0)),
                            currentHealth = mutableStateOf(5)
                        )
                    )
                }
                Text("Low HP", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
