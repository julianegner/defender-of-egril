package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.game.canManuallyTargetAttacker
import de.egril.defender.model.*
import de.egril.defender.ui.MapPanController
import de.egril.defender.ui.icon.SpeakerHighIcon
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import defender_of_egril.composeapp.generated.resources.*

@Composable
internal fun ColumnScope.GamePlayMapArea(
    state: GamePlayScreenState,
    gameState: GameState,
    isDemoMode: Boolean,
    showMagicPanel: Boolean,
    captionsEnabled: Boolean,
    availableWindowWidth: Dp,
    availableWindowHeight: Dp,
    demoHoveredPosition: Position?,
    scrollToPosition: Position?,
    onScrollToPositionConsumed: (() -> Unit)?,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onPlaceSupportObject: ((SupportObjectType, Position) -> Boolean)?,
    onPlaceSupportFief: ((de.egril.defender.model.FiefType, Position) -> Boolean)?,
    onSelectSpellTarget: ((Any) -> Unit)?,
    onOpenMagicPanel: (() -> Unit)?,
    onCloseMagicPanel: (() -> Unit)?,
    onMineBuildTrap: ((Int, Position) -> Boolean)?,
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)?,
    onBuildBarricade: ((Int, Position) -> Boolean)?,
    onSandboxPaintTile: ((de.egril.defender.model.Position, de.egril.defender.editor.TileType, de.egril.defender.model.RiverFlow, Int) -> Unit)?,
    mapPanController: MapPanController,
) {
    with(state) {
        // Game Grid with toggle button and overlay
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrollable Game Grid
            GameGrid(
                gameState = gameState,
                selectedDefenderType = selectedDefenderType,
                selectedDefenderId = selectedDefenderId,
                selectedAttackerId = selectedAttackerId,
                selectedTargetId = selectedTargetId,
                selectedTargetPosition = selectedTargetPosition,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                selectedBarricadeAction = selectedBarricadeAction,
                extraFocusTrigger = mapRefocusTrigger,
                mapPanController = mapPanController,
                onCellClick = { position ->
                    // Sandbox: map tile painting takes precedence over all other interactions.
                    val paintType = sandboxPaintTileType
                    if (paintType != null) {
                        onSandboxPaintTile?.invoke(position, paintType, sandboxRiverFlow, sandboxRiverSpeed)
                        return@GameGrid
                    }

                    // Handle spell targeting mode first
                    val targeting = gameState.spellTargeting.value
                    if (targeting != null) {
                        // Check if this position is a valid target
                        when (targeting.activeSpell.targetType) {
                            de.egril.defender.model.SpellTargetType.POSITION -> {
                                // Position click - cast spell on position
                                onSelectSpellTarget?.invoke(position)
                            }
                            de.egril.defender.model.SpellTargetType.ENEMY -> {
                                // Check if there's an enemy at this position
                                val enemy = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                if (enemy != null && targeting.validTargets.contains(enemy)) {
                                    onSelectSpellTarget?.invoke(enemy)
                                }
                            }
                            de.egril.defender.model.SpellTargetType.TOWER -> {
                                // Check if there's a tower at this position
                                val tower = gameState.defenders.find { it.position.value == position }
                                if (tower != null && targeting.validTargets.contains(tower)) {
                                    onSelectSpellTarget?.invoke(tower)
                                }
                            }
                            else -> {
                                // Invalid targeting type, should not happen
                            }
                        }
                        return@GameGrid
                    }

                    // Handle support object placement mode
                    selectedSupportObject?.let { supportType ->
                        if (onPlaceSupportObject?.invoke(supportType, position) == true) {
                            // Deselect if no more of this support object remain
                            val remaining = gameState.supportObjectsRemaining[supportType] ?: 0
                            if (remaining <= 0) {
                                selectedSupportObject = null
                            }
                        }
                        return@GameGrid
                    }

                    // Handle fief support placement mode
                    selectedSupportFief?.let { fiefType ->
                        if (onPlaceSupportFief?.invoke(fiefType, position) == true) {
                            // Deselect if no more of this fief type remain
                            val remaining = gameState.supportFiefRemaining[fiefType] ?: 0
                            if (remaining <= 0) {
                                selectedSupportFief = null
                            }
                        }
                        return@GameGrid
                    }

                    // Try to place defender if one is selected
                    selectedDefenderType?.let { type ->
                        if (onPlaceDefender(type, position)) {
                            // Only deselect if player can no longer afford this tower type
                            if (!gameState.canPlaceDefender(type)) {
                                selectedDefenderType = null
                            }
                            // Track tutorial progress
                            if (gameState.tutorialState.value.isActive &&
                                !gameState.tutorialState.value.hasPlacedFirstTower
                            ) {
                                gameState.tutorialState.value = gameState.tutorialState.value.markTowerPlaced()
                            }
                        }
                        return@GameGrid
                    }

                    val previousSelectedDefenderId = selectedDefenderId
                    val previousSelectedAttackerId = selectedAttackerId

                    // Check if there's a defender at this position
                    val defender = gameState.defenders.find { it.position.value == position }
                    if (defender != null) {
                        if (previousSelectedDefenderId == defender.id) {
                            // Deselect if clicking the same defender
                            selectedDefenderId = null
                            // Clear trap modes when deselecting
                            selectedMineAction = null
                            selectedWizardAction = null
                            selectedBarricadeAction = null
                        } else {
                            // Select this defender, deselect any selected attacker
                            selectedDefenderId = defender.id
                            selectedAttackerId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            selectedBarricadePosition = null
                            selectedGargoyleId = null
                            // Clear trap modes when selecting a different defender
                            selectedMineAction = null
                            selectedWizardAction = null
                            selectedBarricadeAction = null
                            return@GameGrid
                        }
                    }

                    // Check if there's an attacker at this position (only if no defender is being placed)
                    val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                    val positionInShadowFog = gameState.fieldEffects.any { it.type == FieldEffectType.SHADOW_FOG && it.position == position }
                    if (attacker != null &&
                        selectedDefenderId == null &&
                        !positionInShadowFog &&
                        gameState.canManuallyTargetAttacker(attacker)
                    ) {
                        if (previousSelectedAttackerId == attacker.id) {
                            // Deselect if clicking the same attacker
                            selectedAttackerId = null
                        } else {
                            // Select this attacker, deselect any selected defender
                            selectedAttackerId = attacker.id
                            selectedDefenderId = null
                            selectedGargoyleId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            selectedBarricadePosition = null
                            return@GameGrid
                        }
                    }

                    // Inactive gargoyles are stone statues, not attackers; tapping one shows its info panel
                    val inactiveGargoyle = gameState.gargoyles.find { it.position.value == position && !it.isActive.value }
                    if (inactiveGargoyle != null &&
                        attacker == null &&
                        selectedDefenderId == null &&
                        !positionInShadowFog
                    ) {
                        selectedGargoyleId =
                            if (selectedGargoyleId == inactiveGargoyle.id) null else inactiveGargoyle.id
                        selectedAttackerId = null
                        selectedTargetId = null
                        selectedTargetPosition = null
                        selectedBarricadePosition = null
                        return@GameGrid
                    }

                    // Toggle magic panel when the Target tile is clicked
                    if (gameState.level.isTargetPosition(position) &&
                        onOpenMagicPanel != null &&
                        gameState.maxMana.value > 0
                    ) {
                        if (showMagicPanel) {
                            onCloseMagicPanel?.invoke()
                        } else {
                            onOpenMagicPanel.invoke()
                        }
                        return@GameGrid
                    }

                    // Check if there's a barricade at this position
                    // Don't show info panel if barricade has a tower - player must sell tower first
                    val barricade = gameState.barricades.find { it.position == position }
                    if (barricade != null &&
                        !barricade.hasTower() &&
                        selectedDefenderId == null &&
                        selectedAttackerId == null
                    ) {
                        selectedBarricadePosition = position
                        // Clear other selections
                        selectedDefenderId = null
                        selectedAttackerId = null
                        return@GameGrid
                    }

                    // Check if there's a trap at this position - show removal confirmation
                    val trap = gameState.traps.find { it.position == position }
                    if (trap != null && selectedDefenderId == null && selectedAttackerId == null) {
                        trapToRemove = position
                        showRemoveTrapDialog = true
                        return@GameGrid
                    }

                    // Handle targeting for selected defender
                    if (selectedDefenderId != null) {
                        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (selectedDefender != null) {
                            // Handle trap building for mines
                            if (selectedDefender.type == DefenderType.DWARVEN_MINE &&
                                selectedMineAction == MineAction.BUILD_TRAP
                            ) {
                                // Check if position is on the path and in range
                                val distance = selectedDefender.position.value.distanceTo(position)
                                if (gameState.level.isOnPath(position) && distance <= selectedDefender.range) {
                                    if (onMineBuildTrap?.invoke(selectedDefender.id, position) == true) {
                                        // Keep trap placement mode active if tower has actions remaining
                                        if (!shouldKeepPlacementMode(gameState, selectedDefender.id)) {
                                            selectedMineAction = null
                                            showMineActionDialog = false
                                        }
                                    }
                                }
                                return@GameGrid
                            }

                            // Handle magical trap placement for wizard towers (level 10+)
                            if (selectedDefender.hasMagicalTraps &&
                                selectedDefender.level.value >= 10 &&
                                selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
                            ) {
                                // Check if position is on the path and in range
                                val distance = selectedDefender.position.value.distanceTo(position)
                                val hasEnemy = gameState.attackers.any { it.position.value == position && !it.isDefeated.value }
                                val hasTrap = gameState.traps.any { it.position == position }
                                if (gameState.level.isOnPath(position) &&
                                    distance <= selectedDefender.range &&
                                    !hasEnemy &&
                                    !hasTrap
                                ) {
                                    if (onWizardPlaceMagicalTrap?.invoke(selectedDefender.id, position) == true) {
                                        selectedWizardAction = null
                                    }
                                }
                                return@GameGrid
                            }

                            // Handle barricade placement for spike/spear towers (level 10+)
                            if ((
                                    selectedDefender.type == DefenderType.SPIKE_TOWER ||
                                        selectedDefender.type == DefenderType.SPEAR_TOWER
                                ) &&
                                selectedDefender.level.value >= 10 &&
                                selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE
                            ) {
                                // Check if position is on path, within range (3 tiles), and empty
                                val distance = selectedDefender.position.value.distanceTo(position)
                                val hasDefender = gameState.defenders.any { it.position.value == position }
                                val hasEnemy = gameState.attackers.any { it.position.value == position && !it.isDefeated.value }
                                if (gameState.level.isOnPath(position) &&
                                    distance <= 3 &&
                                    !hasDefender &&
                                    !hasEnemy
                                ) {
                                    if (onBuildBarricade?.invoke(selectedDefender.id, position) == true) {
                                        // Keep barricade placement mode active if tower has actions remaining
                                        if (!shouldKeepPlacementMode(gameState, selectedDefender.id)) {
                                            selectedBarricadeAction = null
                                        }
                                    }
                                }
                                return@GameGrid
                            }

                            // For AREA/LASTING (fireball and acid) attacks, allow targeting path tiles OR river tiles
                            val effectiveRange = gameState.effectiveRange(selectedDefender)
                            if (selectedDefender.type.attackType == AttackType.AREA ||
                                selectedDefender.type.attackType == AttackType.LASTING
                            ) {
                                // Check if position is on the path, river, or spawn point and in range
                                val distance = selectedDefender.position.value.distanceTo(position)

                                if (gameState.level.isEnemyOccupiable(position) &&
                                    distance >= selectedDefender.type.minRange &&
                                    distance <= effectiveRange
                                ) {
                                    selectedTargetPosition = position
                                    // Set targetId only if there's a visible enemy (not hidden in shadow fog)
                                    val hasFog = gameState.fieldEffects.any { it.type == FieldEffectType.SHADOW_FOG && it.position == position }
                                    val enemyAtPosition =
                                        if (hasFog) {
                                            null
                                        } else {
                                            gameState.attackers.find {
                                                it.position.value == position &&
                                                    !it.isDefeated.value &&
                                                    gameState.canManuallyTargetAttacker(it)
                                            }
                                        }
                                    selectedTargetId = enemyAtPosition?.id
                                }
                            } else {
                                // For single-target attacks, allow targeting enemies, bridges, or shadow fog tiles
                                val distance = selectedDefender.position.value.distanceTo(position)
                                val attackerForTargeting =
                                    gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                val bridgeAtPosition = gameState.getBridgeAt(position)
                                val inactiveGargoyleAtPosition =
                                    gameState.gargoyles.find { it.position.value == position && !it.isActive.value }
                                val hasShadowFog =
                                    gameState.fieldEffects.any { it.type == FieldEffectType.SHADOW_FOG && it.position == position }

                                if (distance >= selectedDefender.type.minRange && distance <= effectiveRange) {
                                    if (attackerForTargeting != null &&
                                        !hasShadowFog &&
                                        gameState.canManuallyTargetAttacker(attackerForTargeting)
                                    ) {
                                        selectedTargetId = attackerForTargeting.id
                                        selectedTargetPosition = position // to be able to show the 3 circles to highlight the target
                                    } else if (bridgeAtPosition != null && bridgeAtPosition.isActive) {
                                        // Allow targeting bridge tiles
                                        selectedTargetId = null // Bridges don't have attacker IDs
                                        selectedTargetPosition = position
                                    } else if (inactiveGargoyleAtPosition != null && !hasShadowFog) {
                                        // Inactive gargoyles are not attackers; they can only be hit by manual targeting
                                        selectedTargetId = null
                                        selectedTargetPosition = position
                                    } else if (hasShadowFog) {
                                        // Allow targeting shadow fog tiles blind (enemy may be hidden there)
                                        selectedTargetId = null
                                        selectedTargetPosition = position
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                scrollToPosition = scrollToPosition ?: tabScrollPosition,
                onScrollToPositionConsumed = {
                    if (scrollToPosition != null) {
                        onScrollToPositionConsumed?.invoke()
                    } else {
                        tabScrollPosition = null
                    }
                },
                isDemoMode = isDemoMode,
                demoHoveredPosition = demoHoveredPosition,
                keyboardHoveredPosition = keyboardSelectedBuildTile,
                keyboardPlacementCursor = keyboardPlacementTile,
                selectedSupportObject = selectedSupportObject,
                selectedSupportFief = selectedSupportFief,
            )

            // Sandbox: persistent map-tile selector (from the map editor), always
            // available while playing a sandbox level. Selecting a tile type activates
            // painting; tapping a map tile then repaints it. Drawn as an overlay OVER the
            // map (left-center, high zIndex) so it is never hidden behind the map plane.
            if (gameState.level.isSandbox) {
                SandboxTilePalette(
                    selectedTileType = sandboxPaintTileType,
                    onSelectTileType = { sandboxPaintTileType = it },
                    selectedRiverFlow = sandboxRiverFlow,
                    onSelectRiverFlow = { sandboxRiverFlow = it },
                    selectedRiverSpeed = sandboxRiverSpeed,
                    onSelectRiverSpeed = { sandboxRiverSpeed = it },
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .zIndex(30f),
                )
            }

            val captionText = soundCaptionText
            if (captionsEnabled && captionText != null) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .testTag("gameplaySoundCaption"),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SpeakerHighIcon(size = 18.dp)
                        }
                        Text(
                            text = captionText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Overlay panel with Legend and Enemy List (conditionally shown)
            // Auto-open during LEGEND_INFO (legend only) or ENEMY_LIST_INFO (enemy list only) tutorial steps
            val currentTutorialStep = gameState.tutorialState.value.currentStep
            val shouldShowLegendForTutorial = currentTutorialStep == TutorialStep.LEGEND_INFO
            val shouldShowEnemyListForTutorial = currentTutorialStep == TutorialStep.ENEMY_LIST_INFO
            val isOverlayVisible = showOverlay || shouldShowLegendForTutorial || shouldShowEnemyListForTutorial

            if (isOverlayVisible) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .width(250.dp)
                            .fillMaxHeight()
                            .padding(8.dp),
                ) {
                    // Legend - show if mode includes legend OR during LEGEND_INFO tutorial step
                    val showLegendPanel = (showOverlay && overlayMode != 2) || shouldShowLegendForTutorial
                    if (showLegendPanel) {
                        GameLegend(
                            modifier = Modifier.fillMaxWidth(),
                            forceExpanded = shouldShowLegendForTutorial,
                        )

                        // Add spacer only if both legend and enemy list are shown
                        if (showOverlay && overlayMode == 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Enemy List - show if mode includes enemy list OR during ENEMY_LIST_INFO tutorial step
                    val showEnemyPanel = (showOverlay && overlayMode != 1) || shouldShowEnemyListForTutorial
                    if (showEnemyPanel) {
                        UpcomingEventsPanel(
                            gameState = gameState,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        EnemyListPanel(
                            gameState = gameState,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            forceExpanded = shouldShowEnemyListForTutorial,
                        )
                    }
                }
            }

            // Tutorial card (positioned in upper right corner, or to the left of legend/enemy list when they're showing)
            if (gameState.tutorialState.value.shouldShowOverlay() || gameState.infoState.value.shouldShowOverlay()) {
                // Check if we should allow skipping attack step
                // (tower has no actions left or can't reach any enemies)
                if (gameState.tutorialState.value.currentStep == TutorialStep.ATTACKING &&
                    !gameState.tutorialState.value.canSkipAttacking
                ) {
                    val hasTowerWithActions =
                        gameState.defenders.any { defender ->
                            defender.actionsRemaining.value > 0 && defender.buildTimeRemaining.value == 0
                        }
                    val selectedDefender =
                        selectedDefenderId?.let { id ->
                            gameState.defenders.find { it.id == id }
                        }
                    val canReachEnemies =
                        selectedDefender?.let { defender ->
                            gameState.attackers.any { attacker ->
                                !attacker.isDefeated.value &&
                                    defender.position.value.distanceTo(attacker.position.value) <= defender.range
                            }
                        } ?: false

                    // Allow skipping if no tower has actions or selected tower can't reach enemies
                    if (!hasTowerWithActions || (selectedDefender != null && !canReachEnemies)) {
                        gameState.tutorialState.value = gameState.tutorialState.value.allowSkipAttacking()
                    }
                }

                // Position tutorial card to the left of the overlay panel when it's showing
                val tutorialLayout =
                    calculateTutorialOverlayLayout(
                        availableWidth = availableWindowWidth,
                        availableHeight = availableWindowHeight,
                        isMobileWeb = isMobileWebBrowser(),
                        isSideOverlayVisible = isOverlayVisible,
                    )
                val tutorialAlignment =
                    if (isOverlayVisible) {
                        Alignment.TopEnd
                    } else {
                        Alignment.TopEnd
                    }

                // Add padding to position tutorial to the left of the overlay
                val tutorialPaddingEnd = if (isOverlayVisible) 266.dp else 8.dp // 250dp overlay + 16dp spacing

                Box(
                    modifier =
                        Modifier
                            .align(tutorialAlignment)
                            .padding(top = 8.dp, end = tutorialPaddingEnd, start = 8.dp, bottom = 8.dp),
                ) {
                    // Special case for SPECIAL_TOWERS_INFO - show as a separate dialog
                    if (gameState.infoState.value.currentInfo == InfoType.SPECIAL_TOWERS_INFO) {
                        // Refocus after the dialog leaves composition; a refocus issued while it is
                        // still shown can be lost when the dialog removes its focus target.
                        DisposableEffect(Unit) {
                            onDispose { mapRefocusTrigger++ }
                        }
                        val specialTowers =
                            gameState.level.availableTowers.filter {
                                it in
                                    listOf(
                                        DefenderType.WIZARD_TOWER,
                                        DefenderType.ALCHEMY_TOWER,
                                        DefenderType.BALLISTA_TOWER,
                                        DefenderType.DWARVEN_MINE,
                                    )
                            }
                        LevelSpecialTowersInfoDialog(
                            specialTowers = specialTowers,
                            onDismiss = {
                                val currentInfoState = gameState.infoState.value
                                val dismissedInfo = currentInfoState.dismissInfo()
                                gameState.infoState.value = dismissedInfo
                            },
                        )
                    } else if (gameState.infoState.value.currentInfo == InfoType.TOWER_INFO) {
                        // Special case for TOWER_INFO - show tower-specific info dialog
                        val towerId = gameState.infoState.value.towerInfoId
                        val defender = towerId?.let { id -> gameState.defenders.find { it.id == id } }
                        if (defender != null) {
                            TowerInfoDialog(
                                defender = defender,
                                gameState = gameState,
                                onDismiss = {
                                    val currentInfoState = gameState.infoState.value
                                    val dismissedInfo = currentInfoState.dismissInfo()
                                    gameState.infoState.value = dismissedInfo
                                },
                            )
                        }
                    } else {
                        // Show info or tutorial in the tutorial overlay
                        TutorialOverlay(
                            currentStep = gameState.tutorialState.value.currentStep,
                            isNextEnabled = gameState.tutorialState.value.isNextEnabled(gameState.defenders.size),
                            onNext = {
                                val currentTutorialState = gameState.tutorialState.value
                                gameState.tutorialState.value = currentTutorialState.advanceStep()
                            },
                            onSkip = {
                                gameState.tutorialState.value = gameState.tutorialState.value.skip()
                            },
                            currentInfo = gameState.infoState.value.currentInfo,
                            onDismissInfo = {
                                val currentInfoState = gameState.infoState.value
                                val dismissedInfo = currentInfoState.dismissInfo()
                                gameState.infoState.value = dismissedInfo

                                // Remove mine warning from the list if it was a mine warning
                                if (currentInfoState.currentInfo == InfoType.MINE_WARNING) {
                                    currentInfoState.mineWarningId?.let { gameState.mineWarnings.remove(it) }
                                }
                            },
                            layout = tutorialLayout,
                        )
                    }
                }
            }

            // Keyboard navigation hint overlay (bottom-left, away from minimap)
            val showKeyboardHints =
                AppSettings.showButtonShortcutHints.value &&
                    !showMagicPanel &&
                    (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN)
            if (showKeyboardHints) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 8.dp, start = 8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ShortcutKeyChip(
                                    text = formatShortcutBindingForDisplay(AppSettings.shortcutSelectNextTower.value),
                                )
                                Text(stringResource(Res.string.keyboard_nav_next), style = MaterialTheme.typography.labelSmall)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ShortcutKeyChip(
                                    text = formatShortcutBindingForDisplay(AppSettings.shortcutSelectPreviousTower.value),
                                )
                                Text(stringResource(Res.string.keyboard_nav_prev), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ShortcutKeyChip(text = "Enter")
                                Text(
                                    stringResource(Res.string.keyboard_nav_confirm),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        // Mode switching info (only in player turn - tower type vs existing tower)
                        if (gameState.phase.value == GamePhase.PLAYER_TURN) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutSwitchToTowerMode.value),
                                    )
                                    Text(
                                        stringResource(Res.string.keyboard_shortcut_switch_to_tower_mode),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                        // Enemy target cycling (only in player turn, only if a defender is selected)
                        if (gameState.phase.value == GamePhase.PLAYER_TURN && selectedDefenderId != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutNextEnemyTarget.value),
                                    )
                                    Text(
                                        stringResource(Res.string.keyboard_shortcut_next_enemy_target),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutPrevEnemyTarget.value),
                                    )
                                    Text(
                                        stringResource(Res.string.keyboard_shortcut_prev_enemy_target),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                        // Build tile cycling (when a tower type is selected for placement)
                        if (selectedDefenderType != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutNextEnemyTarget.value),
                                    )
                                    Text(
                                        stringResource(Res.string.keyboard_shortcut_next_build_tile),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutPrevEnemyTarget.value),
                                    )
                                    Text(
                                        stringResource(Res.string.keyboard_shortcut_prev_build_tile),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                        // Special action shortcuts (when a tower with special actions is selected)
                        if (gameState.phase.value == GamePhase.PLAYER_TURN && selectedDefenderId != null) {
                            val selectedDefenderForHints = gameState.defenders.find { it.id == selectedDefenderId }
                            if (selectedDefenderForHints != null) {
                                val showKey1 =
                                    when (selectedDefenderForHints.type) {
                                        DefenderType.WIZARD_TOWER, DefenderType.DWARVEN_MINE, DefenderType.ALTAR -> true
                                        DefenderType.SPIKE_TOWER -> selectedDefenderForHints.level.value >= 20
                                        DefenderType.SPEAR_TOWER -> selectedDefenderForHints.level.value >= 10
                                        else -> false
                                    }
                                val key1Label =
                                    when (selectedDefenderForHints.type) {
                                        DefenderType.WIZARD_TOWER -> stringResource(Res.string.generate_mana)
                                        DefenderType.ALTAR -> stringResource(Res.string.activate_altar)
                                        DefenderType.DWARVEN_MINE -> stringResource(Res.string.dig)
                                        DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER ->
                                            stringResource(
                                                Res.string.barricade,
                                            )
                                        else -> ""
                                    }
                                val showKey2 =
                                    when (selectedDefenderForHints.type) {
                                        DefenderType.WIZARD_TOWER, DefenderType.ALTAR -> selectedDefenderForHints.level.value >= 10
                                        DefenderType.DWARVEN_MINE -> true
                                        else -> false
                                    }
                                val key2Label =
                                    when (selectedDefenderForHints.type) {
                                        DefenderType.WIZARD_TOWER, DefenderType.ALTAR -> stringResource(Res.string.magical_trap)
                                        DefenderType.DWARVEN_MINE -> stringResource(Res.string.trap)
                                        else -> ""
                                    }
                                if (showKey1 || showKey2) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (showKey1) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                ShortcutKeyChip(text = "1")
                                                Text(key1Label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (showKey2) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                ShortcutKeyChip(text = "2")
                                                Text(key2Label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
