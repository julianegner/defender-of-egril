package de.egril.defender.ui.gameplay

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import com.hyperether.resources.stringResource
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*

private const val SOUND_CAPTION_DISPLAY_DURATION_MS = 2000L

@Composable
internal fun GamePlayScreenEffects(
    state: GamePlayScreenState,
    gameState: GameState,
    isDemoMode: Boolean,
    demoSelectedDefenderType: DefenderType?,
    demoSelectedDefenderId: Int?,
    demoSelectedTargetPosition: Position?,
    screenFocusRequester: FocusRequester,
    captionsEnabled: Boolean,
    cheatDigOutcome: DigOutcome?,
    onClearCheatDigOutcome: (() -> Unit)?,
) {
    with(state) {
        LaunchedEffect(pendingDigKey) {
            val outcome = pendingDigOutcome ?: return@LaunchedEffect
            if (AppSettings.enableAnimations.value) {
                kotlinx.coroutines.delay(1500L)
            }
            currentDigOutcome = outcome
            showDigOutcomeDialog = true
            pendingDigOutcome = null
        }

        // Info boxes (e.g. special towers) are dialogs too; their dismissal must also hand focus back to the map
        LaunchedEffect(
            showUnsavedChangesDialog,
            showEndTurnConfirmation,
            keyboardUndoOrSellConfirmation,
            gameState.infoState.value.currentInfo,
        ) {
            if (!showUnsavedChangesDialog && !showEndTurnConfirmation && keyboardUndoOrSellConfirmation == null) {
                mapRefocusTrigger++
            }
        }

        // Request focus on screen entry and after dialogs close (mapRefocusTrigger changes)
        LaunchedEffect(Unit, mapRefocusTrigger) {
            screenFocusRequester.requestFocus()
        }

        // When demo mode visual state changes, sync it into the local selection state so
        // the existing rendering code (GameGrid, GameControls) shows the preview without any changes.
        if (isDemoMode) {
            LaunchedEffect(demoSelectedDefenderType) { selectedDefenderType = demoSelectedDefenderType }
            LaunchedEffect(demoSelectedDefenderId) { selectedDefenderId = demoSelectedDefenderId }
            LaunchedEffect(demoSelectedTargetPosition) { selectedTargetPosition = demoSelectedTargetPosition }
        }
        // When demo mode ends, clear any leftover selection state so normal play starts clean
        LaunchedEffect(isDemoMode) {
            if (!isDemoMode) {
                selectedDefenderType = null
                selectedDefenderId = null
                selectedTargetPosition = null
            }
        }

        val audioCaptionEnemySpawn = stringResource(Res.string.audio_enemy_spawn_name)
        val audioCaptionEnemyDestroyed = stringResource(Res.string.audio_enemy_destroyed_name)
        val audioCaptionTrapTrigger = stringResource(Res.string.audio_trap_trigger_name)
        val audioCaptionLifeLost = stringResource(Res.string.audio_life_lost_name)
        val audioCaptionTowerUpgraded = stringResource(Res.string.audio_tower_upgraded_name)
        val audioCaptionBattleStart = stringResource(Res.string.audio_battle_start_name)
        val audioCaptionBombTicking = stringResource(Res.string.audio_bomb_ticking_name)
        val audioCaptionBombExploding = stringResource(Res.string.audio_bomb_exploding_name)

        LaunchedEffect(captionsEnabled) {
            if (!captionsEnabled) {
                soundCaptionText = null
                return@LaunchedEffect
            }
            GlobalSoundManager.soundEvents.collect { event ->
                val caption =
                    when (event) {
                        SoundEvent.ENEMY_SPAWN -> audioCaptionEnemySpawn
                        SoundEvent.ENEMY_DESTROYED -> audioCaptionEnemyDestroyed
                        SoundEvent.TRAP_TRIGGERED -> audioCaptionTrapTrigger
                        SoundEvent.LIFE_LOST -> audioCaptionLifeLost
                        SoundEvent.TOWER_UPGRADED -> audioCaptionTowerUpgraded
                        SoundEvent.BATTLE_START -> audioCaptionBattleStart
                        SoundEvent.BOMB_TICKING -> audioCaptionBombTicking
                        SoundEvent.BOMB_EXPLOSION -> audioCaptionBombExploding
                        else -> null
                    }
                if (caption != null) {
                    soundCaptionText = caption
                    soundCaptionSequence++
                }
            }
        }

        LaunchedEffect(soundCaptionSequence) {
            if (soundCaptionSequence <= 0L) return@LaunchedEffect
            val currentSequence = soundCaptionSequence
            kotlinx.coroutines.delay(SOUND_CAPTION_DISPLAY_DURATION_MS)
            if (soundCaptionSequence == currentSequence) {
                soundCaptionText = null
            }
        }

        // Watch for cheat dig outcomes
        LaunchedEffect(cheatDigOutcome) {
            if (cheatDigOutcome != null) {
                currentDigOutcome = cheatDigOutcome
                showDigOutcomeDialog = true
                onClearCheatDigOutcome?.invoke()
            }
        }

        // Background music management based on health points
        LaunchedEffect(gameState.healthPoints.value) {
            val currentMusic =
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .getInstance()
                    ?.getCurrentMusic()

            if (gameState.healthPoints.value < 5) {
                // Switch to low health music if not already playing
                if (currentMusic != de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH) {
                    de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                        de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH,
                        loop = true,
                    )
                }
            } else {
                // Play normal gameplay music if not already playing
                if (currentMusic != de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL) {
                    de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                        de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL,
                        loop = true,
                    )
                }
            }
        }

        // Stop background music when leaving gameplay
        DisposableEffect(Unit) {
            onDispose {
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .stopMusic()
            }
        }

        // Check for dragon-related infos and show appropriate tutorial
        LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
            val infoState = gameState.infoState.value

            // Skip if already showing an info
            if (infoState.currentInfo != InfoType.NONE) {
                return@LaunchedEffect
            }

            // Check for dragons and show appropriate info
            val dragons = gameState.attackers.filter { it.type.isDragon && !it.isDefeated.value }

            if (dragons.isNotEmpty()) {
                // Priority: Very greedy > Greed > Dragon info
                val veryGreedyDragon = dragons.any { it.greed > 5 }
                val greedyDragon = dragons.any { it.greed > 0 }

                when {
                    veryGreedyDragon && !infoState.hasSeen(InfoType.VERY_GREEDY_INFO) -> {
                        gameState.infoState.value = infoState.showInfo(InfoType.VERY_GREEDY_INFO)
                    }
                    greedyDragon && !infoState.hasSeen(InfoType.GREED_INFO) -> {
                        gameState.infoState.value = infoState.showInfo(InfoType.GREED_INFO)
                    }
                    !infoState.hasSeen(InfoType.DRAGON_INFO) -> {
                        gameState.infoState.value = infoState.showInfo(InfoType.DRAGON_INFO)
                    }
                }
            }
        }

        // Witch info popups
        LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
            val infoState = gameState.infoState.value

            // Skip if already showing an info
            if (infoState.currentInfo != InfoType.NONE) {
                return@LaunchedEffect
            }

            // Check for green witches on the field
            val greenWitches =
                gameState.attackers.filter {
                    it.type == AttackerType.GREEN_WITCH && !it.isDefeated.value
                }
            if (greenWitches.isNotEmpty() && !infoState.hasSeen(InfoType.GREEN_WITCH_INFO)) {
                gameState.infoState.value = infoState.showInfo(InfoType.GREEN_WITCH_INFO)
                return@LaunchedEffect
            }

            // Check for red witches on the field
            val redWitches =
                gameState.attackers.filter {
                    it.type == AttackerType.RED_WITCH && !it.isDefeated.value
                }
            if (redWitches.isNotEmpty() && !infoState.hasSeen(InfoType.RED_WITCH_INFO)) {
                gameState.infoState.value = infoState.showInfo(InfoType.RED_WITCH_INFO)
            }
        }

        // Check for mine warnings
        LaunchedEffect(gameState.mineWarnings.size, gameState.infoState.value) {
            val infoState = gameState.infoState.value

            // Skip if already showing an info
            if (infoState.currentInfo != InfoType.NONE) {
                return@LaunchedEffect
            }

            // Show mine warning if there are warnings and not currently showing one
            if (gameState.mineWarnings.isNotEmpty()) {
                val mineId = gameState.mineWarnings.first()
                gameState.infoState.value = infoState.showInfo(InfoType.MINE_WARNING, mineId)
            }
        }

        // Check for 1 HP warning at the start of the level
        LaunchedEffect(gameState.level.healthPoints) {
            val infoState = gameState.infoState.value

            // Skip if already showing an info or already seen
            if (infoState.currentInfo != InfoType.NONE || infoState.hasSeen(InfoType.ONE_HP_WARNING)) {
                return@LaunchedEffect
            }

            // Show warning if level starts with only 1 HP
            if (gameState.level.healthPoints == 1) {
                gameState.infoState.value = infoState.showInfo(InfoType.ONE_HP_WARNING)
            }
        }

        // Check for first-time special tower availability and show combined info dialog
        LaunchedEffect(gameState.level.availableTowers, gameState.infoState.value) {
            val infoState = gameState.infoState.value

            // Skip if already showing an info
            if (infoState.currentInfo != InfoType.NONE) {
                return@LaunchedEffect
            }

            // Check if level has special towers and we haven't shown the info yet
            val specialTowers =
                gameState.level.availableTowers.filter {
                    it in listOf(DefenderType.WIZARD_TOWER, DefenderType.ALCHEMY_TOWER, DefenderType.BALLISTA_TOWER, DefenderType.DWARVEN_MINE)
                }

            if (specialTowers.isNotEmpty() && !infoState.hasSeen(InfoType.SPECIAL_TOWERS_INFO)) {
                gameState.infoState.value = infoState.showInfo(InfoType.SPECIAL_TOWERS_INFO)
            }
        }

        // Check for river tiles and show river mechanics info (first time only)
        LaunchedEffect(gameState.level.id) {
            val infoState = gameState.infoState.value

            // Skip if already showing an info or already seen river info
            if (infoState.currentInfo != InfoType.NONE || infoState.hasSeen(InfoType.RIVER_INFO)) {
                return@LaunchedEffect
            }

            // Show info if level has river tiles
            if (gameState.level.riverTiles.isNotEmpty()) {
                gameState.infoState.value = infoState.showInfo(InfoType.RIVER_INFO)
            }
        }
    }
}
