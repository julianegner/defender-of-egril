package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventMessageFrameId
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.DrawableResource

/**
 * Registry of the message frames that can be used for a scripted-event message popup.
 *
 * Every drawable named `message_background_<id>` is offered, so the story frame, all villain frames
 * and any frame artwork added later are available to level authors without a code change. Rendering
 * reuses the configuration of the existing story and villain message popups: the standard frame uses
 * the wide story layout, all other frames use the square villain layout with the text placement,
 * text colours and button colour already defined for the villain that frame belongs to.
 */
object EventMessageFrames {
    private const val PREFIX = "message_background_"

    /** Frame id of the classic wooden parchment story frame, used when an event defines no frame. */
    const val STANDARD_FRAME_ID: String = "story"

    /**
     * All selectable frame ids in display order, starting with the standard story frame.
     * Derived from the available drawables so new frame artwork shows up automatically.
     */
    val frameIds: List<EventMessageFrameId> by lazy {
        val ids =
            Res.allDrawableResources.keys
                .filter { it.startsWith(PREFIX) }
                .map { it.removePrefix(PREFIX) }
                .filter { it.isNotEmpty() }
        listOf(STANDARD_FRAME_ID) + ids.filterNot { it == STANDARD_FRAME_ID }.sorted()
    }

    /** True when [frameId] is null, the standard frame, or unknown artwork (falls back to standard). */
    fun isStandard(frameId: EventMessageFrameId?): Boolean = resolve(frameId) == STANDARD_FRAME_ID

    /**
     * Background artwork of [frameId], or null for the standard frame so [NarrativeMessageDialog]
     * uses its own story background (which is rendered with sliced side rails).
     */
    fun background(frameId: EventMessageFrameId?): DrawableResource? {
        val resolved = resolve(frameId)
        if (resolved == STANDARD_FRAME_ID) return null
        return Res.allDrawableResources[PREFIX + resolved]
    }

    /**
     * Dialog style of [frameId]: the standard frame keeps the wide story layout, all other frames
     * reuse the square villain/Ewhad layout their artwork was made for.
     */
    fun narrativeType(frameId: EventMessageFrameId?): NarrativeMessageType = if (isStandard(frameId)) NarrativeMessageType.STORY else NarrativeMessageType.EWHAD

    /**
     * The attacker whose message frame [frameId] belongs to. Used to reuse that villain's text
     * placement and text colours; the villain itself is not shown (see `showTopIcon` of
     * [NarrativeMessageDialog]). Null for frames without a villain (story, Waaagh!, …).
     */
    fun styleAttackerType(frameId: EventMessageFrameId?): AttackerType? {
        val resolved = resolve(frameId)
        if (resolved == STANDARD_FRAME_ID) return null
        return AttackerType.entries.firstOrNull { normalizedVillainFrameId(it) == resolved }
    }

    /** Button accent colour of [frameId], reusing the villain colours of the message frames. */
    fun accentColor(frameId: EventMessageFrameId?): Color? = villainMessageButtonColor(styleAttackerType(frameId)?.name)

    /**
     * Display label of [frameId] for the level editor: the villain's name when the frame belongs to
     * one, otherwise the readable form of the drawable id (e.g. `obsidian_protector` -> "Obsidian
     * Protector").
     */
    fun label(frameId: EventMessageFrameId?): String {
        val resolved = resolve(frameId)
        styleAttackerType(resolved)?.villainName?.let { return it }
        return resolved
            .split('_', '-')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
    }

    /** [frameId] when usable artwork exists for it, otherwise the standard frame. */
    private fun resolve(frameId: EventMessageFrameId?): EventMessageFrameId {
        if (frameId.isNullOrBlank()) return STANDARD_FRAME_ID
        return if (Res.allDrawableResources.containsKey(PREFIX + frameId)) frameId else STANDARD_FRAME_ID
    }

    /** The frame id derived from a villain's name, matching the `message_background_*` file names. */
    private fun normalizedVillainFrameId(attackerType: AttackerType): String? =
        attackerType.villainName
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]+"), "_")
            ?.trim('_')
            ?.takeIf { it.isNotEmpty() }
}
