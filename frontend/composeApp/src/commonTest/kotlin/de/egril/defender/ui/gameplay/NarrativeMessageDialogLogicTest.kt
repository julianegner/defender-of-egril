package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color
import de.egril.defender.model.AttackerType
import defender_of_egril.composeapp.generated.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NarrativeMessageDialogLogicTest {
    private val allowedMissingVillainBackgrounds =
        setOf(
            // Planned: dedicated Morvath frame image will be provided separately.
            "message_background_morvath",
        )

    @Test
    fun gribnakAndMorgukUseTighterEwhadFramePadding() {
        val defaultPadding = narrativeTextFramePaddingFractions(NarrativeMessageType.EWHAD, AttackerType.GAROKK)
        val gribnakPadding = narrativeTextFramePaddingFractions(NarrativeMessageType.EWHAD, AttackerType.SNOTLING_BOSS)
        val morgukPadding = narrativeTextFramePaddingFractions(NarrativeMessageType.EWHAD, AttackerType.MORGUK_BONEWHISPER)
        val araxxaPadding = narrativeTextFramePaddingFractions(NarrativeMessageType.EWHAD, AttackerType.ARAXXA)

        assertTrue(gribnakPadding.top > defaultPadding.top)
        assertTrue(gribnakPadding.bottom > defaultPadding.bottom)
        assertEquals(gribnakPadding.bottom, morgukPadding.bottom)
        assertTrue(gribnakPadding.top > morgukPadding.top)
        assertTrue(araxxaPadding.top > gribnakPadding.top)
        assertEquals(defaultPadding.bottom, araxxaPadding.bottom)
    }

    @Test
    fun silasUsesReadableLightNarrativeTextColors() {
        val silasColors = narrativeTextColors(AttackerType.SILAS_THE_MASKMASTER)
        val defaultColors = narrativeTextColors(AttackerType.GAROKK)

        assertEquals(Color(0xFFF7F1E8), silasColors.title)
        assertEquals(Color(0xFFE9DFD2), silasColors.body)
        assertTrue(silasColors.title != defaultColors.title)
        assertTrue(silasColors.body != defaultColors.body)
    }

    @Test
    fun everyVillainHasOwnStoryMessageBackground() {
        val missingBackgrounds =
            AttackerType.entries
                .filter { it.isVillain }
                .mapNotNull { it.villainName }
                .toSet()
                .map { "message_background_${it.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}" }
                .filterNot { it in allowedMissingVillainBackgrounds }
                .filterNot { Res.allDrawableResources.containsKey(it) }

        assertTrue(
            missingBackgrounds.isEmpty(),
            "Missing villain story message backgrounds: ${missingBackgrounds.joinToString()}",
        )
    }
}
