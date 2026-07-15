package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * The type of narrative message popup.
 */
enum class NarrativeMessageType {
    STORY, // Story message with wooden frame background
    EWHAD, // Ewhad message with dark gargoyle frame background
}

private const val KEYBOARD_SCROLL_STEP = 150

/**
 * A popup dialog for narrative messages (story events and Ewhad events).
 *
 * Displays an image as background with centered text (black/dark gray).
 * For EWHAD type: shows the Ewhad icon in the upper center, a large title below it, and text below the title.
 * For STORY type: shows a centered title and text on the background.
 *
 * @param type     The type of narrative message (STORY or EWHAD).
 * @param title    The title text to display.
 * @param text     The body text to display below the title.
 * @param onDismiss Called when the dialog should be closed.
 * @param supports Optional level supports; when non-null and non-empty, a summary of the level's
 *   available player supports (objects + spell tokens) is shown below the body text.
 * @param eventGains Optional scripted-event actions; when non-null and non-empty, the granted
 *   elements (coins, mana, supports, …) are shown with symbols, names and amounts below the body.
 */
@Composable
fun NarrativeMessageDialog(
    type: NarrativeMessageType,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    supports: de.egril.defender.model.LevelSupports? = null,
    eventGains: List<EventAction>? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
        val isMobile = isPlatformMobile
        val titleFontSize =
            when {
                isMobile && type == NarrativeMessageType.EWHAD -> 16.sp
                isMobile -> 15.sp
                type == NarrativeMessageType.EWHAD -> 22.sp
                else -> 20.sp
            }
        val bodyFontSize = if (isMobile) 12.sp else MaterialTheme.typography.bodyMedium.fontSize
        val iconSize = if (isMobile) 56.dp else 80.dp
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        val backgroundPainter =
            when (type) {
                NarrativeMessageType.STORY -> painterResource(Res.drawable.story_message_background)
                NarrativeMessageType.EWHAD -> painterResource(Res.drawable.ewhad_message_background)
            }
        val buttonColor = if (type == NarrativeMessageType.EWHAD) Color(0xFF4A2060) else Color(0xFF5C3A1E)

        // Both background images are square (500×500 and 1024×1024).
        // Padding keeps text inside the frame border, computed as a fixed fraction of
        // the dialog dimensions:
        //   story_message_background.png: inner parchment starts at px ≈ 165/500 per side (h)
        //                                 and px ≈ 135/500 per side (v).
        //   ewhad_message_background.png: inner area at ≈ 280/1024 per side — smaller, so the
        //                                 story fractions cover both.
        // On mobile, BoxWithConstraints fills the available popup width so the dialog scales to
        // the actual device screen size rather than using a fixed narrow value.
        BoxWithConstraints(
            modifier =
                (if (isMobile) Modifier.fillMaxWidth() else Modifier.width(700.dp))
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(
                                        (scrollState.value + KEYBOARD_SCROLL_STEP).coerceAtMost(scrollState.maxValue),
                                    )
                                }
                                true
                            }
                            Key.DirectionUp -> {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo((scrollState.value - KEYBOARD_SCROLL_STEP).coerceAtLeast(0))
                                }
                                true
                            }
                            Key.Enter, Key.Escape, Key.Back -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            // dialogWidth equals the actual rendered width on all platforms.
            // Keep the dialog square to match the square source images.
            val dialogWidth = maxWidth
            val dialogHeight = dialogWidth
            val horizontalPadding = dialogWidth * (165f / 500f)
            val verticalPadding = dialogHeight * (135f / 500f)

            Box(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .height(dialogHeight),
                contentAlignment = Alignment.Center,
            ) {
                // Background image
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )

                // Content overlaid on background – scrollable so long texts never overflow the frame
                Column(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = horizontalPadding,
                                vertical = verticalPadding,
                            ).fillMaxWidth()
                            .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Scroll hint at the top so it is visible before any scrolling
                    if (AppSettings.showButtonShortcutHints.value) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ShortcutKeyChip(text = "Up/Down")
                            Text(
                                text = stringResource(Res.string.keyboard_nav_scroll),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // For Ewhad type: show Ewhad icon at top center
                    if (type == NarrativeMessageType.EWHAD) {
                        Box(
                            modifier = Modifier.size(iconSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            EnemyTypeIcon(
                                attackerType = AttackerType.EWHAD,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    // Title
                    SelectableText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center,
                        fontSize = titleFontSize,
                    )

                    // Body text
                    if (text.isNotEmpty()) {
                        SelectableText(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333),
                            textAlign = TextAlign.Center,
                            fontSize = bodyFontSize,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Optional summary of the granted elements of a scripted event
                    if (!eventGains.isNullOrEmpty()) {
                        EventGainsSummary(actions = eventGains)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Optional summary of the level's available player supports
                    if (supports != null && supports.isNotEmpty()) {
                        LevelSupportsSummary(
                            supports = supports,
                            title = stringResource(Res.string.level_supports_title),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Dismiss button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.ok),
                                color = Color.White,
                            )
                            ShortcutKeyChip(
                                text = "Enter",
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary of the elements granted by a scripted event, shown inside [NarrativeMessageDialog].
 *
 * Each action is rendered as a row with a symbol, a name and (where relevant) an amount so the
 * player knows exactly what they gained (coins, mana, support objects/spells) or what happened
 * (a mine being destroyed).
 */
@Composable
private fun EventGainsSummary(actions: List<EventAction>) {
    if (actions.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actions.forEach { action ->
            when (action.type) {
                EventActionType.GIVE_COINS ->
                    EventGainRow(label = stringResource(Res.string.event_summary_coins, action.amount)) {
                        MoneyIcon(size = 24.dp)
                    }
                EventActionType.GIVE_MANA ->
                    EventGainRow(label = stringResource(Res.string.event_summary_mana, action.amount)) {
                        PentagramIcon(size = 24.dp)
                    }
                EventActionType.GIVE_SUPPORT_OBJECT -> {
                    // Mirror EventScriptSystem.applyAction: fall back to the first entry / a count of
                    // one so the displayed element matches what was actually granted.
                    val type = action.supportObjectType ?: SupportObjectType.entries.first()
                    EventGainRow(label = eventGainCountLabel(type.localizedSupportName(), action.grantCount())) {
                        SupportObjectIcon(type, 24.dp)
                    }
                }
                EventActionType.GIVE_SUPPORT_SPELL -> {
                    // Mirror EventScriptSystem.applyAction: fall back to the first entry / a count of
                    // one so the displayed element matches what was actually granted.
                    val spell = action.spellType ?: SpellType.entries.first()
                    EventGainRow(label = eventGainCountLabel(spell.getLocalizedName(), action.grantCount())) {
                        SpellTargetIcon(spell = spell, size = 24.dp)
                    }
                }
                EventActionType.DESTROY_MINE ->
                    EventGainRow(label = stringResource(Res.string.event_act_destroy_mine)) {
                        ExplosionIcon(size = 24.dp)
                    }
            }
        }
    }
}

/** Append "×count" to a label when more than one element is granted. */
private fun eventGainCountLabel(
    label: String,
    count: Int,
): String = if (count > 1) "$label ×$count" else label

/** The number of support tokens granted by this action; falls back to one, matching the runtime. */
private fun EventAction.grantCount(): Int = if (amount > 0) amount else 1

@Composable
private fun EventGainRow(
    label: String,
    icon: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF333333),
        )
    }
}
