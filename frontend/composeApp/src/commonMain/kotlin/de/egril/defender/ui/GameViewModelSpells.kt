package de.egril.defender.ui

import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.LogConfig
import de.egril.defender.game.isProtectedByObsidianProtector
import de.egril.defender.game.isProtectedByRuneGlyphWard
import de.egril.defender.game.runeGlyphWardArmor
import de.egril.defender.model.*
import de.egril.defender.ui.animations.SKY_IS_FALLING_DURATION_MS
import de.egril.defender.ui.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Toggle magic panel display
 */
fun GameViewModel.toggleMagicPanel() {
    _showMagicPanel.value = !_showMagicPanel.value
    if (LogConfig.ENABLE_SPELL_LOGGING) {
        println("=== SPELL: Magic panel toggled - now ${if (_showMagicPanel.value) "OPEN" else "CLOSED"}")
    }
}

/**
 * Open magic panel
 */
fun GameViewModel.openMagicPanel() {
    _showMagicPanel.value = true
    if (LogConfig.ENABLE_SPELL_LOGGING) {
        val gameState = _gameState.value
        println("=== SPELL: Magic panel OPENED - Current mana: ${gameState?.currentMana?.value}/${gameState?.maxMana?.value}")
        val playerStats = _currentPlayer.value?.abilities
        val unlockedSpells = playerStats?.unlockedSpells?.size ?: 0
        println("=== SPELL: Player has $unlockedSpells unlocked spell(s)")
    }
}

/**
 * Close magic panel
 */
fun GameViewModel.closeMagicPanel() {
    _showMagicPanel.value = false
    _pendingSpellCast.value = null
    if (LogConfig.ENABLE_SPELL_LOGGING) {
        println("=== SPELL: Magic panel CLOSED")
    }
}

/**
 * Cancel the active Instant Tower spell and return mana (mana was never consumed).
 * Called when the player chooses to abort from the abort dialog.
 */
fun GameViewModel.cancelInstantTowerSpell() {
    val state = _gameState.value ?: return
    if (state.instantTowerSpellActive.value) {
        state.instantTowerSpellActive.value = false
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Instant Tower spell cancelled by player (no mana consumed)")
        }
    }
}

/**
 * Clear pending scroll to position (called after UI has consumed it)
 */
fun GameViewModel.clearPendingScrollPosition() {
    _pendingScrollToPosition.value = null
}

/**
 * Toggle spell selection (select or deselect)
 * No confirmation dialog - enters targeting mode directly
 */
fun GameViewModel.setPendingSpell(spell: SpellType) {
    val gameState = _gameState.value

    // INSTANT_TOWER: toggle the instant deploy mode directly without confirmation dialog.
    // Mana is deferred and only consumed when a tower is actually placed.
    if (spell == SpellType.INSTANT_TOWER) {
        if (gameState == null || gameState.currentMana.value < spell.manaCost) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println(
                    "=== SPELL: Cannot cast ${spell.displayName} - Insufficient mana (Need: ${spell.manaCost}, Have: ${gameState?.currentMana?.value ?: 0})",
                )
            }
            return
        }
        if (gameState.instantTowerSpellActive.value) {
            // Already active - cancel the spell mode (mana was never consumed)
            gameState.instantTowerSpellActive.value = false
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Instant Tower spell cancelled (no mana consumed)")
            }
        } else {
            // Activate instant tower mode (mana deferred until tower is placed)
            gameState.instantTowerSpellActive.value = true
            closeMagicPanel()
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Instant Tower spell activated - next tower will be built instantly")
            }
        }
        return
    }

    if (gameState != null && gameState.currentMana.value >= spell.manaCost) {
        // Toggle selection: if same spell clicked again, deselect it
        if (_selectedSpell.value == spell) {
            _selectedSpell.value = null
            _pendingSpellCast.value = null
            exitSpellTargetingMode()
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Spell deselected - ${spell.displayName}")
            }
        } else {
            _selectedSpell.value = spell
            _pendingSpellCast.value = spell
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Spell selected - ${spell.displayName} (Cost: ${spell.manaCost} mana)")
            }
            // For targeting spells, enter targeting mode immediately
            if (spell.requiresTarget) {
                enterSpellTargetingMode(spell)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Entered targeting mode for ${spell.displayName}")
                }
            } else {
                // For non-targeting spells, show confirmation with spell details
                _showSpellTargetConfirmation.value = Pair(spell, Unit)
            }
        }
    } else {
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println(
                "=== SPELL: Cannot cast ${spell.displayName} - Insufficient mana (Need: ${spell.manaCost}, Have: ${gameState?.currentMana?.value ?: 0})",
            )
        }
    }
}

/**
 * Cancel pending spell cast
 */
fun GameViewModel.cancelPendingSpell() {
    if (LogConfig.ENABLE_SPELL_LOGGING) {
        val spell = _pendingSpellCast.value
        if (spell != null) {
            println("=== SPELL: Spell cast CANCELLED - ${spell.displayName}")
        }
    }
    _pendingSpellCast.value = null
    _selectedSpell.value = null
    _pendingTokenSpell.value = null
}

/**
 * Handle target selection for spell (called when player clicks a target)
 * Shows confirmation or warning dialog based on target validity
 */
fun GameViewModel.onSpellTargetSelected(target: Any) {
    val gameState = _gameState.value ?: return
    // Use activeSpell from targeting state (pendingSpellCast was cleared when targeting mode was entered)
    val spell = gameState.spellTargeting.value?.activeSpell ?: return

    // Check for freeze immunity
    if (spell == SpellType.FREEZE_SPELL && target is Attacker) {
        if (isImmuneToFreeze(target.type) || gameState.isProtectedByRuneGlyphWard(target)) {
            _showFreezeImmuneWarning.value = target
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: ${target.type.displayName} is immune to Freeze!")
            }
            return
        }
    }

    // Show confirmation dialog with target details
    _showSpellTargetConfirmation.value = Pair(spell, target)
    if (LogConfig.ENABLE_SPELL_LOGGING) {
        println("=== SPELL: Target selected for ${spell.displayName}")
    }
}

/**
 * Confirm spell cast after target selection
 */
fun GameViewModel.confirmSpellCast() {
    val confirmation = _showSpellTargetConfirmation.value
    if (confirmation != null) {
        val (spell, target) = confirmation
        val viaToken = _pendingTokenSpell.value == spell
        castSpell(spell, target, viaToken)
        _showSpellTargetConfirmation.value = null
        _selectedSpell.value = null
    }
}

/**
 * Dismiss spell target confirmation dialog
 */
fun GameViewModel.dismissSpellConfirmation() {
    _showSpellTargetConfirmation.value = null
    // Keep spell selected and targeting mode active
}

/**
 * Dismiss freeze immune warning
 */
fun GameViewModel.dismissFreezeImmuneWarning() {
    _showFreezeImmuneWarning.value = null
    // Keep spell selected and targeting mode active
}

/**
 * Check if an enemy type is immune to freeze spell
 */
private fun GameViewModel.isImmuneToFreeze(type: AttackerType): Boolean =
    type == AttackerType.BLUE_DEMON ||
        type == AttackerType.RED_DEMON ||
        type == AttackerType.DRAGON ||
        type == AttackerType.EWHAD

/**
 * Cast a spell (called after targeting is complete)
 * For non-targeting spells, this is called immediately after confirmation
 */
fun GameViewModel.castSpell(
    spell: SpellType,
    target: Any? = null,
    viaToken: Boolean = false,
) {
    val gameState = _gameState.value ?: return
    val currentPlayer = _currentPlayer.value ?: return

    if (LogConfig.ENABLE_SPELL_LOGGING) {
        println("=== SPELL: Cast spell called - ${spell.displayName}${if (viaToken) " (token)" else ""}")
        println("=== SPELL: Current mana: ${gameState.currentMana.value}/${gameState.maxMana.value}")
    }

    if (viaToken) {
        // Casting via a level support token: skip mana and unlocked-spell validation.
        if ((gameState.supportSpellsRemaining[spell] ?: 0) <= 0) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - No support token remaining for ${spell.displayName}")
            }
            return
        }
    } else {
        // Validate mana cost
        if (gameState.currentMana.value < spell.manaCost) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - Not enough mana to cast ${spell.displayName}")
            }
            return
        }

        // Validate spell is unlocked
        if (!currentPlayer.abilities.unlockedSpells.contains(spell)) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - Spell ${spell.displayName} is not unlocked")
            }
            return
        }
    }

    // If spell requires targeting and no target provided, enter targeting mode
    if (spell.requiresTarget && target == null) {
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Spell requires targeting - Entering targeting mode")
        }
        enterSpellTargetingMode(spell)
        return
    }

    if (viaToken) {
        // Consume one support token instead of mana.
        val remaining = gameState.supportSpellsRemaining[spell] ?: 0
        if (remaining > 0) {
            gameState.supportSpellsRemaining[spell] = consumeSupportCount(remaining)
        }
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Support token consumed - ${gameState.supportSpellsRemaining[spell]} remaining")
        }
    } else {
        // Deduct mana cost
        val previousMana = gameState.currentMana.value
        gameState.currentMana.value -= spell.manaCost
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Mana deducted - $previousMana -> ${gameState.currentMana.value}")
        }
    }

    // Execute spell effect
    executeSpellEffect(spell, target, gameState)

    // Process any enemies defeated by the spell (award coins, remove from list)
    if (spell == SpellType.ATTACK_AIMED || spell == SpellType.ATTACK_AREA) {
        gameEngine?.processDefeatedAttackers()
    }

    // A spell can change the kill count (attack spells) and spend mana, which may satisfy
    // threshold-based scripted events (enemies killed, mana at or below). Evaluate them now so
    // they fire immediately during the player's turn rather than waiting for the next turn.
    gameEngine?.evaluateImmediateEvents()
    // Surface any messages queued by the kill (e.g. EWHAD_RETREATS/EWHAD_DEFEATED) or by a
    // scripted event the spell triggered.
    surfaceNextPendingMessageIfIdle()

    // Check if the spell killed the last enemy or caused a villain to breach a target
    // A villain breaching a target loses immediately, even if all enemies are defeated
    if (spell == SpellType.ATTACK_AIMED || spell == SpellType.ATTACK_AREA) {
        val currentStateAfterSpell = _gameState.value
        if (currentStateAfterSpell != null && currentStateAfterSpell.isLevelLost()) {
            completeLevel(currentStateAfterSpell.level.id, won = false)
        } else if (currentStateAfterSpell != null && currentStateAfterSpell.isLevelWon()) {
            completeLevel(currentStateAfterSpell.level.id, won = true)
        }
    }

    // Clear pending spell and targeting state
    _pendingSpellCast.value = null
    _pendingTokenSpell.value = null
    exitSpellTargetingMode()

    // Close magic panel after casting
    closeMagicPanel()

    if (LogConfig.ENABLE_SPELL_LOGGING) {
        println("=== SPELL: Spell cast COMPLETE - ${spell.displayName}")
    }
}

/**
 * Execute the effect of a cast spell
 */
private fun GameViewModel.executeSpellEffect(
    spell: SpellType,
    target: Any?,
    gameState: GameState,
) {
    when (spell) {
        SpellType.ATTACK_AIMED -> {
            // Attack Aimed: Deal 80 damage to the enemy on a targeted tile
            val position = target as? Position
            if (position != null) {
                val attacker = gameState.attackers.find { !it.isDefeated.value && it.position.value == position }
                if (attacker != null) {
                    damageAttacker(attacker, 80)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println(
                            "Attack Aimed: Dealt 80 damage to ${attacker.type.displayName} at $position (HP: ${attacker.currentHealth.value})",
                        )
                    }
                } else {
                    val bridge = gameState.getBridgeAt(position)
                    if (bridge != null && bridge.isActive) {
                        bridge.takeDamage(80)
                        if (LogConfig.ENABLE_SPELL_LOGGING) {
                            println("Attack Aimed: Dealt 80 damage to bridge at $position (HP: ${bridge.currentHealth.value})")
                        }
                    }
                }
            }
        }
        SpellType.HEAL -> {
            // Heal: Restore 3 health points (cap at max)
            val currentHP = gameState.healthPoints.value
            val maxHP = gameState.level.healthPoints + (_currentPlayer.value?.abilities?.healthAbility ?: 0)
            val newHP = (currentHP + 3).coerceAtMost(maxHP)
            gameState.healthPoints.value = newHP
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("Heal: Restored health to $newHP/$maxHP")
            }
        }
        SpellType.ATTACK_AREA -> {
            // Attack Area: Deal 50 damage to all enemies within 2 hex range of position
            val position = target as? Position
            if (position != null) {
                var damagedCount = 0
                gameState.attackers.filter { !it.isDefeated.value }.forEach { attacker ->
                    val distance = attacker.position.value.hexDistanceTo(position)
                    if (distance <= 2) {
                        damageAttacker(attacker, 50)
                        damagedCount++
                    }
                }
                gameState.bridges
                    .filter { it.isActive && it.positions.any { bridgePosition -> bridgePosition.hexDistanceTo(position) <= 2 } }
                    .forEach { it.takeDamage(50) }
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Attack Area: Dealt 50 damage to $damagedCount enemies within 2 hex range of $position")
                }
            }
        }
        SpellType.INSTANT_TOWER -> {
            // Instant Tower is handled via instantTowerSpellActive mode in GameState.
            // Mana is deferred and only consumed when a tower is placed (see placeDefender).
            // This fallback path activates the mode if called directly (e.g., from cheat codes).
            println("=== SPELL: WARN - Instant Tower executeSpellEffect fallback triggered; activating mode")
            gameState.instantTowerSpellActive.value = true
        }
        SpellType.DOUBLE_TOWER_LEVEL -> {
            // Double Tower Level: Double tower level for 1 turn
            val defender = target as? Defender
            if (defender != null) {
                val effect =
                    ActiveSpellEffect(
                        spell = SpellType.DOUBLE_TOWER_LEVEL,
                        defenderId = defender.id,
                        turnsRemaining = 1,
                        castTurn = gameState.turnNumber.value,
                    )
                gameState.activeSpellEffects.add(effect)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Double Tower Level: ${defender.type.displayName} level doubled for 1 turn!")
                }
            }
        }
        SpellType.DOUBLE_TOWER_REACH -> {
            // Double Tower Reach: Double tower range for 1 turn
            val defender = target as? Defender
            if (defender != null) {
                val effect =
                    ActiveSpellEffect(
                        spell = SpellType.DOUBLE_TOWER_REACH,
                        defenderId = defender.id,
                        turnsRemaining = 1,
                        castTurn = gameState.turnNumber.value,
                    )
                gameState.activeSpellEffects.add(effect)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Double Tower Reach: ${defender.type.displayName} range doubled for 1 turn!")
                }
            }
        }
        SpellType.BOMB -> {
            // Bomb: Place bomb at position, explodes after 2 turns
            val position = target as? Position
            if (position != null) {
                val effect =
                    ActiveSpellEffect(
                        spell = SpellType.BOMB,
                        position = position,
                        turnsRemaining = 3, // Explodes at the start of turn 3 (2 full turns + explosion)
                        castTurn = gameState.turnNumber.value,
                    )
                gameState.activeSpellEffects.add(effect)
                // Play ticking sound when bomb is placed
                GlobalSoundManager.playSound(SoundEvent.BOMB_TICKING)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Bomb: Placed at $position, will explode in 2 turns!")
                }
            }
        }
        SpellType.FREEZE_SPELL -> {
            // Freeze: Freeze enemy for 1+ turns (base 1 turn for 10 mana)
            val attacker = target as? Attacker
            if (attacker != null) {
                // Check immunity: Does not work on Demons, Dragons, Ewhad
                val isImmune =
                    attacker.type.isDragon ||
                        attacker.type == AttackerType.BLUE_DEMON ||
                        attacker.type == AttackerType.RED_DEMON ||
                        attacker.type == AttackerType.EWHAD ||
                        gameState.isProtectedByRuneGlyphWard(attacker)

                if (isImmune) {
                    println("Freeze Spell: ${attacker.type.displayName} is immune to freeze!")
                } else {
                    // For now, freeze for 1 turn (base cost)
                    // TODO: Add duration selection dialog for spending more mana
                    val effect =
                        ActiveSpellEffect(
                            spell = SpellType.FREEZE_SPELL,
                            attackerId = attacker.id,
                            turnsRemaining = 1,
                            castTurn = gameState.turnNumber.value,
                        )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println("Freeze Spell: Froze ${attacker.type.displayName} for 1 turn!")
                    }
                }
            }
        }
        SpellType.COOLING_SPELL -> {
            // Cooling Spell: Create area that slows enemies for 3 turns
            val position = target as? Position
            if (position != null) {
                val effect =
                    ActiveSpellEffect(
                        spell = SpellType.COOLING_SPELL,
                        position = position,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value,
                    )
                gameState.activeSpellEffects.add(effect)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Cooling Spell: Created cooling area at $position for 3 turns!")
                }
            }
        }
        SpellType.FEAR_SPELL -> {
            // Fear Spell: Single target enemy flees towards spawn for 3 turns
            val attacker = target as? Attacker
            if (attacker != null) {
                val effect =
                    ActiveSpellEffect(
                        spell = SpellType.FEAR_SPELL,
                        attackerId = attacker.id,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value,
                    )
                gameState.activeSpellEffects.add(effect)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Fear Spell: ${attacker.type.displayName} flees for 3 turns!")
                }
            }
        }
        SpellType.FEAR_SPELL_AREA -> {
            // Fear Spell (Area): Create fear zone that makes enemies flee for 3 turns
            val position = target as? Position
            if (position != null) {
                val effect =
                    ActiveSpellEffect(
                        spell = SpellType.FEAR_SPELL_AREA,
                        position = position,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value,
                    )
                gameState.activeSpellEffects.add(effect)
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Fear Spell (Area): Created fear zone at $position for 3 turns!")
                }
            }
        }
    }
}

/**
 * Enter spell targeting mode
 */
fun GameViewModel.enterSpellTargetingMode(spell: SpellType) {
    val gameState = _gameState.value ?: return

    // Calculate valid targets based on spell type
    val validTargets: Set<Any> =
        when (spell.targetType) {
            SpellTargetType.POSITION -> {
                if (spell == SpellType.BOMB) {
                    // Bomb can only be placed on empty path tiles:
                    // no enemies, no barricades, no field effects, no traps, no other bombs
                    val occupiedByEnemy =
                        gameState.attackers
                            .filter { !it.isDefeated.value }
                            .map { it.position.value }
                            .toSet()
                    val occupiedByBarricade = gameState.barricades.map { it.position }.toSet()
                    val occupiedByFieldEffect = gameState.fieldEffects.map { it.position }.toSet()
                    val occupiedByTrap = gameState.traps.map { it.position }.toSet()
                    val occupiedByFief = gameState.fiefs.map { it.position }.toSet()
                    val occupiedByBomb =
                        gameState.activeSpellEffects
                            .filter { it.spell == SpellType.BOMB && it.position != null }
                            .map { it.position!! }
                            .toSet()
                    val blocked =
                        occupiedByEnemy + occupiedByBarricade + occupiedByFieldEffect +
                            occupiedByTrap + occupiedByBomb + occupiedByFief
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            val pos = Position(x, y)
                            if (gameState.level.isOnPath(pos) && pos !in blocked) {
                                positions.add(pos)
                            }
                        }
                    }
                    positions
                } else if (spell == SpellType.COOLING_SPELL) {
                    // Cooling spell can only be placed on path/spawn tiles without barricades
                    val occupiedByBarricade = gameState.barricades.map { it.position }.toSet()
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            val pos = Position(x, y)
                            if (gameState.level.isEnemyTraversable(pos) &&
                                pos !in occupiedByBarricade
                            ) {
                                positions.add(pos)
                            }
                        }
                    }
                    positions
                } else if (spell == SpellType.ATTACK_AREA) {
                    // Attack Area: enemy-occupiable tiles (path/spawn/river/bridge) without a barricade
                    val occupiedByBarricade =
                        gameState.barricades
                            .filter { !it.isDestroyed() }
                            .map { it.position }
                            .toSet()
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            val pos = Position(x, y)
                            if ((gameState.level.isEnemyOccupiable(pos) || gameState.isBridgeAt(pos)) && pos !in occupiedByBarricade) {
                                positions.add(pos)
                            }
                        }
                    }
                    positions
                } else if (spell == SpellType.ATTACK_AIMED) {
                    // Attack Aimed: tiles that have an enemy or active bridge on them
                    val attackerPositions =
                        gameState.attackers
                            .filter { !it.isDefeated.value }
                            .map { it.position.value }
                            .toSet()
                    val bridgePositions =
                        gameState.bridges
                            .filter { it.isActive }
                            .flatMap { it.positions }
                            .toSet()
                    attackerPositions + bridgePositions
                } else {
                    // All tiles on the map are valid positions for other spells
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            positions.add(Position(x, y))
                        }
                    }
                    positions
                }
            }
            SpellTargetType.ENEMY -> {
                // All active (non-defeated) enemies are valid targets
                gameState.attackers.filter { !it.isDefeated.value }.toSet()
            }
            SpellTargetType.TOWER -> {
                // All placed defenders are valid targets
                gameState.defenders.toSet()
            }
            SpellTargetType.NONE -> emptySet()
        }

    // Set targeting state
    gameState.spellTargeting.value =
        SpellTargetingState(
            activeSpell = spell,
            validTargets = validTargets,
        )

    // Clear pending spell cast (we're now in targeting mode)
    _pendingSpellCast.value = null
    // Close magic panel so the targeting instruction card becomes visible
    _showMagicPanel.value = false
}

/**
 * Exit spell targeting mode (cancel)
 */
fun GameViewModel.exitSpellTargetingMode() {
    val gameState = _gameState.value ?: return
    gameState.spellTargeting.value = null
    _pendingTokenSpell.value = null
}

/**
 * Select a target and cast the spell
 */
fun GameViewModel.selectSpellTarget(target: Any) {
    val gameState = _gameState.value ?: return
    val targeting = gameState.spellTargeting.value ?: return

    // Validate target is in valid targets set
    if (!targeting.validTargets.contains(target)) {
        println("Invalid target for spell")
        return
    }

    // Show confirmation dialog with target details (or warning for immune enemies)
    onSpellTargetSelected(target)
}

/**
 * Handle a click on a level support spell token box.
 * Casting a spell via a token does not consume mana and works even if the spell is not unlocked.
 * Toggles the selection; enters targeting mode for targeting spells or shows a confirmation for others.
 */
fun GameViewModel.onSupportSpellTokenClicked(spell: SpellType) {
    val gameState = _gameState.value ?: return
    if ((gameState.supportSpellsRemaining[spell] ?: 0) <= 0) return

    // Spell tokens can only be used once the level has started (not during initial build phase)
    if (gameState.phase.value == GamePhase.INITIAL_BUILDING) return

    // Toggle: if the same token is already active, cancel it
    if (_pendingTokenSpell.value == spell) {
        _pendingTokenSpell.value = null
        _selectedSpell.value = null
        _pendingSpellCast.value = null
        _showSpellTargetConfirmation.value = null
        exitSpellTargetingMode()
        return
    }

    _pendingTokenSpell.value = spell
    _selectedSpell.value = spell
    // Close the magic panel if it is open so token targeting is visible
    _showMagicPanel.value = false

    if (spell.requiresTarget) {
        enterSpellTargetingMode(spell)
    } else {
        _showSpellTargetConfirmation.value = Pair(spell, Unit)
    }
}

/**
 * Place a player-granted support object (trap, magical trap, or barricade) at a position.
 * Does not require a tower or tech level and does not consume tower actions.
 * Returns true if the object was placed and a token consumed.
 */
fun GameViewModel.placeSupportObject(
    type: SupportObjectType,
    position: Position,
): Boolean {
    val gameState = _gameState.value ?: return false
    val remaining = gameState.supportObjectsRemaining[type] ?: 0
    if (remaining <= 0) return false

    val config =
        gameState.level.supports.objects
            .firstOrNull { it.type == type }
    val success =
        when (type) {
            SupportObjectType.DWARVEN_TRAP ->
                gameEngine?.placeSupportTrap(position, config?.damage ?: 10, TrapType.DWARVEN) ?: false
            SupportObjectType.MAGICAL_TRAP ->
                gameEngine?.placeSupportTrap(position, 0, TrapType.MAGICAL) ?: false
            SupportObjectType.BARRICADE ->
                gameEngine?.placeSupportBarricade(position, config?.healthPoints ?: 50) ?: false
        }

    if (success) {
        gameState.supportObjectsRemaining[type] = consumeSupportCount(remaining)
    }
    return success
}

/**
 * Place a player-granted fief support token at a position.
 * Does not require a tower or tech level and does not consume tower actions.
 * Returns true if the fief was placed and a token consumed.
 */
fun GameViewModel.placeSupportFief(
    type: de.egril.defender.model.FiefType,
    position: de.egril.defender.model.Position,
): Boolean {
    val gameState = _gameState.value ?: return false
    val remaining = gameState.supportFiefRemaining[type] ?: 0
    if (remaining <= 0) return false

    val success = gameEngine?.placeSupportFief(position, type) ?: false

    if (success) {
        gameState.supportFiefRemaining[type] = consumeSupportCount(remaining)
    }
    return success
}

/**
 * Activate a cooldown-based support power for the whole level.
 *
 * Does nothing if the power is not part of the level, is still on cooldown, or the level has not
 * started yet (initial building phase). On success the power's effect is applied immediately and
 * its cooldown is (re)started.
 */
fun GameViewModel.activateCooldownPower(type: CooldownPowerType) {
    val gameState = _gameState.value ?: return
    val power =
        gameState.level.supports.cooldownPowers
            .firstOrNull { it.type == type } ?: return

    // Powers can only be used once the level has started (not during the initial build phase)
    if (gameState.phase.value == GamePhase.INITIAL_BUILDING) return

    // Must be ready (readyIn == 0)
    if ((gameState.cooldownPowerReadyIn[type] ?: 0) > 0) return

    applyCooldownPowerEffect(gameState, type)

    // Start the cooldown
    gameState.cooldownPowerReadyIn[type] = power.cooldownTurns
}

/** Apply the immediate effect of a cooldown support power. */
internal fun GameViewModel.applyCooldownPowerEffect(
    gameState: GameState,
    type: CooldownPowerType,
) {
    when (type) {
        CooldownPowerType.COIN_SURGE -> {
            // Double all coins earned for the remainder of this turn/round.
            gameState.coinSurgeActive.value = true
        }
        CooldownPowerType.SKY_IS_FALLING -> {
            // Trigger the full-map falling-meteor animation overlay first, then remove the
            // enemies' health once the meteors have visibly struck the map.
            gameState.skyIsFallingTrigger.value += 1
            viewModelScope.launch {
                if (AppSettings.enableAnimations.value) {
                    delay(SKY_IS_FALLING_DURATION_MS.toLong())
                }
                applySkyIsFallingDamage(gameState)
            }
        }
        CooldownPowerType.CONSTRUCTION_REPAIRS -> {
            // All existing barricades gain 10 health points.
            gameState.barricades
                .filterNot { it.isDestroyed() }
                .forEach { it.reinforce(GameViewModel.CONSTRUCTION_REPAIRS_HP) }
        }
        CooldownPowerType.MANA_WELL -> gameEngine?.addMana(GameViewModel.MANA_WELL_MANA)
        CooldownPowerType.DEEP_MANA_WELL -> gameEngine?.addMana(GameViewModel.DEEP_MANA_WELL_MANA)
    }
}

/** Apply the "Sky is Falling" damage to all live enemies and resolve any defeats. */
private fun GameViewModel.applySkyIsFallingDamage(gameState: GameState) {
    // All enemy units lose 10 health points.
    gameState.attackers
        .filter { !it.isDefeated.value }
        .forEach { attacker ->
            damageAttacker(attacker, GameViewModel.SKY_IS_FALLING_DAMAGE)
        }
    // Award coins/XP and remove defeated enemies immediately (mirrors damage spells).
    gameEngine?.processDefeatedAttackers()
    surfaceNextPendingMessageIfIdle()
    val stateAfter = _gameState.value
    // Check loss first: a villain breaching a target loses immediately, even if all enemies are defeated
    if (stateAfter != null && stateAfter.isLevelLost()) {
        completeLevel(stateAfter.level.id, won = false)
    } else if (stateAfter != null && stateAfter.isLevelWon()) {
        completeLevel(stateAfter.level.id, won = true)
    }
}

private fun GameViewModel.damageAttacker(
    attacker: Attacker,
    damage: Int,
) {
    val gameState = _gameState.value
    if (gameState?.isProtectedByObsidianProtector(attacker) == true) return
    val effectiveDamage = (damage - (gameState?.runeGlyphWardArmor(attacker) ?: 0)).coerceAtLeast(0)
    attacker.recordDamageTaken(minOf(attacker.currentHealth.value, effectiveDamage))
    attacker.currentHealth.value = (attacker.currentHealth.value - effectiveDamage).coerceAtLeast(0)
    if (attacker.currentHealth.value <= 0) {
        attacker.isDefeated.value = true
    }
}
