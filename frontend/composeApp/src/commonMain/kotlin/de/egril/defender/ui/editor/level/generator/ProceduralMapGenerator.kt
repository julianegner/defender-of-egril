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
    val spawnCount: Int = 1,
    val targetCount: Int = 1,
    val pathWindingFactor: Float = 0.3f, // 0.0 = straight, 1.0 = very winding
    val waterLevel: Float = 0.2f, // 0.0 = dry, 1.0 = very wet
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
                random = Random(config.seed xor config.mapDescription.hashCode()),
            ).generate(
                mapId = "${levelId}_map",
                mapName = "${config.title} Map",
                author = config.author,
            )
        return if (map.validateReadyToUse()) {
            map.copy(readyToUse = true)
        } else {
            repairGeneratedMap(map).copy(readyToUse = true)
        }
    }

    private fun buildGenerationConfig(config: LevelGeneratorConfig): GenerationConfig {
        val normalized = config.mapDescription.lowercase()
        val rosters = listOfNotNull(config.primaryRoster, config.secondaryRoster)

        val suggestedSpawnCount =
            when {
                config.spawnCount > 0 -> config.spawnCount
                "web" in normalized || "spider" in normalized -> 4
                "cross" in normalized || "split" in normalized || "dual" in normalized || "fork" in normalized -> 3
                GeneratorEnemyRoster.SPIDERS in rosters -> 4
                GeneratorEnemyRoster.PIRATES in rosters -> 3
                else -> 2
            }
        val suggestedTargetCount =
            when {
                config.targetCount > 0 -> config.targetCount
                "multi target" in normalized || "multiple target" in normalized || "several target" in normalized -> 2
                "cross" in normalized || "split" in normalized -> 2
                else -> 1
            }
        val suggestedWaterLevel =
            when {
                "river" in normalized || "water" in normalized || "harbor" in normalized || "raft" in normalized -> 0.6f
                "island" in normalized || "archipelago" in normalized -> 0.75f
                GeneratorEnemyRoster.PIRATES in rosters -> 0.65f
                else -> config.waterLevel
            }
        val suggestedWinding =
            when {
                "straight" in normalized || "plain" in normalized -> 0.05f
                "snake" in normalized || "serpentine" in normalized || "winding" in normalized || "maze" in normalized -> 0.8f
                else -> config.pathWindingFactor
            }

        return GenerationConfig(
            width = config.mapWidth.coerceIn(5, 100),
            height = config.mapHeight.coerceIn(5, 100),
            spawnCount = suggestedSpawnCount.coerceIn(1, 8),
            targetCount = suggestedTargetCount.coerceIn(1, 4),
            pathWindingFactor = suggestedWinding.coerceIn(0f, 1f),
            waterLevel = suggestedWaterLevel.coerceIn(0f, 1f),
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
            tiles[key(position)] = TileType.BUILD_AREA
        }

        generateRiversAndWater(tiles, riverTiles)
        generateNoPlayAreas(tiles)

        val targets = mutableListOf<Position>()
        repeat(config.targetCount) {
            val target = findValidExtremePosition(tiles, preferLeft = false, excluded = targets)
            tiles[key(target)] = TileType.TARGET
            targetInfoMap[key(target)] = EditorTargetInfo(name = "Target ${targets.size + 1}", type = TargetType.STANDARD)
            targets += target
        }

        val spawns = mutableListOf<Position>()
        repeat(config.spawnCount) { index ->
            val preferWaterSpawn = config.waterLevel > 0.4f && index % 2 == 0
            val spawn = findSpawnPosition(tiles, preferWaterSpawn, spawns + targets)
            val wasRiverSpawn = tileType(tiles, spawn) == TileType.RIVER
            tiles[key(spawn)] = TileType.SPAWN_POINT
            spawnPointInfoMap[key(spawn)] = if (wasRiverSpawn) SpawnPointType.WATER else SpawnPointType.LAND
            spawns += spawn
        }

        if (config.requirePath && targets.isNotEmpty()) {
            spawns.forEachIndexed { index, spawn ->
                val target = targets[index % targets.size]
                val path = findWindingPath(spawn, target, tiles)
                path.forEach { position ->
                    if (position != spawn && position != target) {
                        val type = tileType(tiles, position)
                        if (type != TileType.SPAWN_POINT && type != TileType.TARGET) {
                            tiles[key(position)] = TileType.PATH
                        }
                    }
                }
            }
        }

        // Ensure path tiles have enough adjacent tower placement possibilities.
        tiles.entries
            .filter { it.value == TileType.PATH || it.value == TileType.SPAWN_POINT || it.value == TileType.TARGET }
            .map { decode(it.key) }
            .forEach { pathTile ->
                pathTile.getHexNeighbors()
                    .filter { it in validGridPositions }
                    .forEach { neighbor ->
                        val current = tileType(tiles, neighbor)
                        if (current == TileType.BUILD_AREA && random.nextFloat() < 0.6f) {
                            tiles[key(neighbor)] = TileType.BUILD_AREA
                        }
                    }
            }

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
                    current.getHexNeighbors()
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
            if (tileType(tiles, position) == TileType.BUILD_AREA && random.nextFloat() < blockedChance) {
                tiles[key(position)] = TileType.NO_PLAY
            }
        }
    }

    private fun findSpawnPosition(
        tiles: Map<String, TileType>,
        preferWater: Boolean,
        excluded: List<Position>,
    ): Position {
        if (preferWater) {
            val riverSpawns =
                validGridPositions.filter { position ->
                    tileType(tiles, position) == TileType.RIVER &&
                        position !in excluded &&
                        position.x < config.width / 3
                }
            if (riverSpawns.isNotEmpty()) return riverSpawns.random(random)
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
            current.getHexNeighbors()
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

    private fun tileType(
        tiles: Map<String, TileType>,
        position: Position,
    ): TileType = tiles[key(position)] ?: TileType.NO_PLAY

    private fun key(position: Position): String = "${position.x},${position.y}"

    private fun decode(key: String): Position {
        val parts = key.split(",")
        return Position(parts[0].toInt(), parts[1].toInt())
    }
}

private fun repairGeneratedMap(map: EditorMap): EditorMap {
    val target = map.getTarget() ?: return map
    val spawnPoints = map.getSpawnPoints()
    if (spawnPoints.isEmpty()) return map

    val tiles = map.tiles.toMutableMap()
    val spawnSet = spawnPoints.toSet()

    spawnPoints.forEach { spawn ->
        var current = spawn
        var guard = 0
        while (current != target && guard < map.width * map.height) {
            guard++
            val next =
                current
                    .getHexNeighbors()
                    .filter { it.x in 0 until map.width && it.y in 0 until map.height }
                    .minByOrNull { it.hexDistanceTo(target) } ?: break
            if (next.hexDistanceTo(target) >= current.hexDistanceTo(target)) break
            if (next != target && next !in spawnSet) {
                tiles["${next.x},${next.y}"] = TileType.PATH
            }
            current = next
        }
    }

    return map.copy(tiles = tiles)
}
