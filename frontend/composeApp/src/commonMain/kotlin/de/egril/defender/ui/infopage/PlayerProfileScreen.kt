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
import de.egril.defender.iam.IamState
import de.egril.defender.model.Achievement
import de.egril.defender.model.AchievementDefinitions
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.ProfileTabScrollbar
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.getLocalizedDescription
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.ui.icon.TrophyIcon
import de.egril.defender.ui.icon.UnlockIcon
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.utils.formatTimestamp
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Screen displaying player profile information and achievements
 */
@Composable
fun PlayerProfileScreen(
    playerProfile: PlayerProfile,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onSelectPlayer: () -> Unit = {},
    onNavigateToStats: (() -> Unit)? = null,
    onUpgradeAbility: ((de.egril.defender.model.AbilityType) -> Unit)? = null,
    onUnlockSpell: ((de.egril.defender.model.SpellType) -> Unit)? = null,
    iamState: IamState = IamState(),
    iamLoginInProgress: Boolean = false,
    onIamLogin: () -> Unit = {},
    onIamLogout: () -> Unit = {},
    onManageAccount: () -> Unit = {},
    onAlwaysLoginChanged: (Boolean) -> Unit = {},
    onUseRemoteSettingsChanged: (Boolean) -> Unit = {}
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
            
            SelectionContainer {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: toggle between full and compact view
                var headerCollapsed by remember { mutableStateOf(false) }

                if (headerCollapsed) {
                    // Compact single-row header: name | remote account | XP | ability points
                    val remoteAccountName = if (iamState.isAuthenticated && iamState.username != null) {
                        iamState.username
                    } else {
                        playerProfile.remoteUsername
                    }
                    val stats = playerProfile.abilities
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .padding(end = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Player name and info on the LEFT, taking remaining space
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Player name
                            Text(
                                text = playerProfile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (remoteAccountName != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    UnlockIcon(size = 12.dp)
                                    Text(
                                        text = remoteAccountName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            // XP
                            Text(
                                text = stringResource(Res.string.xp_progress, stats.totalXP, de.egril.defender.model.PlayerAbilities.getXPForNextLevel(stats.level)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Available ability points
                            if (stats.availableAbilityPoints > 0) {
                                Text(
                                    text = stringResource(Res.string.available_stat_points, stats.availableAbilityPoints),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Expand button on the RIGHT
                        TextButton(
                            onClick = { headerCollapsed = false },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.expand),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        // Switch player button on the RIGHT
                        OutlinedButton(
                            onClick = onSelectPlayer,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.switch_player),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    // Full header with player name title, switch player button, and collapse button
                    val headerBottomPadding = if (isPlatformMobile) 8.dp else 24.dp
                    val headerButtonHeight = if (isPlatformMobile) 28.dp else 36.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = headerBottomPadding)
                            .padding(end = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title on the LEFT, centered in remaining space
                        Text(
                            text = stringResource(Res.string.player_profile),
                            style = if (isPlatformMobile) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        // Collapse button on the RIGHT
                        TextButton(
                            onClick = { headerCollapsed = true },
                            modifier = Modifier.height(headerButtonHeight)
                        ) {
                            Text(
                                text = stringResource(Res.string.collapse),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        // Switch player button on the RIGHT
                        OutlinedButton(
                            onClick = onSelectPlayer,
                            modifier = Modifier.height(headerButtonHeight)
                        ) {
                            Text(
                                text = stringResource(Res.string.switch_player),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Scrollable content
                var selectedTabIndex by remember { mutableStateOf(1) }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (!headerCollapsed) {
                        // Cards: side-by-side on landscape, stacked+scrollable on portrait
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isPortrait = maxHeight > maxWidth
                            if (isPortrait) {
                                // Mobile/portrait: stacked, scrollable
                                val cardsScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(cardsScrollState)
                                ) {
                                    PlayerInfoCard(
                                        playerProfile = playerProfile,
                                        onEditName = onEditName
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    UserAccountCard(
                                        iamState = iamState,
                                        iamLoginInProgress = iamLoginInProgress,
                                        onIamLogin = onIamLogin,
                                        onIamLogout = onIamLogout,
                                        onManageAccount = onManageAccount,
                                        alwaysLogin = playerProfile.alwaysLogin,
                                        onAlwaysLoginChanged = onAlwaysLoginChanged,
                                        useRemoteSettings = playerProfile.useRemoteSettings,
                                        onUseRemoteSettingsChanged = onUseRemoteSettingsChanged
                                    )
                                }
                            } else {
                                // Desktop/landscape: side by side, ~30% profile / ~70% account
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(modifier = Modifier.weight(0.3f)) {
                                        PlayerInfoCard(
                                            playerProfile = playerProfile,
                                            onEditName = onEditName
                                        )
                                    }
                                    Box(modifier = Modifier.weight(0.7f)) {
                                        UserAccountCard(
                                            iamState = iamState,
                                            iamLoginInProgress = iamLoginInProgress,
                                            onIamLogin = onIamLogin,
                                            onIamLogout = onIamLogout,
                                            onManageAccount = onManageAccount,
                                            alwaysLogin = playerProfile.alwaysLogin,
                                            onAlwaysLoginChanged = onAlwaysLoginChanged,
                                            useRemoteSettings = playerProfile.useRemoteSettings,
                                            onUseRemoteSettingsChanged = onUseRemoteSettingsChanged
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Tabs (only show if stats callback is provided)
                    if (onNavigateToStats != null) {
                        PrimaryTabRow(
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
                        
                        // Tab content (scrollable) with vertical scrollbar
                        val tabScrollState = rememberScrollState()
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(tabScrollState)
                                    .padding(end = 12.dp)
                            ) {
                                when (selectedTabIndex) {
                                    0 -> {
                                        // Achievements tab - show achievements directly
                                        AchievementsListDirect(achievements = playerProfile.achievements)
                                    }
                                    1 -> {
                                        // Stats & Abilities tab - show stats directly if callbacks provided
                                        if (onUpgradeAbility != null && onUnlockSpell != null) {
                                            StatsAndAbilitiesContent(
                                                playerProfile = playerProfile,
                                                onUpgradeAbility = onUpgradeAbility,
                                                onUnlockSpell = onUnlockSpell
                                            )
                                        } else {
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
                            ProfileTabScrollbar(scrollState = tabScrollState)
                        }
                    } else {
                        // No stats callback - just show achievements directly (old behavior)
                        val tabScrollState = rememberScrollState()
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(tabScrollState)
                                    .padding(end = 12.dp)
                            ) {
                                AchievementsListDirect(achievements = playerProfile.achievements)
                            }
                            ProfileTabScrollbar(scrollState = tabScrollState)
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Available ability points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.available_stat_points_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${playerProfile.abilities.availableAbilityPoints}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (playerProfile.abilities.availableAbilityPoints > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
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
    onUpgradeAbility: (de.egril.defender.model.AbilityType) -> Unit,
    onUnlockSpell: (de.egril.defender.model.SpellType) -> Unit
) {
    val stats = playerProfile.abilities
    
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
                val currentLevelXP = de.egril.defender.model.PlayerAbilities.getXPForLevel(stats.level)
                val nextLevelXP = de.egril.defender.model.PlayerAbilities.getXPForNextLevel(stats.level)
                val progressInLevel = stats.totalXP - currentLevelXP
                val requiredForLevel = nextLevelXP - currentLevelXP
                val progress = if (requiredForLevel > 0) progressInLevel.toFloat() / requiredForLevel.toFloat() else 1f
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
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
        
        // Available ability points
        if (stats.availableAbilityPoints > 0) {
            Text(
                text = stringResource(Res.string.available_stat_points, stats.availableAbilityPoints),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Abilities Section Header
        Text(
            text = stringResource(Res.string.abilities),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Ability Cards Grid (3 columns)
        var showConstructionInfo by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                de.egril.defender.ui.AbilityCardGrid(
                    name = stringResource(Res.string.stat_health),
                    currentLevel = stats.healthAbility,
                    effect = stringResource(Res.string.stat_health_effect, stats.getBonusHealth()),
                    canUpgrade = stats.availableAbilityPoints > 0,
                    onUpgrade = { onUpgradeAbility(de.egril.defender.model.AbilityType.HEALTH) },
                    icon = { de.egril.defender.ui.icon.HeartIcon(size = 32.dp) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                de.egril.defender.ui.AbilityCardGrid(
                    name = stringResource(Res.string.stat_treasury),
                    currentLevel = stats.treasuryAbility,
                    effect = stringResource(Res.string.stat_treasury_effect, stats.getBonusStartCoins()),
                    canUpgrade = stats.availableAbilityPoints > 0,
                    onUpgrade = { onUpgradeAbility(de.egril.defender.model.AbilityType.TREASURY) },
                    icon = { de.egril.defender.ui.icon.MoneyIcon(size = 32.dp) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                de.egril.defender.ui.AbilityCardGrid(
                    name = stringResource(Res.string.stat_income),
                    currentLevel = stats.incomeAbility,
                    effect = stringResource(Res.string.stat_income_effect, stats.incomeAbility * 10),
                    canUpgrade = stats.availableAbilityPoints > 0,
                    onUpgrade = { onUpgradeAbility(de.egril.defender.model.AbilityType.INCOME) },
                    icon = { de.egril.defender.ui.icon.MoneyIcon(size = 32.dp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                de.egril.defender.ui.AbilityCardWithInfoGrid(
                    name = stringResource(Res.string.stat_construction),
                    currentLevel = stats.constructionAbility,
                    effect = de.egril.defender.ui.buildConstructionEffect(stats.constructionAbility),
                    canUpgrade = stats.availableAbilityPoints > 0,
                    onUpgrade = { onUpgradeAbility(de.egril.defender.model.AbilityType.CONSTRUCTION) },
                    icon = { de.egril.defender.ui.icon.HammerIcon(size = 32.dp) },
                    onShowInfo = { showConstructionInfo = true }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                de.egril.defender.ui.AbilityCardGrid(
                    name = stringResource(Res.string.stat_mana),
                    currentLevel = stats.manaAbility,
                    effect = stringResource(Res.string.stat_mana_effect, stats.getMaxMana()),
                    canUpgrade = stats.availableAbilityPoints > 0,
                    onUpgrade = { onUpgradeAbility(de.egril.defender.model.AbilityType.MANA) },
                    icon = { de.egril.defender.ui.icon.StarIcon(size = 32.dp) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        
        if (showConstructionInfo) {
            de.egril.defender.ui.ConstructionInfoDialog(
                onDismiss = { showConstructionInfo = false }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
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
            modifier = Modifier.padding(bottom = 3.dp)
        )
        
        // Spells Grid (3 columns)
        de.egril.defender.model.SpellType.values().toList().chunked(3).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { spell ->
                    val isUnlocked = stats.isSpellUnlocked(spell)
                    Box(modifier = Modifier.weight(1f)) {
                        de.egril.defender.ui.SpellCardGrid(
                            spell = spell,
                            isUnlocked = isUnlocked,
                            canUnlock = !isUnlocked && stats.availableAbilityPoints > 0,
                            onUnlock = { onUnlockSpell(spell) }
                        )
                    }
                }
                repeat(3 - chunk.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
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

/**
 * Card showing the IAM / user account state (logged-in info, login/logout button,
 * and the "always log in" toggle).
 */
@Composable
private fun UserAccountCard(
    iamState: IamState,
    iamLoginInProgress: Boolean,
    onIamLogin: () -> Unit,
    onIamLogout: () -> Unit,
    onManageAccount: () -> Unit = {},
    alwaysLogin: Boolean = false,
    onAlwaysLoginChanged: (Boolean) -> Unit = {},
    useRemoteSettings: Boolean = true,
    onUseRemoteSettingsChanged: (Boolean) -> Unit = {}
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.user_account),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (iamState.isAuthenticated) {
                // Three-column layout when wide enough: names | action buttons | toggles
                // Stacked layout on narrow widths
                val fullName = listOfNotNull(iamState.firstName, iamState.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 560.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Column 1: User names
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                iamState.username?.let { username ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        UnlockIcon(size = 14.dp)
                                        Text(
                                            text = username,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (fullName != null) {
                                    Text(
                                        text = fullName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                iamState.email?.let { email ->
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Column 2: Action buttons
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onManageAccount,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    ToolsIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_manage_account),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedButton(
                                    onClick = onIamLogout,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    LockIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_logout),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            // Column 3: Toggles
                            AccountSettingToggles(
                                alwaysLogin = alwaysLogin,
                                onAlwaysLoginChanged = onAlwaysLoginChanged,
                                useRemoteSettings = useRemoteSettings,
                                onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                                spreadAcrossWidth = false
                            )
                        }
                    } else {
                        // Narrow: stacked layout
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            iamState.username?.let { username ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    UnlockIcon(size = 14.dp)
                                    Text(
                                        text = username,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (fullName != null) {
                                Text(
                                    text = fullName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            iamState.email?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onManageAccount,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    ToolsIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_manage_account),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedButton(
                                    onClick = onIamLogout,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    LockIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_logout),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            HorizontalDivider()
                            AccountSettingToggles(
                                alwaysLogin = alwaysLogin,
                                onAlwaysLoginChanged = onAlwaysLoginChanged,
                                useRemoteSettings = useRemoteSettings,
                                onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                                spreadAcrossWidth = true
                            )
                        }
                    }
                }
            } else if (iamLoginInProgress) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.height(36.dp),
                    enabled = false
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.iam_login_waiting),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                AccountSettingToggles(
                    alwaysLogin = alwaysLogin,
                    onAlwaysLoginChanged = onAlwaysLoginChanged,
                    useRemoteSettings = useRemoteSettings,
                    onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                    spreadAcrossWidth = true
                )
            } else {
                OutlinedButton(
                    onClick = onIamLogin,
                    modifier = Modifier.height(36.dp)
                ) {
                    UnlockIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.iam_login),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                AccountSettingToggles(
                    alwaysLogin = alwaysLogin,
                    onAlwaysLoginChanged = onAlwaysLoginChanged,
                    useRemoteSettings = useRemoteSettings,
                    onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                    spreadAcrossWidth = true
                )
            }
        }
    }
}

/**
 * Reusable composable for the "Always log in" and "Use remote settings" toggle rows.
 *
 * @param spreadAcrossWidth When true, each row fills the available width with SpaceBetween
 *   arrangement (used in the stacked / non-authenticated layout). When false, label and switch
 *   are placed compactly side by side (used as the third column in the wide authenticated layout).
 */
@Composable
private fun AccountSettingToggles(
    alwaysLogin: Boolean,
    onAlwaysLoginChanged: (Boolean) -> Unit,
    useRemoteSettings: Boolean,
    onUseRemoteSettingsChanged: (Boolean) -> Unit,
    spreadAcrossWidth: Boolean = false
) {
    val rowModifier = if (spreadAcrossWidth) Modifier.fillMaxWidth() else Modifier
    val rowArrangement = if (spreadAcrossWidth) Arrangement.SpaceBetween else Arrangement.spacedBy(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = rowArrangement
        ) {
            Text(
                text = stringResource(Res.string.always_log_in),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = alwaysLogin,
                onCheckedChange = { onAlwaysLoginChanged(it) }
            )
        }
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = rowArrangement
        ) {
            Text(
                text = stringResource(Res.string.use_remote_settings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = useRemoteSettings,
                onCheckedChange = { onUseRemoteSettingsChanged(it) }
            )
        }
    }
}
