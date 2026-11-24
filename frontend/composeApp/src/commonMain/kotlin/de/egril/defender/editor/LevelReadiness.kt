package de.egril.defender.editor

/**
 * A single reason why a level is not ready to play.
 */
sealed interface LevelReadinessIssue {
    object NoTowers : LevelReadinessIssue
    object NoSpawns : LevelReadinessIssue
    object SpawnGroupsInvalid : LevelReadinessIssue
    object NoStartCoins : LevelReadinessIssue
    object NoStartHealth : LevelReadinessIssue
    object MapMissing : LevelReadinessIssue
    object MapNotReady : LevelReadinessIssue
    object PortalsMissing : LevelReadinessIssue
    object NoBuildableTileFallback : LevelReadinessIssue
    object WaypointsInvalid : LevelReadinessIssue

    /** A problem reported by the level's map validation. */
    data class MapProblem(val issue: MapValidationIssue) : LevelReadinessIssue
}

/**
 * Collects every reason why a level is not ready to play. An empty list means the level is ready.
 * Unlike a boolean check, all failing conditions are reported so the editor can explain them.
 */
fun EditorStorage.getLevelReadinessIssues(level: EditorLevel): List<LevelReadinessIssue> {
    val issues = mutableListOf<LevelReadinessIssue>()
    if (!level.hasTowerSelectionOptions()) issues += LevelReadinessIssue.NoTowers
    if (!level.isSandbox && !level.hasConfiguredSpawns()) issues += LevelReadinessIssue.NoSpawns
    if (level.startCoins <= 0) issues += LevelReadinessIssue.NoStartCoins
    if (level.startHealthPoints <= 0) issues += LevelReadinessIssue.NoStartHealth

    val spawnGroups = level.spawnGroups
    if (!level.isSandbox && spawnGroups != null) {
        val stoppedLoopIds = level.events.stoppedSpawnLoopIds()
        val map = getMap(level.mapId)
        val hasGroupIssues =
            spawnGroupIssues(spawnGroups, stoppedLoopIds = stoppedLoopIds).isNotEmpty() ||
                (map != null && spawnGroupIssues(spawnGroups, map, stoppedLoopIds).isNotEmpty())
        if (hasGroupIssues) issues += LevelReadinessIssue.SpawnGroupsInvalid
    }

    val map = getMap(level.mapId)
    if (map == null) {
        issues += LevelReadinessIssue.MapMissing
        return issues
    }

    // Rivers are always treated as walkable here (matching the editor default), so community maps
    // whose path crosses river tiles are not rejected. Runtime pathfinding decides crossing rules.
    if (!map.validateReadyToUse(includeRiversAsWalkable = true)) {
        val mapIssues = map.getValidationIssues(includeRiversAsWalkable = true)
        if (mapIssues.isEmpty()) {
            issues += LevelReadinessIssue.MapNotReady
        } else {
            mapIssues.forEach { issues += LevelReadinessIssue.MapProblem(it) }
        }
    }

    if (map.allowNoDirectPath) {
        val portals = level.getEffectiveInitialData().portals
        if (!map.validateReadyToUseWithPortals(portals, includeRiversAsWalkable = true)) {
            issues += LevelReadinessIssue.PortalsMissing
        }
    }

    if (map.allowNoBuildableTiles && !map.hasBuildablePlacementTiles() && !level.hasNoBuildableTileFallback()) {
        issues += LevelReadinessIssue.NoBuildableTileFallback
    }

    val waypointResult =
        level.validateWaypointsDetailed(
            targetPositions = map.getTargets(),
            spawnPoints = map.getSpawnPoints(),
        )
    if (!waypointResult.isValid) issues += LevelReadinessIssue.WaypointsInvalid

    return issues
}
