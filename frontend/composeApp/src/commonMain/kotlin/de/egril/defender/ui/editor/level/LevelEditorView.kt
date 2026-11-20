package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.config.LogConfig
import de.egril.defender.editor.*
import de.egril.defender.model.DEFAULT_AUTO_TOWER_BASE_EXTRA_COST
import de.egril.defender.model.DefenderType
import de.egril.defender.model.SpawnRepeatMode
import de.egril.defender.model.isRealVillain
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.editor.level.enemies.EnemySpawnsTab
import de.egril.defender.ui.editor.level.enemies.loopSummary
import de.egril.defender.ui.editor.level.enemies.spawnIssueTexts
import de.egril.defender.ui.editor.level.tower.TowersTab
import de.egril.defender.ui.editor.level.waypoint.WaypointsTab
import de.egril.defender.ui.icon.*
import defender_of_egril.composeapp.generated.resources.*

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
    val state = remember { LevelEditorViewState(level) }
    with(state) {
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
            singleHitTargetOrder = level.singleHitTargetOrder
        }
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
                    tabIndices.placementArea,
                    tabIndices.supports,
                    tabIndices.events,
                )
            if (selectedTabIndex !in visibleTabIndices) {
                selectedTabIndex = tabIndices.levelInfo
            }
        }
        val coroutineScope = rememberCoroutineScope()

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

        val spawnTimeline = EditorSpawnTimeline(enemySpawns, maxTurnNumber, spawnLoops)

        // Villains are unique enemy heroes: only one of each type may be placed in a level.
        val presentVillainTypes = enemySpawns.filter { it.attackerType.isRealVillain }.map { it.attackerType }.toSet()

        // Check if any enemies are spawned outside valid spawn points
        val mapSpawnPoints = remember(currentMap) { currentMap?.getSpawnPoints()?.toSet() ?: emptySet() }
        val configuredSpawnEntries =
            remember(enemySpawns) { enemySpawns.toList() }
        val hasEnemiesOutsideSpawnPoints =
            remember(enemySpawns, mapSpawnPoints) {
                configuredSpawnEntries.any { spawn ->
                    spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
                }
            }

        // Check readiness for each tab
        val coinsInt = startCoins.toIntOrNull() ?: 0
        val hpInt = startHP.toIntOrNull() ?: 0
        val isLevelInfoReady = coinsInt > 0 && hpInt > 0
        val stoppedSpawnLoopIds = remember(eventsState) { eventsState.stoppedSpawnLoopIds() }
        val spawnIssues =
            remember(enemySpawns, maxTurnNumber, spawnLoops, currentMap, stoppedSpawnLoopIds) {
                spawnTimelineIssues(spawnTimeline, currentMap, stoppedSpawnLoopIds)
            }
        // Infinite spawn loops must be ended by a "Stop spawn loop" event action (shown in both tabs).
        val unstoppedInfiniteLoopIds =
            if (isSandbox) {
                emptyList()
            } else {
                spawnLoops.filter { it.repeatMode == SpawnRepeatMode.INFINITE && it.id !in stoppedSpawnLoopIds }.map { it.id }
            }
        val invalidSpawnLoopStops = if (isSandbox) 0 else invalidSpawnLoopStopCount(eventsState, spawnLoops.map { it.id }.toSet())
        val spawnLoopEventIssues =
            listOfNotNull(
                unstoppedInfiniteLoopIds.takeIf { it.isNotEmpty() }?.let {
                    stringResource(Res.string.events_infinite_loops_not_stopped, it.joinToString(", "))
                },
                invalidSpawnLoopStops.takeIf { it > 0 }?.let { stringResource(Res.string.events_invalid_spawn_loop_stops, it) },
            )
        val spawnPlanValid = isSandbox || spawnIssues.isEmpty()
        val isEnemySpawnsReady = isSandbox || spawnPlanValid && enemySpawns.isNotEmpty()
        val hasInitialSetupTowerSupport = supportsState.isNotEmpty() || initialDataState.defenders.isNotEmpty()
        val hasInitialTowerBases = initialDataState.barricades.any { it.supportsTower }
        val mapNeedsNoBuildFallback = currentMap?.allowNoBuildableTiles == true && !currentMap.hasBuildablePlacementTiles()
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
                maxTurnNumber,
                spawnLoops,
                availableTowersState,
                waypointsState,
                singleHitTargetOrder,
                testingOnly,
                allowAutoAttack,
                connectedToPreviousLevel,
                autoTowerBase,
                autoTowerBaseExtraCost,
                isSandbox,
                supportsState,
                eventsState,
                initialDataState,
            ) {
                level
                    .copy(
                        title = title,
                        subtitle = subtitle,
                        author = author,
                        communityDescription = communityDescription,
                        mapId = selectedMapId,
                        startCoins = startCoins.toIntOrNull() ?: 100,
                        startHealthPoints = startHP.toIntOrNull() ?: 10,
                        enemySpawns = emptyList(),
                        spawnGroups = null,
                        availableTowers = availableTowersState,
                        waypoints = waypointsState.toList(),
                        singleHitTargetOrder = singleHitTargetOrder,
                        testingOnly = testingOnly,
                        allowAutoAttack = allowAutoAttack,
                        connectedToPreviousLevel = connectedToPreviousLevel,
                        autoTowerBase = autoTowerBase,
                        autoTowerBaseExtraCost =
                            autoTowerBaseExtraCost.toIntOrNull() ?: DEFAULT_AUTO_TOWER_BASE_EXTRA_COST,
                        isSandbox = isSandbox,
                        waaghEnabled = waaghEnabled,
                        supports = supportsState,
                        events = eventsState,
                        initialData = initialDataState,
                    ).let { if (isSandbox) it else it.withSpawnTimeline(spawnTimeline) }
            }
        val levelDesignSummary = remember(draftLevel, currentMap) { analyzeLevelDesign(draftLevel, currentMap) }
        val waveArrivals = remember(draftLevel, currentMap) { buildWaveArrivalBuckets(draftLevel, currentMap) }
        val levelConsistencySummary = remember(draftLevel, currentMap) { analyzeLevelMapConsistency(draftLevel, currentMap) }
        val initialSetupIssueCount = levelConsistencySummary.invalidInitialPlacementCount
        val eventIssueCount = levelConsistencySummary.invalidEventPositionCount
        val hasPortalPathIssue =
            remember(draftLevel, currentMap) {
                currentMap?.allowNoDirectPath == true &&
                    !currentMap.validateReadyToUseWithPortals(
                        portals = draftLevel.getEffectiveInitialData().portals,
                        includeRiversAsWalkable = true,
                    )
            }

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
                                    RedDotBadge(spawnIssueTexts(spawnIssues), testTag = "enemy_spawns_tab_issue_dot")
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
                            if (initialSetupIssueCount > 0 || hasPortalPathIssue) {
                                RedDotBadge()
                            }
                        }
                    },
                )
                Tab(
                    selected = selectedTabIndex == tabIndices.placementArea,
                    onClick = { selectedTabIndex = tabIndices.placementArea },
                    text = { Text(stringResource(Res.string.initial_setup_placement_area)) },
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
                                if (eventIssueCount > 0 || spawnLoopEventIssues.isNotEmpty()) {
                                    RedDotBadge(
                                        listOfNotNull(
                                            eventIssueCount.takeIf { it > 0 }?.let {
                                                stringResource(Res.string.events_invalid_positions_explainer, it)
                                            },
                                        ) + spawnLoopEventIssues,
                                        testTag = "events_tab_issue_dot",
                                    )
                                }
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
                            onMapChange = {
                                if (selectedMapId != it) {
                                    selectedMapId = it
                                    singleHitTargetOrder = emptyList()
                                }
                            },
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
                            autoTowerBase = autoTowerBase,
                            onAutoTowerBaseChange = { autoTowerBase = it },
                            autoTowerBaseExtraCost = autoTowerBaseExtraCost,
                            onAutoTowerBaseExtraCostChange = { value ->
                                autoTowerBaseExtraCost = value.filter { it.isDigit() }
                            },
                            isSandbox = isSandbox,
                            onIsSandboxChange = { isSandbox = it },
                            waaghEnabled = waaghEnabled,
                            onWaaghEnabledChange = { waaghEnabled = it },
                            isOfficial = level.isOfficial,
                            canEnableConnectedToPreviousLevel = hasOtherLevelsOnSameMap,
                        )
                    tabIndices.designPreview ->
                        Column(Modifier.fillMaxSize()) {
                            if (spawnLoops.isNotEmpty()) {
                                SelectableText(stringResource(Res.string.spawn_preview_help))
                            }
                            LevelDesignOverview(
                                summary = levelDesignSummary,
                                arrivals = waveArrivals,
                                consistency = levelConsistencySummary,
                                onApplyTemplate = { template ->
                                    val templated = applyLevelTemplate(draftLevel, currentMap, template)
                                    startCoins = templated.startCoins.toString()
                                    startHP = templated.startHealthPoints.toString()
                                    // A level template creates a new linear plan, so existing loops are removed.
                                    updateEnemySpawnState(
                                        newEnemySpawns = templated.enemySpawns.toMutableList(),
                                        newMaxTurnNumber = templated.enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0,
                                        newSpawnLoops = emptyList(),
                                    )
                                    availableTowersState = templated.availableTowers
                                },
                                onStartPlaytest = { type ->
                                    onSave(draftLevel)
                                    onStartPlaytest(createFocusedPlaytestLevel(draftLevel, levelDesignSummary, type), type)
                                },
                                playtestEnabled = currentMap != null && spawnPlanValid && (isSandbox || draftLevel.hasConfiguredSpawns()),
                                onOpenEnemySpawnTurn = { turn ->
                                    requestedEnemySpawnTurn = turn
                                    enemySpawnTurnRequestNonce++
                                    tabIndices.enemySpawns?.let { selectedTabIndex = it }
                                },
                            )
                        }
                    tabIndices.enemySpawns ->
                        EnemySpawnsTab(
                            timeline = spawnTimeline,
                            issues = spawnIssues,
                            onTimelineChange = { updateSpawnTimeline(it) },
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
                            onUndo = { undoEnemySpawns() },
                            onRedo = { redoEnemySpawns() },
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
                            singleHitTargetOrder = singleHitTargetOrder,
                            onSingleHitTargetOrderChange = { singleHitTargetOrder = it },
                            map = currentMap,
                            isValid = isWaypointsValid,
                        )
                    tabIndices.initialSetup ->
                        de.egril.defender.ui.editor.level.initialsetup.InitialSetupTab(
                            initialData = initialDataState,
                            onInitialDataChange = { initialDataState = it },
                            map = currentMap,
                            availableTowers = availableTowersState,
                            issueDescription =
                                when {
                                    initialSetupIssueCount > 0 ->
                                        stringResource(Res.string.initial_setup_invalid_positions_explainer, initialSetupIssueCount)
                                    hasPortalPathIssue ->
                                        stringResource(Res.string.initial_setup_portal_validation_failed)
                                    else -> null
                                },
                        )
                    tabIndices.placementArea ->
                        de.egril.defender.ui.editor.level.placement.PlacementAreaTab(
                            initialData = initialDataState,
                            onInitialDataChange = { initialDataState = it },
                            map = currentMap,
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
                            issueDescription =
                                if (eventIssueCount > 0) {
                                    stringResource(Res.string.events_invalid_positions_explainer, eventIssueCount)
                                } else {
                                    null
                                },
                            tileZones = currentMap?.tileZones ?: emptyList(),
                            spawnLoops = spawnLoops.map { it.id to "${it.id} (${loopSummary(it)})" },
                            issues = spawnLoopEventIssues,
                        )
                }
            }

            LevelEditorViewActions(
                state = state,
                level = level,
                draftLevel = draftLevel,
                spawnTimeline = spawnTimeline,
                spawnPlanValid = spawnPlanValid,
                coroutineScope = coroutineScope,
                onSave = onSave,
                onCancel = onCancel,
            )
        }

        LevelEditorViewDialogs(
            state = state,
            level = level,
            currentMap = currentMap,
            presentVillainTypes = presentVillainTypes,
            spawnTimeline = spawnTimeline,
            onSave = onSave,
        )
    }
}
