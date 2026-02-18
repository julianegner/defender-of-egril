@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.Achievement
import de.egril.defender.model.AchievementDefinitions
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.getLocalizedDescription
import de.egril.defender.ui.icon.TrophyIcon
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.utils.formatTimestamp
import defender_of_egril.composeapp.generated.resources.*

/**
 * Screen displaying player profile information and achievements
 */
@Composable
fun PlayerProfileScreen(
    playerProfile: PlayerProfile,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onNavigateToStats: (() -> Unit)? = null,  // Optional callback to navigate to stats screen
    onUpgradeStat: ((de.egril.defender.model.StatType) -> Unit)? = null,  // Optional callback for stat upgrades
    onUnlockSpell: ((de.egril.defender.model.SpellType) -> Unit)? = null  // Optional callback for unlocking spells
) {
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
                // Header with player name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.player_profile),
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Scrollable content
                var selectedTabIndex by remember { mutableStateOf(0) }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Player Info Card
                    PlayerInfoCard(
                        playerProfile = playerProfile,
                        onEditName = onEditName
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Tabs (only show if stats callback is provided)
                    if (onNavigateToStats != null) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text(stringResource(Res.string.achievements)) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text(stringResource(Res.string.abilities)) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Tab content (scrollable)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                        ) {
                            when (selectedTabIndex) {
                                0 -> {
                                    // Achievements tab - show achievements directly
                                    AchievementsListDirect(achievements = playerProfile.achievements)
                                }
                                1 -> {
                                    // Stats & Abilities tab - show stats directly if callbacks provided
                                    if (onUpgradeStat != null && onUnlockSpell != null) {
                                        StatsAndAbilitiesContent(
                                            playerProfile = playerProfile,
                                            onUpgradeStat = onUpgradeStat,
                                            onUnlockSpell = onUnlockSpell
                                        )
                                    } else if (onNavigateToStats != null) {
                                        // Fallback to button if callbacks not provided
                                        Button(
                                            onClick = onNavigateToStats,
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.abilities),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // No stats callback - just show achievements directly (old behavior)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                        ) {
                            AchievementsListDirect(achievements = playerProfile.achievements)
                        }
                    }
                }
                
                // Back button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(Res.string.back))
                }
            }
        }
    }
}

/**
 * Card displaying player information
 */
@Composable
private fun PlayerInfoCard(
    playerProfile: PlayerProfile,
    onEditName: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Player Name with edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.player_name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playerProfile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                OutlinedButton(
                    onClick = onEditName,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.edit),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Created date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.player_created),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(playerProfile.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Last played date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.player_last_played),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(playerProfile.lastPlayedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Achievements count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.achievements_earned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${playerProfile.achievements.size} / ${AchievementDefinitions.allAchievements.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Direct achievements list (no collapsible wrapper)
 */
@Composable
private fun AchievementsListDirect(achievements: List<Achievement>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (achievements.isEmpty()) {
            // No achievements yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(Res.string.no_achievements_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            // Show achievements directly
            val sortedAchievements = achievements.sortedByDescending { it.earnedAt }
            
            sortedAchievements.forEach { achievement ->
                AchievementItem(achievement = achievement)
            }
        }
    }
}

/**
 * Stats and abilities content (embedded version for tab)
 */
@Composable
private fun StatsAndAbilitiesContent(
    playerProfile: PlayerProfile,
    onUpgradeStat: (de.egril.defender.model.StatType) -> Unit,
    onUnlockSpell: (de.egril.defender.model.SpellType) -> Unit
) {
    val stats = playerProfile.stats
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player Level Info
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
                    text = stringResource(Res.string.player_level, stats.level),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // XP Progress
                val currentLevelXP = de.egril.defender.model.PlayerStats.getXPForLevel(stats.level)
                val nextLevelXP = de.egril.defender.model.PlayerStats.getXPForNextLevel(stats.level)
                val progressInLevel = stats.totalXP - currentLevelXP
                val requiredForLevel = nextLevelXP - currentLevelXP
                val progress = if (requiredForLevel > 0) progressInLevel.toFloat() / requiredForLevel.toFloat() else 1f
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.xp_progress, progressInLevel, requiredForLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Available stat points
        if (stats.availableStatPoints > 0) {
            Text(
                text = stringResource(Res.string.available_stat_points, stats.availableStatPoints),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Stats Section Header
        Text(
            text = stringResource(Res.string.stats),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Stat Cards (same as StatsUpgradeScreen)
        de.egril.defender.ui.StatCard(
            name = stringResource(Res.string.stat_health),
            description = stringResource(Res.string.stat_health_desc),
            currentLevel = stats.healthStat,
            effect = stringResource(Res.string.stat_health_effect, stats.getBonusHealth()),
            canUpgrade = stats.availableStatPoints > 0,
            onUpgrade = { onUpgradeStat(de.egril.defender.model.StatType.HEALTH) },
            icon = { de.egril.defender.ui.icon.HeartIcon(size = 32.dp) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        de.egril.defender.ui.StatCard(
            name = stringResource(Res.string.stat_treasury),
            description = stringResource(Res.string.stat_treasury_desc),
            currentLevel = stats.treasuryStat,
            effect = stringResource(Res.string.stat_treasury_effect, stats.getBonusStartCoins()),
            canUpgrade = stats.availableStatPoints > 0,
            onUpgrade = { onUpgradeStat(de.egril.defender.model.StatType.TREASURY) },
            icon = { de.egril.defender.ui.icon.MoneyIcon(size = 32.dp) }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        de.egril.defender.ui.StatCard(
            name = stringResource(Res.string.stat_income),
            description = stringResource(Res.string.stat_income_desc),
            currentLevel = stats.incomeStat,
            effect = stringResource(Res.string.stat_income_effect, stats.incomeStat * 10),
            canUpgrade = stats.availableStatPoints > 0,
            onUpgrade = { onUpgradeStat(de.egril.defender.model.StatType.INCOME) },
            icon = { de.egril.defender.ui.icon.MoneyIcon(size = 32.dp) }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Construction stat with info icon
        var showConstructionInfo by remember { mutableStateOf(false) }
        de.egril.defender.ui.StatCardWithInfo(
            name = stringResource(Res.string.stat_construction),
            description = stringResource(Res.string.stat_construction_desc),
            currentLevel = stats.constructionStat,
            effect = de.egril.defender.ui.buildConstructionEffect(stats.constructionStat),
            canUpgrade = stats.availableStatPoints > 0,
            onUpgrade = { onUpgradeStat(de.egril.defender.model.StatType.CONSTRUCTION) },
            icon = { de.egril.defender.ui.icon.HammerIcon(size = 32.dp) },
            onShowInfo = { showConstructionInfo = true }
        )
        
        if (showConstructionInfo) {
            de.egril.defender.ui.ConstructionInfoDialog(
                onDismiss = { showConstructionInfo = false }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        de.egril.defender.ui.StatCard(
            name = stringResource(Res.string.stat_mana),
            description = stringResource(Res.string.stat_mana_desc),
            currentLevel = stats.manaStat,
            effect = stringResource(Res.string.stat_mana_effect, stats.getMaxMana()),
            canUpgrade = stats.availableStatPoints > 0,
            onUpgrade = { onUpgradeStat(de.egril.defender.model.StatType.MANA) },
            icon = { de.egril.defender.ui.icon.StarIcon(size = 32.dp) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Spells Section
        Text(
            text = stringResource(Res.string.spells),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(Res.string.spells_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        de.egril.defender.model.SpellType.values().forEach { spell ->
            val isUnlocked = stats.isSpellUnlocked(spell)
            de.egril.defender.ui.SpellCard(
                spell = spell,
                isUnlocked = isUnlocked,
                canUnlock = !isUnlocked && stats.availableStatPoints > 0,
                onUnlock = { onUnlockSpell(spell) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Single achievement item display
 */
@Composable
private fun AchievementItem(achievement: Achievement) {
    val info = AchievementDefinitions.getInfo(achievement.id)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Trophy icon
            TrophyIcon(
                size = 32.dp,
                tint = MaterialTheme.colorScheme.tertiary
            )
            
            // Achievement info
            Column(modifier = Modifier.weight(1f)) {
                // Achievement name
                Text(
                    text = achievement.id.getLocalizedName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Achievement description
                Text(
                    text = achievement.id.getLocalizedDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Earned date
                Text(
                    text = stringResource(Res.string.earned_on, formatTimestamp(achievement.earnedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
