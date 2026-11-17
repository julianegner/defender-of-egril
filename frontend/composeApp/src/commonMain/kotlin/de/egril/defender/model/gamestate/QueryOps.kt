package de.egril.defender.model

/**
 * Read-only query logic extracted from [GameState]. Each function takes the state as its first
 * parameter and reads the same fields as before. [GameState] keeps the public members as one-line
 * delegates, so call sites are unchanged.
 */
internal object QueryOps {

    fun isBridgeAt(state: GameState, position: Position): Boolean =
        with(state) {
            bridges.any { bridge ->
                bridge.isActive && bridge.coversPosition(position)
            }
        }

    fun getBridgeAt(state: GameState, position: Position): Bridge? =
        with(state) {
            bridges.find { bridge ->
                bridge.isActive && bridge.coversPosition(position)
            }
        }

    fun getPortalAtEntry(state: GameState, position: Position): Portal? =
        with(state) {
            activePortals.firstOrNull { portal ->
                portal.entryPosition == position
            }
        }

    fun isPortalEntry(state: GameState, position: Position): Boolean = getPortalAtEntry(state, position) != null

    fun isPortalExit(state: GameState, position: Position): Boolean =
        with(state) {
            activePortals.any { portal ->
                portal.exitPosition == position
            }
        }

    fun isPortalTile(state: GameState, position: Position): Boolean =
        isPortalEntry(state, position) || isPortalExit(state, position)

    fun isRaftAt(state: GameState, position: Position): Boolean =
        with(state) {
            rafts.any { raft ->
                raft.isActive && raft.currentPosition.value == position
            }
        }

    fun getRaftAt(state: GameState, position: Position): Raft? =
        with(state) {
            rafts.find { raft ->
                raft.isActive && raft.currentPosition.value == position
            }
        }

    fun effectiveRange(state: GameState, defender: Defender): Int {
        val hasDoubleReachBuff =
            state.activeSpellEffects.any {
                it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == defender.id
            }
        return if (hasDoubleReachBuff) defender.range * 2 else defender.range
    }

    fun hasDefendersWithUnusedActions(state: GameState): Boolean =
        with(state) {
            // Get active attackers (not defeated, not building bridges)
            val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value && !it.isDiving.value }

            defenders.any { defender ->
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

    fun getActionableTowersForTab(state: GameState): List<Defender> =
        with(state) {
            val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value && !it.isDiving.value }
            defenders
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

    fun getAutoAttackAvailability(state: GameState): AutoAttackAvailability =
        with(state) {
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
                            return@with AutoAttackAvailability.ATTACK
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
                AutoAttackAvailability.MANA_ONLY
            } else {
                AutoAttackAvailability.NONE
            }
        }
}
