package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
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
    BURNING_TILE, // Ignis-Va death: burning ground that disables nearby towers
    SHADOW_FOG, // Morvath's shadow veil that hides tile information
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

data class SnotlingCannonThrowEffect(
    val sourcePosition: Position,
    val targetPosition: Position,
    val thrownCount: Int,
    val turnNumber: Int,
)

data class GarokkWarCryEffect(
    val position: Position,
    val turnNumber: Int,
)

data class ShadowSpewEffect(
    val sourcePosition: Position, // Xarithon's tile
    val targetPosition: Position, // Center of the 2×2 target area
    val turnNumber: Int, // Turn when Shadow Spew was activated
)

data class MorvathShadowOrbEffect(
    val sourcePosition: Position, // Morvath's tile
    val targetPosition: Position, // Distant fog tile being added
    val turnNumber: Int, // Turn when the orb was launched
    val attackerId: Int, // Morvath's attacker id
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
    WAAAGH_FRENZY, // The horde has entered a Waaagh! frenzy
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
 * @param eventMessageFrame For [GameMessageType.EVENT_MESSAGE]: the visual frame configured for the
 *                      message in the level editor (see [EventMessageFrameId]); null = standard frame.
* @param highlightPositions  Optional pair of positions to highlight (e.g., old and new position for coven swap).
*/
data class GameMessage(
    val type: GameMessageType,
    val name: String? = null,
    val eventActions: List<EventAction>? = null,
    val eventMessageFrame: EventMessageFrameId? = null,
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

/**
 * Pending barge (raft + defender) deletion from Roderich's Broadside attack.
 * The barge is removed after the cannonball animation completes.
 */
data class PendingBargeDeletion(
    val raftId: Int,
    val defenderId: Int,
    val towerCost: Int, // Cost to add to Roderich's treasure
    val bargePosition: Position, // For logging
)

data class PendingSnotlingCannonArrival(
    val targetPosition: Position,
    val thrownCount: Int,
    val turnNumber: Int,
)

/**
 * Bridge damage that is intentionally deferred until the tower attack animation has fully resolved.
 * This avoids reducing bridge health before the projectile/impact visual has finished.
 */
data class PendingBridgeDamage(
    val position: Position,
    val damage: Int,
)

enum class AutoAttackAvailability {
    NONE,
    ATTACK,
    MANA_ONLY,
}

// Marked @Stable so Compose trusts referential/structural equality on this class instead of
// treating it as unstable (which the `var level` property would otherwise force). GameState is
// mutated exclusively through its Compose-observable fields (MutableState/SnapshotStateList); the
// `level` property is set once at construction and never reassigned during gameplay, so this
// annotation does not violate the @Stable contract in practice.
//
// Without this, every composable that takes a GameState parameter (e.g. GridCell, called once per
// map tile) can never be skipped by Compose's recomposition optimizer, forcing a full recompute of
// every visible tile on every single state change (attacker move, effect update, etc.) — this can
// become a severe performance problem on large, mostly-open maps (e.g. Flotsam City's 80x80 grid).
@Stable
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
    val nextBridgeId: MutableState<Int> = mutableStateOf(1),
    val nextPortalId: MutableState<Int> = mutableStateOf(1),
    val currentWaveIndex: MutableState<Int> = mutableStateOf(0),
    val spawnCounter: MutableState<Int> = mutableStateOf(0),
    val attackersToSpawn: SnapshotStateList<AttackerType> = mutableStateListOf(),
    val enemyTurnStartPositions: SnapshotStateMap<Int, Position> = mutableStateMapOf(), // Snapshot of enemy positions at start of enemy turn
    val turnNumber: MutableState<Int> = mutableStateOf(0),
    val actionsRemainingThisTurn: MutableState<Int> = mutableStateOf(0),
    val spawnPlan: List<PlannedEnemySpawn> = level.directSpawnPlan ?: generateSpawnPlan(level.attackerWaves),
    // Spawn loops (issue #694). When [spawnGroups] is non-null it drives spawning instead of
    // [spawnPlan]; [spawnGroupCursor] tracks the active group/iteration and [spawnGroupBindings]
    // maps a group's logical unit ids to the actual spawned attacker ids (for UNIT_ALIVE loops).
    val spawnGroups: List<SpawnSequenceEntry>? = level.spawnGroups,
    val spawnGroupCursor: MutableState<SpawnGroupCursor> = mutableStateOf(SpawnGroupCursor()),
    val spawnGroupBindings: SnapshotStateMap<String, Int> = mutableStateMapOf(),
    // Spawn loops ended by a STOP_SPAWN_LOOP event action; they are skipped and never repeat again.
    val stoppedSpawnLoops: SnapshotStateList<String> = mutableStateListOf(),
    val fieldEffects: SnapshotStateList<FieldEffect> = mutableStateListOf(), // Track active field effects
    val healingEffects: SnapshotStateList<HealingEffect> = mutableStateListOf(), // Track active healing effects
    val damageEffects: SnapshotStateList<DamageEffect> = mutableStateListOf(), // Track barricade damage effects
    val traps: SnapshotStateList<Trap> = mutableStateListOf(), // Track active traps
    val barricades: SnapshotStateList<Barricade> = mutableStateListOf(), // Track active barricades
    val fiefs: SnapshotStateList<Fief> = mutableStateListOf(), // Track active fiefs (income-generating path objects)
    val mushrooms: SnapshotStateList<Mushroom> = mutableStateListOf(), // Track active mushrooms
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
    val activePortals: SnapshotStateList<Portal> = mutableStateListOf(), // Rift portals created by Zythar's demonlings
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
    val snotlingCannonThrowEffects: SnapshotStateList<SnotlingCannonThrowEffect> = mutableStateListOf(), // Track Snotling cannon throw projectile effects
    val garokkWarCryEffects: SnapshotStateList<GarokkWarCryEffect> = mutableStateListOf(), // Track Garokk's war cry pulse effect
    val shadowSpewEffects: SnapshotStateList<ShadowSpewEffect> = mutableStateListOf(), // Track Xarithon shadow spew flying fireball effects
    val morvathShadowOrbEffects: SnapshotStateList<MorvathShadowOrbEffect> = mutableStateListOf(), // Track Morvath's shadow orb flying to distant fog tile
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
    val runes: MutableState<Int> = mutableStateOf(0), // Runes recovered from defeated Runemasters
    val scriptedVictory: MutableState<Boolean> = mutableStateOf(false),
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
    val waaghPoints: MutableState<Int> = mutableStateOf(0), // Current Waaagh! meter (0-100)
    val waaghFrenzyActive: MutableState<Boolean> = mutableStateOf(false), // True while Waaagh! frenzy is active
    val waaghFrenzyRoundsLeft: MutableState<Int> = mutableStateOf(0), // Enemy turns remaining in the current frenzy
    val hasShownWaaghFrenzyMessage: MutableState<Boolean> = mutableStateOf(false), // True once the frenzy intro narrative was shown
    val pendingSoulCalls: SnapshotStateList<PendingSoulCall> = mutableStateListOf(), // Valerius resurrection queue for the next round
    val pendingBargeDeletions: SnapshotStateList<PendingBargeDeletion> = mutableStateListOf(), // Barges (rafts + defenders) to be deleted after animation completes
    val pendingSnotlingCannonArrivals: SnapshotStateList<PendingSnotlingCannonArrival> = mutableStateListOf(), // Snotlings arriving at their landing tile after the cannonball animation completes
    val pendingBridgeDamage: SnapshotStateList<PendingBridgeDamage> = mutableStateListOf(), // Bridge HP reductions deferred until tower attack visuals finish
    // Player-usable supports remaining this level (placable objects + spell tokens + fief tokens)
    val supportObjectsRemaining: SnapshotStateMap<SupportObjectType, Int> = mutableStateMapOf(),
    val supportSpellsRemaining: SnapshotStateMap<SpellType, Int> = mutableStateMapOf(),
    val supportFiefRemaining: SnapshotStateMap<FiefType, Int> = mutableStateMapOf(),
    // Cooldown-based support powers: turns remaining until the power can be used again (0 = ready)
    val cooldownPowerReadyIn: SnapshotStateMap<CooldownPowerType, Int> = mutableStateMapOf(),
    // True when the Coin Surge power is active this turn (doubles coins earned)
    val coinSurgeActive: MutableState<Boolean> = mutableStateOf(false),
    // Monotonically-increasing counter, incremented each time the "Sky is Falling" power is used,
    // to trigger the full-map falling-meteor animation overlay.
    val skyIsFallingTrigger: MutableState<Int> = mutableStateOf(0),
    // Tile-scoped one-shot animations that have already been shown for the current save state.
    // Prevents replay when a tile is temporarily removed from composition (e.g. viewport culling)
    // and later composed again while the same visual effect is still present.
    val playedTileAnimationKeys: SnapshotStateMap<String, Boolean> = mutableStateMapOf(),
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
    // Tile zones (see [TileZone]) currently active, in activation order. Switched by scripted events.
    val activeTileZoneIds: SnapshotStateList<String> = mutableStateListOf(),
    // Tiles whose type currently differs from the original map because of an active tile zone
    // (position -> current type). Used to overlay the new tile image over a pre-rendered map.
    val zonePaintedTiles: SnapshotStateMap<Position, de.egril.defender.editor.TileType> = mutableStateMapOf(),
    // River flow of zone-painted river tiles (only populated for positions currently RIVER due to a zone).
    val zonePaintedRiverTiles: SnapshotStateMap<Position, RiverTile> = mutableStateMapOf(),
    // Units that sank to the river bed when their tile was flooded (see [AttackerType.survivesSubmersion]).
    // They are kept out of [attackers] so they can neither act nor be targeted, and do not count
    // towards winning the level. They re-surface once their tile is dry again.
    val submergedAttackers: SnapshotStateList<Attacker> = mutableStateListOf(),
    // Running loops of scripted events (see [EventLoop]).
    val activeEventLoops: SnapshotStateList<ActiveEventLoop> = mutableStateListOf(),
    val activeEventMapImages: SnapshotStateList<EventMapImage> = mutableStateListOf(),
) {
    // The original map tile type / river flow for every position, captured once from the level as
    // it was first loaded (before any runtime edits). Absent positions are NO_PLAY.
    private val originalTileTypes: Map<Position, de.egril.defender.editor.TileType> by lazy { buildTileTypeMap(originalLevel) }
    private val originalRiverTiles: Map<Position, RiverTile> by lazy { originalLevel.riverTiles.toMap() }
    private val originalLevel: Level = level

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

        replaceTiles(mapOf(position to (tileType to RiverTile(position = position, flowDirection = riverFlow, flowSpeed = riverSpeed))))
        val targetInfoMap = level.targetInfoMap
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
     * Replace the type of several map tiles at once with a single rebuild of the level's tile
     * collections. Each entry maps a position to its new type and, for
     * [de.egril.defender.editor.TileType.RIVER], the river flow to use (ignored for other types).
     * Target tiles regain their original target metadata. Does not bump [mapEditVersion].
     */
    fun replaceTiles(changes: Map<Position, Pair<de.egril.defender.editor.TileType, RiverTile?>>) {
        if (changes.isEmpty()) return
        val pathCells = level.pathCells.toMutableSet()
        val buildAreas = level.buildAreas.toMutableSet()
        val startPositions = level.startPositions.toMutableList()
        val targetPositions = level.targetPositions.toMutableList()
        val riverTiles = level.riverTiles.toMutableMap()
        val targetInfoMap = level.targetInfoMap.toMutableMap()

        for ((position, change) in changes) {
            val (tileType, riverTile) = change
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
                    originalLevel.targetInfoMap[position]?.let { originalTargetInfo ->
                        targetInfoMap[position] = originalTargetInfo
                    }
                }
                de.egril.defender.editor.TileType.RIVER ->
                    riverTiles[position] = (riverTile ?: RiverTile(position = position)).copy(position = position)
                de.egril.defender.editor.TileType.NO_PLAY -> {} // Already cleared from all collections.
            }
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
    }

    /** The tile type [position] has on the original (unmodified) map. */
    fun originalTileTypeAt(position: Position): de.egril.defender.editor.TileType = originalTileTypes[position] ?: de.egril.defender.editor.TileType.NO_PLAY

    /** The river flow [position] has on the original (unmodified) map, if it is a river tile there. */
    fun originalRiverTileAt(position: Position): RiverTile? = originalRiverTiles[position]

    /** The tile type [position] currently has (reflecting runtime edits). */
    fun currentTileTypeAt(position: Position): de.egril.defender.editor.TileType =
        when {
            level.riverTiles.containsKey(position) -> de.egril.defender.editor.TileType.RIVER
            level.startPositions.contains(position) -> de.egril.defender.editor.TileType.SPAWN_POINT
            level.targetPositions.contains(position) -> de.egril.defender.editor.TileType.TARGET
            level.pathCells.contains(position) -> de.egril.defender.editor.TileType.PATH
            level.buildAreas.contains(position) -> de.egril.defender.editor.TileType.BUILD_AREA
            else -> de.egril.defender.editor.TileType.NO_PLAY
        }

    /**
     * The tile type a runtime edit shows on [position] instead of the original map (sandbox paint or
     * active tile zone), or null when the tile looks like the original map.
     */
    fun paintedTileTypeAt(position: Position): de.egril.defender.editor.TileType? = (if (level.isSandbox) sandboxPaintedTiles[position] else null) ?: zonePaintedTiles[position]

    /** River flow of a runtime-painted river tile at [position] (see [paintedTileTypeAt]). */
    fun paintedRiverTileAt(position: Position): RiverTile? = (if (level.isSandbox) sandboxPaintedRiverTiles[position] else null) ?: zonePaintedRiverTiles[position]

    /**
     * Recompute the tile types resulting from the currently active tile zones for [positions] and
     * write them into the level. Later-activated zones take precedence over earlier ones; tiles not
     * covered by any active zone return to the original map. Updates [zonePaintedTiles] /
     * [zonePaintedRiverTiles] and bumps [mapEditVersion].
     *
     * @return the positions whose tile type actually changed, mapped to (old type, new type).
     */
    fun refreshZoneTiles(
        positions: Collection<Position>,
    ): Map<Position, Pair<de.egril.defender.editor.TileType, de.egril.defender.editor.TileType>> {
        val activeZones = activeTileZoneIds.mapNotNull { id -> level.tileZones.firstOrNull { it.id == id } }
        val changes = mutableMapOf<Position, Pair<de.egril.defender.editor.TileType, RiverTile?>>()
        val typeChanges = mutableMapOf<Position, Pair<de.egril.defender.editor.TileType, de.egril.defender.editor.TileType>>()
        for (position in positions.toSet()) {
            val zone = activeZones.lastOrNull { it.tiles.containsKey(position) }
            val newType = zone?.tiles?.get(position) ?: originalTileTypeAt(position)
            val newRiver =
                if (newType == de.egril.defender.editor.TileType.RIVER) {
                    zone?.riverTiles?.get(position) ?: originalRiverTileAt(position) ?: RiverTile(position = position)
                } else {
                    null
                }
            val oldType = currentTileTypeAt(position)
            val oldRiver = level.riverTiles[position]
            if (oldType != newType || oldRiver != newRiver?.copy(position = position)) {
                changes[position] = newType to newRiver
            }
            if (oldType != newType) {
                typeChanges[position] = oldType to newType
            }
            if (zone != null && (newType != originalTileTypeAt(position) || newRiver != originalRiverTileAt(position))) {
                zonePaintedTiles[position] = newType
                if (newRiver != null) zonePaintedRiverTiles[position] = newRiver.copy(position = position) else zonePaintedRiverTiles.remove(position)
            } else {
                zonePaintedTiles.remove(position)
                zonePaintedRiverTiles.remove(position)
            }
        }
        replaceTiles(changes)
        if (changes.isNotEmpty()) mapEditVersion.value++
        return typeChanges
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

    private fun AttackerType.countsAsHordeForWaagh(): Boolean = faction == EnemyFaction.HORDE || unitSize > 0

    val hasHordeUnitsInLevel: Boolean
        get() =
            spawnPlan.any { it.attackerType.countsAsHordeForWaagh() } ||
                (spawnGroups?.allSpawnEntries()?.any { it.attackerType.countsAsHordeForWaagh() } ?: false) ||
                level.getEffectiveInitialData().attackers.any { it.type.countsAsHordeForWaagh() } ||
                attackers.any { !it.isDefeated.value && it.type.countsAsHordeForWaagh() }

    fun addWaaghPoints(amount: Int) {
        if (amount <= 0 || !level.waaghEnabled) return
        waaghPoints.value = (waaghPoints.value + amount).coerceAtMost(100)
    }

    fun isLevelWon(): Boolean {
        // Sandbox levels can never be won, even when all enemies are gone.
        if (level.isSandbox) return false
        if (scriptedVictory.value) return true
        // Check if all planned spawns have occurred and all enemies are defeated.
        // Spawn-loop levels (issue #694) are "all spawned" once the group cursor has finished, or
        // once the remaining scripted schedule is statically exhausted (every future spawn is in the
        // past). The latter keeps victory prompt even though the cursor advances lazily (the loop
        // decision is deferred one turn so CONDITION checks see fully-resolved combat). A forecast of
        // null means the schedule is unbounded (an active CONDITION/INFINITE group), so such levels
        // cannot be won by clearing the field.
        val allSpawned =
            if (spawnGroups != null) {
                spawnGroupCursor.value.finished || forecastRemainingGroupSpawns()?.isEmpty() == true
            } else {
                spawnPlan.all { it.spawnTurn <= turnNumber.value }
            }
        return allSpawned &&
            attackers
                .filter { it.type != AttackerType.THE_KRAKEN }
                .all { it.isDefeated.value }
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
            total += attacker.calculateTargetDamage().toLong()
        }
        for (spawn in spawnPlan) {
            if (spawn.spawnTurn > turnNumber.value) {
                total += attackerTargetDamage(spawn.attackerType, spawn.level).toLong()
            }
        }
        // Spawn-loop levels: add the worst-case threat of all remaining scripted spawns. When the
        // remaining spawns are unbounded (an active CONDITION/INFINITE group), the threat is treated
        // as unbounded so no guaranteed win is ever offered.
        if (spawnGroups != null && !spawnGroupCursor.value.finished) {
            val forecast = forecastRemainingGroupSpawns()
            if (forecast == null) {
                return Long.MAX_VALUE
            }
            for (spawn in forecast) {
                total += attackerTargetDamage(spawn.attackerType, spawn.level).toLong()
            }
        }
        return total
    }

    /**
     * Returns true when the level is guaranteed to be won: even if every remaining enemy reached the
     * target, the player would still have health points left (or, for SINGLE_HIT-only levels, not
     * enough remaining enemies to take all remaining targets). Used to offer an instant "Win Level now".
     *
     * Excluded cases where a win cannot be guaranteed:
     *  - Not during the player's turn (e.g. building phase or enemy turn).
     *  - When a summoner enemy remains, since it can create an unbounded number of additional units.
     *  - When a villain remains (on the field or still to spawn), since a villain reaching a target
     *    loses the level outright, regardless of remaining health.
     *
     * Levels mixing SINGLE_HIT and STANDARD targets (or with no SINGLE_HIT targets at all) fall back
     * to the HP-based calculation, since at least one STANDARD target always remains reachable.
     */
    fun canWinLevelNow(): Boolean {
        // Sandbox levels can never be won, so never offer the instant win.
        if (level.isSandbox) return false
        if (phase.value != GamePhase.PLAYER_TURN) return false

        val singleHitTargets = level.targetInfoMap.filter { it.value.type == TargetType.SINGLE_HIT }.keys
        val onlySingleHitTargets = singleHitTargets.isNotEmpty() && singleHitTargets.size == level.targetInfoMap.size
        if (isLevelLost() || isLevelWon()) return false

        val aliveEnemies = attackers.filter { !it.isDefeated.value }
        // A spawn-loop level with an unbounded remaining schedule (active CONDITION/INFINITE group)
        // can never offer a guaranteed win, since the number of future enemies is not bounded.
        if (spawnGroups != null && !spawnGroupCursor.value.finished && forecastRemainingGroupSpawns() == null) {
            return false
        }
        val enemiesToSpawn =
            if (spawnGroups != null) {
                forecastRemainingGroupSpawns() ?: return false
            } else {
                spawnPlan.filter { it.spawnTurn > turnNumber.value }
            }
        // There must be at least one remaining enemy (otherwise the level is already won).
        if (aliveEnemies.isEmpty() && enemiesToSpawn.isEmpty()) return false
        // Summoners can create additional enemies, so the total threat cannot be bounded.
        if (aliveEnemies.any { it.type.isSummoner() } || enemiesToSpawn.any { it.attackerType.isSummoner() }) return false
        // A villain (on the field or still to spawn) loses the level the moment it reaches a target,
        // regardless of remaining health, so a guaranteed win can never be offered while one remains.
        if (aliveEnemies
                .filter { it.type != AttackerType.THE_KRAKEN }
                .any { it.type.isRealVillain } ||
            enemiesToSpawn
                .filter { it.attackerType != AttackerType.THE_KRAKEN }
                .any { it.attackerType.isRealVillain }
        ) {
            return false
        }

        if (onlySingleHitTargets) {
            // Each remaining enemy can take at most one SINGLE_HIT target. If there are fewer
            // remaining enemies than remaining (untaken) targets, not all targets can be taken,
            // so the level is guaranteed to be won once all enemies are defeated.
            val remainingSingleHitTargets = singleHitTargets.count { !takenTargets.contains(it) }
            return aliveEnemies.size + enemiesToSpawn.size < remainingSingleHitTargets
        }

        return getRemainingEnemyThreat() < healthPoints.value.toLong()
    }

    /**
     * Returns true if [position] is a target that can still be reached by enemies.
     * Taken SINGLE_HIT targets are excluded.
     */
    fun isActiveTargetPosition(position: Position): Boolean {
        if (!level.isTargetPosition(position) || takenTargets.contains(position)) return false
        val nextOrderedTarget = getNextSingleHitTargetPosition()
        return nextOrderedTarget == null || position == nextOrderedTarget
    }

    /**
     * Returns the next unclaimed SINGLE_HIT target in the configured order, or null when ordering
     * is disabled or every ordered target has been claimed.
     */
    fun getNextSingleHitTargetPosition(): Position? =
        level.singleHitTargetOrder.firstOrNull { target ->
            level.isTargetPosition(target) &&
                level.targetInfoMap[target]?.type == TargetType.SINGLE_HIT &&
                !takenTargets.contains(target)
        }

    /**
     * Returns the target positions enemies may currently attack.
     * While an ordered SINGLE_HIT target remains, it is the only available enemy destination.
     */
    fun getActiveTargetPositions(): List<Position> {
        val nextOrderedTarget = getNextSingleHitTargetPosition()
        return if (nextOrderedTarget != null) {
            listOf(nextOrderedTarget)
        } else {
            level.targetPositions.filter { !takenTargets.contains(it) }
        }
    }

    /**
     * When a SINGLE_HIT target at [takenPosition] is taken, redirect enemies to the next
     * ordered target when sequencing is enabled, otherwise to their nearest remaining target.
     */
    fun retargetEnemiesFromTakenTarget(takenPosition: Position) {
        val remaining = getActiveTargetPositions()
        if (remaining.isEmpty()) return // No active targets left – level will be lost
        val nextOrderedTarget = getNextSingleHitTargetPosition()
        for (enemy in attackers) {
            if (enemy.isDefeated.value) continue
            val newTarget =
                nextOrderedTarget
                    ?: if (enemy.currentTarget?.value == takenPosition) {
                        remaining.minByOrNull { enemy.position.value.distanceTo(it) } ?: continue
                    } else {
                        continue
                    }
            enemy.currentTarget?.value = newTarget
            println("Enemy ${enemy.id} (${enemy.type}) retargeted from $takenPosition to $newTarget")
        }
    }

    /**
     * Returns the effective next waypoint target, redirecting to the nearest active target
     * if the waypoint's next target is a taken SINGLE_HIT target.
     */
    fun resolveWaypointNextTarget(
        waypointNextTarget: Position,
        from: Position,
    ): Position {
        val nextOrderedTarget = getNextSingleHitTargetPosition()
        if (nextOrderedTarget != null && level.isTargetPosition(waypointNextTarget)) return nextOrderedTarget
        return if (takenTargets.contains(waypointNextTarget)) {
            getActiveTargetPositions().minByOrNull { from.distanceTo(it) } ?: waypointNextTarget
        } else {
            waypointNextTarget
        }
    }

    fun canPlaceDefender(type: DefenderType): Boolean = type != DefenderType.ALTAR && (level.isSandbox || coins.value >= type.baseCost) && level.availableTowers.contains(type)

    fun canUpgradeDefender(defender: Defender): Boolean = defender.type != DefenderType.ALTAR && (level.isSandbox || coins.value >= defender.upgradeCost) && !defender.isGrippedByKraken.value

    fun canSanctifyDefender(defender: Defender): Boolean =
        defender in defenders &&
            defender.type == DefenderType.WIZARD_TOWER &&
            defender.level.value >= 10 &&
            defender.isReady &&
            !defender.isDisabled.value &&
            defender.raftId.value == null &&
            currentTileTypeAt(defender.position.value) == de.egril.defender.editor.TileType.BUILD_AREA &&
            level.isBuildArea(defender.position.value) &&
            !level.isRiverTile(defender.position.value) &&
            runes.value >= 1 &&
            (phase.value == GamePhase.PLAYER_TURN || phase.value == GamePhase.INITIAL_BUILDING)

    fun canActivateAltar(defender: Defender): Boolean =
        defender in defenders &&
            defender.type == DefenderType.ALTAR &&
            phase.value == GamePhase.PLAYER_TURN &&
            defender.isReady &&
            !defender.isDisabled.value &&
            defender.actionsRemaining.value > 0 &&
            !defender.isChanneling.value

    fun hasActionsRemaining(): Boolean = actionsRemainingThisTurn.value > 0

    fun getRemainingPlannedEnemySpawns(): List<PlannedEnemySpawn> =
        if (spawnGroups != null) {
            forecastRemainingGroupSpawns() ?: emptyList()
        } else {
            spawnPlan.filter { it.spawnTurn > turnNumber.value }
        }

    fun getRemainingEnemyCount(): Int = getRemainingPlannedEnemySpawns().size

    // ---------------------------------------------------------------------------------------------
    // Spawn loops (issue #694)
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the planned spawns for the given absolute [turn].
     *
     * For legacy levels (no [spawnGroups]) this simply filters the flat [spawnPlan]. For spawn-loop
     * levels it advances the [spawnGroupCursor] exactly once per turn and materializes the spawns of
     * the active group's current turn offset. Called once per turn by the spawn code paths (turn 1
     * via initial spawning, later turns via the enemy-turn spawner). Re-querying an already-processed
     * turn returns no spawns, guarding against accidental double-spawning.
     */
    fun plannedSpawnsForTurn(turn: Int): List<PlannedEnemySpawn> {
        val root = spawnSequenceRootGroup ?: return spawnPlan.filter { it.spawnTurn == turn }
        return advanceSpawnGroups(root, turn)
    }

    /** The level's top-level spawn sequence wrapped in the implicit, non-repeating root group. */
    private val spawnSequenceRootGroup: SpawnGroup? by lazy { spawnGroups?.let { spawnSequenceRoot(it) } }

    /**
     * Binds a spawn group's logical [unitId] to the actually-spawned [attackerId]. The binding is
     * only set once (first spawned unit wins) so a named boss keeps a stable identity across loop
     * iterations and save/load.
     */
    fun bindSpawnGroupUnit(
        unitId: String,
        attackerId: Int,
    ) {
        if (!spawnGroupBindings.containsKey(unitId)) {
            spawnGroupBindings[unitId] = attackerId
        }
    }

    /** True if the unit bound to [unitId] exists and is not defeated. Missing bindings count as dead. */
    fun isSpawnGroupUnitAlive(unitId: String): Boolean {
        val boundId = spawnGroupBindings[unitId] ?: return false
        return attackers.any { it.id == boundId && !it.isDefeated.value }
    }

    private fun advanceSpawnGroups(
        root: SpawnGroup,
        turn: Int,
    ): List<PlannedEnemySpawn> {
        val startCursor = spawnGroupCursor.value
        if (startCursor.finished) return emptyList()
        // Only resolve each absolute turn once, in increasing order.
        if (turn <= startCursor.lastProcessedTurn) return emptyList()

        // Resolve any pending end-of-iteration decisions *before* spawning. This is done lazily at
        // the start of the turn so a loop predicate (e.g. CONDITION/UNIT_ALIVE) is evaluated only
        // after the previous iteration's final subturn — including that turn's resolved combat,
        // lasting (acid) damage, traps, and defeated-attacker processing — has fully played out.
        val cursor = SpawnSequenceRunner.resolve(root, startCursor, turn, stoppedSpawnLoops.toSet(), ::shouldLoop)
        spawnGroupCursor.value = cursor.copy(lastProcessedTurn = turn)
        return SpawnSequenceRunner.spawnsAt(root, cursor, turn)
    }

    /**
     * Permanently ends the spawn loop [groupId] (event action STOP_SPAWN_LOOP). A running loop stops
     * right away and spawning continues after it from the next unprocessed turn; a loop that has not
     * started yet is skipped when reached.
     */
    fun stopSpawnLoop(groupId: String) {
        if (groupId !in stoppedSpawnLoops) stoppedSpawnLoops.add(groupId)
        val root = spawnSequenceRootGroup ?: return
        val cursor = spawnGroupCursor.value
        spawnGroupCursor.value = SpawnSequenceRunner.stop(root, cursor, groupId, cursor.lastProcessedTurn + 1)
    }

    private fun shouldLoop(
        group: SpawnGroup,
        repetition: Int,
    ): Boolean =
        when (group.repeatMode) {
            SpawnRepeatMode.COUNT -> repetition < group.repeatCount - 1
            SpawnRepeatMode.CONDITION ->
                group.condition == SpawnCondition.UNIT_ALIVE &&
                    group.targetUnitId != null &&
                    isSpawnGroupUnitAlive(group.targetUnitId)
            SpawnRepeatMode.INFINITE -> true
        }

    /**
     * Best-effort forecast of all remaining scripted spawns for spawn-loop levels, used for UI
     * previews and guaranteed-win detection. Returns `null` when the remaining schedule is unbounded
     * (an [SpawnRepeatMode.INFINITE] group, or a [SpawnRepeatMode.CONDITION] group whose loop
     * decision depends on future game state). Does not mutate the cursor.
     */
    fun forecastRemainingGroupSpawns(): List<PlannedEnemySpawn>? {
        val root = spawnSequenceRootGroup ?: return emptyList()
        val cursor = spawnGroupCursor.value
        if (cursor.finished) return emptyList()
        return SpawnSequenceRunner.forecast(root, cursor, turnNumber.value, stoppedSpawnLoops.toSet())
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

    fun getPortalAtEntry(position: Position): Portal? =
        activePortals.firstOrNull { portal ->
            portal.entryPosition == position
        }

    fun isPortalEntry(position: Position): Boolean = getPortalAtEntry(position) != null

    fun isPortalExit(position: Position): Boolean =
        activePortals.any { portal ->
            portal.exitPosition == position
        }

    fun isPortalTile(position: Position): Boolean = isPortalEntry(position) || isPortalExit(position)

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
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value && !it.isDiving.value }

        return defenders.any { defender ->
            if (!defender.isReady ||
                defender.actionsRemaining.value <= 0 ||
                defender.isDisabled.value
            ) {
                return@any false
            }

            val hasEnemiesInRange = activeAttackers.any { attacker -> defender.canAttack(attacker, effectiveRange(defender)) }

            // Special handling for different tower types
            when (defender.type) {
                DefenderType.DWARVEN_MINE -> {
                    // Mines always count as having unused actions (digging)
                    true
                }
                DefenderType.ALTAR -> canActivateAltar(defender) || canWizardPlaceAnyMagicalTrap(defender)
                DefenderType.WIZARD_TOWER -> {
                    if (hasEnemiesInRange) {
                        true
                    } else {
                        currentMana.value < maxMana.value
                    }
                }
                else -> {
                    // Only count attack towers if they have AttackType and enemies in range
                    if (defender.type.attackType == AttackType.NONE) {
                        false
                    } else {
                        hasEnemiesInRange
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
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value && !it.isDiving.value }
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
                    DefenderType.ALTAR -> canActivateAltar(defender) || canWizardPlaceAnyMagicalTrap(defender)
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
     * Determine whether the auto-action button can perform attacks or mana generation this turn.
     * ATTACK means at least one ready tower can auto-attack an enemy in range.
     * MANA_ONLY means no attack is currently possible, but a wizard can auto-generate mana.
     */
    fun getAutoAttackAvailability(): AutoAttackAvailability {
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value && !it.isDiving.value }
        var hasManaOnlyAutoAction = false

        for (defender in defenders) {
            if (!defender.isReady ||
                defender.actionsRemaining.value <= 0 ||
                defender.isDisabled.value
            ) {
                continue
            }

            // Only count towers that can do regular auto-attacks.
            // Exclude mines (no attack). Wizards can also qualify when auto-attack would spend
            // their action on mana generation because they have no valid target.
            when {
                defender.type == DefenderType.DWARVEN_MINE -> continue
                defender.type.attackType == AttackType.NONE -> continue
                else -> {
                    val hasEnemiesInRange = activeAttackers.any { attacker -> defender.canAttack(attacker, effectiveRange(defender)) }
                    if (hasEnemiesInRange) {
                        return AutoAttackAvailability.ATTACK
                    }
                    if (defender.type == DefenderType.WIZARD_TOWER &&
                        currentMana.value < maxMana.value
                    ) {
                        hasManaOnlyAutoAction = true
                    } else {
                        continue
                    }
                }
            }
        }

        if (hasManaOnlyAutoAction) {
            return AutoAttackAvailability.MANA_ONLY
        } else {
            return AutoAttackAvailability.NONE
        }
    }

    fun hasDefendersForAutoAttack(): Boolean = getAutoAttackAvailability() != AutoAttackAvailability.NONE

    /**
     * Check if there are defenders with special actions that cannot be automated effectively.
     * Returns a list of defender types that have remaining special actions.
     */
    fun getDefenderTypesWithSpecialActions(): List<DefenderType> {
        val typesWithActions = mutableSetOf<DefenderType>()
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value && !it.isDiving.value }

        defenders.forEach { defender ->
            if (!defender.isReady || defender.actionsRemaining.value <= 0 || defender.isDisabled.value) {
                return@forEach
            }

            when {
                defender.type == DefenderType.ALTAR -> {
                    if (canActivateAltar(defender) || canWizardPlaceAnyMagicalTrap(defender)) {
                        typesWithActions.add(DefenderType.ALTAR)
                    }
                }
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
                    if (canWizardPlaceAnyMagicalTrap(defender)) {
                        typesWithActions.add(DefenderType.WIZARD_TOWER)
                    }
                }
            }
        }

        return typesWithActions.toList()
    }

    fun canWizardPlaceMagicalTrapAt(
        wizard: Defender,
        trapPosition: Position,
    ): Boolean {
        if (!wizard.hasMagicalTraps) return false
        if (wizard.level.value < 10) return false
        if (!wizard.isReady || wizard.actionsRemaining.value <= 0 || wizard.isDisabled.value) return false
        if (wizard.trapCooldownRemaining.value > 0) return false

        val distance = wizard.position.value.distanceTo(trapPosition)
        if (distance > effectiveRange(wizard)) return false
        if (!level.isOnPath(trapPosition)) return false
        if (traps.any { it.position == trapPosition }) return false
        if (attackers.any { it.position.value == trapPosition && !it.isDefeated.value }) return false
        if (fieldEffects.any { it.position == trapPosition }) return false
        if (fiefs.any { it.position == trapPosition }) return false

        return true
    }

    fun canWizardPlaceAnyMagicalTrap(wizard: Defender): Boolean =
        level.pathCells.any { position ->
            canWizardPlaceMagicalTrapAt(wizard, position)
        }

    /**
     * Initialize pre-placed defenders, attackers, traps, and barricades from level configuration.
     * This should be called right after GameState creation to set up the initial level state.
     */
    fun initializePrePlacedElements() {
        // Get initial data using the helper method that handles both old and new formats
        val initialData = level.getEffectiveInitialData()

        // Initialize player-usable supports (placable objects + spell tokens + fief tokens) for this level
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
        supportFiefRemaining.clear()
        for (supportFief in level.supports.fiefs) {
            supportFiefRemaining[supportFief.type] =
                combineSupportCounts(supportFiefRemaining[supportFief.type] ?: 0, supportFief.count)
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

        // Place initial bridges
        for (initialBridge in initialData.bridges) {
            val bridge =
                Bridge(
                    id = nextBridgeId.value++,
                    type = initialBridge.type,
                    positions = listOf(initialBridge.position),
                    currentHealth = mutableStateOf(initialBridge.healthPoints),
                    createdByAttackerId = 0,
                    createdOnTurn = 0,
                    isIndestructible = initialBridge.isIndestructible,
                )
            bridges.add(bridge)
        }

        // Place initial fiefs
        for (initialFief in initialData.fiefs) {
            fiefs.add(Fief(position = initialFief.position, type = initialFief.type))
        }

        // Place initial mushrooms
        for (initialMushroom in initialData.mushrooms) {
            mushrooms.add(Mushroom(position = initialMushroom.position))
        }

        // Place initial portals (level-designer-placed portal pairs, villainId = 0)
        for ((index, initialPortal) in initialData.portals.withIndex()) {
            activePortals.add(
                Portal(
                    id = nextPortalId.value++,
                    entryPosition = initialPortal.entryPosition,
                    exitPosition = initialPortal.exitPosition,
                    villainId = 0,
                    runeIndex = index % Portal.RUNE_POOL_SIZE,
                ),
            )
        }
    }
}
