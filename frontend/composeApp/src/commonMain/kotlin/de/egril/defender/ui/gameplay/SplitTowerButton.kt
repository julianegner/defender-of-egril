package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.SpellInstantTowerColor
import de.egril.defender.ui.common.SplitButton
import de.egril.defender.ui.common.SplitButtonDefaults
import de.egril.defender.ui.gameplay.defenderButtons.DefenderButton
import de.egril.defender.ui.gameplay.defenderButtons.TowerStats
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.getLocalizedShortName
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.isPlatformMobile

/** Width of the selector (chevron) button on the right side of the split control. */
private val SplitSelectorButtonWidth = SplitButtonDefaults.SelectorButtonWidth

/** Gap between the main tower button and the selector button. */
private val SplitButtonGap = SplitButtonDefaults.Gap

/**
 * Total width of the split tower control (main button + gap + selector button).
 * Used externally to reserve exactly this much horizontal space so the info area
 * beside it can take all remaining width.
 */
val SplitControlMaxWidth = GamePlayConstants.ButtonSizes.DefenderButtonMaxWidth + SplitSelectorButtonWidth + SplitButtonGap

/**
 * A split button combining a tower-build button with a chevron that opens an overlay list of
 * all available tower types, followed by the End Turn / Start Battle button.
 *
 * The tower list opens above the split button as a popup and does not affect the bottom panel height.
 */
@Composable
fun ColumnScope.SplitTowerBuildControls(
    availableTypes: List<DefenderType>,
    selectedDefenderType: DefenderType?,
    coinsState: State<Int>,
    instantTowerActive: Boolean,
    onSelectDefenderType: (DefenderType?) -> Unit,
    isPlayerTurn: Boolean,
    onPrimaryAction: () -> Unit,
    highlightEndTurnButton: Boolean,
    autoAttackAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    if (availableTypes.isEmpty()) {
        TurnButton(
            isPlayerTurn = isPlayerTurn,
            modifier = Modifier.fillMaxWidth(),
            onPrimaryAction = onPrimaryAction,
            highlighted = highlightEndTurnButton,
            autoAttackAvailable = autoAttackAvailable,
        )
        return
    }

    var preferredType by remember(availableTypes) { mutableStateOf(availableTypes.first()) }
    var selectorExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDefenderType, availableTypes) {
        if (selectedDefenderType != null && availableTypes.contains(selectedDefenderType)) {
            preferredType = selectedDefenderType
        } else if (!availableTypes.contains(preferredType)) {
            preferredType = availableTypes.first()
        }
    }

    val selectedType = if (availableTypes.contains(preferredType)) preferredType else availableTypes.first()
    val selectedIndex = availableTypes.indexOf(selectedType)
    val selectedCanAfford = coinsState.value >= selectedType.baseCost
    val splitButtonHeight = if (isPlatformMobile || isMobileWebBrowser()) 80.dp else 70.dp
    val locale = com.hyperether.resources.currentLanguage.value

    Column(
        modifier = modifier.widthIn(max = SplitControlMaxWidth),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SplitButton(
                expanded = selectorExpanded,
                onExpandedChange = { selectorExpanded = it },
                itemCount = availableTypes.size,
                buttonHeight = splitButtonHeight,
                enabled = availableTypes.size > 1,
                modifier = Modifier.fillMaxWidth(),
                dropdownContent = {
                    availableTypes.forEachIndexed { index, type ->
                        SplitTowerListItem(
                            type = type,
                            index = index,
                            selected = selectedType == type,
                            affordable = coinsState.value >= type.baseCost,
                            locale = locale,
                            shortcutIndex = index,
                            onClick = {
                                preferredType = type
                                selectorExpanded = false
                            },
                        )
                    }
                },
            ) {
                DefenderButton(
                    type = selectedType,
                    isSelected = selectedDefenderType == selectedType,
                    canAfford = selectedCanAfford,
                    coinsState = coinsState,
                    instantTowerActive = false,
                    shortcutIndex = selectedIndex.takeIf { it >= 0 },
                    shape = SplitButtonDefaults.MainButtonShape,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectDefenderType(
                            if (selectedDefenderType == selectedType) null else selectedType,
                        )
                    },
                )
            }

            if (instantTowerActive && selectedCanAfford) {
                InstantTowerSpellAnimation(
                    animate = AppSettings.enableAnimations.value,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .border(2.dp, SpellInstantTowerColor, RoundedCornerShape(percent = 50)),
                )
            }
        }

        TurnButton(
            isPlayerTurn = isPlayerTurn,
            modifier = Modifier.fillMaxWidth().height(splitButtonHeight),
            onPrimaryAction = onPrimaryAction,
            highlighted = highlightEndTurnButton,
            autoAttackAvailable = autoAttackAvailable,
        )
    }
}

@Composable
private fun SplitTowerListItem(
    type: DefenderType,
    index: Int,
    selected: Boolean,
    affordable: Boolean,
    locale: com.hyperether.resources.AppLocale,
    shortcutIndex: Int,
    onClick: () -> Unit,
) {
    val baseBackgroundColor =
        if (index % 2 == 0) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    val backgroundColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else baseBackgroundColor
    val baseContentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val contentColor = if (affordable) baseContentColor else baseContentColor.copy(alpha = 0.55f)
    val infoColor =
        if (affordable) {
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            contentColor.copy(alpha = 0.55f)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(backgroundColor, RoundedCornerShape(0.dp))
                .clickable(enabled = affordable, onClick = onClick)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TowerTypeIcon(defenderType = type, modifier = Modifier.size(46.dp))
        Column(
            modifier = Modifier.width(36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoneyIcon(size = 14.dp)
            Text(
                text = type.baseCost.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = type.getLocalizedShortName(locale),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "#${shortcutIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 1,
            )
            Text(
                text = type.attackType.getLocalizedName(locale),
                style = MaterialTheme.typography.labelSmall,
                color = infoColor,
                maxLines = 1,
            )
            if (type.buildTime > 0) {
                Text(
                    text = "${type.buildTime}T",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
        // Fixed width so stats always start at the same horizontal position across all rows
        Box(modifier = Modifier.width(40.dp)) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                TowerStats(
                    minRange = type.minRange,
                    damage = type.baseDamage,
                    range = type.baseRange,
                    actionsPerTurn = type.actionsPerTurn,
                    rangeColor = contentColor,
                )
            }
        }
    }
}
