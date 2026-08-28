package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.model.DefenderType
import de.egril.defender.model.isRealVillain
import de.egril.defender.ui.*
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.editor.CreateLevelDialog
import de.egril.defender.ui.editor.SaveAsDialog
import de.egril.defender.ui.editor.getDefaultAuthorName
import de.egril.defender.ui.editor.level.enemies.EnemySpawnsTab
import de.egril.defender.ui.editor.level.tower.TowersTab
import de.egril.defender.ui.editor.level.waypoint.WaypointsTab
import de.egril.defender.ui.icon.*
import de.egril.defender.ui.loadgame.SavefileLocationChip
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.official_level_saved_warning_message
import defender_of_egril.composeapp.generated.resources.official_level_saved_warning_title
import kotlinx.coroutines.launch
import kotlin.random.Random

internal data class LevelEditorTabIndices(
    val levelInfo: Int,
    val designPreview: Int,
    val enemySpawns: Int?,
    val towers: Int,
    val waypoints: Int,
    val initialSetup: Int,
    val supports: Int,
    val events: Int?,
)

internal fun levelEditorTabIndices(isSandbox: Boolean): LevelEditorTabIndices {
    var nextIndex = 0
    val levelInfo = nextIndex++
    val designPreview = nextIndex++
    val enemySpawns = if (isSandbox) null else nextIndex++
    val towers = nextIndex++
    val waypoints = nextIndex++
    val initialSetup = nextIndex++
    val supports = nextIndex++
    val events = if (isSandbox) null else nextIndex++

    return LevelEditorTabIndices(
        levelInfo = levelInfo,
        designPreview = designPreview,
        enemySpawns = enemySpawns,
        towers = towers,
        waypoints = waypoints,
        initialSetup = initialSetup,
        supports = supports,
        events = events,
    )
}

private data class EnemySpawnEditorSnapshot(
    val enemySpawns: MutableList<EditorEnemySpawn>,
    val maxTurnNumber: Int,
)

/**
 * Main content for the Level Editor tab
 */
@Composable
internal fun LevelEditorContent(
    initialEditingLevelId: String? = null,
    onStartPlaytest: ((EditorLevel, FocusedPlaytestType) -> Unit)? = null,
) {
    val levels = remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }
    var editingLevel by remember { mutableStateOf<EditorLevel?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showVillainUsage by remember { mutableStateOf(false) }
    var levelToDelete by remember { mutableStateOf<EditorLevel?>(null) }
    val iamState by de.egril.defender.iam.IamService.state

    LaunchedEffect(initialEditingLevelId, levels.value) {
        if (initialEditingLevelId != null) {
            val levelToOpen = levels.value.find { it.id == initialEditingLevelId }
            if (levelToOpen != null) {
                selectedLevelId = levelToOpen.id
                editingLevel = levelToOpen
            }
        }
    }

    if (editingLevel != null) {
        // Level editing view
        LevelEditorView(
            level = editingLevel!!,
            onSave = { updatedLevel ->
                EditorStorage.saveLevel(updatedLevel)
                levels.value = EditorStorage.getAllLevels()
                // Reload the level from storage to trigger UI updates
                editingLevel = EditorStorage.getLevel(updatedLevel.id)
            },
            onCancel = { editingLevel = null },
            onStartPlaytest = { playtestLevel, type -> onStartPlaytest?.invoke(playtestLevel, type) },
        )
    } else if (showVillainUsage) {
        VillainUsagePage(
            levels = levels.value,
            onBack = { showVillainUsage = false },
        )
    } else {
        // Level list view
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.levels),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { showVillainUsage = true }) {
                        Text(stringResource(Res.string.villain_usage))
                    }
                    Button(onClick = { showCreateDialog = true }) {
                        Text(stringResource(Res.string.create_new_level))
                    }
                }
            }

            Text(
                text = stringResource(Res.string.select_level_to_edit),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(levels.value) { level ->
                    LevelCard(
                        level = level,
                        isSelected = selectedLevelId == level.id,
                        onSelect = {
                            selectedLevelId = level.id
                            editingLevel = level
                        },
                        onCopy = {
                            // Copy the level with a new ID and " - Copy" suffix
                            val copyTitle = "${level.title} - Copy"
                            val sanitizedTitle =
                                copyTitle
                                    .trim()
                                    .lowercase()
                                    .replace(" ", "_")
                                    .replace(Regex("[^a-z0-9_]"), "")
                                    .replace(Regex("_+"), "_") // Collapse consecutive underscores
                            val newId = "${sanitizedTitle}_${Random.nextInt(1000, 9999)}"
                            val copiedLevel =
                                level.copy(
                                    id = newId,
                                    title = copyTitle,
                                    isOfficial = false, // Copied levels are always user levels
                                )
                            EditorStorage.saveLevel(copiedLevel)
                            levels.value = EditorStorage.getAllLevels()
                        },
                        onDelete = {
                            levelToDelete = level
                        },
                    )
                }
            }
        }
    }

    // Confirmation dialog for delete level
    if (levelToDelete != null) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete),
            message = stringResource(Res.string.confirm_delete_level),
            onDismiss = { levelToDelete = null },
            onConfirm = {
                EditorStorage.deleteLevel(levelToDelete!!.id)
                levels.value = EditorStorage.getAllLevels()
                if (selectedLevelId == levelToDelete!!.id) {
                    selectedLevelId = null
                }
                levelToDelete = null
            },
        )
    }

    if (showCreateDialog) {
        val defaultAuthor = getDefaultAuthorName(iamState)
        CreateLevelDialog(
            onDismiss = { showCreateDialog = false },
            defaultAuthor = defaultAuthor,
            onCreate = { title, author, template ->
                // Generate ID from title with underscores (lowercase, no "level_" prefix)
                val sanitizedTitle =
                    title
                        .trim()
                        .lowercase()
                        .replace(" ", "_")
                        .replace(Regex("[^a-z0-9_]"), "")
                        .replace(Regex("_+"), "_") // Collapse consecutive underscores
                val newId =
                    if (sanitizedTitle.isNotEmpty()) {
                        sanitizedTitle
                    } else {
                        "custom_${Random.nextInt(10000, 99999)}"
                    }
                // Get first ready-to-use map
                val firstReadyMap = EditorStorage.getAllMaps().filter { it.readyToUse }.firstOrNull()
                val baseLevel =
                    EditorLevel(
                        id = newId,
                        mapId = firstReadyMap?.id ?: "map_30x8",
                        title = title,
                        subtitle = "",
                        startCoins = 100,
                        startHealthPoints = 10,
                        enemySpawns = emptyList(),
                        availableTowers =
                            DefenderType.entries
                                .filter {
                                    it != DefenderType.DRAGONS_LAIR
                                }.toSet(),
                        author = author,
                    )
                val newLevel = applyLevelTemplate(baseLevel, firstReadyMap, template)
                EditorStorage.saveLevel(newLevel)
                levels.value = EditorStorage.getAllLevels()
                showCreateDialog = false
                editingLevel = newLevel
            },
        )
    }
}

@Composable
private fun LevelCard(
    level: EditorLevel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    // Check if any enemies are spawned outside valid spawn points
    val map = remember(level.mapId) { EditorStorage.getMap(level.mapId) }
    val villainTypes = remember(level.enemySpawns) { level.enemySpawns.presentVillainTypes() }
    val villainSummary = remember(level.enemySpawns) { level.enemySpawns.presentVillainSummary { it.villainName ?: it.displayName } }
    val hasEnemiesOutsideSpawnPoints =
        remember(level.enemySpawns, map) {
            val mapSpawnPoints = map?.getSpawnPoints()?.toSet() ?: emptySet()
            level.enemySpawns.any { spawn ->
                spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect() }
                            .padding(12.dp)
                            .padding(top = 24.dp), // Add top padding for the badges
                ) {
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    // Add warning badge if enemies are outside spawn points
                    if (hasEnemiesOutsideSpawnPoints) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            WarningBadge()
                        }
                    }
                    if (level.subtitle.isNotEmpty()) {
                        Text(
                            text = level.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "${stringResource(Res.string.file)}: ${level.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${stringResource(
                            Res.string.map_label,
                        )}: ${level.mapId} | ${stringResource(
                            Res.string.coins,
                        )}: ${level.startCoins} | ${stringResource(Res.string.hp_short)}: ${level.startHealthPoints}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${stringResource(Res.string.enemies)}: ${level.enemySpawns.size}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (villainSummary.isNotEmpty()) {
                        Text(
                            text = "${stringResource(if (villainTypes.size > 1) Res.string.villains else Res.string.villain)}: $villainSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                        )
                    }
                    Text(
                        text =
                            if (EditorStorage.isLevelReadyToPlay(
                                    level,
                                )
                            ) {
                                stringResource(Res.string.ready_to_use)
                            } else {
                                stringResource(Res.string.not_ready)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (EditorStorage.isLevelReadyToPlay(level)) Color.Green else Color.Red,
                    )
                }

                // Badges in upper right corner
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Test Level badge
                        if (level.testingOnly) {
                            Text(
                                text = stringResource(Res.string.test_level),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        // Ready/not ready check indicator
                        if (EditorStorage.isLevelReadyToPlay(level)) {
                            CheckmarkIcon(
                                size = 20.dp,
                                tint = Color.Green,
                            )
                        } else {
                            CrossIcon(
                                size = 20.dp,
                                tint = Color.Red,
                            )
                        }
                    }
                    // Official/User badge below the check
                    if (level.isOfficial) {
                        Text(
                            text = stringResource(Res.string.official_level),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    // Community badge: shown for levels downloaded from the community backend
                    if (level.isCommunity) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SavefileLocationChip(
                                label = stringResource(Res.string.savefile_chip_local),
                                color = MaterialTheme.colorScheme.tertiary,
                                onColor = MaterialTheme.colorScheme.onTertiary,
                                isMobile = false,
                            )
                            SavefileLocationChip(
                                label = stringResource(Res.string.savefile_chip_remote),
                                color = MaterialTheme.colorScheme.primary,
                                onColor = MaterialTheme.colorScheme.onPrimary,
                                isMobile = false,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.copy_level))
                }
                Button(
                    onClick = onDelete,
                    enabled = !level.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
    }
}

/**
 * View for editing a level
 */
@Composable
internal fun LevelEditorView(
    level: EditorLevel,
    onSave: (EditorLevel) -> Unit,
    onCancel: () -> Unit,
    onStartPlaytest: (EditorLevel, FocusedPlaytestType) -> Unit,
) {
    var title by remember { mutableStateOf(level.title) }
    var subtitle by remember { mutableStateOf(level.subtitle) }
    var author by remember { mutableStateOf(level.author) }
    var communityDescription by remember { mutableStateOf(level.communityDescription) }
    var startCoins by remember { mutableStateOf(level.startCoins.toString()) }
    var startHP by remember { mutableStateOf(level.startHealthPoints.toString()) }
    var selectedMapId by remember { mutableStateOf(level.mapId) }
    var enemySpawns by remember { mutableStateOf(level.enemySpawns.toMutableList()) }
    var availableTowersState by remember { mutableStateOf(level.availableTowers.toSet()) }
    var waypointsState by remember { mutableStateOf(level.waypoints.toMutableList()) }
    var initialDataState by remember { mutableStateOf(level.getEffectiveInitialData()) }
    var testingOnly by remember { mutableStateOf(level.testingOnly) }
    var allowAutoAttack by remember { mutableStateOf(level.allowAutoAttack) }
    var connectedToPreviousLevel by remember { mutableStateOf(level.connectedToPreviousLevel) }
    var isSandbox by remember { mutableStateOf(level.isSandbox) }
    var waaghEnabled by remember { mutableStateOf(level.waaghEnabled) }
    var supportsState by remember { mutableStateOf(level.supports) }
    var eventsState by remember { mutableStateOf(level.events) }

    // Update state when level changes (e.g., after reload from disk)
    LaunchedEffect(level.id, level.initialData, level.hashCode()) {
        if (LogConfig.ENABLE_UI_LOGGING) {
            println(
                "LevelEditor LaunchedEffect triggered: levelId=${level.id}, initialData=${level.initialData}, effectiveData=${level.getEffectiveInitialData()}",
            )
        }
        if (LogConfig.ENABLE_UI_LOGGING) {
            println(
                "  Defenders: ${level.getEffectiveInitialData().defenders.size}, Attackers: ${level.getEffectiveInitialData().attackers.size}",
            )
        }
        if (LogConfig.ENABLE_UI_LOGGING) {
            println("level data: $level")
        }

        initialDataState = level.getEffectiveInitialData()
    }
    var showEnemyDialog by remember { mutableStateOf(false) }
    var showEnemyDialogForTurn by remember { mutableStateOf(1) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showOfficialLevelSavedWarning by remember { mutableStateOf(false) }
    var pendingLevelToSave by remember { mutableStateOf<EditorLevel?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var requestedEnemySpawnTurn by remember { mutableStateOf<Int?>(null) }
    var enemySpawnTurnRequestNonce by remember { mutableStateOf(0) }
    val tabIndices = remember(isSandbox) { levelEditorTabIndices(isSandbox) }
    LaunchedEffect(isSandbox) {
        val visibleTabIndices =
            listOfNotNull(
                tabIndices.levelInfo,
                tabIndices.designPreview,
                tabIndices.enemySpawns,
                tabIndices.towers,
                tabIndices.waypoints,
                tabIndices.initialSetup,
                tabIndices.supports,
                tabIndices.events,
            )
        if (selectedTabIndex !in visibleTabIndices) {
            selectedTabIndex = tabIndices.levelInfo
        }
    }
    var communityUploadStatus by remember { mutableStateOf<String?>(null) }
    var isUploadingToCommunity by remember { mutableStateOf(false) }
    var showCommunityUploadConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showRemoveAllTurnsDialog by remember { mutableStateOf(false) }
    // Track the maximum turn number explicitly to support empty turns
    var maxTurnNumber by remember {
        mutableStateOf(level.enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0)
    }
    var enemySpawnUndoHistory by remember { mutableStateOf(listOf<EnemySpawnEditorSnapshot>()) }
    var enemySpawnRedoHistory by remember { mutableStateOf(listOf<EnemySpawnEditorSnapshot>()) }

    // Get only ready-to-use maps for selection
    val maps = remember { EditorStorage.getAllMaps().filter { it.readyToUse } }

    // Get all levels (excluding the current one) to check if any share the same map.
    // Used to enable/disable the "Connected to Previous Level" toggle.
    val allLevels = remember { EditorStorage.getAllLevels() }
    val hasOtherLevelsOnSameMap =
        remember(selectedMapId, level.id) {
            allLevels.any { it.id != level.id && it.mapId == selectedMapId }
        }

    // When the map changes and no other levels share it, reset the toggle.
    LaunchedEffect(hasOtherLevelsOnSameMap) {
        if (!hasOtherLevelsOnSameMap) {
            connectedToPreviousLevel = false
        }
    }

    // Get current map to access waypoint tiles and target
    val currentMap = remember(selectedMapId) { EditorStorage.getMap(selectedMapId) }

    fun currentEnemySpawnSnapshot(): EnemySpawnEditorSnapshot =
        EnemySpawnEditorSnapshot(
            enemySpawns = enemySpawns.toMutableList(),
            maxTurnNumber = maxTurnNumber,
        )

    fun applyEnemySpawnSnapshot(snapshot: EnemySpawnEditorSnapshot) {
        enemySpawns = snapshot.enemySpawns.toMutableList()
        maxTurnNumber = snapshot.maxTurnNumber
    }

    fun updateEnemySpawnState(
        newEnemySpawns: MutableList<EditorEnemySpawn> = enemySpawns.toMutableList(),
        newMaxTurnNumber: Int = maxTurnNumber,
    ) {
        if (newEnemySpawns == enemySpawns && newMaxTurnNumber == maxTurnNumber) return
        enemySpawnUndoHistory = (enemySpawnUndoHistory + currentEnemySpawnSnapshot()).takeLast(40)
        enemySpawnRedoHistory = emptyList()
        enemySpawns = newEnemySpawns
        maxTurnNumber = newMaxTurnNumber
    }

    // Villains are unique enemy heroes: only one of each type may be placed in a level.
    val presentVillainTypes = enemySpawns.filter { it.attackerType.isRealVillain }.map { it.attackerType }.toSet()

    // Check if any enemies are spawned outside valid spawn points
    val mapSpawnPoints = remember(currentMap) { currentMap?.getSpawnPoints()?.toSet() ?: emptySet() }
    val hasEnemiesOutsideSpawnPoints =
        remember(enemySpawns, mapSpawnPoints) {
            enemySpawns.any { spawn ->
                spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
            }
        }

    // Check readiness for each tab
    val coinsInt = startCoins.toIntOrNull() ?: 0
    val hpInt = startHP.toIntOrNull() ?: 0
    val isLevelInfoReady = coinsInt > 0 && hpInt > 0
    val isEnemySpawnsReady = isSandbox || enemySpawns.isNotEmpty()
    val hasInitialSetupTowerSupport = supportsState.isNotEmpty() || initialDataState.defenders.isNotEmpty()
    val hasInitialTowerBases = initialDataState.barricades.any { it.supportsTower }
    val mapNeedsNoBuildFallback = currentMap?.allowNoBuildableTiles == true && currentMap.getBuildAreas().isEmpty()
    val hasNoBuildFallback = supportsState.isNotEmpty() || hasInitialTowerBases
    val hasNoBuildFallbackIssue = mapNeedsNoBuildFallback && !hasNoBuildFallback
    val isTowersReady = availableTowersState.isNotEmpty() || hasInitialSetupTowerSupport
    val showNoTowersWarning = availableTowersState.isEmpty() && hasInitialSetupTowerSupport
    // Waypoints are optional, but if present they should be valid
    val isWaypointsValid = areWaypointsValid(waypointsState, currentMap, level)
    val draftLevel =
        remember(
            title,
            subtitle,
            author,
            communityDescription,
            selectedMapId,
            startCoins,
            startHP,
            enemySpawns,
            availableTowersState,
            waypointsState,
            testingOnly,
            allowAutoAttack,
            connectedToPreviousLevel,
            isSandbox,
            supportsState,
            eventsState,
            initialDataState,
        ) {
            level.copy(
                title = title,
                subtitle = subtitle,
                author = author,
                communityDescription = communityDescription,
                mapId = selectedMapId,
                startCoins = startCoins.toIntOrNull() ?: 100,
                startHealthPoints = startHP.toIntOrNull() ?: 10,
                enemySpawns = if (isSandbox) emptyList() else enemySpawns.toList(),
                availableTowers = availableTowersState,
                waypoints = waypointsState.toList(),
                testingOnly = testingOnly,
                allowAutoAttack = allowAutoAttack,
                connectedToPreviousLevel = connectedToPreviousLevel,
                isSandbox = isSandbox,
                waaghEnabled = waaghEnabled,
                supports = supportsState,
                events = eventsState,
                initialData = initialDataState,
            )
        }
    val levelDesignSummary = remember(draftLevel, currentMap) { analyzeLevelDesign(draftLevel, currentMap) }
    val waveArrivals = remember(draftLevel, currentMap) { buildWaveArrivalBuckets(draftLevel, currentMap) }
    val levelConsistencySummary = remember(draftLevel, currentMap) { analyzeLevelMapConsistency(draftLevel, currentMap) }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Official level info banner
        if (level.isOfficial) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InfoIcon(size = 20.dp)
                    Text(
                        text = stringResource(Res.string.official_level_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // Title above tabs
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = "${stringResource(Res.string.level_title)}: ${level.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            // Official badge
            if (level.isOfficial) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = stringResource(Res.string.official_level),
                            fontSize = 10.sp,
                        )
                    },
                    colors =
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            labelColor = MaterialTheme.colorScheme.primary,
                        ),
                    modifier = Modifier.height(24.dp),
                )
            }
        }

        // Tab Row with badges
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == tabIndices.levelInfo,
                onClick = { selectedTabIndex = tabIndices.levelInfo },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(Res.string.level_info_tab))
                        if (!isLevelInfoReady) {
                            RedDotBadge()
                        } else if (hasNoBuildFallbackIssue) {
                            WarningBadge()
                        }
                    }
                },
            )
            Tab(
                selected = selectedTabIndex == tabIndices.designPreview,
                onClick = { selectedTabIndex = tabIndices.designPreview },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(Res.string.design_preview))
                    }
                },
            )
            if (!isSandbox) {
                Tab(
                    selected = selectedTabIndex == tabIndices.enemySpawns,
                    onClick = { selectedTabIndex = requireNotNull(tabIndices.enemySpawns) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(stringResource(Res.string.enemy_spawns_tab))
                            if (!isEnemySpawnsReady) {
                                RedDotBadge()
                            } else if (hasEnemiesOutsideSpawnPoints) {
                                WarningBadge()
                            }
                        }
                    },
                )
            }
            Tab(
                selected = selectedTabIndex == tabIndices.towers,
                onClick = { selectedTabIndex = tabIndices.towers },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(Res.string.towers_tab))
                        if (!isTowersReady) {
                            RedDotBadge()
                        } else if (showNoTowersWarning) {
                            WarningBadge()
                        }
                    }
                },
            )
            Tab(
                selected = selectedTabIndex == tabIndices.waypoints,
                onClick = { selectedTabIndex = tabIndices.waypoints },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(Res.string.waypoints_tab))
                        if (!isWaypointsValid) {
                            RedDotBadge()
                        }
                    }
                },
            )
            Tab(
                selected = selectedTabIndex == tabIndices.initialSetup,
                onClick = { selectedTabIndex = tabIndices.initialSetup },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(Res.string.initial_setup))
                    }
                },
            )
            Tab(
                selected = selectedTabIndex == tabIndices.supports,
                onClick = { selectedTabIndex = tabIndices.supports },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(Res.string.supports_tab))
                    }
                },
            )
            if (!isSandbox) {
                Tab(
                    selected = selectedTabIndex == tabIndices.events,
                    onClick = { selectedTabIndex = requireNotNull(tabIndices.events) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(stringResource(Res.string.events_tab))
                        }
                    },
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTabIndex) {
                tabIndices.levelInfo ->
                    LevelInfoTab(
                        title = title,
                        onTitleChange = { title = it },
                        subtitle = subtitle,
                        onSubtitleChange = { subtitle = it },
                        author = author,
                        onAuthorChange = { author = it },
                        communityDescription = communityDescription,
                        onCommunityDescriptionChange = { communityDescription = it },
                        selectedMapId = selectedMapId,
                        onMapChange = { selectedMapId = it },
                        maps = maps,
                        startCoins = startCoins,
                        onStartCoinsChange = { startCoins = it },
                        startHP = startHP,
                        onStartHPChange = { startHP = it },
                        testingOnly = testingOnly,
                        onTestingOnlyChange = { testingOnly = it },
                        allowAutoAttack = allowAutoAttack,
                        onAllowAutoAttackChange = { allowAutoAttack = it },
                        connectedToPreviousLevel = connectedToPreviousLevel,
                        onConnectedToPreviousLevelChange = { connectedToPreviousLevel = it },
                        isSandbox = isSandbox,
                        onIsSandboxChange = { isSandbox = it },
                        waaghEnabled = waaghEnabled,
                        onWaaghEnabledChange = { waaghEnabled = it },
                        isOfficial = level.isOfficial,
                        canEnableConnectedToPreviousLevel = hasOtherLevelsOnSameMap,
                    )
                tabIndices.designPreview ->
                    LevelDesignOverview(
                        summary = levelDesignSummary,
                        arrivals = waveArrivals,
                        consistency = levelConsistencySummary,
                        onApplyTemplate = { template ->
                            val templated = applyLevelTemplate(draftLevel, currentMap, template)
                            startCoins = templated.startCoins.toString()
                            startHP = templated.startHealthPoints.toString()
                            updateEnemySpawnState(
                                newEnemySpawns = templated.enemySpawns.toMutableList(),
                                newMaxTurnNumber = templated.enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0,
                            )
                            availableTowersState = templated.availableTowers
                        },
                        onStartPlaytest = { type ->
                            onSave(draftLevel)
                            onStartPlaytest(createFocusedPlaytestLevel(draftLevel, levelDesignSummary, type), type)
                        },
                        playtestEnabled = currentMap != null && (isSandbox || enemySpawns.isNotEmpty()),
                        onOpenEnemySpawnTurn = { turn ->
                            requestedEnemySpawnTurn = turn
                            enemySpawnTurnRequestNonce++
                            tabIndices.enemySpawns?.let { selectedTabIndex = it }
                        },
                    )
                tabIndices.enemySpawns ->
                    EnemySpawnsTab(
                        enemySpawns = enemySpawns,
                        maxTurnNumber = maxTurnNumber,
                        onMaxTurnNumberChange = { updateEnemySpawnState(newMaxTurnNumber = it) },
                        onEnemySpawnsChange = { updateEnemySpawnState(newEnemySpawns = it) },
                        onShowEnemyDialog = { turn ->
                            showEnemyDialog = true
                            showEnemyDialogForTurn = turn
                        },
                        onShowRemoveAllTurnsDialog = { showRemoveAllTurnsDialog = true },
                        map = currentMap,
                        onApplyTemplate = { template, enemyKind, baseLevel ->
                            val (newSpawns, newMaxTurn) =
                                applySpawnTurnTemplate(enemySpawns, maxTurnNumber, currentMap, template, enemyKind, baseLevel)
                            updateEnemySpawnState(newEnemySpawns = newSpawns, newMaxTurnNumber = newMaxTurn)
                        },
                        onUndo = {
                            val snapshot = enemySpawnUndoHistory.lastOrNull() ?: return@EnemySpawnsTab
                            enemySpawnUndoHistory = enemySpawnUndoHistory.dropLast(1)
                            enemySpawnRedoHistory = (enemySpawnRedoHistory + currentEnemySpawnSnapshot()).takeLast(40)
                            applyEnemySpawnSnapshot(snapshot)
                        },
                        onRedo = {
                            val snapshot = enemySpawnRedoHistory.lastOrNull() ?: return@EnemySpawnsTab
                            enemySpawnRedoHistory = enemySpawnRedoHistory.dropLast(1)
                            enemySpawnUndoHistory = (enemySpawnUndoHistory + currentEnemySpawnSnapshot()).takeLast(40)
                            applyEnemySpawnSnapshot(snapshot)
                        },
                        canUndo = enemySpawnUndoHistory.isNotEmpty(),
                        canRedo = enemySpawnRedoHistory.isNotEmpty(),
                        requestedTurnToOpen = requestedEnemySpawnTurn,
                        turnOpenRequestNonce = enemySpawnTurnRequestNonce,
                    )
                tabIndices.towers ->
                    TowersTab(
                        availableTowers = availableTowersState,
                        onAvailableTowersChange = { availableTowersState = it },
                    )
                tabIndices.waypoints ->
                    WaypointsTab(
                        waypoints = waypointsState.toList(),
                        onWaypointsChange = { waypointsState = it.toMutableList() },
                        map = currentMap,
                        isValid = isWaypointsValid,
                    )
                tabIndices.initialSetup ->
                    de.egril.defender.ui.editor.level.initialsetup.InitialSetupTab(
                        initialData = initialDataState,
                        onInitialDataChange = { initialDataState = it },
                        map = currentMap,
                        availableTowers = availableTowersState,
                    )
                tabIndices.supports ->
                    de.egril.defender.ui.editor.level.supports.SupportsTab(
                        supports = supportsState,
                        onSupportsChange = { supportsState = it },
                    )
                tabIndices.events ->
                    de.egril.defender.ui.editor.level.events.EventsTab(
                        events = eventsState,
                        onEventsChange = { eventsState = it },
                        minePositions =
                            initialDataState.defenders
                                .filter { it.type == DefenderType.DWARVEN_MINE }
                                .map { it.position }
                                .toSet(),
                    )
            }
        }

        // Save/Cancel buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val updatedLevel =
                            level.copy(
                                title = title,
                                subtitle = subtitle,
                                author = author,
                                communityDescription = communityDescription,
                                mapId = selectedMapId,
                                startCoins = startCoins.toIntOrNull() ?: 100,
                                startHealthPoints = startHP.toIntOrNull() ?: 10,
                                // Sandbox levels have no scripted enemy waves; the player spawns test enemies while playing.
                                enemySpawns = if (isSandbox) emptyList() else enemySpawns.toList(),
                                availableTowers = availableTowersState,
                                waypoints = waypointsState.toList(),
                                testingOnly = testingOnly,
                                allowAutoAttack = allowAutoAttack,
                                connectedToPreviousLevel = connectedToPreviousLevel,
                                isSandbox = isSandbox,
                                waaghEnabled = waaghEnabled,
                                supports = supportsState,
                                events = eventsState,
                                initialData = initialDataState,
                            )

                        // Show warning dialog for official levels before saving
                        if (level.isOfficial && de.egril.defender.OfficialEditMode.enabled) {
                            pendingLevelToSave = updatedLevel
                            showOfficialLevelSavedWarning = true
                        } else {
                            // Save immediately if not an official level
                            onSave(updatedLevel)
                        }
                    },
                    enabled = !level.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.save_level))
                }

                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.save_as_new))
                }
            }

            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.cancel))
            }

            // Community upload button - only shown for non-official levels when user is authenticated
            val iamState by de.egril.defender.iam.IamService.state
            if (!level.isOfficial && iamState.isAuthenticated) {
                val storedCommunityJson =
                    remember(level.id) {
                        de.egril.defender.editor.EditorStorage
                            .getStoredCommunityLevelJson(level.id)
                    }
                val currentLevelJson =
                    remember(level.id, level.hashCode()) {
                        de.egril.defender.editor.EditorJsonSerializer
                            .serializeLevel(level)
                    }
                val storedCommunityLevel =
                    remember(level.id) {
                        de.egril.defender.editor.EditorStorage
                            .getCommunityLevel(level.id)
                    }
                val isMyUpload = storedCommunityLevel?.communityAuthorUsername == iamState.username
                val isChanged = storedCommunityJson != null && storedCommunityJson != currentLevelJson

                // Check if the map is a user map that needs auto-uploading
                val levelMap =
                    remember(level.mapId) {
                        de.egril.defender.editor.EditorStorage
                            .getMap(level.mapId)
                    }
                val mapAlsoUploaded =
                    levelMap != null &&
                        !levelMap.isOfficial &&
                        !levelMap.isCommunity &&
                        de.egril.defender.editor.EditorStorage
                            .getCommunityMap(level.mapId) == null

                fun doUpload(token: String) {
                    isUploadingToCommunity = true
                    communityUploadStatus = null
                    coroutineScope.launch {
                        val success =
                            de.egril.defender.save.BackendCommunityService
                                .uploadCommunityFile("LEVEL", level.id, currentLevelJson, token)
                        if (success) {
                            de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                                level.copy(
                                    isCommunity = true,
                                    communityAuthorUsername = iamState.username ?: "",
                                ),
                            )
                            // Auto-upload the map if it hasn't been uploaded yet
                            if (mapAlsoUploaded) {
                                val map =
                                    de.egril.defender.editor.EditorStorage
                                        .getMap(level.mapId)
                                if (map != null) {
                                    val mapJson =
                                        de.egril.defender.editor.EditorJsonSerializer
                                            .serializeMap(map)
                                    val mapSuccess =
                                        de.egril.defender.save.BackendCommunityService
                                            .uploadCommunityFile("MAP", level.mapId, mapJson, token)
                                    if (mapSuccess) {
                                        de.egril.defender.editor.EditorStorage.saveCommunityMap(
                                            map,
                                            iamState.username ?: "",
                                        )
                                    }
                                }
                            }
                            communityUploadStatus = "success"
                        } else {
                            communityUploadStatus = "error"
                        }
                        isUploadingToCommunity = false
                    }
                }

                if (storedCommunityJson == null) {
                    // Level not yet in community - show upload button
                    Button(
                        onClick = { showCommunityUploadConfirm = true },
                        enabled = !isUploadingToCommunity,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (isUploadingToCommunity) {
                                stringResource(Res.string.community_uploading)
                            } else {
                                stringResource(Res.string.upload_as_community_level)
                            },
                        )
                    }
                } else if (isMyUpload && isChanged) {
                    // Level exists in community and belongs to this user and has been changed - show update button
                    Button(
                        onClick = {
                            val token =
                                de.egril.defender.iam.IamService
                                    .getToken() ?: return@Button
                            doUpload(token)
                        },
                        enabled = !isUploadingToCommunity,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (isUploadingToCommunity) {
                                stringResource(Res.string.community_uploading)
                            } else {
                                stringResource(Res.string.update_community_level)
                            },
                        )
                    }
                }
                communityUploadStatus?.let { status ->
                    Text(
                        text =
                            if (status == "success") {
                                stringResource(Res.string.community_upload_success)
                            } else {
                                stringResource(Res.string.community_upload_failed)
                            },
                        color =
                            if (status == "success") {
                                androidx.compose.ui.graphics
                                    .Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Confirmation dialog before first community upload
                if (showCommunityUploadConfirm) {
                    val username = iamState.username ?: ""
                    val confirmMessage =
                        if (mapAlsoUploaded) {
                            stringResource(Res.string.upload_community_level_confirm_message, username) +
                                "\n\n" + stringResource(Res.string.upload_community_also_uploads_map)
                        } else {
                            stringResource(Res.string.upload_community_level_confirm_message, username)
                        }
                    de.egril.defender.ui.editor.ConfirmationDialog(
                        title = stringResource(Res.string.upload_community_confirm_title),
                        message = confirmMessage,
                        onDismiss = { showCommunityUploadConfirm = false },
                        onConfirm = {
                            showCommunityUploadConfirm = false
                            val token =
                                de.egril.defender.iam.IamService
                                    .getToken() ?: return@ConfirmationDialog
                            doUpload(token)
                        },
                    )
                }
            }
        }
    }

    if (showEnemyDialog) {
        AddEnemyDialog(
            presentVillainTypes = presentVillainTypes,
            turn = showEnemyDialogForTurn,
            map = currentMap,
            onDismiss = { showEnemyDialog = false },
            onAdd = { enemyType, level, amount, spawnPoint ->
                updateEnemySpawnState(
                    newEnemySpawns =
                        enemySpawns.toMutableList().apply {
                            repeat(amount) {
                                add(EditorEnemySpawn(enemyType, level, showEnemyDialogForTurn, spawnPoint))
                            }
                        },
                )
                showEnemyDialog = false
            },
        )
    }

    if (showRemoveAllTurnsDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_all_turns),
            message = stringResource(Res.string.confirm_remove_all_turns),
            onDismiss = { showRemoveAllTurnsDialog = false },
            onConfirm = {
                updateEnemySpawnState(mutableListOf(), 0)
                showRemoveAllTurnsDialog = false
            },
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            title = "Save Level As New",
            label = "Level Title",
            currentValue = title,
            onDismiss = { showSaveAsDialog = false },
            onSave = { newTitle ->
                // Generate ID from title with underscores
                val sanitizedTitle = newTitle.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId =
                    if (sanitizedTitle.isNotEmpty()) {
                        "level_$sanitizedTitle"
                    } else {
                        "level_copy_${Random.nextInt(10000, 99999)}"
                    }
                val newLevel =
                    level.copy(
                        id = newId,
                        title = newTitle,
                        subtitle = subtitle,
                        author = author,
                        mapId = selectedMapId,
                        startCoins = startCoins.toIntOrNull() ?: 100,
                        startHealthPoints = startHP.toIntOrNull() ?: 10,
                        enemySpawns = if (isSandbox) emptyList() else enemySpawns.toList(),
                        availableTowers = availableTowersState,
                        waypoints = waypointsState.toList(),
                        testingOnly = testingOnly,
                        allowAutoAttack = allowAutoAttack,
                        connectedToPreviousLevel = connectedToPreviousLevel,
                        isSandbox = isSandbox,
                        waaghEnabled = waaghEnabled,
                        supports = supportsState,
                        events = eventsState,
                        initialData = initialDataState,
                    )
                onSave(newLevel)
                showSaveAsDialog = false
            },
        )
    }

    // Warning dialog when saving official level
    if (showOfficialLevelSavedWarning) {
        AlertDialog(
            onDismissRequest = {
                showOfficialLevelSavedWarning = false
                pendingLevelToSave = null
            },
            title = { Text(stringResource(Res.string.official_level_saved_warning_title)) },
            text = { Text(stringResource(Res.string.official_level_saved_warning_message)) },
            confirmButton = {
                Button(onClick = {
                    showOfficialLevelSavedWarning = false
                    // Save the pending level after user acknowledges the warning
                    pendingLevelToSave?.let { onSave(it) }
                    pendingLevelToSave = null
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
        )
    }
}

private fun areWaypointsValid(
    waypointsState: MutableList<EditorWaypoint>,
    currentMap: EditorMap?,
    level: EditorLevel,
): Boolean {
    val targets = currentMap?.getTargets() ?: emptyList()
    if (targets.isEmpty() || currentMap == null) {
        return false
    }

    val spawnPoints = currentMap.getSpawnPoints()
    val tempLevel = level.copy(waypoints = waypointsState.toList())
    val validationResult = tempLevel.validateWaypointsDetailed(targets, spawnPoints)

    // Valid if validation passes
    return validationResult.isValid
}

/**
 * Red dot badge to indicate incomplete data
 */
@Composable
private fun RedDotBadge() {
    Box(
        modifier =
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.Red),
    )
}

/**
 * Warning badge to indicate issues that don't prevent playability
 */
@Composable
private fun WarningBadge() {
    Box(
        modifier =
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA500)), // Orange color
    )
}
