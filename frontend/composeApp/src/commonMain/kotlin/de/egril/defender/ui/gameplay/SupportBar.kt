package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.Attacker
import de.egril.defender.model.CooldownPowerType
import de.egril.defender.model.Defender
import de.egril.defender.model.FiefType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.SpellTargetType
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.isIndefiniteSupportCount
import de.egril.defender.model.supportCountDisplayText
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.HammerIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.fief_fisher_hut
import defender_of_egril.composeapp.generated.resources.fief_marketplace
import defender_of_egril.composeapp.generated.resources.fief_quarry
import defender_of_egril.composeapp.generated.resources.fief_woodcutter
import org.jetbrains.compose.resources.painterResource

/** Colors used for the support boxes (objects vs spell tokens vs cooldown powers). */
private object SupportBarColors {
    val ObjectBorder = Color(0xFF795548) // Brown - objects use solid, square boxes
    val SpellBorder = Color(0xFF7E57C2) // Purple - spell tokens use dashed, rounded boxes
    val CooldownBorder = Color(0xFF00897B) // Teal - cooldown powers use solid, rounded boxes
    val Selected = Color(0xFFFFC107) // Amber highlight for the active selection
    val Focus = Color(0xFFFFFFFF) // White ring for the keyboard-navigation cursor
}

private val SUPPORT_BOX_SIZE = 56.dp
private val SUPPORT_ICON_SIZE = 32.dp

// Space reserved around every box for the keyboard-focus ring, so the row layout stays stable
// whether or not a box is currently focused.
private val SUPPORT_FOCUS_RING_PADDING = 3.dp

/**
 * A support box shown in the [SupportBar]. Objects and spell tokens are only present while at least
 * one is remaining; cooldown powers are always present (even while recharging). [visibleSupportSlots]
 * lists them in on-screen order — objects, then spell tokens, then cooldown powers — so the list
 * index can be used to drive keyboard navigation (left/right to move the focus cursor, a select key
 * to activate the focused box).
 */
sealed interface SupportSlot {
    data class ObjectSlot(
        val type: SupportObjectType,
    ) : SupportSlot

    data class SpellSlot(
        val spell: SpellType,
    ) : SupportSlot

    data class PowerSlot(
        val type: CooldownPowerType,
    ) : SupportSlot

    data class FiefSlot(
        val type: de.egril.defender.model.FiefType,
    ) : SupportSlot
}

/**
 * The ordered list of support boxes currently visible in the [SupportBar] for [gameState].
 * Used to assign — and resolve — the sequential keyboard shortcuts shown on the boxes.
 */
fun visibleSupportSlots(gameState: GameState): List<SupportSlot> {
    val slots = mutableListOf<SupportSlot>()
    displayedSupportObjectTypes(gameState).forEach { slots.add(SupportSlot.ObjectSlot(it)) }
    displayedSupportSpellTypes(gameState).forEach { slots.add(SupportSlot.SpellSlot(it)) }
    displayedSupportFiefTypes(gameState).forEach { slots.add(SupportSlot.FiefSlot(it)) }
    gameState.level.supports.cooldownPowers.forEach { power ->
        slots.add(SupportSlot.PowerSlot(power.type))
    }
    return slots
}

/**
 * Ordered support-object types to show: those declared for the level, followed by any granted at
 * runtime by a scripted event (see [de.egril.defender.game.EventScriptSystem]). Only types with at
 * least one use remaining are kept.
 */
fun displayedSupportObjectTypes(gameState: GameState): List<SupportObjectType> {
    val declared =
        gameState.level.supports.objects
            .map { it.type }
    val extra =
        gameState.supportObjectsRemaining.keys
            .filter { it !in declared }
            .sortedBy { it.ordinal }
    return (declared + extra).filter { (gameState.supportObjectsRemaining[it] ?: 0) > 0 }
}

/**
 * Ordered support-spell types to show: those declared for the level, followed by any granted at
 * runtime by a scripted event. Only types with at least one use remaining are kept.
 */
fun displayedSupportSpellTypes(gameState: GameState): List<SpellType> {
    val declared =
        gameState.level.supports.spells
            .map { it.spell }
    val extra =
        gameState.supportSpellsRemaining.keys
            .filter { it !in declared }
            .sortedBy { it.ordinal }
    return (declared + extra).filter { (gameState.supportSpellsRemaining[it] ?: 0) > 0 }
}

/**
 * Ordered fief types to show as support tokens: those declared for the level that still have
 * at least one placement remaining.
 */
fun displayedSupportFiefTypes(gameState: GameState): List<de.egril.defender.model.FiefType> {
    val declared =
        gameState.level.supports.fiefs
            .map { it.type }
    return declared.filter { (gameState.supportFiefRemaining[it] ?: 0) > 0 }
}

/**
 * Whether the support box for [slot] can currently be used. Mirrors the per-box enable logic used
 * when rendering the [SupportBar] so that keyboard navigation behaves exactly like clicking the box.
 *
 * [barEnabled] is true while the player may act (player turn or initial building phase).
 */
fun isSupportSlotEnabled(
    gameState: GameState,
    slot: SupportSlot,
    barEnabled: Boolean,
): Boolean {
    val isInitialBuilding = gameState.phase.value == GamePhase.INITIAL_BUILDING
    val powersEnabled = barEnabled && !isInitialBuilding
    val healthAtMax = gameState.healthPoints.value >= gameState.level.healthPoints
    val manaAtMax = gameState.currentMana.value >= gameState.maxMana.value
    return when (slot) {
        is SupportSlot.ObjectSlot -> barEnabled
        is SupportSlot.FiefSlot -> barEnabled
        is SupportSlot.SpellSlot -> {
            val atMaxEffect = slot.spell == SpellType.HEAL && healthAtMax
            powersEnabled && !atMaxEffect
        }
        is SupportSlot.PowerSlot -> {
            val readyIn = gameState.cooldownPowerReadyIn[slot.type] ?: 0
            val atMaxEffect = slot.type.addsMana && manaAtMax
            powersEnabled && readyIn == 0 && !atMaxEffect
        }
    }
}

/**
 * Next position of the support-bar keyboard-focus cursor over [slotCount] visible boxes.
 *
 * [current] is the current cursor index, or null when the bar is not being navigated yet — the
 * first move then lands on the near end (0 when moving [forward], the last box when moving
 * backwards). Subsequent moves wrap around: moving forward from the last box returns to index 0,
 * and moving backward from index 0 returns to the last box. Returns null when there are no boxes.
 */
fun nextSupportFocusIndex(
    current: Int?,
    slotCount: Int,
    forward: Boolean,
): Int? {
    if (slotCount <= 0) return null
    return when {
        current == null -> if (forward) 0 else slotCount - 1
        forward -> (current + 1) % slotCount
        else -> (current - 1 + slotCount) % slotCount
    }
}

/**
 * The ordered list of tiles on which the support object [type] may currently be placed, mirroring
 * the validation used by the game engine when placing support-granted traps and barricades. Used to
 * drive keyboard placement: a cursor cycles through these tiles and the selected one is confirmed
 * with the place key.
 *
 * Tiles are ordered top-to-bottom, left-to-right so the keyboard cursor moves predictably.
 */
fun supportObjectPlacementTiles(
    gameState: GameState,
    type: SupportObjectType,
): List<Position> {
    // Pre-compute occupied positions once so the per-tile checks below are O(1) lookups instead of
    // scanning every attacker/trap/barricade/defender for each grid cell.
    val attackerPositions =
        gameState.attackers
            .filter { !it.isDefeated.value }
            .map { it.position.value }
            .toHashSet()
    val trapPositions = gameState.traps.map { it.position }.toHashSet()
    val barricadePositions = gameState.barricades.map { it.position }.toHashSet()
    val fieldEffectPositions = gameState.fieldEffects.map { it.position }.toHashSet()
    val defenderNotOnTowerBasePositions =
        gameState.defenders
            .filter { it.towerBaseBarricadeId.value == null }
            .map { it.position.value }
            .toHashSet()

    val tiles = mutableListOf<Position>()
    for (y in 0 until gameState.level.gridHeight) {
        for (x in 0 until gameState.level.gridWidth) {
            val pos = Position(x, y)
            if (!gameState.level.isOnPath(pos)) continue
            if (pos in attackerPositions || pos in trapPositions) continue
            val valid =
                when (type) {
                    SupportObjectType.DWARVEN_TRAP, SupportObjectType.MAGICAL_TRAP ->
                        pos !in barricadePositions && pos !in fieldEffectPositions
                    SupportObjectType.BARRICADE ->
                        pos !in defenderNotOnTowerBasePositions
                }
            if (valid) tiles.add(pos)
        }
    }
    return tiles
}

/**
 * The ordered list of tiles on which a fief support token may currently be placed. Fiefs require
 * an empty path tile (no attacker, trap, barricade, or existing fief). Ordered top-to-bottom,
 * left-to-right so keyboard placement moves predictably.
 */
fun supportFiefPlacementTiles(gameState: GameState): List<Position> {
    val attackerPositions =
        gameState.attackers
            .filter { !it.isDefeated.value }
            .map { it.position.value }
            .toHashSet()
    val trapPositions = gameState.traps.map { it.position }.toHashSet()
    val barricadePositions = gameState.barricades.map { it.position }.toHashSet()
    val fiefPositions = gameState.fiefs.map { it.position }.toHashSet()

    val tiles = mutableListOf<Position>()
    for (y in 0 until gameState.level.gridHeight) {
        for (x in 0 until gameState.level.gridWidth) {
            val pos = Position(x, y)
            if (!gameState.level.isOnPath(pos)) continue
            if (pos in attackerPositions || pos in trapPositions || pos in barricadePositions || pos in fiefPositions) continue
            tiles.add(pos)
        }
    }
    return tiles
}

/**
 * The ordered list of tiles that can currently be selected as a target for the active spell
 * targeting mode (position/enemy/tower spells), or an empty list when no spell is being targeted.
 * Used to drive keyboard targeting of spells (e.g. the Bomb) with the same place/cancel keys as
 * support object placement.
 *
 * Tiles are ordered top-to-bottom, left-to-right so the keyboard cursor moves predictably.
 */
fun spellTargetPositions(gameState: GameState): List<Position> {
    val targeting = gameState.spellTargeting.value ?: return emptyList()
    val positions =
        when (targeting.activeSpell.targetType) {
            SpellTargetType.POSITION -> targeting.validTargets.filterIsInstance<Position>()
            SpellTargetType.ENEMY ->
                targeting.validTargets.filterIsInstance<Attacker>().map { it.position.value }
            SpellTargetType.TOWER ->
                targeting.validTargets.filterIsInstance<Defender>().map { it.position.value }
            SpellTargetType.NONE -> emptyList()
        }
    return positions.distinct().sortedWith(compareBy({ it.y }, { it.x }))
}

/**
 * The tile one grid row above ([up] = true) or below ([up] = false) [current] within
 * [candidateTiles], keeping roughly the same column (nearest x). Returns null when there is no
 * candidate tile in an adjacent row in the requested direction.
 *
 * [candidateTiles] is expected to be ordered top-to-bottom, left-to-right (see
 * [supportObjectPlacementTiles] / [spellTargetPositions]). Used so keyboard placement can move a
 * whole row up/down, complementing the previous/next stepping through the flat list.
 */
fun rowStepPlacementTile(
    candidateTiles: List<Position>,
    current: Position?,
    up: Boolean,
): Position? {
    if (candidateTiles.isEmpty()) return null
    val from = current ?: candidateTiles.first()
    // Rows in the requested direction, ordered nearest-first.
    val targetRows =
        candidateTiles
            .asSequence()
            .map { it.y }
            .filter { if (up) it < from.y else it > from.y }
            .distinct()
            .sortedBy { if (up) -it else it }
            .toList()
    for (rowY in targetRows) {
        val best =
            candidateTiles
                .filter { it.y == rowY }
                .minByOrNull { kotlin.math.abs(it.x - from.x) }
        if (best != null) return best
    }
    return null
}

/**
 * Row of support boxes shown at the lower edge of the screen, above the tower buttons.
 *
 * Objects (traps, magical traps, barricades) are drawn as square boxes with a solid border,
 * while spell tokens are drawn as rounded boxes with a dashed border. When more than one of a
 * support is available, its remaining count is shown in the upper-right corner of the box.
 *
 * Clicking an object box enters placement mode; clicking a spell token box starts casting the
 * spell (which does not consume mana).
 *
 * Keyboard users can navigate the boxes with left/right and activate the focused box with a select
 * key (see [GamePlayScreen]); [focusedSlotIndex] is the index (into [visibleSupportSlots]) of the
 * box currently under that focus cursor, or null when nothing is focused.
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
    onCooldownPowerClick: (CooldownPowerType) -> Unit = {},
    selectedSupportFief: de.egril.defender.model.FiefType? = null,
    onFiefClick: (de.egril.defender.model.FiefType) -> Unit = {},
    focusedSlotIndex: Int? = null,
) {
    val supports = gameState.level.supports
    val objectTypes = displayedSupportObjectTypes(gameState)
    val spellTypes = displayedSupportSpellTypes(gameState)
    val fiefTypes = displayedSupportFiefTypes(gameState)
    if (objectTypes.isEmpty() && spellTypes.isEmpty() && fiefTypes.isEmpty() && supports.cooldownPowers.isEmpty()) return

    // Spell tokens and cooldown powers are unusable during the initial building phase — only
    // placeable objects can be placed before the first enemy turn. Reflect this by disabling
    // (graying out) those supports while building.
    val isInitialBuilding = gameState.phase.value == GamePhase.INITIAL_BUILDING
    val powersEnabled = enabled && !isInitialBuilding

    // Supports that top up a resource are pointless — and shown grayed out — when that resource is
    // already full: the Heal spell token when health is at maximum, and the mana wells when mana is
    // at maximum.
    val healthAtMax = gameState.healthPoints.value >= gameState.level.healthPoints
    val manaAtMax = gameState.currentMana.value >= gameState.maxMana.value

    Row(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Running index over the visible boxes (objects, then spells, then powers) matching the
        // ordering of visibleSupportSlots(), so the keyboard focus cursor lands on the same box.
        var slotIndex = 0

        // Placable objects (declared for the level or granted by an event; hidden once used up)
        objectTypes.forEach { type ->
            val remaining = gameState.supportObjectsRemaining[type] ?: 0
            val isFocused = slotIndex == focusedSlotIndex
            slotIndex++
            SupportBox(
                remaining = remaining,
                isSpell = false,
                isSelected = selectedSupportObject == type,
                isFocused = isFocused,
                enabled = enabled,
                tooltip = supportTooltip("support_type_object", type.localizedSupportName()),
                onClick = { onObjectClick(type) },
            ) {
                SupportObjectIcon(type, SUPPORT_ICON_SIZE)
            }
        }

        // Spell tokens (declared for the level or granted by an event; hidden once used up)
        spellTypes.forEach { spell ->
            val remaining = gameState.supportSpellsRemaining[spell] ?: 0
            // The Heal token is pointless while the player is already at full health.
            val atMaxEffect = spell == SpellType.HEAL && healthAtMax
            val isFocused = slotIndex == focusedSlotIndex
            slotIndex++
            SupportBox(
                remaining = remaining,
                isSpell = true,
                isSelected = activeSpellToken == spell,
                isFocused = isFocused,
                enabled = powersEnabled && !atMaxEffect,
                tooltip = supportTooltip("support_type_spell", spell.getLocalizedName()),
                onClick = { onSpellClick(spell) },
            ) {
                SpellTargetIcon(spell = spell, size = SUPPORT_ICON_SIZE)
            }
        }

        // Fief support tokens (hexagon-shaped buttons with path color background)
        fiefTypes.forEach { type ->
            val remaining = gameState.supportFiefRemaining[type] ?: 0
            val isFocused = slotIndex == focusedSlotIndex
            slotIndex++
            FiefSupportBox(
                type = type,
                remaining = remaining,
                isSelected = selectedSupportFief == type,
                isFocused = isFocused,
                enabled = enabled,
                tooltip = supportTooltip("support_type_object", type.localizedFiefName()),
                onClick = { onFiefClick(type) },
            )
        }

        // Cooldown-based powers (always shown, even while on cooldown)
        supports.cooldownPowers.forEach { power ->
            val readyIn = gameState.cooldownPowerReadyIn[power.type] ?: 0
            // Mana wells are pointless while mana is already at maximum.
            val atMaxEffect = power.type.addsMana && manaAtMax
            val isFocused = slotIndex == focusedSlotIndex
            slotIndex++
            CooldownPowerBox(
                type = power.type,
                readyIn = readyIn,
                enabled = powersEnabled && readyIn == 0 && !atMaxEffect,
                isFocused = isFocused,
                tooltip = supportTooltip("support_type_power", power.type.localizedCooldownPowerName()),
                onClick = { onCooldownPowerClick(power.type) },
            )
        }
    }
}

@Composable
private fun SupportBox(
    remaining: Int,
    isSpell: Boolean,
    isSelected: Boolean,
    isFocused: Boolean,
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
        SupportFocusRing(isFocused = isFocused, shape = shape) {
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
                // Dim the icon (not just the border) when the support is disabled.
                Box(
                    modifier = Modifier.alpha(alpha),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }

                // Count badge in the upper-right corner when more than one is available
                if (remaining > 1) {
                    SupportCountBadge(
                        count = remaining,
                        color = borderColor,
                        alpha = alpha,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

/**
 * Wraps a support box in a constant-size gutter that draws a bright ring around the box while it is
 * the current keyboard-focus cursor. The gutter is always reserved so neighbouring boxes do not
 * shift as the focus moves between them.
 */
@Composable
private fun SupportFocusRing(
    isFocused: Boolean,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(shape)
                .then(
                    if (isFocused) {
                        Modifier.border(width = 3.dp, color = SupportBarColors.Focus, shape = shape)
                    } else {
                        Modifier
                    },
                ).padding(SUPPORT_FOCUS_RING_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SupportCountBadge(
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Box(
        modifier =
            modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = alpha))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = supportCountDisplayText(count),
            color = Color.White.copy(alpha = alpha),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun SupportObjectIcon(
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
 * A cooldown-based support power box. Uses a rounded, solid border in a distinct color and is
 * always shown, even while the power is on cooldown. When on cooldown, the remaining number of
 * turns is displayed as a large number over a dimmed icon. A small white timer symbol is drawn in
 * the top-left corner so cooldown powers are easy to distinguish from other supports.
 */
@Composable
private fun CooldownPowerBox(
    type: CooldownPowerType,
    readyIn: Int,
    enabled: Boolean,
    isFocused: Boolean,
    tooltip: String,
    onClick: () -> Unit,
) {
    val onCooldown = readyIn > 0
    val alpha = if (enabled) 1f else 0.4f
    val shape = RoundedCornerShape(14.dp)
    val borderColor = SupportBarColors.CooldownBorder

    TooltipWrapper(text = tooltip, preferAbove = true) {
        SupportFocusRing(isFocused = isFocused, shape = shape) {
            Box(
                modifier =
                    Modifier
                        .size(SUPPORT_BOX_SIZE)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * alpha))
                        .border(
                            width = 2.dp,
                            color = borderColor.copy(alpha = alpha),
                            shape = shape,
                        ).clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Dim the icon while the power is recharging or otherwise disabled (e.g. during
                    // the initial building phase) so the symbol — not just the border — is grayed out.
                    Box(
                        modifier = Modifier.alpha((if (onCooldown) 0.35f else 1f) * alpha),
                        contentAlignment = Alignment.Center,
                    ) {
                        CooldownPowerIcon(type, SUPPORT_ICON_SIZE)
                    }

                    // Large cooldown number over the dimmed icon
                    if (onCooldown) {
                        Text(
                            text = "$readyIn",
                            color = borderColor.copy(alpha = alpha),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Small white timer symbol on a teal disc in the top-left corner so cooldown
                // powers are easy to distinguish from other supports on any background. The whole
                // badge (disc and glyph) is dimmed together when the power is disabled.
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .size(16.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(8.dp))
                            .background(borderColor),
                    contentAlignment = Alignment.Center,
                ) {
                    TimerBadge(dimension = 11.dp)
                }
            }
        }
    }
}

/** Small white clock/timer glyph drawn in a Canvas so it can be tinted white on any background. */
@Composable
private fun TimerBadge(
    modifier: Modifier = Modifier,
    dimension: Dp = 12.dp,
) {
    Canvas(modifier = modifier.size(dimension)) {
        val stroke = size.minDimension * 0.12f
        val radius = size.minDimension / 2f - stroke
        val center = Offset(size.width / 2f, size.height / 2f)
        // Clock face outline
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )
        // Hour hand (up)
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // Minute hand (right)
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(center.x + radius * 0.7f, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CooldownPowerIcon(
    type: CooldownPowerType,
    size: Dp,
) {
    when (type) {
        CooldownPowerType.COIN_SURGE -> MoneyIcon(size = size)
        CooldownPowerType.SKY_IS_FALLING -> ExplosionIcon(size = size)
        CooldownPowerType.CONSTRUCTION_REPAIRS -> HammerIcon(size = size)
        CooldownPowerType.MANA_WELL -> PentagramIcon(size = size)
        CooldownPowerType.DEEP_MANA_WELL -> PentagramIcon(size = size)
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
        supports.cooldownPowers.forEach { power ->
            SupportSummaryRow(
                count = 1,
                label = power.type.localizedCooldownPowerName(),
            ) {
                CooldownPowerIcon(power.type, 24.dp)
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
            text =
                if (isIndefiniteSupportCount(count)) {
                    "$label ∞"
                } else if (count > 1) {
                    "$label ×$count"
                } else {
                    label
                },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF333333),
        )
    }
}

/** Localized display name for a support object type. */
fun SupportObjectType.localizedSupportName(
    locale: com.hyperether.resources.AppLocale = com.hyperether.resources.currentLanguage.value,
): String {
    val key =
        when (this) {
            SupportObjectType.DWARVEN_TRAP -> "dwarven_trap"
            SupportObjectType.MAGICAL_TRAP -> "magical_trap"
            SupportObjectType.BARRICADE -> "barricade"
        }
    return com.hyperether.resources.LocalizedStrings
        .get(key, locale)
}

/**
 * Build a support tooltip that includes the support's type and name, e.g.
 * "Placeable Object: Magical Trap". [typeKey] is the localization key for the type label.
 */
private fun supportTooltip(
    typeKey: String,
    name: String,
    locale: com.hyperether.resources.AppLocale = com.hyperether.resources.currentLanguage.value,
): String {
    val typeLabel =
        com.hyperether.resources.LocalizedStrings
            .get(typeKey, locale)
    return "$typeLabel: $name"
}

fun CooldownPowerType.localizedCooldownPowerName(
    locale: com.hyperether.resources.AppLocale = com.hyperether.resources.currentLanguage.value,
): String {
    val key =
        when (this) {
            CooldownPowerType.COIN_SURGE -> "cooldown_power_coin_surge"
            CooldownPowerType.SKY_IS_FALLING -> "cooldown_power_sky_is_falling"
            CooldownPowerType.CONSTRUCTION_REPAIRS -> "cooldown_power_construction_repairs"
            CooldownPowerType.MANA_WELL -> "cooldown_power_mana_well"
            CooldownPowerType.DEEP_MANA_WELL -> "cooldown_power_deep_mana_well"
        }
    return com.hyperether.resources.LocalizedStrings
        .get(key, locale)
}

/** Localized display name for a fief type. */
fun FiefType.localizedFiefName(
    locale: com.hyperether.resources.AppLocale = com.hyperether.resources.currentLanguage.value,
): String =
    com.hyperether.resources.LocalizedStrings
        .get(this.nameKey, locale)

/** Path tile color used as the background for fief support buttons. */
private fun fiefButtonBackgroundColor(isDarkMode: Boolean): Color =
    if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)

/**
 * A hexagon-shaped support button for a fief token. Uses the path tile background color to signal
 * that fiefs are placed on path tiles. Clicking enters fief placement mode.
 */
@Composable
private fun FiefSupportBox(
    type: FiefType,
    remaining: Int,
    isSelected: Boolean,
    isFocused: Boolean,
    enabled: Boolean,
    tooltip: String,
    onClick: () -> Unit,
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val backgroundColor = fiefButtonBackgroundColor(isDarkMode)
    val borderColor = if (isSelected) SupportBarColors.Selected else backgroundColor
    val alpha = if (enabled) 1f else 0.4f
    val hexShape = HexagonShape()

    TooltipWrapper(text = tooltip, preferAbove = true) {
        SupportFocusRing(isFocused = isFocused, shape = hexShape) {
            Box(
                modifier =
                    Modifier
                        .size(SUPPORT_BOX_SIZE)
                        .clip(hexShape)
                        .background(backgroundColor.copy(alpha = 0.85f * alpha))
                        .border(
                            width = if (isSelected) 3.dp else 2.dp,
                            color = borderColor.copy(alpha = alpha),
                            shape = hexShape,
                        ).clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.alpha(alpha),
                    contentAlignment = Alignment.Center,
                ) {
                    val iconSize = (SUPPORT_BOX_SIZE.value * 0.55f).dp
                    when (type) {
                        FiefType.FISHER ->
                            Image(
                                painter = painterResource(Res.drawable.fief_fisher_hut),
                                contentDescription = type.localizedFiefName(),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(iconSize),
                            )
                        FiefType.WOODCUTTER ->
                            Image(
                                painter = painterResource(Res.drawable.fief_woodcutter),
                                contentDescription = type.localizedFiefName(),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(iconSize),
                            )
                        FiefType.QUARRY ->
                            Image(
                                painter = painterResource(Res.drawable.fief_quarry),
                                contentDescription = type.localizedFiefName(),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(iconSize),
                            )
                        FiefType.MARKETPLACE ->
                            Image(
                                painter = painterResource(Res.drawable.fief_marketplace),
                                contentDescription = type.localizedFiefName(),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(iconSize),
                            )
                    }
                }

                if (remaining > 1) {
                    SupportCountBadge(
                        count = remaining,
                        color = SupportBarColors.ObjectBorder,
                        alpha = alpha,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}
