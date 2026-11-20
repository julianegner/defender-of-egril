package de.egril.defender.ui.gameplay

import androidx.compose.runtime.*
import de.egril.defender.model.*

/**
 * Local UI state of the gameplay screen. Held by a single remembered instance so the screen,
 * effect, keyboard and layout functions can share it without passing every value individually.
 */
internal class GamePlayScreenState {
    var selectedDefenderType by mutableStateOf<DefenderType?>(null)
    var selectedSupportObject by mutableStateOf<SupportObjectType?>(null)
    var selectedSupportFief by mutableStateOf<de.egril.defender.model.FiefType?>(null)
    var selectedDefenderId by mutableStateOf<Int?>(null)
    var selectedAttackerId by mutableStateOf<Int?>(null) // Add enemy selection
    var selectedTargetId by mutableStateOf<Int?>(null)
    var selectedTargetPosition by mutableStateOf<Position?>(null)
    var showCheatDialog by mutableStateOf(false)
    var showSandboxTools by mutableStateOf(false)

    // Sandbox: active map tile-paint type; when non-null, tapping a tile repaints it to this type.
    var sandboxPaintTileType by mutableStateOf<de.egril.defender.editor.TileType?>(null)

    // Sandbox: water flow direction/speed applied when painting RIVER tiles.
    var sandboxRiverFlow by mutableStateOf(de.egril.defender.model.RiverFlow.EAST)
    var sandboxRiverSpeed by mutableStateOf(1)
    var cheatCodeInput by mutableStateOf("")
    var showMineActionDialog by mutableStateOf(false)
    var selectedMineAction by mutableStateOf<MineAction?>(null)
    var selectedWizardAction by mutableStateOf<WizardAction?>(null) // For wizard magical trap placement
    var selectedBarricadeAction by mutableStateOf<BarricadeAction?>(null) // For spike/spear tower barricade placement

    // Removal confirmation dialog states
    var showRemoveBarricadeDialog by mutableStateOf(false)
    var barricadeToRemove by mutableStateOf<Position?>(null)
    var selectedBarricadePosition by mutableStateOf<Position?>(null) // For barricade info panel
    var selectedGargoyleId by mutableStateOf<Int?>(null) // For inactive gargoyle info panel
    var showRemoveTrapDialog by mutableStateOf(false)
    var trapToRemove by mutableStateOf<Position?>(null)

    var highlightEndTurnButton by mutableStateOf(false)
    var splitSelectorToggle by mutableStateOf(0)
    var splitSelectorExpanded by mutableStateOf(false)
    var tabScrollPosition by mutableStateOf<Position?>(null) // Tab-triggered scroll-to-tower
    var keyboardSelectedBuildTile by mutableStateOf<Position?>(null)

    // Position of the keyboard cursor while placing a support object or targeting a spell on the map.
    // The cursor cycles through the valid placement/target tiles (supportObjectPlacementTiles /
    // spellTargetPositions); the place key confirms the highlighted tile and a cancel key exits.
    var keyboardPlacementTile by mutableStateOf<Position?>(null)
    var nextSpawnPointIndex by mutableStateOf(0)
    var keyboardSpellFocusIndex by mutableStateOf(0)

    // Index (into visibleSupportSlots) of the support box under the keyboard-navigation cursor, or
    // null while the support bar is not being navigated. Left/right move the cursor, a select key
    // activates the focused box.
    var supportFocusIndex by mutableStateOf<Int?>(null)

    // Non-null means the keyboard undo/sell shortcut was pressed and the confirmation dialog is open.
    // The Boolean flag indicates whether this is an "undo" (true) or "sell" (false) operation.
    var keyboardUndoOrSellConfirmation by mutableStateOf<Pair<Int, Boolean>?>(null)

    // External dialog triggers for header icon shortcuts
    var triggerShowShortcuts by mutableStateOf(false)
    var triggerShowHelp by mutableStateOf(false)
    var triggerShowFeedback by mutableStateOf(false)
    var triggerShowSettings by mutableStateOf(false)

    var currentDigOutcome by mutableStateOf<DigOutcome?>(null)
    var currentDragonName by mutableStateOf<String?>(null) // Track dragon name for dig outcome
    var showDigOutcomeDialog by mutableStateOf(false)

    // Pending dig outcome waiting for the mine-dig animation to finish (1.5 s when animations ON)
    var pendingDigOutcome by mutableStateOf<DigOutcome?>(null)
    var pendingDigKey by mutableStateOf(0)
    var showOverlay by mutableStateOf(false) // MutableState for overlay visibility

    // 0 = both legend+enemies, 1 = legend only, 2 = enemy list only
    var overlayMode by mutableStateOf(0)
    var showSaveDialog by mutableStateOf(false) // Save dialog with comment
    var saveCommentInput by mutableStateOf("") // Comment input for save
    var showSaveConfirmation by mutableStateOf(false) // Save confirmation
    var showUnsavedChangesDialog by mutableStateOf(false) // Unsaved changes dialog
    var showEndTurnConfirmation by mutableStateOf(false) // End turn confirmation dialog
    var showAbortInstantTowerDialog by mutableStateOf(false) // Abort instant tower spell dialog
    var soundCaptionText by mutableStateOf<String?>(null)
    var soundCaptionSequence by mutableStateOf(0L)

    // Demo mode: "stop demo?" confirmation dialog
    var showStopDemoDialog by mutableStateOf(false)

    // Counter to trigger map focus requests after dialogs close (ensures ESC works after UI interactions)
    var mapRefocusTrigger by mutableStateOf(0)
}
