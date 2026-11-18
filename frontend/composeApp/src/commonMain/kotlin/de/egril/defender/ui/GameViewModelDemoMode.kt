package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.GameEngine
import de.egril.defender.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------------------
// Demo Mode
// -------------------------------------------------------------------------

fun GameViewModel.startDemoMode(scenario: de.egril.defender.game.DemoMode.Scenario = de.egril.defender.game.DemoMode.Scenario.STANDARD) {
    _pendingDemoDeepLink.value = false
    _isDemoMode.value = true
    editorPlaytestSession = null
    demoScenario = scenario
    demoLevelIndex = 0
    loadDemoLevel(0)
}

fun GameViewModel.stopDemoMode() {
    demoJob?.cancel()
    demoJob = null
    _isDemoMode.value = false
    // Clear any pending visual preview state
    _demoSelectedDefenderType.value = null
    _demoHoveredPosition.value = null
    _demoSelectedDefenderId.value = null
    _demoSelectedTargetPosition.value = null
    navigateToWorldMap()
}

internal fun GameViewModel.loadDemoLevel(index: Int) {
    val demoSetup =
        de.egril.defender.game.DemoMode
            .createDemoLevelSetup(index, demoScenario) ?: return
    demoInitialTowers = demoSetup.initialTowers
    val demoLevel = demoSetup.level

    val newGameState =
        GameState(
            level = demoLevel,
            coins = mutableStateOf(demoLevel.initialCoins),
            healthPoints = mutableStateOf(demoLevel.healthPoints),
            spawnPlan = demoLevel.directSpawnPlan ?: emptyList(),
        )
    // Defenders are placed dynamically by startDemoAutoPlay(), but other initial elements like
    // tower-base barricades still have to be initialized from the level data.
    newGameState.initializePrePlacedElements()

    _gameState.value = newGameState
    gameEngine = GameEngine(newGameState)
    _currentScreen.value = Screen.GamePlay(demoLevel.id)

    // Dismiss any info dialogs (e.g. river info on the creek level) after a short delay
    viewModelScope.launch {
        delay(de.egril.defender.game.DemoMode.INFO_DISMISS_DELAY_MS)
        if (_isDemoMode.value) {
            _gameState.value?.let { state ->
                if (state.infoState.value.currentInfo != de.egril.defender.model.InfoType.NONE) {
                    state.infoState.value = state.infoState.value.dismissInfo()
                }
            }
        }
    }

    startDemoAutoPlay()
}

/**
 * Place a tower with a visual preview: shows the placement highlight for
 * [DemoMode.TOWER_PLACE_DELAY_MS] ms, then places the tower, then waits
 * [DemoMode.TOWER_AFTER_PLACE_MS] ms before continuing.
 */
private suspend fun GameViewModel.demoPlaceTowerWithPreview(
    type: DefenderType,
    position: Position,
) {
    _demoSelectedDefenderType.value = type
    _demoHoveredPosition.value = position
    delay(de.egril.defender.game.DemoMode.TOWER_PLACE_DELAY_MS)
    _demoSelectedDefenderType.value = null
    _demoHoveredPosition.value = null
    gameEngine?.placeDefender(type, position)
    delay(de.egril.defender.game.DemoMode.TOWER_AFTER_PLACE_MS)
}

/**
 * Perform all remaining auto-attacks for the current player turn, one attack at a time.
 * Before each attack the aiming circles are shown for [DemoMode.ATTACK_AIM_MS] ms,
 * and after each attack there is a [DemoMode.ATTACK_AFTER_MS] ms pause.
 * Reuses the same target-selection logic as [GameEngine.autoDefenderAttacks].
 *
 * The selected defender is kept highlighted across all its attacks (no flicker between
 * attacks of the same tower) and switches directly to the next defender without going
 * through an unselected state. Selection is only cleared after all attacks are done.
 *
 * If the level is won or lost mid-attack, [completeLevel] is called immediately so the
 * demo advances to the level-complete screen without waiting for the player to end the turn.
 */
private suspend fun GameViewModel.demoAttackOneByOne() {
    val engine = gameEngine ?: return
    val defenderIds =
        _gameState.value
            ?.defenders
            ?.filter { it.isReady && !it.isDisabled.value && it.type.attackType != AttackType.NONE }
            ?.map { it.id }
            ?: return

    try {
        for (id in defenderIds) {
            if (!_isDemoMode.value || _gameState.value?.phase?.value != GamePhase.PLAYER_TURN) return
            // Select this defender now — switching directly from the previous one (no unselected gap)
            _demoSelectedDefenderId.value = id
            // Keep attacking while this defender still has action points
            while (_isDemoMode.value && _gameState.value?.phase?.value == GamePhase.PLAYER_TURN) {
                val defender = _gameState.value?.defenders?.find { it.id == id } ?: break
                if (defender.actionsRemaining.value <= 0) break
                val targetPos = engine.getNextAutoAttackTargetPosition(defender) ?: break

                // Show aiming circles (defender already selected above)
                _demoSelectedTargetPosition.value = targetPos
                delay(de.egril.defender.game.DemoMode.ATTACK_AIM_MS)

                // Clear only the target position before executing the attack;
                // keep the defender highlighted so the UI stays in tower-selected view
                _demoSelectedTargetPosition.value = null

                val success = engine.performOneAutoAttack(id)
                if (!success) break

                // Check for immediate level end (last enemy killed or villain breached) — trigger completeLevel
                // right now instead of waiting for endPlayerTurn so the demo advances immediately.
                // Check loss first: a villain breaching a target loses immediately, even if all enemies are defeated
                val stateAfterAttack = _gameState.value
                if (stateAfterAttack != null) {
                    if (stateAfterAttack.isLevelLost()) {
                        completeLevel(stateAfterAttack.level.id, won = false)
                        return
                    } else if (stateAfterAttack.isLevelWon()) {
                        completeLevel(stateAfterAttack.level.id, won = true)
                        return
                    }
                }

                delay(de.egril.defender.game.DemoMode.ATTACK_AFTER_MS)
            }
            // Loop will directly set the next defender id without an unselected frame
        }
    } finally {
        // Clear selection only once, after all attacks are complete
        _demoSelectedDefenderId.value = null
        _demoSelectedTargetPosition.value = null
    }
}

private fun GameViewModel.startDemoAutoPlay() {
    demoJob?.cancel()
    demoJob =
        viewModelScope.launch {
            // Phase 1: place initial towers one by one with preview + delays in the INITIAL_BUILDING phase
            delay(de.egril.defender.game.DemoMode.INITIAL_BUILDING_DELAY_MS)
            for (tower in demoInitialTowers) {
                if (!_isDemoMode.value || _gameState.value?.phase?.value != GamePhase.INITIAL_BUILDING) break
                demoPlaceTowerWithPreview(tower.type, tower.position)
            }
            // Brief pause after last tower is placed so the player can see all towers before battle starts
            delay(de.egril.defender.game.DemoMode.INITIAL_BUILDING_DELAY_MS)
            if (_isDemoMode.value && _gameState.value?.phase?.value == GamePhase.INITIAL_BUILDING) {
                startFirstPlayerTurn()
            }

            // Phase 2: auto-play turns
            while (_isDemoMode.value) {
                when (_gameState.value?.phase?.value) {
                    GamePhase.PLAYER_TURN -> {
                        delay(de.egril.defender.game.DemoMode.PLAYER_TURN_DELAY_MS)
                        val currentState = _gameState.value ?: break
                        if (!_isDemoMode.value || currentState.phase.value != GamePhase.PLAYER_TURN) continue
                        if (currentState.isLevelWon() || currentState.isLevelLost()) {
                            delay(de.egril.defender.game.DemoMode.ENEMY_TURN_POLL_MS)
                            continue
                        }

                        // Try to place a new tower if coins allow and a free build area or tower base exists
                        val placementTargets = getDemoPlacementTargets(currentState)
                        for (type in currentState.level.availableTowers.sortedByDescending { it.baseCost }) {
                            if (currentState.canAffordTowerButton(type)) {
                                val targetPos = findDemoPlacementPosition(currentState, type, placementTargets) ?: continue
                                // Show preview, then place
                                _demoSelectedDefenderType.value = type
                                _demoHoveredPosition.value = targetPos
                                delay(de.egril.defender.game.DemoMode.TOWER_PLACE_DELAY_MS)
                                _demoSelectedDefenderType.value = null
                                _demoHoveredPosition.value = null
                                val stateNow = _gameState.value
                                // Re-check that position is still free after the delay
                                val stillFree = stateNow?.defenders?.none { it.position.value == targetPos } == true
                                if (_isDemoMode.value && stateNow?.phase?.value == GamePhase.PLAYER_TURN && stillFree) {
                                    gameEngine?.placeDefender(type, targetPos)
                                    delay(de.egril.defender.game.DemoMode.TOWER_AFTER_PLACE_MS)
                                }
                                break
                            }
                        }

                        // Try to upgrade the cheapest upgradeable tower
                        val stateForUpgrade = _gameState.value
                        if (stateForUpgrade != null &&
                            _isDemoMode.value &&
                            stateForUpgrade.phase.value == GamePhase.PLAYER_TURN
                        ) {
                            val upgradeCandidate =
                                stateForUpgrade.defenders
                                    .filter { stateForUpgrade.canUpgradeDefender(it) }
                                    .minByOrNull { it.upgradeCost }
                            if (upgradeCandidate != null) {
                                delay(de.egril.defender.game.DemoMode.TOWER_PLACE_DELAY_MS)
                                val stateNow = _gameState.value
                                if (_isDemoMode.value && stateNow?.phase?.value == GamePhase.PLAYER_TURN) {
                                    gameEngine?.upgradeDefender(upgradeCandidate.id)
                                }
                            }
                        }

                        // Attack one by one with aiming circles, then end turn
                        val finalState = _gameState.value ?: break
                        if (!_isDemoMode.value || finalState.phase.value != GamePhase.PLAYER_TURN) continue
                        if (finalState.isLevelWon() || finalState.isLevelLost()) continue
                        demoAttackOneByOne()
                        if (_isDemoMode.value &&
                            _gameState.value?.phase?.value == GamePhase.PLAYER_TURN &&
                            !(_gameState.value?.isLevelWon() ?: false) &&
                            !(_gameState.value?.isLevelLost() ?: false)
                        ) {
                            endPlayerTurn()
                        }
                    }
                    GamePhase.ENEMY_TURN,
                    GamePhase.INITIAL_BUILDING,
                    -> delay(de.egril.defender.game.DemoMode.ENEMY_TURN_POLL_MS)
                    null -> break
                }
            }
        }
}

private data class DemoPlacementTargets(
    val buildAreas: List<Position>,
    val towerBases: List<Position>,
)

private fun GameViewModel.getDemoPlacementTargets(state: GameState): DemoPlacementTargets {
    val occupiedPositions = state.defenders.map { it.position.value }.toSet()
    val barricadePositions = state.barricades.map { it.position }.toSet()
    val freeBuildAreas = state.level.buildAreas.filter { it !in occupiedPositions && it !in barricadePositions }
    val freeTowerBases =
        state.barricades
            .filter { it.canSupportTower() && !it.hasTower() }
            .map { it.position }
    return DemoPlacementTargets(
        buildAreas = freeBuildAreas,
        towerBases = freeTowerBases,
    )
}

/**
 * Buildable targets first; PATH tiles only as a fallback when the auto tower base option
 * is enabled. The engine check applies the surcharge for PATH tiles.
 */
private fun GameViewModel.findDemoPlacementPosition(
    state: GameState,
    type: DefenderType,
    placementTargets: DemoPlacementTargets,
): Position? {
    val candidatePositions =
        if (type == DefenderType.DWARVEN_MINE) {
            placementTargets.buildAreas
        } else {
            placementTargets.towerBases + placementTargets.buildAreas
        }
    val buildable =
        candidatePositions.firstOrNull { position ->
            gameEngine?.canPlaceDefenderAt(type, position) == true
        }
    return buildable ?: state.keyboardPlacementCandidates(type).firstOrNull { position ->
        gameEngine?.canPlaceDefenderAt(type, position) == true
    }
}
