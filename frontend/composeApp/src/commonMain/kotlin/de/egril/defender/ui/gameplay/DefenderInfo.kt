package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.a11y.a11ySemantics
import de.egril.defender.ui.animations.SpellDoubleLevelColor
import de.egril.defender.ui.animations.SpellDoubleReachColor
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.gameplay.defenderButtons.TowerStats
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun DefenderInfo(
    defender: Defender,
    gameState: GameState,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    onWizardAction: ((Int, WizardAction) -> Unit)? = null, // For wizard tower magical traps - click to select action, then click map
    selectedMineAction: MineAction? = null, // Current trap placement mode
    selectedWizardAction: WizardAction? = null, // Current wizard trap placement mode
    onBarricadeAction: ((Int, BarricadeAction) -> Unit)? = null, // For spike/spear tower barricades - click to select action, then click map
    selectedBarricadeAction: BarricadeAction? = null, // Current barricade placement mode
    compactBuyPanel: Boolean = false,
    isMobile: Boolean = false, // Add platform parameter
    selectedTargetId: Int? = null,
    selectedTargetPosition: Position? = null,
    onDefenderAttack: ((Int, Int) -> Boolean)? = null,
    onDefenderAttackPosition: ((Int, Position) -> Boolean)? = null,
    isPlayerTurn: Boolean = false,
    hasUnlockedSpells: Boolean = false, // Whether player has unlocked any spells
    onSanctifyDefender: ((Int) -> Boolean)? = null,
    onActivateAltar: ((Int) -> Boolean)? = null,
) {
    val locale = com.hyperether.resources.currentLanguage.value
    val buttonHeight = if (isMobile) 100.dp else 60.dp
    val defenderDisplayName =
        if (defender.type == DefenderType.DRAGONS_LAIR) {
            val dragonAlive =
                defender.dragonId.value?.let { dragonId ->
                    gameState.attackers.any { it.id == dragonId && !it.isDefeated.value }
                } ?: false
            if (dragonAlive) {
                if (defender.dragonName != null) {
                    "${stringResource(Res.string.lair_of_the_dragon)} ${defender.dragonName}"
                } else {
                    stringResource(Res.string.dragons_lair)
                }
            } else {
                stringResource(Res.string.empty_dragons_lair)
            }
        } else if (defender.raftId.value != null) {
            "${defender.type.getLocalizedShortName(locale)} ${stringResource(Res.string.raft)}"
        } else {
            defender.type.getLocalizedName(locale)
        }
    val levelLabel = stringResource(Res.string.level)
    val damageLabel = stringResource(Res.string.damage)
    val rangeLabel = stringResource(Res.string.range)
    val defenderCardLabel =
        buildString {
            append(defenderDisplayName)
            append(", ")
            append(levelLabel)
            append(": ")
            append(defender.level.value)
            append(", ")
            append(damageLabel)
            append(": ")
            append(defender.damage)
            append(", ")
            append(rangeLabel)
            append(": ")
            append(defender.range)
        }
    // Use key to force recomposition when defender stats change
    key(
        defender.id,
        defender.level,
        defender.damage,
        defender.range,
        defender.actionsRemaining.value,
        defender.buildTimeRemaining.value,
        defender.isReady,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(if (isMobile) 4.dp else 8.dp)
                    .a11ySemantics(label = defenderCardLabel),
        ) {
            // Compute active spell effects once for use throughout the card
            val doubleLevelEffect =
                gameState.activeSpellEffects.find {
                    it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == defender.id
                }
            val doubleReachEffect =
                gameState.activeSpellEffects.find {
                    it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == defender.id
                }
            // Tower icon, name, and actions in one row
            Row(
                // modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                // Tower icon - double the original size
                val iconSize = if (isMobile) 64.dp else 96.dp
                val iconInnerSize = if (isMobile) 56.dp else 88.dp
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    TowerIcon(defender = defender, modifier = Modifier.size(iconInnerSize), gameState = gameState)
                }

                val horizontalSpacing = if (isMobile) 4.dp else 8.dp
                Spacer(modifier = Modifier.width(horizontalSpacing))

                // Tower name and level
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        defenderDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    // Compute spell effect once and reuse below
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (doubleLevelEffect != null) {
                            val effectiveLevel = defender.level.value * 2
                            Text(
                                "Level $effectiveLevel (×2)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpellDoubleLevelColor,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text(
                                "Level ${defender.level.value}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GamePlayColors.Success,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SwordIcon(size = 12.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                defender.type.attackType.getLocalizedName(locale),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                    // Show Double Tower Level spell active description
                    if (doubleLevelEffect != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            LightningIcon(size = 12.dp)
                            Text(
                                stringResource(Res.string.double_level_active),
                                style = MaterialTheme.typography.bodySmall,
                                color = SpellDoubleLevelColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    // Show tower base info if on tower base
                    val towerBase = gameState.barricades.find { it.id == defender.towerBaseBarricadeId.value }
                    if (towerBase != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            WoodIcon(size = 12.dp)
                            Text(
                                stringResource(Res.string.tower_base_hp_label, towerBase.healthPoints.value),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (towerBase.healthPoints.value < 100) GamePlayColors.Warning else GamePlayColors.Success,
                            )
                        }
                    }
                    // Show raft health: a raft that strands becomes a barricade with this health
                    // minus the stranding damage, and needs 100 health left to keep carrying the tower.
                    val raft = defender.raftId.value?.let { raftId -> gameState.rafts.find { it.id == raftId } }
                    if (raft != null) {
                        val strandedHealth = raft.healthPoints.value - de.egril.defender.model.Raft.STRANDING_DAMAGE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            WoodIcon(size = 12.dp)
                            Text(
                                stringResource(Res.string.raft_hp_label, raft.healthPoints.value),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (strandedHealth < 100) GamePlayColors.Warning else GamePlayColors.Success,
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
                        val targetingDragon =
                            gameState.attackers.find {
                                it.type == AttackerType.DRAGON &&
                                    it.targetMineId.value == defender.id &&
                                    !it.isDefeated.value
                            }
                        if (targetingDragon != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                WarningIcon(size = 12.dp)
                                Text(
                                    "${stringResource(
                                        Res.string.mine_targeted_by_dragon,
                                    )}: ${targetingDragon.dragonName ?: stringResource(Res.string.dragon_name)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Show undo button for towers that are still building (in same turn as placed)
                if (!defender.isReady && defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value) {
                    Spacer(modifier = Modifier.width(horizontalSpacing))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        UndoOrSellButton(
                            defender = defender,
                            gameState = gameState,
                            onUndoTower = onUndoTower,
                            onSellTower = onSellTower,
                            modifier =
                                Modifier
                                    .width(240.dp)
                                    .height(buttonHeight),
                        )
                    }
                }

                if (defender.isGrippedByKraken.value) {
                    Spacer(modifier = Modifier.width(horizontalSpacing))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(Res.string.villain_kraken_barge_grip_short),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GamePlayColors.ErrorDark,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (defender.isReady) {
                    if (defender.type == DefenderType.DRAGONS_LAIR) {
                        // Dragon's lair - no actions, can't be sold
                        // Show dragon name in normal text if available
                        val dragonAlive =
                            defender.dragonId.value?.let { dragonId ->
                                gameState.attackers.any {
                                    it.id == dragonId && !it.isDefeated.value
                                }
                            } ?: false

                        if (dragonAlive && defender.dragonName != null) {
                            Text(
                                "${stringResource(Res.string.lair_of_the_dragon)} ${defender.dragonName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GamePlayColors.ErrorDark,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Text(
                            stringResource(Res.string.dragons_lair_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        )
                    } else if (defender.type == DefenderType.ALTAR) {
                        if (defender.isChanneling.value) {
                            SelectableText(stringResource(Res.string.altar_channeling))
                        }
                        if (isPlayerTurn && onActivateAltar != null) {
                            Button(
                                onClick = { onActivateAltar(defender.id) },
                                enabled = gameState.canActivateAltar(defender),
                                modifier = Modifier.width(220.dp).height(buttonHeight),
                            ) {
                                Text(stringResource(Res.string.activate_altar))
                            }
                        }
                        if (isPlayerTurn && onWizardAction != null) {
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                            MagicalTrapButton(
                                defender = defender,
                                onWizardAction = onWizardAction,
                                selectedWizardAction = selectedWizardAction,
                                modifier = Modifier.width(220.dp).height(buttonHeight),
                            )
                        }
                        Spacer(modifier = Modifier.width(horizontalSpacing))
                        UndoOrSellButton(
                            defender = defender,
                            gameState = gameState,
                            onUndoTower = onUndoTower,
                            onSellTower = onSellTower,
                            modifier = Modifier.width(220.dp).height(buttonHeight),
                        )
                    } else {
                        // Normal tower stats and buttons
                        // Calculate next level stats using helper functions
                        val currentLevel = defender.level.value
                        val nextLevel = currentLevel + 1
                        val nextActualDamage =
                            if (defender.type == DefenderType.DWARVEN_MINE) {
                                calculateTrapDamage(defender, nextLevel)
                            } else {
                                calculateActualDamage(defender, nextLevel)
                            }
                        val nextRange = calculateRange(defender, nextLevel)
                        val nextActions = calculateActionsPerTurn(defender, nextLevel)

                        // Effective stats considering active spells
                        val effectiveLevel = if (doubleLevelEffect != null) currentLevel * 2 else currentLevel
                        val currentDisplayDamage =
                            if (doubleLevelEffect != null) {
                                if (defender.type == DefenderType.DWARVEN_MINE) {
                                    calculateTrapDamage(defender, effectiveLevel)
                                } else {
                                    calculateActualDamage(defender, effectiveLevel)
                                }
                            } else {
                                if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.actualDamage
                            }
                        val currentDisplayRange =
                            when {
                                doubleReachEffect != null -> defender.range * 2
                                doubleLevelEffect != null -> calculateRange(defender, effectiveLevel)
                                else -> defender.range
                            }
                        val currentDisplayActions =
                            if (doubleLevelEffect != null) {
                                calculateActionsPerTurn(defender, effectiveLevel)
                            } else {
                                defender.actionsPerTurnCalculated
                            }

                        // Determine whether any extra action button columns (attack, mana, trap,
                        // barricade) will be rendered. Used to decide whether to fill the row with
                        // a placeholder spacer or let the upgrade/sell buttons expand naturally.
                        val hasAttackColumn =
                            isPlayerTurn &&
                                defender.type != DefenderType.DWARVEN_MINE &&
                                defender.type != DefenderType.DRAGONS_LAIR &&
                                onDefenderAttack != null &&
                                onDefenderAttackPosition != null
                        val hasManaColumn =
                            isPlayerTurn &&
                                defender.type == DefenderType.WIZARD_TOWER &&
                                gameState.currentMana.value < gameState.maxMana.value &&
                                onWizardAction != null
                        val hasTrapColumn =
                            isPlayerTurn &&
                                defender.type == DefenderType.WIZARD_TOWER &&
                                defender.level.value >= 10 &&
                                onWizardAction != null
                        val hasBarricadeColumn =
                            isPlayerTurn &&
                                onBarricadeAction != null &&
                                when (defender.type) {
                                    DefenderType.SPIKE_TOWER ->
                                        defender.level.value >= 20 &&
                                            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_2
                                    DefenderType.SPEAR_TOWER ->
                                        defender.level.value >= 10 &&
                                            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
                                    else -> false
                                }
                        val hasExtraButtonColumns =
                            hasAttackColumn ||
                                hasManaColumn ||
                                hasTrapColumn ||
                                hasBarricadeColumn ||
                                (defender.type == DefenderType.WIZARD_TOWER && defender.level.value >= 10 && onSanctifyDefender != null)

                        if (defender.type == DefenderType.WIZARD_TOWER &&
                            defender.level.value >= 10 &&
                            onSanctifyDefender != null
                        ) {
                            TooltipWrapper(text = stringResource(Res.string.altar_conversion_info)) {
                                Button(
                                    onClick = { onSanctifyDefender(defender.id) },
                                    enabled = gameState.canSanctifyDefender(defender),
                                    modifier = Modifier.height(buttonHeight),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        RuneCountIcon(size = 20.dp, color = LocalContentColor.current)
                                        Text(stringResource(Res.string.sanctify))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                        }

                        // Current stats column
                        Column(modifier = Modifier.weight(0.5f)) {
                            Text(
                                if (doubleLevelEffect != null) "Lvl $effectiveLevel (×2)" else "Lvl $currentLevel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (doubleLevelEffect != null) SpellDoubleLevelColor else Color.Unspecified,
                            )
                            TowerStats(
                                defender.type.minRange,
                                currentDisplayDamage,
                                currentDisplayRange,
                                currentDisplayActions,
                                rangeColor = if (doubleReachEffect != null) SpellDoubleReachColor else Color.Unspecified,
                            )
                        }

                        // After upgrade stats column
                        Column(modifier = Modifier.weight(0.5f)) {
                            Text(
                                "Lvl $nextLevel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (gameState.canUpgradeDefender(defender)) GamePlayColors.Success else Color.Gray,
                            )
                            TowerStats(
                                defender.type.minRange,
                                nextActualDamage,
                                nextRange,
                                nextActions,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            UpgradeButton(
                                defender,
                                gameState,
                                onUpgradeDefender = onUpgradeDefender,
                                modifier =
                                    Modifier
                                        .width(240.dp)
                                        .height(buttonHeight),
                            )
                        }
                        Spacer(modifier = Modifier.width(horizontalSpacing))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            UndoOrSellButton(
                                defender = defender,
                                gameState = gameState,
                                onUndoTower = onUndoTower,
                                onSellTower = onSellTower,
                                modifier =
                                    Modifier
                                        .width(240.dp)
                                        .height(buttonHeight),
                            )
                        }

                        if (isPlayerTurn &&
                            defender.type != DefenderType.DWARVEN_MINE &&
                            defender.type != DefenderType.DRAGONS_LAIR &&
                            onDefenderAttack != null &&
                            onDefenderAttackPosition != null
                        ) {
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
                                    modifier =
                                        Modifier
                                            .width(240.dp)
                                            .height(buttonHeight),
                                )
                            }
                        }

                        // Generate Mana button for wizard tower (when mana is below max)
                        if (isPlayerTurn &&
                            defender.type == DefenderType.WIZARD_TOWER &&
                            gameState.currentMana.value < gameState.maxMana.value &&
                            onWizardAction != null
                        ) {
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                            Column(modifier = Modifier.weight(1.3f)) {
                                GenerateManaButton(
                                    defender = defender,
                                    gameState = gameState,
                                    onWizardAction = onWizardAction,
                                    modifier =
                                        Modifier
                                            .width(240.dp)
                                            .height(buttonHeight),
                                )
                            }
                        }

                        // Magical trap button for wizard tower level 10+
                        if (isPlayerTurn &&
                            defender.type == DefenderType.WIZARD_TOWER &&
                            defender.level.value >= 10 &&
                            onWizardAction != null
                        ) {
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                            Column(modifier = Modifier.weight(1.3f)) {
                                MagicalTrapButton(
                                    defender = defender,
                                    onWizardAction = onWizardAction,
                                    selectedWizardAction = selectedWizardAction,
                                    modifier =
                                        Modifier
                                            .width(240.dp)
                                            .height(buttonHeight),
                                )
                            }
                        }

                        // Barricade button for spike tower (level 20+) or spear tower (level 10+)
                        // Requires Construction level 1 for spear, level 2 for spike
                        if (isPlayerTurn && onBarricadeAction != null) {
                            val canBuildBarricade =
                                when (defender.type) {
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
                                        modifier =
                                            Modifier
                                                .width(240.dp)
                                                .height(buttonHeight),
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
                            buttonHeight,
                            hasExtraButtonColumns,
                        )
                    }
                }
            }
        }
    }
}
