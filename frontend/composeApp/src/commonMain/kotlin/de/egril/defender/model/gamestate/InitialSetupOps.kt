package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf

/**
 * Places the pre-configured level elements (supports, barricades, defenders, attackers, traps,
 * bridges, fiefs, mushrooms, gargoyles, portals) into a freshly created [GameState].
 */
internal object InitialSetupOps {
    /**
     * Initialize pre-placed defenders, attackers, traps, and barricades from level configuration.
     * This should be called right after GameState creation to set up the initial level state.
     */
    fun initializePrePlacedElements(state: GameState) =
        with(state) {
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

            // Place initial gargoyles (inactive guardians at full health for their level)
            for (initialGargoyle in initialData.gargoyles) {
                val gargoyleLevel = initialGargoyle.level.coerceAtLeast(1)
                gargoyles.add(
                    Gargoyle(
                        id = nextGargoyleId.value++,
                        position = mutableStateOf(initialGargoyle.position),
                        level = mutableStateOf(gargoyleLevel),
                        currentHealth = mutableStateOf(GargoyleRules.HEALTH_PER_LEVEL * gargoyleLevel),
                    ),
                )
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
