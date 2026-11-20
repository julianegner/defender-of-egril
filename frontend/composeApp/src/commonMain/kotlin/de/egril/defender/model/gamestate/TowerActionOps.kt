package de.egril.defender.model

/**
 * Special-action and magical-trap queries for towers, used to decide which towers
 * need manual attention instead of auto-attacking.
 */
internal object TowerActionOps {
    /**
     * Check if there are defenders with special actions that cannot be automated effectively.
     * Returns a list of defender types that have remaining special actions.
     */
    fun getDefenderTypesWithSpecialActions(state: GameState): List<DefenderType> =
        with(state) {
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

            typesWithActions.toList()
        }

    fun canWizardPlaceMagicalTrapAt(
        state: GameState,
        wizard: Defender,
        trapPosition: Position,
    ): Boolean =
        with(state) {
            if (!wizard.hasMagicalTraps) return@with false
            if (wizard.level.value < 10) return@with false
            if (!wizard.isReady || wizard.actionsRemaining.value <= 0 || wizard.isDisabled.value) return@with false
            if (wizard.trapCooldownRemaining.value > 0) return@with false

            val distance = wizard.position.value.distanceTo(trapPosition)
            if (distance > effectiveRange(wizard)) return@with false
            if (!level.isOnPath(trapPosition)) return@with false
            if (!level.isPlacementAllowed(trapPosition)) return@with false

            if (attackers.any { it.position.value == trapPosition && !it.isDefeated.value }) return@with false
            if (isInactiveGargoyleAt(trapPosition)) return@with false
            if (fieldEffects.any { it.position == trapPosition }) return@with false
            if (fiefs.any { it.position == trapPosition }) return@with false

            true
        }

    fun canWizardPlaceAnyMagicalTrap(
        state: GameState,
        wizard: Defender,
    ): Boolean =
        state.level.pathCells.any { position ->
            canWizardPlaceMagicalTrapAt(state, wizard, position)
        }
}
