package de.egril.defender.ui

import de.egril.defender.utils.CheatCodeHandler

/**
 * Sandbox: spawn an adjustable test enemy (type + level) onto the running level.
 */
fun GameViewModel.sandboxSpawnEnemy(
    type: de.egril.defender.model.AttackerType,
    level: Int,
    spawnPoint: de.egril.defender.model.Position? = null,
) {
    if (_gameState.value?.level?.isSandbox != true) return
    gameEngine?.spawnEnemy(type, level.coerceAtLeast(1), spawnPoint)
    surfaceNextPendingMessageIfIdle()
}

/**
 * Sandbox: add coins to the running level.
 */
fun GameViewModel.sandboxAddCoins(amount: Int = 1000) {
    if (_gameState.value?.level?.isSandbox != true) return
    gameEngine?.addCoins(amount)
}

/**
 * Sandbox: repaint a map tile at runtime to the given tile type.
 */
fun GameViewModel.sandboxPaintTile(
    position: de.egril.defender.model.Position,
    tileType: de.egril.defender.editor.TileType,
    riverFlow: de.egril.defender.model.RiverFlow = de.egril.defender.model.RiverFlow.EAST,
    riverSpeed: Int = 1,
) {
    val state = _gameState.value ?: return
    if (!state.level.isSandbox) return
    state.sandboxPaintTile(position, tileType, riverFlow, riverSpeed)
}

fun GameViewModel.applyCheatCode(code: String): Boolean {
    // Check for reminder testing cheat codes first
    val lowerCode = code.lowercase().trim()
    if (lowerCode == "breakreminder" || lowerCode == "break") {
        // Trigger break reminder with current session time
        val currentTime =
            de.egril.defender.utils
                .currentTimeMillis()
        gameSessionStartTime?.let { sessionStart ->
            val elapsedMs = currentTime - sessionStart
            _reminderMessage.value =
                ReminderMessage(
                    type = de.egril.defender.ui.gameplay.ReminderType.BREAK,
                    elapsedMs = elapsedMs,
                )
        }
        return true
    }

    if (lowerCode == "sleepreminder" || lowerCode == "sleep") {
        // Trigger sleep reminder
        val currentTime =
            de.egril.defender.utils
                .currentTimeMillis()
        val hour =
            de.egril.defender.utils
                .getLocalHour(currentTime)
        val timeDescription =
            when {
                hour == 23 -> "close_to_midnight"
                hour == 0 -> "midnight"
                else -> "after_midnight"
            }
        _reminderMessage.value =
            ReminderMessage(
                type = de.egril.defender.ui.gameplay.ReminderType.SLEEP,
                timeDescription = timeDescription,
            )
        return true
    }

    val (success, digOutcome) =
        CheatCodeHandler.applyCheatCode(
            code = code,
            addCoins = { amount -> gameEngine?.addCoins(amount) },
            setCoins = { amount -> gameEngine?.setCoins(amount) },
            performMineDigWithOutcome = { outcome -> performMineDigWithOutcome(outcome) },
            spawnEnemy = { attackerType, level -> gameEngine?.spawnEnemy(attackerType, level) },
            showPlatformInfo = { _showPlatformInfo.value = true },
            setBigHeadMode = { enabled -> de.egril.defender.utils.BigHeadMode.isEnabled.value = enabled },
            addMana = { amount -> gameEngine?.addMana(amount) },
            removeMana = { amount -> gameEngine?.removeMana(amount) },
            showCheatHelp = { _showCheatHelp.value = true },
        )

    if (digOutcome != null) {
        _cheatDigOutcome.value = digOutcome
    }

    return success
}

fun GameViewModel.clearCheatDigOutcome() {
    _cheatDigOutcome.value = null
}

fun GameViewModel.clearPlatformInfo() {
    _showPlatformInfo.value = false
}

fun GameViewModel.clearCheatHelp() {
    _showCheatHelp.value = false
}

// Player stat/XP/spell cheat methods
private fun GameViewModel.addPlayerXP(amount: Int) {
    val currentPlayer = _currentPlayer.value ?: return
    val updatedStats = currentPlayer.abilities.addXP(amount)
    val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
    _currentPlayer.value = updatedPlayer
    de.egril.defender.save.PlayerProfileStorage
        .updateProfile(updatedPlayer)
    uploadUserDataToBackend()
}

private fun GameViewModel.removePlayerXP(amount: Int) {
    val currentPlayer = _currentPlayer.value ?: return
    val currentXP = currentPlayer.abilities.totalXP
    val newXP = maxOf(0, currentXP - amount)
    val newLevel =
        de.egril.defender.model.PlayerAbilities
            .calculateLevel(newXP)
    val updatedStats = currentPlayer.abilities.copy(totalXP = newXP, level = newLevel)
    val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
    _currentPlayer.value = updatedPlayer
    de.egril.defender.save.PlayerProfileStorage
        .updateProfile(updatedPlayer)
    uploadUserDataToBackend()
}

private fun GameViewModel.addPlayerStat(
    statName: String,
    amount: Int,
) {
    val currentPlayer = _currentPlayer.value ?: return
    val oldStats = currentPlayer.abilities

    val updatedStats =
        when (statName.lowercase()) {
            "health" -> oldStats.copy(healthAbility = oldStats.healthAbility + amount)
            "treasury" -> oldStats.copy(treasuryAbility = oldStats.treasuryAbility + amount)
            "income" -> oldStats.copy(incomeAbility = oldStats.incomeAbility + amount)
            "construction" -> oldStats.copy(constructionAbility = oldStats.constructionAbility + amount)
            "mana" -> oldStats.copy(manaAbility = oldStats.manaAbility + amount)
            else -> return // Invalid stat name
        }

    val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
    _currentPlayer.value = updatedPlayer
    de.egril.defender.save.PlayerProfileStorage
        .updateProfile(updatedPlayer)
    uploadUserDataToBackend()
}

private fun GameViewModel.removePlayerStat(
    statName: String,
    amount: Int,
) {
    val currentPlayer = _currentPlayer.value ?: return
    val oldStats = currentPlayer.abilities

    val updatedStats =
        when (statName.lowercase()) {
            "health" -> oldStats.copy(healthAbility = maxOf(0, oldStats.healthAbility - amount))
            "treasury" -> oldStats.copy(treasuryAbility = maxOf(0, oldStats.treasuryAbility - amount))
            "income" -> oldStats.copy(incomeAbility = maxOf(0, oldStats.incomeAbility - amount))
            "construction" -> oldStats.copy(constructionAbility = maxOf(0, oldStats.constructionAbility - amount))
            "mana" -> oldStats.copy(manaAbility = maxOf(0, oldStats.manaAbility - amount))
            else -> return // Invalid stat name
        }

    val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
    _currentPlayer.value = updatedPlayer
    de.egril.defender.save.PlayerProfileStorage
        .updateProfile(updatedPlayer)
    uploadUserDataToBackend()
}

private fun GameViewModel.unlockPlayerSpell(spellName: String) {
    val currentPlayer = _currentPlayer.value ?: return
    val oldStats = currentPlayer.abilities

    // Parse spell name to SpellType
    val spell =
        when (spellName.lowercase().replace(" ", "_")) {
            "attack_aimed", "attackaimed" -> de.egril.defender.model.SpellType.ATTACK_AIMED
            "attack_area", "attackarea", "fireball" -> de.egril.defender.model.SpellType.ATTACK_AREA
            "heal" -> de.egril.defender.model.SpellType.HEAL
            "instant_tower", "instanttower" -> de.egril.defender.model.SpellType.INSTANT_TOWER
            "bomb" -> de.egril.defender.model.SpellType.BOMB
            "double_level", "doublelevel", "double_tower_level", "doubletowerlevel" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_LEVEL
            "cooling", "cooling_spell", "coolingspell" -> de.egril.defender.model.SpellType.COOLING_SPELL
            "freeze", "freeze_spell", "freezespell" -> de.egril.defender.model.SpellType.FREEZE_SPELL
            "double_reach", "doublereach", "double_tower_reach", "doubletowerreach" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_REACH
            "fear", "fear_spell", "fearspell" -> de.egril.defender.model.SpellType.FEAR_SPELL
            "fear_area", "feararea", "fear_spell_area", "fearspellarea" -> de.egril.defender.model.SpellType.FEAR_SPELL_AREA
            else -> return // Invalid spell name
        }

    val updatedStats = oldStats.copy(unlockedSpells = oldStats.unlockedSpells + spell)
    val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
    _currentPlayer.value = updatedPlayer
    de.egril.defender.save.PlayerProfileStorage
        .updateProfile(updatedPlayer)
    uploadUserDataToBackend()
}

private fun GameViewModel.lockPlayerSpell(spellName: String) {
    val currentPlayer = _currentPlayer.value ?: return
    val oldStats = currentPlayer.abilities

    // Parse spell name to SpellType
    val spell =
        when (spellName.lowercase().replace(" ", "_")) {
            "attack_aimed", "attackaimed" -> de.egril.defender.model.SpellType.ATTACK_AIMED
            "attack_area", "attackarea", "fireball" -> de.egril.defender.model.SpellType.ATTACK_AREA
            "heal" -> de.egril.defender.model.SpellType.HEAL
            "instant_tower", "instanttower" -> de.egril.defender.model.SpellType.INSTANT_TOWER
            "bomb" -> de.egril.defender.model.SpellType.BOMB
            "double_level", "doublelevel", "double_tower_level", "doubletowerlevel" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_LEVEL
            "cooling", "cooling_spell", "coolingspell" -> de.egril.defender.model.SpellType.COOLING_SPELL
            "freeze", "freeze_spell", "freezespell" -> de.egril.defender.model.SpellType.FREEZE_SPELL
            "double_reach", "doublereach", "double_tower_reach", "doubletowerreach" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_REACH
            "fear", "fear_spell", "fearspell" -> de.egril.defender.model.SpellType.FEAR_SPELL
            "fear_area", "feararea", "fear_spell_area", "fearspellarea" -> de.egril.defender.model.SpellType.FEAR_SPELL_AREA
            else -> return // Invalid spell name
        }

    val updatedStats = oldStats.copy(unlockedSpells = oldStats.unlockedSpells - spell)
    val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
    _currentPlayer.value = updatedPlayer
    de.egril.defender.save.PlayerProfileStorage
        .updateProfile(updatedPlayer)
    uploadUserDataToBackend()
}

fun GameViewModel.applyWorldMapCheatCode(code: String): Boolean {
    // Check for "demo" cheat code (starts automated demo mode)
    if (code.lowercase().trim() == "demo") {
        startDemoMode()
        return true
    }
    if (code.lowercase().trim() == "demodemo") {
        startDemoMode(de.egril.defender.game.DemoMode.Scenario.DEMO_DEMO)
        return true
    }

    // Check for "sticker" cheat code first (navigation cheat)
    if (code.lowercase().trim() == "sticker") {
        navigateToSticker()
        return true
    }

    // Check for "spinner" cheat code (shows loading spinner demo for 30s)
    if (code.lowercase().trim() == "spinner") {
        navigateToLoadingSpinnerDemo()
        return true
    }

    // Check for "credits" cheat code (shows the final credits screen)
    if (code.lowercase().trim() == "credits") {
        navigateToFinalCredits()
        return true
    }

    // Check for "animation" cheat code (shows the animation test/preview screen)
    if (code.lowercase().trim() == "animation") {
        navigateToAnimationTest()
        return true
    }

    return CheatCodeHandler.applyWorldMapCheatCode(
        code = code,
        unlockAllLevels = { unlockAllLevels() },
        unlockLevel = { editorLevelId -> unlockLevel(editorLevelId) },
        lockAllLevels = { lockAllLevels() },
        lockLevel = { editorLevelId -> lockLevel(editorLevelId) },
        worldLevels = _worldLevels.value,
        showPlatformInfo = { _showPlatformInfo.value = true },
        addXP = { amount -> addPlayerXP(amount) },
        removeXP = { amount -> removePlayerXP(amount) },
        addStatLevel = { statName, amount -> addPlayerStat(statName, amount) },
        removeStatLevel = { statName, amount -> removePlayerStat(statName, amount) },
        unlockSpell = { spellName -> unlockPlayerSpell(spellName) },
        lockSpell = { spellName -> lockPlayerSpell(spellName) },
        showCheatHelp = { _showCheatHelp.value = true },
    )
}

private fun GameViewModel.unlockAllLevels() {
    _worldLevels.value = CheatCodeHandler.unlockAllLevels(_worldLevels.value)
    // Save updated world map status
    saveWorldMapStatus()
    uploadUserDataToBackend()
}

private fun GameViewModel.unlockLevel(editorLevelId: String) {
    _worldLevels.value = CheatCodeHandler.unlockLevel(_worldLevels.value, editorLevelId)
    // Save updated world map status
    saveWorldMapStatus()
    uploadUserDataToBackend()
}

private fun GameViewModel.lockAllLevels() {
    _worldLevels.value = CheatCodeHandler.lockAllLevels(_worldLevels.value)
    // Save updated world map status
    saveWorldMapStatus()
    uploadUserDataToBackend()
}

private fun GameViewModel.lockLevel(editorLevelId: String) {
    _worldLevels.value = CheatCodeHandler.lockLevel(_worldLevels.value, editorLevelId)
    // Save updated world map status
    saveWorldMapStatus()
    uploadUserDataToBackend()
}
