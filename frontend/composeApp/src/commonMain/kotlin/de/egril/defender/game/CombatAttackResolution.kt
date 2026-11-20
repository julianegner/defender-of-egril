package de.egril.defender.game

import de.egril.defender.game.CombatSystem.Companion.BURNING_TILE_DISABLE_TURNS
import de.egril.defender.game.CombatSystem.Companion.BURNING_TILE_RANGE
import de.egril.defender.game.CombatSystem.Companion.BURNING_TILE_VISUAL_TURNS
import de.egril.defender.game.CombatSystem.Companion.LASTING_DAMAGE_DIVISOR
import de.egril.defender.model.*

internal fun CombatSystem.singleTargetAttack(
    defender: Defender,
    target: Attacker,
) {
    if (state.isShieldWallAttackBlocked(defender, target) || state.isProtectedByObsidianProtector(target)) {
        return
    }
    if (target.type.isMirrorImage) {
        removeHitMirrorImages(defender, listOf(target))
        return
    }
    if (target.type.isGargoyle && defender.type in GargoyleRules.NO_DAMAGE_DEFENDERS) {
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
    target.recordDamageTaken(actualDamage)
    target.currentHealth.value -= damage
    trackWaaghChargeFromHit(target, actualDamage)
    if (target.currentHealth.value <= 0) {
        target.isDefeated.value = true
        recordDirectKill(defender, target, targetWasUninjured)
    }

    // Apply spike barbs effect (level 10+ with Construction level 1+)
    if (defender.type == DefenderType.SPIKE_TOWER &&
        defender.level.value >= 10 &&
        state.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1 &&
        !state.isProtectedByRuneGlyphWard(target)
    ) {
        target.movementPenalty.value += 1
    }
}

internal fun CombatSystem.areaAttack(
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
    val unblockedTargets =
        targets.filterNot {
            state.isShieldWallAttackBlocked(defender, it) || state.isProtectedByObsidianProtector(it)
        }
    removeHitMirrorImages(defender, unblockedTargets)

    val blockedPositions =
        affectedPositions.filterTo(mutableSetOf()) { position ->
            state.isShieldWallAttackBlocked(defender, position) ||
                state.attackers.any {
                    !it.isDefeated.value &&
                        it.position.value == position &&
                        state.isProtectedByObsidianProtector(it)
                }
        }

    for (target in unblockedTargets) {
        if (target.type.isMirrorImage) continue
        // Check immunity to fireball (Red Demons)
        if (target.canBeDamagedByFireball()) {
            val targetWasUninjured = target.currentHealth.value == target.maxHealth
            val damage = getEffectiveDamageAgainst(defender, target)
            val actualDamage = minOf(target.currentHealth.value, damage)
            target.recordDamageTaken(actualDamage)
            target.currentHealth.value -= damage
            trackWaaghChargeFromHit(target, actualDamage)
            if (target.currentHealth.value <= 0) {
                target.isDefeated.value = true
                recordDirectKill(defender, target, targetWasUninjured)
            }
        }
    }

    // Stone gargoyles in the blast take full fireball damage, regardless of tile type.
    for (gargoyle in inactiveGargoylesInBlast(targetPosition, radius, blockedPositions)) {
        damageInactiveGargoyle(gargoyle, getEffectiveDamage(defender), defender.type.name)
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

    // Queue bridge damage until after the fireball animation has finished.
    affectedPositions.forEach { pos ->
        if (pos in blockedPositions) return@forEach
        val bridge = state.getBridgeAt(pos)
        if (bridge != null && bridge.isActive) {
            queueBridgeDamage(pos, getEffectiveDamage(defender))
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

internal fun CombatSystem.lastingAttack(
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
    val unblockedTargets =
        targets.filterNot {
            state.isShieldWallAttackBlocked(defender, it) || state.isProtectedByObsidianProtector(it)
        }
    removeHitMirrorImages(defender, unblockedTargets)

    val blockedPositions =
        affectedPositions.filterTo(mutableSetOf()) { position ->
            state.isShieldWallAttackBlocked(defender, position) ||
                state.attackers.any {
                    !it.isDefeated.value &&
                        it.position.value == position &&
                        state.isProtectedByObsidianProtector(it)
                }
        }

    for (target in unblockedTargets) {
        if (target.type.isMirrorImage) continue
        // Check immunity to acid (Blue Demons)
        if (target.canBeDamagedByAcid() && !target.type.immuneToNonMagicTowerDamage) {
            val targetWasUninjured = target.currentHealth.value == target.maxHealth
            // Initial damage is same as DOT tick damage (not full damage)
            val damage = getEffectiveDamageAgainst(defender, target) / LASTING_DAMAGE_DIVISOR
            val actualDamage = minOf(target.currentHealth.value, damage)
            target.recordDamageTaken(actualDamage)
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

    // Stone gargoyles in the acid take the same initial hit as enemies; their acid puddle ticks
    // continue through applyLastingEffects.
    for (gargoyle in inactiveGargoylesInBlast(targetPosition, radius, blockedPositions)) {
        damageInactiveGargoyle(gargoyle, getEffectiveDamage(defender) / LASTING_DAMAGE_DIVISOR, defender.type.name)
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

    // Queue bridge damage until after the acid projectile/impact animation has finished.
    affectedPositions.forEach { pos ->
        if (pos in blockedPositions) return@forEach
        val bridge = state.getBridgeAt(pos)
        if (bridge != null && bridge.isActive) {
            queueBridgeDamage(pos, getEffectiveDamage(defender))
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
internal fun CombatSystem.applyIgnisVaBurningTile(ignisVa: Attacker) {
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
