package de.egril.defender.ui.gameplay

import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.DrawableResource

/**
 * Symbols that can represent a scripted event in the upcoming enemies list.
 *
 * Every drawable whose name starts with [PREFIX] is offered, so new event artwork is available to
 * level authors without a code change. The stored symbol is the drawable id itself (e.g. `event_tide`).
 */
object EventUpcomingSymbols {
    const val PREFIX = "event_"

    /** All selectable symbol ids in alphabetical order. */
    val symbolIds: List<String> by lazy {
        Res.allDrawableResources.keys
            .filter { it.startsWith(PREFIX) }
            .sorted()
    }

    /** Artwork of [symbolId], or null when the id is empty or no such drawable exists. */
    fun resource(symbolId: String?): DrawableResource? {
        if (symbolId.isNullOrBlank() || !symbolId.startsWith(PREFIX)) return null
        return Res.allDrawableResources[symbolId]
    }
}
