package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.ui.icon.DoorIcon
import com.defenderofegril.ui.icon.LightningIcon
import com.defenderofegril.ui.icon.TargetIcon
import com.defenderofegril.ui.icon.TimerIcon
import com.defenderofegril.ui.settings.SettingsButton
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage

@Composable
fun RulesScreen(
    onBack: () -> Unit
) {
    val locale = currentLanguage.value
    
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Settings button in top-right corner
        SettingsButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "How to Play",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
            // Game Overview
            SectionTitle("Game Overview")
            SectionText("Defender of Egril is a turn-based tower defense game. Defend the meadows of Egril against waves of enemies under the evil banner of Ewhad.")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Initial Building Phase
            SectionTitle("Initial Building Phase")
            SectionText("At the start of each level:")
            BulletPoint("Place towers instantly (no build time)")
            BulletPoint("Use your starting coins strategically")
            BulletPoint("Towers are ready to attack immediately")
            BulletPoint("Click \"Start Battle\" when ready")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Your Turn
            SectionTitle("Your Turn")
            SectionText("During your turn, you can:")
            BulletPointWithIcon("Timer", { TimerIcon(size = 14.dp) }, "Place New Towers - costs coins, requires build time")
            BulletPointWithIcon("Lightning", { LightningIcon(size = 14.dp) }, "Attack Enemies - click tower with actions, then enemy in range")
            BulletPoint("Upgrade Towers - increases damage and range")
            BulletPoint("End Turn - click \"End Turn\" to finish")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enemy Turn
            SectionTitle("Enemy Turn")
            BulletPoint("Enemies move toward the target")
            BulletPoint("New enemies spawn")
            BulletPointWithIcon("Timer", { TimerIcon(size = 14.dp) }, "Build timers advance (counts down)")
            BulletPoint("Damage-over-time effects are applied")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tower Types
            SectionTitle("Tower Types")
            // Filter out DRAGONS_LAIR as it's not a regular tower
            DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR }.forEach { defenderType ->
                TowerInfo(defenderType)
            }

            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Attack Types
            SectionTitle("Attack Types")
            BulletPoint("Melee/Ranged: Single target")
            BulletPoint("Fireball (Area): Damages all enemies within 1 cell of impact")
            BulletPoint("Acid: Continuous damage for 3 turns within 1 cell of impact")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enemies
            SectionTitle("Enemy Types")
            // Filter out DRAGON as it's a boss/special enemy
            AttackerType.entries.filter { it != AttackerType.DRAGON }.forEach { attackerType ->
                EnemyInfo(attackerType)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Victory and Defeat
            SectionTitle("Victory & Defeat")
            SectionText("Victory: Defeat all enemies in all waves with at least 1 HP remaining")
            SectionText("Defeat: Each enemy reaching the target costs 1 HP. At 0 HP, you lose.")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid Legend
            SectionTitle("Grid Legend")
            BulletPointWithIcon("Door icon", { DoorIcon(size = 16.dp) }, "Spawn: Start position (enemies spawn)")
            BulletPointWithIcon("Target icon", { TargetIcon(size = 16.dp) }, "Target: Target position (defend this!)")
            BulletPoint("Blue: Your ready towers")
            BulletPoint("Gray: Towers still building")
            BulletPoint("Red: Enemies")
            BulletPointWithIcon("Timer icon", { TimerIcon(size = 16.dp) }, "Build time remaining")
            BulletPointWithIcon("Lightning icon", { LightningIcon(size = 16.dp) }, "Actions remaining this turn")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tips
            SectionTitle("Strategic Tips")
            BulletPoint("Use initial building phase wisely")
            BulletPoint("Save 20-30 coins for mid-game")
            BulletPoint("Focus fire on tough enemies (Ogres, Orks)")
            BulletPoint("Wizard Towers for massive Areal damage")
            BulletPoint("Upgrade high-level towers rather than building new ones")
            
            Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Back button
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(LocalizedStrings.get("back", locale))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    ) {
        Text("• ", style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BulletPointWithIcon(iconDescription: String, icon: @Composable () -> Unit, text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• ", style = MaterialTheme.typography.bodyLarge)
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TowerInfo(defenderType: DefenderType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tower icon on blue hexagon
        TowerIconOnHexagon(
            defenderType = defenderType,
            size = 40.dp
        )
        
        // Tower details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = defenderType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Build description based on tower type
            val description = when {
                defenderType.isMine -> "Special: dig for valuables or place traps on the path"
                defenderType.baseDamage == 0 -> "Special building"
                else -> "${defenderType.baseCost} coins | ${defenderType.baseDamage} damage | Range ${defenderType.baseRange} | ${defenderType.attackType.displayName}"
            }
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EnemyInfo(attackerType: AttackerType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enemy icon on red hexagon
        EnemyIconOnHexagon(
            attackerType = attackerType,
            size = 40.dp
        )
        
        // Enemy details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attackerType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${attackerType.health} HP | Speed ${attackerType.speed} | ${attackerType.reward} coins",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
    }
}
