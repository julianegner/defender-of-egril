package de.egril.defender.ui.editor.level.generator

import de.egril.defender.editor.DEFAULT_MAP_TOOLING_INFO
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.hexDistanceTo
import kotlin.random.Random

/**
 * Configuration-driven procedural generator used by the level generator.
 * The values are intentionally UI-friendly and can be bound to form controls directly.
 */
internal data class GenerationConfig(
    val width: Int = 20,
    val height: Int = 15,
    val landSpawnCount: Int = 1,
    val waterSpawnCount: Int = 0,
    val targetCount: Int = 1,
    val pathWindingFactor: Float = 0.3f, // 0.0 = straight, 1.0 = very winding
    val waterLevel: Float = 0.2f, // 0.0 = dry, 1.0 = very wet
    val minPathWidth: Int = 3,
    val requirePath: Boolean = true,
)

internal object ProceduralMapGenerator {
    fun generateMap(
        levelId: String,
        config: LevelGeneratorConfig,
        random: Random,
    ): EditorMap {
        val generationConfig = buildGenerationConfig(config)
        val map =
            HexMapGenerator(
                config = generationConfig,
                random = Random(config.seed),
            ).generate(
                mapId = "${levelId}_map",
                mapName = "${config.title} Map",
                author = config.author,
            )
        return if (map.validateReadyToUse()) {
            map.copy(readyToUse = true)
        } else {
            repairGeneratedMap(map, generationConfig.minPathWidth).copy(readyToUse = true)
        }
    }

    private fun buildGenerationConfig(config: LevelGeneratorConfig): GenerationConfig {
        val suggestedLandSpawnCount = config.landSpawnCount
        val suggestedTargetCount = config.targetCount
        val suggestedWaterSpawns = config.waterSpawnCount

        val clampedLandSpawnCount = suggestedLandSpawnCount.coerceIn(0, 8)
        val clampedWaterSpawnCount = suggestedWaterSpawns.coerceIn(0, 8)
        val totalSpawnCount = clampedLandSpawnCount + clampedWaterSpawnCount
        val normalizedLandSpawnCount = if (totalSpawnCount == 0) 1 else clampedLandSpawnCount
        val normalizedWaterSpawnCount = if (totalSpawnCount == 0) 0 else clampedWaterSpawnCount

        return GenerationConfig(
            width = config.mapWidth.coerceIn(5, 100),
            height = config.mapHeight.coerceIn(5, 100),
            landSpawnCount = normalizedLandSpawnCount,
            waterSpawnCount = normalizedWaterSpawnCount,
            targetCount = suggestedTargetCount.coerceIn(1, 4),
            pathWindingFactor = config.pathWindingFactor.coerceIn(0f, 1f),
            waterLevel = config.waterLevel.coerceIn(0f, 1f),
            minPathWidth = config.minPathWidth.coerceIn(1, 4),
            requirePath = config.requirePath,
        )
    }
}

private class HexMapGenerator(
    private val config: GenerationConfig,
    private val random: Random,
) {
    private val validGridPositions: Set<Position> by lazy {
        buildSet {
            for (x in 0 until config.width) {
                for (y in 0 until config.height) {
                    add(Position(x, y))
                }
            }
        }
    }

    fun generate(
        mapId: String,
        mapName: String,
        author: String,
    ): EditorMap {
        val tiles = mutableMapOf<String, TileType>()
        val riverTiles = mutableMapOf<String, RiverTile>()
        val targetInfoMap = mutableMapOf<String, EditorTargetInfo>()
        val spawnPointInfoMap = mutableMapOf<String, SpawnPointType>()

        validGridPositions.forEach { position ->
            tiles[key(position)] = TileType.PATH
        }

        generateRiversAndWater(tiles, riverTiles)
        generateNoPlayAreas(tiles)

        val spawns = mutableListOf<Position>()
        repeat(config.waterSpawnCount) {
            val spawn = findSpawnPosition(tiles, requireWater = true, excluded = spawns)
            tiles[key(spawn)] = TileType.SPAWN_POINT
            spawnPointInfoMap[key(spawn)] = SpawnPointType.WATER
            spawns += spawn
        }
        repeat(config.landSpawnCount) {
            val spawn = findSpawnPosition(tiles, requireWater = false, excluded = spawns)
            tiles[key(spawn)] = TileType.SPAWN_POINT
            spawnPointInfoMap[key(spawn)] = SpawnPointType.LAND
            spawns += spawn
        }

        val targets = mutableListOf<Position>()
        val minimumDistance = minimumSpawnTargetDistance()
        repeat(config.targetCount) {
            val target = findFarTargetPosition(tiles, spawns, spawnPointInfoMap, targets, minimumDistance)
            tiles[key(target)] = TileType.TARGET
            targetInfoMap[key(target)] = EditorTargetInfo(name = "Target ${targets.size + 1}", type = TargetType.STANDARD)
            targets += target
        }

        val protectedPathTiles = mutableSetOf<Position>()
        if (config.requirePath && targets.isNotEmpty()) {
            spawns.forEachIndexed { index, spawn ->
                if (spawnPointInfoMap[key(spawn)] == SpawnPointType.WATER) {
                    return@forEachIndexed
                }
                val target = farthestTargetForSpawn(spawn, targets, spawnPointInfoMap.getValue(key(spawn)), tiles, index)
                val path = findWindingPath(spawn, target, tiles)
                val widenedPath = widenPath(path, config.minPathWidth)
                protectedPathTiles.addAll(widenedPath)
                widenedPath.forEach { position ->
                    if (position != spawn && position != target) {
                        val type = tileType(tiles, position)
                        if (type != TileType.SPAWN_POINT && type != TileType.TARGET) {
                            tiles[key(position)] = TileType.PATH
                        }
                    }
                }
            }
        }

        addBuildAreas(tiles, protectedPathTiles, spawns, targets)

        val map =
            EditorMap(
                id = mapId,
                name = mapName,
                width = config.width,
                height = config.height,
                tiles = tiles,
                riverTiles = riverTiles,
                targetInfoMap = targetInfoMap,
                spawnPointInfoMap = spawnPointInfoMap,
                author = author,
                mapToolingInfo = DEFAULT_MAP_TOOLING_INFO,
                allowNoDirectPath = !config.requirePath,
            )
        return map.copy(readyToUse = map.validateReadyToUse())
    }

    private fun generateRiversAndWater(
        tiles: MutableMap<String, TileType>,
        riverTiles: MutableMap<String, RiverTile>,
    ) {
        if (config.waterLevel <= 0f) return
        val riverBranches = (config.waterLevel * 6f).toInt().coerceAtLeast(1)

        repeat(riverBranches) {
            var current =
                validGridPositions
                    .filter { it.x == 0 || it.y == 0 || it.y == config.height - 1 }
                    .random(random)
            val visited = mutableSetOf<Position>()
            val maxLength = ((config.width + config.height) * (0.6f + config.waterLevel)).toInt().coerceAtLeast(12)

            while (current in validGridPositions && visited.size < maxLength) {
                tiles[key(current)] = TileType.RIVER
                riverTiles[key(current)] =
                    RiverTile(
                        position = current,
                        flowDirection = RiverFlow.SOUTH_EAST,
                        flowSpeed = 1,
                    )
                visited += current

                if (config.waterLevel > 0.5f) {
                    current
                        .getHexNeighbors()
                        .filter { it in validGridPositions && random.nextFloat() < 0.35f + ((config.waterLevel - 0.5f) * 0.35f) }
                        .forEach { neighbor ->
                            tiles[key(neighbor)] = TileType.RIVER
                            riverTiles[key(neighbor)] =
                                RiverTile(
                                    position = neighbor,
                                    flowDirection = RiverFlow.SOUTH_EAST,
                                    flowSpeed = 1,
                                )
                        }
                }

                val nextOptions = current.getHexNeighbors().filter { it in validGridPositions && it !in visited }
                if (nextOptions.isEmpty()) break
                current =
                    nextOptions.maxByOrNull { option ->
                        // Push river generally from left to right but keep organic turns.
                        val progressBias = option.x.toFloat() / config.width
                        val turnNoise = random.nextFloat() * 0.8f
                        progressBias + turnNoise
                    } ?: break
            }
        }
    }

    private fun generateNoPlayAreas(tiles: MutableMap<String, TileType>) {
        val blockedChance = 0.14f * (1f - config.waterLevel)
        validGridPositions.forEach { position ->
            if (tileType(tiles, position) == TileType.PATH && random.nextFloat() < blockedChance) {
                tiles[key(position)] = TileType.NO_PLAY
            }
        }
    }

    private fun addBuildAreas(
        tiles: MutableMap<String, TileType>,
        protectedPathTiles: Set<Position>,
        spawns: List<Position>,
        targets: List<Position>,
    ) {
        val reserved = (spawns + targets).toSet() + protectedPathTiles
        validGridPositions.forEach { position ->
            if (position in reserved) return@forEach
            if (tileType(tiles, position) != TileType.PATH) return@forEach
            val nearProtectedPath = position.getHexNeighbors().any { it in protectedPathTiles }
            val chance = if (nearProtectedPath) 0.5f else 0.08f
            if (random.nextFloat() < chance) {
                tiles[key(position)] = TileType.BUILD_AREA
            }
        }

        val hasBuildAreas = tiles.values.any { it == TileType.BUILD_AREA }
        val hasRiverTiles = tiles.values.any { it == TileType.RIVER }
        if (!hasBuildAreas && !hasRiverTiles) {
            val fallbackBuildArea =
                validGridPositions.firstOrNull { position ->
                    position !in reserved && tileType(tiles, position) == TileType.PATH
                }
            if (fallbackBuildArea != null) {
                tiles[key(fallbackBuildArea)] = TileType.BUILD_AREA
            }
        }
    }

    private fun findSpawnPosition(
        tiles: Map<String, TileType>,
        requireWater: Boolean,
        excluded: List<Position>,
    ): Position {
        if (requireWater) {
            val riverSpawns =
                validGridPositions.filter { position ->
                    tileType(tiles, position) == TileType.RIVER &&
                        position !in excluded &&
                        position.x < config.width / 3
                }
            if (riverSpawns.isNotEmpty()) {
                return riverSpawns.maxByOrNull { it.hexDistanceTo(Position(config.width - 1, config.height / 2)) } ?: riverSpawns.first()
            }
        }
        return findValidExtremePosition(tiles, preferLeft = true, excluded = excluded)
    }

    private fun findValidExtremePosition(
        tiles: Map<String, TileType>,
        preferLeft: Boolean,
        excluded: List<Position>,
    ): Position {
        val candidates =
            validGridPositions
                .filter { it !in excluded && tileType(tiles, it) != TileType.NO_PLAY && tileType(tiles, it) != TileType.RIVER }
                .ifEmpty { validGridPositions.filter { it !in excluded } }
        return if (preferLeft) {
            candidates.minByOrNull { it.x + random.nextInt(0, 3) } ?: candidates.first()
        } else {
            candidates.maxByOrNull { it.x - random.nextInt(0, 3) } ?: candidates.first()
        }
    }

    private fun minimumSpawnTargetDistance(): Int {
        val shorterSide = minOf(config.width, config.height)
        return (shorterSide * 0.5f).toInt().coerceAtLeast(6)
    }

    private fun findFarTargetPosition(
        tiles: Map<String, TileType>,
        spawns: List<Position>,
        spawnPointInfoMap: Map<String, SpawnPointType>,
        existingTargets: List<Position>,
        minimumDistance: Int,
    ): Position {
        val spawnAnchors = spawns.filter { tileType(tiles, it) == TileType.SPAWN_POINT }
        val candidates =
            validGridPositions
                .filter { position ->
                    position !in existingTargets &&
                        tileType(tiles, position) != TileType.NO_PLAY &&
                        tileType(tiles, position) != TileType.RIVER &&
                        position.x >= config.width / 2
                }.ifEmpty {
                    validGridPositions.filter { position ->
                        position !in existingTargets &&
                            tileType(tiles, position) != TileType.NO_PLAY &&
                            tileType(tiles, position) != TileType.RIVER
                    }
                }
        if (candidates.isEmpty()) {
            return findValidExtremePosition(tiles, preferLeft = false, excluded = existingTargets)
        }

        val candidateScores =
            candidates.map { target ->
                val pathLengths =
                    spawnAnchors.mapNotNull { spawn ->
                        val spawnType = spawnPointInfoMap[key(spawn)] ?: SpawnPointType.LAND
                        shortestPathLength(
                            start = spawn,
                            end = target,
                            tiles = tiles,
                            allowRiverTiles = spawnType == SpawnPointType.WATER,
                        )
                    }
                val minPathLength = pathLengths.minOrNull() ?: -1
                target to minPathLength
            }
        val withThreshold = candidateScores.filter { (_, minPathLength) -> minPathLength >= minimumDistance }
        val evaluationPool = if (withThreshold.isNotEmpty()) withThreshold else candidateScores

        return evaluationPool
            .maxByOrNull { (target, minPathLength) ->
                val pathLengthScore = if (minPathLength < 0) 0f else minPathLength.toFloat() * 4f
                val targetSpacingScore =
                    if (existingTargets.isEmpty()) {
                        0f
                    } else {
                        existingTargets.minOf { it.hexDistanceTo(target) } * 0.4f
                    }
                val rightSideBias = (target.x.toFloat() / config.width) * 1.5f
                pathLengthScore + targetSpacingScore + rightSideBias + random.nextFloat() * 0.5f
            }?.first ?: evaluationPool.first().first
    }

    private fun farthestTargetForSpawn(
        spawn: Position,
        targets: List<Position>,
        spawnType: SpawnPointType,
        tiles: Map<String, TileType>,
        salt: Int,
    ): Position =
        targets.maxByOrNull { target ->
            val shortestPath =
                shortestPathLength(
                    start = spawn,
                    end = target,
                    tiles = tiles,
                    allowRiverTiles = spawnType == SpawnPointType.WATER,
                ) ?: spawn.hexDistanceTo(target)
            shortestPath * 100 + random.nextInt(0, 3) + salt
        } ?: targets.first()

    private fun shortestPathLength(
        start: Position,
        end: Position,
        tiles: Map<String, TileType>,
        allowRiverTiles: Boolean,
    ): Int? {
        val queue = ArrayDeque<Pair<Position, Int>>()
        val visited = mutableSetOf(start)
        queue.add(start to 0)

        while (queue.isNotEmpty()) {
            val (current, distance) = queue.removeFirst()
            if (current == end) return distance

            current
                .getHexNeighbors()
                .filter { it in validGridPositions && it !in visited }
                .forEach { neighbor ->
                    val neighborType = tileType(tiles, neighbor)
                    val traversable =
                        when (neighborType) {
                            TileType.NO_PLAY,
                            TileType.BUILD_AREA,
                            -> false
                            TileType.RIVER -> allowRiverTiles
                            else -> true
                        }
                    if (traversable) {
                        visited += neighbor
                        queue.add(neighbor to distance + 1)
                    }
                }
        }
        return null
    }

    private fun findWindingPath(
        start: Position,
        end: Position,
        tiles: Map<String, TileType>,
    ): List<Position> {
        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<Position, Position>()
        val gScore = mutableMapOf(start to 0f)
        val fScore = mutableMapOf(start to start.hexDistanceTo(end).toFloat())

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore[it] ?: Float.MAX_VALUE } ?: break
            if (current == end) return reconstructPath(cameFrom, end)

            openSet.remove(current)
            current
                .getHexNeighbors()
                .filter { it in validGridPositions }
                .forEach { neighbor ->
                    val baseCost =
                        when (tileType(tiles, neighbor)) {
                            TileType.NO_PLAY -> 20f
                            TileType.RIVER -> 5f
                            else -> 1f
                        }
                    val windingNoise = random.nextFloat() * config.pathWindingFactor * 15f
                    val tentative = (gScore[current] ?: 0f) + baseCost + windingNoise
                    if (tentative < (gScore[neighbor] ?: Float.MAX_VALUE)) {
                        cameFrom[neighbor] = current
                        gScore[neighbor] = tentative
                        fScore[neighbor] = tentative + neighbor.hexDistanceTo(end)
                        openSet += neighbor
                    }
                }
        }
        return forceFallbackLine(start, end)
    }

    private fun reconstructPath(
        cameFrom: Map<Position, Position>,
        current: Position,
    ): List<Position> {
        val path = mutableListOf(current)
        var cursor = current
        while (cameFrom.containsKey(cursor)) {
            cursor = cameFrom.getValue(cursor)
            path += cursor
        }
        return path.reversed()
    }

    private fun forceFallbackLine(
        start: Position,
        end: Position,
    ): List<Position> {
        val path = mutableListOf(start)
        var current = start
        var guard = 0
        while (current != end && guard < config.width * config.height) {
            guard++
            val next =
                current
                    .getHexNeighbors()
                    .filter { it in validGridPositions }
                    .minByOrNull { it.hexDistanceTo(end) } ?: break
            if (next.hexDistanceTo(end) >= current.hexDistanceTo(end)) break
            current = next
            path += current
        }
        return path
    }

    private fun widenPath(
        path: List<Position>,
        minPathWidth: Int,
    ): Set<Position> {
        val radius = (minPathWidth - 1).coerceAtLeast(0)
        if (radius == 0) return path.toSet()

        val widened = path.toMutableSet()
        path.forEach { start ->
            val visited = mutableSetOf(start)
            var frontier = setOf(start)
            repeat(radius) {
                frontier =
                    frontier
                        .flatMap { current -> current.getHexNeighbors() }
                        .filter { it in validGridPositions && visited.add(it) }
                        .toSet()
                widened.addAll(frontier)
            }
        }
        return widened
    }

    private fun tileType(
        tiles: Map<String, TileType>,
        position: Position,
    ): TileType = tiles[key(position)] ?: TileType.NO_PLAY

    private fun key(position: Position): String = "${position.x},${position.y}"
}

private fun repairGeneratedMap(
    map: EditorMap,
    minPathWidth: Int,
): EditorMap {
    val target = map.getTarget() ?: return map
    val spawnPoints = map.getSpawnPoints()
    if (spawnPoints.isEmpty()) return map

    val tiles = map.tiles.toMutableMap()
    val requiredSpawns = spawnPoints.filter { map.getSpawnPointType(it) != SpawnPointType.WATER }
    val spawnSet = requiredSpawns.toSet()

    requiredSpawns.forEach { spawn ->
        var current = spawn
        val linePath = mutableListOf(current)
        var guard = 0
        while (current != target && guard < map.width * map.height) {
            guard++
            val next =
                current
                    .getHexNeighbors()
                    .filter { it.x in 0 until map.width && it.y in 0 until map.height }
                    .minByOrNull { it.hexDistanceTo(target) } ?: break
            if (next.hexDistanceTo(target) >= current.hexDistanceTo(target)) break
            current = next
            linePath += current
        }

        widenPathWithinBounds(linePath, minPathWidth, map.width, map.height).forEach { position ->
            if (position != target && position !in spawnSet) {
                tiles["${position.x},${position.y}"] = TileType.PATH
            }
        }
    }

    return map.copy(tiles = tiles)
}

private fun widenPathWithinBounds(
    path: List<Position>,
    minPathWidth: Int,
    width: Int,
    height: Int,
): Set<Position> {
    val radius = (minPathWidth - 1).coerceAtLeast(0)
    if (radius == 0) return path.toSet()

    val widened = path.toMutableSet()
    path.forEach { start ->
        val visited = mutableSetOf(start)
        var frontier = setOf(start)
        repeat(radius) {
            frontier =
                frontier
                    .flatMap { current -> current.getHexNeighbors() }
                    .filter { it.x in 0 until width && it.y in 0 until height && visited.add(it) }
                    .toSet()
            widened.addAll(frontier)
        }
    }
    return widened
}
