package de.egril.defender.ui.editor.level

import androidx.compose.runtime.*
import de.egril.defender.editor.*
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position

/**
 * Editable state of [LevelEditorView]. The view creates it once with `remember` (no keys), so the
 * initial values come from the level the view was first opened with.
 */
internal class LevelEditorViewState(level: EditorLevel) {
    private val initialSpawnTimeline = level.spawnTimeline()

    var title by mutableStateOf(level.title)
    var subtitle by mutableStateOf(level.subtitle)
    var author by mutableStateOf(level.author)
    var communityDescription by mutableStateOf(level.communityDescription)
    var startCoins by mutableStateOf(level.startCoins.toString())
    var startHP by mutableStateOf(level.startHealthPoints.toString())
    var selectedMapId by mutableStateOf(level.mapId)
    // The spawn plan is edited as one timeline of turns; loops wrap turn ranges and may be nested.
    var enemySpawns by mutableStateOf(initialSpawnTimeline.spawns.toMutableList())
    var spawnLoops by mutableStateOf(initialSpawnTimeline.loops)
    var availableTowersState by mutableStateOf(level.availableTowers.toSet())
    var waypointsState by mutableStateOf(level.waypoints.toMutableList())
    var singleHitTargetOrder by mutableStateOf(level.singleHitTargetOrder)
    var initialDataState by mutableStateOf(level.getEffectiveInitialData())
    var testingOnly by mutableStateOf(level.testingOnly)
    var allowAutoAttack by mutableStateOf(level.allowAutoAttack)
    var connectedToPreviousLevel by mutableStateOf(level.connectedToPreviousLevel)
    var autoTowerBase by mutableStateOf(level.autoTowerBase)
    var autoTowerBaseExtraCost by mutableStateOf(level.autoTowerBaseExtraCost.toString())
    var isSandbox by mutableStateOf(level.isSandbox)
    var waaghEnabled by mutableStateOf(level.waaghEnabled)
    var supportsState by mutableStateOf(level.supports)
    var eventsState by mutableStateOf(level.events)
    var showEnemyDialog by mutableStateOf(false)
    var showEnemyDialogForTurn by mutableStateOf(1)
    var lastEnemyDialogType by mutableStateOf(AttackerType.GOBLIN)
    var lastEnemyDialogLevel by mutableStateOf("1")
    var lastEnemyDialogAmount by mutableStateOf("1")
    var lastEnemyDialogSpawnPoint by mutableStateOf<Position?>(null)
    var showSaveAsDialog by mutableStateOf(false)
    var showOfficialLevelSavedWarning by mutableStateOf(false)
    var pendingLevelToSave by mutableStateOf<EditorLevel?>(null)
    var selectedTabIndex by mutableStateOf(0)
    var requestedEnemySpawnTurn by mutableStateOf<Int?>(null)
    var enemySpawnTurnRequestNonce by mutableStateOf(0)
    var communityUploadStatus by mutableStateOf<String?>(null)
    var isUploadingToCommunity by mutableStateOf(false)
    var showCommunityUploadConfirm by mutableStateOf(false)
    var showRemoveAllTurnsDialog by mutableStateOf(false)
    // Track the maximum turn number explicitly to support empty turns
    var maxTurnNumber by mutableStateOf(initialSpawnTimeline.maxTurn)
    var enemySpawnUndoHistory by mutableStateOf(listOf<EnemySpawnEditorSnapshot>())
    var enemySpawnRedoHistory by mutableStateOf(listOf<EnemySpawnEditorSnapshot>())

    fun currentEnemySpawnSnapshot(): EnemySpawnEditorSnapshot =
        EnemySpawnEditorSnapshot(
            enemySpawns = enemySpawns.toMutableList(),
            maxTurnNumber = maxTurnNumber,
            spawnLoops = spawnLoops,
        )

    fun applyEnemySpawnSnapshot(snapshot: EnemySpawnEditorSnapshot) {
        enemySpawns = snapshot.enemySpawns.toMutableList()
        maxTurnNumber = snapshot.maxTurnNumber
        spawnLoops = snapshot.spawnLoops
    }

    fun updateEnemySpawnState(
        newEnemySpawns: MutableList<EditorEnemySpawn> = enemySpawns.toMutableList(),
        newMaxTurnNumber: Int = maxTurnNumber,
        newSpawnLoops: List<EditorSpawnLoop> = spawnLoops,
    ) {
        if (newEnemySpawns == enemySpawns && newMaxTurnNumber == maxTurnNumber && newSpawnLoops == spawnLoops) return
        enemySpawnUndoHistory = (enemySpawnUndoHistory + currentEnemySpawnSnapshot()).takeLast(40)
        enemySpawnRedoHistory = emptyList()
        enemySpawns = newEnemySpawns
        maxTurnNumber = newMaxTurnNumber
        spawnLoops = newSpawnLoops
    }

    fun updateSpawnTimeline(timeline: EditorSpawnTimeline) = updateEnemySpawnState(timeline.spawns.toMutableList(), timeline.maxTurn, timeline.loops)

    fun undoEnemySpawns() {
        val snapshot = enemySpawnUndoHistory.lastOrNull() ?: return
        enemySpawnUndoHistory = enemySpawnUndoHistory.dropLast(1)
        enemySpawnRedoHistory = (enemySpawnRedoHistory + currentEnemySpawnSnapshot()).takeLast(40)
        applyEnemySpawnSnapshot(snapshot)
    }

    fun redoEnemySpawns() {
        val snapshot = enemySpawnRedoHistory.lastOrNull() ?: return
        enemySpawnRedoHistory = enemySpawnRedoHistory.dropLast(1)
        enemySpawnUndoHistory = (enemySpawnUndoHistory + currentEnemySpawnSnapshot()).takeLast(40)
        applyEnemySpawnSnapshot(snapshot)
    }
}
