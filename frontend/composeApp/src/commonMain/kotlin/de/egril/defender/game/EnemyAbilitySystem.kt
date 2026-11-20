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
    random: Random = Random.Default,
) {
    private val bridgeSystem = BridgeSystem(state)
    private val barricadeSystem = BarricadeSystem(state)
    private val krakenAndBarge = KrakenAndBargeAbilities(state)
    private val zytharPortal = ZytharPortalAbilities(state)
    private val summon = SummonAbilities(state, zytharPortal)
    private val garokk = GarokkAbilities(state)
    private val runemaster = RunemasterAbilities(state, barricadeSystem)
    private val razzik = RazzikAbilities(state, barricadeSystem, bridgeSystem, random)
    private val villainTower = VillainTowerAbilities(state)
    private val swarm = SwarmAbilities(state, isWithinBounds = ::isWithinBounds, isTileOccupiedByStaticObject = ::isTileOccupiedByStaticObject)
    private val silas = SilasAbilities(state, isTileOccupiedByStaticObject = ::isTileOccupiedByStaticObject)
    private val morvath =
        MorvathAbilities(
            state,
            isWithinBounds = ::isWithinBounds,
            isTileOccupiedByStaticObject = ::isTileOccupiedByStaticObject,
        )
    private val spider =
        SpiderAbilities(
            state,
            swarm = swarm,
            isTileOccupiedByStaticObject = ::isTileOccupiedByStaticObject,
            maxSwarmSpawnSearchRings = MAX_SWARM_SPAWN_SEARCH_RINGS,
        )
    private val baron =
        BaronAbilities(
            state = state,
            isWithinBounds = ::isWithinBounds,
            isTileOccupiedByStaticObject = ::isTileOccupiedByStaticObject,
        )
    private val xarithon = XarithonAbilities(state)
    private val morguk = MorgukAbilities(state)
    private val witch = WitchAbilities(state, pathfinding)
    private val villainAuras =
        VillainAuraAbilities(
            state,
            applyCovenHealBoost = witch::applyCovenHealBoost,
            applyCovenDisableBoost = witch::applyCovenDisableBoost,
        )
    private val snotlings =
        SnotlingAbilities(
            state = state,
            swarm = swarm,
            isWithinBounds = ::isWithinBounds,
            maxSnotlingsPerTile = MAX_SNOTLINGS_PER_TILE,
            maxSwarmSpawnSearchRings = MAX_SWARM_SPAWN_SEARCH_RINGS,
        )
    private val snotlingCannon =
        SnotlingCannonAbilities(
            state = state,
            pathfinding = pathfinding,
            maxSnotlingsPerTile = MAX_SNOTLINGS_PER_TILE,
            getOrCreateSnotlingStack = snotlings::getOrCreateSnotlingStack,
        )

    companion object {
        private const val MAX_SWARM_SPAWN_SEARCH_RINGS = 5
        private const val MAX_SNOTLINGS_PER_TILE = 250
    }

    fun processSnotlingGrowth() = snotlings.processSnotlingGrowth()

    fun processHordeEating() = snotlings.processHordeEating()

    fun processEnemyAbilities() {
        garokk.handleGarokkWarCry()
        garokk.applyHordeMomentumWaagh()

        // Create a snapshot of attackers to avoid ConcurrentModificationException
        // when spawning new demons during iteration
        val attackersSnapshot = state.attackers.toList()

        // Villain auras are recomputed from scratch each enemy turn, so clear last turn's bonuses.
        for (attacker in attackersSnapshot) {
            attacker.speedBonus.value = 0
        }
        witch.covenEnhancedHealWitchIds.clear()
        witch.covenEnhancedDisableWitchIds.clear()
        villainAuras.processVillainAuras(attackersSnapshot)

        for (attacker in attackersSnapshot) {
            if (attacker.isDefeated.value) continue

            // Decrement summon cooldown
            if (attacker.summonCooldown.value > 0) {
                attacker.summonCooldown.value--
            }

            when (attacker.type) {
                AttackerType.EVIL_WIZARD, AttackerType.RUNEMASTER -> {
                    summon.handleSummon(
                        attacker,
                        state,
                        blueDemons = attacker.level.value,
                        redDemons = attacker.level.value / 2,
                        fixedLevel = attacker.level.value, // demons spawn at same level as wizard for more consistent scaling
                    )
                    if (attacker.type == AttackerType.RUNEMASTER) {
                        runemaster.handleRunemasterArcaneBlast(attacker)
                    }
                }
                AttackerType.EWHAD -> {
                    summon.handleSummon(
                        attacker,
                        state,
                        blueDemons = attacker.level.value * 2,
                        redDemons = attacker.level.value,
                        undead = 3,
                    )
                }
                AttackerType.GRAND_RUNEMASTER_VAELEN ->
                    villainTower.handleVaelenRunicSurge(attacker)
                AttackerType.GREEN_WITCH -> {
                    val abilityUses = if (attacker.mushroomTurnsRemaining.value > 0) 2 else 1
                    repeat(abilityUses) {
                        witch.applyGreenWitchHealing(attacker, enhanced = attacker.id in witch.covenEnhancedHealWitchIds)
                    }
                }

                AttackerType.RED_WITCH -> {
                    val abilityUses = if (attacker.mushroomTurnsRemaining.value > 0) 2 else 1
                    repeat(abilityUses) {
                        // Disable nearby tower within range 1, with optional coven disable boost
                        witch.disableNearestTowerInRange(
                            witch = attacker,
                            range = 1,
                            extraDisableTurns = if (attacker.id in witch.covenEnhancedDisableWitchIds) WitchAbilities.COVEN_DISABLE_EXTRA_TURNS else 0,
                        )
                    }
                }
                AttackerType.SNOTLING_BOSS -> {
                    // Snotling Rally: summon a rabble of weak snotlings around Gribnak
                    snotlings.handleSnotlingRally(attacker)
                }
                AttackerType.SNOTLING -> {
                    snotlingCannon.handleSnotlingCannon(attacker)
                }
                AttackerType.MORGUK_BONEWHISPER -> {
                    // Spirit Summon: spawn goblins on all adjacent path tiles every 3 turns
                    morguk.handleMorgukSpiritSummon(attacker)
                    // Hex of Silence: disable an adjacent defender
                    witch.disableNearestTowerInRange(attacker, range = 1)
                }
                AttackerType.ARAXXA -> {
                    spider.handleAraxxaWeb(attacker)
                    spider.handleAraxxaSpiderlings(attacker)
                }
                AttackerType.BARON_RATTERZAHN -> {
                    baron.handleBaronRatterzahn(attacker)
                }
                AttackerType.SILAS_THE_MASKMASTER -> {
                    silas.handleSilasMirrorImages(attacker)
                }
                AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> {
                    // Green-witch ability: heal adjacent units with optional coven boost
                    witch.applyGreenWitchHealing(attacker, enhanced = attacker.id in witch.covenEnhancedHealWitchIds)
                    // Red-witch ability: disable nearest tower within range 3
                    witch.disableNearestTowerInRange(
                        witch = attacker,
                        range = 3,
                        extraDisableTurns = if (attacker.id in witch.covenEnhancedDisableWitchIds) WitchAbilities.COVEN_DISABLE_EXTRA_TURNS else 0,
                    )
                    // Coven Swap: every 5 rounds, swap places with a witch within range 3
                    if (attacker.summonCooldown.value == 0) {
                        val swapCooldown = attacker.type.covenSwapCooldown
                        if (swapCooldown != null) {
                            witch.handleCovenSwap(attacker, range = 3)
                            attacker.summonCooldown.value = swapCooldown
                        }
                    }
                }
                AttackerType.HAGA -> {
                    // All green-witch abilities with optional coven heal boost
                    witch.applyGreenWitchHealing(attacker, enhanced = attacker.id in witch.covenEnhancedHealWitchIds)
                }
                AttackerType.ZUSSA -> {
                    // All red-witch abilities with optional coven disable boost
                    witch.disableNearestTowerInRange(
                        witch = attacker,
                        range = 1,
                        extraDisableTurns = if (attacker.id in witch.covenEnhancedDisableWitchIds) WitchAbilities.COVEN_DISABLE_EXTRA_TURNS else 0,
                    )
                }
                AttackerType.SYLVANAS_THE_MOLDING -> {
                    // Root Grip: every 3 rounds, thorny vines block a tower within range 3 for 2 player turns
                    villainTower.handleSylvanasRootGrip(attacker)
                    // Self-Healing: restore selfHealPerTurn HP each enemy turn (handled below)
                }
                AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> {
                    // Time Loop: every 2 rounds, slow time within radius 5 — all towers skip a round
                    villainTower.handleMalakorTimeLoop(attacker)
                }
                AttackerType.IGNIS_VA_THE_DRAGONVOICE -> {
                    // Call of the Brood: every 3 rounds, summon two Dragon-Terrors
                    villainTower.handleIgnisVaCallOfTheBrood(attacker)
                }
                AttackerType.MORVATH_THE_SHADOWMASTER -> {
                    morvath.handleMorvathShadowFog(attacker)
                }
                AttackerType.XARITHON_THE_SHADOW_DRAGON -> {
                    // Shadow Spew: every 3 rounds, dark flames erupt in a 2×2 area disabling towers
                    xarithon.handleXarithonShadowSpew(attacker)
                }
                AttackerType.CAPTAIN_RODERICH -> {
                    // Broadside: every 3 rounds, fire a cannonball at the nearest barge, sinking it
                    krakenAndBarge.handleRoderichBroadside(attacker)
                    // Gold Treasure: accumulate coins each enemy turn
                    krakenAndBarge.handleRoderichCoinGain(attacker)
                }
                AttackerType.THE_KRAKEN -> {
                    // Dive tick: decrement dive timer and re-surface when it expires
                    krakenAndBarge.handleKrakenDiveTick(attacker)
                    // Barge Grip: seize and sink adjacent barges; also trigger periodic dives
                    krakenAndBarge.handleKrakenAbilities(attacker)
                }
                AttackerType.ZYTHAR_THE_RIFTCALLER -> {
                    // Summon: every 3 turns spawn blue/red demons and demonling scouts
                    summon.handleZytharSummon(attacker)
                }
                AttackerType.RAZZIK_THE_ROCKETEER -> {
                    razzik.handleRazzikRockets(attacker)
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

        spider.applySpiderWebBonuses()
    }

    fun processPendingSnotlingCannonArrivals() = snotlingCannon.processPendingSnotlingCannonArrivals()

    private fun isWithinBounds(position: Position): Boolean =
        position.x >= 0 &&
            position.x < state.level.gridWidth &&
            position.y >= 0 &&
            position.y < state.level.gridHeight

    private fun isTileOccupiedByStaticObject(position: Position): Boolean =
        state.defenders.any { it.position.value == position } ||
            state.barricades.any { it.position == position && !it.isDestroyed() } ||
            state.traps.any { it.position == position } ||
            state.fiefs.any { it.position == position } ||
            state.isInactiveGargoyleAt(position)

    /**
     * Update tower disable status - decrement timers and re-enable towers.
     * Delegates to [WitchAbilities].
     */
    fun updateTowerDisableStatus() = witch.updateTowerDisableStatus()

    /**
     * Find the nearest damaged enemy for Green Witch to move towards and heal.
     * Delegates to [WitchAbilities].
     */
    fun findHealingTarget(witch: Attacker): Attacker? = this.witch.findHealingTarget(witch)

    /**
     * Find the nearest tower for Red Witch to move towards and disable.
     * Delegates to [WitchAbilities].
     */
    fun findTowerTarget(witch: Attacker): Position? = this.witch.findTowerTarget(witch)

    /** Processes queued barge deletions from Roderich's Broadside attacks. */
    fun processPendingBargeDeletions() = krakenAndBarge.processPendingBargeDeletions()

    /** Apply fog to all pending Morvath orb targets (called after the orb animation completes). */
    fun applyPendingMorvathFog() = morvath.applyPendingMorvathFog()

    // ------------------------------------------------------------------ Zythar the Riftcaller ---

    /**
     * Zythar summons blue and red demons plus demonling scouts every 3 enemy turns.
     */

    /**
     * Called after every demonling movement step to check whether it opens a rift portal.
     * Delegates to [ZytharPortalAbilities].
     */
    fun checkAndCreatePortalForDemonling(demonling: Attacker) = zytharPortal.checkAndCreatePortalForDemonling(demonling)
}
