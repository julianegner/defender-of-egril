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
import de.egril.defender.model.PlayerAbilities
import de.egril.defender.model.AbilityType
import de.egril.defender.model.SpellType
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.HammerIcon
import de.egril.defender.ui.icon.StarIcon
import de.egril.defender.ui.icon.InfoIcon
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.gameplay.ScrollableInfoCard
import defender_of_egril.composeapp.generated.resources.*

/**
 * Screen for upgrading player stats and unlocking spells
 */
@Composable
fun AbilitiesUpgradeScreen(
    playerProfile: PlayerProfile,
    onUpgradeAbility: (AbilityType) -> Unit,
    onUnlockSpell: (SpellType) -> Unit,
    onBack: () -> Unit
) {
    val abilities = playerProfile.abilities
    
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
                    text = stringResource(Res.string.abilities),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // XP and Level Info
                PlayerLevelInfo(abilities)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    // Available stat points
                    if (abilities.availableAbilityPoints > 0) {
                        Text(
                            text = stringResource(Res.string.available_stat_points, abilities.availableAbilityPoints),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    // Stats Section
                    Text(
                        text = stringResource(Res.string.abilities),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    AbilityCard(
                        name = stringResource(Res.string.stat_health),
                        description = stringResource(Res.string.stat_health_desc),
                        currentLevel = abilities.healthAbility,
                        effect = stringResource(Res.string.stat_health_effect, abilities.getBonusHealth()),
                        canUpgrade = abilities.availableAbilityPoints > 0,
                        onUpgrade = { onUpgradeAbility(AbilityType.HEALTH) },
                        icon = { HeartIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AbilityCard(
                        name = stringResource(Res.string.stat_treasury),
                        description = stringResource(Res.string.stat_treasury_desc),
                        currentLevel = abilities.treasuryAbility,
                        effect = stringResource(Res.string.stat_treasury_effect, abilities.getBonusStartCoins()),
                        canUpgrade = abilities.availableAbilityPoints > 0,
                        onUpgrade = { onUpgradeAbility(AbilityType.TREASURY) },
                        icon = { MoneyIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AbilityCard(
                        name = stringResource(Res.string.stat_income),
                        description = stringResource(Res.string.stat_income_desc),
                        currentLevel = abilities.incomeAbility,
                        effect = stringResource(Res.string.stat_income_effect, abilities.incomeAbility * 10),
                        canUpgrade = abilities.availableAbilityPoints > 0,
                        onUpgrade = { onUpgradeAbility(AbilityType.INCOME) },
                        icon = { MoneyIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Construction stat with info icon
                    var showConstructionInfo by remember { mutableStateOf(false) }
                    AbilityCardWithInfo(
                        name = stringResource(Res.string.stat_construction),
                        description = stringResource(Res.string.stat_construction_desc),
                        currentLevel = abilities.constructionAbility,
                        effect = buildConstructionEffect(abilities.constructionAbility),
                        canUpgrade = abilities.availableAbilityPoints > 0,
                        onUpgrade = { onUpgradeAbility(AbilityType.CONSTRUCTION) },
                        icon = { HammerIcon(size = 32.dp) },
                        onShowInfo = { showConstructionInfo = true }
                    )
                    
                    if (showConstructionInfo) {
                        ConstructionInfoDialog(
                            onDismiss = { showConstructionInfo = false }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AbilityCard(
                        name = stringResource(Res.string.stat_mana),
                        description = stringResource(Res.string.stat_mana_desc),
                        currentLevel = abilities.manaAbility,
                        effect = stringResource(Res.string.stat_mana_effect, abilities.getMaxMana()),
                        canUpgrade = abilities.availableAbilityPoints > 0,
                        onUpgrade = { onUpgradeAbility(AbilityType.MANA) },
                        icon = { StarIcon(size = 32.dp) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Spells Section
                    Text(
                        text = stringResource(Res.string.spells),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = stringResource(Res.string.spells_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    SpellType.values().forEach { spell ->
                        val isUnlocked = abilities.isSpellUnlocked(spell)
                        SpellCard(
                            spell = spell,
                            isUnlocked = isUnlocked,
                            canUnlock = !isUnlocked && abilities.availableAbilityPoints > 0,
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
                    Text(stringResource(Res.string.back))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PlayerLevelInfo(abilities: PlayerAbilities) {
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
                text = stringResource(Res.string.player_level, abilities.level),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // XP Progress Bar
            val currentLevelXP = PlayerAbilities.getXPForLevel(abilities.level)
            val nextLevelXP = PlayerAbilities.getXPForNextLevel(abilities.level)
            val progressInLevel = abilities.totalXP - currentLevelXP
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
                    text = stringResource(Res.string.xp_progress, progressInLevel, requiredForLevel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
internal fun AbilityCard(
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
                    text = stringResource(Res.string.stat_level_effect, currentLevel, effect),
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
internal fun AbilityCardWithInfo(
    name: String,
    description: String,
    currentLevel: Int,
    effect: String,
    canUpgrade: Boolean,
    onUpgrade: () -> Unit,
    icon: @Composable () -> Unit,
    onShowInfo: () -> Unit
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Info icon button
                    IconButton(
                        onClick = onShowInfo,
                        modifier = Modifier.size(24.dp)
                    ) {
                        InfoIcon(size = 16.dp)
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.stat_level_effect, currentLevel, effect),
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
internal fun ConstructionInfoDialog(
    onDismiss: () -> Unit
) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.stat_construction),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        onDismiss = onDismiss,
        width = 900.dp  // 3x wider than default (300dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.construction_info_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Level 1
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Column {
                    Text(
                        text = stringResource(Res.string.construction_level_1_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.construction_level_1_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Level 2
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Column {
                    Text(
                        text = stringResource(Res.string.construction_level_2_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.construction_level_2_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Level 3
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Column {
                    Text(
                        text = stringResource(Res.string.construction_level_3_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.construction_level_3_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
internal fun SpellCard(
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
                        text = spell.getLocalizedName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.spell_unlocked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.spell_mana_cost, spell.manaCost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = spell.getLocalizedDescription(),
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
                    Text(stringResource(Res.string.unlock))
                }
            }
        }
    }
}

@Composable
internal fun buildConstructionEffect(level: Int): String {
    return when {
        level >= 3 -> stringResource(Res.string.stat_construction_effect_level3)
        level >= 2 -> stringResource(Res.string.stat_construction_effect_level2)
        level >= 1 -> stringResource(Res.string.stat_construction_effect_level1)
        else -> stringResource(Res.string.stat_construction_effect_none)
    }
}
