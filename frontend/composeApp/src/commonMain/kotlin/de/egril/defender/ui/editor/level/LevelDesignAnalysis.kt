package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorEnemyTemplateKind
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.SpawnTurnTemplateDefinition
import de.egril.defender.editor.SpawnTurnTemplateEntry
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.isRealVillain
import kotlin.math.ceil

internal enum class FocusedPlaytestType {
    FULL,
    FIRST_VILLAIN,
    PEAK_PRESSURE,
    CLIMAX,
}

internal enum class EditorLevelTemplate {
    TUTORIAL,
    STEADY_PRESSURE,
    VILLAIN_DUEL,
    RIVER_PRESSURE,
    ENDURANCE,
}

internal enum class TimingBand {
    NONE,
    EARLY,
    MID,
    LATE,
}

internal enum class PacingBand {
    CALM,
    STEADY,
    SPIKY,
}

internal enum class EconomyBand {
    HARSH,
    TIGHT,
    GOOD,
}

internal enum class MapLaneShape {
    STRAIGHT,
    BRANCHING,
}

internal enum class DensityBand {
    SPARSE,
    GOOD,
    DENSE,
}

internal enum class TravelBand {
    SHORT,
    GOOD,
    LONG,
}

internal data class TurnPressurePreview(
    val turn: Int,
    val enemyCount: Int,
    val totalHealth: Int,
    val totalReward: Int,
    val pressureScore: Double,
    val earliestArrivalTurn: Int?,
    val latestArrivalTurn: Int?,
    val villainNames: List<String>,
)

internal data class LevelDesignSummary(
    val turnPreviews: List<TurnPressurePreview>,
    val totalHealth: Int,
    val totalReward: Int,
    val totalTargetDamage: Int,
    val firstVillainTurn: Int?,
    val peakPressureTurn: Int?,
    val peakPressureScore: Double,
    val longestCalmGap: Int,
    val economyBand: EconomyBand,
    val pacingBand: PacingBand,
    val villainTimingBand: TimingBand,
    val missingCounters: List<DefenderType>,
    val arrivalOverlapTurns: List<Int>,
    val quietTurns: List<Int>,
    val peakArrivalTurn: Int?,
    val peakArrivalCount: Int,
)

internal data class MapFlowSummary(
    val isReady: Boolean,
    val laneShape: MapLaneShape,
    val buildCoverage: DensityBand,
    val travelLength: TravelBand,
    val longestDeadCorridor: Int,
    val spawnCount: Int,
    val targetCount: Int,
)

internal data class WaveArrivalBucket(
    val turn: Int,
    val enemyCount: Int,
    val spawnTurns: List<Int>,
    val villainNames: List<String>,
)

internal data class LevelConsistencySummary(
    val invalidSpawnAssignments: List<EditorEnemySpawn>,
    val missingCompatibleSpawnTypes: List<AttackerType>,
    val invalidWaypoints: List<EditorWaypoint>,
    val invalidInitialPlacementCount: Int,
    val invalidEventPositionCount: Int,
) {
    val issueCount: Int
        get() =
            invalidSpawnAssignments.size +
                missingCompatibleSpawnTypes.size +
                invalidWaypoints.size +
                invalidInitialPlacementCount +
                invalidEventPositionCount

    val hasIssues: Boolean
        get() = issueCount > 0
}

private data class RouteContext(
    val distances: Map<Position, Int>,
    val traversableCells: Set<Position>,
)

internal fun analyzeLevelDesign(
    level: EditorLevel,
    map: EditorMap?,
): LevelDesignSummary {
    val maxTurn = level.enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0
    if (maxTurn == 0) {
        return LevelDesignSummary(
            turnPreviews = emptyList(),
            totalHealth = 0,
            totalReward = 0,
            totalTargetDamage = 0,
            firstVillainTurn = null,
            peakPressureTurn = null,
            peakPressureScore = 0.0,
            longestCalmGap = 0,
            economyBand = EconomyBand.HARSH,
            pacingBand = PacingBand.CALM,
            villainTimingBand = TimingBand.NONE,
            missingCounters = emptyList(),
            arrivalOverlapTurns = emptyList(),
            quietTurns = emptyList(),
            peakArrivalTurn = null,
            peakArrivalCount = 0,
        )
    }

    val routeContext = map?.buildRouteContext(level.waypoints)
    val arrivalBuckets =
        level.enemySpawns
            .mapNotNull { spawn ->
                estimateArrivalTurn(spawn, map, level.waypoints, routeContext)?.let { arrivalTurn ->
                    arrivalTurn to spawn
                }
            }.groupBy({ it.first }, { it.second })
    val previews =
        (1..maxTurn).map { turn ->
            val spawns = level.enemySpawns.filter { it.spawnTurn == turn }
            val arrivals = spawns.mapNotNull { spawn -> estimateArrivalTurn(spawn, map, level.waypoints, routeContext) }
            TurnPressurePreview(
                turn = turn,
                enemyCount = spawns.size,
                totalHealth = spawns.sumOf { it.healthPoints },
                totalReward = spawns.sumOf { it.attackerType.reward * it.level },
                pressureScore = spawns.sumOf { spawnPressureScore(it) },
                earliestArrivalTurn = arrivals.minOrNull(),
                latestArrivalTurn = arrivals.maxOrNull(),
                villainNames = spawns.mapNotNull { it.attackerType.villainName }.distinct(),
            )
        }

    val peakPreview = previews.maxByOrNull { it.pressureScore }
    val nonEmptyTurns = previews.filter { it.enemyCount > 0 }
    val longestCalmGap = calmGapBetweenActiveTurns(nonEmptyTurns.map { it.turn })
    val firstVillainTurn = nonEmptyTurns.firstOrNull { it.villainNames.isNotEmpty() }?.turn
    val latestArrivalTurn = arrivalBuckets.keys.maxOrNull() ?: 0
    val arrivalOverlapTurns = arrivalBuckets.filterValues { it.size > 1 }.keys.sorted()
    val peakArrivalEntry = arrivalBuckets.maxByOrNull { it.value.size }
    val quietTurns =
        (1..maxOf(maxTurn, latestArrivalTurn))
            .filter { turn ->
                previews.none { it.turn == turn && it.enemyCount > 0 } && arrivalBuckets[turn].isNullOrEmpty()
            }
    val economyBand =
        when {
            level.startCoins < 60 -> EconomyBand.HARSH
            level.startCoins < 100 -> EconomyBand.TIGHT
            else -> EconomyBand.GOOD
        }
    val averagePressure = nonEmptyTurns.map { it.pressureScore }.average().takeIf { !it.isNaN() } ?: 0.0
    val pacingBand =
        when {
            peakPreview == null || averagePressure == 0.0 -> PacingBand.CALM
            peakPreview.pressureScore >= averagePressure * 2.1 -> PacingBand.SPIKY
            else -> PacingBand.STEADY
        }
    val villainTimingBand =
        when {
            firstVillainTurn == null -> TimingBand.NONE
            firstVillainTurn <= 5 -> TimingBand.EARLY
            firstVillainTurn >= ((maxTurn * 2) / 3).coerceAtLeast(8) -> TimingBand.LATE
            else -> TimingBand.MID
        }

    return LevelDesignSummary(
        turnPreviews = previews,
        totalHealth = level.enemySpawns.sumOf { it.healthPoints },
        totalReward = level.enemySpawns.sumOf { it.attackerType.reward * it.level },
        totalTargetDamage = level.enemySpawns.sumOf { it.attackerType.calculateTargetDamage(it.level) },
        firstVillainTurn = firstVillainTurn,
        peakPressureTurn = peakPreview?.turn,
        peakPressureScore = peakPreview?.pressureScore ?: 0.0,
        longestCalmGap = longestCalmGap,
        economyBand = economyBand,
        pacingBand = pacingBand,
        villainTimingBand = villainTimingBand,
        missingCounters = missingCounterTowers(level),
        arrivalOverlapTurns = arrivalOverlapTurns,
        quietTurns = quietTurns,
        peakArrivalTurn = peakArrivalEntry?.key,
        peakArrivalCount = peakArrivalEntry?.value?.size ?: 0,
    )
}

internal fun analyzeMapFlow(map: EditorMap): MapFlowSummary {
    val buildAreas = map.getBuildAreas()
    val pathCells = map.getPathCells()
    val traversable = pathCells + map.getSpawnPoints() + map.getTargets() + map.getRiverCells()
    val branchCount = traversable.count { cell -> cell.getHexNeighbors().count { it in traversable } >= 3 }
    val adjacentBuildCounts =
        traversable.map { cell ->
            cell.getHexNeighbors().count { it in buildAreas }
        }
    val averageAdjacentBuilds = adjacentBuildCounts.average().takeIf { !it.isNaN() } ?: 0.0
    val longestDeadCorridor = longestDeadCorridor(traversable, buildAreas)
    val longestTravelDistance =
        map.getSpawnPoints().maxOfOrNull { spawn ->
            shortestDistanceToAnyTarget(spawn, traversable, map.getTargets(), map.width, map.height)
        } ?: 0

    return MapFlowSummary(
        isReady = map.validateReadyToUse(),
        laneShape = if (branchCount > 0) MapLaneShape.BRANCHING else MapLaneShape.STRAIGHT,
        buildCoverage =
            when {
                averageAdjacentBuilds < 0.8 -> DensityBand.SPARSE
                averageAdjacentBuilds > 2.4 -> DensityBand.DENSE
                else -> DensityBand.GOOD
            },
        travelLength =
            when {
                longestTravelDistance < 10 -> TravelBand.SHORT
                longestTravelDistance > 30 -> TravelBand.LONG
                else -> TravelBand.GOOD
            },
        longestDeadCorridor = longestDeadCorridor,
        spawnCount = map.getSpawnPoints().size,
        targetCount = map.getTargets().size,
    )
}

internal fun buildWaveArrivalBuckets(
    level: EditorLevel,
    map: EditorMap?,
): List<WaveArrivalBucket> {
    val routeContext = map?.buildRouteContext(level.waypoints)
    return level.enemySpawns
        .mapNotNull { spawn ->
            estimateArrivalTurn(spawn, map, level.waypoints, routeContext)?.let { arrivalTurn ->
                arrivalTurn to spawn
            }
        }.groupBy({ it.first }, { it.second })
        .entries
        .sortedBy { it.key }
        .map { (turn, spawns) ->
            WaveArrivalBucket(
                turn = turn,
                enemyCount = spawns.size,
                spawnTurns = spawns.map { it.spawnTurn }.distinct().sorted(),
                villainNames = spawns.mapNotNull { it.attackerType.villainName }.distinct(),
            )
        }
}

internal fun analyzeLevelMapConsistency(
    level: EditorLevel,
    map: EditorMap?,
): LevelConsistencySummary {
    if (map == null) {
        return LevelConsistencySummary(
            invalidSpawnAssignments = level.enemySpawns.filter { it.spawnPoint != null },
            missingCompatibleSpawnTypes = level.enemySpawns.map { it.attackerType }.distinct(),
            invalidWaypoints = level.waypoints,
            invalidInitialPlacementCount = countInitialPlacementIssues(level, null),
            invalidEventPositionCount = countInvalidEventPositions(level, null),
        )
    }

    val mapSpawnPoints = map.getSpawnPoints().toSet()
    val waterSpawnPoints = map.getSpawnPoints().filter { map.getSpawnPointType(it) == de.egril.defender.model.SpawnPointType.WATER }
    val traversableCells = map.getPathCells() + map.getSpawnPoints() + map.getTargets() + map.getRiverCells()
    val waypointAnchors = map.getTargets().toSet() + level.waypoints.map { it.position }.toSet()
    val invalidSpawnAssignments =
        level.enemySpawns.filter { spawn ->
            val spawnPoint = spawn.spawnPoint ?: return@filter false
            val compatiblePoints = map.getCompatibleSpawnPoints(spawn.attackerType)
            spawnPoint !in mapSpawnPoints || compatiblePoints.isNotEmpty() && spawnPoint !in compatiblePoints
        }
    val missingCompatibleSpawnTypes =
        level.enemySpawns
            .filter {
                it.spawnPoint == null &&
                    (
                        map.getCompatibleSpawnPoints(it.attackerType).isEmpty() ||
                            (it.attackerType.canTraverseRiver && waterSpawnPoints.isEmpty())
                    )
            }
            .map { it.attackerType }
            .distinct()
    val invalidWaypoints =
        level.waypoints.filter { waypoint ->
            waypoint.position !in traversableCells || waypoint.nextTargetPosition !in waypointAnchors
        }

    return LevelConsistencySummary(
        invalidSpawnAssignments = invalidSpawnAssignments,
        missingCompatibleSpawnTypes = missingCompatibleSpawnTypes,
        invalidWaypoints = invalidWaypoints,
        invalidInitialPlacementCount = countInitialPlacementIssues(level, map),
        invalidEventPositionCount = countInvalidEventPositions(level, map),
    )
}

internal fun applyLevelTemplate(
    level: EditorLevel,
    map: EditorMap?,
    template: EditorLevelTemplate,
): EditorLevel {
    val generatedSpawns =
        when (template) {
            EditorLevelTemplate.TUTORIAL -> generateTutorialSpawns(map)
            EditorLevelTemplate.STEADY_PRESSURE -> generateSteadyPressureSpawns(map)
            EditorLevelTemplate.VILLAIN_DUEL -> generateVillainDuelSpawns(map)
            EditorLevelTemplate.RIVER_PRESSURE -> generateRiverPressureSpawns(map)
            EditorLevelTemplate.ENDURANCE -> generateEnduranceSpawns(map)
        }

    val towers =
        when (template) {
            EditorLevelTemplate.TUTORIAL -> setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER, DefenderType.BOW_TOWER)
            EditorLevelTemplate.STEADY_PRESSURE -> setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER, DefenderType.BOW_TOWER, DefenderType.ALCHEMY_TOWER)
            EditorLevelTemplate.VILLAIN_DUEL -> setOf(
                DefenderType.SPIKE_TOWER,
                DefenderType.SPEAR_TOWER,
                DefenderType.BOW_TOWER,
                DefenderType.WIZARD_TOWER,
                DefenderType.ALCHEMY_TOWER,
                DefenderType.BALLISTA_TOWER,
            )
            EditorLevelTemplate.RIVER_PRESSURE,
            EditorLevelTemplate.ENDURANCE,
            -> DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR }.toSet()
        }
    val (coins, hp) =
        when (template) {
            EditorLevelTemplate.TUTORIAL -> 120 to 12
            EditorLevelTemplate.STEADY_PRESSURE -> 135 to 12
            EditorLevelTemplate.VILLAIN_DUEL -> 180 to 14
            EditorLevelTemplate.RIVER_PRESSURE -> 150 to 13
            EditorLevelTemplate.ENDURANCE -> 200 to 16
        }
    return level.copy(
        startCoins = coins,
        startHealthPoints = hp,
        enemySpawns = generatedSpawns,
        availableTowers = towers,
    )
}

internal fun applySpawnTurnTemplate(
    enemySpawns: List<EditorEnemySpawn>,
    maxTurnNumber: Int,
    map: EditorMap?,
    template: SpawnTurnTemplateDefinition,
    enemyKind: EditorEnemyTemplateKind,
    baseLevel: Int,
): Pair<MutableList<EditorEnemySpawn>, Int> {
    val startTurn = maxTurnNumber + 1
    val variant = template.variantFor(enemyKind) ?: return enemySpawns.toMutableList() to maxTurnNumber
    val additions =
        buildTemplateSpawns(
            map = map,
            entries = variant.entries,
            startTurn = startTurn,
            baseLevel = baseLevel,
        )
    return (enemySpawns + additions).sortedBy { it.spawnTurn }.toMutableList() to (additions.maxOfOrNull { it.spawnTurn } ?: maxTurnNumber)
}

internal fun createFocusedPlaytestLevel(
    level: EditorLevel,
    summary: LevelDesignSummary,
    type: FocusedPlaytestType,
): EditorLevel {
    if (type == FocusedPlaytestType.FULL || level.enemySpawns.isEmpty()) {
        return level
    }
    val maxTurn = level.enemySpawns.maxOf { it.spawnTurn }
    val focusTurn =
        when (type) {
            FocusedPlaytestType.FIRST_VILLAIN -> summary.firstVillainTurn ?: summary.peakPressureTurn ?: 1
            FocusedPlaytestType.PEAK_PRESSURE -> summary.peakPressureTurn ?: 1
            FocusedPlaytestType.CLIMAX -> (maxTurn - 3).coerceAtLeast(1)
            FocusedPlaytestType.FULL -> 1
        }
    val windowStart =
        when (type) {
            FocusedPlaytestType.CLIMAX -> (maxTurn - 5).coerceAtLeast(1)
            else -> (focusTurn - 2).coerceAtLeast(1)
        }
    val windowEnd =
        when (type) {
            FocusedPlaytestType.CLIMAX -> maxTurn
            else -> (focusTurn + 4).coerceAtMost(maxTurn)
        }
    val trimmedSpawns =
        level.enemySpawns
            .filter { it.spawnTurn in windowStart..windowEnd }
            .map { it.copy(spawnTurn = it.spawnTurn - windowStart + 1) }
    val removedReward =
        level.enemySpawns
            .filter { it.spawnTurn < windowStart }
            .sumOf { it.attackerType.reward * it.level }
    val bonusCoins = (removedReward / 2).coerceAtLeast(0)

    return level.copy(
        title = "${level.title} (${type.name.lowercase().replace('_', ' ')})",
        enemySpawns = trimmedSpawns,
        startCoins = level.startCoins + bonusCoins,
    )
}

private fun buildTemplateSpawns(
    map: EditorMap?,
    entries: List<SpawnTurnTemplateEntry>,
    startTurn: Int,
    baseLevel: Int,
): List<EditorEnemySpawn> {
    val output = mutableListOf<EditorEnemySpawn>()
    entries.forEach { entry ->
        val compatiblePoints = map?.getCompatibleSpawnPoints(entry.attackerType).orEmpty()
        repeat(entry.amount) { index ->
            val spawnPoint =
                if (compatiblePoints.isEmpty()) {
                    null
                } else {
                    compatiblePoints[index % compatiblePoints.size]
                }
            output +=
                EditorEnemySpawn(
                    attackerType = entry.attackerType,
                    level = (baseLevel + entry.levelOffset).coerceAtLeast(1),
                    spawnTurn = startTurn + entry.turnOffset,
                    spawnPoint = spawnPoint,
                )
        }
    }
    return output
}

private fun generateTutorialSpawns(map: EditorMap?): List<EditorEnemySpawn> =
    buildTemplateSpawns(
        map = map,
        startTurn = 0,
        baseLevel = 1,
        entries = listOf(
            SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 2, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 4, amount = 3),
            SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 6, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.SKELETON, turnOffset = 8, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 10, amount = 2),
        ),
    )

private fun generateSteadyPressureSpawns(map: EditorMap?): List<EditorEnemySpawn> =
    buildTemplateSpawns(
        map = map,
        startTurn = 0,
        baseLevel = 1,
        entries = listOf(
            SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 2, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 3, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 5, amount = 3),
            SpawnTurnTemplateEntry(AttackerType.SKELETON, turnOffset = 6, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.RED_WITCH, turnOffset = 8, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.OGRE, turnOffset = 9, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.GREEN_WITCH, turnOffset = 11, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 12, amount = 2),
        ),
    )

private fun generateVillainDuelSpawns(map: EditorMap?): List<EditorEnemySpawn> =
    buildTemplateSpawns(
        map = map,
        startTurn = 0,
        baseLevel = 1,
        entries = listOf(
            SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 2, amount = 3),
            SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 4, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.GREEN_WITCH, turnOffset = 6, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.RED_WITCH, turnOffset = 7, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.GAROKK, turnOffset = 9, amount = 1, levelOffset = 1),
            SpawnTurnTemplateEntry(AttackerType.OGRE, turnOffset = 9, amount = 1, levelOffset = 1),
            SpawnTurnTemplateEntry(AttackerType.SKELETON, turnOffset = 11, amount = 3),
        ),
    )

private fun generateRiverPressureSpawns(map: EditorMap?): List<EditorEnemySpawn> {
    val hasWaterSpawn = map?.getSpawnPoints()?.any { map.getSpawnPointType(it) == de.egril.defender.model.SpawnPointType.WATER } == true
    return if (hasWaterSpawn) {
        buildTemplateSpawns(
            map = map,
            startTurn = 0,
            baseLevel = 1,
            entries = listOf(
                SpawnTurnTemplateEntry(AttackerType.PIRATE, turnOffset = 2, amount = 2),
                SpawnTurnTemplateEntry(AttackerType.BLUE_DEMON, turnOffset = 4, amount = 1),
                SpawnTurnTemplateEntry(AttackerType.PIRATE, turnOffset = 5, amount = 3),
                SpawnTurnTemplateEntry(AttackerType.RED_WITCH, turnOffset = 7, amount = 1),
                SpawnTurnTemplateEntry(AttackerType.CAPTAIN_RODERICH, turnOffset = 10, amount = 1, levelOffset = 1),
            ),
        )
    } else {
        generateSteadyPressureSpawns(map)
    }
}

private fun generateEnduranceSpawns(map: EditorMap?): List<EditorEnemySpawn> =
    buildTemplateSpawns(
        map = map,
        startTurn = 0,
        baseLevel = 1,
        entries = listOf(
            SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 2, amount = 3),
            SpawnTurnTemplateEntry(AttackerType.SKELETON, turnOffset = 4, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 6, amount = 2),
            SpawnTurnTemplateEntry(AttackerType.GREEN_WITCH, turnOffset = 8, amount = 1),
            SpawnTurnTemplateEntry(AttackerType.OGRE, turnOffset = 10, amount = 1, levelOffset = 1),
            SpawnTurnTemplateEntry(AttackerType.RED_DEMON, turnOffset = 12, amount = 1, levelOffset = 1),
            SpawnTurnTemplateEntry(AttackerType.EVIL_WIZARD, turnOffset = 14, amount = 1, levelOffset = 1),
            SpawnTurnTemplateEntry(AttackerType.MORGUK_BONEWHISPER, turnOffset = 17, amount = 1, levelOffset = 1),
        ),
    )

private fun calmGapBetweenActiveTurns(turns: List<Int>): Int {
    if (turns.size < 2) return 0
    return turns.zipWithNext { a, b -> (b - a - 1).coerceAtLeast(0) }.maxOrNull() ?: 0
}

private fun missingCounterTowers(level: EditorLevel): List<DefenderType> {
    val needsFire = level.enemySpawns.any { it.attackerType.immuneToAcid || it.attackerType.immuneToNonMagical || it.attackerType.immuneToNonMagicTowerDamage }
    val needsAcid = level.enemySpawns.any { it.attackerType.immuneToFireball }
    val needsRange = level.enemySpawns.any { it.attackerType.speed >= 5 || it.attackerType.canTraverseRiver }
    return buildList {
        if (needsFire && DefenderType.WIZARD_TOWER !in level.availableTowers) add(DefenderType.WIZARD_TOWER)
        if (needsAcid && DefenderType.ALCHEMY_TOWER !in level.availableTowers) add(DefenderType.ALCHEMY_TOWER)
        if (needsRange && level.availableTowers.none { it == DefenderType.BOW_TOWER || it == DefenderType.BALLISTA_TOWER }) add(DefenderType.BOW_TOWER)
    }
}

private fun spawnPressureScore(spawn: EditorEnemySpawn): Double {
    val type = spawn.attackerType
    val immunities =
        listOf(
            type.immuneToAcid,
            type.immuneToFireball,
            type.immuneToNonMagical,
            type.immuneToNonMagicTowerDamage,
            type.immuneToBladeAttacks,
        ).count { it }
    val utilityBonus =
        when {
            type.canSummon -> 22.0
            type.canHeal || type.canDisableTowers -> 15.0
            else -> 0.0
        }
    val villainBonus = if (type.isRealVillain) 30.0 else 0.0
    return spawn.healthPoints / 10.0 + (type.speed * 2.5) + utilityBonus + villainBonus + (immunities * 5) + spawn.level
}

private fun estimateArrivalTurn(
    spawn: EditorEnemySpawn,
    map: EditorMap?,
    waypoints: List<EditorWaypoint>,
    routeContext: RouteContext?,
): Int? {
    val chosenSpawnPoint =
        spawn.spawnPoint
            ?: map?.getCompatibleSpawnPoints(spawn.attackerType)?.firstOrNull()
            ?: map?.getSpawnPoints()?.firstOrNull()
            ?: return null
    val distance =
        when {
            spawn.attackerType.canFlyOverTerrain -> {
                map?.getTargets()?.minOfOrNull { target -> chosenSpawnPoint.distanceTo(target) } ?: return null
            }
            else -> {
                val distances = routeContext?.distances ?: map.buildRouteContext(waypoints).distances
                distances[chosenSpawnPoint] ?: return null
            }
        }
    val travelTurns =
        when {
            spawn.attackerType.movesEveryOtherTurn -> distance * 2
            spawn.attackerType.speed <= 0 -> distance
            else -> ceil(distance.toDouble() / spawn.attackerType.speed.toDouble()).toInt()
        }.coerceAtLeast(1)
    return spawn.spawnTurn + travelTurns
}

private fun EditorMap?.buildRouteContext(waypoints: List<EditorWaypoint>): RouteContext {
    if (this == null) return RouteContext(emptyMap(), emptySet())
    val traversableCells = getPathCells() + getSpawnPoints() + getTargets() + getRiverCells() + waypoints.map { it.position }
    return RouteContext(
        distances = computeDistancesToTargets(traversableCells, getTargets(), width, height),
        traversableCells = traversableCells,
    )
}

private fun computeDistancesToTargets(
    traversableCells: Set<Position>,
    targets: List<Position>,
    width: Int,
    height: Int,
): Map<Position, Int> {
    val queue = ArrayDeque<Position>()
    val distances = mutableMapOf<Position, Int>()
    targets.forEach { target ->
        if (target.x in 0 until width && target.y in 0 until height) {
            queue.add(target)
            distances[target] = 0
        }
    }
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        val currentDistance = distances.getValue(current)
        current.getHexNeighbors()
            .filter { it.x in 0 until width && it.y in 0 until height && it in traversableCells }
            .forEach { neighbor ->
                if (neighbor !in distances) {
                    distances[neighbor] = currentDistance + 1
                    queue.add(neighbor)
                }
            }
    }
    return distances
}

private fun shortestDistanceToAnyTarget(
    start: Position,
    traversableCells: Set<Position>,
    targets: List<Position>,
    width: Int,
    height: Int,
): Int {
    val distances = computeDistancesToTargets(traversableCells, targets, width, height)
    return distances[start] ?: Int.MAX_VALUE
}

private fun longestDeadCorridor(
    traversableCells: Set<Position>,
    buildAreas: Set<Position>,
): Int {
    var longest = 0
    var current = 0
    traversableCells.sortedWith(compareBy(Position::y, Position::x)).forEach { cell ->
        val adjacentBuilds = cell.getHexNeighbors().count { it in buildAreas }
        if (adjacentBuilds == 0) {
            current += 1
            longest = maxOf(longest, current)
        } else {
            current = 0
        }
    }
    return longest
}

private fun countInitialPlacementIssues(
    level: EditorLevel,
    map: EditorMap?,
): Int {
    val initialData = level.getEffectiveInitialData()
    if (map == null) {
        return initialData.defenders.size +
            initialData.attackers.size +
            initialData.traps.size +
            initialData.barricades.size +
            initialData.fiefs.size
    }
    val buildAreas = map.getBuildAreas()
    val traversable = map.getPathCells() + map.getSpawnPoints() + map.getTargets() + map.getRiverCells()
    return initialData.defenders.count { defender ->
        defender.position !in buildAreas && initialData.barricades.none { it.supportsTower && it.position == defender.position }
    } +
        initialData.attackers.count { it.position !in traversable } +
        initialData.traps.count { it.position !in traversable } +
        initialData.barricades.count { !it.position.isInside(map.width, map.height) } +
        initialData.fiefs.count { it.position !in traversable }
}

private fun countInvalidEventPositions(
    level: EditorLevel,
    map: EditorMap?,
): Int =
    level.events.events.count { event ->
        val conditionInvalid = event.condition.position?.let { position -> map == null || !position.isInside(map.width, map.height) } == true
        val actionInvalid = event.actions.any { action -> action.position?.let { position -> map == null || !position.isInside(map.width, map.height) } == true }
        conditionInvalid || actionInvalid
    }

private fun Position.isInside(
    width: Int,
    height: Int,
): Boolean = x in 0 until width && y in 0 until height

private fun Position.distanceTo(other: Position): Int {
    val dx = x - other.x
    val dy = y - other.y
    return kotlin.math.abs(dx) + kotlin.math.abs(dy)
}

private fun AttackerType.calculateTargetDamage(level: Int): Int =
    when {
        this == AttackerType.EWHAD -> 99
        this.isRealVillain || this.isDragon || canDisableTowers || canHeal || this == AttackerType.EVIL_WIZARD || this == AttackerType.BLUE_DEMON || this == AttackerType.RED_DEMON -> level
        else -> 1
    }
