package de.egril.defender.ui.editor

import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import de.egril.defender.editor.LevelReadinessIssue
import de.egril.defender.editor.MapValidationIssue
import de.egril.defender.model.MIN_ROTATABLE_REGION_TILES
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun MapValidationIssue.localizedText(): String =
    when (this) {
        MapValidationIssue.INVALID_MAP_DATA -> stringResource(Res.string.map_validation_invalid_map_data)
        MapValidationIssue.UNSUPPORTED_SIZE -> stringResource(Res.string.map_validation_unsupported_size)
        MapValidationIssue.NO_SPAWN_POINT -> stringResource(Res.string.map_validation_no_spawn_point)
        MapValidationIssue.NO_TARGET -> stringResource(Res.string.map_validation_no_target)
        MapValidationIssue.NO_BUILDABLE_TILES -> stringResource(Res.string.map_validation_no_buildable_tiles)
        MapValidationIssue.NO_PATH_FROM_SPAWN_TO_TARGET -> stringResource(Res.string.map_validation_no_path_from_spawn_to_target)
        MapValidationIssue.ROTATABLE_ZONE_TOO_SMALL ->
            stringResource(Res.string.map_validation_rotatable_zone_too_small, MIN_ROTATABLE_REGION_TILES)
    }

@Composable
fun LevelReadinessIssue.localizedText(): String =
    when (this) {
        LevelReadinessIssue.NoTowers -> stringResource(Res.string.level_issue_no_towers)
        LevelReadinessIssue.NoSpawns -> stringResource(Res.string.level_issue_no_spawns)
        LevelReadinessIssue.SpawnGroupsInvalid -> stringResource(Res.string.level_issue_spawn_groups_invalid)
        LevelReadinessIssue.NoStartCoins -> stringResource(Res.string.level_issue_no_start_coins)
        LevelReadinessIssue.NoStartHealth -> stringResource(Res.string.level_issue_no_start_health)
        LevelReadinessIssue.MapMissing -> stringResource(Res.string.level_issue_map_missing)
        LevelReadinessIssue.MapNotReady -> stringResource(Res.string.level_issue_map_not_ready)
        LevelReadinessIssue.PortalsMissing -> stringResource(Res.string.level_issue_portals_missing)
        LevelReadinessIssue.NoBuildableTileFallback -> stringResource(Res.string.level_issue_no_buildable_tile_fallback)
        LevelReadinessIssue.WaypointsInvalid -> stringResource(Res.string.level_issue_waypoints_invalid)
        is LevelReadinessIssue.MapProblem -> this.issue.localizedText()
    }
