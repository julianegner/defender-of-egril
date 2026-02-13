package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.PlayerStats
import de.egril.defender.model.StatType
import de.egril.defender.model.SpellType
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.HammerIcon
import de.egril.defender.ui.icon.StarIcon
import de.egril.defender.ui.settings.SettingsButton
import defender_of_egril.composeapp.generated.resources.*

/**
 * Screen for upgrading player stats and unlocking spells
 */
@Composable
fun StatsUpgradeScreen(
    playerProfile: PlayerProfile,
    onUpgradeStat: (StatType) -> Unit,
    onUnlockSpell: (SpellType) -> Unit,
    onBack: () -> Unit
) {
    val stats = playerProfile.stats
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    text = "Stats & Abilities",  // TODO: localize
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // XP and Level Info
                PlayerLevelInfo(stats)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    // Available stat points
                    if (stats.availableStatPoints > 0) {
                        Text(
                            text = "Available Stat Points: ${stats.availableStatPoints}",  // TODO: localize
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    // Stats Section
                    Text(
                        text = "Stats",  // TODO: localize
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    StatCard(
                        name = "Health",  // TODO: localize
                        description = "+1 bonus HP per level",  // TODO: localize
                        currentLevel = stats.healthStat,
                        effect = "+${stats.getBonusHealth()} HP",
                        canUpgrade = stats.availableStatPoints > 0,
                        onUpgrade = { onUpgradeStat(StatType.HEALTH) },
                        icon = { HeartIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StatCard(
                        name = "Treasury",  // TODO: localize
                        description = "+50 start coins per level",  // TODO: localize
                        currentLevel = stats.treasuryStat,
                        effect = "+${stats.getBonusStartCoins()} coins",
                        canUpgrade = stats.availableStatPoints > 0,
                        onUpgrade = { onUpgradeStat(StatType.TREASURY) },
                        icon = { MoneyIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StatCard(
                        name = "Income",  // TODO: localize
                        description = "+10% coins from all sources",  // TODO: localize
                        currentLevel = stats.incomeStat,
                        effect = "+${(stats.incomeStat * 10)}%",
                        canUpgrade = stats.availableStatPoints > 0,
                        onUpgrade = { onUpgradeStat(StatType.INCOME) },
                        icon = { MoneyIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StatCard(
                        name = "Construction",  // TODO: localize
                        description = "Unlocks tower abilities",  // TODO: localize
                        currentLevel = stats.constructionStat,
                        effect = buildConstructionEffect(stats.constructionStat),
                        canUpgrade = stats.availableStatPoints > 0,
                        onUpgrade = { onUpgradeStat(StatType.CONSTRUCTION) },
                        icon = { HammerIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StatCard(
                        name = "Mana",  // TODO: localize
                        description = "+5 max mana per level",  // TODO: localize
                        currentLevel = stats.manaStat,
                        effect = "${stats.getMaxMana()} max mana",
                        canUpgrade = stats.availableStatPoints > 0,
                        onUpgrade = { onUpgradeStat(StatType.MANA) },
                        icon = { StarIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Spells Section
                    Text(
                        text = "Spells",  // TODO: localize
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Unlock powerful spells to cast during battle",  // TODO: localize
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    SpellType.values().forEach { spell ->
                        val isUnlocked = stats.isSpellUnlocked(spell)
                        SpellCard(
                            spell = spell,
                            isUnlocked = isUnlocked,
                            canUnlock = !isUnlocked && stats.availableStatPoints > 0,
                            onUnlock = { onUnlockSpell(spell) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Back button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.width(200.dp).height(50.dp)
                ) {
                    Text("Back")  // TODO: localize
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PlayerLevelInfo(stats: PlayerStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Level ${stats.level}",  // TODO: localize
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // XP Progress Bar
            val currentLevelXP = PlayerStats.getXPForLevel(stats.level)
            val nextLevelXP = PlayerStats.getXPForNextLevel(stats.level)
            val progressInLevel = stats.totalXP - currentLevelXP
            val requiredForLevel = nextLevelXP - currentLevelXP
            val progress = if (requiredForLevel > 0) progressInLevel.toFloat() / requiredForLevel.toFloat() else 1f
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$progressInLevel / $requiredForLevel XP",  // TODO: localize
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    name: String,
    description: String,
    currentLevel: Int,
    effect: String,
    canUpgrade: Boolean,
    onUpgrade: () -> Unit,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Level $currentLevel: $effect",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Upgrade button
            Button(
                onClick = onUpgrade,
                enabled = canUpgrade,
                modifier = Modifier.width(80.dp)
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun SpellCard(
    spell: SpellType,
    isUnlocked: Boolean,
    canUnlock: Boolean,
    onUnlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isUnlocked) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spell.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓ Unlocked",  // TODO: localize
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "${spell.manaCost} Mana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = spell.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (!isUnlocked) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onUnlock,
                    enabled = canUnlock,
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Unlock")  // TODO: localize
                }
            }
        }
    }
}

private fun buildConstructionEffect(level: Int): String {
    return when {
        level >= 3 -> "All abilities unlocked"  // TODO: localize
        level >= 2 -> "Spike barricades"  // TODO: localize
        level >= 1 -> "Spear barricades, Spike barbs"  // TODO: localize
        else -> "No abilities"  // TODO: localize
    }
}
