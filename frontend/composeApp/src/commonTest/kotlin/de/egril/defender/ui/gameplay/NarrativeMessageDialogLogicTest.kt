package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color
import de.egril.defender.model.AttackerType
import defender_of_egril.composeapp.generated.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NarrativeMessageDialogLogicTest {
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
    fun darkVillainsUseReadableLightNarrativeTextColors() {
        val silasColors = narrativeTextColors(AttackerType.SILAS_THE_MASKMASTER)
        val malakorColors = narrativeTextColors(AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE)
        val morvathColors = narrativeTextColors(AttackerType.MORVATH_THE_SHADOWMASTER)
        val defaultColors = narrativeTextColors(AttackerType.GAROKK)

        assertEquals(Color(0xFFF7F1E8), silasColors.title)
        assertEquals(Color(0xFFE9DFD2), silasColors.body)
        assertEquals(Color(0xFFF7F1E8), malakorColors.title)
        assertEquals(Color(0xFFE9DFD2), malakorColors.body)
        assertEquals(Color(0xFFF7F1E8), morvathColors.title)
        assertEquals(Color(0xFFE9DFD2), morvathColors.body)
        assertTrue(silasColors.title != defaultColors.title)
        assertTrue(silasColors.body != defaultColors.body)
        assertTrue(malakorColors.title != defaultColors.title)
        assertTrue(malakorColors.body != defaultColors.body)
        assertTrue(morvathColors.title != defaultColors.title)
        assertTrue(morvathColors.body != defaultColors.body)
    }

    @Test
    fun everyVillainHasOwnStoryMessageBackground() {
        val missingBackgrounds =
            AttackerType.entries
                .filter { it.isVillain }
                .mapNotNull { it.villainName }
                .toSet()
                .map { "message_background_${it.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}" }
                .filterNot { Res.allDrawableResources.containsKey(it) }

        assertTrue(
            missingBackgrounds.isEmpty(),
            "Missing villain story message backgrounds: ${missingBackgrounds.joinToString()}",
        )
    }
}
