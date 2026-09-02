package de.egril.defender.ui.editor.level.generator

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EnemyFaction
import de.egril.defender.model.Position
import de.egril.defender.model.isRealVillain
import kotlin.random.Random

/**
 * Sizes offered by the level generator when a new map is generated together with the level.
 */
internal enum class GeneratedMapSize(
    val width: Int,
    val height: Int,
) {
    // The dimensions follow the sizes of the existing official maps and can be adjusted
    // after a size has been selected.
    SMALL(20, 20),
    MEDIUM(30, 30),
    LARGE(40, 40),
    GIGANTIC(50, 50),
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
    EASY(waveCount = 6, enemiesPerWave = 8, baseEnemyLevel = 1, startCoins = 220, startHealthPoints = 15),
    MEDIUM(waveCount = 9, enemiesPerWave = 12, baseEnemyLevel = 1, startCoins = 160, startHealthPoints = 12),
    HARD(waveCount = 12, enemiesPerWave = 16, baseEnemyLevel = 2, startCoins = 120, startHealthPoints = 10),
    NIGHTMARE(waveCount = 15, enemiesPerWave = 20, baseEnemyLevel = 3, startCoins = 100, startHealthPoints = 8),
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
 * Enemy rosters that can be picked when no villain is selected. Each roster is a themed group of
 * regular enemies that the generator draws its waves from.
 */
internal enum class GeneratorEnemyRoster(
    val types: List<AttackerType>,
) {
    HORDE(listOf(AttackerType.GOBLIN, AttackerType.ORK, AttackerType.OGRE, AttackerType.TROLL, AttackerType.ROBOTIC_GOBLIN)),
    UNDEAD(listOf(AttackerType.SKELETON, AttackerType.ZOMBIE, AttackerType.GHOST)),
    DEMONS(listOf(AttackerType.BLUE_DEMON, AttackerType.RED_DEMON, AttackerType.EVIL_WIZARD)),
    WITCHES(listOf(AttackerType.RED_WITCH, AttackerType.GREEN_WITCH)),
    PIRATES(listOf(AttackerType.PIRATE)),
    SPIDERS(listOf(AttackerType.SPIDERLING)),
    WILDS(listOf(AttackerType.TROLL, AttackerType.OGRE)),
}

/**
 * Villains that can be picked in the level generator. Haga and Zussa are the coven twins of
 * Grand Coven-Mother Sybilla and only make sense together with her, so they are not offered as
 * standalone villains here.
 */
internal val AttackerType.isSelectableGeneratorVillain: Boolean
    get() = isRealVillain && this != AttackerType.HAGA && this != AttackerType.ZUSSA

/**
 * The roster that fits a villain. Villains without a faction (for example Araxxa) would otherwise
 * end up with a random enemy mix, so every villain gets an explicitly themed roster here.
 */
internal fun AttackerType.generatorRoster(): GeneratorEnemyRoster =
    when (this) {
        AttackerType.SNOTLING_BOSS,
        AttackerType.GAROKK,
        AttackerType.MORGUK_BONEWHISPER,
        AttackerType.BARON_RATTERZAHN,
        AttackerType.EWHAD,
        -> GeneratorEnemyRoster.HORDE
        AttackerType.FALLEN_SHIELDMAIDEN_FREYA,
        AttackerType.PRINCE_VALERIUS_THE_SOULREAPER,
        AttackerType.MORVATH_THE_SHADOWMASTER,
        -> GeneratorEnemyRoster.UNDEAD
        AttackerType.ZYTHAR_THE_RIFTCALLER,
        AttackerType.IGNIS_VA_THE_DRAGONVOICE,
        AttackerType.XARITHON_THE_SHADOW_DRAGON,
        -> GeneratorEnemyRoster.DEMONS
        AttackerType.GRAND_COVEN_MOTHER_SYBILLA,
        AttackerType.HAGA,
        AttackerType.ZUSSA,
        AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE,
        AttackerType.SILAS_THE_MASKMASTER,
        AttackerType.SYLVANAS_THE_MOLDING,
        -> GeneratorEnemyRoster.WITCHES
        AttackerType.CAPTAIN_RODERICH,
        AttackerType.THE_KRAKEN,
        -> GeneratorEnemyRoster.PIRATES
        AttackerType.ARAXXA,
        -> GeneratorEnemyRoster.SPIDERS
        else ->
            when (faction) {
                EnemyFaction.UNDEAD -> GeneratorEnemyRoster.UNDEAD
                else -> GeneratorEnemyRoster.HORDE
            }
    }

/**
 * The rosters that fit a set of villains. The first roster is used as primary roster, the second
 * one (if the villains span two themes) as secondary roster. The generator dialog uses this to
 * pre-set the roster dropdowns, which stay editable afterwards.
 */
internal fun rostersForVillains(villains: Collection<AttackerType>): Pair<GeneratorEnemyRoster, GeneratorEnemyRoster?> {
    val rosters =
        villains
            .filter { it.isRealVillain }
            .map { it.generatorRoster() }
            .distinct()
    return (rosters.firstOrNull() ?: GeneratorEnemyRoster.HORDE) to rosters.getOrNull(1)
}

/**
 * All inputs of a level generator run.
 */
internal data class LevelGeneratorConfig(
    val title: String,
    val author: String = "",
    val difficulty: GeneratorDifficulty = GeneratorDifficulty.MEDIUM,
    // Selected villains. They spawn as unique enemies; their themed rosters are pre-selected in
    // the generator dialog but can be changed there.
    val villains: Set<AttackerType> = emptySet(),
    // Enemy rosters the regular waves are drawn from.
    val primaryRoster: GeneratorEnemyRoster = GeneratorEnemyRoster.HORDE,
    val secondaryRoster: GeneratorEnemyRoster? = null,
    val mapSource: GeneratorMapSource = GeneratorMapSource.GENERATED_MAP,
    // Used when [mapSource] is EXISTING_MAP.
    val existingMap: EditorMap? = null,
    // Used when [mapSource] is GENERATED_MAP.
    val mapSize: GeneratedMapSize = GeneratedMapSize.MEDIUM,
    // Exact dimensions of the generated map. They default to the selected [mapSize] but can be
    // adjusted in the generator dialog.
    val mapWidth: Int = mapSize.width,
    val mapHeight: Int = mapSize.height,
    // Optional plain-text description of the desired map style (for example "river map with
    // islands and multiple spawns"). Used to pick fitting procedural layouts.
    val mapDescription: String = "",
    // Optional amount of spawn points on generated maps (0 = auto based on description/roster).
    val spawnCount: Int = 0,
    // Optional amount of targets on generated maps (0 = auto based on description/roster).
    val targetCount: Int = 0,
    // 0.0 = straight paths, 1.0 = very winding paths.
    val pathWindingFactor: Float = 0.35f,
    // 0.0 = dry, 1.0 = very wet map with broader rivers.
    val waterLevel: Float = 0.2f,
    val requirePath: Boolean = true,
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
    // Layouts used when no villain theme applies. The spiral layout is left out on purpose: it is
    // just a long S-shaped path and makes for boring levels.
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
        val minionPool = minionPoolFor(config.primaryRoster, config.secondaryRoster)
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
     * The regular enemies the waves are built from. The rosters are pre-set from the selected
     * villains in the generator dialog but can be changed there, so the generator always uses the
     * rosters of the configuration.
     */
    fun minionPoolFor(
        primaryRoster: GeneratorEnemyRoster = GeneratorEnemyRoster.HORDE,
        secondaryRoster: GeneratorEnemyRoster? = null,
    ): List<AttackerType> =
        listOfNotNull(primaryRoster, secondaryRoster)
            .flatMap { it.types }
            .distinct()
            .filter { !it.isRealVillain }

    private fun generateMap(
        levelId: String,
        config: LevelGeneratorConfig,
        random: Random,
    ): EditorMap {
        return ProceduralMapGenerator.generateMap(levelId, config, random)
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
        // Villains enter at the end of the first third of the waves: most of them need time to build
        // up their potential (summoning, auras), so spawning them late would waste their abilities.
        val firstVillainWave = ((difficulty.waveCount + 2) / 3).coerceAtLeast(1)
        val villainWaves =
            villains
                .mapIndexed { index, villain -> (firstVillainWave + index).coerceAtMost(difficulty.waveCount) to villain }
                .groupBy({ it.first }, { it.second })

        val maxUnitsPerTurn = 6
        for (wave in 1..difficulty.waveCount) {
            val enemyLevel = difficulty.baseEnemyLevel + (wave - 1) / 3
            // Keep the wave size stable instead of growing indefinitely with each wave. The generator
            // must respect the limited number of tiles available around a spawn point, otherwise late
            // turns end up with impossible spawn densities.
            val amount = difficulty.enemiesPerWave
            val firstTurn = wave * 2 - 1
            val amountPerTurn = listOf((amount + 1) / 2, amount / 2).map { it.coerceAtMost(maxUnitsPerTurn) }
            amountPerTurn.forEachIndexed { turnOffset, turnAmount ->
                repeat(turnAmount) { index ->
                    val type = minionPool[random.nextInt(minionPool.size)]
                    spawns +=
                        EditorEnemySpawn(
                            attackerType = type,
                            level = enemyLevel,
                            spawnTurn = firstTurn + turnOffset,
                            spawnPoint = spawnPointFor(map, type, index),
                        )
                }
            }
            villainWaves[wave].orEmpty().forEach { villain ->
                spawns +=
                    EditorEnemySpawn(
                        attackerType = villain,
                        level = enemyLevel,
                        spawnTurn = firstTurn,
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
