@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings
import dev.vicart.compose.material.symbols.FilledSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.tooltip_feedback
import defender_of_egril.composeapp.generated.resources.feedback_form_title
import defender_of_egril.composeapp.generated.resources.close

/**
 * Feedback button with chat/feedback icon that opens a dialog containing the feedback form.
 * Can be placed on any screen to provide quick access to the feedback form.
 * Modeled after SettingsButton for consistent UX.
 *
 * @param gameContext Optional game context (level name, turn, state) passed when the button is
 *   placed on the gameplay screen; null on menu/info screens.
 */
@Composable
fun FeedbackButton(
    modifier: Modifier = Modifier,
    gameContext: GameFeedbackContext? = null,
    shortcutKey: String? = null,
    triggerOpen: Boolean = false,
    onTriggerHandled: (() -> Unit)? = null
) {
    var showFeedback by remember { mutableStateOf(false) }
    val feedbackLabel = stringResource(Res.string.tooltip_feedback)

    LaunchedEffect(triggerOpen) {
        if (triggerOpen) {
            showFeedback = true
            onTriggerHandled?.invoke()
        }
    }

    TooltipWrapper(text = feedbackLabel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { showFeedback = true },
                modifier = modifier.semantics { contentDescription = feedbackLabel }
            ) {
                FilledSymbol(
                    icon = MaterialSymbols.RATE_REVIEW,
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (shortcutKey != null && AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(2.dp))
                ShortcutKeyChip(text = shortcutKey)
            }
        }
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false }, gameContext = gameContext)
    }
}

/**
 * Full-screen dialog containing the feedback form.
 * Scrollable to accommodate all form fields on smaller screens.
 */
@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    gameContext: GameFeedbackContext? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 560.dp)
                .fillMaxHeight(fraction = 0.92f)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Back || event.key == Key.Escape)
                    ) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // Title row with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.feedback_form_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val closeLabel = stringResource(Res.string.close)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics { contentDescription = closeLabel }
                    ) {
                        FilledSymbol(
                            icon = MaterialSymbols.CLOSE,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Scrollable form content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    FeedbackFormContent(
                        showTitle = false,
                        gameContext = gameContext
                    )
                }
            }
        }
    }
}
