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

@Composable
fun RulesScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            BulletPoint("Place New Towers - costs coins, requires build time ⏱")
            BulletPoint("Attack Enemies - click tower with ⚡, then enemy in range")
            BulletPoint("Upgrade Towers - increases damage and range")
            BulletPoint("End Turn - click \"End Turn\" to finish")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enemy Turn
            SectionTitle("Enemy Turn")
            BulletPoint("Enemies move toward the target")
            BulletPoint("New enemies spawn")
            BulletPoint("Build timers advance (⏱ counts down)")
            BulletPoint("Damage-over-time effects are applied")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tower Types
            SectionTitle("Tower Types")
            TowerInfo("Spike Tower", "10 coins", "5 damage", "1 range", "Melee")
            TowerInfo("Spear Tower", "15 coins", "8 damage", "2 range", "Ranged")
            TowerInfo("Bow Tower", "20 coins", "10 damage", "3 range", "Ranged")
            TowerInfo("Wizard Tower", "50 coins", "30 damage", "3 range", "AOE")
            TowerInfo("Alchemy Tower", "40 coins", "15 damage", "2 range", "DoT")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Attack Types
            SectionTitle("Attack Types")
            BulletPoint("Melee/Ranged: Single target")
            BulletPoint("AOE (Area of Effect): Damages all enemies within 1 cell")
            BulletPoint("DoT (Damage over Time): Continuous damage for 3 turns")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enemies
            SectionTitle("Enemy Types")
            EnemyInfo("Goblin", "20 HP", "Speed 2", "5 coins")
            EnemyInfo("Skeleton", "15 HP", "Speed 2", "7 coins")
            EnemyInfo("Ork", "40 HP", "Speed 1", "10 coins")
            EnemyInfo("Witch", "25 HP", "Speed 2", "12 coins")
            EnemyInfo("Evil Wizard", "30 HP", "Speed 1", "15 coins")
            EnemyInfo("Ogre", "80 HP", "Speed 1", "20 coins")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Victory and Defeat
            SectionTitle("Victory & Defeat")
            SectionText("Victory: Defeat all enemies in all waves with at least 1 HP remaining")
            SectionText("Defeat: Each enemy reaching the target costs 1 HP. At 0 HP, you lose.")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid Legend
            SectionTitle("Grid Legend")
            BulletPoint("S: Start position (enemies spawn)")
            BulletPoint("T: Target position (defend this!)")
            BulletPoint("Blue: Your ready towers")
            BulletPoint("Gray: Towers still building")
            BulletPoint("Red: Enemies")
            BulletPoint("⏱: Build time remaining")
            BulletPoint("⚡: Actions remaining this turn")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tips
            SectionTitle("Strategic Tips")
            BulletPoint("Use initial building phase wisely")
            BulletPoint("Save 20-30 coins for mid-game")
            BulletPoint("Focus fire on tough enemies (Ogres, Orks)")
            BulletPoint("Wizard Towers for massive AOE damage")
            BulletPoint("Upgrade high-level towers rather than building new ones")
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Back button
        Button(
            onClick = onBack,
            modifier = Modifier.width(200.dp).height(50.dp)
        ) {
            Text("Back")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
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
private fun TowerInfo(name: String, cost: String, damage: String, range: String, type: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$cost | $damage | Range $range | $type",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EnemyInfo(name: String, hp: String, speed: String, reward: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$hp | $speed | $reward",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp
        )
    }
}
