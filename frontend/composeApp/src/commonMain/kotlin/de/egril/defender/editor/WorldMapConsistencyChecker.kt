package de.egril.defender.editor

/**
 * Severity of a world map consistency finding.
 */
enum class ConsistencySeverity {
    ERROR, // Data is broken (dangling references, duplicate placements)
    WARNING, // Data is valid but likely unintended (unplaced levels, unreachable locations)
}

/**
 * A single finding reported by [WorldMapConsistencyChecker].
 */
data class ConsistencyIssue(
    val severity: ConsistencySeverity,
    val message: String,
)

/**
 * Checks that the level sequence and the world map agree with each other:
 * every level is placed exactly once, all references resolve, and every location
 * can be reached from the first location of the sequence via paths.
 */
object WorldMapConsistencyChecker {
    /**
     * @param ignoredLevelIds levels intentionally not placed on the map (e.g. test levels)
     * @param ignoredLocationIds locations intentionally hidden or unreachable (e.g. test locations)
     */
    fun check(
        sequence: List<String>,
        worldMap: WorldMapData,
        ignoredLevelIds: Set<String> = emptySet(),
        ignoredLocationIds: Set<String> = emptySet(),
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()
        val locationIds = worldMap.locations.map { it.id }.toSet()

        val placements = mutableMapOf<String, MutableList<String>>()
        worldMap.locations.forEach { location ->
            location.levelIds.forEach { levelId ->
                placements.getOrPut(levelId) { mutableListOf() }.add(location.id)
            }
        }

        placements.filter { it.value.size > 1 }.forEach { (levelId, locations) ->
            issues += ConsistencyIssue(
                ConsistencySeverity.ERROR,
                "Level '$levelId' is placed on multiple locations: ${locations.joinToString()}",
            )
        }

        worldMap.locations.filter { it.levelIds.isEmpty() }.forEach { location ->
            issues += ConsistencyIssue(
                ConsistencySeverity.WARNING,
                "Location '${location.id}' has no levels",
            )
        }

        placements.keys.filter { it !in sequence }.forEach { levelId ->
            issues += ConsistencyIssue(
                ConsistencySeverity.ERROR,
                "Level '$levelId' is placed on the map but not in the level sequence",
            )
        }

        sequence.filter { it !in placements && it !in ignoredLevelIds }.forEach { levelId ->
            issues += ConsistencyIssue(
                ConsistencySeverity.WARNING,
                "Level '$levelId' is in the sequence but not placed on any location",
            )
        }

        worldMap.paths.forEach { path ->
            listOf(path.fromLocationId, path.toLocationId)
                .filter { it !in locationIds }
                .forEach { missing ->
                    issues += ConsistencyIssue(
                        ConsistencySeverity.ERROR,
                        "Path ${path.fromLocationId} -> ${path.toLocationId} references unknown location '$missing'",
                    )
                }
        }

        worldMapStartLocation(sequence, worldMap)?.let { start ->
            val reached = worldMapReachableLocationIds(sequence, worldMap)
            worldMap.locations.filter { it.id !in reached && it.id !in ignoredLocationIds }.forEach { location ->
                issues += ConsistencyIssue(
                    ConsistencySeverity.WARNING,
                    "Location '${location.id}' is not reachable from '${start.id}' via paths",
                )
            }
        }

        return issues
    }
}
