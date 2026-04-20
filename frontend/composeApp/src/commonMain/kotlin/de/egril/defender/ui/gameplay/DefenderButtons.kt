package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.SpellInstantTowerColor
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.TargetIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource



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
    coinsState: State<Int>,
    instantTowerActive: Boolean = false,
    onClick: () -> Unit
) {
    val locale = com.hyperether.resources.currentLanguage.value
    val actuallyCanAfford = coinsState.value >= type.baseCost

    val towerName = type.getLocalizedName(locale)
    val attackTypeName = type.attackType.getLocalizedName(locale)
    val description = "$towerName, $attackTypeName, " +
        "${stringResource(Res.string.damage)}: ${type.baseDamage}, " +
        "${stringResource(Res.string.range)}: ${type.baseRange}, " +
        "${stringResource(Res.string.coins_label)}: ${type.baseCost}" +
        if (isSelected) ", ${stringResource(Res.string.selected)}" else ""

    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(70.dp)
        .androidTVModifier(isSelected = isSelected, description = description)

    Box(modifier = buttonModifier) {
        // Stone slab background – edges of the visible image act as the button border
        Image(
            painter = painterResource(Res.drawable.stone_slab_wide),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Clickable content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = actuallyCanAfford, onClick = onClick)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val bw = maxWidth
                when {
                    bw >= 170.dp -> {
                        // Full layout: icon | price | name+buildtime | stats
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TowerIconOnHexagon(defenderType = type, size = 54.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            DefenderPriceColumn(cost = type.baseCost, moneyIconSize = 16.dp, priceFontSize = 14.sp, width = 32.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            DefenderInfoColumn(
                                type = type, locale = locale,
                                nameFontSize = 12.sp, attackTypeFontSize = 10.sp,
                                timerIconSize = 12.dp, buildTimeFontSize = 10.sp,
                                modifier = Modifier.fillMaxHeight().weight(1f)
                            )
                            // Column 4: stats
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 4.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                TowerStats(type.minRange, type.baseDamage, type.baseRange, type.actionsPerTurn)
                            }
                        }
                    }
                    bw >= 110.dp -> {
                        // Middle layout: icon | price | name+buildtime
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TowerIconOnHexagon(defenderType = type, size = 46.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            DefenderPriceColumn(cost = type.baseCost, moneyIconSize = 13.dp, priceFontSize = 12.sp, width = 28.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            DefenderInfoColumn(
                                type = type, locale = locale,
                                nameFontSize = 10.sp, attackTypeFontSize = 9.sp,
                                timerIconSize = 10.dp, buildTimeFontSize = 9.sp,
                                modifier = Modifier.fillMaxHeight().weight(1f)
                            )
                        }
                    }
                    bw >= 60.dp -> {
                        // Lesser layout: icon | price
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TowerIconOnHexagon(defenderType = type, size = 40.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            DefenderPriceColumn(
                                cost = type.baseCost, moneyIconSize = 12.dp, priceFontSize = 11.sp,
                                modifier = Modifier.fillMaxHeight().weight(1f)
                            )
                        }
                    }
                    else -> {
                        // Minimum layout: icon only
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TowerIconOnHexagon(defenderType = type, size = 50.dp)
                        }
                    }
                }
            }
        }

        // Selected overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x552196F3))
            )
        }

        // Not affordable overlay
        if (!actuallyCanAfford) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
            )
        }

        // Show glow animation overlay + purple border when Instant Tower spell is active and tower is affordable
        if (instantTowerActive && actuallyCanAfford) {
            InstantTowerSpellAnimation(
                animate = AppSettings.enableAnimations.value,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, SpellInstantTowerColor, RoundedCornerShape(percent = 50))
            )
        }
    }
}

@Composable
private fun DefenderPriceColumn(
    cost: Int,
    moneyIconSize: Dp,
    priceFontSize: TextUnit,
    width: Dp? = null,
    modifier: Modifier = Modifier
) {
    val columnModifier = if (width != null) modifier.width(width) else modifier
    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MoneyIcon(size = moneyIconSize)
        Text(
            "$cost",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = priceFontSize,
            color = Color.White
        )
    }
}

@Composable
private fun DefenderInfoColumn(
    type: DefenderType,
    locale: com.hyperether.resources.AppLocale,
    nameFontSize: TextUnit,
    attackTypeFontSize: TextUnit,
    timerIconSize: Dp,
    buildTimeFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            type.getLocalizedShortName(locale),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = nameFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        Text(
            type.attackType.getLocalizedName(locale),
            style = MaterialTheme.typography.labelSmall,
            fontSize = attackTypeFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = GamePlayColors.Yellow
        )
        if (type.buildTime > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimerIcon(size = timerIconSize)
                Text(
                    "${type.buildTime}T",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = buildTimeFontSize,
                    color = Color.White
                )
            }
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
