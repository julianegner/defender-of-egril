package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import defender_of_egril.composeapp.generated.resources.*

/**
 * Helper function to determine if target selection should be preserved after an attack.
 * Selection is kept active if:
 * - The tower still has action points remaining after the attack
 * - The target enemy is still alive (not defeated)
 *
 * This allows players to continue attacking the same target with multi-action towers.
 */
internal fun shouldKeepTargetSelection(
    gameState: GameState,
    defenderId: Int,
    targetId: Int,
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    val target = gameState.attackers.find { it.id == targetId } ?: return false

    return defender.actionsRemaining.value > 0 && !target.isDefeated.value
}

/**
 * Helper function to determine if target selection should be preserved after a position-based attack.
 * Same logic as shouldKeepTargetSelection but uses position instead of ID to find the target.
 */
internal fun shouldKeepTargetSelectionForPosition(
    gameState: GameState,
    defenderId: Int,
    targetPosition: Position,
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    val target = gameState.attackers.find { it.position.value == targetPosition } ?: return false

    return defender.actionsRemaining.value > 0 && !target.isDefeated.value
}

/**
 * Helper function to determine if a placement mode should be preserved after placing a trap or barricade.
 * Placement mode is kept active if the tower still has action points remaining.
 *
 * This allows players to place multiple traps or barricades with towers that have multiple actions per turn.
 */
internal fun shouldKeepPlacementMode(
    gameState: GameState,
    defenderId: Int,
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    return defender.actionsRemaining.value > 0
}

/**
 * Compact keyboard-hint row shown on the placement / spell-targeting instruction cards. It documents
 * the keys used to move the on-map placement cursor and to confirm the placement, so keyboard users
 * can discover them. Renders nothing when the shortcut-hint setting is disabled (each chip self-guards).
 */
@Composable
internal fun PlacementKeyboardHints(modifier: Modifier = Modifier) {
    if (!AppSettings.showButtonShortcutHints.value) return
    val hintColor = LocalContentColor.current.copy(alpha = 0.75f)
    val nextBinding = formatShortcutBindingForDisplay(AppSettings.shortcutNextEnemyTarget.value)
    val prevBinding = formatShortcutBindingForDisplay(AppSettings.shortcutPrevEnemyTarget.value)
    val rowUpBinding = formatShortcutBindingForDisplay(AppSettings.shortcutPanUp.value)
    val rowDownBinding = formatShortcutBindingForDisplay(AppSettings.shortcutPanDown.value)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ShortcutKeyChip(text = nextBinding, color = hintColor)
        ShortcutKeyChip(text = prevBinding, color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_move),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShortcutKeyChip(text = rowUpBinding, color = hintColor)
        ShortcutKeyChip(text = rowDownBinding, color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_row),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShortcutKeyChip(text = "Enter", color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_place),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
    }
}

/**
 * Compact keyboard-hint row shown beneath the support bar. It documents how to navigate between the
 * support elements (placeable objects, spell tokens, cooldown powers) with the keyboard: left/right
 * to move the focus cursor and Enter to activate the focused element. Renders nothing when the
 * shortcut-hint setting is disabled (each chip self-guards).
 */
@Composable
internal fun SupportBarKeyboardHints(modifier: Modifier = Modifier) {
    if (!AppSettings.showButtonShortcutHints.value) return
    val hintColor = LocalContentColor.current.copy(alpha = 0.75f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // \u2190\u2192 = left/right arrows; ShortcutKeyChip renders them as Material Symbol icons.
        ShortcutKeyChip(text = "\u2190\u2192", color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_placement_move),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShortcutKeyChip(text = "Enter", color = hintColor)
        Text(
            text = stringResource(Res.string.keyboard_nav_select),
            style = MaterialTheme.typography.labelSmall,
            color = hintColor,
        )
    }
}

/**
 * Returns a villain's message frame from drawables named `message_background_<abbreviated_villain_name>`.
 *
 * The lookup is automatic and uses [AttackerType.villainName]. Example:
 * `message_background_garokk.png` is used for "Garokk".
 */
internal fun villainMessageBackground(name: String?): org.jetbrains.compose.resources.DrawableResource? {
    val attackerType = attackerTypeFromMessageName(name) ?: return null
    if (!attackerType.isVillain) return null
    val villainShortName = attackerType.villainName ?: return null
    val normalizedVillainName = villainShortName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    if (normalizedVillainName.isEmpty()) return null
    return Res.allDrawableResources["message_background_$normalizedVillainName"]
}

/**
 * Returns a villain-specific button color for narrative message dialogs.
 *
 * Each villain gets a thematic button color based on their visual design and personality.
 * For non-villains or unrecognized types, returns null (uses default color).
 *
 * The color should be distinct, fitting to the villain's character, and maintain good
 * contrast with white text on the button.
 */
internal fun villainMessageButtonColor(name: String?): Color? {
    val attackerType = attackerTypeFromMessageName(name) ?: return null
    if (!attackerType.isVillain) return null

    return when (attackerType) {
        // Horde warchief – aggressive red
        AttackerType.GAROKK -> Color(0xFFE31C1C)
        // Goblin summoner – olive green
        AttackerType.SNOTLING_BOSS -> Color(0xFF6B8E23)
        // Shaman summoner – dark indigo
        AttackerType.MORGUK_BONEWHISPER -> Color(0xFF4B0082)
        // Giant spider – earthy brown
        AttackerType.ARAXXA -> Color(0xFF2F1B0C)
        // Rat warlord – dark steel gray
        AttackerType.BARON_RATTERZAHN -> Color(0xFF4B4F56)
        // Illusionist – deep purple magic
        AttackerType.SILAS_THE_MASKMASTER -> Color(0xFF5D3A8C)
        // Mirror images use the same color as Silas
        AttackerType.SILAS_MIRROR_IMAGE -> Color(0xFF5D3A8C)
        // Undead shieldmaiden – dark armor blue
        AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> Color(0xFF2A2F3A)
        AttackerType.OBSIDIAN_PROTECTOR -> Color(0xFF30144D)
        // Undead prince – soul-reaper purple
        AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> Color(0xFF28304D)
        // Witch coven leader – magical purple
        AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> Color(0xFF5B2C6F)
        // Green witch – healing green
        AttackerType.HAGA -> Color(0xFF006400)
        // Red witch – disabling crimson
        AttackerType.ZUSSA -> Color(0xFF6B0000)
        // Corrupted nature – dark emerald
        AttackerType.SYLVANAS_THE_MOLDING -> Color(0xFF1A3A20)
        // Archmage forbidden astral magic – deep void blue
        AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> Color(0xFF0D1B3E)
        // Dragon cultist – deep dragon-fire crimson
        AttackerType.IGNIS_VA_THE_DRAGONVOICE -> Color(0xFF8B1A00)
        // Shadowmaster – black-violet shadow mist
        AttackerType.MORVATH_THE_SHADOWMASTER -> Color(0xFF2A003A)
        // Shadow dragon – void purple-black
        AttackerType.XARITHON_THE_SHADOW_DRAGON -> Color(0xFF1E0040)
        // Pirate captain – deep ocean blue
        AttackerType.CAPTAIN_RODERICH -> Color(0xFF0D4E74)
        // Ancient deep-sea horror – dark abyss teal
        AttackerType.THE_KRAKEN -> Color(0xFF0A3D4A)
        // Riftcaller dark sorcerer – deep void indigo
        AttackerType.ZYTHAR_THE_RIFTCALLER -> Color(0xFF0A001E)
        // Rocketeer's launch bay: dark steel with a hot exhaust tint
        AttackerType.RAZZIK_THE_ROCKETEER -> Color(0xFF1C1A22)
        AttackerType.GRAND_RUNEMASTER_VAELEN -> Color(0xFF74152B)
        // Default for EWHAD and other villains (if any added in future)
        else -> null
    }
}

/**
 * Safely converts a queued game-message name to [AttackerType].
 * Returns null when the name is null or does not map to a valid enum constant.
 *
 * Villain narrative messages carry the attacker type as a string in [GameMessage.name],
 * so this helper prevents crashes from invalid payloads and keeps message rendering resilient.
 */
internal fun attackerTypeFromMessageName(name: String?): AttackerType? = name?.let { runCatching { AttackerType.valueOf(it) }.getOrNull() }
