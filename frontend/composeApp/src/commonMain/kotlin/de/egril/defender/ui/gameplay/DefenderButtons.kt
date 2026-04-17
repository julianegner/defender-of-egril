package de.egril.defender.ui.gameplay

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.SpellInstantTowerColor
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.TargetIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Holds all size and spacing constants for one tier of the responsive [DefenderButton] layout.
 *
 * Three pre-built instances are provided in the companion object:
 * - [Wide]   – used when the button cell is ≥ 130 dp (desktop layout with ample space)
 * - [Medium] – used when the button cell is ≥ 60 dp  (tablet / mobile grid)
 * - [Narrow] – fallback for very small cells (< 60 dp)
 */
private data class DefenderButtonConfig(
    // Icon
    val iconSize: Dp,
    val iconBoxSize: Dp,       // outer Box wrapper size (equals iconSize for Medium/Narrow)
    val iconSpacing: Dp,       // spacer between the icon and the next element

    // Price column
    val moneyIconSize: Dp,
    val priceFontSize: TextUnit,
    val priceSpacing: Dp,      // spacer between the price column and the text column

    // Name / attack-type labels
    val nameFontSize: TextUnit,
    val attackTypeFontSize: TextUnit,

    // Build-time row (Wide and Medium 2-row)
    val timerIconSize: Dp,
    val buildTimeFontSize: TextUnit,

    // Inline stats row (Medium and Wide)
    val statsIconSize: Dp,
    val statsInnerSpacing: Dp,     // spacer between a stat icon and its value text
    val statsBetweenSpacing: Dp,   // spacer between the damage and range pairs
    val statsFontSize: TextUnit,

    // Separate stats column (Wide only)
    val statsColumnPaddingStart: Dp
) {
    companion object {
        /** Desktop / large-window layout – 60 dp icon, prominent 16 sp price, full stats column. */
        val Wide = DefenderButtonConfig(
            iconSize = 56.dp, iconBoxSize = 60.dp, iconSpacing = 4.dp,
            moneyIconSize = 16.dp, priceFontSize = 16.sp, priceSpacing = 4.dp,
            nameFontSize = 12.sp, attackTypeFontSize = 10.sp,
            timerIconSize = 15.dp, buildTimeFontSize = 10.sp,
            statsIconSize = 12.dp, statsInnerSpacing = 1.dp, statsBetweenSpacing = 4.dp, statsFontSize = 10.sp,
            statsColumnPaddingStart = 4.dp
        )

        /** Tablet / mobile layout – 28 dp icon, 13 sp price, 2-row layout when tall enough. */
        val Medium = DefenderButtonConfig(
            iconSize = 28.dp, iconBoxSize = 28.dp, iconSpacing = 2.dp,
            moneyIconSize = 12.dp, priceFontSize = 13.sp, priceSpacing = 4.dp,
            nameFontSize = 10.sp, attackTypeFontSize = 9.sp,
            timerIconSize = 10.dp, buildTimeFontSize = 9.sp,
            statsIconSize = 10.dp, statsInnerSpacing = 1.dp, statsBetweenSpacing = 4.dp, statsFontSize = 9.sp,
            statsColumnPaddingStart = 0.dp
        )

        /** Narrow fallback – icon + name + price stacked vertically. */
        val Narrow = DefenderButtonConfig(
            iconSize = 22.dp, iconBoxSize = 22.dp, iconSpacing = 0.dp,
            moneyIconSize = 10.dp, priceFontSize = 11.sp, priceSpacing = 2.dp,
            nameFontSize = 8.sp, attackTypeFontSize = 0.sp,
            timerIconSize = 0.dp, buildTimeFontSize = 0.sp,
            statsIconSize = 0.dp, statsInnerSpacing = 0.dp, statsBetweenSpacing = 0.dp, statsFontSize = 0.sp,
            statsColumnPaddingStart = 0.dp
        )
    }
}

// ─── Shared private helpers ───────────────────────────────────────────────────

/** Tower icon centred inside a sized box (Wide uses iconBoxSize > iconSize; others equal). */
@Composable
private fun TowerButtonIcon(type: DefenderType, cfg: DefenderButtonConfig) {
    Box(
        modifier = Modifier.size(cfg.iconBoxSize),
        contentAlignment = Alignment.Center
    ) {
        TowerTypeIcon(defenderType = type, modifier = Modifier.size(cfg.iconSize))
    }
}

/** Money icon + cost text as a horizontal row (compact, used inside a single content row). */
@Composable
private fun TowerButtonPriceRow(type: DefenderType, cfg: DefenderButtonConfig) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MoneyIcon(size = cfg.moneyIconSize)
        Text(
            "${type.baseCost}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = cfg.priceFontSize
        )
    }
}

/** Build-time row (timer icon + "NT"). Renders nothing when timerIconSize == 0.dp. */
@Composable
private fun TowerButtonBuildTime(type: DefenderType, cfg: DefenderButtonConfig) {
    if (cfg.timerIconSize > 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimerIcon(size = cfg.timerIconSize)
            Text(
                "${type.buildTime}T",
                style = MaterialTheme.typography.labelSmall,
                fontSize = cfg.buildTimeFontSize
            )
        }
    }
}

/** Damage + range inline row. Renders nothing when statsIconSize == 0.dp. */
@Composable
private fun TowerButtonInlineStats(type: DefenderType, cfg: DefenderButtonConfig) {
    if (cfg.statsIconSize > 0.dp) {
        val rangeText = if (type.minRange > 0) "${type.minRange}-${type.baseRange}" else "${type.baseRange}"
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExplosionIcon(size = cfg.statsIconSize)
            Spacer(modifier = Modifier.width(cfg.statsInnerSpacing))
            Text("${type.baseDamage}", style = MaterialTheme.typography.labelSmall, fontSize = cfg.statsFontSize)
            Spacer(modifier = Modifier.width(cfg.statsBetweenSpacing))
            TargetIcon(size = cfg.statsIconSize)
            Spacer(modifier = Modifier.width(cfg.statsInnerSpacing))
            Text(rangeText, style = MaterialTheme.typography.labelSmall, fontSize = cfg.statsFontSize)
        }
    }
}


@Composable
fun CompactDefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    instantTowerActive: Boolean = false,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    val locale = com.hyperether.resources.currentLanguage.value
    
    // Create accessible content description
    val towerName = type.getLocalizedShortName(locale)
    val description = "$towerName, ${stringResource(Res.string.coins_label)}: ${type.baseCost}" +
        if (isSelected) ", ${stringResource(Res.string.selected)}" else ""
    
    // Apply Android TV modifiers for accessibility and focus
    val buttonModifier = modifier.androidTVModifier(
        isSelected = isSelected,
        description = description
    )

    Box(modifier = buttonModifier) {
        Button(
            onClick = onClick,
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) GamePlayColors.InfoDark else MaterialTheme.colorScheme.primary,
                contentColor = if (isSelected && isDarkMode) Color.White else Color.White,  // Brighter text when selected in dark mode
                disabledContainerColor = GamePlayColors.DisabledButton,
                disabledContentColor = GamePlayColors.DisabledButtonText
            ),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Tower icon
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TowerTypeIcon(defenderType = type, modifier = Modifier.size(30.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                val locale = com.hyperether.resources.currentLanguage.value
                Text(
                    type.getLocalizedShortName(locale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (isSelected && isDarkMode) Color.White else Color.White  // Ensure bright text
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Cost
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${type.baseCost}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        // Show glow animation overlay + purple border when Instant Tower spell is active and tower is affordable
        if (instantTowerActive && canAfford) {
            InstantTowerSpellAnimation(
                animate = AppSettings.enableAnimations.value,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .border(2.dp, SpellInstantTowerColor, RoundedCornerShape(percent = 50))
            )
        }
    }
}

@Composable
fun DefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    coinsState: State<Int>,  // Accept State instead of Int
    instantTowerActive: Boolean = false,
    onClick: () -> Unit
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    val locale = com.hyperether.resources.currentLanguage.value
    // Recalculate canAfford based on current coins.value to ensure reactivity
    val actuallyCanAfford = coinsState.value >= type.baseCost

    // Create accessible content description
    val towerName = type.getLocalizedName(locale)
    val attackTypeName = type.attackType.getLocalizedName(locale)
    val description = "$towerName, $attackTypeName, " +
        "${stringResource(Res.string.damage)}: ${type.baseDamage}, " +
        "${stringResource(Res.string.range)}: ${type.baseRange}, " +
        "${stringResource(Res.string.coins_label)}: ${type.baseCost}" +
        if (isSelected) ", ${stringResource(Res.string.selected)}" else ""

    // Apply Android TV modifiers for accessibility and focus
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(70.dp)
        .androidTVModifier(
            isSelected = isSelected,
            description = description
        )

    Box(modifier = buttonModifier) {
        Button(
            onClick = onClick,
            enabled = actuallyCanAfford,  // Use recalculated value
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) GamePlayColors.InfoDark else MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = GamePlayColors.DisabledButton,
                disabledContentColor = GamePlayColors.DisabledButtonText
            ),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cfg = when {
                    maxWidth >= 130.dp -> DefenderButtonConfig.Wide
                    maxWidth >= 60.dp  -> DefenderButtonConfig.Medium
                    else               -> DefenderButtonConfig.Narrow
                }
                // Use 2-row Medium layout only when there is enough vertical space
                val use2RowMedium = cfg == DefenderButtonConfig.Medium && maxHeight >= 50.dp

                when {
                    cfg == DefenderButtonConfig.Wide -> {
                        // Wide layout (desktop): icon box → name/type/buildtime column → price column → stats column
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TowerButtonIcon(type, cfg)
                            Spacer(modifier = Modifier.width(cfg.iconSpacing))
                            Column(
                                modifier = Modifier.fillMaxHeight().weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    type.getLocalizedShortName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = cfg.nameFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    type.attackType.getLocalizedName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = cfg.attackTypeFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = GamePlayColors.Yellow
                                )
                                TowerButtonBuildTime(type, cfg)
                            }
                            Spacer(modifier = Modifier.width(cfg.priceSpacing))
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MoneyIcon(size = cfg.moneyIconSize)
                                Text(
                                    "${type.baseCost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = cfg.priceFontSize
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxHeight().padding(start = cfg.statsColumnPaddingStart),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                TowerStats(type.minRange, type.baseDamage, type.baseRange, type.actionsPerTurn)
                            }
                        }
                    }
                    use2RowMedium -> {
                        // Medium 2-row layout (tablet / single-row mobile grid): all info visible, no truncation
                        // Row 1: icon → name (flex) → price
                        // Row 2: (indented) attack type → stats → build time
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                TowerButtonIcon(type, cfg)
                                Spacer(modifier = Modifier.width(cfg.iconSpacing))
                                Text(
                                    type.getLocalizedShortName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = cfg.nameFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(cfg.priceSpacing))
                                TowerButtonPriceRow(type, cfg)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = cfg.iconBoxSize + cfg.iconSpacing),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    type.attackType.getLocalizedName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = cfg.attackTypeFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = GamePlayColors.Yellow,
                                    modifier = Modifier.weight(1f)
                                )
                                TowerButtonInlineStats(type, cfg)
                                Spacer(modifier = Modifier.width(cfg.statsBetweenSpacing))
                                TowerButtonBuildTime(type, cfg)
                            }
                        }
                    }
                    cfg == DefenderButtonConfig.Medium -> {
                        // Medium single-row layout (compact cells, e.g. 2-row grid on small phones)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TowerButtonIcon(type, cfg)
                            Spacer(modifier = Modifier.width(cfg.iconSpacing))
                            Column(
                                modifier = Modifier.fillMaxHeight().weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    type.getLocalizedShortName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = cfg.nameFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    type.attackType.getLocalizedName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = cfg.attackTypeFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = GamePlayColors.Yellow
                                )
                                TowerButtonInlineStats(type, cfg)
                            }
                            Spacer(modifier = Modifier.width(cfg.priceSpacing))
                            TowerButtonPriceRow(type, cfg)
                        }
                    }
                    else -> {
                        // Narrow layout (very small cells): icon + name + cost stacked vertically
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TowerButtonIcon(type, cfg)
                            Text(
                                type.getLocalizedShortName(locale),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = cfg.nameFontSize,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyIcon(size = cfg.moneyIconSize)
                                Spacer(modifier = Modifier.width(cfg.priceSpacing))
                                Text(
                                    "${type.baseCost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = cfg.priceFontSize
                                )
                            }
                        }
                    }
                }
            }
        }
        // Show glow animation overlay + purple border when Instant Tower spell is active and tower is affordable
        if (instantTowerActive && actuallyCanAfford) {
            InstantTowerSpellAnimation(
                animate = AppSettings.enableAnimations.value,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .border(2.dp, SpellInstantTowerColor, RoundedCornerShape(percent = 50))
            )
        }
    }
}

@Composable
fun TowerStats(minRange: Int, damage: Int, range: Int, actionsPerTurn: Int, rangeColor: Color = Color.Unspecified) {
    Column {
        IconTextRow(
            icon = { size -> ExplosionIcon(size = size) },
            text = damage.toString(),
            iconSize = GamePlayConstants.IconSizes.Small,
            spacerWidth = GamePlayConstants.Spacing.IconText
        )
        
        val rangeText = if (minRange > 0) "$minRange-$range" else range.toString()
        Row(verticalAlignment = Alignment.CenterVertically) {
            TargetIcon(size = GamePlayConstants.IconSizes.Small)
            Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
            Text(rangeText, style = MaterialTheme.typography.bodySmall, color = rangeColor)
        }
        
        IconTextRow(
            icon = { size -> LightningIcon(size = size) },
            text = actionsPerTurn.toString(),
            iconSize = GamePlayConstants.IconSizes.Small,
            spacerWidth = GamePlayConstants.Spacing.IconText
        )
    }
}
