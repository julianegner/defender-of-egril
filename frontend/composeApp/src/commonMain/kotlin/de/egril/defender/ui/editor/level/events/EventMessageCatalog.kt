package de.egril.defender.ui.editor.level.events

import com.hyperether.resources.AppLocale
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage

/**
 * Curated list of predefined story-message string-resource keys that can be attached to a scripted
 * level event. The level editor presents these keys in a dropdown; at runtime the corresponding
 * localized text is resolved via [LocalizedStrings.get] and shown in a narrative message dialog.
 *
 * All keys listed here must exist in every `strings.xml` translation file.
 */
object EventMessageCatalog {
    /** All selectable predefined event-message keys, in display order. */
    val keys: List<String> =
        listOf(
            "event_msg_reinforcements",
            "event_msg_coins_received",
            "event_msg_mana_received",
            "event_msg_low_health_warning",
            "event_msg_enemies_approaching",
            "event_msg_hold_the_line",
            "event_msg_mine_destroyed",
        )

    /**
     * Localized preview text for the given message [key], used to show the editor a readable label.
     */
    fun preview(
        key: String,
        locale: AppLocale = currentLanguage.value,
    ): String = LocalizedStrings.get(key, locale)
}
