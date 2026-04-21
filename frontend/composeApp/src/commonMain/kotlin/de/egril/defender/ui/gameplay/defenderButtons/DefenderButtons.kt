package de.egril.defender.ui.gameplay.defenderButtons

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.androidTVModifier
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.SpellInstantTowerColor
import de.egril.defender.ui.gameplay.GamePlayColors
import de.egril.defender.ui.gameplay.GamePlayConstants
import de.egril.defender.ui.gameplay.IconTextRow
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.getLocalizedShortName
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.TargetIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun DefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    coinsState: State<Int>,
    instantTowerActive: Boolean = false,
    onClick: () -> Unit
) {
    val isDarkMode = AppSettings.isDarkMode.value
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
        Button(
            onClick = onClick,
            enabled = actuallyCanAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) GamePlayColors.InfoDark else MaterialTheme.colorScheme.primary,
                contentColor = if (isSelected && isDarkMode) Color.White else Color.White,
                disabledContainerColor = GamePlayColors.DisabledButton,
                disabledContentColor = GamePlayColors.DisabledButtonText
            ),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val bw = maxWidth
                // icon | price | name+buildtime | stats
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TowerTypeIcon(defenderType = type, modifier = Modifier.size(54.dp))
                    if (bw >= 60.dp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        DefenderPriceColumn(
                            cost = type.baseCost,
                            moneyIconSize = 16.dp,
                            priceFontSize = 14.sp,
                            width = 32.dp
                        )
                        if (bw >= 110.dp) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DefenderInfoColumn(
                                type = type, locale = locale,
                                nameFontSize = 12.sp, attackTypeFontSize = 10.sp,
                                timerIconSize = 12.dp, buildTimeFontSize = 10.sp,
                                modifier = Modifier.fillMaxHeight().width(100.dp)
                            )
                            if (bw >= 170.dp) {
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
    modifier: Modifier = Modifier.Companion
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
    modifier: Modifier = Modifier.Companion
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
