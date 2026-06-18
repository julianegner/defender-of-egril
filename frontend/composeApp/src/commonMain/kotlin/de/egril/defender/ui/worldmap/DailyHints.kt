@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.worldmap

import de.egril.defender.iam.IamState
import de.egril.defender.utils.formatTimestampISO
import de.egril.defender.utils.currentTimeMillis
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.daily_hint_account_message
import defender_of_egril.composeapp.generated.resources.daily_hint_shortcuts_message
import defender_of_egril.composeapp.generated.resources.daily_hint_xp_spells_message
import defender_of_egril.composeapp.generated.resources.daily_hint_xp_ability_construction_message
import defender_of_egril.composeapp.generated.resources.daily_hint_xp_other_abilities_message
import defender_of_egril.composeapp.generated.resources.daily_hint_tower_range_message
import defender_of_egril.composeapp.generated.resources.daily_hint_tower_actions_message
import defender_of_egril.composeapp.generated.resources.daily_hint_tower_damage_message
import defender_of_egril.composeapp.generated.resources.daily_hint_barricade_message
import defender_of_egril.composeapp.generated.resources.daily_hint_barricade_details_message
import defender_of_egril.composeapp.generated.resources.daily_hint_magical_trap_message
import defender_of_egril.composeapp.generated.resources.daily_hint_mine_message
import defender_of_egril.composeapp.generated.resources.daily_hint_mine_dragon_message
import defender_of_egril.composeapp.generated.resources.daily_hint_barge_message
import defender_of_egril.composeapp.generated.resources.daily_hint_placement_message
import defender_of_egril.composeapp.generated.resources.daily_hint_combine_towers_message
import defender_of_egril.composeapp.generated.resources.daily_hint_immunities_message
import defender_of_egril.composeapp.generated.resources.daily_hint_aoe_message
import defender_of_egril.composeapp.generated.resources.daily_hint_min_range_message
import defender_of_egril.composeapp.generated.resources.daily_hint_save_load_message
import org.jetbrains.compose.resources.StringResource

/**
 * A daily hint shown on the world map.
 *
 * Hints rotate one per calendar day; some hints are conditional (for example the
 * "create an account" hint is hidden once the user is logged in).
 *
 * @param id stable identifier (used for tests, never localized).
 * @param messageRes localized hint body text.
 * @param shouldShow predicate evaluated against the current [IamState]; when it
 *  returns false the hint is skipped during selection.
 */
data class DailyHint(
    val id: String,
    val messageRes: StringResource,
    val shouldShow: (IamState) -> Boolean = { true }
)

/**
 * Catalog of daily hints shown on the world map. Hints are presented in this order
 * (skipping any whose [DailyHint.shouldShow] predicate returns false) so users see
 * a fresh tip each day.
 */
val DAILY_HINTS: List<DailyHint> = listOf(
    // Account / cross-platform sync. Hidden once the player has a Keycloak account.
    DailyHint(
        id = "account",
        messageRes = Res.string.daily_hint_account_message,
        shouldShow = { iamState -> !iamState.isAuthenticated }
    ),
    // Cross-platform save/load — relevant once the player has an account.
    DailyHint(
        id = "save_load",
        messageRes = Res.string.daily_hint_save_load_message
    ),
    // Keyboard shortcuts.
    DailyHint(id = "shortcuts", messageRes = Res.string.daily_hint_shortcuts_message),
    // XP / ability points.
    DailyHint(id = "xp_spells", messageRes = Res.string.daily_hint_xp_spells_message),
    DailyHint(id = "xp_ability_construction", messageRes = Res.string.daily_hint_xp_ability_construction_message),
    DailyHint(id = "xp_other_abilities", messageRes = Res.string.daily_hint_xp_other_abilities_message),
    // Tower upgrades.
    DailyHint(id = "tower_range", messageRes = Res.string.daily_hint_tower_range_message),
    DailyHint(id = "tower_actions", messageRes = Res.string.daily_hint_tower_actions_message),
    DailyHint(id = "tower_damage", messageRes = Res.string.daily_hint_tower_damage_message),
    // High-level abilities.
    DailyHint(id = "barricade", messageRes = Res.string.daily_hint_barricade_message),
    DailyHint(id = "barricade_details", messageRes = Res.string.daily_hint_barricade_details_message),
    DailyHint(id = "magical_trap", messageRes = Res.string.daily_hint_magical_trap_message),
    // Mine.
    DailyHint(id = "mine", messageRes = Res.string.daily_hint_mine_message),
    DailyHint(id = "mine_dragon", messageRes = Res.string.daily_hint_mine_dragon_message),
    // Barge / rivers.
    DailyHint(id = "barge", messageRes = Res.string.daily_hint_barge_message),
    // Tower placement / usage tips.
    DailyHint(id = "placement", messageRes = Res.string.daily_hint_placement_message),
    DailyHint(id = "combine_towers", messageRes = Res.string.daily_hint_combine_towers_message),
    DailyHint(id = "aoe", messageRes = Res.string.daily_hint_aoe_message),
    DailyHint(id = "min_range", messageRes = Res.string.daily_hint_min_range_message),
    DailyHint(id = "immunities", messageRes = Res.string.daily_hint_immunities_message),
)

/**
 * Result of a daily hint selection.
 *
 * @param hint the hint to display.
 * @param index its position in [DAILY_HINTS] (used to persist rotation state).
 */
data class SelectedDailyHint(val hint: DailyHint, val index: Int)

/** Returns today's calendar date as `YYYY-MM-DD` in the local time zone. */
fun todayLocalDateString(): String =
    formatTimestampISO(currentTimeMillis()).take(10)

/**
 * Select the next daily hint to show, given the previous index and current IAM state.
 *
 * Iterates through [DAILY_HINTS] starting from `lastIndex + 1`, returning the first
 * hint whose [DailyHint.shouldShow] predicate is true for [iamState]. Returns null
 * when no hint is eligible (which can happen if the catalog is empty).
 */
fun selectDailyHint(lastIndex: Int, iamState: IamState): SelectedDailyHint? {
    if (DAILY_HINTS.isEmpty()) return null
    val size = DAILY_HINTS.size
    for (offset in 1..size) {
        val candidateIndex = ((lastIndex + offset) % size + size) % size
        val candidate = DAILY_HINTS[candidateIndex]
        if (candidate.shouldShow(iamState)) {
            return SelectedDailyHint(candidate, candidateIndex)
        }
    }
    return null
}
