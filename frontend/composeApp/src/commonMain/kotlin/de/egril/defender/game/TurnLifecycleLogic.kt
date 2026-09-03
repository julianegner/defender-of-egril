package de.egril.defender.game

import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.config.LogConfig
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.BombExplosionEffect
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.TowerConstructionEffect
import de.egril.defender.model.getHexNeighborsWithinRadius
import de.egril.defender.model.hexDistanceTo

class TurnLifecycleLogic(
    private val state: GameState,
    private val enemyMovement: EnemyMovementSystem,
    private val enemyAbilities: EnemyAbilitySystem,
    private val bridgeSystem: BridgeSystem,
    private val raftSystem: RaftSystem,
    private val combatSystem: CombatSystem,
    private val eventScriptSystem: EventScriptSystem,
    private val waaghLogic: WaaghLogic,
    private val processSoulCallResurrections: () -> Unit,
    private val spawnInitialEnemies: () -> Unit,
    private val checkAndActivateTraps: () -> Unit,
    private val startTurnTracking: () -> Unit,
    private val recordDragonLevelChange: (Attacker, Int) -> Unit,
) {
    fun startFirstPlayerTurn() {
        if (state.phase.value != GamePhase.INITIAL_BUILDING) return

        GameLogBuffer.log("GAME", "Game started — Turn 1, ${state.defenders.size} towers placed")
        GlobalSoundManager.playSound(SoundEvent.BATTLE_START)
        startTurnTracking()

        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 1

        if (state.currentWaveIndex.value == 0 && state.attackersToSpawn.isEmpty()) {
            enemyMovement.loadNextWave()
        }

        spawnInitialEnemies()
        resetDefenderActions()
        eventScriptSystem.evaluate(EventTrigger.PLAYER_TURN_START)
    }

    fun startEnemyTurn() {
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("GameEngine.startEnemyTurn: phase=${state.phase.value}")
        }
        if (state.phase.value != GamePhase.PLAYER_TURN) {
            println("GameEngine.startEnemyTurn: Not in PLAYER_TURN phase, returning")
            return
        }

        state.turnNumber.value++
        processSoulCallResurrections()
        state.phase.value = GamePhase.ENEMY_TURN
        waaghLogic.updateWaaghFrenzyAtEnemyTurnStart()
        enemyAbilities.processSnotlingGrowth()
        state.enemyTurnStartPositions.clear()
        state.attackers
            .filter { !it.isDefeated.value }
            .forEach { attacker ->
                state.enemyTurnStartPositions[attacker.id] = attacker.position.value
                attacker.teleportedThisTurn.value = false
            }
        // Reset portal usage so each portal can be used once per enemy turn.
        state.activePortals.forEach { it.usedThisTurn.value = false }

        eventScriptSystem.evaluate(EventTrigger.ENEMY_TURN_START)

        GameLogBuffer.log(
            "TURN",
            "Enemy turn ${state.turnNumber.value} — HP: ${state.healthPoints.value}, Coins: ${state.coins.value}, Enemies alive: ${state.attackers.count {
                !it.isDefeated.value
            }}",
        )
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("GameEngine.startEnemyTurn: Changed phase to ENEMY_TURN, turn=${state.turnNumber.value}")
        }

        state.trapTriggerEffects.clear()

        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("GameEngine.startEnemyTurn: About to call raftSystem.processRaftMovements()")
        }
        raftSystem.processRaftMovements()
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("GameEngine.startEnemyTurn: Completed raft movement processing")
        }

        val hasBombs = state.activeSpellEffects.any { it.spell == SpellType.BOMB }
        if (hasBombs) {
            GlobalSoundManager.playSound(SoundEvent.BOMB_TICKING)
        }
    }

    fun spawnEnemyTurnAttackers() {
        enemyMovement.spawnAttackers()
    }

    fun completeEnemyTurn() {
        if (state.phase.value != GamePhase.ENEMY_TURN) return

        if (state.pendingCoinGains.value > 0) {
            state.coins.value += state.pendingCoinGains.value
            state.pendingCoinGains.value = 0
        }

        state.playedTileAnimationKeys.clear()
        state.bombExplosionEffects.clear()
        state.defeatedEnemyEffects.clear()
        state.coinGainEffects.clear()
        state.towerAttackEffects.clear()
        state.constructionCompleteEffects.clear()
        state.enemySpawnEffects.clear()
        state.enemyMoveEffects.clear()
        state.dragonLevelChangeEffects.clear()
        state.mineDigEffects.clear()
        state.arrowAttackEffects.clear()
        state.ballistaAttackEffects.clear()
        state.bowAttackEffects.clear()
        state.spearAttackEffects.clear()
        state.wizardAttackEffects.clear()
        state.alchemyAttackEffects.clear()
        state.rocketAttackEffects.clear()
        state.snotlingCannonThrowEffects.clear()
        state.garokkWarCryEffects.clear()
        state.shadowSpewEffects.clear()
        state.morvathShadowOrbEffects.clear()

        tickBloodlustAfterMovement()
        enemyAbilities.processHordeEating()
        checkAndActivateTraps()
        combatSystem.applyLastingEffects()
        enemyMovement.updateFieldEffects()
        updateSpellBuffs()
        enemyAbilities.processEnemyAbilities()
        bridgeSystem.processBridges()
        combatSystem.processDefeatedAttackers()

        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            enemyMovement.loadNextWave()
        }

        advanceBuildTimers()
        enemyAbilities.updateTowerDisableStatus()
        state.trapTriggerEffects.clear()

        for (type in state.cooldownPowerReadyIn.keys.toList()) {
            val remaining = state.cooldownPowerReadyIn[type] ?: 0
            if (remaining > 0) {
                state.cooldownPowerReadyIn[type] = remaining - 1
            }
        }
        state.coinSurgeActive.value = false

        val fiefIncome = state.fiefs.sumOf { it.type.incomePerTurn }
        if (fiefIncome > 0) {
            state.coins.value += fiefIncome
        }

        state.phase.value = GamePhase.PLAYER_TURN
        resetDefenderActions()
        eventScriptSystem.evaluate(EventTrigger.PLAYER_TURN_START)
        waaghLogic.updateWaaghFrenzyAtEnemyTurnEnd()
    }

    private fun resetDefenderActions() {
        state.defenders.forEach { it.resetActions() }
    }

    private fun advanceBuildTimers() {
        state.defenders.forEach { defender ->
            if (defender.buildTimeRemaining.value > 0) {
                defender.buildTimeRemaining.value--
                if (defender.buildTimeRemaining.value == 0) {
                    defender.resetActions()
                    state.constructionCompleteEffects.add(
                        TowerConstructionEffect(
                            position = defender.position.value,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                }
            }
            if (defender.type == DefenderType.WIZARD_TOWER && defender.trapCooldownRemaining.value > 0) {
                defender.trapCooldownRemaining.value--
            }
        }
    }

    private fun tickBloodlustAfterMovement() {
        state.attackers.forEach { attacker ->
            if (attacker.bloodlustRoundsLeft.value > 0) {
                attacker.bloodlustRoundsLeft.value--
            }
            if (attacker.mushroomTurnsRemaining.value > 0) {
                attacker.mushroomTurnsRemaining.value--
                if (attacker.mushroomTurnsRemaining.value == 0) {
                    attacker.currentHealth.value =
                        if (attacker.isDefeated.value) {
                            maxOf(0, attacker.currentHealth.value - attacker.mushroomBonusHealth)
                        } else {
                            maxOf(1, attacker.currentHealth.value - attacker.mushroomBonusHealth)
                        }
                    attacker.mushroomLevelBonus.value = 0
                }
            }
        }
    }

    private fun updateSpellBuffs() {
        val toRemove = mutableListOf<Int>()

        state.activeSpellEffects.forEachIndexed { index, effect ->
            val newTurnsRemaining = effect.turnsRemaining - 1

            if (newTurnsRemaining <= 0) {
                when (effect.spell) {
                    SpellType.BOMB -> {
                        if (effect.position != null) {
                            executeBombExplosion(effect.position)
                        }
                    }
                    else -> {
                    }
                }

                toRemove.add(index)
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Spell effect ${effect.spell.displayName} expired")
                }
            } else {
                state.activeSpellEffects[index] = effect.copy(turnsRemaining = newTurnsRemaining)
            }
        }

        toRemove.reversed().forEach { index ->
            state.activeSpellEffects.removeAt(index)
        }
    }

    private fun executeBombExplosion(position: Position) {
        val explosionRange = 3
        var enemiesDamaged = 0
        var barricadesDamaged = 0

        fun damageAt(distance: Int): Int =
            when (distance) {
                0 -> 200
                1 -> 150
                2 -> 100
                else -> 50
            }

        state.attackers.forEach { attacker ->
            if (!attacker.isDefeated.value) {
                val distance = attacker.position.value.hexDistanceTo(position)
                if (distance <= explosionRange && !attacker.type.isMirrorImage) {
                    val dmg = damageAt(distance)
                    attacker.currentHealth.value = (attacker.currentHealth.value - dmg).coerceAtLeast(0)
                    enemiesDamaged++
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
                    }
                    if (attacker.type.isDragon) {
                        val oldLevelBomb = attacker.level.value
                        attacker.updateDragonLevel()
                        recordDragonLevelChange(attacker, oldLevelBomb)
                    }
                }
            }
        }

        state.barricades.forEach { barricade ->
            val distance = barricade.position.hexDistanceTo(position)
            if (distance <= explosionRange) {
                val dmg = damageAt(distance)
                barricade.healthPoints.value = (barricade.healthPoints.value - dmg).coerceAtLeast(0)
                barricadesDamaged++
            }
        }

        val bridgesToRemove =
            state.bridges.filter { bridge ->
                bridge.positions.any { bridgePos ->
                    bridgePos.hexDistanceTo(position) <= explosionRange
                }
            }
        val bridgesDestroyed = bridgesToRemove.size
        bridgesToRemove.forEach { bridge ->
            state.bridges.remove(bridge)
        }

        combatSystem.processDefeatedAttackers()

        val affectedPositions =
            position
                .getHexNeighborsWithinRadius(
                    explosionRange,
                    state.level.gridWidth,
                    state.level.gridHeight,
                ).toMutableList()
        affectedPositions.add(position)

        state.bombExplosionEffects.add(
            BombExplosionEffect(
                center = position,
                affectedPositions = affectedPositions,
                turnNumber = state.turnNumber.value,
            ),
        )

        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println(
                "Bomb exploded at $position! Damaged $enemiesDamaged enemies, $barricadesDamaged barricades, destroyed $bridgesDestroyed bridges",
            )
        }
        GlobalSoundManager.playSound(SoundEvent.BOMB_EXPLOSION)
    }
}
