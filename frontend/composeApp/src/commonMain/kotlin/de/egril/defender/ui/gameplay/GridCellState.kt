package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.TileType
import de.egril.defender.model.*
import de.egril.defender.ui.GradientBlendedTileCell
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.icon.enemy.EnemyAttackPreview
import de.egril.defender.ui.icon.enemy.enemyAttackPreview

@Composable
internal fun GridCellFrame(
    hexSize: Dp,
    position: Position,
    tileType: TileType,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    useDashedBorder: Boolean,
    backgroundPainter: Painter?,
    onClick: () -> Unit,
    onHover: ((Boolean) -> Unit)?,
    shouldUseGradientBlending: Boolean,
    getNeighborTileType: (Position) -> TileType?,
    neighborPainters: Map<Position, Painter?>,
    content: @Composable BoxScope.() -> Unit,
) {
    if (shouldUseGradientBlending) {
        GradientBlendedTileCell(
            hexSize = hexSize,
            position = position,
            tileType = tileType,
            backgroundColor = backgroundColor,
            borderColor = if (useDashedBorder) Color.Transparent else borderColor,
            borderWidth = if (useDashedBorder) 0.dp else borderWidth,
            backgroundPainter = backgroundPainter,
            onClick = onClick,
            onHover = onHover,
            getNeighborTileType = getNeighborTileType,
            getNeighborTilePainter = { pos, _ -> neighborPainters[pos] },
            content = content,
        )
    } else {
        BaseGridCell(
            hexSize = hexSize,
            backgroundColor = backgroundColor,
            borderColor = if (useDashedBorder) Color.Transparent else borderColor,
            borderWidth = if (useDashedBorder) 0.dp else borderWidth,
            backgroundPainter = backgroundPainter,
            onClick = onClick,
            onHover = onHover,
            content = content,
        )
    }
}

/**
 * Derived, non-Compose per-cell state for [GridCell]: spell/cooling areas, attack previews and
 * trap/barricade placement highlights. Built from the cell's inputs; holds no `remember` state.
 */
internal class GridCellState(
    position: Position,
    gameState: GameState,
    defender: Defender?,
    attacker: Attacker?,
    selectedDefender: Defender?,
    selectedDefenderId: Int?,
    isHovering: Boolean,
    isEnemyTraversable: Boolean,
    isOnPath: Boolean,
    fieldEffect: FieldEffect?,
    trap: Trap?,
    fief: Fief?,
    mushroom: Mushroom?,
    gargoyle: Gargoyle?,
    targetCircleInfo: TargetCircleInfo?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction?,
    selectedBarricadeAction: BarricadeAction?,
    isSelectedAttackTarget: Boolean,
) {
    // Check if this tile is in a cooling spell area (show snowflake on affected path tiles)
    val isInCoolingArea =
        isEnemyTraversable &&
            gameState.activeSpellEffects.any { effect ->
                effect.spell == SpellType.COOLING_SPELL &&
                    effect.position != null &&
                    position.hexDistanceTo(effect.position) <= 2
            }

    // Cooling area turns remaining (for active cooling effects on this tile)
    val coolingAreaTurnsRemaining: Int? =
        if (isInCoolingArea) {
            gameState.activeSpellEffects
                .filter { effect ->
                    effect.spell == SpellType.COOLING_SPELL &&
                        effect.position != null &&
                        position.hexDistanceTo(effect.position) <= 2
                }.minOfOrNull { it.turnsRemaining }
        } else {
            null
        }

    // Is this tile part of a cooling spell placement preview?
    val isCoolingSpellPreview =
        targetCircleInfo != null &&
            gameState.spellTargeting.value?.activeSpell == SpellType.COOLING_SPELL

    // Check for active bomb spell effect at this position
    val bombEffect =
        gameState.activeSpellEffects.find {
            it.spell == SpellType.BOMB && it.position == position
        }

    // Check for bomb explosion visual effect at this position
    val bombExplosion =
        gameState.bombExplosionEffects.find { explosion ->
            explosion.center == position || explosion.affectedPositions.contains(position)
        }

    // Check if this tile is a valid spell target
    val spellTargeting = gameState.spellTargeting.value
    val isValidSpellTarget =
        if (spellTargeting != null) {
            when (spellTargeting.activeSpell.targetType) {
                de.egril.defender.model.SpellTargetType.ENEMY -> {
                    val enemyHere = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                    enemyHere != null && spellTargeting.validTargets.contains(enemyHere)
                }
                de.egril.defender.model.SpellTargetType.TOWER -> {
                    val towerHere = gameState.defenders.find { it.position.value == position }
                    towerHere != null && spellTargeting.validTargets.contains(towerHere)
                }
                de.egril.defender.model.SpellTargetType.POSITION -> {
                    spellTargeting.validTargets.contains(position)
                }
                else -> false
            }
        } else {
            false
        }
    val selectedDefenderForRange = selectedDefender
    val hasDoubleReachBuff =
        selectedDefenderForRange?.let { sel ->
            gameState.activeSpellEffects.any { it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == sel.id }
        } ?: false
    val cellIsInRange =
        selectedDefenderForRange?.let { sel ->
            if (sel.position.value == position) {
                false // Don't highlight the defender's own cell
            } else {
                val distance = sel.position.value.distanceTo(position)
                val effectiveRange = gameState.effectiveRange(sel)
                distance >= sel.type.minRange && distance <= effectiveRange
            }
        } ?: false

    // Tiles that are in range ONLY because of the double-reach spell (beyond normal range)
    val cellIsInDoubleReachOnlyRange =
        if (hasDoubleReachBuff) {
            val sel = selectedDefenderForRange!!
            if (sel.position.value == position) {
                false
            } else {
                val distance = sel.position.value.distanceTo(position)
                distance >= sel.type.minRange && distance > sel.range && distance <= sel.range * 2
            }
        } else {
            false
        }

    // Attack damage / lethality / immunity preview shown at the left border of an enemy that is
    // affected by the currently selected tower attack: the selected target for single-target
    // attacks, or any enemy within the blast area for AREA/LASTING attacks. Only shown when no
    // build/trap/barricade placement mode is active (issue #591). Computed here (where
    // selectedDefender is known) and passed into GridCellContent for rendering.
    val attackPreview: EnemyAttackPreview? =
        run {
            val sel = selectedDefender
            // Stone gargoyles show the immunity shield against tower types that deal no damage to them
            if (gargoyle != null && sel != null) {
                val distance = sel.position.value.distanceTo(position)
                if (sel.type in GargoyleRules.NO_DAMAGE_DEFENDERS &&
                    distance >= sel.type.minRange &&
                    distance <= sel.range &&
                    sel.isReady &&
                    sel.actionsRemaining.value > 0 &&
                    selectedMineAction == null &&
                    selectedWizardAction == null &&
                    selectedBarricadeAction == null
                ) {
                    return@run EnemyAttackPreview(damage = 0, isLethal = false, isImmune = true)
                }
            }
            if (attacker == null ||
                sel == null ||
                sel.type.attackType == AttackType.NONE ||
                !sel.isReady ||
                sel.actionsRemaining.value <= 0 ||
                !isSelectedAttackTarget ||
                selectedMineAction != null ||
                selectedWizardAction != null ||
                selectedBarricadeAction != null
            ) {
                null
            } else {
                val hasDoubleLevelBuff =
                    gameState.activeSpellEffects.any {
                        it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == sel.id
                    }
                enemyAttackPreview(attacker, sel, hasDoubleLevelBuff, gameState)
            }
        }

    // Calculate hover preview for trap placement
    val isHoveringForTrapPreview = isHovering
    val isTrapPlacementMode = selectedMineAction == MineAction.BUILD_TRAP || selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP

    // Check if this tile is valid for trap placement (on path, in range, no enemy, no existing trap, no field effects)
    val isValidTrapPlacement =
        if (isTrapPlacementMode && isHoveringForTrapPreview && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                val distance = sel.position.value.distanceTo(position)
                val hasEnemy = attacker != null
                val hasTrap = trap != null
                val hasFieldEffect = fieldEffect != null
                val hasFief = fief != null
                val hasMushroom = mushroom != null
                isOnPath && distance <= sel.range && !hasEnemy && !hasTrap && !hasFieldEffect && !hasFief && !hasMushroom
            } ?: false
        } else {
            false
        }

    val showTrapPreview = isValidTrapPlacement

    // Barricade placement range detection (3 tiles, yellow borders for empty path tiles)
    val isBarricadePlacement = selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE
    val cellIsInBarricadeRange =
        if (isBarricadePlacement && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                // Check if within 3 tiles range
                val distance = sel.position.value.distanceTo(position)
                val isInRange = distance > 0 && distance <= 3
                // Check if empty path tile (no defender, no attacker, can have existing barricade for reinforcement)
                val isEmptyPath = isOnPath && defender == null && attacker == null && fief == null && mushroom == null
                isInRange && isEmptyPath
            } ?: false
        } else {
            false
        }

    // Show barricade preview when hovering over valid barricade placement tile
    val showBarricadePreview = isBarricadePlacement && isHovering && cellIsInBarricadeRange

    // Mine trap placement range detection (path tiles within range of selected mine)
    val isMineTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP
    val cellIsValidForMineTrapPlacement =
        if (isMineTrapPlacement && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                val distance = sel.position.value.distanceTo(position)
                val isInRange = distance > 0 && distance <= sel.range
                val isEmptyPath = isOnPath && attacker == null && trap == null && fieldEffect == null && fief == null && mushroom == null
                isInRange && isEmptyPath
            } ?: false
        } else {
            false
        }

    // Magical trap placement range detection (path tiles within range of selected wizard)
    val isMagicalTrapPlacement = selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    val cellIsValidForMagicalTrapPlacement =
        if (isMagicalTrapPlacement && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                val distance = sel.position.value.distanceTo(position)
                val isInRange = distance > 0 && distance <= sel.range
                val isEmptyPath = isOnPath && attacker == null && trap == null && fieldEffect == null && fief == null && mushroom == null
                isInRange && isEmptyPath
            } ?: false
        } else {
            false
        }
}
