package de.egril.defender.ui.gameplay

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*
import de.egril.defender.ui.CheatCodeDialog
import de.egril.defender.ui.ReminderMessage
import de.egril.defender.ui.a11y.accessibilityVisualFilter
import de.egril.defender.ui.animations.CoinFlightOverlay
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.getGameplayUIScale
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.SpeakerHighIcon
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import de.egril.defender.ui.settings.isShortcutBindingPressed
import defender_of_egril.composeapp.generated.resources.*

private const val SOUND_CAPTION_DISPLAY_DURATION_MS = 2000L

@Composable
fun GamePlayScreen(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onAutoAttackAndEndTurn: () -> Unit, // Add auto-attack callback
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null, // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null, // Add cheat code callback
    onMineDig: ((Int) -> DigOutcome?)? = null, // Add mine dig callback
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null, // Add mine build trap callback
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)? = null, // Add wizard magical trap callback
    onWizardGenerateMana: ((Int) -> Boolean)? = null, // Add wizard mana generation callback
    onBuildBarricade: ((Int, Position) -> Boolean)? = null, // Add barricade building callback
    onRemoveBarricade: ((Position) -> Int)? = null, // Add barricade removal callback - returns coin refund
    cheatDigOutcome: DigOutcome? = null, // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null, // Callback to clear cheat dig outcome
    showPlatformInfo: Boolean = false, // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null, // Callback to clear platform info
    showCheatHelp: Boolean = false, // Show cheat code help screen
    onClearCheatHelp: (() -> Unit)? = null, // Callback to clear cheat help
    hasUnsavedChanges: (() -> Boolean)? = null, // Callback to check for unsaved changes
    specialActionsRemaining: List<DefenderType> = emptyList(), // List of defender types with remaining special actions
    onClearSpecialActionsWarning: (() -> Unit)? = null, // Callback to clear special actions warning
    reminderMessage: ReminderMessage? = null, // Time reminder message
    onClearReminderMessage: (() -> Unit)? = null, // Callback to clear reminder message
    // Magic panel parameters
    showMagicPanel: Boolean = false, // Show magic panel overlay
    playerStats: PlayerAbilities? = null, // Player stats for spell list
    selectedSpell: SpellType? = null, // Currently selected spell (highlighted with border)
    onOpenMagicPanel: (() -> Unit)? = null, // Callback to open magic panel
    onCloseMagicPanel: (() -> Unit)? = null, // Callback to close magic panel
    onCastSpell: ((SpellType) -> Unit)? = null, // Callback to cast/select spell
    onCancelInstantTowerSpell: (() -> Unit)? = null, // Callback to cancel instant tower spell (abort dialog)
    pendingSpellCast: SpellType? = null, // Spell awaiting confirmation
    onConfirmSpellCast: (() -> Unit)? = null, // Callback to confirm spell cast
    onCancelSpellCast: (() -> Unit)? = null, // Callback to cancel spell cast
    onSelectSpellTarget: ((Any) -> Unit)? = null, // Callback to select spell target
    onExitSpellTargeting: (() -> Unit)? = null, // Callback to exit targeting mode
    // Post-target confirmation dialogs
    showSpellTargetConfirmation: Pair<SpellType, Any>? = null, // Show confirmation after target selected
    onConfirmTargetSpell: (() -> Unit)? = null, // Callback to confirm spell on target
    onDismissTargetConfirmation: (() -> Unit)? = null, // Callback to dismiss target confirmation
    showFreezeImmuneWarning: de.egril.defender.model.Attacker? = null, // Show warning for immune enemy
    onDismissFreezeWarning: (() -> Unit)? = null, // Callback to dismiss freeze warning
    scrollToPosition: de.egril.defender.model.Position? = null, // Scroll map to position (e.g. bomb explosion)
    onScrollToPositionConsumed: (() -> Unit)? = null, // Callback after scroll consumed
    pendingGameMessage: de.egril.defender.model.GameMessage? = null, // In-game event message (target taken, gate destroyed)
    onDismissGameMessage: (() -> Unit)? = null, // Callback to dismiss current message and show next
    isDemoMode: Boolean = false, // True when demo mode is active
    onStopDemoMode: (() -> Unit)? = null, // Callback to stop demo mode
    demoSelectedDefenderType: DefenderType? = null,
    demoHoveredPosition: Position? = null,
    demoSelectedDefenderId: Int? = null,
    demoSelectedTargetPosition: Position? = null,
    onGetAutoAttackTarget: ((Int) -> Position?)? = null, // Get best auto-attack target for a tower
    onWinLevelNow: (() -> Unit)? = null, // Instantly win the level when guaranteed (finish level fast)
    onPlaceSupportObject: ((SupportObjectType, Position) -> Boolean)? = null, // Place a level support object at a position
    onPlaceSupportFief: ((de.egril.defender.model.FiefType, Position) -> Boolean)? = null, // Place a level support fief token at a position
    onCastSupportSpellToken: ((SpellType) -> Unit)? = null, // Start casting a level support spell token (no mana cost)
    activeSpellToken: SpellType? = null, // Currently active support spell token (highlighted in support bar)
    onActivateCooldownPower: ((de.egril.defender.model.CooldownPowerType) -> Unit)? = null, // Activate a cooldown-based support power
    onSandboxSpawnEnemy: ((de.egril.defender.model.AttackerType, Int, de.egril.defender.model.Position?) -> Unit)? = null, // Sandbox: spawn an adjustable test enemy at an optional spawn point
    onSandboxAddCoins: (() -> Unit)? = null, // Sandbox: add coins
    onSandboxPaintTile: ((de.egril.defender.model.Position, de.egril.defender.editor.TileType, de.egril.defender.model.RiverFlow, Int) -> Unit)? = null, // Sandbox: repaint a map tile (with river flow when RIVER)
) {
    GamePlayScreenContent(
        gameState = gameState,
        onPlaceDefender = onPlaceDefender,
        onUpgradeDefender = onUpgradeDefender,
        onUndoTower = onUndoTower,
        onSellTower = onSellTower,
        onStartFirstPlayerTurn = onStartFirstPlayerTurn,
        onDefenderAttack = onDefenderAttack,
        onDefenderAttackPosition = onDefenderAttackPosition,
        onEndPlayerTurn = onEndPlayerTurn,
        onAutoAttackAndEndTurn = onAutoAttackAndEndTurn,
        onBackToMap = onBackToMap,
        onSaveGame = onSaveGame,
        onCheatCode = onCheatCode,
        onMineDig = onMineDig,
        onMineBuildTrap = onMineBuildTrap,
        onWizardPlaceMagicalTrap = onWizardPlaceMagicalTrap,
        onWizardGenerateMana = onWizardGenerateMana,
        onBuildBarricade = onBuildBarricade,
        onRemoveBarricade = onRemoveBarricade,
        cheatDigOutcome = cheatDigOutcome,
        onClearCheatDigOutcome = onClearCheatDigOutcome,
        showPlatformInfo = showPlatformInfo,
        onClearPlatformInfo = onClearPlatformInfo,
        showCheatHelp = showCheatHelp,
        onClearCheatHelp = onClearCheatHelp,
        hasUnsavedChanges = hasUnsavedChanges,
        specialActionsRemaining = specialActionsRemaining,
        onClearSpecialActionsWarning = onClearSpecialActionsWarning,
        reminderMessage = reminderMessage,
        onClearReminderMessage = onClearReminderMessage,
        showMagicPanel = showMagicPanel,
        playerStats = playerStats,
        selectedSpell = selectedSpell,
        onOpenMagicPanel = onOpenMagicPanel,
        onCloseMagicPanel = onCloseMagicPanel,
        onCastSpell = onCastSpell,
        onCancelInstantTowerSpell = onCancelInstantTowerSpell,
        pendingSpellCast = pendingSpellCast,
        onConfirmSpellCast = onConfirmSpellCast,
        onCancelSpellCast = onCancelSpellCast,
        onSelectSpellTarget = onSelectSpellTarget,
        onExitSpellTargeting = onExitSpellTargeting,
        showSpellTargetConfirmation = showSpellTargetConfirmation,
        onConfirmTargetSpell = onConfirmTargetSpell,
        onDismissTargetConfirmation = onDismissTargetConfirmation,
        showFreezeImmuneWarning = showFreezeImmuneWarning,
        onDismissFreezeWarning = onDismissFreezeWarning,
        scrollToPosition = scrollToPosition,
        onScrollToPositionConsumed = onScrollToPositionConsumed,
        pendingGameMessage = pendingGameMessage,
        onDismissGameMessage = onDismissGameMessage,
        isDemoMode = isDemoMode,
        onStopDemoMode = onStopDemoMode,
        demoSelectedDefenderType = demoSelectedDefenderType,
        demoHoveredPosition = demoHoveredPosition,
        demoSelectedDefenderId = demoSelectedDefenderId,
        demoSelectedTargetPosition = demoSelectedTargetPosition,
        onGetAutoAttackTarget = onGetAutoAttackTarget,
        onWinLevelNow = onWinLevelNow,
        onPlaceSupportObject = onPlaceSupportObject,
        onPlaceSupportFief = onPlaceSupportFief,
        onCastSupportSpellToken = onCastSupportSpellToken,
        activeSpellToken = activeSpellToken,
        onActivateCooldownPower = onActivateCooldownPower,
        onSandboxSpawnEnemy = onSandboxSpawnEnemy,
        onSandboxAddCoins = onSandboxAddCoins,
        onSandboxPaintTile = onSandboxPaintTile,
    )
}

@Composable
private fun GamePlayScreenContent(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onAutoAttackAndEndTurn: () -> Unit, // Add auto-attack callback
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null, // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null,
    onMineDig: ((Int) -> DigOutcome?)? = null,
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null,
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)? = null, // Add wizard magical trap callback
    onWizardGenerateMana: ((Int) -> Boolean)? = null, // Add wizard mana generation callback
    onBuildBarricade: ((Int, Position) -> Boolean)? = null, // Add barricade building callback
    onRemoveBarricade: ((Position) -> Int)? = null, // Add barricade removal callback - returns coin refund
    cheatDigOutcome: DigOutcome? = null, // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null, // Callback to clear cheat dig outcome
    showPlatformInfo: Boolean = false, // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null, // Callback to clear platform info
    showCheatHelp: Boolean = false, // Show cheat code help screen
    onClearCheatHelp: (() -> Unit)? = null, // Callback to clear cheat help
    hasUnsavedChanges: (() -> Boolean)? = null, // Callback to check for unsaved changes
    specialActionsRemaining: List<DefenderType> = emptyList(), // List of defender types with remaining special actions
    onClearSpecialActionsWarning: (() -> Unit)? = null, // Callback to clear special actions warning
    reminderMessage: ReminderMessage? = null, // Time reminder message
    onClearReminderMessage: (() -> Unit)? = null, // Callback to clear reminder message
    // Magic panel parameters
    showMagicPanel: Boolean = false,
    playerStats: PlayerAbilities? = null,
    selectedSpell: SpellType? = null,
    onOpenMagicPanel: (() -> Unit)? = null,
    onCloseMagicPanel: (() -> Unit)? = null,
    onCastSpell: ((SpellType) -> Unit)? = null,
    onCancelInstantTowerSpell: (() -> Unit)? = null,
    pendingSpellCast: SpellType? = null,
    onConfirmSpellCast: (() -> Unit)? = null,
    onCancelSpellCast: (() -> Unit)? = null,
    onSelectSpellTarget: ((Any) -> Unit)? = null,
    onExitSpellTargeting: (() -> Unit)? = null,
    showSpellTargetConfirmation: Pair<SpellType, Any>? = null,
    onConfirmTargetSpell: (() -> Unit)? = null,
    onDismissTargetConfirmation: (() -> Unit)? = null,
    showFreezeImmuneWarning: de.egril.defender.model.Attacker? = null,
    onDismissFreezeWarning: (() -> Unit)? = null,
    scrollToPosition: de.egril.defender.model.Position? = null,
    onScrollToPositionConsumed: (() -> Unit)? = null,
    pendingGameMessage: de.egril.defender.model.GameMessage? = null, // In-game event message (target taken, gate destroyed)
    onDismissGameMessage: (() -> Unit)? = null, // Callback to dismiss current message and show next
    isDemoMode: Boolean = false, // True when demo mode is active
    onStopDemoMode: (() -> Unit)? = null, // Callback to stop demo mode
    // Demo visual state – drives placement preview and attack aiming in the UI
    demoSelectedDefenderType: DefenderType? = null,
    demoHoveredPosition: Position? = null,
    demoSelectedDefenderId: Int? = null,
    demoSelectedTargetPosition: Position? = null,
    onGetAutoAttackTarget: ((Int) -> Position?)? = null, // Get best auto-attack target for a tower
    onWinLevelNow: (() -> Unit)? = null, // Instantly win the level when guaranteed (finish level fast)
    onPlaceSupportObject: ((SupportObjectType, Position) -> Boolean)? = null, // Place a level support object at a position
    onPlaceSupportFief: ((de.egril.defender.model.FiefType, Position) -> Boolean)? = null, // Place a level support fief token at a position
    onCastSupportSpellToken: ((SpellType) -> Unit)? = null, // Start casting a level support spell token (no mana cost)
    activeSpellToken: SpellType? = null, // Currently active support spell token (highlighted in support bar)
    onActivateCooldownPower: ((de.egril.defender.model.CooldownPowerType) -> Unit)? = null, // Activate a cooldown-based support power
    onSandboxSpawnEnemy: ((de.egril.defender.model.AttackerType, Int, de.egril.defender.model.Position?) -> Unit)? = null, // Sandbox: spawn an adjustable test enemy at an optional spawn point
    onSandboxAddCoins: (() -> Unit)? = null, // Sandbox: add coins
    onSandboxPaintTile: ((de.egril.defender.model.Position, de.egril.defender.editor.TileType, de.egril.defender.model.RiverFlow, Int) -> Unit)? = null, // Sandbox: repaint a map tile (with river flow when RIVER)
) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedSupportObject by remember { mutableStateOf<SupportObjectType?>(null) }
    var selectedSupportFief by remember { mutableStateOf<de.egril.defender.model.FiefType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    var selectedAttackerId by remember { mutableStateOf<Int?>(null) } // Add enemy selection
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetPosition by remember { mutableStateOf<Position?>(null) }
    var showCheatDialog by remember { mutableStateOf(false) }
    var showSandboxTools by remember { mutableStateOf(false) }
    // Sandbox: active map tile-paint type; when non-null, tapping a tile repaints it to this type.
    var sandboxPaintTileType by remember { mutableStateOf<de.egril.defender.editor.TileType?>(null) }
    // Sandbox: water flow direction/speed applied when painting RIVER tiles.
    var sandboxRiverFlow by remember { mutableStateOf(de.egril.defender.model.RiverFlow.EAST) }
    var sandboxRiverSpeed by remember { mutableStateOf(1) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var showMineActionDialog by remember { mutableStateOf(false) }
    var selectedMineAction by remember { mutableStateOf<MineAction?>(null) }
    var selectedWizardAction by remember { mutableStateOf<WizardAction?>(null) } // For wizard magical trap placement
    var selectedBarricadeAction by remember { mutableStateOf<BarricadeAction?>(null) } // For spike/spear tower barricade placement

    // Removal confirmation dialog states
    var showRemoveBarricadeDialog by remember { mutableStateOf(false) }
    var barricadeToRemove by remember { mutableStateOf<Position?>(null) }
    var selectedBarricadePosition by remember { mutableStateOf<Position?>(null) } // For barricade info panel
    var showRemoveTrapDialog by remember { mutableStateOf(false) }
    var trapToRemove by remember { mutableStateOf<Position?>(null) }

    var highlightEndTurnButton by remember { mutableStateOf(false) }
    var splitSelectorToggle by remember { mutableStateOf(0) }
    var splitSelectorExpanded by remember { mutableStateOf(false) }
    var tabScrollPosition by remember { mutableStateOf<Position?>(null) } // Tab-triggered scroll-to-tower
    var keyboardSelectedBuildTile by remember { mutableStateOf<Position?>(null) }
    // Position of the keyboard cursor while placing a support object or targeting a spell on the map.
    // The cursor cycles through the valid placement/target tiles (supportObjectPlacementTiles /
    // spellTargetPositions); the place key confirms the highlighted tile and a cancel key exits.
    var keyboardPlacementTile by remember { mutableStateOf<Position?>(null) }
    var nextSpawnPointIndex by remember { mutableStateOf(0) }
    var keyboardSpellFocusIndex by remember { mutableStateOf(0) }
    // Index (into visibleSupportSlots) of the support box under the keyboard-navigation cursor, or
    // null while the support bar is not being navigated. Left/right move the cursor, a select key
    // activates the focused box.
    var supportFocusIndex by remember { mutableStateOf<Int?>(null) }
    // Non-null means the keyboard undo/sell shortcut was pressed and the confirmation dialog is open.
    // The Boolean flag indicates whether this is an "undo" (true) or "sell" (false) operation.
    var keyboardUndoOrSellConfirmation by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    // External dialog triggers for header icon shortcuts
    var triggerShowShortcuts by remember { mutableStateOf(false) }
    var triggerShowHelp by remember { mutableStateOf(false) }
    var triggerShowFeedback by remember { mutableStateOf(false) }
    var triggerShowSettings by remember { mutableStateOf(false) }

    var currentDigOutcome by remember { mutableStateOf<DigOutcome?>(null) }
    var currentDragonName by remember { mutableStateOf<String?>(null) } // Track dragon name for dig outcome
    var showDigOutcomeDialog by remember { mutableStateOf(false) }
    // Pending dig outcome waiting for the mine-dig animation to finish (1.5 s when animations ON)
    var pendingDigOutcome by remember { mutableStateOf<DigOutcome?>(null) }
    var pendingDigKey by remember { mutableStateOf(0) }
    LaunchedEffect(pendingDigKey) {
        val outcome = pendingDigOutcome ?: return@LaunchedEffect
        if (AppSettings.enableAnimations.value) {
            kotlinx.coroutines.delay(1500L)
        }
        currentDigOutcome = outcome
        showDigOutcomeDialog = true
        pendingDigOutcome = null
    }
    var showOverlay by remember { mutableStateOf(false) } // MutableState for overlay visibility
    // 0 = both legend+enemies, 1 = legend only, 2 = enemy list only
    var overlayMode by remember { mutableStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) } // Save dialog with comment
    var saveCommentInput by remember { mutableStateOf("") } // Comment input for save
    var showSaveConfirmation by remember { mutableStateOf(false) } // Save confirmation
    var showUnsavedChangesDialog by remember { mutableStateOf(false) } // Unsaved changes dialog
    var showEndTurnConfirmation by remember { mutableStateOf(false) } // End turn confirmation dialog
    var showAbortInstantTowerDialog by remember { mutableStateOf(false) } // Abort instant tower spell dialog
    var soundCaptionText by remember { mutableStateOf<String?>(null) }
    var soundCaptionSequence by remember { mutableStateOf(0L) }

    // Wrap end-turn callbacks to always clear the selected tower type when the turn ends
    val endPlayerTurnAction: () -> Unit = {
        selectedDefenderType = null
        onEndPlayerTurn()
    }
    val autoAttackAndEndTurnAction: () -> Unit = {
        selectedDefenderType = null
        onAutoAttackAndEndTurn()
    }

    // Demo mode: "stop demo?" confirmation dialog
    var showStopDemoDialog by remember { mutableStateOf(false) }

    // Focus requester for the screen container - ensures keyboard events (especially ESC) always work
    val screenFocusRequester = remember { FocusRequester() }

    // Counter to trigger map focus requests after dialogs close (ensures ESC works after UI interactions)
    var mapRefocusTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(showUnsavedChangesDialog, showEndTurnConfirmation, keyboardUndoOrSellConfirmation) {
        if (!showUnsavedChangesDialog && !showEndTurnConfirmation && keyboardUndoOrSellConfirmation == null) {
            mapRefocusTrigger++
        }
    }

    // Request focus on screen entry and after dialogs close (mapRefocusTrigger changes)
    LaunchedEffect(Unit, mapRefocusTrigger) {
        screenFocusRequester.requestFocus()
    }

    // When demo mode visual state changes, sync it into the local selection state so
    // the existing rendering code (GameGrid, GameControls) shows the preview without any changes.
    if (isDemoMode) {
        LaunchedEffect(demoSelectedDefenderType) { selectedDefenderType = demoSelectedDefenderType }
        LaunchedEffect(demoSelectedDefenderId) { selectedDefenderId = demoSelectedDefenderId }
        LaunchedEffect(demoSelectedTargetPosition) { selectedTargetPosition = demoSelectedTargetPosition }
    }
    // When demo mode ends, clear any leftover selection state so normal play starts clean
    LaunchedEffect(isDemoMode) {
        if (!isDemoMode) {
            selectedDefenderType = null
            selectedDefenderId = null
            selectedTargetPosition = null
        }
    }

    // Check if unsaved changes feature is enabled (both hasUnsavedChanges and onSaveGame must be available)
    val unsavedChangesEnabled = hasUnsavedChanges != null && onSaveGame != null

    // Get platform-specific UI scale for mobile (affects layout, not just rendering)
    val uiScale = getGameplayUIScale()

    // Create a scaled density with separate scaling for layout (dp) and text (sp)
    // Layout elements (padding, spacing) scaled to 0.5x to save space
    // Text/icons scaled to 1.5x (doubled from 0.75x) for better readability on mobile
    val density = LocalDensity.current
    val scaledDensity =
        remember(density, uiScale) {
            // Scale layout (dp) by uiScale, but scale text (sp) to be larger
            val textScale =
                if (uiScale < 1f) {
                    // For mobile, use 1.5x for text (doubled from previous 0.75x)
                    // This means text will be at ~original size despite layout being 0.5x
                    1.5f
                } else {
                    1f // Desktop unchanged
                }
            Density(density.density * uiScale, density.fontScale * textScale)
        }

    val audioCaptionEnemySpawn = stringResource(Res.string.audio_enemy_spawn_name)
    val audioCaptionEnemyDestroyed = stringResource(Res.string.audio_enemy_destroyed_name)
    val audioCaptionTrapTrigger = stringResource(Res.string.audio_trap_trigger_name)
    val audioCaptionLifeLost = stringResource(Res.string.audio_life_lost_name)
    val audioCaptionTowerUpgraded = stringResource(Res.string.audio_tower_upgraded_name)
    val audioCaptionBattleStart = stringResource(Res.string.audio_battle_start_name)
    val audioCaptionBombTicking = stringResource(Res.string.audio_bomb_ticking_name)
    val audioCaptionBombExploding = stringResource(Res.string.audio_bomb_exploding_name)
    val captionsEnabled = AppSettings.captionsEnabled.value

    LaunchedEffect(captionsEnabled) {
        if (!captionsEnabled) {
            soundCaptionText = null
            return@LaunchedEffect
        }
        GlobalSoundManager.soundEvents.collect { event ->
            val caption =
                when (event) {
                    SoundEvent.ENEMY_SPAWN -> audioCaptionEnemySpawn
                    SoundEvent.ENEMY_DESTROYED -> audioCaptionEnemyDestroyed
                    SoundEvent.TRAP_TRIGGERED -> audioCaptionTrapTrigger
                    SoundEvent.LIFE_LOST -> audioCaptionLifeLost
                    SoundEvent.TOWER_UPGRADED -> audioCaptionTowerUpgraded
                    SoundEvent.BATTLE_START -> audioCaptionBattleStart
                    SoundEvent.BOMB_TICKING -> audioCaptionBombTicking
                    SoundEvent.BOMB_EXPLOSION -> audioCaptionBombExploding
                    else -> null
                }
            if (caption != null) {
                soundCaptionText = caption
                soundCaptionSequence++
            }
        }
    }

    LaunchedEffect(soundCaptionSequence) {
        if (soundCaptionSequence <= 0L) return@LaunchedEffect
        val currentSequence = soundCaptionSequence
        kotlinx.coroutines.delay(SOUND_CAPTION_DISPLAY_DURATION_MS)
        if (soundCaptionSequence == currentSequence) {
            soundCaptionText = null
        }
    }

    // Watch for cheat dig outcomes
    LaunchedEffect(cheatDigOutcome) {
        if (cheatDigOutcome != null) {
            currentDigOutcome = cheatDigOutcome
            showDigOutcomeDialog = true
            onClearCheatDigOutcome?.invoke()
        }
    }

    // Background music management based on health points
    LaunchedEffect(gameState.healthPoints.value) {
        val currentMusic =
            de.egril.defender.audio.GlobalBackgroundMusicManager
                .getInstance()
                ?.getCurrentMusic()

        if (gameState.healthPoints.value < 5) {
            // Switch to low health music if not already playing
            if (currentMusic != de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH) {
                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                    de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH,
                    loop = true,
                )
            }
        } else {
            // Play normal gameplay music if not already playing
            if (currentMusic != de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL) {
                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                    de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL,
                    loop = true,
                )
            }
        }
    }

    // Stop background music when leaving gameplay
    DisposableEffect(Unit) {
        onDispose {
            de.egril.defender.audio.GlobalBackgroundMusicManager
                .stopMusic()
        }
    }

    // Check for dragon-related infos and show appropriate tutorial
    LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }

        // Check for dragons and show appropriate info
        val dragons = gameState.attackers.filter { it.type.isDragon && !it.isDefeated.value }

        if (dragons.isNotEmpty()) {
            // Priority: Very greedy > Greed > Dragon info
            val veryGreedyDragon = dragons.any { it.greed > 5 }
            val greedyDragon = dragons.any { it.greed > 0 }

            when {
                veryGreedyDragon && !infoState.hasSeen(InfoType.VERY_GREEDY_INFO) -> {
                    gameState.infoState.value = infoState.showInfo(InfoType.VERY_GREEDY_INFO)
                }
                greedyDragon && !infoState.hasSeen(InfoType.GREED_INFO) -> {
                    gameState.infoState.value = infoState.showInfo(InfoType.GREED_INFO)
                }
                !infoState.hasSeen(InfoType.DRAGON_INFO) -> {
                    gameState.infoState.value = infoState.showInfo(InfoType.DRAGON_INFO)
                }
            }
        }
    }

    // Witch info popups
    LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }

        // Check for green witches on the field
        val greenWitches =
            gameState.attackers.filter {
                it.type == AttackerType.GREEN_WITCH && !it.isDefeated.value
            }
        if (greenWitches.isNotEmpty() && !infoState.hasSeen(InfoType.GREEN_WITCH_INFO)) {
            gameState.infoState.value = infoState.showInfo(InfoType.GREEN_WITCH_INFO)
            return@LaunchedEffect
        }

        // Check for red witches on the field
        val redWitches =
            gameState.attackers.filter {
                it.type == AttackerType.RED_WITCH && !it.isDefeated.value
            }
        if (redWitches.isNotEmpty() && !infoState.hasSeen(InfoType.RED_WITCH_INFO)) {
            gameState.infoState.value = infoState.showInfo(InfoType.RED_WITCH_INFO)
        }
    }

    // Check for mine warnings
    LaunchedEffect(gameState.mineWarnings.size, gameState.infoState.value) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }

        // Show mine warning if there are warnings and not currently showing one
        if (gameState.mineWarnings.isNotEmpty()) {
            val mineId = gameState.mineWarnings.first()
            gameState.infoState.value = infoState.showInfo(InfoType.MINE_WARNING, mineId)
        }
    }

    // Check for 1 HP warning at the start of the level
    LaunchedEffect(gameState.level.healthPoints) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info or already seen
        if (infoState.currentInfo != InfoType.NONE || infoState.hasSeen(InfoType.ONE_HP_WARNING)) {
            return@LaunchedEffect
        }

        // Show warning if level starts with only 1 HP
        if (gameState.level.healthPoints == 1) {
            gameState.infoState.value = infoState.showInfo(InfoType.ONE_HP_WARNING)
        }
    }

    // Check for first-time special tower availability and show combined info dialog
    LaunchedEffect(gameState.level.availableTowers, gameState.infoState.value) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }

        // Check if level has special towers and we haven't shown the info yet
        val specialTowers =
            gameState.level.availableTowers.filter {
                it in listOf(DefenderType.WIZARD_TOWER, DefenderType.ALCHEMY_TOWER, DefenderType.BALLISTA_TOWER, DefenderType.DWARVEN_MINE)
            }

        if (specialTowers.isNotEmpty() && !infoState.hasSeen(InfoType.SPECIAL_TOWERS_INFO)) {
            gameState.infoState.value = infoState.showInfo(InfoType.SPECIAL_TOWERS_INFO)
        }
    }

    // Check for river tiles and show river mechanics info (first time only)
    LaunchedEffect(gameState.level.id) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info or already seen river info
        if (infoState.currentInfo != InfoType.NONE || infoState.hasSeen(InfoType.RIVER_INFO)) {
            return@LaunchedEffect
        }

        // Show info if level has river tiles
        if (gameState.level.riverTiles.isNotEmpty()) {
            gameState.infoState.value = infoState.showInfo(InfoType.RIVER_INFO)
        }
    }

    // Helper: select the next (or previous if reversed) actionable tower, scroll to it, and
    // pre-select its best auto-attack target. If no actionable tower exists, visually highlight
    // the End Turn button (do NOT actually end the turn).
    val jumpToNextActionableTower: (Int?, Boolean) -> Unit = { currentId, reversed ->
        val actionable = gameState.getActionableTowersForTab()
        if (actionable.isEmpty()) {
            // No tower with actions left → highlight the End Turn button (keyboard focus)
            highlightEndTurnButton = true
        } else {
            highlightEndTurnButton = false
            val currentIdx = actionable.indexOfFirst { it.id == currentId }
            val nextIdx =
                if (reversed) {
                    if (currentIdx <= 0) actionable.size - 1 else currentIdx - 1
                } else {
                    if (currentIdx < 0 || currentIdx >= actionable.size - 1) 0 else currentIdx + 1
                }
            val nextTower = actionable[nextIdx]
            selectedDefenderId = nextTower.id
            selectedAttackerId = null
            selectedMineAction = null
            selectedWizardAction = null
            selectedBarricadeAction = null
            tabScrollPosition = nextTower.position.value

            // Auto-select the best attack target using the same logic as automatic attacks
            val autoTarget = onGetAutoAttackTarget?.invoke(nextTower.id)
            if (autoTarget != null) {
                selectedTargetPosition = autoTarget
                selectedTargetId =
                    gameState.attackers
                        .find {
                            !it.isDefeated.value && it.position.value == autoTarget
                        }?.id
            } else {
                selectedTargetId = null
                selectedTargetPosition = null
            }
        }
    }

    val keyboardSelectableTowers =
        remember(gameState.level.availableTowers) {
            gameState.level.availableTowers.filter { it != DefenderType.DRAGONS_LAIR }
        }

    val cycleTowerBuySelection: (Boolean) -> Unit = cycleTowerBuySelection@{ reversed ->
        // Filter to only affordable towers when cycling
        val affordableTowers = keyboardSelectableTowers.filter { gameState.coins.value >= it.baseCost }
        if (affordableTowers.isEmpty()) {
            return@cycleTowerBuySelection
        }
        val currentIndex =
            selectedDefenderType?.let { current ->
                affordableTowers.indexOf(current).takeIf { it != -1 }
            } ?: -1
        val nextIndex =
            if (reversed) {
                if (currentIndex <= 0) affordableTowers.lastIndex else currentIndex - 1
            } else {
                if (currentIndex < 0 || currentIndex >= affordableTowers.lastIndex) 0 else currentIndex + 1
            }
        selectedDefenderType = affordableTowers[nextIndex]
        selectedDefenderId = null
        selectedAttackerId = null
        highlightEndTurnButton = false
    }

    val handleTowerSelectionShortcut: (Boolean) -> Unit = { reversed ->
        if (gameState.phase.value == GamePhase.INITIAL_BUILDING) {
            cycleTowerBuySelection(reversed)
        } else if (gameState.phase.value == GamePhase.PLAYER_TURN) {
            if (selectedDefenderType != null && selectedDefenderId == null && !showMagicPanel) {
                cycleTowerBuySelection(reversed)
            } else {
                jumpToNextActionableTower(selectedDefenderId, reversed)
            }
        }
    }

    val keyboardSelectableSpells =
        remember(playerStats) {
            SpellType.entries.filter { spell ->
                playerStats?.unlockedSpells?.contains(spell) == true
            }
        }
    LaunchedEffect(showMagicPanel, selectedSpell, keyboardSelectableSpells) {
        if (!showMagicPanel || keyboardSelectableSpells.isEmpty()) {
            keyboardSpellFocusIndex = 0
            return@LaunchedEffect
        }
        val currentIndex = keyboardSelectableSpells.indexOf(selectedSpell)
        keyboardSpellFocusIndex = if (currentIndex >= 0) currentIndex else 0
    }

    // Auto-jump: select first actionable tower when player turn starts (if setting is ON)
    LaunchedEffect(gameState.phase.value) {
        if (AppSettings.autoJumpToNextTower.value &&
            gameState.phase.value == GamePhase.PLAYER_TURN
        ) {
            jumpToNextActionableTower(null, false)
        }
    }

    // Auto-jump: when the current selected tower runs out of actions, jump to the next (if setting is ON).
    // Use SideEffect to track the actions-remaining value from the previous composition so that we
    // only jump on the transition (> 0) → 0 for the *same* tower, and ignore manually selected
    // towers that already have 0 actions when selected.
    val selectedDefenderActionsRemaining =
        selectedDefenderId?.let { id ->
            gameState.defenders
                .find { it.id == id }
                ?.actionsRemaining
                ?.value
        }
    val prevAutoJumpDefenderId = remember { mutableStateOf<Int?>(null) }
    val prevAutoJumpActions = remember { mutableStateOf<Int?>(null) }
    val actionsJustExhausted =
        selectedDefenderId != null &&
            selectedDefenderId == prevAutoJumpDefenderId.value &&
            selectedDefenderActionsRemaining == 0 &&
            (prevAutoJumpActions.value ?: 0) > 0
    SideEffect {
        prevAutoJumpDefenderId.value = selectedDefenderId
        prevAutoJumpActions.value = selectedDefenderActionsRemaining
    }
    LaunchedEffect(actionsJustExhausted) {
        if (actionsJustExhausted &&
            AppSettings.autoJumpToNextTower.value &&
            gameState.phase.value == GamePhase.PLAYER_TURN
        ) {
            jumpToNextActionableTower(selectedDefenderId, false)
        }
    }

    // Clear End Turn button highlight when a defender is selected (manual map click or other means)
    LaunchedEffect(selectedDefenderId) {
        if (selectedDefenderId != null) {
            highlightEndTurnButton = false
        }
    }

    // Reset keyboard build tile selection when tower-place mode exits
    LaunchedEffect(selectedDefenderType) {
        if (selectedDefenderType == null) {
            keyboardSelectedBuildTile = null
        }
    }

    // Reset the keyboard placement/targeting cursor when neither a support object is selected nor a
    // spell is being targeted, so a stale cursor does not linger after placement finishes or is cancelled.
    LaunchedEffect(selectedSupportObject, selectedSupportFief, gameState.spellTargeting.value) {
        if (selectedSupportObject == null && selectedSupportFief == null && gameState.spellTargeting.value == null) {
            keyboardPlacementTile = null
        }
    }

    // Mine action handler
    val handleMineAction: (Int, MineAction) -> Unit = { mineId, action ->
        when (action) {
            MineAction.DIG -> {
                val outcome = onMineDig?.invoke(mineId)
                if (outcome != null) {
                    // If a dragon was spawned, find the newly created lair and get the dragon's name
                    if (outcome == DigOutcome.DRAGON) {
                        val newLair = gameState.defenders.lastOrNull { it.type == DefenderType.DRAGONS_LAIR }
                        currentDragonName = newLair?.dragonName
                    } else {
                        currentDragonName = null
                    }
                    // Show dig result after the mine-dig animation plays (1.5 s when animations ON)
                    pendingDigOutcome = outcome
                    pendingDigKey++
                }
            }

            MineAction.BUILD_TRAP -> {
                // Toggle trap placement mode - if already selected, deselect it
                selectedMineAction = if (selectedMineAction == action) null else action
                // Clear target selection when entering trap placement mode
                if (selectedMineAction != null) {
                    selectedTargetId = null
                    selectedTargetPosition = null
                }
                showMineActionDialog = true
            }
        }
    }

    // Wizard action handler - similar to mine action, click button first then select on map
    val handleWizardAction: (Int, WizardAction) -> Unit = { wizardId, action ->
        when (action) {
            WizardAction.GENERATE_MANA -> {
                // Generate mana - immediate action
                onWizardGenerateMana?.invoke(wizardId)
            }
            WizardAction.PLACE_MAGICAL_TRAP -> {
                // Toggle trap placement mode - if already selected, deselect it
                selectedWizardAction = if (selectedWizardAction == action) null else action
                // Clear target selection when entering trap placement mode
                if (selectedWizardAction != null) {
                    selectedTargetId = null
                    selectedTargetPosition = null
                }
                // The user will now click on the map to place the trap
            }
        }
    }

    // Barricade action handler - similar to wizard action, click button first then select on map
    val handleBarricadeAction: (Int, BarricadeAction) -> Unit = { towerId, action ->
        when (action) {
            BarricadeAction.BUILD_BARRICADE -> {
                // Toggle placement mode - if already selected, deselect it
                selectedBarricadeAction = if (selectedBarricadeAction == action) null else action
                // Clear target selection when entering barricade placement mode
                if (selectedBarricadeAction != null) {
                    selectedTargetId = null
                    selectedTargetPosition = null
                }
                // The user will now click on the map to place the barricade
            }
        }
    }

    // Keyboard event handler for shortcuts
    // Using onPreviewKeyEvent to intercept before HexagonalMapView handles it
    // This works in the "capture" phase and doesn't require focus on this element
    val keyboardHandler: (KeyEvent) -> Boolean =
        remember(
            onSaveGame,
            onCheatCode,
            onOpenMagicPanel,
            onCloseMagicPanel,
            onCastSpell,
            onExitSpellTargeting,
            onStartFirstPlayerTurn,
            onDefenderAttack,
            onDefenderAttackPosition,
            showMagicPanel,
            showUnsavedChangesDialog,
            keyboardSelectableSpells,
            endPlayerTurnAction,
            autoAttackAndEndTurnAction,
            isDemoMode,
        ) {
            { event ->
                when {
                    // In demo mode any key press shows "stop demo?" dialog
                    isDemoMode && event.type == KeyEventType.KeyDown -> {
                        showStopDemoDialog = true
                        true
                    }
                    // Unsaved-changes dialog key handling: intercept before generic Esc/Enter handlers
                    event.type == KeyEventType.KeyDown && showUnsavedChangesDialog -> {
                        when {
                            event.key == Key.Enter && !event.isCtrlPressed && !event.isAltPressed -> {
                                // Enter: save and exit
                                onSaveGame?.invoke(null)
                                showUnsavedChangesDialog = false
                                onBackToMap()
                                true
                            }
                            event.key == Key.Escape ||
                                isShortcutBindingPressed(event, AppSettings.shortcutBackToWorldMap.value) -> {
                                // Esc: cancel the dialog
                                showUnsavedChangesDialog = false
                                true
                            }
                            event.key == Key.D && !event.isCtrlPressed && !event.isAltPressed -> {
                                // D: discard changes and exit
                                showUnsavedChangesDialog = false
                                onBackToMap()
                                true
                            }
                            else -> false
                        }
                    }
                    // Ctrl+S: Save game
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSaveGame.value) &&
                        onSaveGame != null -> {
                        showSaveDialog = true
                        true
                    }
                    // M (remappable): Open/close spell menu
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutToggleSpellMenu.value) &&
                        onOpenMagicPanel != null &&
                        gameState.maxMana.value > 0 &&
                        !isDemoMode -> {
                        if (showMagicPanel) {
                            onCloseMagicPanel?.invoke()
                        } else {
                            onOpenMagicPanel.invoke()
                        }
                        true
                    }
                    // T (remappable): Toggle tower-place mode (close spell UI/targeting); if already
                    // in tower-place mode, switch back to attack/select mode instead
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSwitchToTowerMode.value) -> {
                        val isInTowerPlaceMode = selectedDefenderType != null && !showMagicPanel && gameState.spellTargeting.value == null
                        if (isInTowerPlaceMode) {
                            // Already in tower-place mode → switch back to attack/select mode
                            selectedDefenderType = null
                        } else {
                            // Enter tower-place mode (close spell UI/targeting first)
                            onCloseMagicPanel?.invoke()
                            if (gameState.spellTargeting.value != null) {
                                onExitSpellTargeting?.invoke()
                            }
                            selectedDefenderType = keyboardSelectableTowers.firstOrNull()
                            selectedDefenderId = null
                            selectedAttackerId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            // Leaving the support bar for tower-place mode: drop the support focus cursor.
                            supportFocusIndex = null
                        }
                        true
                    }
                    // B: Toggle split tower selector dropdown (only when split build button is active)
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.B &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        AppSettings.splitBuildTowerButton.value &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        splitSelectorToggle++
                        true
                    }
                    // Spell menu keyboard mode: Tab/Shift+Tab navigates spells
                    event.type == KeyEventType.KeyDown &&
                        showMagicPanel &&
                        event.key == Key.Tab &&
                        keyboardSelectableSpells.isNotEmpty() -> {
                        val delta = if (event.isShiftPressed) -1 else 1
                        val size = keyboardSelectableSpells.size
                        keyboardSpellFocusIndex = (keyboardSpellFocusIndex + delta + size) % size
                        true
                    }
                    // Spell menu keyboard mode: Enter selects/casts focused spell
                    event.type == KeyEventType.KeyDown &&
                        showMagicPanel &&
                        event.key == Key.Enter &&
                        keyboardSelectableSpells.isNotEmpty() &&
                        onCastSpell != null -> {
                        val focusedSpell = keyboardSelectableSpells[keyboardSpellFocusIndex]
                        onCastSpell.invoke(focusedSpell)
                        true
                    }
                    // Ctrl+A: Auto-attack all towers and end turn (player turn only)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutAutoAttackEndTurn.value) &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        autoAttackAndEndTurnAction()
                        true
                    }
                    // F: Attack with selected tower's current target (player turn only)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutAttackSelectedTarget.value) &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defenderId = selectedDefenderId
                        val targetId = selectedTargetId
                        val targetPosition = selectedTargetPosition
                        val defender = defenderId?.let { gameState.defenders.find { it.id == defenderId } }
                        if (defender != null && defender.isReady && defender.actionsRemaining.value > 0) {
                            when {
                                targetId != null -> {
                                    onDefenderAttack(defenderId, targetId)
                                    true
                                }
                                targetPosition != null -> {
                                    onDefenderAttackPosition(defenderId, targetPosition)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // U (remappable): Upgrade currently selected tower
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutUpgradeSelectedTower.value) &&
                        selectedDefenderId != null -> {
                        selectedDefenderId?.let { defenderId ->
                            onUpgradeDefender(defenderId)
                            true
                        } ?: false
                    }
                    // X (remappable): Undo (if eligible) or sell selected tower – always shows confirmation
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutUndoOrSellSelectedTower.value) &&
                        selectedDefenderId != null -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null) {
                            val canUndo = defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value
                            // Cannot sell while the Kraken has gripped this barge
                            val canSell = defender.isReady && defender.actionsRemaining.value > 0 && !defender.isGrippedByKraken.value
                            when {
                                canUndo -> {
                                    // Show confirmation dialog: isUndo = true (full refund)
                                    keyboardUndoOrSellConfirmation = Pair(defender.id, true)
                                    true
                                }
                                canSell -> {
                                    // Show confirmation dialog: isUndo = false (75% refund)
                                    keyboardUndoOrSellConfirmation = Pair(defender.id, false)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // Tab / Shift+Tab: Select next/previous actionable tower (player turn only)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSelectNextTower.value) &&
                        (
                            gameState.phase.value == GamePhase.INITIAL_BUILDING ||
                                gameState.phase.value == GamePhase.PLAYER_TURN
                        ) -> {
                        handleTowerSelectionShortcut(false)
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSelectPreviousTower.value) &&
                        (
                            gameState.phase.value == GamePhase.INITIAL_BUILDING ||
                                gameState.phase.value == GamePhase.PLAYER_TURN
                        ) -> {
                        handleTowerSelectionShortcut(true)
                        true
                    }
                    // Remappable: Center map on selected tower
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutCenterSelectedTower.value) -> {
                        val defender =
                            selectedDefenderId?.let { defenderId ->
                                gameState.defenders.find { it.id == defenderId }
                            }
                        if (defender != null) {
                            tabScrollPosition = defender.position.value
                            true
                        } else {
                            false
                        }
                    }
                    // Remappable: Center map on next spawn point
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutCenterNextSpawnPoint.value) -> {
                        val spawnPoints = gameState.level.startPositions
                        if (spawnPoints.isEmpty()) {
                            false
                        } else {
                            val nextPosition = spawnPoints[nextSpawnPointIndex % spawnPoints.size]
                            tabScrollPosition = nextPosition
                            nextSpawnPointIndex = (nextSpawnPointIndex + 1) % spawnPoints.size
                            true
                        }
                    }
                    // C: Open cheat code dialog
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutCheat.value) &&
                        onCheatCode != null -> {
                        showCheatDialog = true
                        true
                    }
                    // E: Toggle enemy list overlay (cycle: off → both → legend only → enemy list only → off)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutToggleEnemyList.value) -> {
                        if (!showOverlay) {
                            showOverlay = true
                            overlayMode = 0 // both
                        } else {
                            when (overlayMode) {
                                0 -> overlayMode = 1 // legend only
                                1 -> overlayMode = 2 // enemy list only
                                else -> {
                                    showOverlay = false
                                    overlayMode = 0
                                }
                            }
                        }
                        true
                    }
                    // /: Open keyboard shortcuts dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Slash &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed -> {
                        triggerShowShortcuts = true
                        true
                    }
                    // H: Open tutorials/help dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.H &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed &&
                        selectedDefenderId == null &&
                        selectedDefenderType == null -> {
                        triggerShowHelp = true
                        true
                    }
                    // Period (.): Open feedback dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Period &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed -> {
                        triggerShowFeedback = true
                        true
                    }
                    // Comma (,): Open settings dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Comma &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed -> {
                        triggerShowSettings = true
                        true
                    }
                    // P (remappable): Toggle audio on/off
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutToggleAudio.value) -> {
                        AppSettings.saveSoundEnabled(!AppSettings.isSoundEnabled.value)
                        true
                    }
                    // Reuses the "next enemy target" binding (default N) for cycling build tiles in tower-place mode
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value) &&
                        selectedDefenderType != null &&
                        !showMagicPanel &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val buildTiles =
                            gameState.level.buildAreas
                                .filter { pos -> gameState.defenders.none { it.position.value == pos } }
                        if (buildTiles.isNotEmpty()) {
                            val currentIdx = keyboardSelectedBuildTile?.let { buildTiles.indexOf(it).takeIf { i -> i != -1 } }
                            val nextIdx = if (currentIdx == null || currentIdx >= buildTiles.lastIndex) 0 else currentIdx + 1
                            keyboardSelectedBuildTile = buildTiles[nextIdx]
                            tabScrollPosition = buildTiles[nextIdx]
                            true
                        } else {
                            false
                        }
                    }
                    // Reuses the "prev enemy target" binding (default Shift+N) for cycling build tiles in tower-place mode
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutPrevEnemyTarget.value) &&
                        selectedDefenderType != null &&
                        !showMagicPanel &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val buildTiles =
                            gameState.level.buildAreas
                                .filter { pos -> gameState.defenders.none { it.position.value == pos } }
                        if (buildTiles.isNotEmpty()) {
                            val currentIdx = keyboardSelectedBuildTile?.let { buildTiles.indexOf(it).takeIf { i -> i != -1 } }
                            val prevIdx = if (currentIdx == null || currentIdx <= 0) buildTiles.lastIndex else currentIdx - 1
                            keyboardSelectedBuildTile = buildTiles[prevIdx]
                            tabScrollPosition = buildTiles[prevIdx]
                            true
                        } else {
                            false
                        }
                    }
                    // Move the keyboard placement/targeting cursor a whole grid row up/down while placing
                    // a support object or targeting a spell. Reuses the pan up/down bindings (W/S by
                    // default) — during placement the map already auto-follows the cursor, so these keys
                    // move between rows instead of panning. Complements the next/prev (row-major) stepping
                    // below. Placed before pan handling (onPreviewKeyEvent) so it takes precedence.
                    event.type == KeyEventType.KeyDown &&
                        (
                            isShortcutBindingPressed(event, AppSettings.shortcutPanUp.value) ||
                                isShortcutBindingPressed(event, AppSettings.shortcutPanDown.value)
                        ) &&
                        !showMagicPanel &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val candidateTiles =
                            selectedSupportObject?.let { supportObjectPlacementTiles(gameState, it) }
                                ?: selectedSupportFief?.let { supportFiefPlacementTiles(gameState, it) }
                                ?: spellTargetPositions(gameState)
                        if (candidateTiles.isEmpty()) {
                            false
                        } else {
                            val up = isShortcutBindingPressed(event, AppSettings.shortcutPanUp.value)
                            val target = rowStepPlacementTile(candidateTiles, keyboardPlacementTile, up)
                            if (target != null) {
                                keyboardPlacementTile = target
                                tabScrollPosition = target
                            }
                            // Consume the key even at the top/bottom edge so it never falls through to
                            // panning while a placement/targeting mode is active.
                            true
                        }
                    }
                    // Cycle the keyboard placement/targeting cursor over the valid tiles while placing a
                    // support object or targeting a spell (reuses the next/prev enemy-target bindings, as
                    // tower placement does for build tiles). Placed before the enemy-target handlers so it
                    // takes precedence whenever a placement/targeting mode is active.
                    event.type == KeyEventType.KeyDown &&
                        (
                            isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value) ||
                                isShortcutBindingPressed(event, AppSettings.shortcutPrevEnemyTarget.value)
                        ) &&
                        !showMagicPanel &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val candidateTiles =
                            selectedSupportObject?.let { supportObjectPlacementTiles(gameState, it) }
                                ?: selectedSupportFief?.let { supportFiefPlacementTiles(gameState, it) }
                                ?: spellTargetPositions(gameState)
                        if (candidateTiles.isEmpty()) {
                            false
                        } else {
                            val forward = isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value)
                            val currentIdx =
                                keyboardPlacementTile?.let { candidateTiles.indexOf(it).takeIf { i -> i != -1 } }
                            val nextIdx =
                                when {
                                    currentIdx == null -> if (forward) 0 else candidateTiles.lastIndex
                                    forward -> (currentIdx + 1) % candidateTiles.size
                                    else -> (currentIdx - 1 + candidateTiles.size) % candidateTiles.size
                                }
                            keyboardPlacementTile = candidateTiles[nextIdx]
                            tabScrollPosition = candidateTiles[nextIdx]
                            true
                        }
                    }
                    // N (remappable): Cycle to next reachable enemy target
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value) &&
                        selectedDefenderId != null &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null) {
                            val reachableEnemies =
                                gameState.attackers.filter { attacker ->
                                    !attacker.isDefeated.value &&
                                        defender.position.value.distanceTo(attacker.position.value) <= defender.range
                                }
                            if (reachableEnemies.isNotEmpty()) {
                                val currentIdx =
                                    selectedTargetId?.let { tid ->
                                        reachableEnemies.indexOfFirst { it.id == tid }
                                    } ?: -1
                                val nextIdx = (currentIdx + 1) % reachableEnemies.size
                                val nextEnemy = reachableEnemies[nextIdx]
                                selectedTargetId = nextEnemy.id
                                selectedTargetPosition = nextEnemy.position.value
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    // Shift+N (remappable): Cycle to previous reachable enemy target
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutPrevEnemyTarget.value) &&
                        selectedDefenderId != null &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null) {
                            val reachableEnemies =
                                gameState.attackers.filter { attacker ->
                                    !attacker.isDefeated.value &&
                                        defender.position.value.distanceTo(attacker.position.value) <= defender.range
                                }
                            if (reachableEnemies.isNotEmpty()) {
                                val currentIdx =
                                    selectedTargetId?.let { tid ->
                                        reachableEnemies.indexOfFirst { it.id == tid }
                                    } ?: 0
                                val prevIdx = (currentIdx - 1 + reachableEnemies.size) % reachableEnemies.size
                                val prevEnemy = reachableEnemies[prevIdx]
                                selectedTargetId = prevEnemy.id
                                selectedTargetPosition = prevEnemy.position.value
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    // Left / Right: Move the keyboard-focus cursor between support elements
                    // (placeable objects, spell tokens, cooldown powers) in the support bar.
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed &&
                        !showMagicPanel &&
                        (gameState.phase.value == GamePhase.PLAYER_TURN || gameState.phase.value == GamePhase.INITIAL_BUILDING) -> {
                        val slots = visibleSupportSlots(gameState)
                        if (slots.isEmpty()) {
                            false
                        } else {
                            supportFocusIndex =
                                nextSupportFocusIndex(
                                    current = supportFocusIndex,
                                    slotCount = slots.size,
                                    forward = event.key == Key.DirectionRight,
                                )
                            true
                        }
                    }
                    // Enter / Space: Activate (select / cast / trigger) the support element currently
                    // under the keyboard-focus cursor. Placed before the plain-Enter placement handler
                    // so it only intercepts Enter while a support box is focused.
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.Spacebar) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed &&
                        !showMagicPanel &&
                        supportFocusIndex != null &&
                        (gameState.phase.value == GamePhase.PLAYER_TURN || gameState.phase.value == GamePhase.INITIAL_BUILDING) -> {
                        val slots = visibleSupportSlots(gameState)
                        val slot = slots.getOrNull(supportFocusIndex ?: -1)
                        if (slot != null && isSupportSlotEnabled(gameState, slot, barEnabled = true)) {
                            when (slot) {
                                is SupportSlot.ObjectSlot -> {
                                    selectedSupportObject = if (selectedSupportObject == slot.type) null else slot.type
                                    selectedSupportFief = null
                                    selectedDefenderType = null
                                }
                                is SupportSlot.FiefSlot -> {
                                    selectedSupportFief = if (selectedSupportFief == slot.type) null else slot.type
                                    selectedSupportObject = null
                                    selectedDefenderType = null
                                }
                                is SupportSlot.SpellSlot -> {
                                    selectedSupportObject = null
                                    selectedSupportFief = null
                                    onCastSupportSpellToken?.invoke(slot.spell)
                                }
                                is SupportSlot.PowerSlot -> {
                                    selectedSupportObject = null
                                    selectedSupportFief = null
                                    onActivateCooldownPower?.invoke(slot.type)
                                }
                            }
                            // Drop the focus cursor after activating so a plain Enter afterwards
                            // confirms tower placement again instead of re-triggering this support.
                            supportFocusIndex = null
                            true
                        } else {
                            // Consume Enter/Space even when the focused box can't be used, so pressing
                            // select on a focused-but-disabled support doesn't fall through to the
                            // tower-placement / attack handlers bound to the same keys.
                            true
                        }
                    }
                    // Enter / Space: Confirm placing the selected support object, or casting the spell
                    // being targeted, on the keyboard placement cursor tile (falling back to the first
                    // valid tile). Placed before the tower-placement Enter handler so it wins whenever a
                    // support object is selected or a spell is being targeted.
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.Spacebar) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isMetaPressed &&
                        !showMagicPanel &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val supportType = selectedSupportObject
                        val fiefType = selectedSupportFief
                        if (supportType != null) {
                            val candidateTiles = supportObjectPlacementTiles(gameState, supportType)
                            val tile =
                                keyboardPlacementTile?.takeIf { candidateTiles.contains(it) }
                                    ?: candidateTiles.firstOrNull()
                            if (tile != null && onPlaceSupportObject?.invoke(supportType, tile) == true) {
                                val remaining = gameState.supportObjectsRemaining[supportType] ?: 0
                                if (remaining <= 0) {
                                    selectedSupportObject = null
                                    keyboardPlacementTile = null
                                } else {
                                    // Keep the placement cursor near the tile just used instead of
                                    // resetting it to the first tile, so consecutive placements stay
                                    // in the same area of the map. The placed tile is now occupied and
                                    // dropped from the list, so the same (clamped) index lands on the
                                    // next remaining tile.
                                    val placedIndex = candidateTiles.indexOf(tile)
                                    val remainingTiles = supportObjectPlacementTiles(gameState, supportType)
                                    keyboardPlacementTile =
                                        if (remainingTiles.isEmpty()) {
                                            null
                                        } else {
                                            remainingTiles[placedIndex.coerceIn(0, remainingTiles.lastIndex)]
                                        }
                                }
                            }
                        } else if (fiefType != null) {
                            val candidateTiles = supportFiefPlacementTiles(gameState, fiefType)
                            val tile =
                                keyboardPlacementTile?.takeIf { candidateTiles.contains(it) }
                                    ?: candidateTiles.firstOrNull()
                            if (tile != null && onPlaceSupportFief?.invoke(fiefType, tile) == true) {
                                val remaining = gameState.supportFiefRemaining[fiefType] ?: 0
                                if (remaining <= 0) {
                                    selectedSupportFief = null
                                    keyboardPlacementTile = null
                                } else {
                                    val placedIndex = candidateTiles.indexOf(tile)
                                    val remainingTiles = supportFiefPlacementTiles(gameState, fiefType)
                                    keyboardPlacementTile =
                                        if (remainingTiles.isEmpty()) {
                                            null
                                        } else {
                                            remainingTiles[placedIndex.coerceIn(0, remainingTiles.lastIndex)]
                                        }
                                }
                            }
                        } else {
                            val targeting = gameState.spellTargeting.value
                            val candidateTiles = spellTargetPositions(gameState)
                            val tile =
                                keyboardPlacementTile?.takeIf { candidateTiles.contains(it) }
                                    ?: candidateTiles.firstOrNull()
                            if (targeting != null && tile != null) {
                                when (targeting.activeSpell.targetType) {
                                    de.egril.defender.model.SpellTargetType.POSITION ->
                                        onSelectSpellTarget?.invoke(tile)
                                    de.egril.defender.model.SpellTargetType.ENEMY -> {
                                        val enemy =
                                            gameState.attackers.find { it.position.value == tile && !it.isDefeated.value }
                                        if (enemy != null && targeting.validTargets.contains(enemy)) {
                                            onSelectSpellTarget?.invoke(enemy)
                                        }
                                    }
                                    de.egril.defender.model.SpellTargetType.TOWER -> {
                                        val tower = gameState.defenders.find { it.position.value == tile }
                                        if (tower != null && targeting.validTargets.contains(tower)) {
                                            onSelectSpellTarget?.invoke(tower)
                                        }
                                    }
                                    else -> {}
                                }
                                keyboardPlacementTile = null
                            }
                        }
                        // Always consume the key so it never falls through to tower placement / attack.
                        true
                    }
                    // 1-8: Select defender type by index.
                    // Keys 1-2 are intentionally always enabled for tower-type selection as well.
                    event.type == KeyEventType.KeyDown &&
                        event.key in setOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) &&
                        !showMagicPanel -> {
                        val digit =
                            when (event.key) {
                                Key.One -> 1
                                Key.Two -> 2
                                Key.Three -> 3
                                Key.Four -> 4
                                Key.Five -> 5
                                Key.Six -> 6
                                Key.Seven -> 7
                                Key.Eight -> 8
                                else -> 0
                            }
                        if (digit > 0 && digit <= keyboardSelectableTowers.size) {
                            selectedDefenderType =
                                if (selectedDefenderType ==
                                    keyboardSelectableTowers[digit - 1]
                                ) {
                                    null
                                } else {
                                    keyboardSelectableTowers[digit - 1]
                                }
                            selectedAttackerId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            // Leaving the support bar for a tower: drop the support focus cursor so
                            // Enter confirms tower placement rather than a focused support element.
                            supportFocusIndex = null
                            true
                        } else {
                            false
                        }
                    }
                    // 1: First special action for selected tower
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.One &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        selectedDefenderId != null &&
                        !splitSelectorExpanded &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null && defender.isReady && defender.actionsRemaining.value > 0) {
                            when (defender.type) {
                                DefenderType.DWARVEN_MINE -> {
                                    handleMineAction(defender.id, MineAction.DIG)
                                    true
                                }
                                DefenderType.WIZARD_TOWER -> {
                                    val isAtMaxMana = gameState.currentMana.value >= gameState.maxMana.value
                                    if (!isAtMaxMana) {
                                        handleWizardAction(defender.id, WizardAction.GENERATE_MANA)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                DefenderType.SPIKE_TOWER -> {
                                    val canBuild =
                                        defender.level.value >= 20 &&
                                            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_2
                                    if (canBuild) {
                                        handleBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                DefenderType.SPEAR_TOWER -> {
                                    val canBuild =
                                        defender.level.value >= 10 &&
                                            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
                                    if (canBuild) {
                                        handleBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // 2: Second special action for selected tower
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Two &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        selectedDefenderId != null &&
                        !splitSelectorExpanded &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null && defender.isReady && defender.actionsRemaining.value > 0) {
                            when (defender.type) {
                                DefenderType.DWARVEN_MINE -> {
                                    handleMineAction(defender.id, MineAction.BUILD_TRAP)
                                    true
                                }
                                DefenderType.WIZARD_TOWER -> {
                                    if (defender.level.value >= 10) {
                                        handleWizardAction(defender.id, WizardAction.PLACE_MAGICAL_TRAP)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // Escape (remappable): Cancel an active support-object placement or spell targeting
                    // first, so keyboard users can stop placing (e.g. the Bomb) without leaving the level.
                    // A second press then falls through to the back-to-world-map handler below.
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutBackToWorldMap.value) &&
                        !isDemoMode &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) -> {
                        when {
                            selectedSupportObject != null -> selectedSupportObject = null
                            selectedSupportFief != null -> selectedSupportFief = null
                            else -> onExitSpellTargeting?.invoke()
                        }
                        keyboardPlacementTile = null
                        true
                    }
                    // Escape (remappable): Back to world map with confirmation
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutBackToWorldMap.value) &&
                        !isDemoMode -> {
                        // Skip unsaved changes check if in initial building phase with no defenders placed
                        val hasSandboxMapEdits =
                            gameState.level.isSandbox &&
                                (gameState.sandboxPaintedTiles.isNotEmpty() || gameState.sandboxPaintedRiverTiles.isNotEmpty())
                        val isInitialWithNothingDone =
                            gameState.phase.value == GamePhase.INITIAL_BUILDING &&
                                gameState.defenders.isEmpty() &&
                                !hasSandboxMapEdits
                        if (!isInitialWithNothingDone && unsavedChangesEnabled && hasUnsavedChanges.invoke()) {
                            showUnsavedChangesDialog = true
                        } else {
                            onBackToMap()
                        }
                        true
                    }
                    // End turn / start battle (Ctrl+Enter by default)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutEndTurnStartBattle.value) -> {
                        when (gameState.phase.value) {
                            GamePhase.PLAYER_TURN -> {
                                when {
                                    highlightEndTurnButton -> {
                                        highlightEndTurnButton = false
                                        endPlayerTurnAction()
                                        true
                                    }
                                    gameState.hasDefendersWithUnusedActions() -> {
                                        showEndTurnConfirmation = true
                                        true
                                    }
                                    else -> {
                                        endPlayerTurnAction()
                                        true
                                    }
                                }
                            }
                            GamePhase.INITIAL_BUILDING -> {
                                onStartFirstPlayerTurn()
                                true
                            }
                            else -> false
                        }
                    }
                    // Enter (plain): Confirm tower type selection / attack with selected tower
                    // Always consume Enter in gameplay phases to prevent focused buttons from handling it
                    // (Start Battle / End Turn buttons should only be triggered via shortcutEndTurnStartBattle)
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Enter &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isMetaPressed -> {
                        when (gameState.phase.value) {
                            GamePhase.INITIAL_BUILDING -> {
                                // In build phase, Enter confirms placement on keyboard-selected build tile (or first available)
                                val defType = selectedDefenderType
                                if (defType != null) {
                                    val buildTile =
                                        keyboardSelectedBuildTile?.takeIf { pos ->
                                            gameState.defenders.none { it.position.value == pos } &&
                                                gameState.canPlaceDefender(defType)
                                        } ?: gameState.level.buildAreas
                                            .firstOrNull { pos ->
                                                gameState.defenders.none { it.position.value == pos } &&
                                                    gameState.canPlaceDefender(defType)
                                            }
                                    if (buildTile != null && onPlaceDefender(defType, buildTile)) {
                                        if (!gameState.canPlaceDefender(defType)) {
                                            selectedDefenderType = null
                                        }
                                        keyboardSelectedBuildTile = null
                                    }
                                }
                                // Always consume Enter in build phase (prevent Start Battle button activation)
                                true
                            }
                            GamePhase.PLAYER_TURN -> {
                                when {
                                    // If a tower type is selected (buy mode), place on first available tile
                                    selectedDefenderType != null && selectedDefenderId == null && !showMagicPanel -> {
                                        val defType = selectedDefenderType
                                        if (defType != null) {
                                            val buildTile =
                                                keyboardSelectedBuildTile?.takeIf { pos ->
                                                    gameState.defenders.none { it.position.value == pos } &&
                                                        gameState.canPlaceDefender(defType)
                                                } ?: gameState.level.buildAreas
                                                    .firstOrNull { pos ->
                                                        gameState.defenders.none { it.position.value == pos } &&
                                                            gameState.canPlaceDefender(defType)
                                                    }
                                            if (buildTile != null && onPlaceDefender(defType, buildTile)) {
                                                if (!gameState.canPlaceDefender(defType)) {
                                                    selectedDefenderType = null
                                                }
                                                keyboardSelectedBuildTile = null
                                            }
                                        }
                                        true
                                    }
                                    // If a tower is selected with a valid target, attack
                                    selectedDefenderId != null && !showMagicPanel -> {
                                        val defenderId = selectedDefenderId
                                        val targetId = selectedTargetId
                                        val targetPos = selectedTargetPosition
                                        val defender = defenderId?.let { id -> gameState.defenders.find { it.id == id } }
                                        if (defender != null &&
                                            defender.isReady &&
                                            defender.actionsRemaining.value > 0 &&
                                            (targetId != null || targetPos != null)
                                        ) {
                                            when {
                                                targetId != null -> {
                                                    onDefenderAttack(defenderId!!, targetId)
                                                    true
                                                }
                                                targetPos != null -> {
                                                    onDefenderAttackPosition(defenderId!!, targetPos)
                                                    true
                                                }
                                                else -> true
                                            }
                                        } else {
                                            // Consume Enter anyway to prevent End Turn button activation
                                            true
                                        }
                                    }
                                    // Always consume Enter in player turn to prevent End Turn button activation
                                    else -> true
                                }
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
        }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(screenFocusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent(keyboardHandler)
                    // In demo mode, intercept any click/tap to show "stop demo?" dialog
                    .then(
                        if (isDemoMode) {
                            Modifier.clickable(
                                indication = null,
                                interactionSource =
                                    remember {
                                        androidx.compose.foundation.interaction
                                            .MutableInteractionSource()
                                    },
                            ) { showStopDemoDialog = true }
                        } else {
                            Modifier
                        },
                    ),
        ) {
            val availableWindowWidth = maxWidth
            val availableWindowHeight = maxHeight
            val windowSize =
                remember(maxWidth, maxHeight) {
                    "Window: ${maxWidth.value.toInt()} x ${maxHeight.value.toInt()} dp"
                }

            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .accessibilityVisualFilter(
                            highContrastEnabled = AppSettings.highContrastEnabled.value,
                            colorBlindPalette = AppSettings.colorBlindPalette.value,
                        ),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header with prominent phase indicator (collapsible)
                    GameHeader(
                        gameState = gameState,
                        showOverlay = showOverlay,
                        onShowOverlayChange = { showOverlay = it },
                        onBackToMap = {
                            if (isDemoMode) {
                                showStopDemoDialog = true
                            } else if (unsavedChangesEnabled && hasUnsavedChanges.invoke()) {
                                showUnsavedChangesDialog = true
                            } else {
                                onBackToMap()
                            }
                        },
                        onSaveGame =
                            if (onSaveGame != null && !isDemoMode) {
                                { showSaveDialog = true }
                            } else {
                                null
                            },
                        onCheatCode =
                            if (onCheatCode != null && !isDemoMode) {
                                { showCheatDialog = true }
                            } else {
                                null
                            },
                        onEnemyCountClick = { showOverlay = !showOverlay },
                        onWinLevelInfoClick = { showEndTurnConfirmation = true },
                        onSandboxTools =
                            if (onSandboxSpawnEnemy != null && !isDemoMode) {
                                { showSandboxTools = true }
                            } else {
                                null
                            },
                        onManaClick =
                            if (onOpenMagicPanel != null && gameState.maxMana.value > 0 && !isDemoMode) {
                                {
                                    if (gameState.instantTowerSpellActive.value) {
                                        showAbortInstantTowerDialog = true
                                    } else if (showMagicPanel) {
                                        onCloseMagicPanel?.invoke()
                                    } else {
                                        onOpenMagicPanel.invoke()
                                    }
                                }
                            } else {
                                null
                            },
                        isDemoMode = isDemoMode,
                        onDemoTitleClick =
                            if (isDemoMode) {
                                { showStopDemoDialog = true }
                            } else {
                                null
                            },
                        externalShowShortcuts = triggerShowShortcuts,
                        onExternalShowShortcutsHandled = { triggerShowShortcuts = false },
                        externalShowHelp = triggerShowHelp,
                        onExternalShowHelpHandled = { triggerShowHelp = false },
                        externalShowFeedback = triggerShowFeedback,
                        onExternalShowFeedbackHandled = { triggerShowFeedback = false },
                        externalShowSettings = triggerShowSettings,
                        onExternalShowSettingsHandled = { triggerShowSettings = false },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Game Grid with toggle button and overlay
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Scrollable Game Grid
                        GameGrid(
                            gameState = gameState,
                            selectedDefenderType = selectedDefenderType,
                            selectedDefenderId = selectedDefenderId,
                            selectedTargetId = selectedTargetId,
                            selectedTargetPosition = selectedTargetPosition,
                            selectedMineAction = selectedMineAction,
                            selectedWizardAction = selectedWizardAction,
                            selectedBarricadeAction = selectedBarricadeAction,
                            extraFocusTrigger = mapRefocusTrigger,
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
                                if (attacker != null && selectedDefenderId == null && !positionInShadowFog) {
                                    if (previousSelectedAttackerId == attacker.id) {
                                        // Deselect if clicking the same attacker
                                        selectedAttackerId = null
                                    } else {
                                        // Select this attacker, deselect any selected defender
                                        selectedAttackerId = attacker.id
                                        selectedDefenderId = null
                                        selectedTargetId = null
                                        selectedTargetPosition = null
                                        selectedBarricadePosition = null
                                        return@GameGrid
                                    }
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
                                        if (selectedDefender.type == DefenderType.WIZARD_TOWER &&
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
                                                    if (hasFog) null else gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                                selectedTargetId = enemyAtPosition?.id
                                            }
                                        } else {
                                            // For single-target attacks, allow targeting enemies, bridges, or shadow fog tiles
                                            val distance = selectedDefender.position.value.distanceTo(position)
                                            val attackerForTargeting =
                                                gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                            val bridgeAtPosition = gameState.getBridgeAt(position)
                                            val hasShadowFog =
                                                gameState.fieldEffects.any { it.type == FieldEffectType.SHADOW_FOG && it.position == position }

                                            if (distance >= selectedDefender.type.minRange && distance <= effectiveRange) {
                                                if (attackerForTargeting != null && !hasShadowFog) {
                                                    selectedTargetId = attackerForTargeting.id
                                                    selectedTargetPosition = position // to be able to show the 3 circles to highlight the target
                                                } else if (bridgeAtPosition != null && bridgeAtPosition.isActive) {
                                                    // Allow targeting bridge tiles
                                                    selectedTargetId = null // Bridges don't have attacker IDs
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
                                                    DefenderType.WIZARD_TOWER, DefenderType.DWARVEN_MINE -> true
                                                    DefenderType.SPIKE_TOWER -> selectedDefenderForHints.level.value >= 20
                                                    DefenderType.SPEAR_TOWER -> selectedDefenderForHints.level.value >= 10
                                                    else -> false
                                                }
                                            val key1Label =
                                                when (selectedDefenderForHints.type) {
                                                    DefenderType.WIZARD_TOWER -> stringResource(Res.string.generate_mana)
                                                    DefenderType.DWARVEN_MINE -> stringResource(Res.string.dig)
                                                    DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER ->
                                                        stringResource(
                                                            Res.string.barricade,
                                                        )
                                                    else -> ""
                                                }
                                            val showKey2 =
                                                when (selectedDefenderForHints.type) {
                                                    DefenderType.WIZARD_TOWER -> selectedDefenderForHints.level.value >= 10
                                                    DefenderType.DWARVEN_MINE -> true
                                                    else -> false
                                                }
                                            val key2Label =
                                                when (selectedDefenderForHints.type) {
                                                    DefenderType.WIZARD_TOWER -> stringResource(Res.string.magical_trap)
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

                    // Track the maximum height the controls panel has reached (in pixels).
                    // Using heightIn(min = ...) on the container prevents it from shrinking when switching
                    // between the tall PLAYER_TURN panel and the short ENEMY_TURN indicator, which would
                    // otherwise cause the map area (weight=1f) to expand and the map to visibly jump.
                    var maxControlsHeightPx by remember { mutableStateOf(0) }

                    // Wrap in Box that tracks and maintains its maximum seen height to prevent map jumping.
                    // contentAlignment = BottomStart ensures that when the current content is shorter than
                    // the maximum recorded height, it stays pinned to the bottom of the reserved area so
                    // the tower buttons and enemy-turn banner appear as low on the screen as possible.
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .then(
                                    if (maxControlsHeightPx > 0) {
                                        with(LocalDensity.current) { Modifier.heightIn(min = maxControlsHeightPx.toDp()) }
                                    } else {
                                        Modifier
                                    },
                                ).onSizeChanged { size ->
                                    if (size.height > maxControlsHeightPx) {
                                        maxControlsHeightPx = size.height
                                    }
                                },
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        // Show magic panel inline (non-overlay) when open - map remains accessible
                        if (showMagicPanel && playerStats != null && onCloseMagicPanel != null && onCastSpell != null) {
                            MagicPanel(
                                playerStats = playerStats,
                                currentMana = gameState.currentMana.value,
                                maxMana = gameState.maxMana.value,
                                currentHealthPoints = gameState.healthPoints.value,
                                maxHealthPoints = gameState.level.healthPoints,
                                gamePhase = gameState.phase.value,
                                selectedSpell = selectedSpell,
                                onCastSpell = onCastSpell,
                                onClose = {
                                    onCloseMagicPanel.invoke()
                                    // Also exit targeting mode if active when closing the panel
                                    if (gameState.spellTargeting.value != null) {
                                        onExitSpellTargeting?.invoke()
                                    }
                                },
                            )
                        } else if (gameState.spellTargeting.value != null) {
                            // Spell targeting mode: show compact instruction in bottom panel so map stays accessible
                            val targeting = gameState.spellTargeting.value!!
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Spell type icon
                                    SpellTargetIcon(spell = targeting.activeSpell, size = 32.dp)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targeting.activeSpell.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text =
                                                when (targeting.activeSpell.targetType) {
                                                    de.egril.defender.model.SpellTargetType.POSITION ->
                                                        stringResource(Res.string.spell_targeting_position)
                                                    de.egril.defender.model.SpellTargetType.ENEMY ->
                                                        stringResource(Res.string.spell_targeting_enemy)
                                                    de.egril.defender.model.SpellTargetType.TOWER ->
                                                        stringResource(Res.string.spell_targeting_tower)
                                                    else -> ""
                                                },
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        PlacementKeyboardHints(modifier = Modifier.padding(top = 4.dp))
                                    }
                                    OutlinedButton(onClick = { onExitSpellTargeting?.invoke() }) {
                                        Text(stringResource(Res.string.spell_targeting_cancel))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ShortcutKeyChip(
                                            text = "Esc",
                                            color = LocalContentColor.current.copy(alpha = 0.75f),
                                        )
                                    }
                                }
                            }
                        } else if (selectedSupportObject != null) {
                            // Support object placement mode: show a compact instruction + cancel card
                            // (mirrors the spell targeting card) so keyboard users can see how to cancel.
                            val placingType = selectedSupportObject!!
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SupportObjectIcon(placingType, 32.dp)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = placingType.localizedSupportName(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = stringResource(Res.string.spell_targeting_position),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        PlacementKeyboardHints(modifier = Modifier.padding(top = 4.dp))
                                    }
                                    OutlinedButton(onClick = { selectedSupportObject = null }) {
                                        Text(stringResource(Res.string.spell_targeting_cancel))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ShortcutKeyChip(
                                            text = "Esc",
                                            color = LocalContentColor.current.copy(alpha = 0.75f),
                                        )
                                    }
                                }
                            }
                        } else if (selectedSupportFief != null) {
                            // Fief placement mode: show a compact instruction + cancel card
                            // selectedSupportFief is a mutable var, so smart-cast is not possible;
                            // capture a local non-null copy for use within this branch.
                            val placingFief = selectedSupportFief!!
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = placingFief.localizedFiefName(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = stringResource(Res.string.spell_targeting_position),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        PlacementKeyboardHints(modifier = Modifier.padding(top = 4.dp))
                                    }
                                    OutlinedButton(onClick = { selectedSupportFief = null }) {
                                        Text(stringResource(Res.string.spell_targeting_cancel))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ShortcutKeyChip(
                                            text = "Esc",
                                            color = LocalContentColor.current.copy(alpha = 0.75f),
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Support bar: placable objects + spell tokens shown directly above the button row
                                SupportBar(
                                    gameState = gameState,
                                    selectedSupportObject = selectedSupportObject,
                                    activeSpellToken = activeSpellToken,
                                    enabled =
                                        gameState.phase.value == GamePhase.PLAYER_TURN ||
                                            gameState.phase.value == GamePhase.INITIAL_BUILDING,
                                    onObjectClick = { type ->
                                        // Toggle object placement selection; clear other selections
                                        selectedSupportObject = if (selectedSupportObject == type) null else type
                                        selectedSupportFief = null
                                        selectedDefenderType = null
                                    },
                                    onSpellClick = { spell ->
                                        selectedSupportObject = null
                                        selectedSupportFief = null
                                        onCastSupportSpellToken?.invoke(spell)
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    onCooldownPowerClick = { power ->
                                        selectedSupportObject = null
                                        selectedSupportFief = null
                                        onActivateCooldownPower?.invoke(power)
                                    },
                                    selectedSupportFief = selectedSupportFief,
                                    onFiefClick = { type ->
                                        selectedSupportFief = if (selectedSupportFief == type) null else type
                                        selectedSupportObject = null
                                        selectedDefenderType = null
                                    },
                                    focusedSlotIndex =
                                        supportFocusIndex?.let { idx ->
                                            val count = visibleSupportSlots(gameState).size
                                            if (count == 0) null else idx.coerceIn(0, count - 1)
                                        },
                                )
                                // Keyboard-navigation hint for the support bar: shown only while there
                                // are support elements to navigate and hints are enabled.
                                if (visibleSupportSlots(gameState).isNotEmpty()) {
                                    SupportBarKeyboardHints(
                                        modifier =
                                            Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(bottom = 4.dp),
                                    )
                                }
                                // Control Panel based on phase
                                when (gameState.phase.value) {
                                    GamePhase.INITIAL_BUILDING -> {
                                        GameControlsPanel(
                                            phase = GamePhase.INITIAL_BUILDING,
                                            gameState = gameState,
                                            coinsState = gameState.coins,
                                            selectedDefenderType = selectedDefenderType,
                                            selectedDefenderId = selectedDefenderId,
                                            selectedAttackerId = selectedAttackerId,
                                            selectedTargetId = null,
                                            selectedTargetPosition = null,
                                            selectedBarricadePosition = selectedBarricadePosition,
                                            onSelectDefenderType = {
                                                selectedDefenderType = it
                                                selectedSupportObject = null
                                            },
                                            onUpgradeDefender = { onUpgradeDefender(it) },
                                            onUndoTower = { defenderId ->
                                                if (onUndoTower(defenderId)) {
                                                    selectedDefenderType = null
                                                    selectedDefenderId = null
                                                }
                                            },
                                            onSellTower = { defenderId ->
                                                if (onSellTower(defenderId)) {
                                                    selectedDefenderType = null
                                                    selectedDefenderId = null
                                                }
                                            },
                                            onDefenderAttack = { _, _ -> false },
                                            onDefenderAttackPosition = { _, _ -> false },
                                            onPrimaryAction = {
                                                selectedDefenderType = null // Clear defender type selection when starting battle
                                                selectedDefenderId = null // Clear defender selection when starting battle
                                                selectedAttackerId = null // Clear attacker selection when starting battle
                                                onStartFirstPlayerTurn()

                                                // Show first-time auto-attack info at the start of the level if allowed and not seen
                                                if (gameState.level.allowAutoAttack &&
                                                    !gameState.infoState.value.hasSeen(InfoType.AUTO_ATTACK_INFO)
                                                ) {
                                                    gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.AUTO_ATTACK_INFO)
                                                }

                                                // Track tutorial progress and auto-advance START_COMBAT step
                                                if (gameState.tutorialState.value.isActive) {
                                                    if (!gameState.tutorialState.value.hasStartedFirstTurn) {
                                                        gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                                                    }
                                                    // Auto-advance if currently showing START_COMBAT step
                                                    if (gameState.tutorialState.value.currentStep == TutorialStep.START_COMBAT) {
                                                        gameState.tutorialState.value = gameState.tutorialState.value.advanceStep()
                                                    }
                                                }
                                            },
                                            onMineAction = handleMineAction,
                                            onWizardAction = handleWizardAction,
                                            selectedMineAction = selectedMineAction,
                                            selectedWizardAction = selectedWizardAction,
                                            onBarricadeAction = handleBarricadeAction,
                                            selectedBarricadeAction = selectedBarricadeAction,
                                            onRemoveBarricade = { pos ->
                                                onRemoveBarricade?.invoke(pos)
                                                selectedBarricadePosition = null
                                            },
                                            uiScale = uiScale,
                                            onShowDragonInfo = {
                                                gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.DRAGON_INFO)
                                            },
                                            splitSelectorToggle = splitSelectorToggle,
                                            onSplitSelectorExpandedChanged = { splitSelectorExpanded = it },
                                        )
                                    }

                                    GamePhase.PLAYER_TURN -> {
                                        GameControlsPanel(
                                            phase = GamePhase.PLAYER_TURN,
                                            gameState = gameState,
                                            coinsState = gameState.coins,
                                            selectedDefenderType = selectedDefenderType,
                                            selectedDefenderId = selectedDefenderId,
                                            selectedAttackerId = selectedAttackerId,
                                            selectedTargetId = selectedTargetId,
                                            selectedTargetPosition = selectedTargetPosition,
                                            selectedBarricadePosition = selectedBarricadePosition,
                                            onSelectDefenderType = {
                                                selectedDefenderType = it
                                                selectedSupportObject = null
                                            },
                                            onUpgradeDefender = { onUpgradeDefender(it) },
                                            onUndoTower = { defenderId ->
                                                if (onUndoTower(defenderId)) {
                                                    selectedDefenderType = null
                                                    selectedDefenderId = null
                                                }
                                            },
                                            onSellTower = { defenderId ->
                                                if (onSellTower(defenderId)) {
                                                    selectedDefenderType = null
                                                    selectedDefenderId = null
                                                }
                                            },
                                            onDefenderAttack = { defenderId, targetId ->
                                                if (onDefenderAttack(defenderId, targetId)) {
                                                    // Check if we should keep the selection active:
                                                    // - Tower still has actions remaining
                                                    // - Enemy is still alive
                                                    if (!shouldKeepTargetSelection(gameState, defenderId, targetId)) {
                                                        selectedTargetId = null
                                                        selectedTargetPosition = null
                                                    }

                                                    // Track tutorial progress
                                                    if (gameState.tutorialState.value.isActive &&
                                                        !gameState.tutorialState.value.hasAttackedEnemy
                                                    ) {
                                                        gameState.tutorialState.value = gameState.tutorialState.value.markAttackedEnemy()
                                                    }
                                                    true
                                                } else {
                                                    false
                                                }
                                            },
                                            onDefenderAttackPosition = { defenderId, targetPos ->
                                                if (onDefenderAttackPosition(defenderId, targetPos)) {
                                                    // Check if we should keep the selection active:
                                                    // - Tower still has actions remaining
                                                    // - There's still a living enemy at the target position
                                                    if (!shouldKeepTargetSelectionForPosition(gameState, defenderId, targetPos)) {
                                                        selectedTargetId = null
                                                        selectedTargetPosition = null
                                                    }

                                                    // Track tutorial progress
                                                    if (gameState.tutorialState.value.isActive &&
                                                        !gameState.tutorialState.value.hasAttackedEnemy
                                                    ) {
                                                        gameState.tutorialState.value = gameState.tutorialState.value.markAttackedEnemy()
                                                    }
                                                    true
                                                } else {
                                                    false
                                                }
                                            },
                                            onPrimaryAction = {
                                                // Check if there are unused action points before ending turn
                                                if (gameState.hasDefendersWithUnusedActions()) {
                                                    // Show confirmation dialog
                                                    showEndTurnConfirmation = true
                                                } else {
                                                    // End turn directly
                                                    endPlayerTurnAction()
                                                    // Track tutorial progress
                                                    if (gameState.tutorialState.value.isActive &&
                                                        !gameState.tutorialState.value.hasStartedFirstTurn
                                                    ) {
                                                        gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                                                    }
                                                }
                                            },
                                            onMineAction = handleMineAction,
                                            onWizardAction = handleWizardAction,
                                            selectedMineAction = selectedMineAction,
                                            selectedWizardAction = selectedWizardAction,
                                            onBarricadeAction = handleBarricadeAction,
                                            selectedBarricadeAction = selectedBarricadeAction,
                                            onRemoveBarricade = { pos ->
                                                onRemoveBarricade?.invoke(pos)
                                                selectedBarricadePosition = null
                                            },
                                            uiScale = uiScale,
                                            onShowDragonInfo = {
                                                gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.DRAGON_INFO)
                                            },
                                            highlightEndTurnButton = highlightEndTurnButton,
                                            splitSelectorToggle = splitSelectorToggle,
                                            onSplitSelectorExpandedChanged = { splitSelectorExpanded = it },
                                        )
                                    }

                                    GamePhase.ENEMY_TURN -> {
                                        EnemyTurnInfo()
                                    }
                                }
                            }
                        }
                    } // end Box(height-locked controls panel)

                    // Dig outcome dialog
                    if (showDigOutcomeDialog && currentDigOutcome != null) {
                        DigOutcomeDialog(
                            outcome = currentDigOutcome!!,
                            onDismiss = { showDigOutcomeDialog = false },
                            onResetSelections = {
                                selectedDefenderType = null
                                selectedDefenderId = null
                            },
                            dragonName = currentDragonName,
                        )
                    }

                    // Abort Instant Tower spell dialog
                    if (showAbortInstantTowerDialog) {
                        AbortInstantTowerSpellDialog(
                            onAbort = {
                                showAbortInstantTowerDialog = false
                                onCancelInstantTowerSpell?.invoke()
                                onOpenMagicPanel?.invoke()
                            },
                            onContinue = { showAbortInstantTowerDialog = false },
                        )
                    }

                    // Save game dialog (with optional comment input)
                    if (showSaveDialog && onSaveGame != null) {
                        SaveGameDialog(
                            saveCommentInput = saveCommentInput,
                            onSaveCommentChange = { saveCommentInput = it },
                            onSave = { comment ->
                                onSaveGame(comment)
                                showSaveDialog = false
                                saveCommentInput = ""
                                showSaveConfirmation = true
                            },
                            onDismiss = {
                                showSaveDialog = false
                                saveCommentInput = ""
                            },
                        )
                    }

                    // Save confirmation dialog
                    if (showSaveConfirmation) {
                        SaveConfirmationDialog(
                            onDismiss = { showSaveConfirmation = false },
                        )
                    }

                    // Cheat code dialog
                    if (showCheatDialog && onCheatCode != null) {
                        CheatCodeDialog(
                            onDismiss = {
                                showCheatDialog = false
                                cheatCodeInput = ""
                            },
                            onApplyCheatCode = onCheatCode,
                            showHints = true,
                            initialInput = cheatCodeInput,
                            onInputChange = { cheatCodeInput = it },
                        )
                    }

                    if (showSandboxTools && onSandboxSpawnEnemy != null) {
                        SandboxToolsDialog(
                            spawnPoints = gameState.level.startPositions,
                            onSpawnEnemy = { type, level, spawnPoint -> onSandboxSpawnEnemy(type, level, spawnPoint) },
                            onAddCoins = { onSandboxAddCoins?.invoke() },
                            onDismiss = { showSandboxTools = false },
                        )
                    }

                    // Platform info dialog (from platform cheat code)
                    if (showPlatformInfo && onClearPlatformInfo != null) {
                        de.egril.defender.ui.PlatformInfoDialog(
                            platformInfo =
                                de.egril.defender.utils
                                    .getPlatform()
                                    .name,
                            windowSize = windowSize,
                            onDismiss = onClearPlatformInfo,
                        )
                    }

                    // Cheat code help screen (from cheat/cheats/help cheat code)
                    if (showCheatHelp && onClearCheatHelp != null) {
                        de.egril.defender.ui.CheatCodeHelpScreen(
                            onDismiss = onClearCheatHelp,
                            isInGameplay = true,
                        )
                    }

                    // Remove barricade confirmation dialog
                    // Note: This dialog only shows for barricades without towers
                    if (showRemoveBarricadeDialog && barricadeToRemove != null) {
                        ConfirmationDialog(
                            title = stringResource(Res.string.remove_barricade_title),
                            message = stringResource(Res.string.remove_barricade_message),
                            onConfirm = {
                                val actualRefund = onRemoveBarricade?.invoke(barricadeToRemove!!) ?: 0
                                if (actualRefund > 0) {
                                    // Add coins back to player (should be 0 for barricades without towers)
                                    gameState.coins.value += actualRefund
                                }
                                showRemoveBarricadeDialog = false
                                barricadeToRemove = null
                            },
                            onDismiss = {
                                showRemoveBarricadeDialog = false
                                barricadeToRemove = null
                            },
                        )
                    }

                    // Remove trap confirmation dialog
                    if (showRemoveTrapDialog && trapToRemove != null) {
                        ConfirmationDialog(
                            title = stringResource(Res.string.remove_trap_title),
                            message = stringResource(Res.string.remove_trap_message),
                            onConfirm = {
                                // Remove trap from game state
                                gameState.traps.removeAll { it.position == trapToRemove }
                                showRemoveTrapDialog = false
                                trapToRemove = null
                            },
                            onDismiss = {
                                showRemoveTrapDialog = false
                                trapToRemove = null
                            },
                        )
                    }

                    val keyboardConfirmation = keyboardUndoOrSellConfirmation
                    if (keyboardConfirmation != null) {
                        val (confirmDefenderId, isUndo) = keyboardConfirmation
                        val defender = gameState.defenders.find { it.id == confirmDefenderId }
                        if (defender != null) {
                            val locale = com.hyperether.resources.currentLanguage.value
                            val towerName = defender.type.getLocalizedName(locale)
                            val coinsLabel = stringResource(Res.string.coins_label)
                            val refundAmount = if (isUndo) defender.totalCost else (defender.totalCost * 0.75).toInt()
                            val titleStr =
                                if (isUndo) {
                                    stringResource(
                                        Res.string.undo_tower_title,
                                    )
                                } else {
                                    stringResource(Res.string.sell_tower_title)
                                }
                            val messageStr =
                                if (isUndo) {
                                    stringResource(Res.string.undo_tower_message, towerName, refundAmount.toString(), coinsLabel)
                                } else {
                                    stringResource(Res.string.sell_tower_message, towerName, refundAmount.toString(), coinsLabel)
                                }
                            val confirmLabel = if (isUndo) stringResource(Res.string.undo) else stringResource(Res.string.sell)
                            val confirmColor = if (isUndo) GamePlayColors.Success else GamePlayColors.Warning
                            AlertDialog(
                                onDismissRequest = { keyboardUndoOrSellConfirmation = null },
                                title = { Text(titleStr) },
                                text = { Text(messageStr) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (isUndo) {
                                                onUndoTower(defender.id)
                                            } else if (onSellTower(defender.id)) {
                                                selectedDefenderType = null
                                                selectedDefenderId = null
                                            }
                                            keyboardUndoOrSellConfirmation = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(confirmLabel)
                                            ShortcutKeyChip(
                                                text = formatShortcutBindingForDisplay(AppSettings.shortcutUndoOrSellSelectedTower.value),
                                            )
                                        }
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = { keyboardUndoOrSellConfirmation = null },
                                    ) {
                                        Text(stringResource(Res.string.cancel))
                                    }
                                },
                            )
                        } else {
                            keyboardUndoOrSellConfirmation = null
                        }
                    }

                    // Unsaved changes dialog
                    if (showUnsavedChangesDialog && unsavedChangesEnabled) {
                        UnsavedChangesDialog(
                            onSaveAndExit = {
                                // Save the game first
                                onSaveGame(null)
                                showUnsavedChangesDialog = false
                                // Then navigate back to map
                                onBackToMap()
                            },
                            onDiscardChanges = {
                                showUnsavedChangesDialog = false
                                // Navigate back without saving
                                onBackToMap()
                            },
                            onCancel = {
                                // Just close the dialog and stay in the game
                                showUnsavedChangesDialog = false
                            },
                        )
                    }

                    // End turn confirmation dialog
                    if (showEndTurnConfirmation) {
                        val canWinLevelNow = gameState.canWinLevelNow()
                        EndTurnConfirmationDialog(
                            onConfirm = {
                                showEndTurnConfirmation = false
                                endPlayerTurnAction()
                                // Track tutorial progress
                                if (gameState.tutorialState.value.isActive &&
                                    !gameState.tutorialState.value.hasStartedFirstTurn
                                ) {
                                    gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                                }
                            },
                            onAutoAttackAndConfirm = {
                                showEndTurnConfirmation = false
                                autoAttackAndEndTurnAction()
                                // Track tutorial progress
                                if (gameState.tutorialState.value.isActive &&
                                    !gameState.tutorialState.value.hasStartedFirstTurn
                                ) {
                                    gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                                }
                            },
                            onCancel = {
                                showEndTurnConfirmation = false
                            },
                            showAutoAttackButton = gameState.level.allowAutoAttack && gameState.hasDefendersForAutoAttack(),
                            showEndTurnWarning = gameState.hasDefendersWithUnusedActions(),
                            showWinLevelNow = canWinLevelNow && onWinLevelNow != null,
                            onWinLevelNow = {
                                showEndTurnConfirmation = false
                                onWinLevelNow?.invoke()
                            },
                        )
                    }

                    // Special actions remaining dialog
                    if (specialActionsRemaining.isNotEmpty()) {
                        SpecialActionsRemainingDialog(
                            remainingTypes = specialActionsRemaining,
                            onContinueTurn = {
                                onClearSpecialActionsWarning?.invoke()
                            },
                        )
                    }

                    // Time reminder dialog
                    reminderMessage?.let { reminder ->
                        val elapsedTime =
                            reminder.elapsedMs?.let { elapsedMs ->
                                val hours = elapsedMs / (60 * 60 * 1000)
                                val minutes = (elapsedMs % (60 * 60 * 1000)) / (60 * 1000)
                                buildString {
                                    if (hours > 0) {
                                        val hourStr =
                                            if (hours == 1L) {
                                                stringResource(Res.string.hour, hours.toInt())
                                            } else {
                                                stringResource(Res.string.hours, hours.toInt())
                                            }
                                        append(hourStr)
                                    }
                                    if (minutes > 0) {
                                        if (hours > 0) append(" ")
                                        val minuteStr =
                                            if (minutes == 1L) {
                                                stringResource(Res.string.minute, minutes.toInt())
                                            } else {
                                                stringResource(Res.string.minutes, minutes.toInt())
                                            }
                                        append(minuteStr)
                                    }
                                    if (hours == 0L && minutes == 0L) {
                                        append(stringResource(Res.string.minutes, 0))
                                    }
                                }
                            }
                        ReminderDialog(
                            type = reminder.type,
                            elapsedTime = elapsedTime,
                            timeDescription =
                                when (reminder.timeDescription) {
                                    "close_to_midnight" -> stringResource(Res.string.time_for_sleep_close_to_midnight)
                                    "midnight" -> stringResource(Res.string.time_for_sleep_midnight)
                                    "after_midnight" -> stringResource(Res.string.time_for_sleep_after_midnight)
                                    else -> null
                                },
                            onDismiss = {
                                onClearReminderMessage?.invoke()
                            },
                        )
                    }

                    // Post-target spell confirmation dialog (shows after target is selected)
                    if (showSpellTargetConfirmation != null && onConfirmTargetSpell != null && onDismissTargetConfirmation != null) {
                        val (spell, target) = showSpellTargetConfirmation
                        SpellTargetConfirmationDialog(
                            spell = spell,
                            target = target,
                            currentMana = gameState.currentMana.value,
                            onConfirm = { onConfirmTargetSpell.invoke() },
                            onDismiss = { onDismissTargetConfirmation.invoke() },
                            isTokenCast = activeSpellToken == spell,
                        )
                    }

                    // Freeze immune warning dialog
                    if (showFreezeImmuneWarning != null && onDismissFreezeWarning != null) {
                        FreezeImmuneWarningDialog(
                            enemy = showFreezeImmuneWarning,
                            onDismiss = { onDismissFreezeWarning.invoke() },
                        )
                    }

                    // In-game event message dialog (target captured, gate destroyed, or Ewhad narrative events)
                    pendingGameMessage?.let { msg ->
                        when (msg.type) {
                            GameMessageType.EWHAD_ENTERS ->
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.ewhad_enters_title),
                                    text = stringResource(Res.string.ewhad_enters_text),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                )
                            GameMessageType.EWHAD_RETREATS ->
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.ewhad_retreats_title),
                                    text = stringResource(Res.string.ewhad_retreats_text),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                )
                            GameMessageType.EWHAD_DEFEATED ->
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.ewhad_defeated_title),
                                    text = stringResource(Res.string.ewhad_defeated_text),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                )
                            GameMessageType.VILLAIN_ENTERS -> {
                                val villainType = attackerTypeFromMessageName(msg.name)
                                val (villainTitle, villainText) =
                                    when (msg.name) {
                                        AttackerType.GAROKK.name ->
                                            stringResource(Res.string.villain_garokk_title) to
                                                (stringResource(Res.string.villain_garokk_backstory) + "\n" + stringResource(Res.string.villain_garokk_description))
                                        AttackerType.SNOTLING_BOSS.name ->
                                            stringResource(Res.string.villain_gribnak_title) to
                                                (stringResource(Res.string.villain_gribnak_backstory) + "\n" + stringResource(Res.string.villain_gribnak_description))
                                        AttackerType.MORGUK_BONEWHISPER.name ->
                                            stringResource(Res.string.villain_morguk_title) to
                                                (stringResource(Res.string.villain_morguk_backstory) + "\n" + stringResource(Res.string.villain_morguk_description))
                                        AttackerType.ARAXXA.name ->
                                            stringResource(Res.string.villain_araxxa_title) to
                                                (stringResource(Res.string.villain_araxxa_backstory) + "\n" + stringResource(Res.string.villain_araxxa_description))
                                        AttackerType.BARON_RATTERZAHN.name ->
                                            stringResource(Res.string.villain_ratterzahn_title) to
                                                (stringResource(Res.string.villain_ratterzahn_backstory) + "\n" + stringResource(Res.string.villain_ratterzahn_description))
                                        AttackerType.FALLEN_SHIELDMAIDEN_FREYA.name ->
                                            stringResource(Res.string.villain_freya_title) to
                                                (stringResource(Res.string.villain_freya_backstory) + "\n" + stringResource(Res.string.villain_freya_description))
                                        AttackerType.PRINCE_VALERIUS_THE_SOULREAPER.name ->
                                            stringResource(Res.string.villain_valerius_title) to
                                                (stringResource(Res.string.villain_valerius_backstory) + "\n" + stringResource(Res.string.villain_valerius_description))
                                        AttackerType.SILAS_THE_MASKMASTER.name ->
                                            stringResource(Res.string.villain_silas_title) to
                                                (stringResource(Res.string.villain_silas_backstory) + "\n" + stringResource(Res.string.villain_silas_description))
                                        AttackerType.GRAND_COVEN_MOTHER_SYBILLA.name ->
                                            stringResource(Res.string.villain_sybilla_title) to
                                                (stringResource(Res.string.villain_sybilla_backstory) + "\n" + stringResource(Res.string.villain_sybilla_description))
                                        AttackerType.SYLVANAS_THE_MOLDING.name ->
                                            stringResource(Res.string.villain_sylvanas_title) to
                                                (stringResource(Res.string.villain_sylvanas_backstory) + "\n" + stringResource(Res.string.villain_sylvanas_description))
                                        AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE.name ->
                                            stringResource(Res.string.villain_malakor_title) to
                                                (stringResource(Res.string.villain_malakor_backstory) + "\n" + stringResource(Res.string.villain_malakor_description))
                                        AttackerType.IGNIS_VA_THE_DRAGONVOICE.name ->
                                            stringResource(Res.string.villain_ignis_va_title) to
                                                (stringResource(Res.string.villain_ignis_va_backstory) + "\n" + stringResource(Res.string.villain_ignis_va_description))
                                        AttackerType.MORVATH_THE_SHADOWMASTER.name ->
                                            stringResource(Res.string.villain_morvath_title) to
                                                (stringResource(Res.string.villain_morvath_backstory) + "\n" + stringResource(Res.string.villain_morvath_description))
                                        AttackerType.XARITHON_THE_SHADOW_DRAGON.name ->
                                            stringResource(Res.string.villain_xarithon_title) to
                                                (stringResource(Res.string.villain_xarithon_backstory) + "\n" + stringResource(Res.string.villain_xarithon_description))
                                        AttackerType.CAPTAIN_RODERICH.name ->
                                            stringResource(Res.string.villain_roderich_title) to
                                                (stringResource(Res.string.villain_roderich_backstory) + "\n" + stringResource(Res.string.villain_roderich_description))
                                        AttackerType.THE_KRAKEN.name ->
                                            stringResource(Res.string.villain_kraken_title) to
                                                (stringResource(Res.string.villain_kraken_backstory) + "\n" + stringResource(Res.string.villain_kraken_description))
                                        else ->
                                            stringResource(Res.string.villain_enters_title) to
                                                stringResource(Res.string.villain_enters_text)
                                    }
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = villainTitle,
                                    text = villainText,
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                    supports = gameState.level.supports,
                                    backgroundOverride = villainMessageBackground(msg.name),
                                    accentColorOverride = villainMessageButtonColor(msg.name),
                                    iconAttackerTypeOverride = villainType,
                                )
                            }
                            GameMessageType.VILLAIN_DEFEATED -> {
                                val villainType = attackerTypeFromMessageName(msg.name)
                                val villainName = villainType?.villainName ?: stringResource(Res.string.villain)
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.villain_defeated_title, villainName),
                                    text = stringResource(Res.string.villain_defeated_text),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                    backgroundOverride = villainMessageBackground(msg.name),
                                    accentColorOverride = villainMessageButtonColor(msg.name),
                                    iconAttackerTypeOverride = villainType,
                                )
                            }
                            GameMessageType.SILAS_MIRROR_HIT ->
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.villain_silas_mirror_hit_title),
                                    text = stringResource(Res.string.villain_silas_mirror_hit_text),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                    backgroundOverride = villainMessageBackground(AttackerType.SILAS_THE_MASKMASTER.name),
                                    accentColorOverride = villainMessageButtonColor(AttackerType.SILAS_THE_MASKMASTER.name),
                                    iconAttackerTypeOverride = AttackerType.SILAS_THE_MASKMASTER,
                                )
                            GameMessageType.COVEN_SWAP -> {
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.villain_coven_swap_title),
                                    text = stringResource(Res.string.villain_coven_swap_text),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                    backgroundOverride = villainMessageBackground(AttackerType.GRAND_COVEN_MOTHER_SYBILLA.name),
                                    accentColorOverride = villainMessageButtonColor(AttackerType.GRAND_COVEN_MOTHER_SYBILLA.name),
                                    iconAttackerTypeOverride = AttackerType.GRAND_COVEN_MOTHER_SYBILLA,
                                )
                            }
                            GameMessageType.WAAAGH_FRENZY ->
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.EWHAD,
                                    title = stringResource(Res.string.waaagh_frenzy_title),
                                    text = stringResource(Res.string.waaagh_frenzy_message),
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                    backgroundOverride = Res.drawable.message_background_waaagh,
                                    accentColorOverride = Color(0xFFD32F2F),
                                    topImageOverride = Res.drawable.waaagh_image,
                                    topImageFillWidth = true,
                                    contentTopOffset = 48.dp,
                                )
                            GameMessageType.STORY_INTRO -> {
                                val levelEditorId = msg.name
                                if (levelEditorId != null) {
                                    val fortressTitle = stringResource(Res.string.level_the_fortress_title)
                                    // Map level editor ID to (NarrativeMessageType, title, storyText)
                                    val story: Triple<NarrativeMessageType, String, String>? =
                                        when (levelEditorId) {
                                            "the_first_wave" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_first_wave_title),
                                                    stringResource(Res.string.story_the_first_wave),
                                                )
                                            "mixed_forces" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_mixed_forces_title),
                                                    stringResource(Res.string.story_mixed_forces),
                                                )
                                            "the_ork_invasion" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_ork_invasion_title),
                                                    stringResource(Res.string.story_the_ork_invasion),
                                                )
                                            "dark_magic_rises" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_dark_magic_rises_title),
                                                    stringResource(Res.string.story_dark_magic_rises),
                                                )
                                            "prison_break" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_prison_break_title),
                                                    stringResource(Res.string.story_prison_break),
                                                )
                                            "defend_the_city" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_defend_the_city_title),
                                                    stringResource(Res.string.story_defend_the_city),
                                                )
                                            "the_plains" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_plains_title),
                                                    stringResource(Res.string.story_the_plains),
                                                )
                                            "the_spiral_challenge" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_spiral_challenge_title),
                                                    stringResource(Res.string.story_the_spiral_challenge),
                                                )
                                            "the_dance" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_dance_title),
                                                    stringResource(Res.string.story_the_dance),
                                                )
                                            "the_rush" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_rush_title),
                                                    stringResource(Res.string.story_the_rush),
                                                )
                                            "the_woods_first_incursion" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_woods_first_incursion_title),
                                                    stringResource(Res.string.story_the_woods_first_incursion),
                                                )
                                            "the_woods_full_assault" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_woods_full_assault_title),
                                                    stringResource(Res.string.story_the_woods_full_assault),
                                                )
                                            "the_fast_and_furious" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_fast_and_furious_title),
                                                    stringResource(Res.string.story_the_fast_and_furious),
                                                )
                                            "the_cross" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_cross_title),
                                                    stringResource(Res.string.story_the_cross),
                                                )
                                            "the_winding_path" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_winding_path_title),
                                                    stringResource(Res.string.story_the_winding_path),
                                                )
                                            "the_fortress_1_first_attacks" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    "$fortressTitle - ${stringResource(
                                                        Res.string.level_the_fortress_1_first_attacks_subtitle,
                                                    )}",
                                                    stringResource(Res.string.story_the_fortress_1_first_attacks),
                                                )
                                            "the_fortress_2_orks_marching" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    "$fortressTitle - ${stringResource(
                                                        Res.string.level_the_fortress_2_orks_marching_subtitle,
                                                    )}",
                                                    stringResource(Res.string.story_the_fortress_2_orks_marching),
                                                )
                                            "the_fortress_3_necromancer" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    "$fortressTitle - ${stringResource(
                                                        Res.string.level_the_fortress_3_necromancer_subtitle,
                                                    )}",
                                                    stringResource(Res.string.story_the_fortress_3_necromancer),
                                                )
                                            "the_fortress_4_magic_assault" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    "$fortressTitle - ${stringResource(
                                                        Res.string.level_the_fortress_4_magic_assault_subtitle,
                                                    )}",
                                                    stringResource(Res.string.story_the_fortress_4_magic_assault),
                                                )
                                            "the_fortress_5_mighty_forces" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    "$fortressTitle - ${stringResource(
                                                        Res.string.level_the_fortress_5_mighty_forces_subtitle,
                                                    )}",
                                                    stringResource(Res.string.story_the_fortress_5_mighty_forces),
                                                )
                                            "the_final_stand" ->
                                                Triple(
                                                    NarrativeMessageType.EWHAD,
                                                    stringResource(Res.string.level_the_final_stand_title),
                                                    stringResource(Res.string.story_the_final_stand),
                                                )
                                            "the_creek" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_creek_title),
                                                    stringResource(Res.string.story_the_creek),
                                                )
                                            "the_island" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_island_title),
                                                    stringResource(Res.string.story_the_island),
                                                )
                                            "the_river" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_river_title),
                                                    stringResource(Res.string.story_the_river),
                                                )
                                            "creek_valley" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_creek_valley_title),
                                                    stringResource(Res.string.story_creek_valley),
                                                )
                                            "maelstrom" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_maelstrom_title),
                                                    stringResource(Res.string.story_maelstrom),
                                                )
                                            "the_tower_of_the_hermit" ->
                                                Triple(
                                                    NarrativeMessageType.STORY,
                                                    stringResource(Res.string.level_the_tower_of_the_hermit_title),
                                                    stringResource(Res.string.story_the_tower_of_the_hermit),
                                                )
                                            else -> null
                                        }
                                    if (story != null) {
                                        val (narrativeType, storyTitle, storyText) = story
                                        NarrativeMessageDialog(
                                            type = narrativeType,
                                            title = storyTitle,
                                            text = storyText,
                                            onDismiss = { onDismissGameMessage?.invoke() },
                                            supports = gameState.level.supports,
                                        )
                                    } else {
                                        // No predefined story text for this level (e.g. user-created
                                        // levels): dismiss so it does not block later queued messages
                                        // (scripted-event messages, target-taken, etc.).
                                        LaunchedEffect(msg) { onDismissGameMessage?.invoke() }
                                    }
                                } else {
                                    LaunchedEffect(msg) { onDismissGameMessage?.invoke() }
                                }
                            }
                            GameMessageType.EVENT_MESSAGE -> {
                                val messageKey = msg.name
                                val eventText =
                                    if (messageKey != null) {
                                        com.hyperether.resources.LocalizedStrings
                                            .get(messageKey, com.hyperether.resources.currentLanguage.value)
                                    } else {
                                        ""
                                    }
                                NarrativeMessageDialog(
                                    type = NarrativeMessageType.STORY,
                                    title = stringResource(Res.string.event_message_title),
                                    text = eventText,
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                    eventGains = msg.eventActions,
                                )
                            }
                            else ->
                                GameEventMessageDialog(
                                    message = msg,
                                    onDismiss = { onDismissGameMessage?.invoke() },
                                )
                        }
                    }

                    // Stop demo mode confirmation dialog
                    if (showStopDemoDialog && onStopDemoMode != null) {
                        AlertDialog(
                            onDismissRequest = { showStopDemoDialog = false },
                            title = { Text(stringResource(Res.string.stop_demo_title)) },
                            text = { Text(stringResource(Res.string.stop_demo_message)) },
                            confirmButton = {
                                Button(onClick = {
                                    showStopDemoDialog = false
                                    onStopDemoMode.invoke()
                                }) {
                                    Text(stringResource(Res.string.yes))
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showStopDemoDialog = false }) {
                                    Text(stringResource(Res.string.no))
                                }
                            },
                        )
                    }
                }
            }

            // Coin fly-to-counter animation overlay: sits above header + map so coins can travel
            // between them. No-op when animations are disabled.
            CoinFlightOverlay()
        }
    }
}

/**
 * Helper function to determine if target selection should be preserved after an attack.
 * Selection is kept active if:
 * - The tower still has action points remaining after the attack
 * - The target enemy is still alive (not defeated)
 *
 * This allows players to continue attacking the same target with multi-action towers.
 */
private fun shouldKeepTargetSelection(
    gameState: GameState,
    defenderId: Int,
    targetId: Int,
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    val target = gameState.attackers.find { it.id == targetId } ?: return false

    return defender.actionsRemaining.value > 0 && !target.isDefeated.value
}

/**
 * Helper function to determine if target selection should be preserved after a position-based attack.
 * Same logic as shouldKeepTargetSelection but uses position instead of ID to find the target.
 */
private fun shouldKeepTargetSelectionForPosition(
    gameState: GameState,
    defenderId: Int,
    targetPosition: Position,
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    val target = gameState.attackers.find { it.position.value == targetPosition } ?: return false

    return defender.actionsRemaining.value > 0 && !target.isDefeated.value
}

/**
 * Helper function to determine if a placement mode should be preserved after placing a trap or barricade.
 * Placement mode is kept active if the tower still has action points remaining.
 *
 * This allows players to place multiple traps or barricades with towers that have multiple actions per turn.
 */
private fun shouldKeepPlacementMode(
    gameState: GameState,
    defenderId: Int,
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    return defender.actionsRemaining.value > 0
}

/**
 * Compact keyboard-hint row shown on the placement / spell-targeting instruction cards. It documents
 * the keys used to move the on-map placement cursor and to confirm the placement, so keyboard users
 * can discover them. Renders nothing when the shortcut-hint setting is disabled (each chip self-guards).
 */
@Composable
private fun PlacementKeyboardHints(modifier: Modifier = Modifier) {
    if (!AppSettings.showButtonShortcutHints.value) return
    val hintColor = LocalContentColor.current.copy(alpha = 0.75f)
    val nextBinding = formatShortcutBindingForDisplay(AppSettings.shortcutNextEnemyTarget.value)
    val prevBinding = formatShortcutBindingForDisplay(AppSettings.shortcutPrevEnemyTarget.value)
    val rowUpBinding = formatShortcutBindingForDisplay(AppSettings.shortcutPanUp.value)
    val rowDownBinding = formatShortcutBindingForDisplay(AppSettings.shortcutPanDown.value)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ShortcutKeyChip(text = nextBinding, color = hintColor)
        ShortcutKeyChip(text = prevBinding, color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_move),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShortcutKeyChip(text = rowUpBinding, color = hintColor)
        ShortcutKeyChip(text = rowDownBinding, color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_row),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShortcutKeyChip(text = "Enter", color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_place),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
    }
}

/**
 * Compact keyboard-hint row shown beneath the support bar. It documents how to navigate between the
 * support elements (placeable objects, spell tokens, cooldown powers) with the keyboard: left/right
 * to move the focus cursor and Enter to activate the focused element. Renders nothing when the
 * shortcut-hint setting is disabled (each chip self-guards).
 */
@Composable
private fun SupportBarKeyboardHints(modifier: Modifier = Modifier) {
    if (!AppSettings.showButtonShortcutHints.value) return
    val hintColor = LocalContentColor.current.copy(alpha = 0.75f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // \u2190\u2192 = left/right arrows; ShortcutKeyChip renders them as Material Symbol icons.
        ShortcutKeyChip(text = "\u2190\u2192", color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_move),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShortcutKeyChip(text = "Enter", color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_nav_select),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
    }
}

/**
 * Returns a villain's message frame from drawables named `message_background_<abbreviated_villain_name>`.
 *
 * The lookup is automatic and uses [AttackerType.villainName]. Example:
 * `message_background_garokk.png` is used for "Garokk".
 */
private fun villainMessageBackground(name: String?): org.jetbrains.compose.resources.DrawableResource? {
    val attackerType = attackerTypeFromMessageName(name) ?: return null
    if (!attackerType.isVillain) return null
    val villainShortName = attackerType.villainName ?: return null
    val normalizedVillainName = villainShortName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    if (normalizedVillainName.isEmpty()) return null
    return Res.allDrawableResources["message_background_$normalizedVillainName"]
}

/**
 * Returns a villain-specific button color for narrative message dialogs.
 *
 * Each villain gets a thematic button color based on their visual design and personality.
 * For non-villains or unrecognized types, returns null (uses default color).
 *
 * The color should be distinct, fitting to the villain's character, and maintain good
 * contrast with white text on the button.
 */
private fun villainMessageButtonColor(name: String?): Color? {
    val attackerType = attackerTypeFromMessageName(name) ?: return null
    if (!attackerType.isVillain) return null

    return when (attackerType) {
        // Horde warchief – aggressive red
        AttackerType.GAROKK -> Color(0xFFE31C1C)
        // Goblin summoner – olive green
        AttackerType.SNOTLING_BOSS -> Color(0xFF6B8E23)
        // Shaman summoner – dark indigo
        AttackerType.MORGUK_BONEWHISPER -> Color(0xFF4B0082)
        // Giant spider – earthy brown
        AttackerType.ARAXXA -> Color(0xFF2F1B0C)
        // Rat warlord – dark steel gray
        AttackerType.BARON_RATTERZAHN -> Color(0xFF4B4F56)
        // Illusionist – deep purple magic
        AttackerType.SILAS_THE_MASKMASTER -> Color(0xFF5D3A8C)
        // Mirror images use the same color as Silas
        AttackerType.SILAS_MIRROR_IMAGE -> Color(0xFF5D3A8C)
        // Undead shieldmaiden – dark armor blue
        AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> Color(0xFF2A2F3A)
        // Undead prince – soul-reaper purple
        AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> Color(0xFF28304D)
        // Witch coven leader – magical purple
        AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> Color(0xFF5B2C6F)
        // Green witch – healing green
        AttackerType.HAGA -> Color(0xFF006400)
        // Red witch – disabling crimson
        AttackerType.ZUSSA -> Color(0xFF6B0000)
        // Corrupted nature – dark emerald
        AttackerType.SYLVANAS_THE_MOLDING -> Color(0xFF1A3A20)
        // Archmage forbidden astral magic – deep void blue
        AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> Color(0xFF0D1B3E)
        // Dragon cultist – deep dragon-fire crimson
        AttackerType.IGNIS_VA_THE_DRAGONVOICE -> Color(0xFF8B1A00)
        // Shadowmaster – black-violet shadow mist
        AttackerType.MORVATH_THE_SHADOWMASTER -> Color(0xFF2A003A)
        // Shadow dragon – void purple-black
        AttackerType.XARITHON_THE_SHADOW_DRAGON -> Color(0xFF1E0040)
        // Pirate captain – deep ocean blue
        AttackerType.CAPTAIN_RODERICH -> Color(0xFF0D4E74)
        // Ancient deep-sea horror – dark abyss teal
        AttackerType.THE_KRAKEN -> Color(0xFF0A3D4A)
        // Default for EWHAD and other villains (if any added in future)
        else -> null
    }
}

/**
 * Safely converts a queued game-message name to [AttackerType].
 * Returns null when the name is null or does not map to a valid enum constant.
 *
 * Villain narrative messages carry the attacker type as a string in [GameMessage.name],
 * so this helper prevents crashes from invalid payloads and keeps message rendering resilient.
 */
private fun attackerTypeFromMessageName(name: String?): AttackerType? = name?.let { runCatching { AttackerType.valueOf(it) }.getOrNull() }
