package de.egril.defender.ui.gameplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.LevelSupports
import defender_of_egril.composeapp.generated.resources.*

/** In-game event and narrative message dialogs (target captured, gate destroyed, Ewhad and villain story events). */
@Composable
fun GameMessageDialogs(
    pendingGameMessage: GameMessage?,
    onDismissGameMessage: (() -> Unit)?,
    supports: LevelSupports,
) {
    // In-game event message dialog (target captured, gate destroyed, or Ewhad narrative events)
    pendingGameMessage?.let { msg ->
        when (msg.type) {
            GameMessageType.EWHAD_ENTERS ->
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.ewhad_enters_title),
                    text = stringResource(Res.string.ewhad_enters_text),
                    onDismiss = { onDismissGameMessage?.invoke() },
                )
            GameMessageType.EWHAD_RETREATS ->
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.ewhad_retreats_title),
                    text = stringResource(Res.string.ewhad_retreats_text),
                    onDismiss = { onDismissGameMessage?.invoke() },
                )
            GameMessageType.EWHAD_DEFEATED ->
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.ewhad_defeated_title),
                    text = stringResource(Res.string.ewhad_defeated_text),
                    onDismiss = { onDismissGameMessage?.invoke() },
                )
            GameMessageType.VILLAIN_ENTERS -> {
                val villainType = attackerTypeFromMessageName(msg.name)
                val (villainTitle, villainText) = villainEnterContent(msg.name)
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = villainTitle,
                    text = villainText,
                    onDismiss = { onDismissGameMessage?.invoke() },
                    supports = supports,
                    backgroundOverride = villainMessageBackground(msg.name),
                    accentColorOverride = villainMessageButtonColor(msg.name),
                    iconAttackerTypeOverride = villainType,
                )
            }
            GameMessageType.VILLAIN_DEFEATED -> {
                val villainType = attackerTypeFromMessageName(msg.name)
                val villainName = villainType?.villainName ?: stringResource(Res.string.villain)
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.villain_defeated_title),
                    text = stringResource(Res.string.villain_defeated_text, villainName),
                    onDismiss = { onDismissGameMessage?.invoke() },
                    backgroundOverride = villainMessageBackground(msg.name),
                    accentColorOverride = villainMessageButtonColor(msg.name),
                    iconAttackerTypeOverride = villainType,
                )
            }
            GameMessageType.SILAS_MIRROR_HIT ->
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.villain_silas_mirror_hit_title),
                    text = stringResource(Res.string.villain_silas_mirror_hit_text),
                    onDismiss = { onDismissGameMessage?.invoke() },
                    backgroundOverride = villainMessageBackground(AttackerType.SILAS_THE_MASKMASTER.name),
                    accentColorOverride = villainMessageButtonColor(AttackerType.SILAS_THE_MASKMASTER.name),
                    iconAttackerTypeOverride = AttackerType.SILAS_THE_MASKMASTER,
                )
            GameMessageType.COVEN_SWAP -> {
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.villain_coven_swap_title),
                    text = stringResource(Res.string.villain_coven_swap_text),
                    onDismiss = { onDismissGameMessage?.invoke() },
                    backgroundOverride = villainMessageBackground(AttackerType.GRAND_COVEN_MOTHER_SYBILLA.name),
                    accentColorOverride = villainMessageButtonColor(AttackerType.GRAND_COVEN_MOTHER_SYBILLA.name),
                    iconAttackerTypeOverride = AttackerType.GRAND_COVEN_MOTHER_SYBILLA,
                )
            }
            GameMessageType.WAAAGH_FRENZY ->
                NarrativeMessageDialog(
                    type = NarrativeMessageType.EWHAD,
                    title = stringResource(Res.string.waaagh_frenzy_title),
                    text = stringResource(Res.string.waaagh_frenzy_message),
                    onDismiss = { onDismissGameMessage?.invoke() },
                    backgroundOverride = Res.drawable.message_background_waaagh,
                    accentColorOverride = Color(0xFFD32F2F),
                    topImageOverride = Res.drawable.waaagh_image,
                    topImageFillWidth = true,
                    contentTopOffset = 48.dp,
                )
            GameMessageType.STORY_INTRO -> {
                val levelEditorId = msg.name
                if (levelEditorId != null) {
                    val fortressTitle = stringResource(Res.string.level_the_fortress_title)
                    // Map level editor ID to (NarrativeMessageType, title, storyText)
                    val story: Triple<NarrativeMessageType, String, String>? =
                        when (levelEditorId) {
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
                                    "$fortressTitle - ${stringResource(
                                        Res.string.level_the_fortress_1_first_attacks_subtitle,
                                    )}",
                                    stringResource(Res.string.story_the_fortress_1_first_attacks),
                                )
                            "the_fortress_2_orks_marching" ->
                                Triple(
                                    NarrativeMessageType.STORY,
                                    "$fortressTitle - ${stringResource(
                                        Res.string.level_the_fortress_2_orks_marching_subtitle,
                                    )}",
                                    stringResource(Res.string.story_the_fortress_2_orks_marching),
                                )
                            "the_fortress_3_necromancer" ->
                                Triple(
                                    NarrativeMessageType.STORY,
                                    "$fortressTitle - ${stringResource(
                                        Res.string.level_the_fortress_3_necromancer_subtitle,
                                    )}",
                                    stringResource(Res.string.story_the_fortress_3_necromancer),
                                )
                            "the_fortress_4_magic_assault" ->
                                Triple(
                                    NarrativeMessageType.STORY,
                                    "$fortressTitle - ${stringResource(
                                        Res.string.level_the_fortress_4_magic_assault_subtitle,
                                    )}",
                                    stringResource(Res.string.story_the_fortress_4_magic_assault),
                                )
                            "the_fortress_5_mighty_forces" ->
                                Triple(
                                    NarrativeMessageType.STORY,
                                    "$fortressTitle - ${stringResource(
                                        Res.string.level_the_fortress_5_mighty_forces_subtitle,
                                    )}",
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
                    if (story != null) {
                        val (narrativeType, storyTitle, storyText) = story
                        NarrativeMessageDialog(
                            type = narrativeType,
                            title = storyTitle,
                            text = storyText,
                            onDismiss = { onDismissGameMessage?.invoke() },
                            supports = supports,
                        )
                    } else {
                        // No predefined story text for this level (e.g. user-created
                        // levels): dismiss so it does not block later queued messages
                        // (scripted-event messages, target-taken, etc.).
                        LaunchedEffect(msg) { onDismissGameMessage?.invoke() }
                    }
                } else {
                    LaunchedEffect(msg) { onDismissGameMessage?.invoke() }
                }
            }
            GameMessageType.EVENT_MESSAGE -> {
                val messageKey = msg.name
                if (messageKey == null) {
                    // Events configured with "No message" never show a popup; this
                    // only guards messages queued by older versions/saves.
                    LaunchedEffect(msg) { onDismissGameMessage?.invoke() }
                } else {
                    val eventText =
                        if (messageKey == "event_msg_rune_network_taken_over") {
                            com.hyperether.resources.LocalizedStrings
                                .getFormatted(
                                    messageKey,
                                    msg.eventMessageAmount ?: 0,
                                    locale = com.hyperether.resources.currentLanguage.value,
                                )
                        } else {
                            com.hyperether.resources.LocalizedStrings
                                .get(messageKey, com.hyperether.resources.currentLanguage.value)
                        }
                    val frame = msg.eventMessageFrame
                    NarrativeMessageDialog(
                        type = EventMessageFrames.narrativeType(frame),
                        title = stringResource(Res.string.event_message_title),
                        text = eventText,
                        onDismiss = { onDismissGameMessage?.invoke() },
                        eventGains = msg.eventActions,
                        backgroundOverride = EventMessageFrames.background(frame),
                        accentColorOverride = EventMessageFrames.accentColor(frame),
                        iconAttackerTypeOverride = EventMessageFrames.styleAttackerType(frame),
                        showTopIcon = false,
                    )
                }
            }
            else ->
                GameEventMessageDialog(
                    message = msg,
                    onDismiss = { onDismissGameMessage?.invoke() },
                )
        }
    }
}
