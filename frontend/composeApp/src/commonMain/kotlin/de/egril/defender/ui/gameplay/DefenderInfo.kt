package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.SpellDoubleLevelColor
import de.egril.defender.ui.animations.SpellDoubleReachColor
import de.egril.defender.ui.icon.HoleIcon
import de.egril.defender.ui.icon.InfoIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.PickIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import de.egril.defender.ui.icon.WarningIcon
import com.hyperether.resources.AppLocale
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun DefenderInfo(
    defender: Defender,
    gameState: GameState,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    onWizardAction: ((Int, WizardAction) -> Unit)? = null,  // For wizard tower magical traps - click to select action, then click map
    selectedMineAction: MineAction? = null,  // Current trap placement mode
    selectedWizardAction: WizardAction? = null,  // Current wizard trap placement mode
    onBarricadeAction: ((Int, BarricadeAction) -> Unit)? = null,  // For spike/spear tower barricades - click to select action, then click map
    selectedBarricadeAction: BarricadeAction? = null,  // Current barricade placement mode
    compactBuyPanel: Boolean = false,
    isMobile: Boolean = false,  // Add platform parameter
    selectedTargetId: Int? = null,
    selectedTargetPosition: Position? = null,
    onDefenderAttack: ((Int, Int) -> Boolean)? = null,
    onDefenderAttackPosition: ((Int, Position) -> Boolean)? = null,
    isPlayerTurn: Boolean = false,
    hasUnlockedSpells: Boolean = false  // Whether player has unlocked any spells
) {
    val locale = com.hyperether.resources.currentLanguage.value
    val buttonHeight = if (isMobile) 100.dp else 60.dp
    // Use key to force recomposition when defender stats change
    key(
        defender.id,
        defender.level,
        defender.damage,
        defender.range,
        defender.actionsRemaining.value,
        defender.buildTimeRemaining.value,
        defender.isReady
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isMobile) 4.dp else 8.dp)
        ) {
                // Compute active spell effects once for use throughout the card
                val doubleLevelEffect = gameState.activeSpellEffects.find {
                    it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == defender.id
                }
                val doubleReachEffect = gameState.activeSpellEffects.find {
                    it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == defender.id
                }
                // Tower icon, name, and actions in one row
                Row(
                    // modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Tower icon - double the original size
                    val iconSize = if (isMobile) 64.dp else 96.dp
                    val iconInnerSize = if (isMobile) 56.dp else 88.dp
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        TowerIcon(defender = defender, modifier = Modifier.size(iconInnerSize), gameState = gameState)
                    }

                    val horizontalSpacing = if (isMobile) 4.dp else 8.dp
                    Spacer(modifier = Modifier.width(horizontalSpacing))

                    // Tower name and level
                    Column(modifier = Modifier.weight(1f)
                    ) {
                        val displayName = if (defender.type == DefenderType.DRAGONS_LAIR) {
                            // Check if the specific dragon from this lair is still alive
                            val dragonAlive = defender.dragonId.value?.let { dragonId ->
                                gameState.attackers.any {
                                    it.id == dragonId && !it.isDefeated.value
                                }
                            } ?: false
                            if (dragonAlive) {
                                // Show "Lair of the Dragon [name]" in normal text above the italic desc
                                if (defender.dragonName != null) {
                                    "${stringResource(Res.string.lair_of_the_dragon)} ${defender.dragonName}"
                                } else {
                                    stringResource(Res.string.dragons_lair)
                                }
                            } else {
                                stringResource(Res.string.empty_dragons_lair)
                            }
                        } else if (defender.raftId.value != null) {
                            // If defender is on a raft, show localized "Type Raft" with space separator
                            // Use getLocalizedShortName() to get just the type (e.g., "Bow" instead of "Bow Tower")
                            "${defender.type.getLocalizedShortName(locale)} ${stringResource(Res.string.raft)}"
                        } else {
                            // Regular tower - use localized name
                            defender.type.getLocalizedName(locale)
                        }
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        // Compute spell effect once and reuse below
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (doubleLevelEffect != null) {
                                val effectiveLevel = defender.level.value * 2
                                Text(
                                    "Level $effectiveLevel (×2)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpellDoubleLevelColor,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "Level ${defender.level.value}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GamePlayColors.Success
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SwordIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    defender.type.attackType.getLocalizedName(locale),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                        // Show Double Tower Level spell active description
                        if (doubleLevelEffect != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                LightningIcon(size = 12.dp)
                                Text(
                                    stringResource(Res.string.double_level_active),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpellDoubleLevelColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Show tower base info if on tower base
                        val towerBase = gameState.barricades.find { it.id == defender.towerBaseBarricadeId.value }
                        if (towerBase != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                WoodIcon(size = 12.dp)
                                Text(
                                    stringResource(Res.string.tower_base_hp_label, towerBase.healthPoints.value),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (towerBase.healthPoints.value < 100) GamePlayColors.Warning else GamePlayColors.Success
                                )
                            }
                        }
                        Row {
                            DefenderActionsInfo(defender)
                            dwarvenMineInfoButtonArea(defender)
                            TowerInfoButtonArea(defender, gameState)
                        }
                        // Warn when a dragon is targeting this mine
                        if (defender.type == DefenderType.DWARVEN_MINE) {
                            val targetingDragon = gameState.attackers.find {
                                it.type == AttackerType.DRAGON &&
                                    it.targetMineId.value == defender.id &&
                                    !it.isDefeated.value
                            }
                            if (targetingDragon != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    WarningIcon(size = 12.dp)
                                    Text(
                                        "${stringResource(Res.string.mine_targeted_by_dragon)}: ${targetingDragon.dragonName ?: stringResource(Res.string.dragon_name)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                
                // Show undo button for towers that are still building (in same turn as placed)
                if (!defender.isReady && defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value) {
                    Spacer(modifier = Modifier.width(horizontalSpacing))
                    Column(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        UndoOrSellButton(
                            defender = defender,
                            gameState = gameState,
                            onUndoTower = onUndoTower,
                            onSellTower = onSellTower,
                            modifier = Modifier
                                .width(240.dp)
                                .height(buttonHeight)
                        )
                    }
                }

                if (defender.isReady) {
                    if (defender.type == DefenderType.DRAGONS_LAIR) {
                        // Dragon's lair - no actions, can't be sold
                        // Show dragon name in normal text if available
                        val dragonAlive = defender.dragonId.value?.let { dragonId ->
                            gameState.attackers.any {
                                it.id == dragonId && !it.isDefeated.value
                            }
                        } ?: false
                        
                        if (dragonAlive && defender.dragonName != null) {
                            Text(
                                "${stringResource(Res.string.lair_of_the_dragon)} ${defender.dragonName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GamePlayColors.ErrorDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            stringResource(Res.string.dragons_lair_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {

                        // Normal tower stats and buttons
                        // Calculate next level stats using helper functions
                        val currentLevel = defender.level.value
                        val nextLevel = currentLevel + 1
                        val nextActualDamage = if (defender.type == DefenderType.DWARVEN_MINE) {
                            calculateTrapDamage(defender, nextLevel)
                        } else {
                            calculateActualDamage(defender, nextLevel)
                        }
                        val nextRange = calculateRange(defender, nextLevel)
                        val nextActions = calculateActionsPerTurn(defender, nextLevel)

                        // Effective stats considering active spells
                        val effectiveLevel = if (doubleLevelEffect != null) currentLevel * 2 else currentLevel
                        val currentDisplayDamage = if (doubleLevelEffect != null) {
                            if (defender.type == DefenderType.DWARVEN_MINE) calculateTrapDamage(defender, effectiveLevel)
                            else calculateActualDamage(defender, effectiveLevel)
                        } else {
                            if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.actualDamage
                        }
                        val currentDisplayRange = when {
                            doubleReachEffect != null -> defender.range * 2
                            doubleLevelEffect != null -> calculateRange(defender, effectiveLevel)
                            else -> defender.range
                        }
                        val currentDisplayActions = if (doubleLevelEffect != null) {
                            calculateActionsPerTurn(defender, effectiveLevel)
                        } else {
                            defender.actionsPerTurnCalculated
                        }

                        // Current stats column
                            Column(modifier = Modifier.weight(0.5f)) {
                                Text(
                                    if (doubleLevelEffect != null) "Lvl $effectiveLevel (×2)" else "Lvl $currentLevel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doubleLevelEffect != null) SpellDoubleLevelColor else Color.Unspecified
                                )
                                TowerStats(
                                    defender.type.minRange,
                                    currentDisplayDamage,
                                    currentDisplayRange,
                                    currentDisplayActions,
                                    rangeColor = if (doubleReachEffect != null) SpellDoubleReachColor else Color.Unspecified
                                )
                            }

                            // After upgrade stats column
                            Column(modifier = Modifier.weight(0.5f)) {
                                Text(
                                    "Lvl $nextLevel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gameState.canUpgradeDefender(defender)) GamePlayColors.Success else Color.Gray
                                )
                                TowerStats(
                                    defender.type.minRange,
                                    nextActualDamage,
                                    nextRange,
                                    nextActions
                                )
                            }

                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                UpgradeButton(defender, gameState,
                                    onUpgradeDefender = onUpgradeDefender,
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(buttonHeight),
                                )
                            }
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                                ) {
                                UndoOrSellButton(
                                    defender = defender,
                                    gameState = gameState,
                                    onUndoTower = onUndoTower,
                                    onSellTower = onSellTower,
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(buttonHeight)
                                )
                            }

                            if (isPlayerTurn &&
                                defender.type != DefenderType.DWARVEN_MINE &&
                                defender.type != DefenderType.DRAGONS_LAIR &&
                                onDefenderAttack != null &&
                                onDefenderAttackPosition != null) {

                                Spacer(modifier = Modifier.width(horizontalSpacing))
                                Column(modifier = Modifier.weight(1.3f)) {
                                    AttackButton(
                                        defender = defender,
                                        gameState = gameState,
                                        selectedTargetId = selectedTargetId,
                                        selectedTargetPosition = selectedTargetPosition,
                                        onDefenderAttack = { defenderId, targetId ->
                                            onDefenderAttack(defenderId, targetId)
                                        },
                                        onDefenderAttackPosition = { defenderId, targetPos ->
                                            onDefenderAttackPosition(defenderId, targetPos)
                                        },
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(buttonHeight)
                                    )
                                }
                            }

                            // Generate Mana button for wizard tower (when mana is below max)
                            if (isPlayerTurn &&
                                defender.type == DefenderType.WIZARD_TOWER &&
                                gameState.currentMana.value < gameState.maxMana.value &&
                                onWizardAction != null) {

                                Spacer(modifier = Modifier.width(horizontalSpacing))
                                Column(modifier = Modifier.weight(1.3f)) {
                                    GenerateManaButton(
                                        defender = defender,
                                        gameState = gameState,
                                        onWizardAction = onWizardAction,
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(buttonHeight)
                                    )
                                }
                            }

                            // Magical trap button for wizard tower level 10+
                            if (isPlayerTurn &&
                                defender.type == DefenderType.WIZARD_TOWER &&
                                defender.level.value >= 10 &&
                                onWizardAction != null) {

                                Spacer(modifier = Modifier.width(horizontalSpacing))
                                Column(modifier = Modifier.weight(1.3f)) {
                                    MagicalTrapButton(
                                        defender = defender,
                                        onWizardAction = onWizardAction,
                                        selectedWizardAction = selectedWizardAction,
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(buttonHeight)
                                    )
                                }
                            }

                            // Barricade button for spike tower (level 20+) or spear tower (level 10+)
                            // Requires Construction level 1 for spear, level 2 for spike
                            if (isPlayerTurn && onBarricadeAction != null) {
                                val canBuildBarricade = when (defender.type) {
                                    DefenderType.SPIKE_TOWER -> 
                                        defender.level.value >= 20 && 
                                        gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_2
                                    DefenderType.SPEAR_TOWER -> 
                                        defender.level.value >= 10 && 
                                        gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
                                    else -> false
                                }
                                
                                if (canBuildBarricade) {
                                    Spacer(modifier = Modifier.width(horizontalSpacing))
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        BarricadeButton(
                                            defender = defender,
                                            onBarricadeAction = onBarricadeAction,
                                            selectedBarricadeAction = selectedBarricadeAction,
                                            doubleLevelEffect = doubleLevelEffect,
                                            modifier = Modifier
                                                .width(240.dp)
                                                .height(buttonHeight)
                                        )
                                    }
                                }
                            }

                            dwarvenMineActionButtonArea(
                                defender.type,
                                gameState,
                                defender,
                                onMineAction,
                                selectedMineAction,
                                compactBuyPanel,
                                horizontalSpacing,
                                buttonHeight
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun dwarvenMineInfoButtonArea(defender: Defender) {
    // Mine info is now handled by the combined TowerInfoButtonArea
    // This function is kept for compatibility but does nothing
}

@Composable
private fun RowScope.dwarvenMineActionButtonArea(
    type: DefenderType,
    gameState: GameState,
    defender: Defender,
    onMineAction: ((Int, MineAction) -> Unit)?,
    selectedMineAction: MineAction? = null,  // Current trap placement mode
    compactBuyPanel: Boolean = false,
    horizontalSpacing: Dp = 8.dp,
    buttonHeight: Dp = 60.dp

) {
    if (type == DefenderType.DWARVEN_MINE) {
        val mineActionsEnabled =
            gameState.phase.value != GamePhase.INITIAL_BUILDING && defender.actionsRemaining.value > 0

        if (mineActionsEnabled || gameState.phase.value == GamePhase.INITIAL_BUILDING) {
            // Dig button
            Column(modifier = Modifier.weight(0.5f)) {
                Button(
                    onClick = { onMineAction?.invoke(defender.id, MineAction.DIG) },
                    enabled = mineActionsEnabled,
                    modifier = Modifier
                        .width(240.dp)
                        .height(buttonHeight)
                        .padding(start = horizontalSpacing),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PickIcon(size = 24.dp)
                        Text(stringResource(Res.string.dig), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }


            Column(modifier = Modifier.weight(0.5f)) {
                // Trap button
                val isTrapModeActive = selectedMineAction == MineAction.BUILD_TRAP
                Button(
                    onClick = {
                        onMineAction?.invoke(
                            defender.id,
                            MineAction.BUILD_TRAP
                        )
                    },
                    enabled = mineActionsEnabled,
                    modifier = Modifier
                        .width(240.dp)
                        .height(buttonHeight)
                        .padding(start = horizontalSpacing),
                    border = if (isTrapModeActive) {
                        androidx.compose.foundation.BorderStroke(
                            width = 3.dp,
                            color = GamePlayColors.Yellow
                        )
                    } else null,
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrapIcon(size = 24.dp)
                        Text(stringResource(Res.string.trap), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (!compactBuyPanel) {
            Spacer(modifier = Modifier.weight(3f))
        }
    } else {
        Spacer(modifier = Modifier.weight(if (compactBuyPanel) 1f else 4f))
    }
}

@Composable
fun MiningOutcomeGrid() {
    Column {
        // Header row
        Row {
            Text(stringResource(Res.string.dig_outcome_name), Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.dig_outcome_chance), Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.dig_outcome_reward), Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        // Data rows
        DigOutcome.values().forEach { outcome ->
            Row {
                Text(outcome.displayName, Modifier.weight(1f))
                Text("${outcome.probability}", Modifier.weight(1f))
                Text("${outcome.coins}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DefenderActionsInfo(defender: Defender) {
    if (!defender.isReady) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Building: ${defender.buildTimeRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.Warning
            )
        }
    } else if (defender.isDisabled.value) {
        // Show disabled status by Red Witch
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            de.egril.defender.ui.icon.LockIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${stringResource(Res.string.disabled)}: ${defender.disabledTurnsRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.ErrorDark,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LightningIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${defender.actionsRemaining.value}/${defender.actionsPerTurnCalculated}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * Button for wizard tower to generate mana
 * Generates base 5 mana + (level / 5) bonus mana
 * Always available when wizard has actions
 */
@Composable
fun GenerateManaButton(
    defender: Defender,
    gameState: GameState,
    onWizardAction: (Int, WizardAction) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady) {
        // Calculate mana generation amount
        val manaAmount = 5 + (defender.level.value / 5)
        val isAtMaxMana = gameState.currentMana.value >= gameState.maxMana.value
        
        // Button to generate mana - enabled when wizard has actions and not at max mana
        Button(
            onClick = { onWizardAction(defender.id, WizardAction.GENERATE_MANA) },
            enabled = defender.actionsRemaining.value > 0 && !isAtMaxMana,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C27B0)  // Purple color for mana
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 4.dp)
            ) {
                // Mana icon on the left
                de.egril.defender.ui.icon.PentagramIcon(size = 40.dp)
                Spacer(modifier = Modifier.width(4.dp))
                // Two rows on the right
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Upper row: "Generate Mana"
                    Text(
                        stringResource(Res.string.generate_mana),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    // Lower row: "+X Mana"
                    Text(
                        "+$manaAmount ${stringResource(Res.string.mana_label)}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

/**
 * Button for wizard tower to place magical traps (level 10+)
 * Works like dwarven mine trap button - click to enter placement mode, then click on map
 * Always shows when wizard is ready, with cooldown info when on cooldown
 */
@Composable
fun MagicalTrapButton(
    defender: Defender,
    onWizardAction: (Int, WizardAction) -> Unit,
    selectedWizardAction: WizardAction? = null,  // Current wizard trap placement mode
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady) {
        val isOnCooldown = defender.trapCooldownRemaining.value > 0
        val isTrapModeActive = selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP

        // Button to enter magical trap placement mode - enabled when trap is ready and has actions
        Button(
            onClick = { onWizardAction(defender.id, WizardAction.PLACE_MAGICAL_TRAP) },
            enabled = !isOnCooldown && defender.actionsRemaining.value > 0,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = GamePlayColors.InfoDark
            ),
            border = if (isTrapModeActive) {
                androidx.compose.foundation.BorderStroke(
                    width = 3.dp,
                    color = GamePlayColors.Yellow
                )
            } else null
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                de.egril.defender.ui.icon.PentagramIcon(size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(3f)) {
                    Text(
                        stringResource(Res.string.magical_trap),
                        fontSize = if (isOnCooldown) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isOnCooldown) {
                    Column(modifier = Modifier.weight(1f)) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            defender.trapCooldownRemaining.value.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Button for spike/spear tower to place barricades
 * Spike Tower: level 20+ with HP = (level - 20) / 2 (minimum 1)
 * Spear Tower: level 10+ with HP = level - 10 (minimum 1)
 * Works like wizard magical trap button - click to enter placement mode, then click on map
 */
@Composable
fun BarricadeButton(
    defender: Defender,
    onBarricadeAction: (Int, BarricadeAction) -> Unit,
    selectedBarricadeAction: BarricadeAction? = null,  // Current barricade placement mode
    doubleLevelEffect: ActiveSpellEffect? = null,  // Active DOUBLE_TOWER_LEVEL spell effect
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady) {
        // Calculate HP that will be added based on tower type, using effective level
        val effectiveLevel = if (doubleLevelEffect != null) defender.level.value * 2 else defender.level.value
        val hpAmount = if (defender.type == DefenderType.SPIKE_TOWER) {
            maxOf(1, (effectiveLevel - 20) / 2)
        } else {
            maxOf(1, effectiveLevel - 10)
        }
        val isBarricadeModeActive = selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE
        
        // Button to enter barricade placement mode - enabled when tower has actions
        Button(
            onClick = { onBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE) },
            enabled = defender.actionsRemaining.value > 0,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF795548)  // Brown color for wood
            ),
            border = if (isBarricadeModeActive) {
                androidx.compose.foundation.BorderStroke(
                    width = 3.dp,
                    color = GamePlayColors.Yellow
                )
            } else null
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 4.dp)
            ) {
                // Icon on the left
                WoodIcon(size = 40.dp)
                Spacer(modifier = Modifier.width(4.dp))
                // Two rows on the right
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Upper row: "Barricade"
                    Text(
                        stringResource(Res.string.barricade),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    // Lower row: "X HP"
                    Text(
                        "$hpAmount ${stringResource(Res.string.hp_label)}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

/**
 * Calculate the damage for a defender at a specific level
 */
private fun calculateDamage(defender: Defender, level: Int): Int {
    return defender.type.baseDamage + (level - 1) * 5
}

/**
 * Calculate the actual damage (accounting for LASTING attack type) for a defender at a specific level
 */
private fun calculateActualDamage(defender: Defender, level: Int): Int {
    val baseDamage = calculateDamage(defender, level)
    return when (defender.type.attackType) {
        AttackType.LASTING -> baseDamage / 2
        else -> baseDamage
    }
}

/**
 * Calculate trap damage for dwarven mine at a specific level
 */
private fun calculateTrapDamage(defender: Defender, level: Int): Int {
    return if (defender.type == DefenderType.DWARVEN_MINE) {
        10 + ((level / 2) * 5)
    } else {
        0
    }
}

/**
 * Calculate the range for a defender at a specific level
 */
private fun calculateRange(defender: Defender, level: Int): Int {
    val baseCalculatedRange = if (defender.type == DefenderType.DWARVEN_MINE) {
        // Dwarven mine has special growth: 3 base + 1 every 5 levels
        3 + (level / 5)
    } else {
        // Standard growth: baseRange + (level - 1) / 2
        defender.type.baseRange + (level - 1) / 2
    }
    // Apply maxRange cap if defined for this tower type
    return if (defender.type.maxRange != null) {
        minOf(baseCalculatedRange, defender.type.maxRange)
    } else {
        baseCalculatedRange
    }
}

/**
 * Calculate actions per turn for a defender at a specific level
 */
private fun calculateActionsPerTurn(defender: Defender, level: Int): Int {
    return if (defender.type == DefenderType.SPIKE_TOWER) {
        val bonusActions = level / 5
        minOf(defender.type.actionsPerTurn + bonusActions, 3)
    } else if (defender.type == DefenderType.DWARVEN_MINE) {
        1 + (level / 5)
    } else {
        defender.type.actionsPerTurn
    }
}

/**
 * Info message data for displaying in combined tower info dialog
 */
private data class TowerInfoMessage(
    val title: String,
    val message: String,
    val icon: @Composable () -> Unit,
    val color: Color,
    val extraContent: (@Composable () -> Unit)? = null  // Optional additional content after message
)

/**
 * Get all relevant info messages for a specific tower
 */
@Composable
private fun getTowerInfoMessages(defender: Defender, gameState: GameState): List<TowerInfoMessage> {
    val messages = mutableListOf<TowerInfoMessage>()
    
    // Add first-use info message for the tower type
    when (defender.type) {
        DefenderType.WIZARD_TOWER -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.wizard_first_use_title),
                    message = stringResource(Res.string.wizard_first_use_message),
                    icon = { 
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray, CircleShape),  // Gray background for visibility
                            contentAlignment = Alignment.Center
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.WIZARD_TOWER,
                                modifier = Modifier.size(56.dp)  // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFF9C27B0)  // Purple
                )
            )
        }
        DefenderType.ALCHEMY_TOWER -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.alchemy_first_use_title),
                    message = stringResource(Res.string.alchemy_first_use_message),
                    icon = { 
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray, CircleShape),  // Gray background for visibility
                            contentAlignment = Alignment.Center
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.ALCHEMY_TOWER,
                                modifier = Modifier.size(56.dp)  // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFF4CAF50)  // Green
                )
            )
        }
        DefenderType.BALLISTA_TOWER -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.ballista_first_use_title),
                    message = stringResource(Res.string.ballista_first_use_message),
                    icon = { 
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray, CircleShape),  // Gray background for visibility
                            contentAlignment = Alignment.Center
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.BALLISTA_TOWER,
                                modifier = Modifier.size(56.dp)  // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFF795548)  // Brown
                )
            )
        }
        DefenderType.DWARVEN_MINE -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.mine_first_use_title),
                    message = stringResource(Res.string.mine_first_use_message),
                    icon = { 
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray, CircleShape),  // Gray background for visibility
                            contentAlignment = Alignment.Center
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.DWARVEN_MINE,
                                modifier = Modifier.size(56.dp)  // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFFFFD700),  // Gold
                    extraContent = {
                        // Add mining probabilities section
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.mining_probabilities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MiningOutcomeGrid()
                    }
                )
            )
        }
        else -> {}
    }
    
    // Add ability info messages based on tower level
    
    // Spike barbs info (spike tower level 10+ AND Construction level 1+)
    if (defender.type == DefenderType.SPIKE_TOWER && 
        defender.level.value >= 10 && 
        gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.spike_barbs_info_title),
                message = stringResource(Res.string.spike_barbs_info_message),
                icon = { SwordIcon(size = 32.dp) },
                color = Color(0xFF8B4513)  // SaddleBrown
            )
        )
    }
    
    // Barricade info (spike tower level 20+ with Construction 2+ or spear tower level 10+ with Construction 1+)
    val hasBarricadeAbility = when (defender.type) {
        DefenderType.SPIKE_TOWER -> 
            defender.level.value >= 20 && 
            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_2
        DefenderType.SPEAR_TOWER -> 
            defender.level.value >= 10 && 
            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
        else -> false
    }
    if (hasBarricadeAbility) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.barricade_info_title),
                message = stringResource(Res.string.barricade_info_message),
                icon = { WoodIcon(size = 32.dp) },
                color = Color(0xFF795548)  // Brown
            )
        )
    }
    
    // Magical trap info (wizard tower level 10+)
    if (defender.type == DefenderType.WIZARD_TOWER && defender.level.value >= 10) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.magical_trap_tutorial_title),
                message = stringResource(Res.string.magical_trap_tutorial_message),
                icon = { de.egril.defender.ui.icon.PentagramIcon(size = 32.dp) },
                color = Color(0xFF9C27B0)  // Purple
            )
        )
    }
    
    // Extended area info (wizard or alchemy tower level 20+)
    if ((defender.type == DefenderType.WIZARD_TOWER || defender.type == DefenderType.ALCHEMY_TOWER) && 
        defender.level.value >= 20) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.extended_area_tutorial_title),
                message = stringResource(Res.string.extended_area_tutorial_message),
                icon = { de.egril.defender.ui.icon.ExplosionIcon(size = 32.dp) },
                color = Color(0xFFFF5722)  // Deep orange
            )
        )
    }
    
    return messages
}

/**
 * Tower info icon and dialog component (similar to mine info icon)
 * Triggers InfoState to show dialog as proper overlay
 */
@Composable
private fun TowerInfoButtonArea(defender: Defender, gameState: GameState) {
    val messages = getTowerInfoMessages(defender, gameState)
    
    // Only show info icon if there are info messages for this tower
    if (messages.isEmpty()) {
        return
    }
    
    // Info icon button - triggers InfoState to show dialog as overlay
    InfoIcon(
        size = 16.dp,
        modifier = Modifier
            .clickable {
                gameState.infoState.value = gameState.infoState.value.showInfo(
                    type = InfoType.TOWER_INFO,
                    towerId = defender.id
                )
            }
            .padding(4.dp)
    )
}

/**
 * Tower info dialog displayed as overlay (accessed by GamePlayScreen)
 * Shows all relevant info for the specified tower
 */
@Composable
internal fun TowerInfoDialog(
    defender: Defender,
    gameState: GameState,
    onDismiss: () -> Unit
) {
    val messages = getTowerInfoMessages(defender, gameState)
    
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.tower_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        width = 600.dp,
        maxHeight = 500.dp,
        onDismiss = onDismiss
    ) {
        messages.forEachIndexed { index, info ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            // Subtitle with icon (doubled in size)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(modifier = Modifier.size(64.dp)) {  // Doubled from 32.dp
                    info.icon()
                }
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = info.color,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Message content
            Text(
                text = info.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Extra content (if any)
            info.extraContent?.invoke()
        }
    }
}
