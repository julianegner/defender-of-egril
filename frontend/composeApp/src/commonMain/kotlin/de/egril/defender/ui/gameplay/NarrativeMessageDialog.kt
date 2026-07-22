package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/**
 * The type of narrative message popup.
 */
enum class NarrativeMessageType {
    STORY, // Story message with wooden frame background
    EWHAD, // Ewhad message with dark gargoyle frame background
}

internal data class NarrativeTextFramePaddingFractions(
    val top: Float,
    val bottom: Float,
)

private const val KEYBOARD_SCROLL_STEP = 150

// message_background_story.png has original source dimensions of 500×500 px, with wooden side rails starting at 165 px
// and whose parchment content begins 135 px from the top and bottom.
private const val STORY_BACKGROUND_SOURCE_SIZE = 500
private const val STORY_BACKGROUND_SIDE_SLICE_PX = 165
private const val STORY_BACKGROUND_VERTICAL_PADDING_PX = 135f

// Keep the precomputed ratio alongside the source-asset constants so every sizing calculation uses
// the same scaling factor instead of re-deriving it at each call site.
private const val STORY_BACKGROUND_SIDE_RATIO = 165f / STORY_BACKGROUND_SOURCE_SIZE.toFloat()
private const val NARRATIVE_DEFAULT_VERTICAL_PADDING_RATIO = STORY_BACKGROUND_VERTICAL_PADDING_PX / STORY_BACKGROUND_SOURCE_SIZE
private val STORY_DIALOG_DESKTOP_WIDTH = 960.dp
private val STORY_DIALOG_DESKTOP_HEIGHT = 700.dp
private val EWHAD_DIALOG_DESKTOP_WIDTH = 700.dp

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
 * @param iconAttackerTypeOverride Optional attacker type used for the top icon in Ewhad-style
 *   narrative dialogs (including villain messages that reuse this frame). When null, Ewhad is
 *   shown as before.
 */
@Composable
fun NarrativeMessageDialog(
    type: NarrativeMessageType,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    supports: de.egril.defender.model.LevelSupports? = null,
    eventGains: List<EventAction>? = null,
    // Optional per-message frame overrides. Used to give each villain its own distinct message
    // border/background (see issue #538): pass the villain's dedicated background image and an accent
    // colour for the button. When null, the [type]-based defaults are used.
    backgroundOverride: org.jetbrains.compose.resources.DrawableResource? = null,
    accentColorOverride: Color? = null,
    iconAttackerTypeOverride: AttackerType? = null,
) {
    val isMobile = isPlatformMobile
    val useWideStoryLayout = type == NarrativeMessageType.STORY && !isMobile

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !useWideStoryLayout),
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
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
            when {
                backgroundOverride != null -> painterResource(backgroundOverride)
                type == NarrativeMessageType.STORY -> painterResource(Res.drawable.message_background_story)
                else -> painterResource(Res.drawable.message_background_ewhad)
            }
        val buttonColor = accentColorOverride ?: if (type == NarrativeMessageType.EWHAD) Color(0xFF4A2060) else Color(0xFF5C3A1E)

        // Both background images are square (500×500 and 1024×1024).
        // Padding keeps text inside the frame border, computed as a fixed fraction of
        // the dialog dimensions:
        //   message_background_story.png: inner parchment starts at px ≈ 165/500 per side (h)
        //                                 and px ≈ 135/500 per side (v).
        //   message_background_ewhad.png: inner area at ≈ 280/1024 per side — smaller, so the
        //                                 story fractions cover both.
        // On mobile, BoxWithConstraints fills the available popup width so the dialog scales to
        // the actual device screen size rather than using a fixed narrow value.
        BoxWithConstraints(
            modifier =
                when {
                    isMobile -> Modifier.fillMaxWidth()
                    useWideStoryLayout -> Modifier.width(STORY_DIALOG_DESKTOP_WIDTH)
                    else -> Modifier.width(EWHAD_DIALOG_DESKTOP_WIDTH)
                }.focusRequester(focusRequester)
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
            val dialogWidth = maxWidth
            val dialogHeight = if (useWideStoryLayout) STORY_DIALOG_DESKTOP_HEIGHT else dialogWidth
            val horizontalPadding =
                if (useWideStoryLayout) {
                    dialogHeight * STORY_BACKGROUND_SIDE_RATIO
                } else {
                    dialogWidth * STORY_BACKGROUND_SIDE_RATIO
                }
            val verticalPaddingFractions = narrativeTextFramePaddingFractions(type, iconAttackerTypeOverride)
            val topPadding = dialogHeight * verticalPaddingFractions.top
            val bottomPadding = dialogHeight * verticalPaddingFractions.bottom

            Box(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .height(dialogHeight)
                        .testTag("narrativeMessageDialog"),
                contentAlignment = Alignment.Center,
            ) {
                if (useWideStoryLayout) {
                    StoryMessageBackground(modifier = Modifier.matchParentSize())
                } else {
                    Image(
                        painter = backgroundPainter,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                }

                // Content overlaid on background – scrollable so long texts never overflow the frame
                Column(
                    modifier =
                        if (type == NarrativeMessageType.EWHAD) {
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = topPadding,
                                    bottom = bottomPadding,
                                ).verticalScroll(scrollState)
                        } else {
                            Modifier
                                .padding(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = topPadding,
                                    bottom = bottomPadding,
                                ).fillMaxWidth()
                                .verticalScroll(scrollState)
                        },
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
                                attackerType = iconAttackerTypeOverride ?: AttackerType.EWHAD,
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
                    if (type == NarrativeMessageType.STORY && supports != null && supports.isNotEmpty()) {
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

internal fun narrativeTextFramePaddingFractions(
    type: NarrativeMessageType,
    attackerType: AttackerType?,
): NarrativeTextFramePaddingFractions =
    when {
        type == NarrativeMessageType.EWHAD && attackerType == AttackerType.SNOTLING_BOSS ->
            NarrativeTextFramePaddingFractions(
                top = 0.31f,
                bottom = 0.33f,
            )

        type == NarrativeMessageType.EWHAD && attackerType == AttackerType.MORGUK_BONEWHISPER ->
            NarrativeTextFramePaddingFractions(
                top = 0.30f,
                bottom = 0.33f,
            )

        else ->
            NarrativeTextFramePaddingFractions(
                top = NARRATIVE_DEFAULT_VERTICAL_PADDING_RATIO,
                bottom = NARRATIVE_DEFAULT_VERTICAL_PADDING_RATIO,
            )
    }

@Composable
private fun StoryMessageBackground(modifier: Modifier = Modifier) {
    val source = imageResource(Res.drawable.message_background_story)

    Canvas(modifier = modifier) {
        val sideSlicePx = STORY_BACKGROUND_SIDE_SLICE_PX
        val centerSlicePx = source.width - (sideSlicePx * 2)
        // Defensive: avoid invalid rendering sizes if the canvas height rounds down to zero.
        val destinationHeight = size.height.roundToInt().coerceAtLeast(1)
        val destinationSideWidth =
            minOf(
                size.height * STORY_BACKGROUND_SIDE_RATIO,
                size.width / 2f,
            ).roundToInt().coerceAtLeast(1)
        // Prevent negative center widths when the side slices overlap.
        // A zero-width center is safe because the draw below only runs when destinationCenterWidth > 0.
        val destinationCenterWidth = (size.width.roundToInt() - (destinationSideWidth * 2)).coerceAtLeast(0)

        drawImage(
            image = source,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(sideSlicePx, source.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(destinationSideWidth, destinationHeight),
        )
        if (destinationCenterWidth > 0) {
            drawImage(
                image = source,
                srcOffset = IntOffset(sideSlicePx, 0),
                srcSize = IntSize(centerSlicePx, source.height),
                dstOffset = IntOffset(destinationSideWidth, 0),
                dstSize = IntSize(destinationCenterWidth, destinationHeight),
            )
        }
        drawImage(
            image = source,
            srcOffset = IntOffset(source.width - sideSlicePx, 0),
            srcSize = IntSize(sideSlicePx, source.height),
            dstOffset = IntOffset(destinationSideWidth + destinationCenterWidth, 0),
            dstSize = IntSize(destinationSideWidth, destinationHeight),
        )
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
