package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import kotlin.math.min

/**
 * Handles special enemy abilities like summoning demons, healing, disabling towers, and building bridges.
 */
class EnemyAbilitySystem(
    private val state: GameState,
) {
    private val bridgeSystem = BridgeSystem(state)

    companion object {
        private const val WEB_DURATION_TURNS = 10
        private const val SPIDER_WEB_SPEED_BONUS = 1
        private const val MAX_SWARM_SPAWN_SEARCH_RINGS = 5
        private const val BARON_SCRAP_BOT_COUNT = 2

        /** Heal multiplier applied to green witches empowered by the Coven Synergy aura (50 % extra). */
        private const val COVEN_HEAL_BOOST_MULTIPLIER = 1.5f

        /** Extra disable rounds granted to red witches empowered by the Coven Synergy aura. */
        private const val COVEN_DISABLE_EXTRA_TURNS = 1
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

    fun processEnemyAbilities() {
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
                    applyGreenWitchHealing(attacker, enhanced = attacker.id in covenEnhancedHealWitchIds)
                }
                AttackerType.RED_WITCH -> {
                    // Disable nearby tower within range 1, with optional coven disable boost
                    disableNearestTowerInRange(
                        witch = attacker,
                        range = 1,
                        extraDisableTurns = if (attacker.id in covenEnhancedDisableWitchIds) COVEN_DISABLE_EXTRA_TURNS else 0,
                    )
                }
                AttackerType.SNOTLING_BOSS -> {
                    // Snotling Rally: summon a rabble of weak snotlings around Gribnak
                    handleSnotlingRally(attacker)
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
        }

        applySpiderWebBonuses()
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
     * Apply villain aura abilities (issue #538). A villain buffs friendly units of its own faction
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
                val baseHeal = witch.level.value * 5
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
        val witchTypes = setOf(AttackerType.GREEN_WITCH, AttackerType.RED_WITCH, AttackerType.HAGA, AttackerType.ZUSSA)
        val target =
            state.attackers
                .filter { ally ->
                    !ally.isDefeated.value &&
                        ally.id != sybilla.id &&
                        ally.type in witchTypes &&
                        sybillaPos.hexDistanceTo(ally.position.value) <= range
                }.minByOrNull { ally -> sybillaPos.hexDistanceTo(ally.position.value) }

        if (target != null) {
            val targetPos = target.position.value
            target.position.value = sybillaPos
            sybilla.position.value = targetPos
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Sybilla ${sybilla.id} swapped with ${target.type} ${target.id} ($sybillaPos <-> $targetPos)")
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
                level = 1,
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
            state.traps.any { it.position == position }

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
                val canDisable = tower.level.value <= witch.level.value

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
            val disableDuration = 1 + (witch.level.value / 5) + 1 + extraDisableTurns

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
                tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.level.value
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
                tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.level.value
            }

        if (availableTowers.isEmpty()) return null

        // Find the closest tower
        val nearestTower =
            availableTowers.minByOrNull { tower ->
                witch.position.value.distanceTo(tower.position.value)
            }

        return nearestTower?.position?.value
    }
}
