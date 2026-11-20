package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.egril.defender.ui.settings.DifficultyLevel

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
    val nextGargoyleId: MutableState<Int> = mutableStateOf(1),
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
    val gargoyles: SnapshotStateList<Gargoyle> = mutableStateListOf(), // Pre-placed guardians (inactive or active)
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
    val storyLog: SnapshotStateList<StoryLogEntry> = mutableStateListOf(), // Messages already shown, oldest first
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
    // Movable tile regions (see [TileZone.movable]) that have been moved away from their home tiles:
    // zone id -> current positions of the region with the terrain it carries. Persisted in saves.
    val movedTileRegions: SnapshotStateMap<String, Map<Position, RegionTile>> = mutableStateMapOf(),
    // Rotation pivot of each moved region (zone id -> position). It moves with the region, so repeated
    // rotations stay exact. A region without an entry uses the centre of its home tiles.
    val movedTileRegionPivots: SnapshotStateMap<String, Position> = mutableStateMapOf(),
    // Units that sank to the river bed when their tile was flooded (see [AttackerType.survivesSubmersion]).
    // They are kept out of [attackers] so they can neither act nor be targeted, and do not count
    // towards winning the level. They re-surface once their tile is dry again.
    val submergedAttackers: SnapshotStateList<Attacker> = mutableStateListOf(),
    // Running loops of scripted events (see [EventLoop]).
    val activeEventLoops: SnapshotStateList<ActiveEventLoop> = mutableStateListOf(),
    val activeEventMapImages: SnapshotStateList<EventMapImage> = mutableStateListOf(),
    // Lines between activated altars shown by scripted events (see [AltarLink]).
    val activeAltarLinks: SnapshotStateList<AltarLink> = mutableStateListOf(),
) {
    // The original map tile type / river flow for every position, captured once from the level as
    // it was first loaded (before any runtime edits). Absent positions are NO_PLAY.
    private val originalTileTypes: Map<Position, de.egril.defender.editor.TileType> by lazy { buildTileTypeMap(originalLevel) }
    private val originalRiverTiles: Map<Position, RiverTile> by lazy { originalLevel.riverTiles.toMap() }
    internal val originalLevel: Level = level

    // Sandbox: the original map tile type for every position, captured once from the level as it was
    // first loaded (before any runtime edits). Used so runtime paints can be compared against the
    // original map and only genuine differences are tracked, persisted, and overlaid.
    internal val originalSandboxTileTypes: Map<Position, de.egril.defender.editor.TileType> =
        if (level.isSandbox) buildTileTypeMap(level) else emptyMap()
    internal val originalSandboxRiverTiles: Map<Position, RiverTile> =
        if (level.isSandbox) level.riverTiles.toMap() else emptyMap()
    internal val originalSandboxTargetInfoMap: Map<Position, TargetInfo> =
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
    ) = TileMapOps.sandboxPaintTile(this, position, tileType, riverFlow, riverSpeed)

    /**
     * Replace the type of several map tiles at once with a single rebuild of the level's tile
     * collections. Each entry maps a position to its new type and, for
     * [de.egril.defender.editor.TileType.RIVER], the river flow to use (ignored for other types).
     * Target tiles regain their original target metadata. Does not bump [mapEditVersion].
     */
    fun replaceTiles(changes: Map<Position, Pair<de.egril.defender.editor.TileType, RiverTile?>>) = TileMapOps.replaceTiles(this, changes)

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
     * The current tiles of the movable region [zone] with the terrain they carry. Until the region
     * is moved these are its home tiles with their current terrain.
     */
    fun regionTilesOf(zone: TileZone): Map<Position, RegionTile> = TileMapOps.regionTilesOf(this, zone)

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
    ): Map<Position, Pair<de.egril.defender.editor.TileType, de.egril.defender.editor.TileType>> = TileMapOps.refreshZoneTiles(this, positions)

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

    fun isLevelWon(): Boolean = WinLossRules.isLevelWon(this)

    fun isLevelLost(): Boolean = WinLossRules.isLevelLost(this)

    /**
     * Total worst-case health-point damage the player can still take, assuming every remaining
     * enemy (both those alive on the field and those still to spawn) reaches the target unhindered.
     * Uses [Long] so summoner/boss "all HP" markers ([Int.MAX_VALUE]) can be summed without overflow.
     */
    fun getRemainingEnemyThreat(): Long = WinLossRules.getRemainingEnemyThreat(this)

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
    fun canWinLevelNow(): Boolean = WinLossRules.canWinLevelNow(this)

    /**
     * Returns true if an inactive gargoyle stands on [position]. Inactive gargoyles are not in
     * [attackers], so units must check this separately: no unit may step onto their tile.
     */
    fun isInactiveGargoyleAt(position: Position): Boolean = gargoyles.any { !it.isActive.value && it.position.value == position }

    /**
     * Returns true if [position] is a target that can still be reached by enemies.
     * Taken SINGLE_HIT targets are excluded.
     */
    fun isActiveTargetPosition(position: Position): Boolean = TargetPlacementOps.isActiveTargetPosition(this, position)

    fun getNextSingleHitTargetPosition(): Position? = TargetPlacementOps.getNextSingleHitTargetPosition(this)

    fun getActiveTargetPositions(): List<Position> = TargetPlacementOps.getActiveTargetPositions(this)

    /**
     * When a SINGLE_HIT target at [takenPosition] is taken, redirect enemies to the next
     * ordered target when sequencing is enabled, otherwise to their nearest remaining target.
     */
    fun retargetEnemiesFromTakenTarget(takenPosition: Position) = TargetPlacementOps.retargetEnemiesFromTakenTarget(this, takenPosition)

    fun resolveWaypointNextTarget(
        waypointNextTarget: Position,
        from: Position,
    ): Position = TargetPlacementOps.resolveWaypointNextTarget(this, waypointNextTarget, from)

    fun canPlaceDefender(
        type: DefenderType,
        extraCost: Int = 0,
    ): Boolean = TargetPlacementOps.canPlaceDefender(this, type, extraCost)

    fun canAffordTowerButton(type: DefenderType): Boolean = TargetPlacementOps.canAffordTowerButton(this, type)

    fun keyboardPlacementCandidates(type: DefenderType): List<Position> = TargetPlacementOps.keyboardPlacementCandidates(this, type)

    fun isAutoTowerBasePathSpot(
        type: DefenderType,
        position: Position,
    ): Boolean = TargetPlacementOps.isAutoTowerBasePathSpot(this, type, position)

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
    ): List<PlannedEnemySpawn> = SpawnLoopOps.advanceSpawnGroups(this, root, turn)

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
    ): Boolean = SpawnLoopOps.shouldLoop(this, group, repetition)

    /**
     * Best-effort forecast of all remaining scripted spawns for spawn-loop levels, used for UI
     * previews and guaranteed-win detection. Returns `null` when the remaining schedule is unbounded
     * (an [SpawnRepeatMode.INFINITE] group, or a [SpawnRepeatMode.CONDITION] group whose loop
     * decision depends on future game state). Does not mutate the cursor.
     */
    fun forecastRemainingGroupSpawns(): List<PlannedEnemySpawn>? {
        val root = spawnSequenceRootGroup ?: return emptyList()
        return SpawnLoopOps.forecastRemainingGroupSpawns(this, root)
    }

    fun getActiveEnemyCount(): Int {
        // Count only non-defeated enemies that are NOT building bridges
        return this.attackers.count { !it.isDefeated.value && !it.isBuildingBridge.value }
    }

    /**
     * Check if a position is covered by any active bridge
     */
    fun isBridgeAt(position: Position): Boolean = QueryOps.isBridgeAt(this, position)

    /**
     * Get the bridge at a position, if any
     */
    fun getBridgeAt(position: Position): Bridge? = QueryOps.getBridgeAt(this, position)

    fun getPortalAtEntry(position: Position): Portal? = QueryOps.getPortalAtEntry(this, position)

    fun isPortalEntry(position: Position): Boolean = QueryOps.isPortalEntry(this, position)

    fun isPortalExit(position: Position): Boolean = QueryOps.isPortalExit(this, position)

    fun isPortalTile(position: Position): Boolean = QueryOps.isPortalTile(this, position)

    /**
     * Check if a position has a raft
     */
    fun isRaftAt(position: Position): Boolean = QueryOps.isRaftAt(this, position)

    /**
     * Get the raft at a position, if any
     */
    fun getRaftAt(position: Position): Raft? = QueryOps.getRaftAt(this, position)

    /**
     * Effective attack range for a defender, accounting for the DOUBLE_TOWER_REACH spell buff
     * (whether granted by a mana spell or a spell-token support). This must be used everywhere
     * range is evaluated for attacking so the buff behaves consistently in the UI and combat.
     */
    fun effectiveRange(defender: Defender): Int = QueryOps.effectiveRange(this, defender)

    /**
     * Check if there are defenders with unused action points and enemies in range
     * Used to show end turn confirmation dialog
     */
    fun hasDefendersWithUnusedActions(): Boolean = QueryOps.hasDefendersWithUnusedActions(this)

    /**
     * Get defenders that can be tabbed to: have action points left and either are mines
     * or have at least one enemy within attack range. Sorted by position (top-to-bottom,
     * left-to-right) for deterministic Tab cycling.
     */
    fun getActionableTowersForTab(): List<Defender> = QueryOps.getActionableTowersForTab(this)

    /**
     * Determine whether the auto-action button can perform attacks or mana generation this turn.
     * ATTACK means at least one ready tower can auto-attack an enemy in range.
     * MANA_ONLY means no attack is currently possible, but a wizard can auto-generate mana.
     */
    fun getAutoAttackAvailability(): AutoAttackAvailability = QueryOps.getAutoAttackAvailability(this)

    fun hasDefendersForAutoAttack(): Boolean = getAutoAttackAvailability() != AutoAttackAvailability.NONE

    /** See [TowerActionOps.getDefenderTypesWithSpecialActions]. */
    fun getDefenderTypesWithSpecialActions(): List<DefenderType> = TowerActionOps.getDefenderTypesWithSpecialActions(this)

    fun canWizardPlaceMagicalTrapAt(
        wizard: Defender,
        trapPosition: Position,
    ): Boolean = TowerActionOps.canWizardPlaceMagicalTrapAt(this, wizard, trapPosition)

    fun canWizardPlaceAnyMagicalTrap(wizard: Defender): Boolean = TowerActionOps.canWizardPlaceAnyMagicalTrap(this, wizard)

    /**
     * Initialize pre-placed defenders, attackers, traps, and barricades from level configuration.
     * This should be called right after GameState creation to set up the initial level state.
     */
    fun initializePrePlacedElements() = InitialSetupOps.initializePrePlacedElements(this)
}
