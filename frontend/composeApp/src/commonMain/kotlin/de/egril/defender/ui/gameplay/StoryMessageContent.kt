package de.egril.defender.ui.gameplay

import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.StoryLogEntry
import defender_of_egril.composeapp.generated.resources.*

/**
 * Title and text of a story message, shared by the message dialog and the story logbook.
 * Returns null for messages without narrative text (e.g. target captured, gate destroyed).
 */
@Composable
fun storyMessageTitleAndText(msg: GameMessage): Pair<String, String>? =
    when (msg.type) {
        GameMessageType.EWHAD_ENTERS ->
            stringResource(Res.string.ewhad_enters_title) to stringResource(Res.string.ewhad_enters_text)
        GameMessageType.EWHAD_RETREATS ->
            stringResource(Res.string.ewhad_retreats_title) to stringResource(Res.string.ewhad_retreats_text)
        GameMessageType.EWHAD_DEFEATED ->
            stringResource(Res.string.ewhad_defeated_title) to stringResource(Res.string.ewhad_defeated_text)
        GameMessageType.VILLAIN_ENTERS -> villainEnterContent(msg.name)
        GameMessageType.VILLAIN_DEFEATED -> {
            val villainName = attackerTypeFromMessageName(msg.name)?.villainName ?: stringResource(Res.string.villain)
            stringResource(Res.string.villain_defeated_title) to
                stringResource(Res.string.villain_defeated_text, villainName)
        }
        GameMessageType.SILAS_MIRROR_HIT ->
            stringResource(Res.string.villain_silas_mirror_hit_title) to
                stringResource(Res.string.villain_silas_mirror_hit_text)
        GameMessageType.COVEN_SWAP ->
            stringResource(Res.string.villain_coven_swap_title) to stringResource(Res.string.villain_coven_swap_text)
        GameMessageType.WAAAGH_FRENZY ->
            stringResource(Res.string.waaagh_frenzy_title) to stringResource(Res.string.waaagh_frenzy_message)
        GameMessageType.STORY_INTRO -> msg.name?.let { storyIntroContent(it) }?.let { it.second to it.third }
        GameMessageType.EVENT_MESSAGE -> {
            val messageKey = msg.name
            if (messageKey == null) {
                null
            } else {
                val text =
                    if (messageKey == "event_msg_rune_network_taken_over") {
                        com.hyperether.resources.LocalizedStrings.getFormatted(
                            messageKey,
                            msg.eventMessageAmount ?: 0,
                            locale = com.hyperether.resources.currentLanguage.value,
                        )
                    } else {
                        com.hyperether.resources.LocalizedStrings.get(
                            messageKey,
                            com.hyperether.resources.currentLanguage.value,
                        )
                    }
                stringResource(Res.string.event_message_title) to text
            }
        }
        else -> null
    }

/**
 * True when the story logbook has at least one entry the dialog can display. Log entries
 * without narrative text (e.g. levels without a predefined story) must not offer the logbook.
 */
@Composable
fun storyLogHasDisplayableEntries(storyLog: List<StoryLogEntry>): Boolean = storyLog.any { storyMessageTitleAndText(it.message) != null }

/** Title and text for a villain's arrival, falling back to a generic arrival text. */
@Composable
fun villainEnterContent(name: String?): Pair<String, String> =
    when (name) {
        AttackerType.GAROKK.name ->
            stringResource(Res.string.villain_garokk_title) to
                (stringResource(Res.string.villain_garokk_backstory) + "\n" + stringResource(Res.string.villain_garokk_description))
        AttackerType.SNOTLING_BOSS.name ->
            stringResource(Res.string.villain_gribnak_title) to
                (stringResource(Res.string.villain_gribnak_backstory) + "\n" + stringResource(Res.string.villain_gribnak_description))
        AttackerType.MORGUK_BONEWHISPER.name ->
            stringResource(Res.string.villain_morguk_title) to
                (stringResource(Res.string.villain_morguk_backstory) + "\n" + stringResource(Res.string.villain_morguk_description))
        AttackerType.ARAXXA.name ->
            stringResource(Res.string.villain_araxxa_title) to
                (stringResource(Res.string.villain_araxxa_backstory) + "\n" + stringResource(Res.string.villain_araxxa_description))
        AttackerType.BARON_RATTERZAHN.name ->
            stringResource(Res.string.villain_ratterzahn_title) to
                (stringResource(Res.string.villain_ratterzahn_backstory) + "\n" + stringResource(Res.string.villain_ratterzahn_description))
        AttackerType.FALLEN_SHIELDMAIDEN_FREYA.name ->
            stringResource(Res.string.villain_freya_title) to
                (stringResource(Res.string.villain_freya_backstory) + "\n" + stringResource(Res.string.villain_freya_description))
        AttackerType.OBSIDIAN_PROTECTOR.name ->
            stringResource(Res.string.villain_obsidian_protector_title) to
                (stringResource(Res.string.villain_obsidian_protector_backstory) + "\n" + stringResource(Res.string.villain_obsidian_protector_description))
        AttackerType.PRINCE_VALERIUS_THE_SOULREAPER.name ->
            stringResource(Res.string.villain_valerius_title) to
                (stringResource(Res.string.villain_valerius_backstory) + "\n" + stringResource(Res.string.villain_valerius_description))
        AttackerType.SILAS_THE_MASKMASTER.name ->
            stringResource(Res.string.villain_silas_title) to
                (stringResource(Res.string.villain_silas_backstory) + "\n" + stringResource(Res.string.villain_silas_description))
        AttackerType.GRAND_COVEN_MOTHER_SYBILLA.name ->
            stringResource(Res.string.villain_sybilla_title) to
                (stringResource(Res.string.villain_sybilla_backstory) + "\n" + stringResource(Res.string.villain_sybilla_description))
        AttackerType.SYLVANAS_THE_MOLDING.name ->
            stringResource(Res.string.villain_sylvanas_title) to
                (stringResource(Res.string.villain_sylvanas_backstory) + "\n" + stringResource(Res.string.villain_sylvanas_description))
        AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE.name ->
            stringResource(Res.string.villain_malakor_title) to
                (stringResource(Res.string.villain_malakor_backstory) + "\n" + stringResource(Res.string.villain_malakor_description))
        AttackerType.IGNIS_VA_THE_DRAGONVOICE.name ->
            stringResource(Res.string.villain_ignis_va_title) to
                (stringResource(Res.string.villain_ignis_va_backstory) + "\n" + stringResource(Res.string.villain_ignis_va_description))
        AttackerType.MORVATH_THE_SHADOWMASTER.name ->
            stringResource(Res.string.villain_morvath_title) to
                (stringResource(Res.string.villain_morvath_backstory) + "\n" + stringResource(Res.string.villain_morvath_description))
        AttackerType.XARITHON_THE_SHADOW_DRAGON.name ->
            stringResource(Res.string.villain_xarithon_title) to
                (stringResource(Res.string.villain_xarithon_backstory) + "\n" + stringResource(Res.string.villain_xarithon_description))
        AttackerType.CAPTAIN_RODERICH.name ->
            stringResource(Res.string.villain_roderich_title) to
                (stringResource(Res.string.villain_roderich_backstory) + "\n" + stringResource(Res.string.villain_roderich_description))
        AttackerType.THE_KRAKEN.name ->
            stringResource(Res.string.villain_kraken_title) to
                (stringResource(Res.string.villain_kraken_backstory) + "\n" + stringResource(Res.string.villain_kraken_description))
        AttackerType.ZYTHAR_THE_RIFTCALLER.name ->
            stringResource(Res.string.villain_zythar_title) to
                (stringResource(Res.string.villain_zythar_backstory) + "\n" + stringResource(Res.string.villain_zythar_description))
        AttackerType.GRAND_RUNEMASTER_VAELEN.name ->
            stringResource(Res.string.villain_vaelen_title) to
                (stringResource(Res.string.villain_vaelen_backstory) + "\n" + stringResource(Res.string.villain_vaelen_description))
        else ->
            stringResource(Res.string.villain_enters_title) to stringResource(Res.string.villain_enters_text)
    }

/**
 * Narrative type, title and text for a level's start story, keyed by the level editor ID.
 * Returns null when the level has no predefined story (e.g. user-created levels).
 */
@Composable
fun storyIntroContent(levelEditorId: String): Triple<NarrativeMessageType, String, String>? {
    val fortressTitle = stringResource(Res.string.level_the_fortress_title)
    return when (levelEditorId) {
        "the_first_wave" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_first_wave_title),
                stringResource(Res.string.story_the_first_wave),
            )
        "mixed_forces" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_mixed_forces_title),
                stringResource(Res.string.story_mixed_forces),
            )
        "the_ork_invasion" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_ork_invasion_title),
                stringResource(Res.string.story_the_ork_invasion),
            )
        "dark_magic_rises" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_dark_magic_rises_title),
                stringResource(Res.string.story_dark_magic_rises),
            )
        "prison_break" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_prison_break_title),
                stringResource(Res.string.story_prison_break),
            )
        "defend_the_city" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_defend_the_city_title),
                stringResource(Res.string.story_defend_the_city),
            )
        "the_plains" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_plains_title),
                stringResource(Res.string.story_the_plains),
            )
        "the_spiral_challenge" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_spiral_challenge_title),
                stringResource(Res.string.story_the_spiral_challenge),
            )
        "the_dance" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_dance_title),
                stringResource(Res.string.story_the_dance),
            )
        "the_rush" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_rush_title),
                stringResource(Res.string.story_the_rush),
            )
        "the_woods_first_incursion" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_woods_first_incursion_title),
                stringResource(Res.string.story_the_woods_first_incursion),
            )
        "the_woods_full_assault" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_woods_full_assault_title),
                stringResource(Res.string.story_the_woods_full_assault),
            )
        "the_fast_and_furious" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_fast_and_furious_title),
                stringResource(Res.string.story_the_fast_and_furious),
            )
        "the_cross" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_cross_title),
                stringResource(Res.string.story_the_cross),
            )
        "the_winding_path" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_winding_path_title),
                stringResource(Res.string.story_the_winding_path),
            )
        "the_fortress_1_first_attacks" ->
            Triple(
                NarrativeMessageType.STORY,
                "$fortressTitle - ${stringResource(Res.string.level_the_fortress_1_first_attacks_subtitle)}",
                stringResource(Res.string.story_the_fortress_1_first_attacks),
            )
        "the_fortress_2_orks_marching" ->
            Triple(
                NarrativeMessageType.STORY,
                "$fortressTitle - ${stringResource(Res.string.level_the_fortress_2_orks_marching_subtitle)}",
                stringResource(Res.string.story_the_fortress_2_orks_marching),
            )
        "the_fortress_3_necromancer" ->
            Triple(
                NarrativeMessageType.STORY,
                "$fortressTitle - ${stringResource(Res.string.level_the_fortress_3_necromancer_subtitle)}",
                stringResource(Res.string.story_the_fortress_3_necromancer),
            )
        "the_fortress_4_magic_assault" ->
            Triple(
                NarrativeMessageType.STORY,
                "$fortressTitle - ${stringResource(Res.string.level_the_fortress_4_magic_assault_subtitle)}",
                stringResource(Res.string.story_the_fortress_4_magic_assault),
            )
        "the_fortress_5_mighty_forces" ->
            Triple(
                NarrativeMessageType.STORY,
                "$fortressTitle - ${stringResource(Res.string.level_the_fortress_5_mighty_forces_subtitle)}",
                stringResource(Res.string.story_the_fortress_5_mighty_forces),
            )
        "the_final_stand" ->
            Triple(
                NarrativeMessageType.EWHAD,
                stringResource(Res.string.level_the_final_stand_title),
                stringResource(Res.string.story_the_final_stand),
            )
        "the_creek" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_creek_title),
                stringResource(Res.string.story_the_creek),
            )
        "the_island" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_island_title),
                stringResource(Res.string.story_the_island),
            )
        "the_river" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_river_title),
                stringResource(Res.string.story_the_river),
            )
        "creek_valley" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_creek_valley_title),
                stringResource(Res.string.story_creek_valley),
            )
        "maelstrom" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_maelstrom_title),
                stringResource(Res.string.story_maelstrom),
            )
        "the_tower_of_the_hermit" ->
            Triple(
                NarrativeMessageType.STORY,
                stringResource(Res.string.level_the_tower_of_the_hermit_title),
                stringResource(Res.string.story_the_tower_of_the_hermit),
            )
        else -> null
    }
}
