package de.egril.defender.ui.editor.level.generator

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.editor.MapTemplateLayoutKind
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EnemyFaction
import de.egril.defender.model.Position
import de.egril.defender.model.isRealVillain
import de.egril.defender.model.isSwarmUnit
import de.egril.defender.ui.editor.map.createMapFromTemplate
import kotlin.random.Random

/**
 * Sizes offered by the level generator when a new map is generated together with the level.
 */
internal enum class GeneratedMapSize(
    val width: Int,
    val height: Int,
) {
    SMALL(20, 10),
    MEDIUM(34, 16),
    LARGE(50, 24),
    GIGANTIC(70, 34),
}

/**
 * Difficulty presets of the level generator. They control the number of waves, the enemy strength
 * and the resources the player starts with.
 */
internal enum class GeneratorDifficulty(
    val waveCount: Int,
    val enemiesPerWave: Int,
    val baseEnemyLevel: Int,
    val startCoins: Int,
    val startHealthPoints: Int,
) {
    EASY(waveCount = 6, enemiesPerWave = 2, baseEnemyLevel = 1, startCoins = 220, startHealthPoints = 15),
    MEDIUM(waveCount = 9, enemiesPerWave = 3, baseEnemyLevel = 1, startCoins = 160, startHealthPoints = 12),
    HARD(waveCount = 12, enemiesPerWave = 4, baseEnemyLevel = 2, startCoins = 120, startHealthPoints = 10),
    NIGHTMARE(waveCount = 15, enemiesPerWave = 5, baseEnemyLevel = 3, startCoins = 100, startHealthPoints = 8),
}

/**
 * Whether the generator should use an existing map or generate a new one.
 * This has to be decided before the level itself is generated.
 */
internal enum class GeneratorMapSource {
    EXISTING_MAP,
    GENERATED_MAP,
}

/**
 * All inputs of a level generator run.
 */
internal data class LevelGeneratorConfig(
    val title: String,
    val author: String = "",
    val difficulty: GeneratorDifficulty = GeneratorDifficulty.MEDIUM,
    // Selected villains. Their factions determine which regular enemies are mainly used.
    val villains: Set<AttackerType> = emptySet(),
    val mapSource: GeneratorMapSource = GeneratorMapSource.GENERATED_MAP,
    // Used when [mapSource] is EXISTING_MAP.
    val existingMap: EditorMap? = null,
    // Used when [mapSource] is GENERATED_MAP.
    val mapSize: GeneratedMapSize = GeneratedMapSize.MEDIUM,
    val seed: Int = 0,
)

/**
 * Result of a generator run. [generatedMap] is only set when a new map was generated and therefore
 * has to be stored together with the level.
 */
internal data class GeneratedLevelResult(
    val level: EditorLevel,
    val generatedMap: EditorMap?,
)

/**
 * Generates a complete, playable level draft from a small set of inputs (difficulty, villains and
 * the map to play on). The result is a normal [EditorLevel] that can be refined in the level editor.
 */
internal object LevelGenerator {
    private val baseTowers =
        setOf(
            DefenderType.SPIKE_TOWER,
            DefenderType.SPEAR_TOWER,
            DefenderType.BOW_TOWER,
        )

    fun generate(config: LevelGeneratorConfig): GeneratedLevelResult {
        val random = Random(config.seed)
        val levelId = generateId(config.title, random)
        val generatedMap =
            if (config.mapSource == GeneratorMapSource.GENERATED_MAP) {
                generateMap(levelId, config, random)
            } else {
                null
            }
        val map = generatedMap ?: config.existingMap
        val villains = config.villains.filter { it.isRealVillain }.sortedBy { it.ordinal }
        val minionPool = minionPoolFor(villains)
        val spawns = generateSpawns(map, config.difficulty, villains, minionPool, random)

        val level =
            EditorLevel(
                id = levelId,
                mapId = map?.id ?: "",
                title = config.title,
                startCoins = config.difficulty.startCoins,
                startHealthPoints = config.difficulty.startHealthPoints,
                enemySpawns = spawns,
                availableTowers = availableTowersFor(spawns),
                author = config.author,
            )
        return GeneratedLevelResult(level = level, generatedMap = generatedMap)
    }

    /**
     * Regular enemies matching the factions of the selected villains. When no villain is selected
     * (or all of them are faction-less), enemies of all factions may be used.
     */
    fun minionPoolFor(villains: Collection<AttackerType>): List<AttackerType> {
        val factions = villains.map { it.faction }.filter { it != EnemyFaction.NONE }.toSet()
        val spawnable =
            AttackerType.entries.filter {
                !it.isRealVillain &&
                    !it.isMirrorImage &&
                    !it.isBoss &&
                    !it.isDragon &&
                    !it.isSwarmUnit() &&
                    it.canSpawnOnLand
            }
        val matching = spawnable.filter { it.faction in factions }
        return matching.ifEmpty { spawnable }
    }

    private fun generateMap(
        levelId: String,
        config: LevelGeneratorConfig,
        random: Random,
    ): EditorMap {
        val layoutKind = MapTemplateLayoutKind.entries[random.nextInt(MapTemplateLayoutKind.entries.size)]
        return createMapFromTemplate(
            id = "${levelId}_map",
            name = "${config.title} Map",
            width = config.mapSize.width,
            height = config.mapSize.height,
            author = config.author,
            template =
                MapTemplateDefinition(
                    id = "generated_${layoutKind.name.lowercase()}",
                    name = layoutKind.name,
                    layoutKind = layoutKind,
                ),
        )
    }

    private fun generateSpawns(
        map: EditorMap?,
        difficulty: GeneratorDifficulty,
        villains: List<AttackerType>,
        minionPool: List<AttackerType>,
        random: Random,
    ): List<EditorEnemySpawn> {
        if (minionPool.isEmpty()) return emptyList()
        val spawns = mutableListOf<EditorEnemySpawn>()
        // Villains appear in the last waves, one per wave, so the level ends with its hardest fight.
        val firstVillainWave = (difficulty.waveCount - villains.size + 1).coerceAtLeast(1)

        for (wave in 1..difficulty.waveCount) {
            val turn = wave * 2
            val enemyLevel = difficulty.baseEnemyLevel + (wave - 1) / 3
            val amount = difficulty.enemiesPerWave + (wave - 1) / 3
            repeat(amount) { index ->
                val type = minionPool[random.nextInt(minionPool.size)]
                spawns +=
                    EditorEnemySpawn(
                        attackerType = type,
                        level = enemyLevel,
                        spawnTurn = turn,
                        spawnPoint = spawnPointFor(map, type, index),
                    )
            }
            val villainIndex = wave - firstVillainWave
            if (villainIndex in villains.indices) {
                val villain = villains[villainIndex]
                spawns +=
                    EditorEnemySpawn(
                        attackerType = villain,
                        level = enemyLevel,
                        spawnTurn = turn,
                        spawnPoint = spawnPointFor(map, villain, 0),
                    )
            }
        }
        return spawns.sortedBy { it.spawnTurn }
    }

    private fun spawnPointFor(
        map: EditorMap?,
        type: AttackerType,
        index: Int,
    ): Position? {
        val compatible = map?.getCompatibleSpawnPoints(type).orEmpty()
        return if (compatible.isEmpty()) null else compatible[index % compatible.size]
    }

    /**
     * Adds the towers needed to counter the generated enemies to the basic tower set.
     */
    private fun availableTowersFor(spawns: List<EditorEnemySpawn>): Set<DefenderType> {
        val types = spawns.map { it.attackerType }.toSet()
        val towers = baseTowers.toMutableSet()
        if (types.any { it.immuneToAcid || it.immuneToNonMagical || it.immuneToNonMagicTowerDamage || it.immuneToBladeAttacks }) {
            towers += DefenderType.WIZARD_TOWER
        }
        if (types.any { it.immuneToFireball }) {
            towers += DefenderType.ALCHEMY_TOWER
        }
        if (types.any { it.isRealVillain }) {
            towers += DefenderType.WIZARD_TOWER
            towers += DefenderType.ALCHEMY_TOWER
            towers += DefenderType.BALLISTA_TOWER
        }
        return towers
    }

    private fun generateId(
        title: String,
        random: Random,
    ): String {
        val sanitizedTitle =
            title
                .trim()
                .lowercase()
                .replace(" ", "_")
                .replace(Regex("[^a-z0-9_]"), "")
                .replace(Regex("_+"), "_")
        val prefix = sanitizedTitle.ifEmpty { "generated" }
        return "${prefix}_${random.nextInt(1000, 9999)}"
    }
}
