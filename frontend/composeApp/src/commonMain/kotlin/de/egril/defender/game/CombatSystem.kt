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
    internal val state: GameState,
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

    internal fun recordDirectKill(
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
        internal const val LASTING_DAMAGE_DIVISOR = 2

        /** Hex-distance radius of the burning tile's tower-disable effect. */
        internal const val BURNING_TILE_RANGE = 2

        /** Number of player turns that towers near the burning tile are disabled. */
        internal const val BURNING_TILE_DISABLE_TURNS = 2

        /** How many enemy turns the burning tile visual effect remains visible. */
        internal const val BURNING_TILE_VISUAL_TURNS = 2
    }

    private fun blindTowerAfterMirrorHit(defender: Defender) {
        val blindDurationTurns = (AttackerType.SILAS_THE_MASKMASTER.mirrorBlindDurationTurns ?: 2) + 1
        defender.isDisabled.value = true
        defender.disabledTurnsRemaining.value = blindDurationTurns
        defender.actionsRemaining.value = 0
        state.pendingMessages.add(GameMessage(type = GameMessageType.SILAS_MIRROR_HIT))
    }

    internal fun trackWaaghChargeFromHit(
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

    internal fun removeHitMirrorImages(
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
    internal fun getEffectiveDamage(defender: Defender): Int {
        val effectiveLevel = getEffectiveLevel(defender)
        return defender.type.baseDamage + (effectiveLevel - 1) * 5
    }

    /**
     * Returns the damage dealt by [defender] to [target], applying any target-side passive
     * reductions. Currently this handles Cap'n Roderich's **Seaworthy** passive: attacks from
     * barge-mounted towers (defenders on a raft) deal only 50 % damage to him.
     */
    internal fun getEffectiveDamageAgainst(
        defender: Defender,
        target: Attacker,
    ): Int {
        val raw = getEffectiveDamage(defender)
        val seaworthyReduction = target.type.seaworthyDamageReduction
        val isOnBarge = defender.raftId.value != null
        var damage =
            if (seaworthyReduction > 0f && isOnBarge) {
                (raw * (1f - seaworthyReduction)).toInt().coerceAtLeast(1)
            } else {
                raw
            }
        if (defender.type == DefenderType.WIZARD_TOWER && target.type.wizardTowerDamageReduction > 0f) {
            damage = (damage * (1f - target.type.wizardTowerDamageReduction)).toInt().coerceAtLeast(1)
        }
        return (damage - state.runeGlyphWardArmor(target)).coerceAtLeast(0)
    }

    internal fun queueBridgeDamage(
        position: Position,
        damage: Int,
    ) {
        state.pendingBridgeDamage.add(PendingBridgeDamage(position = position, damage = damage))
    }

    /**
     * Returns true if the given position is a valid area-attack target tile:
     * on any enemy-occupiable tile (path, spawn point, river) or a bridge.
     */
    internal fun isValidAreaTargetPosition(position: Position): Boolean = state.level.isEnemyOccupiable(position) || state.isBridgeAt(position)

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
        if (defender.type.attackType == AttackType.NONE) return false

        // Check if defender can reach the target position
        val distance = defender.position.value.distanceTo(targetPosition)
        if (distance < defender.type.minRange || distance > getEffectiveRange(defender)) return false
        if (!defender.isReady || defender.actionsRemaining.value <= 0) return false

        // Mark defender as used
        defender.hasBeenUsed.value = true

        // For AOE and DOT attacks, target position must be on the path, a river tile, or a spawn point
        if (defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) {
            if (!state.level.isEnemyOccupiable(targetPosition) && findInactiveGargoyle(targetPosition) == null) return false
        } else {
            // For single-target attacks, prioritize enemy over bridge at the same position;
            // shadow fog tiles are always valid targets even if the enemy is not visible.
            val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }
            val bridge = state.getBridgeAt(targetPosition)
            val hasShadowFog = state.fieldEffects.any { it.type == FieldEffectType.SHADOW_FOG && it.position == targetPosition }
            val inactiveGargoyle = findInactiveGargoyle(targetPosition)
            if (target == null && inactiveGargoyle == null && (bridge == null || !bridge.isActive) && !hasShadowFog) return false
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

        var deferredBridgeDamage: Int? = null

        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> {
                // Single target attack - prioritize enemy over bridge
                val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }

                val inactiveGargoyle = findInactiveGargoyle(targetPosition)
                if (target != null) {
                    // Attack enemy (takes priority)
                    singleTargetAttack(defender, target)
                } else if (inactiveGargoyle != null) {
                    attackInactiveGargoyle(defender, inactiveGargoyle)
                } else {
                    // No enemy, attack bridge if present (deferred until after attack visuals are queued)
                    val bridge = state.getBridgeAt(targetPosition)
                    if (bridge != null && bridge.isActive) {
                        deferredBridgeDamage = getEffectiveDamage(defender)
                    }
                    // If targeting a shadow fog tile with no enemy/bridge, the action is
                    // consumed but the attack misses (the tile was targeted blind).
                }
            }
            AttackType.AREA -> {
                // Area attack affects both enemies AND bridges in range; bridge HP is deferred
                // until the animation has finished so the damage appears after projectile impact.
                areaAttack(defender, targetPosition)
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

        // Apply deferred bridge damage after the attack animation is queued so the visual starts first.
        if (deferredBridgeDamage != null) {
            queueBridgeDamage(targetPosition, deferredBridgeDamage)
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

    private fun findInactiveGargoyle(position: Position): Gargoyle? = state.gargoyles.find { it.position.value == position && !it.isActive.value }

    /**
     * Manual attacks hit a stone gargoyle for its full damage. Pike, spear and bow towers
     * consume their action but cannot harm gargoyles. A destroyed gargoyle grants XP only
     * (no coins, no kill counts).
     */
    private fun attackInactiveGargoyle(
        defender: Defender,
        gargoyle: Gargoyle,
    ) {
        val damage =
            if (defender.type in GargoyleRules.NO_DAMAGE_DEFENDERS) 0 else getEffectiveDamage(defender)
        damageInactiveGargoyle(gargoyle, damage, defender.type.name)
    }

    /**
     * Applies damage to a stone gargoyle (manual hits, area/acid splash, acid puddles).
     * Destruction grants XP only, never coins or kill counts.
     */
    internal fun damageInactiveGargoyle(
        gargoyle: Gargoyle,
        damage: Int,
        sourceName: String,
    ) {
        if (damage <= 0) return
        gargoyle.currentHealth.value = (gargoyle.currentHealth.value - damage).coerceAtLeast(0)
        if (gargoyle.currentHealth.value > 0) return

        state.gargoyles.remove(gargoyle)
        state.defeatedEnemyEffects.add(
            EnemyDeathEffect(
                position = gargoyle.position.value,
                turnNumber = state.turnNumber.value,
                attackerType = AttackerType.GARGOYLE,
                attackerLevel = gargoyle.level.value,
            ),
        )
        state.xpEarnedThisLevel.value += AttackerType.GARGOYLE.xp * gargoyle.level.value
        GameLogBuffer.log("COMBAT", "Stone gargoyle destroyed by $sourceName")
    }

    /**
     * Inactive gargoyles inside the blast of an AREA/LASTING attack, excluding positions
     * blocked by shield walls or obsidian protectors. Unlike enemies, a gargoyle may stand on a
     * build-area tile, so the tile-validity filter used for enemies is not applied here.
     */
    internal fun inactiveGargoylesInBlast(
        targetPosition: Position,
        radius: Int,
        blockedPositions: Set<Position>,
    ): List<Gargoyle> {
        val blastPositions = mutableSetOf(targetPosition)
        if (radius == 1) {
            blastPositions.addAll(
                targetPosition.getHexNeighbors().filter { neighbor ->
                    neighbor.x >= 0 &&
                        neighbor.x < state.level.gridWidth &&
                        neighbor.y >= 0 &&
                        neighbor.y < state.level.gridHeight
                },
            )
        } else {
            blastPositions.addAll(
                targetPosition.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight),
            )
        }
        return state.gargoyles.filter {
            !it.isActive.value && it.position.value in blastPositions && it.position.value !in blockedPositions
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
                if (state.isProtectedByObsidianProtector(attacker)) continue
                // Check immunity to acid (Blue Demons)
                if (attacker.canBeDamagedByAcid()) {
                    val damage = (effect.damage - state.runeGlyphWardArmor(attacker)).coerceAtLeast(0)
                    val actualDamage = minOf(attacker.currentHealth.value, damage)
                    attacker.recordDamageTaken(actualDamage)
                    attacker.currentHealth.value -= damage
                    trackWaaghChargeFromHit(attacker, actualDamage)
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
                    }
                }
            }

            val gargoylesInAcid = state.gargoyles.filter { !it.isActive.value && it.position.value == effect.position }
            for (gargoyle in gargoylesInAcid) {
                damageInactiveGargoyle(gargoyle, effect.damage, "acid")
            }
        }
    }

    fun processDefeatedAttackers() {
        val defeated = state.attackers.filter { it.isDefeated.value && !state.level.isTargetPosition(it.position.value) }

        // Non-reward defeats (e.g. merged swarm units or enemy-on-enemy trampling) are not real kills.
        val nonRewardDefeats = defeated.filter { it.wasMerged.value }
        // Gargoyles grant no kills or coins, whether destroyed by the player or by enemies.
        val actualKills = defeated.filter { !it.wasMerged.value && !it.type.isMirrorImage && !it.type.isGargoyle }
        // Destroyed gargoyles still grant XP at the end of the level, but no coins or kill counts.
        val destroyedGargoyles = defeated.filter { !it.wasMerged.value && it.type.isGargoyle }
        for (gargoyle in destroyedGargoyles) {
            state.xpEarnedThisLevel.value += gargoyle.type.xp * gargoyle.level.value
            state.defeatedEnemyEffects.add(
                EnemyDeathEffect(
                    position = gargoyle.position.value,
                    turnNumber = state.turnNumber.value,
                    attackerType = gargoyle.type,
                    attackerLevel = gargoyle.level.value,
                ),
            )
        }

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
            if (attacker.type == AttackerType.RUNEMASTER) {
                state.runes.value++
            }

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
}
