package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.GameState
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.barricade
import defender_of_egril.composeapp.generated.resources.dwarven_trap
import defender_of_egril.composeapp.generated.resources.magical_trap
import org.jetbrains.compose.resources.stringResource

/** Colors used for the support boxes (objects vs spell tokens). */
private object SupportBarColors {
    val ObjectBorder = Color(0xFF795548) // Brown - objects use solid, square boxes
    val SpellBorder = Color(0xFF7E57C2) // Purple - spell tokens use dashed, rounded boxes
    val Selected = Color(0xFFFFC107) // Amber highlight for the active selection
}

private val SUPPORT_BOX_SIZE = 56.dp
private val SUPPORT_ICON_SIZE = 32.dp

/**
 * Row of support boxes shown at the lower edge of the screen, above the tower buttons.
 *
 * Objects (traps, magical traps, barricades) are drawn as square boxes with a solid border,
 * while spell tokens are drawn as rounded boxes with a dashed border. When more than one of a
 * support is available, its remaining count is shown in the upper-right corner of the box.
 *
 * Clicking an object box enters placement mode; clicking a spell token box starts casting the
 * spell (which does not consume mana).
 */
@Composable
fun SupportBar(
    gameState: GameState,
    selectedSupportObject: SupportObjectType?,
    activeSpellToken: SpellType?,
    enabled: Boolean,
    onObjectClick: (SupportObjectType) -> Unit,
    onSpellClick: (SpellType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val supports = gameState.level.supports
    if (supports.isEmpty()) return

    Row(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Placable objects
        supports.objects.forEach { supportObject ->
            val remaining = gameState.supportObjectsRemaining[supportObject.type] ?: 0
            SupportBox(
                remaining = remaining,
                isSpell = false,
                isSelected = selectedSupportObject == supportObject.type,
                enabled = enabled && remaining > 0,
                tooltip = supportObject.type.localizedSupportName(),
                onClick = { onObjectClick(supportObject.type) },
            ) {
                SupportObjectIcon(supportObject.type, SUPPORT_ICON_SIZE)
            }
        }

        // Spell tokens
        supports.spells.forEach { supportSpell ->
            val remaining = gameState.supportSpellsRemaining[supportSpell.spell] ?: 0
            SupportBox(
                remaining = remaining,
                isSpell = true,
                isSelected = activeSpellToken == supportSpell.spell,
                enabled = enabled && remaining > 0,
                tooltip = supportSpell.spell.getLocalizedName(),
                onClick = { onSpellClick(supportSpell.spell) },
            ) {
                SpellTargetIcon(spell = supportSpell.spell, size = SUPPORT_ICON_SIZE)
            }
        }
    }
}

@Composable
private fun SupportBox(
    remaining: Int,
    isSpell: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    tooltip: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val borderColor =
        when {
            isSelected -> SupportBarColors.Selected
            isSpell -> SupportBarColors.SpellBorder
            else -> SupportBarColors.ObjectBorder
        }
    val alpha = if (enabled) 1f else 0.4f
    val shape = if (isSpell) RoundedCornerShape(14.dp) else RoundedCornerShape(0.dp)

    TooltipWrapper(text = tooltip, preferAbove = true) {
        Box(
            modifier =
                Modifier
                    .size(SUPPORT_BOX_SIZE)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * alpha))
                    .then(
                        if (isSpell) {
                            // Spell tokens use a dashed border
                            Modifier.drawBehind {
                                val strokeWidth = if (isSelected) 3.dp.toPx() else 2.dp.toPx()
                                drawRoundRect(
                                    color = borderColor.copy(alpha = alpha),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                    style =
                                        Stroke(
                                            width = strokeWidth,
                                            pathEffect =
                                                PathEffect.dashPathEffect(
                                                    floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                                                    0f,
                                                ),
                                        ),
                                )
                            }
                        } else {
                            // Objects use a solid border
                            Modifier.border(
                                width = if (isSelected) 3.dp else 2.dp,
                                color = borderColor.copy(alpha = alpha),
                                shape = shape,
                            )
                        },
                    ).clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()

            // Count badge in the upper-right corner when more than one is available
            if (remaining > 1) {
                SupportCountBadge(
                    count = remaining,
                    color = borderColor,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun SupportCountBadge(
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SupportObjectIcon(
    type: SupportObjectType,
    size: Dp,
) {
    when (type) {
        SupportObjectType.DWARVEN_TRAP -> TrapIcon(size = size)
        SupportObjectType.MAGICAL_TRAP -> PentagramIcon(size = size)
        SupportObjectType.BARRICADE -> WoodIcon(size = size)
    }
}

/**
 * Reusable summary of a level's available supports (objects + spell tokens).
 * Values are inserted from the level's [de.egril.defender.model.LevelSupports] so the composable
 * can be reused anywhere the available supports need to be presented (e.g. the story message).
 */
@Composable
fun LevelSupportsSummary(
    supports: de.egril.defender.model.LevelSupports,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    if (supports.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
            )
        }
        supports.objects.forEach { supportObject ->
            SupportSummaryRow(
                count = supportObject.count,
                label = supportObject.type.localizedSupportName(),
            ) {
                SupportObjectIcon(supportObject.type, 24.dp)
            }
        }
        supports.spells.forEach { supportSpell ->
            SupportSummaryRow(
                count = supportSpell.count,
                label = supportSpell.spell.getLocalizedName(),
            ) {
                SpellTargetIcon(spell = supportSpell.spell, size = 24.dp)
            }
        }
    }
}

@Composable
private fun SupportSummaryRow(
    count: Int,
    label: String,
    icon: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = if (count > 1) "$label ×$count" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF333333),
        )
    }
}

/** Localized display name for a support object type. */
@Composable
fun SupportObjectType.localizedSupportName(): String =
    when (this) {
        SupportObjectType.DWARVEN_TRAP -> stringResource(Res.string.dwarven_trap)
        SupportObjectType.MAGICAL_TRAP -> stringResource(Res.string.magical_trap)
        SupportObjectType.BARRICADE -> stringResource(Res.string.barricade)
    }
