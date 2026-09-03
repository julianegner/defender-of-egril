package de.egril.defender.game

import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.model.*

/**
 * Result of a combat action
 */
data class CombatKillInfo(
    val enemyType: AttackerType,
    val wasUninjured: Boolean = false,
    val usedSupportElement: Boolean = false,
)

data class CombatResult(
    val killsThisAttack: Int = 0,
    val killedEnemyTypes: List<AttackerType> = emptyList(),
    val killInfos: List<CombatKillInfo> = emptyList(),
)

/**
 * Handles combat mechanics including single-target, area, and lasting attacks.
 */
class CombatSystem(
    private val state: GameState,
    private val bridgeSystem: BridgeSystem,
    private val getEffectiveLevel: (Defender) -> Int = { it.level.value },
    private val getEffectiveRange: (Defender) -> Int = { it.range },
) {
    // Track kills this turn for achievements
    private var killsThisTurn = 0
    private var killedTypesThisTurn = mutableListOf<AttackerType>()

    // Callback for combat results (for achievements)
    var onCombatResult: ((CombatResult) -> Unit)? = null

    private val pendingKillInfos = mutableListOf<CombatKillInfo>()

    /**
     * Reset turn counters at start of turn
     */
    fun startTurn() {
        killsThisTurn = 0
        killedTypesThisTurn.clear()
        pendingKillInfos.clear()
    }

    private fun usesSupportElement(defender: Defender): Boolean {
        if (defender.raftId.value != null) return true
        val towerBaseId = defender.towerBaseBarricadeId.value ?: return false
        val barricade = state.barricades.find { it.id == towerBaseId } ?: return false
        return barricade.defenderId < 0
    }

    private fun recordDirectKill(
        defender: Defender,
        target: Attacker,
        targetWasUninjured: Boolean,
    ) {
        pendingKillInfos.add(
            CombatKillInfo(
                enemyType = target.type,
                wasUninjured = targetWasUninjured,
                usedSupportElement = usesSupportElement(defender),
            ),
        )
    }

    fun recordSupportTrapKill(
        attackerType: AttackerType,
        wasUninjured: Boolean,
    ) {
        pendingKillInfos.add(
            CombatKillInfo(
                enemyType = attackerType,
                wasUninjured = wasUninjured,
                usedSupportElement = true,
            ),
        )
    }

    companion object {
        // LASTING damage is applied at half the initial damage per turn
        private const val LASTING_DAMAGE_DIVISOR = 2

        /** Hex-distance radius of the burning tile's tower-disable effect. */
        private const val BURNING_TILE_RANGE = 2

        /** Number of player turns that towers near the burning tile are disabled. */
        private const val BURNING_TILE_DISABLE_TURNS = 2

        /** How many enemy turns the burning tile visual effect remains visible. */
        private const val BURNING_TILE_VISUAL_TURNS = 2
    }

    private fun blindTowerAfterMirrorHit(defender: Defender) {
        val blindDurationTurns = (AttackerType.SILAS_THE_MASKMASTER.mirrorBlindDurationTurns ?: 2) + 1
        defender.isDisabled.value = true
        defender.disabledTurnsRemaining.value = blindDurationTurns
        defender.actionsRemaining.value = 0
        state.pendingMessages.add(GameMessage(type = GameMessageType.SILAS_MIRROR_HIT))
    }

    private fun trackWaaghChargeFromHit(
        target: Attacker,
        damageDealt: Int,
    ) {
        if (damageDealt <= 0) return
        if (target.type == AttackerType.ORK) {
            state.addWaaghPoints(3)
        }
        if (target.type == AttackerType.SNOTLING) {
            state.addWaaghPoints(damageDealt / 5)
        }
    }

    private fun removeHitMirrorImages(
        defender: Defender,
        targets: List<Attacker>,
    ) {
        val hitMirrors = targets.filter { it.type.isMirrorImage && !it.isDefeated.value }
        if (hitMirrors.isEmpty()) return
        hitMirrors.forEach { it.isDefeated.value = true }
        blindTowerAfterMirrorHit(defender)
    }

    /**
     * Calculate effective damage for a defender, accounting for level buffs
     */
    private fun getEffectiveDamage(defender: Defender): Int {
        val effectiveLevel = getEffectiveLevel(defender)
        return defender.type.baseDamage + (effectiveLevel - 1) * 5
    }

    /**
     * Returns the damage dealt by [defender] to [target], applying any target-side passive
     * reductions. Currently this handles Cap'n Roderich's **Seaworthy** passive: attacks from
     * barge-mounted towers (defenders on a raft) deal only 50 % damage to him.
     */
    private fun getEffectiveDamageAgainst(
        defender: Defender,
        target: Attacker,
    ): Int {
        val raw = getEffectiveDamage(defender)
        val reduction = target.type.seaworthyDamageReduction
        if (reduction <= 0f) return raw
        // Seaworthy: only reduce when the attacker is on a barge (raft-mounted).
        val isOnBarge = defender.raftId.value != null
        return if (isOnBarge) (raw * (1f - reduction)).toInt().coerceAtLeast(1) else raw
    }

    /**
     * Returns true if the given position is a valid area-attack target tile:
     * on any enemy-occupiable tile (path, spawn point, river) or a bridge.
     */
    private fun isValidAreaTargetPosition(position: Position): Boolean = state.level.isEnemyOccupiable(position) || state.isBridgeAt(position)

    fun defenderAttack(
        defenderId: Int,
        targetId: Int,
        processDefeated: () -> Unit,
    ): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        val target = state.attackers.find { it.id == targetId && !it.isDefeated.value } ?: return false

        if (!defender.canAttack(target, getEffectiveRange(defender))) return false

        // Mark defender as used
        defender.hasBeenUsed.value = true

        // Play attack sound based on attack type
        val soundEvent =
            when (defender.type.attackType) {
                AttackType.MELEE -> SoundEvent.ATTACK_MELEE
                AttackType.RANGED -> {
                    // Use different sound for ballista
                    if (defender.type == DefenderType.BALLISTA_TOWER) {
                        SoundEvent.ATTACK_BALLISTA
                    } else {
                        SoundEvent.ATTACK_RANGED
                    }
                }
                AttackType.AREA -> SoundEvent.ATTACK_AREA
                AttackType.LASTING -> SoundEvent.ATTACK_LASTING
                AttackType.NONE -> null
            }
        soundEvent?.let { GlobalSoundManager.playSound(it) }

        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> singleTargetAttack(defender, target)
            AttackType.AREA -> areaAttack(defender, target.position.value)
            AttackType.LASTING -> lastingAttack(defender, target.position.value)
            AttackType.NONE -> return false // Mines and special structures can't attack
        }

        // Record attack impact visual effect at the target position (deduplicated per tile per turn)
        // Always increment the monotonic trigger counter so non-targeted tiles can detect
        // each individual attack even when the same tile is targeted more than once.
        state.attackTriggerCount.value++
        if (state.towerAttackEffects.none { it.targetPosition == target.position.value }) {
            state.towerAttackEffects.add(
                TowerAttackEffect(
                    targetPosition = target.position.value,
                    turnNumber = state.turnNumber.value,
                ),
            )
        }

        // Record visual effect on the tower tile for ranged attacks (Bow, Spear)
        // Ballista uses a separate overlay animation (BallistaAttackEffect)
        if (defender.type.attackType == AttackType.RANGED) {
            if (defender.type == DefenderType.BALLISTA_TOWER) {
                if (state.ballistaAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.ballistaAttackEffects.add(
                        BallistaAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = target.position.value,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            } else if (defender.type == DefenderType.BOW_TOWER) {
                if (state.bowAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.bowAttackEffects.add(
                        BowAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = target.position.value,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            } else if (defender.type == DefenderType.SPEAR_TOWER) {
                if (state.spearAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.spearAttackEffects.add(
                        SpearAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = target.position.value,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            }
        }

        // Record pike extend overlay effect for Spike Tower melee attacks
        if (defender.type == DefenderType.SPIKE_TOWER) {
            if (state.pikeAttackEffects.none { it.sourcePosition == defender.position.value }) {
                state.pikeAttackEffects.add(
                    PikeAttackEffect(
                        sourcePosition = defender.position.value,
                        targetPosition = target.position.value,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }

        // Record wizard fireball overlay effect for Wizard Tower area attacks
        if (defender.type == DefenderType.WIZARD_TOWER) {
            if (state.wizardAttackEffects.none { it.sourcePosition == defender.position.value }) {
                state.wizardAttackEffects.add(
                    WizardAttackEffect(
                        sourcePosition = defender.position.value,
                        targetPosition = target.position.value,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }

        // Record alchemy acid vial overlay effect for Alchemy Tower lasting attacks
        if (defender.type == DefenderType.ALCHEMY_TOWER) {
            if (state.alchemyAttackEffects.none { it.sourcePosition == defender.position.value }) {
                state.alchemyAttackEffects.add(
                    AlchemyAttackEffect(
                        sourcePosition = defender.position.value,
                        targetPosition = target.position.value,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }

        if (defender.isDisabled.value) {
            defender.actionsRemaining.value = 0
        } else {
            defender.actionsRemaining.value--
        }

        // Process defeated attackers immediately to give coins
        processDefeated()

        return true
    }

    fun defenderAttackPosition(
        defenderId: Int,
        targetPosition: Position,
        processDefeated: () -> Unit,
    ): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false

        // Check if defender can reach the target position
        val distance = defender.position.value.distanceTo(targetPosition)
        if (distance < defender.type.minRange || distance > getEffectiveRange(defender)) return false
        if (!defender.isReady || defender.actionsRemaining.value <= 0) return false

        // Mark defender as used
        defender.hasBeenUsed.value = true

        // For AOE and DOT attacks, target position must be on the path, a river tile, or a spawn point
        if (defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) {
            if (!state.level.isEnemyOccupiable(targetPosition)) return false
        } else {
            // For single-target attacks, prioritize enemy over bridge at the same position;
            // shadow fog tiles are always valid targets even if the enemy is not visible.
            val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }
            val bridge = state.getBridgeAt(targetPosition)
            val hasShadowFog = state.fieldEffects.any { it.type == FieldEffectType.SHADOW_FOG && it.position == targetPosition }
            if (target == null && (bridge == null || !bridge.isActive) && !hasShadowFog) return false
        }

        // Play attack sound based on attack type
        val soundEvent =
            when (defender.type.attackType) {
                AttackType.MELEE -> SoundEvent.ATTACK_MELEE
                AttackType.RANGED -> {
                    // Use different sound for ballista
                    if (defender.type == DefenderType.BALLISTA_TOWER) {
                        SoundEvent.ATTACK_BALLISTA
                    } else {
                        SoundEvent.ATTACK_RANGED
                    }
                }
                AttackType.AREA -> SoundEvent.ATTACK_AREA
                AttackType.LASTING -> SoundEvent.ATTACK_LASTING
                AttackType.NONE -> null
            }
        soundEvent?.let { GlobalSoundManager.playSound(it) }

        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> {
                // Single target attack - prioritize enemy over bridge
                val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }

                if (target != null) {
                    // Attack enemy (takes priority)
                    singleTargetAttack(defender, target)
                } else {
                    // No enemy, attack bridge if present
                    val bridge = state.getBridgeAt(targetPosition)
                    if (bridge != null && bridge.isActive) {
                        bridgeSystem.damageBridge(targetPosition, getEffectiveDamage(defender))
                    }
                    // If targeting a shadow fog tile with no enemy/bridge, the action is
                    // consumed but the attack misses (the tile was targeted blind).
                }
            }
            AttackType.AREA -> {
                // Area attack affects both enemies AND bridges in range
                areaAttack(defender, targetPosition)
                // Also damage bridge at target position if present
                val bridge = state.getBridgeAt(targetPosition)
                if (bridge != null && bridge.isActive) {
                    bridgeSystem.damageBridge(targetPosition, getEffectiveDamage(defender))
                }
            }
            AttackType.LASTING -> lastingAttack(defender, targetPosition)
            AttackType.NONE -> return false // Mines and special structures can't attack
        }

        // Record attack impact visual effect at the target position (deduplicated per tile per turn)
        // Always increment the monotonic trigger counter so non-targeted tiles can detect
        // each individual attack even when the same tile is targeted more than once.
        state.attackTriggerCount.value++
        if (state.towerAttackEffects.none { it.targetPosition == targetPosition }) {
            state.towerAttackEffects.add(
                TowerAttackEffect(
                    targetPosition = targetPosition,
                    turnNumber = state.turnNumber.value,
                ),
            )
        }

        // Record visual effect on the tower tile for ranged attacks (Bow, Spear)
        // Ballista uses a separate overlay animation (BallistaAttackEffect)
        if (defender.type.attackType == AttackType.RANGED) {
            if (defender.type == DefenderType.BALLISTA_TOWER) {
                if (state.ballistaAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.ballistaAttackEffects.add(
                        BallistaAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = targetPosition,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            } else if (defender.type == DefenderType.BOW_TOWER) {
                if (state.bowAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.bowAttackEffects.add(
                        BowAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = targetPosition,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            } else if (defender.type == DefenderType.SPEAR_TOWER) {
                if (state.spearAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.spearAttackEffects.add(
                        SpearAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = targetPosition,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            }
        }

        // Record pike extend overlay effect for Spike Tower melee attacks
        if (defender.type == DefenderType.SPIKE_TOWER) {
            if (state.pikeAttackEffects.none { it.sourcePosition == defender.position.value }) {
                state.pikeAttackEffects.add(
                    PikeAttackEffect(
                        sourcePosition = defender.position.value,
                        targetPosition = targetPosition,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }

        // Record wizard fireball overlay effect for Wizard Tower area attacks
        if (defender.type == DefenderType.WIZARD_TOWER) {
            if (state.wizardAttackEffects.none { it.sourcePosition == defender.position.value }) {
                state.wizardAttackEffects.add(
                    WizardAttackEffect(
                        sourcePosition = defender.position.value,
                        targetPosition = targetPosition,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }

        // Record alchemy acid vial overlay effect for Alchemy Tower lasting attacks
        if (defender.type == DefenderType.ALCHEMY_TOWER) {
            if (state.alchemyAttackEffects.none { it.sourcePosition == defender.position.value }) {
                state.alchemyAttackEffects.add(
                    AlchemyAttackEffect(
                        sourcePosition = defender.position.value,
                        targetPosition = targetPosition,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }

        if (defender.isDisabled.value) {
            defender.actionsRemaining.value = 0
        } else {
            defender.actionsRemaining.value--
        }

        // Process defeated attackers immediately to give coins
        processDefeated()

        return true
    }

    private fun singleTargetAttack(
        defender: Defender,
        target: Attacker,
    ) {
        if (state.isShieldWallAttackBlocked(defender, target)) {
            return
        }
        if (target.type.isMirrorImage) {
            removeHitMirrorImages(defender, listOf(target))
            return
        }
        if (target.type.immuneToNonMagicTowerDamage && defender.type.attackType != AttackType.AREA) {
            return
        }
        // Shadow resistance: immune to non-magical (melee/ranged) attacks
        if (target.type.immuneToNonMagical) {
            return
        }
        // Blade immunity: immune to melee and ranged (physical/blade) tower attacks.
        // Only area (fireball) and lasting (acid) attacks can damage a troll.
        if (target.type.immuneToBladeAttacks &&
            (defender.type.attackType == AttackType.MELEE || defender.type.attackType == AttackType.RANGED)
        ) {
            return
        }
        val targetWasUninjured = target.currentHealth.value == target.maxHealth
        val damage = getEffectiveDamageAgainst(defender, target)
        val actualDamage = minOf(target.currentHealth.value, damage)
        target.currentHealth.value -= damage
        trackWaaghChargeFromHit(target, actualDamage)
        if (target.currentHealth.value <= 0) {
            target.isDefeated.value = true
            recordDirectKill(defender, target, targetWasUninjured)
        }

        // Apply spike barbs effect (level 10+ with Construction level 1+)
        if (defender.type == DefenderType.SPIKE_TOWER &&
            defender.level.value >= 10 &&
            state.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
        ) {
            target.movementPenalty.value += 1
        }
    }

    private fun areaAttack(
        defender: Defender,
        targetPosition: Position,
    ) {
        // Calculate affected positions - target and neighbors within area effect radius
        // At level 20+, radius increases from 1 to 2 tiles
        val affectedPositions = mutableSetOf(targetPosition)
        val radius = defender.areaEffectRadius

        if (radius == 1) {
            // Use standard hex neighbors for radius 1
            affectedPositions.addAll(
                targetPosition.getHexNeighbors().filter { neighbor ->
                    neighbor.x >= 0 &&
                        neighbor.x < state.level.gridWidth &&
                        neighbor.y >= 0 &&
                        neighbor.y < state.level.gridHeight &&
                        isValidAreaTargetPosition(neighbor)
                },
            )
        } else {
            // Use extended radius for level 20+
            affectedPositions.addAll(
                targetPosition
                    .getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                    .filter { isValidAreaTargetPosition(it) },
            )
        }

        // Only include target position if it's on the path, a bridge, or a spawn point
        if (!isValidAreaTargetPosition(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }

        // Damage all enemies in affected positions (except those immune to fireballs)
        val targets =
            state.attackers.filter {
                !it.isDefeated.value && affectedPositions.contains(it.position.value)
            }
        val unblockedTargets = targets.filterNot { state.isShieldWallAttackBlocked(defender, it) }
        removeHitMirrorImages(defender, unblockedTargets)

        val blockedPositions =
            affectedPositions.filterTo(mutableSetOf()) { position ->
                state.isShieldWallAttackBlocked(defender, position)
            }

        for (target in unblockedTargets) {
            if (target.type.isMirrorImage) continue
            // Check immunity to fireball (Red Demons)
            if (target.canBeDamagedByFireball()) {
                val targetWasUninjured = target.currentHealth.value == target.maxHealth
                val damage = getEffectiveDamageAgainst(defender, target)
                val actualDamage = minOf(target.currentHealth.value, damage)
                target.currentHealth.value -= damage
                trackWaaghChargeFromHit(target, actualDamage)
                if (target.currentHealth.value <= 0) {
                    target.isDefeated.value = true
                    recordDirectKill(defender, target, targetWasUninjured)
                }
            }
        }

        // Clear existing fireball effects from this defender
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.FIREBALL && it.defenderId == defender.id
        }

        // Remove acid effects from affected positions (fire burns away the acid)
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.ACID && it.position in affectedPositions && it.position !in blockedPositions
        }

        // Fire also burns away spider webs.
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.WEB && it.position in affectedPositions && it.position !in blockedPositions
        }

        // Damage all bridges in affected positions
        affectedPositions.forEach { pos ->
            if (pos in blockedPositions) return@forEach
            val bridge = state.getBridgeAt(pos)
            if (bridge != null && bridge.isActive) {
                bridgeSystem.damageBridge(pos, getEffectiveDamage(defender))
            }
        }

        // Destroy fiefs in affected positions (fireball destroys fiefs)
        affectedPositions.forEach { pos ->
            if (pos !in blockedPositions) {
                state.fiefs.removeAll { it.position == pos }
            }
        }

        // Fireball reduces shadow fog duration by 1 on affected tiles
        affectedPositions.filter { it !in blockedPositions }.forEach { pos ->
            val shadowFog = state.fieldEffects.find { it.type == FieldEffectType.SHADOW_FOG && it.position == pos }
            if (shadowFog != null) {
                shadowFog.turnsRemaining -= 1
                if (shadowFog.turnsRemaining <= 0) {
                    state.fieldEffects.remove(shadowFog)
                }
            }
        }

        // Add new fireball effects (visual only, last for 1 turn to show affected area)
        for (pos in affectedPositions) {
            if (pos in blockedPositions) continue
            state.fieldEffects.add(
                FieldEffect(
                    position = pos,
                    type = FieldEffectType.FIREBALL,
                    damage = getEffectiveDamage(defender),
                    turnsRemaining = 1, // Visual effect lasts 1 turn
                    defenderId = defender.id,
                ),
            )
        }
    }

    private fun lastingAttack(
        defender: Defender,
        targetPosition: Position,
    ) {
        // Calculate affected positions - target and neighbors within area effect radius
        // At level 20+, radius increases from 1 to 2 tiles
        val affectedPositions = mutableSetOf(targetPosition)
        val radius = defender.areaEffectRadius

        if (radius == 1) {
            // Use standard hex neighbors for radius 1
            affectedPositions.addAll(
                targetPosition.getHexNeighbors().filter { neighbor ->
                    neighbor.x >= 0 &&
                        neighbor.x < state.level.gridWidth &&
                        neighbor.y >= 0 &&
                        neighbor.y < state.level.gridHeight &&
                        state.level.isEnemyOccupiable(neighbor)
                },
            )
        } else {
            // Use extended radius for level 20+
            affectedPositions.addAll(
                targetPosition
                    .getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                    .filter { state.level.isEnemyOccupiable(it) },
            )
        }

        // Remove target position only if it's neither on path, on a bridge, nor a spawn point
        if (!isValidAreaTargetPosition(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }

        // Apply initial damage and DOT to all enemies in affected positions
        val targets =
            state.attackers.filter {
                !it.isDefeated.value && affectedPositions.contains(it.position.value)
            }
        val unblockedTargets = targets.filterNot { state.isShieldWallAttackBlocked(defender, it) }
        removeHitMirrorImages(defender, unblockedTargets)

        val blockedPositions =
            affectedPositions.filterTo(mutableSetOf()) { position ->
                state.isShieldWallAttackBlocked(defender, position)
            }

        for (target in unblockedTargets) {
            if (target.type.isMirrorImage) continue
            // Check immunity to acid (Blue Demons)
            if (target.canBeDamagedByAcid() && !target.type.immuneToNonMagicTowerDamage) {
                val targetWasUninjured = target.currentHealth.value == target.maxHealth
                // Initial damage is same as DOT tick damage (not full damage)
                val damage = getEffectiveDamageAgainst(defender, target) / LASTING_DAMAGE_DIVISOR
                val actualDamage = minOf(target.currentHealth.value, damage)
                target.currentHealth.value -= damage
                trackWaaghChargeFromHit(target, actualDamage)
                // Mark for additional rounds of DOT based on tower level
                defender.dotRoundsRemaining[target.id] = defender.dotDuration

                if (target.currentHealth.value <= 0) {
                    target.isDefeated.value = true
                    recordDirectKill(defender, target, targetWasUninjured)
                }
            }
        }

        // Create field effects for acid DOT on all affected positions
        // Don't remove existing acid effects - they should persist until they expire

        // Acid dissolves spider webs on the targeted tiles.
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.WEB && it.position in affectedPositions
        }

        // Acid destroys fiefs on the targeted tiles
        affectedPositions.forEach { pos ->
            if (pos !in blockedPositions) {
                state.fiefs.removeAll { it.position == pos }
            }
        }

        // Get all positions with active fireball effects (fire burns away acid)
        val fireballPositions =
            state.fieldEffects
                .filter { it.type == FieldEffectType.FIREBALL }
                .mapTo(mutableSetOf()) { it.position }

        for (pos in affectedPositions) {
            if (pos in blockedPositions) continue
            // Skip this position if there's an active fireball
            if (pos in fireballPositions) continue

            // Find if there's an enemy at this position
            val enemyAtPos = targets.find { it.position.value == pos }

            // Check if there's already an acid effect at this position
            val existingEffect =
                state.fieldEffects.find {
                    it.type == FieldEffectType.ACID && it.position == pos
                }

            val newDuration = defender.dotDuration

            if (existingEffect != null) {
                // If existing effect has more turns, keep it; otherwise replace it
                if (newDuration > existingEffect.turnsRemaining) {
                    state.fieldEffects.remove(existingEffect)
                    state.fieldEffects.add(
                        FieldEffect(
                            position = pos,
                            type = FieldEffectType.ACID,
                            damage = getEffectiveDamage(defender) / LASTING_DAMAGE_DIVISOR,
                            turnsRemaining = newDuration,
                            defenderId = defender.id,
                            attackerId = enemyAtPos?.id,
                        ),
                    )
                }
                // If existing has equal or more turns, do nothing (keep existing)
            } else {
                // No existing effect, add new one
                state.fieldEffects.add(
                    FieldEffect(
                        position = pos,
                        type = FieldEffectType.ACID,
                        damage = getEffectiveDamage(defender) / LASTING_DAMAGE_DIVISOR,
                        turnsRemaining = newDuration,
                        defenderId = defender.id,
                        attackerId = enemyAtPos?.id,
                    ),
                )
            }
        }
    }

    fun applyLastingEffects() {
        // Apply LASTING damage from acid puddles on the ground
        val acidEffects = state.fieldEffects.filter { it.type == FieldEffectType.ACID }

        for (effect in acidEffects) {
            // Find all enemies standing in the acid
            val enemiesInAcid =
                state.attackers.filter {
                    !it.isDefeated.value && it.position.value == effect.position
                }

            for (attacker in enemiesInAcid) {
                if (attacker.type.isMirrorImage) continue
                // Check immunity to acid (Blue Demons)
                if (attacker.canBeDamagedByAcid()) {
                    val actualDamage = minOf(attacker.currentHealth.value, effect.damage)
                    attacker.currentHealth.value -= effect.damage
                    trackWaaghChargeFromHit(attacker, actualDamage)
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
                    }
                }
            }
        }
    }

    fun processDefeatedAttackers() {
        val defeated = state.attackers.filter { it.isDefeated.value && !state.level.isTargetPosition(it.position.value) }

        // Non-reward defeats (e.g. merged swarm units or enemy-on-enemy trampling) are not real kills.
        val nonRewardDefeats = defeated.filter { it.wasMerged.value }
        val actualKills = defeated.filter { !it.wasMerged.value && !it.type.isMirrorImage }

        // Track kills for this attack
        val killsThisAttack = actualKills.size
        val killedTypes = actualKills.map { it.type }
        val killInfos = pendingKillInfos.toList()
        pendingKillInfos.clear()

        if (killsThisAttack > 0) {
            GameLogBuffer.log("COMBAT", "Defeated $killsThisAttack enemies: ${killedTypes.joinToString()}")
        }
        if (nonRewardDefeats.isNotEmpty()) {
            GameLogBuffer.log("COMBAT", "${nonRewardDefeats.size} enemy defeat(s) without reward")
        }

        // Update turn totals
        killsThisTurn += killsThisAttack
        killedTypesThisTurn.addAll(killedTypes)

        // Update scripted-event kill tracking (total + per type)
        if (killsThisAttack > 0) {
            state.enemiesKilledTotal.value += killsThisAttack
            for (type in killedTypes) {
                state.enemiesKilledByType[type] = (state.enemiesKilledByType[type] ?: 0) + 1
            }
        }

        // Emit combat result for achievement tracking
        if (killsThisAttack > 0) {
            onCombatResult?.invoke(
                CombatResult(
                    killsThisAttack = killsThisAttack,
                    killedEnemyTypes = killedTypes,
                    killInfos = killInfos,
                ),
            )
        }

        // Calculate XP and coins for defeated enemies (merged swarm units are excluded)
        for (attacker in actualKills) {
            queueSoulCallResurrection(attacker)

            // Coin reward is calculated here and stored in CoinGainEffect.amount; the actual
            // state.coins.value increment is performed by the UI (GameMap) when the coin gain
            // animation plays, so the counter visually updates in sync with the animation.
            // pendingCoinGains tracks the total not yet credited; completeEnemyTurn flushes it
            // as a safety net in case the animation coroutine is cancelled before it fires.
            val baseCoins = attacker.type.reward * attacker.level.value * attacker.type.goldRewardMultiplier
            // Cap'n Roderich also drops his accumulated treasure on defeat.
            val treasureBonus = attacker.treasureCoins.value
            val modifiedCoins =
                (baseCoins * state.incomeMultiplier).toInt() * state.coinSurgeMultiplier() + treasureBonus
            if (modifiedCoins > 0) {
                state.pendingCoinGains.value += modifiedCoins
            }

            // Record enemy death visual effect for animation
            state.defeatedEnemyEffects.add(
                EnemyDeathEffect(
                    position = attacker.position.value,
                    turnNumber = state.turnNumber.value,
                    attackerType = attacker.type,
                    attackerLevel = attacker.level.value,
                ),
            )

            // Record coin gain visual effect for animation (only when coins are actually awarded)
            if (modifiedCoins > 0) {
                state.coinGainEffects.add(
                    CoinGainEffect(
                        position = attacker.position.value,
                        amount = modifiedCoins,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }

            // Award XP (multiplied by level for non-dragons)
            val xpEarned = attacker.type.xp * attacker.level.value
            state.xpEarnedThisLevel.value += xpEarned

            // Note: ENEMY_DESTROYED sound is played by the UI (GameMap) when the death animation
            // starts, so that sound and animation are in sync.  Do NOT play it here.

            // Queue Ewhad message (retreats unless it's the final stand level)
            if (attacker.type == AttackerType.EWHAD) {
                val isFinalStand = state.level.editorLevelId == "the_final_stand"
                val messageType =
                    if (isFinalStand) {
                        GameMessageType.EWHAD_DEFEATED
                    } else {
                        GameMessageType.EWHAD_RETREATS
                    }
                state.pendingMessages.add(GameMessage(type = messageType))
            } else if (attacker.type.isRealVillain) {
                state.pendingMessages.add(
                    GameMessage(
                        type = GameMessageType.VILLAIN_DEFEATED,
                        name = attacker.type.name,
                    ),
                )
                // Ignis-Va leaves a burning tile on defeat that disables nearby towers for 2 rounds.
                if (attacker.type == AttackerType.IGNIS_VA_THE_DRAGONVOICE) {
                    applyIgnisVaBurningTile(attacker)
                }
            }
        }
        state.attackers.removeAll { it.isDefeated.value }
    }

    private fun queueSoulCallResurrection(attacker: Attacker) {
        val resurrectedType = attacker.type.getSoulCallResurrectionType() ?: return
        val valeriusInRange =
            state.attackers.any { other ->
                other.type == AttackerType.PRINCE_VALERIUS_THE_SOULREAPER &&
                    !other.isDefeated.value &&
                    other.position.value.hexDistanceTo(attacker.position.value) <= (other.type.soulCallRange ?: 0)
            }
        if (!valeriusInRange) return
        state.pendingSoulCalls.add(
            PendingSoulCall(
                position = attacker.position.value,
                attackerType = resurrectedType,
                level = attacker.level.value,
                reviveTurn = state.turnNumber.value + 1,
                dragonName = attacker.dragonName,
                currentTarget = attacker.currentTarget?.value,
            ),
        )
    }

    /**
     * Get total kills this turn (for achievements)
     */
    fun getKillsThisTurn(): Int = killsThisTurn

    /**
     * When Ignis-Va is defeated, she leaves a burning tile at her last position.
     *
     * The burning tile:
     * - Creates a [FieldEffectType.BURNING_TILE] visual effect (lasts 2 rounds for rendering).
     * - Disables all towers within [BURNING_TILE_RANGE] hexes for [BURNING_TILE_DISABLE_TURNS]
     *   player turns. The extra +1 compensates for the decrement in [updateTowerDisableStatus]
     *   that happens at the end of the current enemy turn when she dies during the enemy turn.
     *   When she dies during the player turn there is no immediate decrement, so we accept up to
     *   one turn of overshoot in that rare case.
     */
    private fun applyIgnisVaBurningTile(ignisVa: Attacker) {
        val burnPosition = ignisVa.position.value

        state.fieldEffects.add(
            FieldEffect(
                position = burnPosition,
                type = FieldEffectType.BURNING_TILE,
                damage = 0,
                turnsRemaining = BURNING_TILE_VISUAL_TURNS,
                defenderId = -1,
            ),
        )

        state.defenders
            .filter { tower ->
                tower.position.value.hexDistanceTo(burnPosition) <= BURNING_TILE_RANGE
            }.forEach { tower ->
                if (!tower.isDisabled.value || tower.disabledTurnsRemaining.value < BURNING_TILE_DISABLE_TURNS + 1) {
                    tower.isDisabled.value = true
                    tower.disabledTurnsRemaining.value =
                        maxOf(tower.disabledTurnsRemaining.value, BURNING_TILE_DISABLE_TURNS + 1)
                }
            }
    }
}
