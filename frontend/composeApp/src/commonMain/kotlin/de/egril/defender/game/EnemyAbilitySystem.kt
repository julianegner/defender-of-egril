package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import kotlin.math.min
import kotlin.random.Random

/**
 * Handles special enemy abilities like summoning demons, healing, disabling towers, and building bridges.
 */
class EnemyAbilitySystem(
    private val state: GameState,
    private val pathfinding: PathfindingSystem,
) {
    private val bridgeSystem = BridgeSystem(state)

    companion object {
        private const val WEB_DURATION_TURNS = 10
        private const val SPIDER_WEB_SPEED_BONUS = 1
        private const val SHADOW_FOG_DURATION_TURNS = 3
        private const val SHADOW_FOG_MAX_RANGE = 10
        private const val MAX_SWARM_SPAWN_SEARCH_RINGS = 5
        private const val BARON_SCRAP_BOT_COUNT = 2
        private const val MAX_SNOTLINGS_PER_TILE = 250
        private const val SNOTLING_CANNON_MIN_STACK_HEALTH = 120
        private const val SNOTLING_CANNON_BASE_HEALTH = 100
        private const val SNOTLING_CANNON_MAX_THROW = 50
        private const val SNOTLING_CANNON_THROW_DISTANCE = 3
        private const val SNOTLING_CANNON_MIN_CASUALTY_PERCENT = 10
        private const val SNOTLING_CANNON_MAX_CASUALTY_PERCENT = 20

        /** Heal multiplier applied to green witches empowered by the Coven Synergy aura (50 % extra). */
        private const val COVEN_HEAL_BOOST_MULTIPLIER = 1.5f

        /** Extra disable rounds granted to red witches empowered by the Coven Synergy aura. */
        private const val COVEN_DISABLE_EXTRA_TURNS = 1

        /** Fixed cooldown (enemy turns) between consecutive Kraken dives. */
        private const val KRAKEN_DIVE_COOLDOWN = 3

        /** How many enemy turns the Kraken stays submerged per dive. */
        private const val KRAKEN_DIVE_DURATION = 2
    }

    /**
     * Attacker IDs of GREEN_WITCH units that benefit from a Coven Heal Boost this round.
     * Recomputed at the start of every [processEnemyAbilities] call.
     */
    private val covenEnhancedHealWitchIds = mutableSetOf<Int>()

    /**
     * Attacker IDs of RED_WITCH units that benefit from a Coven Disable Boost this round.
     * Recomputed at the start of every [processEnemyAbilities] call.
     */
    private val covenEnhancedDisableWitchIds = mutableSetOf<Int>()

    fun processSnotlingGrowth() {
        val snotlings =
            state.attackers.filter {
                !it.isDefeated.value && it.type == AttackerType.SNOTLING
            }
        val growthNumerator = if (state.waaghFrenzyActive.value) 3 else 5
        val growthDenominator = if (state.waaghFrenzyActive.value) 2 else 4

        snotlings.forEach { snotling ->
            val grownCount = (snotling.currentHealth.value * growthNumerator) / growthDenominator
            if (grownCount > MAX_SNOTLINGS_PER_TILE) {
                val overflow = grownCount - MAX_SNOTLINGS_PER_TILE
                distributeSnotlingOverflow(snotling.position.value, overflow)
            }
            snotling.currentHealth.value = minOf(grownCount, MAX_SNOTLINGS_PER_TILE)
        }
    }

    fun processHordeEating() {
        if (!state.level.waaghEnabled) return
        val attackersSnapshot = state.attackers.filter { !it.isDefeated.value }.toList()

        attackersSnapshot.forEach { attacker ->
            when (attacker.type) {
                AttackerType.ORK -> {
                    if (consumeNearbySnotlings(attacker, 10)) {
                        healAttacker(attacker, 10)
                        attacker.bloodlustRoundsLeft.value = maxOf(attacker.bloodlustRoundsLeft.value, 1)
                    }
                }
                AttackerType.OGRE -> {
                    if (consumeNearbySnotlings(attacker, 15)) {
                        healAttacker(attacker, 15)
                    }
                    eatGoblinOnSameTile(attacker)
                }
                else -> Unit
            }
        }
    }

    private fun hordeUnitCountsForWaagh(attackerType: AttackerType): Boolean = attackerType.faction == EnemyFaction.HORDE || attackerType.unitSize > 0

    private fun handleGarokkWarCry() {
        if (!state.level.waaghEnabled) return
        val garokk =
            state.attackers.firstOrNull { attacker -> !attacker.isDefeated.value && attacker.type == AttackerType.GAROKK }
                ?: return
        state.addWaaghPoints(5)
        state.garokkWarCryEffects.add(
            GarokkWarCryEffect(
                position = garokk.position.value,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    private fun applyHordeMomentumWaagh() {
        if (!state.level.waaghEnabled) return
        val hordeUnits = state.attackers.filter { attacker -> !attacker.isDefeated.value && hordeUnitCountsForWaagh(attacker.type) }
        if (hordeUnits.isEmpty()) return

        val visitedIds = mutableSetOf<Int>()
        var totalGain = 0
        for (unit in hordeUnits) {
            if (unit.id in visitedIds) continue
            val queue = ArrayDeque<Attacker>()
            queue.add(unit)
            visitedIds.add(unit.id)
            val cluster = mutableListOf<Attacker>()

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                cluster.add(current)
                val adjacentPositions =
                    current.position.value
                        .getHexNeighbors()
                        .toSet() + current.position.value
                for (candidate in hordeUnits) {
                    if (candidate.id in visitedIds) continue
                    if (candidate.position.value in adjacentPositions) {
                        queue.add(candidate)
                        visitedIds.add(candidate.id)
                    }
                }
            }

            if (cluster.size >= 9) {
                totalGain += cluster.size / 2
            }
        }

        state.addWaaghPoints(totalGain)
    }

    fun processEnemyAbilities() {
        handleGarokkWarCry()
        applyHordeMomentumWaagh()

        // Create a snapshot of attackers to avoid ConcurrentModificationException
        // when spawning new demons during iteration
        val attackersSnapshot = state.attackers.toList()

        // Villain auras are recomputed from scratch each enemy turn, so clear last turn's bonuses.
        for (attacker in attackersSnapshot) {
            attacker.speedBonus.value = 0
        }
        covenEnhancedHealWitchIds.clear()
        covenEnhancedDisableWitchIds.clear()
        processVillainAuras(attackersSnapshot)

        for (attacker in attackersSnapshot) {
            if (attacker.isDefeated.value) continue

            // Decrement summon cooldown
            if (attacker.summonCooldown.value > 0) {
                attacker.summonCooldown.value--
            }

            when (attacker.type) {
                AttackerType.EVIL_WIZARD -> {
                    handleSummon(
                        attacker,
                        state,
                        blueDemons = attacker.level.value,
                        redDemons = attacker.level.value / 2,
                        fixedLevel = attacker.level.value, // demons spawn at same level as wizard for more consistent scaling
                    )
                }
                AttackerType.EWHAD -> {
                    handleSummon(
                        attacker,
                        state,
                        blueDemons = attacker.level.value * 2,
                        redDemons = attacker.level.value,
                        undead = 3,
                    )
                }
                AttackerType.GREEN_WITCH -> {
                    val abilityUses = if (attacker.mushroomTurnsRemaining.value > 0) 2 else 1
                    repeat(abilityUses) {
                        applyGreenWitchHealing(attacker, enhanced = attacker.id in covenEnhancedHealWitchIds)
                    }
                }
                AttackerType.RED_WITCH -> {
                    val abilityUses = if (attacker.mushroomTurnsRemaining.value > 0) 2 else 1
                    repeat(abilityUses) {
                        // Disable nearby tower within range 1, with optional coven disable boost
                        disableNearestTowerInRange(
                            witch = attacker,
                            range = 1,
                            extraDisableTurns = if (attacker.id in covenEnhancedDisableWitchIds) COVEN_DISABLE_EXTRA_TURNS else 0,
                        )
                    }
                }
                AttackerType.SNOTLING_BOSS -> {
                    // Snotling Rally: summon a rabble of weak snotlings around Gribnak
                    handleSnotlingRally(attacker)
                }
                AttackerType.SNOTLING -> {
                    handleSnotlingCannon(attacker)
                }
                AttackerType.MORGUK_BONEWHISPER -> {
                    // Spirit Summon: spawn goblins on all adjacent path tiles every 3 turns
                    handleMorgukSpiritSummon(attacker)
                    // Hex of Silence: disable an adjacent defender
                    disableNearestTowerInRange(attacker, range = 1)
                }
                AttackerType.ARAXXA -> {
                    handleAraxxaWeb(attacker)
                    handleAraxxaSpiderlings(attacker)
                }
                AttackerType.BARON_RATTERZAHN -> {
                    handleBaronRatterzahn(attacker)
                }
                AttackerType.SILAS_THE_MASKMASTER -> {
                    handleSilasMirrorImages(attacker)
                }
                AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> {
                    // Green-witch ability: heal adjacent units with optional coven boost
                    applyGreenWitchHealing(attacker, enhanced = attacker.id in covenEnhancedHealWitchIds)
                    // Red-witch ability: disable nearest tower within range 3
                    disableNearestTowerInRange(
                        witch = attacker,
                        range = 3,
                        extraDisableTurns = if (attacker.id in covenEnhancedDisableWitchIds) COVEN_DISABLE_EXTRA_TURNS else 0,
                    )
                    // Coven Swap: every 5 rounds, swap places with a witch within range 3
                    if (attacker.summonCooldown.value == 0) {
                        val swapCooldown = attacker.type.covenSwapCooldown
                        if (swapCooldown != null) {
                            handleCovenSwap(attacker, range = 3)
                            attacker.summonCooldown.value = swapCooldown
                        }
                    }
                }
                AttackerType.HAGA -> {
                    // All green-witch abilities with optional coven heal boost
                    applyGreenWitchHealing(attacker, enhanced = attacker.id in covenEnhancedHealWitchIds)
                }
                AttackerType.ZUSSA -> {
                    // All red-witch abilities with optional coven disable boost
                    disableNearestTowerInRange(
                        witch = attacker,
                        range = 1,
                        extraDisableTurns = if (attacker.id in covenEnhancedDisableWitchIds) COVEN_DISABLE_EXTRA_TURNS else 0,
                    )
                }
                AttackerType.SYLVANAS_THE_MOLDING -> {
                    // Root Grip: every 3 rounds, thorny vines block a tower within range 3 for 2 player turns
                    handleSylvanasRootGrip(attacker)
                    // Self-Healing: restore selfHealPerTurn HP each enemy turn (handled below)
                }
                AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> {
                    // Time Loop: every 2 rounds, slow time within radius 5 — all towers skip a round
                    handleMalakorTimeLoop(attacker)
                }
                AttackerType.IGNIS_VA_THE_DRAGONVOICE -> {
                    // Call of the Brood: every 3 rounds, summon two Dragon-Terrors
                    handleIgnisVaCallOfTheBrood(attacker)
                }
                AttackerType.MORVATH_THE_SHADOWMASTER -> {
                    handleMorvathShadowFog(attacker)
                }
                AttackerType.XARITHON_THE_SHADOW_DRAGON -> {
                    // Shadow Spew: every 3 rounds, dark flames erupt in a 2×2 area disabling towers
                    handleXarithonShadowSpew(attacker)
                }
                AttackerType.CAPTAIN_RODERICH -> {
                    // Broadside: every 3 rounds, fire a cannonball at the nearest barge, sinking it
                    handleRoderichBroadside(attacker)
                    // Gold Treasure: accumulate coins each enemy turn
                    handleRoderichCoinGain(attacker)
                }
                AttackerType.THE_KRAKEN -> {
                    // Dive tick: decrement dive timer and re-surface when it expires
                    handleKrakenDiveTick(attacker)
                    // Barge Grip: seize and sink adjacent barges; also trigger periodic dives
                    handleKrakenAbilities(attacker)
                }
                AttackerType.ZYTHAR_THE_RIFTCALLER -> {
                    // Summon: every 3 turns spawn blue/red demons and demonling scouts
                    handleZytharSummon(attacker)
                }
                else -> {
                    // Check if this unit should build a bridge
                    // Units build bridges when adjacent to rivers blocking their path
                    if (attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                        if (bridgeSystem.shouldAutoBuildBridge(attacker)) {
                            bridgeSystem.autoBuildBridge(attacker)
                        }
                    }
                }
            }

            // Generic self-healing for any attacker type with selfHealPerTurn > 0
            if (attacker.type.selfHealPerTurn > 0 && attacker.currentHealth.value < attacker.maxHealth) {
                val healAmount = minOf(attacker.type.selfHealPerTurn, attacker.maxHealth - attacker.currentHealth.value)
                attacker.currentHealth.value += healAmount
            }
        }

        applySpiderWebBonuses()
    }

    private fun handleSnotlingCannon(snotling: Attacker) {
        val startPosition = state.enemyTurnStartPositions[snotling.id] ?: return
        if (startPosition == snotling.position.value) return
        if (snotling.currentHealth.value < SNOTLING_CANNON_MIN_STACK_HEALTH) return

        val thrownCount =
            minOf(
                snotling.currentHealth.value - SNOTLING_CANNON_BASE_HEALTH,
                SNOTLING_CANNON_MAX_THROW,
            )
        if (thrownCount <= 0) return

        val landingTile = findSnotlingCannonLandingTile(snotling) ?: return

        val casualtyPercent =
            Random.nextInt(
                from = SNOTLING_CANNON_MIN_CASUALTY_PERCENT,
                until = SNOTLING_CANNON_MAX_CASUALTY_PERCENT + 1,
            )
        val casualties = (thrownCount * casualtyPercent) / 100
        val survivors = thrownCount - casualties

        snotling.currentHealth.value -= thrownCount
        if (survivors <= 0) return

        state.pendingSnotlingCannonArrivals.add(
            PendingSnotlingCannonArrival(
                targetPosition = landingTile,
                thrownCount = survivors,
                turnNumber = state.turnNumber.value,
            ),
        )
        state.snotlingCannonThrowEffects.add(
            SnotlingCannonThrowEffect(
                sourcePosition = snotling.position.value,
                targetPosition = landingTile,
                thrownCount = survivors,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    fun processPendingSnotlingCannonArrivals() {
        for (arrival in state.pendingSnotlingCannonArrivals.toList()) {
            val landingStack = getOrCreateSnotlingStack(arrival.targetPosition)
            landingStack.currentHealth.value = minOf(MAX_SNOTLINGS_PER_TILE, landingStack.currentHealth.value + arrival.thrownCount)
        }
        state.pendingSnotlingCannonArrivals.clear()
    }

    private fun findSnotlingCannonLandingTile(snotling: Attacker): Position? {
        val currentPosition = snotling.position.value
        val currentTarget =
            snotling.currentTarget?.value ?: state.getActiveTargetPositions().minByOrNull { currentPosition.distanceTo(it) }
                ?: return null
        val path = pathfinding.findPath(currentPosition, currentTarget, snotling, ignoreBarricades = true)
        val landingTile = path.getOrNull(SNOTLING_CANNON_THROW_DISTANCE) ?: return null
        if (!state.level.isOnPath(landingTile)) return null
        if (
            state.attackers.any {
                !it.isDefeated.value &&
                    it.type != AttackerType.SNOTLING &&
                    it.position.value == landingTile
            }
        ) {
            return null
        }
        return landingTile
    }

    private fun consumeNearbySnotlings(
        eater: Attacker,
        requiredAmount: Int,
    ): Boolean {
        val candidatePositions =
            listOf(eater.position.value) +
                eater.position.value
                    .getHexNeighbors()
                    .filter { isWithinBounds(it) }
        val snotlingStacks =
            candidatePositions.mapNotNull { position ->
                state.attackers.find {
                    !it.isDefeated.value &&
                        it.type == AttackerType.SNOTLING &&
                        it.position.value == position
                }
            }
        val totalAvailable = snotlingStacks.sumOf { it.currentHealth.value }
        if (totalAvailable < requiredAmount) return false

        var remainingToEat = requiredAmount
        snotlingStacks.forEach { stack ->
            if (remainingToEat <= 0) return@forEach
            val eatenFromStack = min(stack.currentHealth.value, remainingToEat)
            stack.currentHealth.value -= eatenFromStack
            remainingToEat -= eatenFromStack
            if (stack.currentHealth.value <= 0) {
                stack.wasMerged.value = true
                stack.isDefeated.value = true
            }
        }
        state.addWaaghPoints(requiredAmount / 5)
        return true
    }

    private fun eatGoblinOnSameTile(ogre: Attacker) {
        val goblin =
            state.attackers.find {
                !it.isDefeated.value &&
                    it.id != ogre.id &&
                    it.type == AttackerType.GOBLIN &&
                    it.position.value == ogre.position.value
            } ?: return

        healAttacker(ogre, maxOf(10, goblin.currentHealth.value))
        goblin.wasMerged.value = true
        goblin.isDefeated.value = true
    }

    private fun healAttacker(
        attacker: Attacker,
        amount: Int,
    ) {
        attacker.currentHealth.value = minOf(attacker.maxHealth, attacker.currentHealth.value + amount)
    }

    private fun distributeSnotlingOverflow(
        origin: Position,
        overflowAmount: Int,
    ) {
        val pending = ArrayDeque<Pair<Position, Int>>()
        pending.add(origin to overflowAmount)

        while (pending.isNotEmpty()) {
            val (source, amount) = pending.removeFirst()
            if (amount <= 0) continue

            val validNeighbors =
                source.getHexNeighbors().filter { neighbor ->
                    isWithinBounds(neighbor) &&
                        state.level.isEnemyTraversable(neighbor) &&
                        state.attackers.none {
                            !it.isDefeated.value &&
                                it.position.value == neighbor &&
                                it.type != AttackerType.SNOTLING
                        }
                }
            if (validNeighbors.isEmpty()) continue

            val distributed = mutableMapOf<Position, Int>()
            repeat(amount) { index ->
                val neighbor = validNeighbors[index % validNeighbors.size]
                distributed[neighbor] = (distributed[neighbor] ?: 0) + 1
            }

            distributed.forEach { (targetPosition, extraCount) ->
                val stack = getOrCreateSnotlingStack(targetPosition)
                stack.currentHealth.value += extraCount
                if (stack.currentHealth.value > MAX_SNOTLINGS_PER_TILE) {
                    val overflow = stack.currentHealth.value - MAX_SNOTLINGS_PER_TILE
                    stack.currentHealth.value = MAX_SNOTLINGS_PER_TILE
                    pending.add(targetPosition to overflow)
                }
            }
        }
    }

    private fun getOrCreateSnotlingStack(position: Position): Attacker {
        val existing =
            state.attackers.find {
                !it.isDefeated.value &&
                    it.type == AttackerType.SNOTLING &&
                    it.position.value == position
            }
        if (existing != null) return existing

        val initialTarget =
            state.level.getWaypointAt(position)?.let { waypoint ->
                state.resolveWaypointNextTarget(waypoint.nextTarget, position)
            } ?: state.getActiveTargetPositions().minByOrNull { position.distanceTo(it) }
                ?: state.level.targetPositions.first()

        return Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.SNOTLING,
            position = mutableStateOf(position),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(0),
            currentTarget = mutableStateOf(initialTarget),
        ).also { state.attackers.add(it) }
    }

    private fun handleBaronRatterzahn(baron: Attacker) {
        hatchBaronScrapPiles(baron)
        val movedThisTurn = clearBaronScrapPilesAfterMovement(baron)
        dropBaronScrapPiles(baron, movedThisTurn)
        fireBaronRocket(baron)
    }

    private fun clearBaronScrapPilesAfterMovement(baron: Attacker): Boolean {
        val turnStartPosition = state.enemyTurnStartPositions[baron.id] ?: return false
        if (turnStartPosition == baron.position.value) return false
        state.scrapPiles.removeAll { it.ownerAttackerId == baron.id }
        return true
    }

    private fun hatchBaronScrapPiles(baron: Attacker) {
        val hatchable =
            state.scrapPiles.filter {
                it.ownerAttackerId == baron.id &&
                    it.hatchTurn <= state.turnNumber.value
            }
        if (hatchable.isEmpty()) return

        val inheritedTarget =
            baron.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }
        val usedPositions = mutableSetOf<Position>()
        for (scrapPile in hatchable) {
            val spawnPos = resolveRobotGoblinSpawn(scrapPile.position, usedPositions) ?: continue
            usedPositions.add(spawnPos)
            state.attackers.add(
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.ROBOTIC_GOBLIN,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(baron.level.value),
                    currentTarget = mutableStateOf(inheritedTarget),
                ),
            )
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = AttackerType.ROBOTIC_GOBLIN,
                ),
            )
        }
        state.scrapPiles.removeAll(hatchable.toSet())
    }

    private fun resolveRobotGoblinSpawn(
        origin: Position,
        usedPositions: Set<Position>,
    ): Position? {
        val primaryCandidates = listOf(origin) + origin.getHexNeighbors()
        for (candidate in primaryCandidates) {
            if (isValidRobotGoblinSpawnTile(candidate, usedPositions)) {
                return candidate
            }
        }
        return origin
            .getHexNeighborsWithinRadius(
                radius = 3,
                gridWidth = state.level.gridWidth,
                gridHeight = state.level.gridHeight,
            ).firstOrNull { isValidRobotGoblinSpawnTile(it, usedPositions) }
    }

    private fun isValidRobotGoblinSpawnTile(
        position: Position,
        usedPositions: Set<Position>,
    ): Boolean {
        if (!isWithinBounds(position)) return false
        if (position in usedPositions) return false
        if (!state.level.isEnemyTraversable(position)) return false
        if (isTileOccupiedByStaticObject(position)) return false
        return state.attackers.none { !it.isDefeated.value && it.position.value == position }
    }

    private fun dropBaronScrapPiles(
        baron: Attacker,
        movedThisTurn: Boolean,
    ) {
        if (!movedThisTurn) return
        val droppedPositions = mutableSetOf<Position>()
        val candidates = baronScrapDropCandidates(baron)
        for (candidate in candidates) {
            if (droppedPositions.size >= BARON_SCRAP_BOT_COUNT) break
            if (!isValidScrapDropTile(candidate, droppedPositions)) continue
            droppedPositions.add(candidate)
            state.scrapPiles.add(
                ScrapPile(
                    position = candidate,
                    ownerAttackerId = baron.id,
                    hatchTurn = state.turnNumber.value + 1,
                ),
            )
        }
    }

    private fun baronScrapDropCandidates(baron: Attacker): List<Position> {
        val anchorTiles =
            buildSet {
                add(baron.position.value)
                state.enemyTurnStartPositions[baron.id]?.let { add(it) }
            }

        val candidates = mutableListOf<Position>()
        for (anchor in anchorTiles) {
            candidates.add(anchor)
            candidates.addAll(anchor.getHexNeighbors())
        }

        return candidates
            .filter { isWithinBounds(it) }
            .sortedWith(compareBy<Position> { it.hexDistanceTo(baron.position.value) }.thenBy { it.x }.thenBy { it.y })
            .distinct()
    }

    private fun isValidScrapDropTile(
        position: Position,
        droppedPositions: Set<Position>,
    ): Boolean {
        if (position in droppedPositions) return false
        if (state.scrapPiles.any { it.position == position }) return false
        if (isTileOccupiedByStaticObject(position)) return false
        if (!state.level.isEnemyTraversable(position)) return false
        val onOrNextToPath =
            state.level.isOnPath(position) ||
                state.level.isSpawnPoint(position) ||
                position.getHexNeighbors().any {
                    isWithinBounds(it) && (state.level.isOnPath(it) || state.level.isSpawnPoint(it))
                }
        return onOrNextToPath
    }

    private fun fireBaronRocket(baron: Attacker) {
        val cooldown = baron.type.towerDisableCooldown ?: return
        if (baron.villainCooldown.value > 0) {
            baron.villainCooldown.value--
            return
        }

        val range = (baron.type.towerDisableRangeBase ?: 0) + baron.level.value
        val disableDurationTurns = (baron.type.towerDisableDurationTurns ?: 0) + 1 // +1 because timers decrement at enemy-turn end

        val targetTower =
            state.defenders
                .filter { it.isReady && !it.isDisabled.value }
                .filter { baron.position.value.hexDistanceTo(it.position.value) <= range }
                .maxWithOrNull(
                    compareBy<Defender> { it.actualDamage }
                        .thenByDescending { baron.position.value.hexDistanceTo(it.position.value) },
                )

        if (targetTower != null) {
            targetTower.isDisabled.value = true
            targetTower.disabledTurnsRemaining.value = disableDurationTurns
            state.rocketAttackEffects.add(
                RocketAttackEffect(
                    sourcePosition = baron.position.value,
                    targetPosition = targetTower.position.value,
                    turnNumber = state.turnNumber.value,
                ),
            )
        }

        baron.villainCooldown.value = cooldown
    }

    /**
     * Sylvanas's Root Grip: every [towerDisableCooldown] rounds, thorny vines burst from beneath
     * the highest-damage non-disabled tower within [towerDisableRangeBase] tiles, blocking it for
     * [towerDisableDurationTurns] player turns (+1 to account for the immediate decrement at the
     * end of the enemy turn).
     */
    private fun handleSylvanasRootGrip(sylvanas: Attacker) {
        val cooldown = sylvanas.type.towerDisableCooldown ?: return
        if (sylvanas.villainCooldown.value > 0) {
            sylvanas.villainCooldown.value--
            return
        }

        val range = sylvanas.type.towerDisableRangeBase ?: 0
        // +1 to account for the immediate decrement in updateTowerDisableStatus at enemy-turn end
        val disableDurationTurns = (sylvanas.type.towerDisableDurationTurns ?: 0) + 1

        val targetTower =
            state.defenders
                .filter { tower ->
                    tower.isReady &&
                        !tower.isDisabled.value &&
                        sylvanas.position.value.hexDistanceTo(tower.position.value) <= range
                }.maxWithOrNull(
                    compareBy<Defender> { it.actualDamage }
                        .thenByDescending { sylvanas.position.value.hexDistanceTo(it.position.value) },
                )

        if (targetTower != null) {
            targetTower.isDisabled.value = true
            targetTower.disabledTurnsRemaining.value = disableDurationTurns
            targetTower.hasRootGripAnimation.value = true // Show vine animation while disabled
        }

        sylvanas.villainCooldown.value = cooldown
    }

    /**
     * Time Loop (Archmage Malakor the Renegade): every [towerDisableCooldown] rounds, Malakor
     * slows the flow of time in a radius of [towerDisableRangeBase] tiles. All ready, non-disabled
     * towers within that radius must skip their next player turn.
     */
    private fun handleMalakorTimeLoop(malakor: Attacker) {
        val cooldown = malakor.type.towerDisableCooldown ?: return
        if (malakor.villainCooldown.value > 0) {
            malakor.villainCooldown.value--
            return
        }

        val range = malakor.type.towerDisableRangeBase ?: 0
        // +1 to account for the immediate decrement in updateTowerDisableStatus at enemy-turn end
        val disableDurationTurns = (malakor.type.towerDisableDurationTurns ?: 0) + 1

        // Time Loop affects ALL towers in range, not just the nearest one
        state.defenders
            .filter { tower ->
                tower.isReady &&
                    !tower.isDisabled.value &&
                    malakor.position.value.hexDistanceTo(tower.position.value) <= range
            }.forEach { tower ->
                tower.isDisabled.value = true
                tower.disabledTurnsRemaining.value = disableDurationTurns
            }

        malakor.villainCooldown.value = cooldown
    }

    /**
     * Ignis-Va's Call of the Brood: every 3 rounds summons two flying Dragon-Terrors near her
     * position. Each Dragon-Terror spawns at the villain's level (minimum level 2).
     */
    private fun handleIgnisVaCallOfTheBrood(ignisVa: Attacker) {
        if (ignisVa.summonCooldown.value > 0) return

        val spawnLevel = maxOf(2, ignisVa.level.value)
        repeat(2) {
            spawnDragonTerrorNear(ignisVa, spawnLevel)
        }
        ignisVa.summonCooldown.value = 3
    }

    /**
     * Spawn a Dragon-Terror near the given summoner on a free path tile.
     * Dragon-Terrors are always at least level 2.
     */
    private fun spawnDragonTerrorNear(
        summoner: Attacker,
        level: Int,
    ) {
        val summonerPos = summoner.position.value

        val possiblePositions = mutableListOf<Position>()
        possiblePositions.addAll(summonerPos.getHexNeighbors())
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }

        val validPositions =
            possiblePositions
                .filter { pos ->
                    pos.x >= 0 &&
                        pos.x < state.level.gridWidth &&
                        pos.y >= 0 &&
                        pos.y < state.level.gridHeight &&
                        state.level.isOnPath(pos) &&
                        !state.isPortalTile(pos) &&
                        !state.attackers.any { it.position.value == pos && !it.isDefeated.value }
                }.distinct()

        if (validPositions.isEmpty()) return

        val spawnPos = validPositions.random()

        val inheritedTarget =
            summoner.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val dragonTerror =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.DRAGON_TERROR,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(level),
                currentTarget = mutableStateOf(inheritedTarget),
            )
        state.attackers.add(dragonTerror)

        state.enemySpawnEffects.add(
            EnemySpawnEffect(
                position = spawnPos,
                turnNumber = state.turnNumber.value,
                attackerType = AttackerType.DRAGON_TERROR,
            ),
        )
    }

    /**
     * Applies the aura of every villain that is currently on the battlefield to all friendly units
     * within the ability's range while it is on the battlefield. War Cry style abilities activate on
     * a cooldown; while below 50% health the villain also permanently benefits from its own aura.
     */
    private fun processVillainAuras(attackers: List<Attacker>) {
        for (villain in attackers) {
            if (villain.isDefeated.value) continue
            val ability = villain.type.villainAbility ?: continue

            // Decrement the villain's ability cooldown each round.
            if (villain.villainCooldown.value > 0) {
                villain.villainCooldown.value--
            }

            val isBelowHalfHealth = villain.currentHealth.value * 2 <= villain.maxHealth
            val activatesThisRound = villain.villainCooldown.value == 0
            if (activatesThisRound) {
                villain.villainCooldown.value = ability.cooldown
            }

            // Nothing to do this round unless the ability triggers or the wounded villain keeps its
            // permanent self-buff.
            if (!activatesThisRound && !isBelowHalfHealth) continue

            when (ability.effect) {
                VillainAuraEffect.SPEED ->
                    applySpeedAura(
                        villain = villain,
                        ability = ability,
                        applyToAllies = activatesThisRound,
                        applyToSelf = isBelowHalfHealth,
                    )
                VillainAuraEffect.SOUL_CALL -> {
                    // Soul Call is recorded when nearby units die and resolved at the next round start.
                }
                VillainAuraEffect.COVEN_HEAL_BOOST -> {
                    if (activatesThisRound) applyCovenHealBoost(villain, ability)
                }
                VillainAuraEffect.COVEN_DISABLE_BOOST -> {
                    if (activatesThisRound) applyCovenDisableBoost(villain, ability)
                }
                VillainAuraEffect.COVEN_SYNERGY -> {
                    if (activatesThisRound) {
                        applyCovenHealBoost(villain, ability)
                        applyCovenDisableBoost(villain, ability)
                    }
                }
            }
        }
    }

    private fun applySpeedAura(
        villain: Attacker,
        ability: VillainAbility,
        applyToAllies: Boolean,
        applyToSelf: Boolean,
    ) {
        if (applyToSelf) {
            villain.speedBonus.value = maxOf(villain.speedBonus.value, ability.magnitude)
        }
        if (!applyToAllies || villain.type.faction == EnemyFaction.NONE) return
        for (ally in state.attackers) {
            if (ally.isDefeated.value || ally.id == villain.id) continue
            if (ally.type.faction != villain.type.faction) continue
            val inRange =
                ability.range == VillainAbility.FULL_BATTLEFIELD ||
                    villain.position.value.hexDistanceTo(ally.position.value) <= ability.range
            if (inRange) {
                ally.speedBonus.value = maxOf(ally.speedBonus.value, ability.magnitude)
            }
        }
    }

    private fun handleSummon(
        attacker: Attacker,
        state: GameState,
        blueDemons: Int,
        redDemons: Int,
        undead: Int = 0,
        fixedLevel: Int? = null,
    ) {
        if (attacker.summonCooldown.value == 0) {
            repeat(blueDemons) {
                spawnDemonNear(attacker, AttackerType.BLUE_DEMON, fixedLevel ?: state.turnNumber.value)
            }
            repeat(redDemons) {
                spawnDemonNear(attacker, AttackerType.RED_DEMON, fixedLevel ?: state.turnNumber.value)
            }
            repeat(undead) {
                spawnUndeadNear(attacker, 10 + (fixedLevel ?: state.turnNumber.value))
            }
            attacker.summonCooldown.value = 3
        }
    }

    /**
     * Apply green-witch healing to adjacent units.
     *
     * @param witch     The healer (GREEN_WITCH, HAGA, or GRAND_COVEN_MOTHER_SYBILLA).
     * @param enhanced  When true, healing is multiplied by [COVEN_HEAL_BOOST_MULTIPLIER] (coven synergy).
     */
    private fun applyGreenWitchHealing(
        witch: Attacker,
        enhanced: Boolean = false,
    ) {
        val adjacentPositions = witch.position.value.getHexNeighbors()
        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println(
                "DEBUG: Green witch ${witch.id} at ${witch.position.value} checking ${adjacentPositions.size} adjacent positions" +
                    if (enhanced) " (COVEN BOOST)" else "",
            )
        }
        var healedCount = 0
        for (adjacent in adjacentPositions) {
            val adjacentEnemy =
                state.attackers.find {
                    !it.isDefeated.value && it.id != witch.id && it.position.value == adjacent
                }
            if (adjacentEnemy != null) {
                // Heal 5x witch level (×1.5 with coven boost), capped at missing HP
                val baseHeal = witch.effectiveLevel * 5
                val scaledHeal = if (enhanced) (baseHeal * COVEN_HEAL_BOOST_MULTIPLIER).toInt() else baseHeal
                val healAmount = min(scaledHeal, adjacentEnemy.maxHealth - adjacentEnemy.currentHealth.value)
                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Found ${adjacentEnemy.type} at $adjacent, HP ${adjacentEnemy.currentHealth.value}/${adjacentEnemy.maxHealth}, heal amount: $healAmount",
                    )
                }
                if (healAmount > 0) {
                    adjacentEnemy.currentHealth.value += healAmount
                    healedCount++
                    state.healingEffects.add(
                        HealingEffect(
                            position = adjacent,
                            type = HealingEffectType.GREEN_WITCH,
                            healAmount = healAmount,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                    if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                        println(
                            "DEBUG: Healed ${adjacentEnemy.type} for $healAmount HP (new HP: ${adjacentEnemy.currentHealth.value})",
                        )
                    }
                }

                // Remove up to 3 barbs from adjacent enemy
                if (adjacentEnemy.movementPenalty.value > 0) {
                    val barbsToRemove = minOf(3, adjacentEnemy.movementPenalty.value)
                    adjacentEnemy.movementPenalty.value -= barbsToRemove
                    if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                        println(
                            "DEBUG: Green witch removed $barbsToRemove barbs from ${adjacentEnemy.type}, remaining penalty: ${adjacentEnemy.movementPenalty.value}",
                        )
                    }
                }
            }
        }
        if (healedCount > 0) {
            println("DEBUG: Green witch ${witch.id} healed $healedCount enemies${if (enhanced) " (coven boost)" else ""}")
        } else {
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Green witch ${witch.id} found no adjacent damaged enemies to heal")
            }
        }
    }

    /**
     * Mark all GREEN_WITCH attackers within the villain's aura range as having enhanced healing.
     * The boost is applied when [applyGreenWitchHealing] is called with enhanced=true.
     */
    private fun applyCovenHealBoost(
        villain: Attacker,
        ability: VillainAbility,
    ) {
        for (ally in state.attackers) {
            if (ally.isDefeated.value || ally.id == villain.id) continue
            if (ally.type != AttackerType.GREEN_WITCH) continue
            val inRange =
                ability.range == VillainAbility.FULL_BATTLEFIELD ||
                    villain.position.value.hexDistanceTo(ally.position.value) <= ability.range
            if (inRange) covenEnhancedHealWitchIds.add(ally.id)
        }
    }

    /**
     * Mark all RED_WITCH attackers within the villain's aura range as having enhanced disabling.
     * The boost adds [COVEN_DISABLE_EXTRA_TURNS] to the disable duration in [disableNearestTowerInRange].
     */
    private fun applyCovenDisableBoost(
        villain: Attacker,
        ability: VillainAbility,
    ) {
        for (ally in state.attackers) {
            if (ally.isDefeated.value || ally.id == villain.id) continue
            if (ally.type != AttackerType.RED_WITCH) continue
            val inRange =
                ability.range == VillainAbility.FULL_BATTLEFIELD ||
                    villain.position.value.hexDistanceTo(ally.position.value) <= ability.range
            if (inRange) covenEnhancedDisableWitchIds.add(ally.id)
        }
    }

    /**
     * Coven Swap: Sybilla teleports to a witch within [range] tiles (and that witch moves to
     * Sybilla's former position). Prioritises the witch that is closest to Sybilla.
     * Does nothing if no suitable witch is found.
     */
    private fun handleCovenSwap(
        sybilla: Attacker,
        range: Int,
    ) {
        val sybillaPos = sybilla.position.value
        val sybillaStartPos = state.enemyTurnStartPositions[sybilla.id] ?: return

        // Sybilla can't swap if she's already moved this turn
        if (sybillaPos != sybillaStartPos) {
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Sybilla ${sybilla.id} skipped swap because she already moved this turn")
            }
            return
        }

        val witchTypes = setOf(AttackerType.GREEN_WITCH, AttackerType.RED_WITCH, AttackerType.HAGA, AttackerType.ZUSSA)
        val nearbyWitches =
            state.attackers
                .filter { ally ->
                    !ally.isDefeated.value &&
                        ally.id != sybilla.id &&
                        ally.type in witchTypes &&
                        sybillaPos.hexDistanceTo(ally.position.value) <= range
                }

        // Find witches close to target (1-2 moves away)
        val targetAdjacentWitches =
            nearbyWitches
                .mapNotNull { witch ->
                    val target = witch.currentTarget?.value ?: state.level.targetPositions.first()
                    val distanceToTarget = pathfinding.findPath(witch.position.value, target, witch).size - 1
                    if (distanceToTarget in 1..2) witch else null
                }

        // Prefer target-adjacent witches; if none, consider witches in a crowd (3+ witches)
        val swapTarget =
            if (targetAdjacentWitches.isNotEmpty()) {
                targetAdjacentWitches.minByOrNull { sybillaPos.hexDistanceTo(it.position.value) }
            } else if (nearbyWitches.size >= 3) {
                nearbyWitches.minByOrNull { sybillaPos.hexDistanceTo(it.position.value) }
            } else {
                null
            }

        if (swapTarget != null) {
            val targetPos = swapTarget.position.value
            swapTarget.position.value = sybillaPos
            sybilla.position.value = targetPos

            // Queue the swap message with highlight positions
            state.pendingMessages.add(
                GameMessage(
                    type = GameMessageType.COVEN_SWAP,
                    name = null,
                    highlightPositions = sybillaStartPos to targetPos,
                ),
            )

            // Add visual effects for both positions to show the swap
            state.enemyMoveEffects.add(EnemyMoveEffect(sybillaStartPos, state.turnNumber.value))
            state.enemyMoveEffects.add(EnemyMoveEffect(targetPos, state.turnNumber.value))

            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Sybilla ${sybilla.id} swapped with ${swapTarget.type} ${swapTarget.id} ($sybillaStartPos <-> $targetPos)")
            }
        }
    }

    /**
     * Snotling Rally: Gribnak the Squealer summons a rabble of very weak snotlings on every
     * path tile within distance 2. If a candidate tile is blocked by a non-snotling enemy or is
     * not a path tile at all, the snotling is redirected to the closest applicable tile instead
     * (tiles already occupied by snotlings are valid targets, since snotlings merge on arrival).
     */
    private fun handleSnotlingRally(boss: Attacker) {
        if (boss.summonCooldown.value > 0) return

        val bossPos = boss.position.value
        // Collect all tiles within a distance of 2 (neighbours and neighbours-of-neighbours)
        val candidateTiles = mutableSetOf<Position>()
        for (neighbor in bossPos.getHexNeighbors()) {
            candidateTiles.add(neighbor)
            candidateTiles.addAll(neighbor.getHexNeighbors())
        }
        candidateTiles.remove(bossPos)

        val spawnPositions =
            resolveSwarmSpawnPositions(
                candidates = candidateTiles,
                stackableType = AttackerType.SNOTLING,
                isBaseTraversable = { state.level.isOnPath(it) },
                maxSearchRings = MAX_SWARM_SPAWN_SEARCH_RINGS,
            )

        if (spawnPositions.isEmpty()) return

        // Snotlings follow the same waypoint chain as Gribnak
        val inheritedTarget =
            boss.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        for (spawnPos in spawnPositions) {
            summonSwarmUnit(
                type = AttackerType.SNOTLING,
                spawnPos = spawnPos,
                level = boss.level.value,
                currentTarget = inheritedTarget,
            )
        }

        boss.summonCooldown.value = 3
    }

    /**
     * Spirit Summon: Morguk Bonewhisper conjures goblins on every adjacent path tile.
     * Each goblin spawns at the same level as Morguk. Activates every 3 turns.
     */
    private fun handleMorgukSpiritSummon(morguk: Attacker) {
        if (morguk.summonCooldown.value > 0) return

        val morgukPos = morguk.position.value
        val inheritedTarget =
            morguk.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        // Spawn on adjacent path tiles that are not already occupied by a living unit
        val adjacentPathTiles =
            morgukPos.getHexNeighbors().filter { pos ->
                pos.x >= 0 &&
                    pos.x < state.level.gridWidth &&
                    pos.y >= 0 &&
                    pos.y < state.level.gridHeight &&
                    state.level.isOnPath(pos) &&
                    state.attackers.none { !it.isDefeated.value && it.position.value == pos }
            }

        for (spawnPos in adjacentPathTiles) {
            val goblin =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.GOBLIN,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(morguk.level.value),
                    currentTarget = mutableStateOf(inheritedTarget),
                )
            state.attackers.add(goblin)
        }

        morguk.summonCooldown.value = 3
    }

    /**
     * Araxxa spreads a persistent spider web that grows by one tile per enemy turn and always
     * remains under the villain herself.
     */
    private fun handleAraxxaWeb(araxxa: Attacker) {
        val araxxaPosition = araxxa.position.value
        val turnStartPosition = state.enemyTurnStartPositions[araxxa.id]
        val didMoveThisTurn = turnStartPosition != null && turnStartPosition != araxxaPosition

        val webPositions = mutableSetOf<Position>()
        webPositions.add(araxxaPosition)
        webPositions.addAll(
            araxxaPosition.getHexNeighbors().filter { state.level.isEnemyTraversable(it) },
        )

        if (!didMoveThisTurn) {
            webPositions.addAll(
                araxxaPosition
                    .getHexNeighborsWithinRadius(2, state.level.gridWidth, state.level.gridHeight)
                    .filter { it.hexDistanceTo(araxxaPosition) == 2 && state.level.isEnemyTraversable(it) },
            )
        }

        for (position in webPositions) {
            if (isTileOccupiedByStaticObject(position)) continue
            refreshSpiderWebAt(position)
        }
    }

    /**
     * Araxxa summons spiderlings on adjacent enemy-traversable tiles. Spiderlings are swarm units
     * and may share tiles with other spiderlings, just like snotlings.
     */
    private fun handleAraxxaSpiderlings(araxxa: Attacker) {
        val inheritedTarget =
            araxxa.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val spawnPositions =
            resolveSwarmSpawnPositions(
                candidates = araxxa.position.value.getHexNeighbors(),
                stackableType = AttackerType.SPIDERLING,
                isBaseTraversable = { state.level.isEnemyTraversable(it) },
                maxSearchRings = MAX_SWARM_SPAWN_SEARCH_RINGS,
            )

        if (spawnPositions.isEmpty()) return

        for (spawnPos in spawnPositions) {
            summonSwarmUnit(
                type = AttackerType.SPIDERLING,
                spawnPos = spawnPos,
                level = araxxa.level.value,
                currentTarget = inheritedTarget,
            )
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = AttackerType.SPIDERLING,
                    suppressPortalAnimation = true,
                ),
            )
        }
    }

    /**
     * Silas the Maskmaster creates two decoy copies on nearby enemy-traversable tiles every three
     * rounds. Existing images remain on the battlefield. After conjuring the new images, the real
     * Silas swaps to a random position among himself and the fresh copies so the player cannot tell
     * which of the three is genuine.
     */
    private fun handleSilasMirrorImages(silas: Attacker) {
        val mirrorImageCooldown = silas.type.mirrorImageCooldown ?: return
        if (silas.summonCooldown.value > 0) return

        val mirrorCount = silas.type.mirrorImageCount ?: return
        val mirrorRange = silas.type.mirrorImageRange ?: return
        val silasPosition = silas.position.value
        val inheritedTarget =
            silas.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val candidatePositions =
            silasPosition
                .getHexNeighborsWithinRadius(mirrorRange, state.level.gridWidth, state.level.gridHeight)
                .filter { candidate ->
                    candidate != silasPosition &&
                        state.level.isEnemyTraversable(candidate) &&
                        !isTileOccupiedByStaticObject(candidate) &&
                        state.attackers.none { !it.isDefeated.value && it.position.value == candidate }
                }.sortedWith(
                    compareBy<Position> { silasPosition.hexDistanceTo(it) }
                        .thenBy { it.x }
                        .thenBy { it.y },
                ).take(mirrorCount)

        if (candidatePositions.isEmpty()) return

        val mirrors =
            candidatePositions.map { spawnPos ->
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.SILAS_MIRROR_IMAGE,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(silas.level.value),
                    currentTarget = mutableStateOf(inheritedTarget),
                )
            }
        state.attackers.addAll(mirrors)
        candidatePositions.forEach { spawnPos ->
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = AttackerType.SILAS_MIRROR_IMAGE,
                ),
            )
        }

        val shuffledPositions = (listOf(silasPosition) + candidatePositions).shuffled()
        silas.position.value = shuffledPositions.first()
        mirrors.zip(shuffledPositions.drop(1)).forEach { (mirror, newPosition) ->
            mirror.position.value = newPosition
        }

        silas.summonCooldown.value = mirrorImageCooldown
    }

    private fun summonSwarmUnit(
        type: AttackerType,
        spawnPos: Position,
        level: Int,
        currentTarget: Position,
    ) {
        val existingSwarmUnit =
            state.attackers.find {
                !it.isDefeated.value &&
                    it.position.value == spawnPos &&
                    it.type == type
            }
        if (existingSwarmUnit != null) {
            existingSwarmUnit.currentHealth.value += type.health * level
            return
        }

        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(level),
                currentTarget = mutableStateOf(currentTarget),
            ),
        )
    }

    private fun resolveSwarmSpawnPositions(
        candidates: Collection<Position>,
        stackableType: AttackerType,
        isBaseTraversable: (Position) -> Boolean,
        maxSearchRings: Int,
    ): List<Position> {
        fun isValidSpawnTile(pos: Position): Boolean =
            isWithinBounds(pos) &&
                isBaseTraversable(pos) &&
                !isTileOccupiedByStaticObject(pos) &&
                state.attackers.none {
                    !it.isDefeated.value &&
                        it.position.value == pos &&
                        it.type != stackableType
                }

        val spawnPositions = mutableListOf<Position>()
        for (candidate in candidates) {
            if (isValidSpawnTile(candidate)) {
                spawnPositions.add(candidate)
            } else {
                val visited = mutableSetOf(candidate)
                var frontier = candidate.getHexNeighbors().toMutableList()
                var found = false
                repeat(maxSearchRings) {
                    if (found) return@repeat
                    val nextFrontier = mutableListOf<Position>()
                    for (pos in frontier) {
                        if (pos in visited) continue
                        visited.add(pos)
                        if (isValidSpawnTile(pos)) {
                            spawnPositions.add(pos)
                            found = true
                            break
                        }
                        nextFrontier.addAll(pos.getHexNeighbors())
                    }
                    frontier = nextFrontier
                }
            }
        }
        return spawnPositions
    }

    private fun isWithinBounds(position: Position): Boolean =
        position.x >= 0 &&
            position.x < state.level.gridWidth &&
            position.y >= 0 &&
            position.y < state.level.gridHeight

    private fun isTileOccupiedByStaticObject(position: Position): Boolean =
        state.defenders.any { it.position.value == position } ||
            state.barricades.any { it.position == position && !it.isDestroyed() } ||
            state.traps.any { it.position == position } ||
            state.fiefs.any { it.position == position }

    private fun refreshSpiderWebAt(position: Position) {
        val existingEffect =
            state.fieldEffects.find {
                it.type == FieldEffectType.WEB && it.position == position
            }
        if (existingEffect != null) {
            existingEffect.turnsRemaining = WEB_DURATION_TURNS
            return
        }

        state.fieldEffects.add(
            FieldEffect(
                position = position,
                type = FieldEffectType.WEB,
                damage = 0,
                turnsRemaining = WEB_DURATION_TURNS,
                defenderId = 0,
            ),
        )
    }

    private fun applySpiderWebBonuses() {
        val webPositions =
            state.fieldEffects
                .filter { it.type == FieldEffectType.WEB }
                .mapTo(mutableSetOf()) { it.position }

        for (attacker in state.attackers) {
            if (attacker.isDefeated.value || !attacker.type.isSpider()) continue
            if (attacker.position.value in webPositions) {
                attacker.speedBonus.value = maxOf(attacker.speedBonus.value, SPIDER_WEB_SPEED_BONUS)
            }
        }
    }

    private fun handleMorvathShadowFog(morvath: Attacker) {
        val origin = morvath.position.value
        val guaranteedShadowTiles = listOf(origin) + origin.getHexNeighbors()
        val guaranteedShadowSet = guaranteedShadowTiles.toSet()

        // Remove existing shadow fog from any tiles that now have towers, barricades or traps
        state.fieldEffects.removeAll { it.type == FieldEffectType.SHADOW_FOG && isTileOccupiedByStaticObject(it.position) }

        guaranteedShadowTiles
            .filter(::isWithinBounds)
            .filter { !isTileOccupiedByStaticObject(it) }
            .forEach { pos ->
                refreshShadowFogAt(pos, morvath.id)
            }

        // Ranged candidates: exclude already-shadowed tiles, static objects, and guaranteed set
        val rangedCandidates =
            origin
                .getHexNeighborsWithinRadius(
                    SHADOW_FOG_MAX_RANGE,
                    state.level.gridWidth,
                    state.level.gridHeight,
                ).filter { it !in guaranteedShadowSet }
                .filter(::isWithinBounds)
                .filter { !isTileOccupiedByStaticObject(it) }
                .filter { pos -> state.fieldEffects.none { it.type == FieldEffectType.SHADOW_FOG && it.position == pos } }

        val extraTarget =
            rangedCandidates.maxWithOrNull(
                compareBy<Position> { shadowFogPriorityScore(it, origin) }
                    .thenByDescending { it.y }
                    .thenByDescending { it.x },
            )
        if (extraTarget != null) {
            // Fog is NOT applied here — it will be applied after the orb animation completes.
            if (state.morvathShadowOrbEffects.none { it.sourcePosition == origin }) {
                state.morvathShadowOrbEffects.add(
                    MorvathShadowOrbEffect(
                        sourcePosition = origin,
                        targetPosition = extraTarget,
                        turnNumber = state.turnNumber.value,
                        attackerId = morvath.id,
                    ),
                )
            }
        }
    }

    /** Apply fog to all pending Morvath orb targets (called after the orb animation completes). */
    fun applyPendingMorvathFog() {
        state.morvathShadowOrbEffects.forEach { effect ->
            refreshShadowFogAt(effect.targetPosition, effect.attackerId)
        }
    }

    private fun shadowFogPriorityScore(
        position: Position,
        origin: Position,
    ): Int {
        val isOnPath = state.level.isOnPath(position) || state.level.isSpawnPoint(position) || state.level.isRiverTile(position)
        if (!isOnPath) return -10

        // Prefer tiles closer to the player's target than Morvath — i.e. ahead of him on the path
        val nearestTarget = state.level.targetPositions.minByOrNull { origin.hexDistanceTo(it) }
        val progressScore =
            if (nearestTarget != null) {
                val morvathDistToTarget = origin.hexDistanceTo(nearestTarget)
                val tileDistToTarget = position.hexDistanceTo(nearestTarget)
                morvathDistToTarget - tileDistToTarget // positive = tile is ahead toward target
            } else {
                0
            }

        val hasEnemy = state.attackers.any { !it.isDefeated.value && it.position.value == position }
        return if (hasEnemy) progressScore + 20 else progressScore
    }

    private fun refreshShadowFogAt(
        position: Position,
        sourceAttackerId: Int,
    ) {
        val existingEffect =
            state.fieldEffects.find {
                it.type == FieldEffectType.SHADOW_FOG && it.position == position
            }
        if (existingEffect != null) {
            existingEffect.turnsRemaining = SHADOW_FOG_DURATION_TURNS
            return
        }

        state.fieldEffects.add(
            FieldEffect(
                position = position,
                type = FieldEffectType.SHADOW_FOG,
                damage = 0,
                turnsRemaining = SHADOW_FOG_DURATION_TURNS,
                defenderId = 0,
                attackerId = sourceAttackerId,
            ),
        )
    }

    /**
     * Spawn a demon near the given attacker (1-2 cells away)
     */
    private fun spawnDemonNear(
        summoner: Attacker,
        demonType: AttackerType,
        level: Int,
    ) {
        val summonerPos = summoner.position.value

        // Try to find a free position 1-2 cells away
        val possiblePositions = mutableListOf<Position>()

        // Get positions 1 cell away
        possiblePositions.addAll(summonerPos.getHexNeighbors())

        // Get positions 2 cells away
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }

        // Filter valid positions (on path, not occupied, within bounds)
        val validPositions =
            possiblePositions
                .filter { pos ->
                    pos.x >= 0 &&
                        pos.x < state.level.gridWidth &&
                        pos.y >= 0 &&
                        pos.y < state.level.gridHeight &&
                        state.level.isOnPath(pos) &&
                        !state.isPortalTile(pos) &&
                        !state.attackers.any { it.position.value == pos && !it.isDefeated.value }
                }.distinct()

        if (validPositions.isEmpty()) return

        // Pick a random position
        val spawnPos = validPositions.random()

        // Inherit the summoner's current target so demons follow the same waypoint chain
        val inheritedTarget =
            summoner.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                // Use the first waypoint's next target, not the waypoint position itself
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val demon =
            Attacker(
                id = state.nextAttackerId.value++,
                type = demonType,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(level),
                currentTarget = mutableStateOf(inheritedTarget),
            )
        state.attackers.add(demon)
    }

    /**
     * Spawn an undead (skeleton) near the given attacker
     */
    private fun spawnUndeadNear(
        summoner: Attacker,
        level: Int,
    ) {
        spawnDemonNear(summoner, AttackerType.SKELETON, level)
    }

    /**
     * Red Witch disables towers within [range] hex distance.
     * Disables one tower per turn.
     * Duration: 1 turn base, +1 turn for every 5 levels (level 5=2 turns, level 10=3 turns, etc.)
     * +1 for immediate decrement at end of this turn.
     * An optional [extraDisableTurns] is added when a Coven Synergy villain is nearby.
     * Can only disable towers where tower level <= witch level.
     */
    private fun disableNearestTowerInRange(
        witch: Attacker,
        range: Int,
        extraDisableTurns: Int = 0,
    ) {
        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println(
                "DEBUG: Disabler ${witch.id} level ${witch.level.value} at ${witch.position.value} checking range=$range" +
                    if (extraDisableTurns > 0) " (coven boost +$extraDisableTurns)" else "",
            )
        }

        // Log all towers in the game for debugging
        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println("DEBUG: All towers in game (${state.defenders.size}):")
        }
        state.defenders.forEach { tower ->
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println(
                    "DEBUG:   ${tower.type} id=${tower.id} level=${tower.level.value} at ${tower.position.value} isReady=${tower.isReady} isDisabled=${tower.isDisabled.value}",
                )
            }
        }

        // Find towers within range that:
        // - Are ready (not building)
        // - Are not already disabled
        // - Are within the given hex range
        // - Can be disabled by this witch (tower level <= witch level)
        val eligibleTowers =
            state.defenders.filter { tower ->
                val isReady = tower.isReady
                val notDisabled = !tower.isDisabled.value
                val withinRange =
                    if (range == 1) {
                        witch.position.value
                            .getHexNeighbors()
                            .contains(tower.position.value)
                    } else {
                        witch.position.value.hexDistanceTo(tower.position.value) <= range
                    }
                val canDisable = tower.level.value <= witch.effectiveLevel

                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Checking tower ${tower.type} id=${tower.id}: isReady=$isReady, notDisabled=$notDisabled, withinRange=$withinRange, canDisable=$canDisable",
                    )
                }

                isReady && notDisabled && withinRange && canDisable
            }

        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println("DEBUG: Found ${eligibleTowers.size} eligible towers to disable")
        }

        if (eligibleTowers.isEmpty()) {
            println("DEBUG: Disabler ${witch.id} found no eligible towers to disable")
            return
        }

        // Pick the closest tower (any eligible tower is valid; prefer closest)
        val targetTower =
            eligibleTowers.minByOrNull { tower ->
                witch.position.value.hexDistanceTo(tower.position.value)
            }

        if (targetTower != null) {
            // Calculate disable duration: 1 turn base + 1 per 5 levels
            // +1 to account for immediate decrement at end of this turn
            // Level 1-4: 2 turns (disabled for 1 player turn)
            // Level 5-9: 3 turns (disabled for 2 player turns)
            // Level 10-14: 4 turns (disabled for 3 player turns)
            // Level 20-24: 5 turns (disabled for 4 player turns), etc.
            val disableDuration = 1 + (witch.effectiveLevel / 5) + 1 + extraDisableTurns

            targetTower.isDisabled.value = true
            targetTower.disabledTurnsRemaining.value = disableDuration

            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println(
                    "DEBUG: Disabler ${witch.id} disabled ${targetTower.type} id=${targetTower.id} at ${targetTower.position.value} for $disableDuration turns",
                )
            }
        }
    }

    /**
     * Update tower disable status - decrement timers and re-enable towers
     */
    fun updateTowerDisableStatus() {
        for (tower in state.defenders) {
            if (tower.isDisabled.value) {
                tower.disabledTurnsRemaining.value--
                if (tower.disabledTurnsRemaining.value <= 0) {
                    tower.isDisabled.value = false
                    tower.hasRootGripAnimation.value = false // Clear vine animation when disable expires
                    tower.hasShadowSpewAnimation.value = false // Clear shadow cloud animation when disable expires
                }
            }
        }
    }

    /**
     * Find the nearest active tower for Red Witch to target
     */
    fun findNearestActiveTower(witch: Attacker): Defender? {
        val eligibleTowers =
            state.defenders.filter { tower ->
                tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.effectiveLevel
            }

        if (eligibleTowers.isEmpty()) return null

        return eligibleTowers.minByOrNull { tower ->
            tower.position.value.distanceTo(witch.position.value)
        }
    }

    /**
     * Find a position on the path near the target tower for Red Witch to move towards
     */
    fun findPathPositionNearTower(towerPosition: Position): Position {
        // Find all path positions adjacent to the tower
        val adjacentPathPositions =
            towerPosition.getHexNeighbors().filter { pos ->
                pos.x >= 0 &&
                    pos.x < state.level.gridWidth &&
                    pos.y >= 0 &&
                    pos.y < state.level.gridHeight &&
                    state.level.isOnPath(pos)
            }

        // Return the first adjacent path position, or tower position if none found
        return adjacentPathPositions.firstOrNull() ?: towerPosition
    }

    /**
     * Find the nearest damaged enemy for Green Witch to move towards and heal.
     * Prioritizes Ewhad if he exists and has damaged health.
     */
    fun findHealingTarget(witch: Attacker): Attacker? {
        // First check if Ewhad exists and is damaged
        val ewhad =
            state.attackers.find {
                it.type == AttackerType.EWHAD &&
                    !it.isDefeated.value &&
                    it.currentHealth.value < it.maxHealth
            }

        if (ewhad != null) {
            return ewhad // Always prioritize healing Ewhad
        }

        // Otherwise, find nearest damaged enemy
        val damagedEnemies =
            state.attackers.filter {
                !it.isDefeated.value &&
                    it.id != witch.id &&
                    it.currentHealth.value < it.maxHealth
            }

        if (damagedEnemies.isEmpty()) return null

        return damagedEnemies.minByOrNull { enemy ->
            witch.position.value.distanceTo(enemy.position.value)
        }
    }

    /**
     * Find the nearest tower that is not disabled for Red Witch to move towards and disable.
     * Returns the tower's position if found.
     * Only targets towers that the witch can actually disable (tower level <= witch level).
     */
    fun findTowerTarget(witch: Attacker): Position? {
        // Find ready towers that are not disabled and can be disabled by this witch
        val availableTowers =
            state.defenders.filter { tower ->
                tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.effectiveLevel
            }

        if (availableTowers.isEmpty()) return null

        // Find the closest tower
        val nearestTower =
            availableTowers.minByOrNull { tower ->
                witch.position.value.distanceTo(tower.position.value)
            }

        return nearestTower?.position?.value
    }

    /**
     * Shadow Spew: Xarithon the Shadow Dragon breathes shadowy flames onto a 2×2 area every
     * [AttackerType.shadowSpewCooldown] rounds. All ready towers inside the area are disabled for
     * [AttackerType.shadowSpewDurationTurns] player turns.
     *
     * The 2×2 area is chosen to maximise the number of towers it overlaps. Xarithon does not need
     * to be adjacent to the target — the shadow flames reach any location on the battlefield.
     */
    private fun handleXarithonShadowSpew(xarithon: Attacker) {
        val cooldown = xarithon.type.shadowSpewCooldown ?: return
        val duration = xarithon.type.shadowSpewDurationTurns
        if (duration <= 0) return

        // Decrement (or initialise) the spew cooldown via the shared villain cooldown field.
        if (xarithon.villainCooldown.value > 0) {
            xarithon.villainCooldown.value--
            return
        }

        // Cooldown has expired — activate Shadow Spew.
        xarithon.villainCooldown.value = cooldown

        // Find the best 2×2 area: pick the top-left corner of whichever 2×2 block contains the
        // most ready, non-disabled towers.
        val readyTowers =
            state.defenders.filter { tower ->
                tower.isReady && !tower.isDisabled.value
            }
        if (readyTowers.isEmpty()) return

        // Enumerate candidate top-left corners using the positions of existing towers, then pick
        // the corner whose 2×2 square covers the most towers.
        val bestCorner =
            readyTowers
                .flatMap { tower ->
                    val (tx, ty) = tower.position.value
                    // A tower at (tx, ty) is inside a 2×2 block whose top-left is any of:
                    // (tx, ty), (tx-1, ty), (tx, ty-1), (tx-1, ty-1)
                    listOf(
                        Position(tx, ty),
                        Position(tx - 1, ty),
                        Position(tx, ty - 1),
                        Position(tx - 1, ty - 1),
                    )
                }.distinct()
                .maxByOrNull { corner ->
                    val (cx, cy) = corner
                    val affected =
                        setOf(
                            Position(cx, cy),
                            Position(cx + 1, cy),
                            Position(cx, cy + 1),
                            Position(cx + 1, cy + 1),
                        )
                    readyTowers.count { t -> t.position.value in affected }
                } ?: return

        // Disable all ready towers inside the chosen 2×2 area.
        val (cx, cy) = bestCorner
        val spewArea =
            setOf(
                Position(cx, cy),
                Position(cx + 1, cy),
                Position(cx, cy + 1),
                Position(cx + 1, cy + 1),
            )
        // +1 accounts for the immediate decrement at the end of this enemy turn.
        val adjustedDuration = duration + 1
        var anyDisabled = false
        for (tower in state.defenders) {
            if (tower.isReady && !tower.isDisabled.value && tower.position.value in spewArea) {
                tower.isDisabled.value = true
                tower.disabledTurnsRemaining.value = adjustedDuration
                tower.hasShadowSpewAnimation.value = true // Show shadow cloud while disabled
                anyDisabled = true
                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Xarithon Shadow Spew disabled ${tower.type} id=${tower.id} at " +
                            "${tower.position.value} for $duration turns",
                    )
                }
            }
        }

        // Trigger the shadow fireball flying animation from Xarithon to the target area center.
        if (anyDisabled) {
            val targetCenter = Position(cx, cy)
            if (state.shadowSpewEffects.none { it.sourcePosition == xarithon.position.value }) {
                state.shadowSpewEffects.add(
                    ShadowSpewEffect(
                        sourcePosition = xarithon.position.value,
                        targetPosition = targetCenter,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }
    }

    /**
     * Broadside (Cap'n Roderich): every [AttackerType.broadsideCooldown] rounds Roderich fires a
     * heavy cannonball at the nearest barge (raft-mounted tower). The barge is immediately sunk —
     * no coins are refunded to the player. Instead, the tower's total build cost is added to
     * Roderich's personal treasure chest ([Attacker.treasureCoins]).
     *
     * The ballista attack animation is reused for the cannonball visual.
     */
    private fun handleRoderichBroadside(roderich: Attacker) {
        val cooldown = roderich.type.broadsideCooldown ?: return
        if (roderich.villainCooldown.value > 0) {
            roderich.villainCooldown.value--
            return
        }

        // Calculate broadside range: 9 + level
        val broadsideRange = 9 + roderich.level.value

        // Find the nearest barge (any defender mounted on a raft) within range.
        val nearestBarge =
            state.rafts
                .filter { raft -> raft.isActive }
                .mapNotNull { raft ->
                    val defender = state.defenders.find { it.id == raft.defenderId } ?: return@mapNotNull null
                    val distance = roderich.position.value.hexDistanceTo(raft.currentPosition.value)
                    if (distance <= broadsideRange) {
                        Pair(raft, defender)
                    } else {
                        null
                    }
                }.minByOrNull { (raft, _) ->
                    roderich.position.value.hexDistanceTo(raft.currentPosition.value)
                }

        if (nearestBarge != null) {
            val (raft, defender) = nearestBarge
            val bargePosition = raft.currentPosition.value

            // Fire the cannonball animation (reuse ballista effect).
            state.ballistaAttackEffects.add(
                BallistaAttackEffect(
                    sourcePosition = roderich.position.value,
                    targetPosition = bargePosition,
                    turnNumber = state.turnNumber.value,
                ),
            )

            // Queue the barge for deletion after the animation completes.
            // The barge removal is deferred to allow the cannonball animation to play.
            state.pendingBargeDeletions.add(
                PendingBargeDeletion(
                    raftId = raft.id,
                    defenderId = defender.id,
                    towerCost = defender.totalCost,
                    bargePosition = bargePosition,
                ),
            )

            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println(
                    "DEBUG: Roderich Broadside fired at ${defender.type} id=${defender.id} at $bargePosition; " +
                        "range=$broadsideRange (9 + level ${roderich.level.value})",
                )
            }

            // Set cooldown since broadside was fired
            roderich.villainCooldown.value = cooldown
        }
        // If no barge in range, cannonball stays ready (don't set cooldown, don't fire)
    }

    /**
     * Gold Treasure passive (Cap'n Roderich): each enemy turn, Roderich loots [AttackerType.coinsPerTurn]
     * coins and stashes them in his treasure chest ([Attacker.treasureCoins]). The full treasure
     * is awarded to the player when Roderich is eventually defeated.
     */
    private fun handleRoderichCoinGain(roderich: Attacker) {
        val gain = roderich.type.coinsPerTurn
        if (gain <= 0) return
        roderich.treasureCoins.value += gain

        // Show a coin gain animation at Roderich's position to signal the looting.
        state.coinGainEffects.add(
            CoinGainEffect(
                position = roderich.position.value,
                amount = gain,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    /**
     * Process pending barge deletions from Roderich's Broadside attacks.
     * Called after animation effects have been displayed to clean up the barges.
     */
    fun processPendingBargeDeletions() {
        for (deletion in state.pendingBargeDeletions) {
            // Find and destroy the raft
            val raft = state.rafts.find { it.id == deletion.raftId }
            if (raft != null) {
                raft.isDestroyed.value = true
            }

            // Remove the defender
            val defender = state.defenders.find { it.id == deletion.defenderId }
            if (defender != null) {
                state.defenders.remove(defender)
            }

            // Find Roderich and add treasure (he should exist since this was queued from his broadside)
            val roderich = state.attackers.find { it.type == AttackerType.CAPTAIN_RODERICH && !it.isDefeated.value }
            if (roderich != null) {
                roderich.treasureCoins.value += deletion.towerCost

                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Processed barge deletion - sank tower at ${deletion.bargePosition}, " +
                            "treasure now ${roderich.treasureCoins.value}",
                    )
                }
            }
        }

        state.pendingBargeDeletions.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // The Kraken
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dive tick: called every enemy turn for The Kraken.
     *
     * While diving, the Kraken is invisible and unattackable. Each enemy turn the remaining dive
     * counter decrements. When it reaches zero the Kraken re-surfaces.
     */
    private fun handleKrakenDiveTick(kraken: Attacker) {
        if (!kraken.isDiving.value) return
        kraken.diveTurnsRemaining.value--
        if (kraken.diveTurnsRemaining.value <= 0) {
            kraken.isDiving.value = false
            kraken.diveTurnsRemaining.value = 0
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Kraken resurfaces at ${kraken.position.value}")
            }
        }
    }

    /**
     * Main Kraken ability dispatch: called each enemy turn (after the dive tick).
     *
     * Priority order:
     * 1. If currently diving → skip all other abilities.
     * 2. If a barge grip is active (phase 1 or 2) → advance the grip.
     * 3. If the barge-grip cooldown has expired → look for a barge to grip.
     * 4. If the dive cooldown ([Attacker.summonCooldown]) has expired → trigger a dive.
     */
    private fun handleKrakenAbilities(kraken: Attacker) {
        if (kraken.isDiving.value) return // Submerged — do nothing

        // ── Barge Grip ──────────────────────────────────────────────────────
        when (kraken.bargeGripPhase.value) {
            1 -> {
                // Phase 1 → Phase 2: drag the barge under and sink it
                val raftId = kraken.grippedRaftId.value
                if (raftId != null) {
                    sinkKrakenGrippedBarge(kraken, raftId)
                }
                kraken.bargeGripPhase.value = 0
                kraken.grippedRaftId.value = null
                val cooldown = kraken.type.bargeGripCooldown ?: 4
                kraken.villainCooldown.value = cooldown
                return
            }
            2 -> {
                // Should not reach phase 2 via this path; treat as complete
                kraken.bargeGripPhase.value = 0
                kraken.grippedRaftId.value = null
                return
            }
        }

        // ── Barge Grip cooldown ─────────────────────────────────────────────
        if (kraken.villainCooldown.value > 0) {
            kraken.villainCooldown.value--
        }

        if (kraken.villainCooldown.value == 0 && kraken.bargeGripPhase.value == 0) {
            val range = kraken.type.bargeGripRange
            val nearestBarge =
                state.rafts
                    .filter { it.isActive }
                    .filter { raft ->
                        kraken.position.value.hexDistanceTo(raft.currentPosition.value) <= range
                    }.minByOrNull { raft ->
                        kraken.position.value.hexDistanceTo(raft.currentPosition.value)
                    }

            if (nearestBarge != null) {
                // Grip phase 1: lock the barge (tower cannot be sold)
                kraken.grippedRaftId.value = nearestBarge.id
                kraken.bargeGripPhase.value = 1

                // Mark the defender on this raft as gripped so the UI can disable selling
                val grippedDefender = state.defenders.find { it.raftId.value == nearestBarge.id }
                grippedDefender?.isGrippedByKraken?.value = true

                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Kraken grips raft ${nearestBarge.id} at ${nearestBarge.currentPosition.value}",
                    )
                }
                return // Gripped this turn — skip dive check
            }
        }

        // ── Dive cooldown / trigger ─────────────────────────────────────────
        if (kraken.type.canDive) {
            if (kraken.summonCooldown.value > 0) {
                kraken.summonCooldown.value--
            }
            if (kraken.summonCooldown.value == 0) {
                kraken.isDiving.value = true
                kraken.diveTurnsRemaining.value = KRAKEN_DIVE_DURATION
                kraken.summonCooldown.value = KRAKEN_DIVE_COOLDOWN
                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println("DEBUG: Kraken dives at ${kraken.position.value}")
                }
            }
        }
    }

    /**
     * Sink a barge that the Kraken has dragged under (end of grip phase 1).
     *
     * The raft is destroyed and the tower on it is removed with no coin refund.
     * Any [isGrippedByKraken] flag on the defender is also cleared.
     */
    private fun sinkKrakenGrippedBarge(
        kraken: Attacker,
        raftId: Int,
    ) {
        val raft = state.rafts.find { it.id == raftId } ?: return
        val defender = state.defenders.find { it.raftId.value == raftId }

        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println(
                "DEBUG: Kraken sinks raft $raftId at ${raft.currentPosition.value}" +
                    (defender?.let { "; tower ${it.type} id=${it.id}" } ?: ""),
            )
        }

        // Destroy the raft
        raft.isDestroyed.value = true

        // Remove the tower (no refund to player)
        if (defender != null) {
            defender.isGrippedByKraken.value = false
            state.defenders.remove(defender)
        }

        // Visual: reuse the ballista-attack animation as a "drag under" splash effect
        state.ballistaAttackEffects.add(
            BallistaAttackEffect(
                sourcePosition = kraken.position.value,
                targetPosition = raft.currentPosition.value,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    // ------------------------------------------------------------------ Zythar the Riftcaller ---

    /**
     * Zythar summons blue and red demons plus demonling scouts every 3 enemy turns.
     */
    private fun handleZytharSummon(zythar: Attacker) {
        if (zythar.summonCooldown.value > 0) return
        val level = zythar.level.value
        // Summon blue demons, red demons, and demonlings
        repeat(level) { spawnDemonNear(zythar, AttackerType.BLUE_DEMON, level) }
        repeat(maxOf(1, level / 2)) { spawnDemonNear(zythar, AttackerType.RED_DEMON, level) }
        repeat(2 + level) { spawnDemonlingNear(zythar) }
        zythar.summonCooldown.value = 3
    }

    /**
     * Spawn a single demonling scout near [zythar] on a free path tile.
     */
    private fun spawnDemonlingNear(zythar: Attacker) {
        val summonerPos = zythar.position.value
        val possiblePositions = mutableListOf<Position>()
        possiblePositions.addAll(summonerPos.getHexNeighbors())
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }
        val validPositions =
            possiblePositions
                .filter { pos ->
                    pos.x >= 0 &&
                        pos.x < state.level.gridWidth &&
                        pos.y >= 0 &&
                        pos.y < state.level.gridHeight &&
                        state.level.isOnPath(pos) &&
                        !state.attackers.any { it.position.value == pos && !it.isDefeated.value }
                }.distinct()
        if (validPositions.isEmpty()) return
        val spawnPos = validPositions.random()
        val inheritedTarget =
            zythar.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints.first().nextTarget
            } else {
                state.level.targetPositions.first()
            }
        val demonling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.DEMONLING,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(zythar.level.value),
                currentTarget = mutableStateOf(inheritedTarget),
            )
        state.attackers.add(demonling)
        state.enemySpawnEffects.add(
            EnemySpawnEffect(
                position = spawnPos,
                turnNumber = state.turnNumber.value,
                attackerType = AttackerType.DEMONLING,
                suppressPortalAnimation = true,
            ),
        )
    }

    /**
     * Called after every demonling movement step to check whether the demonling should sacrifice
     * itself to open a rift portal.
     *
     * Portal creation triggers when the demonling's distance to the nearest target is:
     * - at most [Portal.PORTAL_NEAR_TARGET_DISTANCE], OR
     * - at least [Portal.PORTAL_ADVANCE_THRESHOLD_WHEN_PORTAL_EXISTS] closer than the closest
     *   existing portal exit (or [Portal.PORTAL_ADVANCE_THRESHOLD_INITIAL] closer than Zythar
     *   when no portal exists yet).
     */
    fun checkAndCreatePortalForDemonling(demonling: Attacker) {
        if (demonling.isDefeated.value) return
        if (demonling.type != AttackerType.DEMONLING) return

        val demonlingPos = demonling.position.value
        val activeTargets = state.getActiveTargetPositions()
        val demonlingDist =
            activeTargets.minOfOrNull { demonlingPos.hexDistanceTo(it) } ?: return

        // Find the reference distance: min dist-to-target for all existing portal exits and Zythar
        val zythar = state.attackers.firstOrNull { !it.isDefeated.value && it.type == AttackerType.ZYTHAR_THE_RIFTCALLER }
        val portalExitDists = state.activePortals.map { portal ->
            activeTargets.minOfOrNull { portal.exitPosition.hexDistanceTo(it) } ?: Int.MAX_VALUE
        }
        val referenceDist: Int =
            if (zythar != null) {
                val zytharDist = activeTargets.minOfOrNull { zythar.position.value.hexDistanceTo(it) } ?: Int.MAX_VALUE
                (portalExitDists + listOf(zytharDist)).min()
            } else if (portalExitDists.isNotEmpty()) {
                portalExitDists.min()
            } else {
                return // No Zythar and no portals — cannot create new portal
            }
        val advanceThreshold =
            if (portalExitDists.isNotEmpty()) {
                Portal.PORTAL_ADVANCE_THRESHOLD_WHEN_PORTAL_EXISTS
            } else {
                Portal.PORTAL_ADVANCE_THRESHOLD_INITIAL
            }

        val shouldCreatePortal =
            demonlingDist <= Portal.PORTAL_NEAR_TARGET_DISTANCE ||
                demonlingDist <= referenceDist - advanceThreshold

        if (!shouldCreatePortal) return

        // Find a free path tile adjacent to Zythar for the portal entry
        // (not a spawn point, not a target tile, and no existing portal entry there)
        val zytharPos = zythar?.position?.value ?: return
        val entryPosition =
            zytharPos.getHexNeighbors().firstOrNull { candidate ->
                state.level.isOnPath(candidate) &&
                    !state.level.isSpawnPoint(candidate) &&
                    !state.level.isTargetPosition(candidate) &&
                    !state.isPortalTile(candidate) &&
                    !state.attackers.any { it.position.value == candidate && !it.isDefeated.value }
            } ?: return

        // The exit must also not be on a spawn point or target tile
        if (state.level.isSpawnPoint(demonlingPos) || state.level.isTargetPosition(demonlingPos) || state.isPortalTile(demonlingPos)) return

        // Create the portal
        val portalId = state.nextPortalId.value++
        val portal =
            Portal(
                id = portalId,
                entryPosition = entryPosition,
                exitPosition = demonlingPos,
                villainId = zythar.id,
                runeIndex = portalId % Portal.RUNE_POOL_SIZE,
            )
        state.activePortals.add(portal)

        // The demonling is consumed by the ritual
        demonling.wasMerged.value = true // suppress reward
        demonling.isDefeated.value = true
    }
}
