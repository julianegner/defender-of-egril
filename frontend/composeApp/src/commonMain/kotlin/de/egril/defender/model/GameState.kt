package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.egril.defender.ui.settings.DifficultyLevel

enum class GamePhase {
    INITIAL_BUILDING, // Initial building phase - towers build instantly
    PLAYER_TURN, // Player can place/upgrade towers and attack
    ENEMY_TURN, // Enemies move
}

enum class FieldEffectType {
    FIREBALL, // Visual effect for wizard fireball area
    ACID, // Visual effect for alchemy acid with duration
    WEB, // Araxxa's spreading spider web area
}

enum class HealingEffectType {
    GREEN_WITCH, // Visual effect for green witch healing
}

/**
 * Spell targeting mode state
 */
data class SpellTargetingState(
    val activeSpell: SpellType,
    val validTargets: Set<Any> = emptySet(), // Can be Position, Attacker, or Defender depending on spell type
)

data class FieldEffect(
    val position: Position,
    val type: FieldEffectType,
    val damage: Int,
    var turnsRemaining: Int,
    val defenderId: Int, // Track which tower created this effect
    val attackerId: Int? = null, // For DOT effects, track which enemy has the effect
)

data class HealingEffect(
    val position: Position,
    val type: HealingEffectType,
    val healAmount: Int,
    val turnNumber: Int, // Track which turn this healing occurred for display timing
)

data class DamageEffect(
    val position: Position,
    val damageAmount: Int,
    val turnNumber: Int, // Track which turn this damage occurred for display timing
)

data class BombExplosionEffect(
    val center: Position, // Center of the explosion
    val affectedPositions: List<Position>, // All affected tile positions
    val turnNumber: Int, // Turn when this explosion occurred
)

data class EnemyDeathEffect(
    val position: Position, // Position where the enemy was defeated
    val turnNumber: Int, // Turn when this defeat occurred
    val attackerType: AttackerType, // Type of the defeated enemy (for ghost rendering during animation)
    val attackerLevel: Int, // Level of the defeated enemy (for level badge during animation)
)

data class CoinGainEffect(
    val position: Position, // Position of the defeated enemy that awarded coins
    val amount: Int, // Amount of coins gained
    val turnNumber: Int, // Turn when this coin gain occurred
)

data class TowerAttackEffect(
    val targetPosition: Position, // Position of the attacked tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class TowerConstructionEffect(
    val position: Position, // Position of the tower that finished building
    val turnNumber: Int, // Turn when construction completed
)

data class EnemySpawnEffect(
    val position: Position, // Spawn position of the newly appeared enemy
    val turnNumber: Int, // Turn when this spawn occurred
    val attackerType: AttackerType? = null, // Spawned enemy type (used to suppress specific spawn visuals)
    val suppressPortalAnimation: Boolean = false,
)

data class ScrapPile(
    val position: Position,
    val ownerAttackerId: Int,
    val hatchTurn: Int,
)

data class TrapTriggerEffect(
    val position: Position, // Position of the trap that was triggered
    val turnNumber: Int, // Turn when this trap triggered
)

data class EnemyMoveEffect(
    val position: Position, // Tile that the enemy just vacated (movement trail)
    val turnNumber: Int, // Turn when this movement occurred
)

data class DragonLevelChangeEffect(
    val position: Position, // Dragon's position when its level changed
    val isLevelUp: Boolean, // true = dragon gained levels (ate units), false = lost levels (took damage)
    val turnNumber: Int, // Turn when this change occurred
)

data class MineDigEffect(
    val position: Position, // Position of the mine that was dug
    val turnNumber: Int, // Turn when digging occurred
)

data class ArrowAttackEffect(
    val sourcePosition: Position, // Tower's tile (source of the arrow)
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class BallistaAttackEffect(
    val sourcePosition: Position, // Ballista tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class BowAttackEffect(
    val sourcePosition: Position, // Bow tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class SpearAttackEffect(
    val sourcePosition: Position, // Spear tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class PikeAttackEffect(
    val sourcePosition: Position, // Pike (spike) tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class WizardAttackEffect(
    val sourcePosition: Position, // Wizard tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class AlchemyAttackEffect(
    val sourcePosition: Position, // Alchemy tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class RocketAttackEffect(
    val sourcePosition: Position,
    val targetPosition: Position,
    val turnNumber: Int,
)

/**
 * Types of in-game event messages that are shown to the player.
 */
enum class GameMessageType {
    TARGET_TAKEN, // A SINGLE_HIT target was captured by an enemy
    GATE_DESTROYED, // A named gate barricade was destroyed
    EWHAD_ENTERS, // Ewhad has entered the battlefield
    EWHAD_RETREATS, // Ewhad has retreated (health reached 0, not final stand)
    EWHAD_DEFEATED, // Ewhad is defeated (health reached 0, final stand level)
    VILLAIN_ENTERS, // A villain has entered the battlefield (name = AttackerType.name)
    VILLAIN_DEFEATED, // A non-Ewhad villain was defeated (name = AttackerType.name)
    SILAS_MIRROR_HIT, // A tower struck Silas's illusion and was blinded
    COVEN_SWAP, // Sybilla swapped places with a witch
    STORY_INTRO, // Story narrative shown at the start of a level (name = editorLevelId)
    EVENT_MESSAGE, // Scripted-event story message (name = string-resource key of the predefined text)
}

/**
 * An in-game event message queued for display to the player.
 * @param type          The kind of event.
 * @param name          Optional name (target name or gate name); for [GameMessageType.EVENT_MESSAGE]
 *                      it is the optional string-resource key of the predefined text (may be null).
 * @param eventActions  For [GameMessageType.EVENT_MESSAGE]: the actions the event applied, so the
 *                      granted elements (coins, mana, supports, …) can be shown to the player.
* @param highlightPositions  Optional pair of positions to highlight (e.g., old and new position for coven swap).
*/
data class GameMessage(
    val type: GameMessageType,
    val name: String? = null,
    val eventActions: List<EventAction>? = null,
    val highlightPositions: Pair<Position, Position>? = null,
)

data class PendingSoulCall(
    val position: Position,
    val attackerType: AttackerType,
    val level: Int,
    val reviveTurn: Int,
    val dragonName: String? = null,
    val currentTarget: Position? = null,
)

data class GameState(
    var level: Level,
    val phase: MutableState<GamePhase> = mutableStateOf(GamePhase.INITIAL_BUILDING),
    val coins: MutableState<Int> = mutableStateOf(level.initialCoins),
    val healthPoints: MutableState<Int> = mutableStateOf(level.healthPoints),
    val defenders: SnapshotStateList<Defender> = mutableStateListOf(),
    val attackers: SnapshotStateList<Attacker> = mutableStateListOf(),
    val nextDefenderId: MutableState<Int> = mutableStateOf(1),
    val nextAttackerId: MutableState<Int> = mutableStateOf(1),
    val nextRaftId: MutableState<Int> = mutableStateOf(1),
    val nextBarricadeId: MutableState<Int> = mutableStateOf(1),
    val currentWaveIndex: MutableState<Int> = mutableStateOf(0),
    val spawnCounter: MutableState<Int> = mutableStateOf(0),
    val attackersToSpawn: SnapshotStateList<AttackerType> = mutableStateListOf(),
    val enemyTurnStartPositions: SnapshotStateMap<Int, Position> = mutableStateMapOf(), // Snapshot of enemy positions at start of enemy turn
    val turnNumber: MutableState<Int> = mutableStateOf(0),
    val actionsRemainingThisTurn: MutableState<Int> = mutableStateOf(0),
    val spawnPlan: List<PlannedEnemySpawn> = level.directSpawnPlan ?: generateSpawnPlan(level.attackerWaves),
    val fieldEffects: SnapshotStateList<FieldEffect> = mutableStateListOf(), // Track active field effects
    val healingEffects: SnapshotStateList<HealingEffect> = mutableStateListOf(), // Track active healing effects
    val damageEffects: SnapshotStateList<DamageEffect> = mutableStateListOf(), // Track barricade damage effects
    val traps: SnapshotStateList<Trap> = mutableStateListOf(), // Track active traps
    val barricades: SnapshotStateList<Barricade> = mutableStateListOf(), // Track active barricades
    val bridges: SnapshotStateList<Bridge> = mutableStateListOf(), // Track active bridges
    val rafts: SnapshotStateList<Raft> = mutableStateListOf(), // Track active rafts (towers on rivers)
    val bombExplosionEffects: SnapshotStateList<BombExplosionEffect> = mutableStateListOf(), // Track bomb explosion visual effects
    val defeatedEnemyEffects: SnapshotStateList<EnemyDeathEffect> = mutableStateListOf(), // Track enemy death visual effects
    val coinGainEffects: SnapshotStateList<CoinGainEffect> = mutableStateListOf(), // Track coin gain visual effects
    val pendingCoinGains: MutableState<Int> = mutableStateOf(0), // Coins earned this turn not yet credited (added by UI when coin animation plays; flushed by completeEnemyTurn as safety net)
    val towerAttackEffects: SnapshotStateList<TowerAttackEffect> = mutableStateListOf(), // Track tower attack impact visual effects
    val attackTriggerCount: MutableState<Int> = mutableStateOf(0), // Monotonically-increasing counter, incremented on every attack (bypasses per-tile deduplication)
    val constructionCompleteEffects: SnapshotStateList<TowerConstructionEffect> = mutableStateListOf(), // Track tower construction complete visual effects
    val enemySpawnEffects: SnapshotStateList<EnemySpawnEffect> = mutableStateListOf(), // Track enemy spawn portal visual effects
    val scrapPiles: SnapshotStateList<ScrapPile> = mutableStateListOf(), // Scrap-Bot wreckage markers waiting to hatch
    val trapTriggerEffects: SnapshotStateList<TrapTriggerEffect> = mutableStateListOf(), // Track trap trigger visual effects
    val enemyMoveEffects: SnapshotStateList<EnemyMoveEffect> = mutableStateListOf(), // Track enemy movement trail visual effects
    val dragonLevelChangeEffects: SnapshotStateList<DragonLevelChangeEffect> = mutableStateListOf(), // Track dragon level change visual effects
    val mineDigEffects: SnapshotStateList<MineDigEffect> = mutableStateListOf(), // Track dwarven mine digging visual effects
    val arrowAttackEffects: SnapshotStateList<ArrowAttackEffect> = mutableStateListOf(), // Track arrow/bolt projectile effects for Bow and Spear towers
    val ballistaAttackEffects: SnapshotStateList<BallistaAttackEffect> = mutableStateListOf(), // Track ballista projectile overlay effects
    val bowAttackEffects: SnapshotStateList<BowAttackEffect> = mutableStateListOf(), // Track bow arrow volley overlay effects
    val spearAttackEffects: SnapshotStateList<SpearAttackEffect> = mutableStateListOf(), // Track spear throw overlay effects
    val pikeAttackEffects: SnapshotStateList<PikeAttackEffect> = mutableStateListOf(), // Track pike extend overlay effects
    val wizardAttackEffects: SnapshotStateList<WizardAttackEffect> = mutableStateListOf(), // Track wizard fireball overlay effects
    val alchemyAttackEffects: SnapshotStateList<AlchemyAttackEffect> = mutableStateListOf(), // Track alchemy acid vial overlay effects
    val rocketAttackEffects: SnapshotStateList<RocketAttackEffect> = mutableStateListOf(), // Track Baron rocket projectile overlay effects
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM, // Track difficulty for this game session
    val tutorialState: MutableState<TutorialState> =
        mutableStateOf(
            // Enable tutorial only for the tutorial level (id=1, title contains "Welcome")
            if (level.id == 1 && level.name.contains("Welcome", ignoreCase = true)) {
                TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
            } else {
                TutorialState(isActive = false, currentStep = TutorialStep.NONE)
            },
        ),
    val infoState: MutableState<InfoState> = mutableStateOf(InfoState()), // Single tutorial infos system
    val destroyedMinePositions: SnapshotStateList<Position> = mutableStateListOf(), // Positions where mines have been destroyed
    val mineWarnings: SnapshotStateList<Int> = mutableStateListOf(), // Mine IDs with active warnings (dragon about to destroy)
    val xpEarnedThisLevel: MutableState<Int> = mutableStateOf(0), // XP earned during this level (awarded on completion; 20% on loss)
    val currentMana: MutableState<Int> = mutableStateOf(0), // Current mana (for spellcasting)
    val maxMana: MutableState<Int> = mutableStateOf(0), // Maximum mana (based on player stats)
    val activeSpellEffects: SnapshotStateList<ActiveSpellEffect> = mutableStateListOf(), // Active spell effects
    val incomeMultiplier: Double = 1.0, // Income multiplier from player stats (default 1.0, e.g. 1.2 for 20% bonus)
    val constructionLevel: Int = 0, // Construction level from player stats (0-3+, gates tower abilities)
    val spellTargeting: MutableState<SpellTargetingState?> = mutableStateOf(null), // Active spell targeting state (null when not targeting)
    val instantTowerSpellActive: MutableState<Boolean> = mutableStateOf(false), // True when Instant Tower spell is active (waiting for next tower placement)
    // Villains: set to true when any villain reaches a target. A villain breaching a target loses the
    // level immediately, regardless of remaining health points (see issue #538).
    val villainReachedTarget: MutableState<Boolean> = mutableStateOf(false),
    // SINGLE_HIT target tracking
    val takenTargets: SnapshotStateList<Position> = mutableStateListOf(), // Positions of taken SINGLE_HIT targets
    val pendingMessages: SnapshotStateList<GameMessage> = mutableStateListOf(), // Messages queued for display
    val pendingSoulCalls: SnapshotStateList<PendingSoulCall> = mutableStateListOf(), // Valerius resurrection queue for the next round
    // Player-usable supports remaining this level (placable objects + spell tokens)
    val supportObjectsRemaining: SnapshotStateMap<SupportObjectType, Int> = mutableStateMapOf(),
    val supportSpellsRemaining: SnapshotStateMap<SpellType, Int> = mutableStateMapOf(),
    // Cooldown-based support powers: turns remaining until the power can be used again (0 = ready)
    val cooldownPowerReadyIn: SnapshotStateMap<CooldownPowerType, Int> = mutableStateMapOf(),
    // True when the Coin Surge power is active this turn (doubles coins earned)
    val coinSurgeActive: MutableState<Boolean> = mutableStateOf(false),
    // Monotonically-increasing counter, incremented each time the "Sky is Falling" power is used,
    // to trigger the full-map falling-meteor animation overlay.
    val skyIsFallingTrigger: MutableState<Int> = mutableStateOf(0),
    // Scripted level event tracking
    val enemiesKilledTotal: MutableState<Int> = mutableStateOf(0), // Total enemies killed (by combat/traps, not those reaching the target)
    val enemiesKilledByType: SnapshotStateMap<AttackerType, Int> = mutableStateMapOf(), // Kills per enemy type
    val triggeredEventIds: SnapshotStateList<String> = mutableStateListOf(), // IDs of scripted events that have already fired
    // Sandbox: incremented whenever the map layout (tiles) is edited at runtime, so the map re-renders.
    val mapEditVersion: MutableState<Int> = mutableStateOf(0),
    // Sandbox: tiles repainted at runtime (position -> new type). Used to draw the new tile image as an
    // overlay over the original (possibly pre-rendered) map so edits are visible, and persisted in saves.
    val sandboxPaintedTiles: SnapshotStateMap<Position, de.egril.defender.editor.TileType> = mutableStateMapOf(),
    // Sandbox: flow direction/speed chosen for river tiles painted at runtime, so the chosen
    // water direction survives save/load. Only populated for positions painted as RIVER.
    val sandboxPaintedRiverTiles: SnapshotStateMap<Position, RiverTile> = mutableStateMapOf(),
) {
    // Sandbox: the original map tile type for every position, captured once from the level as it was
    // first loaded (before any runtime edits). Used so runtime paints can be compared against the
    // original map and only genuine differences are tracked, persisted, and overlaid.
    private val originalSandboxTileTypes: Map<Position, de.egril.defender.editor.TileType> =
        if (level.isSandbox) buildTileTypeMap(level) else emptyMap()
    private val originalSandboxRiverTiles: Map<Position, RiverTile> =
        if (level.isSandbox) level.riverTiles.toMap() else emptyMap()
    private val originalSandboxTargetInfoMap: Map<Position, TargetInfo> =
        if (level.isSandbox) level.targetInfoMap.toMap() else emptyMap()

    /** Multiplier applied to earned coins while the Coin Surge power is active (2x), otherwise 1x. */
    fun coinSurgeMultiplier(): Int = if (coinSurgeActive.value) 2 else 1

    /**
     * Sandbox: repaint a single map tile to the given [tileType] at runtime.
     * Rebuilds the level's tile collections and bumps [mapEditVersion] to trigger a re-render.
     * When painting a [de.egril.defender.editor.TileType.RIVER] tile, [riverFlow] and [riverSpeed]
     * set the water flow direction and speed (1 or 2).
     * Only tiles that differ from the original map are tracked in [sandboxPaintedTiles] (repainting a
     * tile back to its original type removes it), so only genuine differences are overlaid and saved.
     * Only allowed on sandbox levels; a no-op otherwise.
     */
    fun sandboxPaintTile(
        position: Position,
        tileType: de.egril.defender.editor.TileType,
        riverFlow: RiverFlow = RiverFlow.EAST,
        riverSpeed: Int = 1,
    ) {
        if (!level.isSandbox) return
        // Never repaint an occupied tile (defender/barricade/trap) to avoid orphaning game objects.
        if (defenders.any { it.position.value == position }) return
        if (barricades.any { it.position == position }) return

        val pathCells = level.pathCells.toMutableSet()
        val buildAreas = level.buildAreas.toMutableSet()
        val startPositions = level.startPositions.toMutableList()
        val targetPositions = level.targetPositions.toMutableList()
        val riverTiles = level.riverTiles.toMutableMap()
        val targetInfoMap = level.targetInfoMap.toMutableMap()

        // Clear the tile from every collection first so the new type fully replaces the old one.
        pathCells.remove(position)
        buildAreas.remove(position)
        startPositions.remove(position)
        targetPositions.remove(position)
        riverTiles.remove(position)
        targetInfoMap.remove(position)

        when (tileType) {
            de.egril.defender.editor.TileType.PATH -> pathCells.add(position)
            de.egril.defender.editor.TileType.BUILD_AREA -> buildAreas.add(position)
            de.egril.defender.editor.TileType.SPAWN_POINT -> if (!startPositions.contains(position)) startPositions.add(position)
            de.egril.defender.editor.TileType.TARGET -> {
                if (!targetPositions.contains(position)) {
                    targetPositions.add(position)
                }
                originalSandboxTargetInfoMap[position]?.let { originalTargetInfo ->
                    targetInfoMap[position] = originalTargetInfo
                }
            }
            de.egril.defender.editor.TileType.RIVER ->
                riverTiles[position] = RiverTile(position = position, flowDirection = riverFlow, flowSpeed = riverSpeed)
            de.egril.defender.editor.TileType.NO_PLAY -> {} // Already cleared from all collections.
        }

        level =
            level.copy(
                pathCells = pathCells.toSet(),
                buildAreas = buildAreas.toSet(),
                startPositions = startPositions.toList(),
                targetPositions = targetPositions.toList(),
                riverTiles = riverTiles.toMap(),
                targetInfoMap = targetInfoMap.toMap(),
            )
        // Record the repaint so the map can overlay the new tile image over the original map
        // background — but only when it genuinely differs from the original map. Repainting a tile
        // back to its original type removes it from the tracked differences.
        val originalType = originalSandboxTileTypes[position] ?: de.egril.defender.editor.TileType.NO_PLAY
        val originalRiverTile = originalSandboxRiverTiles[position]
        val isSameAsOriginal =
            if (tileType == de.egril.defender.editor.TileType.RIVER) {
                originalType == de.egril.defender.editor.TileType.RIVER &&
                    originalRiverTile != null &&
                    originalRiverTile.flowDirection == riverFlow &&
                    originalRiverTile.flowSpeed == riverSpeed
            } else if (tileType == de.egril.defender.editor.TileType.TARGET) {
                tileType == originalType && targetInfoMap[position] == originalSandboxTargetInfoMap[position]
            } else {
                tileType == originalType
            }
        if (isSameAsOriginal) {
            sandboxPaintedTiles.remove(position)
        } else {
            sandboxPaintedTiles[position] = tileType
        }
        // Track the chosen river flow separately so it can be persisted and restored across saves.
        if (tileType == de.egril.defender.editor.TileType.RIVER) {
            val paintedRiverTile = RiverTile(position = position, flowDirection = riverFlow, flowSpeed = riverSpeed)
            if (originalType == de.egril.defender.editor.TileType.RIVER && originalRiverTile == paintedRiverTile) {
                sandboxPaintedRiverTiles.remove(position)
            } else {
                sandboxPaintedRiverTiles[position] = paintedRiverTile
            }
        } else {
            sandboxPaintedRiverTiles.remove(position)
        }
        mapEditVersion.value++
    }

    /**
     * Build a position -> [de.egril.defender.editor.TileType] map for every non-blocked tile in [lvl].
     * Positions absent from the map are implicitly [de.egril.defender.editor.TileType.NO_PLAY].
     */
    private fun buildTileTypeMap(lvl: Level): Map<Position, de.egril.defender.editor.TileType> {
        val map = mutableMapOf<Position, de.egril.defender.editor.TileType>()
        lvl.pathCells.forEach { map[it] = de.egril.defender.editor.TileType.PATH }
        lvl.buildAreas.forEach { map[it] = de.egril.defender.editor.TileType.BUILD_AREA }
        lvl.startPositions.forEach { map[it] = de.egril.defender.editor.TileType.SPAWN_POINT }
        lvl.targetPositions.forEach { map[it] = de.egril.defender.editor.TileType.TARGET }
        lvl.riverTiles.keys.forEach { map[it] = de.egril.defender.editor.TileType.RIVER }
        return map
    }

    fun isLevelWon(): Boolean {
        // Sandbox levels can never be won, even when all enemies are gone.
        if (level.isSandbox) return false
        // Check if all planned spawns have occurred and all enemies are defeated
        val allSpawned = spawnPlan.all { it.spawnTurn <= turnNumber.value }
        return allSpawned && attackers.all { it.isDefeated.value }
    }

    fun isLevelLost(): Boolean {
        if (healthPoints.value <= 0) return true
        // A villain breaching a target loses the level immediately, regardless of remaining health.
        if (villainReachedTarget.value) return true
        // Level is also lost when all SINGLE_HIT targets have been taken
        val singleHitTargets = level.targetInfoMap.filter { it.value.type == TargetType.SINGLE_HIT }.keys
        if (singleHitTargets.isNotEmpty() && takenTargets.containsAll(singleHitTargets)) return true
        return false
    }

    /**
     * Total worst-case health-point damage the player can still take, assuming every remaining
     * enemy (both those alive on the field and those still to spawn) reaches the target unhindered.
     * Uses [Long] so summoner/boss "all HP" markers ([Int.MAX_VALUE]) can be summed without overflow.
     */
    fun getRemainingEnemyThreat(): Long {
        var total = 0L
        for (attacker in attackers) {
            if (attacker.isDefeated.value) continue
            total += attackerTargetDamage(attacker.type, attacker.level.value).toLong()
        }
        for (spawn in spawnPlan) {
            if (spawn.spawnTurn > turnNumber.value) {
                total += attackerTargetDamage(spawn.attackerType, spawn.level).toLong()
            }
        }
        return total
    }

    /**
     * Returns true when the level is guaranteed to be won: even if every remaining enemy reached the
     * target, the player would still have health points left. Used to offer an instant "Win Level now".
     *
     * Excluded cases where a win cannot be guaranteed:
     *  - Not during the player's turn (e.g. building phase or enemy turn).
     *  - Levels with SINGLE_HIT targets, which can be lost regardless of remaining health.
     *  - When a summoner enemy remains, since it can create an unbounded number of additional units.
     *  - When a villain remains (on the field or still to spawn), since a villain reaching a target
     *    loses the level outright, regardless of remaining health.
     */
    fun canWinLevelNow(): Boolean {
        // Sandbox levels can never be won, so never offer the instant win.
        if (level.isSandbox) return false
        if (phase.value != GamePhase.PLAYER_TURN) return false
        if (level.targetInfoMap.any { it.value.type == TargetType.SINGLE_HIT }) return false
        if (isLevelLost() || isLevelWon()) return false

        val aliveEnemies = attackers.filter { !it.isDefeated.value }
        val enemiesToSpawn = spawnPlan.filter { it.spawnTurn > turnNumber.value }
        // There must be at least one remaining enemy (otherwise the level is already won).
        if (aliveEnemies.isEmpty() && enemiesToSpawn.isEmpty()) return false
        // Summoners can create additional enemies, so the total threat cannot be bounded.
        if (aliveEnemies.any { it.type.isSummoner() } || enemiesToSpawn.any { it.attackerType.isSummoner() }) return false
        // A villain (on the field or still to spawn) loses the level the moment it reaches a target,
        // regardless of remaining health, so a guaranteed win can never be offered while one remains.
        if (aliveEnemies.any { it.type.isRealVillain } || enemiesToSpawn.any { it.attackerType.isRealVillain }) return false

        return getRemainingEnemyThreat() < healthPoints.value.toLong()
    }

    /**
     * Returns true if [position] is a target that can still be reached by enemies.
     * Taken SINGLE_HIT targets are excluded.
     */
    fun isActiveTargetPosition(position: Position): Boolean = level.isTargetPosition(position) && !takenTargets.contains(position)

    /**
     * Returns the active (non-taken) target positions.
     */
    fun getActiveTargetPositions(): List<Position> = level.targetPositions.filter { !takenTargets.contains(it) }

    /**
     * When a SINGLE_HIT target at [takenPosition] is taken, redirect all enemies
     * whose currentTarget points to that position towards the nearest remaining active target.
     */
    fun retargetEnemiesFromTakenTarget(takenPosition: Position) {
        val remaining = getActiveTargetPositions()
        if (remaining.isEmpty()) return // No active targets left – level will be lost
        for (enemy in attackers) {
            if (enemy.isDefeated.value) continue
            if (enemy.currentTarget?.value == takenPosition) {
                val newTarget = remaining.minByOrNull { enemy.position.value.distanceTo(it) } ?: remaining.first()
                enemy.currentTarget.value = newTarget
                println("Enemy ${enemy.id} (${enemy.type}) retargeted from $takenPosition to $newTarget")
            }
        }
    }

    /**
     * Returns the effective next waypoint target, redirecting to the nearest active target
     * if the waypoint's next target is a taken SINGLE_HIT target.
     */
    fun resolveWaypointNextTarget(
        waypointNextTarget: Position,
        from: Position,
    ): Position =
        if (takenTargets.contains(waypointNextTarget)) {
            getActiveTargetPositions().minByOrNull { from.distanceTo(it) } ?: waypointNextTarget
        } else {
            waypointNextTarget
        }

    fun canPlaceDefender(type: DefenderType): Boolean = (level.isSandbox || coins.value >= type.baseCost) && level.availableTowers.contains(type)

    fun canUpgradeDefender(defender: Defender): Boolean = level.isSandbox || coins.value >= defender.upgradeCost

    fun hasActionsRemaining(): Boolean = actionsRemainingThisTurn.value > 0

    fun getRemainingEnemyCount(): Int {
        val totalSpawned = this.nextAttackerId.value - 1
        val plannedSpawns = this.spawnPlan.drop(totalSpawned)
        return plannedSpawns.size
    }

    fun getActiveEnemyCount(): Int {
        // Count only non-defeated enemies that are NOT building bridges
        return this.attackers.count { !it.isDefeated.value && !it.isBuildingBridge.value }
    }

    /**
     * Check if a position is covered by any active bridge
     */
    fun isBridgeAt(position: Position): Boolean =
        bridges.any { bridge ->
            bridge.isActive && bridge.coversPosition(position)
        }

    /**
     * Get the bridge at a position, if any
     */
    fun getBridgeAt(position: Position): Bridge? =
        bridges.find { bridge ->
            bridge.isActive && bridge.coversPosition(position)
        }

    /**
     * Check if a position has a raft
     */
    fun isRaftAt(position: Position): Boolean =
        rafts.any { raft ->
            raft.isActive && raft.currentPosition.value == position
        }

    /**
     * Get the raft at a position, if any
     */
    fun getRaftAt(position: Position): Raft? =
        rafts.find { raft ->
            raft.isActive && raft.currentPosition.value == position
        }

    /**
     * Effective attack range for a defender, accounting for the DOUBLE_TOWER_REACH spell buff
     * (whether granted by a mana spell or a spell-token support). This must be used everywhere
     * range is evaluated for attacking so the buff behaves consistently in the UI and combat.
     */
    fun effectiveRange(defender: Defender): Int {
        val hasDoubleReachBuff =
            activeSpellEffects.any {
                it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == defender.id
            }
        return if (hasDoubleReachBuff) defender.range * 2 else defender.range
    }

    /**
     * Check if there are defenders with unused action points and enemies in range
     * Used to show end turn confirmation dialog
     */
    fun hasDefendersWithUnusedActions(): Boolean {
        // Get active attackers (not defeated, not building bridges)
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }

        return defenders.any { defender ->
            if (!defender.isReady ||
                defender.actionsRemaining.value <= 0 ||
                defender.isDisabled.value
            ) {
                return@any false
            }

            // Special handling for different tower types
            when (defender.type) {
                DefenderType.DWARVEN_MINE -> {
                    // Mines always count as having unused actions (digging)
                    true
                }
                else -> {
                    // Only count attack towers if they have AttackType and enemies in range
                    if (defender.type.attackType == AttackType.NONE) {
                        false
                    } else {
                        // Check if there are any enemies in range
                        activeAttackers.any { attacker -> defender.canAttack(attacker, effectiveRange(defender)) }
                    }
                }
            }
        }
    }

    /**
     * Get defenders that can be tabbed to: have action points left and either are mines
     * or have at least one enemy within attack range. Sorted by position (top-to-bottom,
     * left-to-right) for deterministic Tab cycling.
     */
    fun getActionableTowersForTab(): List<Defender> {
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        return defenders
            .filter { defender ->
                if (!defender.isReady ||
                    defender.actionsRemaining.value <= 0 ||
                    defender.isDisabled.value
                ) {
                    return@filter false
                }
                when (defender.type) {
                    DefenderType.DWARVEN_MINE -> true
                    else -> {
                        if (defender.type.attackType == AttackType.NONE) {
                            false
                        } else {
                            activeAttackers.any { attacker -> defender.canAttack(attacker, effectiveRange(defender)) }
                        }
                    }
                }
            }.sortedWith(compareBy({ it.position.value.y }, { it.position.value.x }))
    }

    /**
     * Check if there are defenders that can perform auto-attacks.
     * Returns true if there are defenders with actions that can be automated (regular attacks).
     * Excludes special actions like mines, traps, and alchemy towers.
     */
    fun hasDefendersForAutoAttack(): Boolean {
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        if (activeAttackers.isEmpty()) return false

        return defenders.any { defender ->
            if (!defender.isReady ||
                defender.actionsRemaining.value <= 0 ||
                defender.isDisabled.value
            ) {
                return@any false
            }

            // Only count towers that can do regular auto-attacks
            // Exclude mines (no attack) and wizard towers level 10+ (have trap ability that needs manual placement)
            // Alchemy towers CAN auto-attack (they check acid immunity like wizard towers check fireball immunity)
            when {
                defender.type == DefenderType.DWARVEN_MINE -> false
                defender.type == DefenderType.WIZARD_TOWER && defender.level.value >= 10 -> false
                defender.type.attackType == AttackType.NONE -> false
                else -> {
                    // Check if there are any enemies in range
                    activeAttackers.any { attacker -> defender.canAttack(attacker, effectiveRange(defender)) }
                }
            }
        }
    }

    /**
     * Check if there are defenders with special actions that cannot be automated effectively.
     * Returns a list of defender types that have remaining special actions.
     */
    fun getDefenderTypesWithSpecialActions(): List<DefenderType> {
        val typesWithActions = mutableSetOf<DefenderType>()
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }

        defenders.forEach { defender ->
            if (!defender.isReady || defender.actionsRemaining.value <= 0 || defender.isDisabled.value) {
                return@forEach
            }

            when {
                // Dwarven mines with digging actions
                defender.type == DefenderType.DWARVEN_MINE -> {
                    typesWithActions.add(DefenderType.DWARVEN_MINE)
                }
                // Alchemy towers with lasting attacks only when no enemies in range
                // (if enemies are in range, they will auto-attack like normal towers)
                defender.type == DefenderType.ALCHEMY_TOWER -> {
                    val hasEnemiesInRange = activeAttackers.any { attacker -> defender.canAttack(attacker, effectiveRange(defender)) }
                    if (!hasEnemiesInRange) {
                        typesWithActions.add(DefenderType.ALCHEMY_TOWER)
                    }
                }
                // Wizard towers (level 10+) with magical trap available
                defender.type == DefenderType.WIZARD_TOWER && defender.level.value >= 10 -> {
                    if (defender.trapCooldownRemaining.value == 0) {
                        typesWithActions.add(DefenderType.WIZARD_TOWER)
                    }
                }
            }
        }

        return typesWithActions.toList()
    }

    /**
     * Initialize pre-placed defenders, attackers, traps, and barricades from level configuration.
     * This should be called right after GameState creation to set up the initial level state.
     */
    fun initializePrePlacedElements() {
        // Get initial data using the helper method that handles both old and new formats
        val initialData = level.getEffectiveInitialData()

        // Initialize player-usable supports (placable objects + spell tokens) for this level
        supportObjectsRemaining.clear()
        for (supportObject in level.supports.objects) {
            supportObjectsRemaining[supportObject.type] =
                combineSupportCounts(supportObjectsRemaining[supportObject.type] ?: 0, supportObject.count)
        }
        supportSpellsRemaining.clear()
        for (supportSpell in level.supports.spells) {
            supportSpellsRemaining[supportSpell.spell] =
                combineSupportCounts(supportSpellsRemaining[supportSpell.spell] ?: 0, supportSpell.count)
        }

        // Initialize cooldown-based support powers. Powers that start active are immediately usable
        // (readyIn = 0); powers that start inactive begin on cooldown.
        cooldownPowerReadyIn.clear()
        for (power in level.supports.cooldownPowers) {
            cooldownPowerReadyIn[power.type] = if (power.startActive) 0 else power.cooldownTurns
        }
        coinSurgeActive.value = false

        // Place initial barricades FIRST (before defenders so we can link them)
        for (initialBarricade in initialData.barricades) {
            val barricade =
                Barricade(
                    id = nextBarricadeId.value++,
                    position = initialBarricade.position,
                    healthPoints = mutableStateOf(initialBarricade.healthPoints),
                    defenderId = 0, // Pre-placed barricades don't belong to any specific defender
                    isGate = initialBarricade.isGate,
                    name = initialBarricade.name,
                )
            barricades.add(barricade)
        }

        // Place initial defenders
        for (initialDefender in initialData.defenders) {
            val defender =
                Defender(
                    id = nextDefenderId.value,
                    type = initialDefender.type,
                    position = mutableStateOf(initialDefender.position),
                    placedOnTurn = 0, // Placed before the game starts
                    dragonName = initialDefender.dragonName,
                )
            defender.level.value = initialDefender.level
            defender.buildTimeRemaining.value = 0 // Already built
            defender.actionsRemaining.value = 0 // No actions in initial phase

            // If this defender should be on a tower base, find the barricade at the same position
            if (initialDefender.onTowerBase) {
                val barricadeAtPosition = barricades.find { it.position == initialDefender.position }
                if (barricadeAtPosition != null && barricadeAtPosition.canSupportTower()) {
                    defender.towerBaseBarricadeId.value = barricadeAtPosition.id
                    barricadeAtPosition.supportedTowerId.value = defender.id
                }
            }

            defenders.add(defender)
            nextDefenderId.value++
        }

        // Place initial attackers
        for (initialAttacker in initialData.attackers) {
            val attacker =
                Attacker(
                    id = nextAttackerId.value,
                    type = initialAttacker.type,
                    position = mutableStateOf(initialAttacker.position),
                    level = mutableStateOf(initialAttacker.level),
                    dragonName = initialAttacker.dragonName,
                )
            // Set custom health if specified, otherwise use default for level
            val health = initialAttacker.currentHealth ?: (initialAttacker.type.health * initialAttacker.level)
            attacker.currentHealth.value = health
            attacker.isDefeated.value = false
            attackers.add(attacker)
            nextAttackerId.value++
        }

        // Place initial traps
        for (initialTrap in initialData.traps) {
            val trapType =
                try {
                    TrapType.valueOf(initialTrap.type)
                } catch (e: Exception) {
                    TrapType.DWARVEN
                }
            // We need a defender ID for the trap, but there may not be one
            // Use defenderId = 0 to indicate it's a pre-placed trap
            val trap =
                Trap(
                    position = initialTrap.position,
                    damage = initialTrap.damage,
                    defenderId = 0, // Pre-placed traps don't belong to any specific defender
                    type = trapType,
                )
            traps.add(trap)
        }
    }
}
