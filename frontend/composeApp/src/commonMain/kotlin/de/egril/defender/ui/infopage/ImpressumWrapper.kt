package de.egril.defender.ui.infopage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.ImpressumConstants
import de.egril.defender.WithImpressum
import androidx.compose.foundation.text.selection.SelectionContainer
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.icon.CrossIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.close

/**
 * Clickable text that looks like a link
 */
@Composable
private fun TextLink(
    url: String,
    text: String = url,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable {
            uriHandler.openUri(url)
        }
    )
}

/**
 * Impressum content (legal information)
 */
@Composable
private fun ImpressumContent() {
    SelectionContainer {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
        Text(
            text = buildString {
                append(ImpressumConstants.IMPRESSUM_NAME)
                append("\n")
                append(ImpressumConstants.IMPRESSUM_STREET)
                append("\n")
                append(ImpressumConstants.IMPRESSUM_POSTAL_CODE)
                append(" ")
                append(ImpressumConstants.IMPRESSUM_CITY)
                append("\n")
                append(ImpressumConstants.IMPRESSUM_COUNTRY)
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ImpressumConstants.IMPRESSUM_EMAIL_LABEL,
                style = MaterialTheme.typography.bodySmall
            )
            TextLink(
                url = "mailto:${ImpressumConstants.IMPRESSUM_EMAIL}",
                text = ImpressumConstants.IMPRESSUM_EMAIL
            )
        }
        }
    }
}

/**
 * Impressum wrapper that shows/hides the impressum content
 * Only displayed on WASM platform when withImpressum flag is true
 */
@Composable
fun ImpressumWrapper(rowModifier: Modifier = Modifier, triggerOpen: Boolean = false, onTriggerOpenHandled: () -> Unit = {}, triggerClose: Boolean = false, onTriggerCloseHandled: () -> Unit = {}, onDismissed: () -> Unit = {}) {
    // Only show impressum if the compile flag is set
    if (!WithImpressum.withImpressum) {
        return
    }
    
    var displayImpressum by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Handle external trigger to open
    LaunchedEffect(triggerOpen) {
        if (triggerOpen) {
            displayImpressum = true
            onTriggerOpenHandled()
        }
    }

    // Handle external trigger to close
    LaunchedEffect(triggerClose) {
        if (triggerClose) {
            displayImpressum = false
            onTriggerCloseHandled()
        }
    }

    // Auto-focus when impressum is opened for keyboard navigation
    LaunchedEffect(displayImpressum) {
        if (displayImpressum) {
            try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
        }
    }
    
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = rowModifier
    ) {
        if (displayImpressum) {
            Card(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && (event.key == Key.Escape || event.key == Key.Back)) {
                            displayImpressum = false
                            onDismissed()
                            true
                        } else false
                    },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ImpressumConstants.IMPRESSUM_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShortcutKeyChip(text = "N")
                            val closeLabel = stringResource(Res.string.close)
                            IconButton(
                                onClick = { displayImpressum = false; onDismissed() },
                                modifier = Modifier.semantics { contentDescription = closeLabel }
                            ) {
                                CrossIcon(size = 16.dp)
                            }
                        }
                    }
                    HorizontalDivider()
                    ImpressumContent()
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = ImpressumConstants.IMPRESSUM_TITLE,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clickable { displayImpressum = true }
                        .padding(8.dp)
                )
                ShortcutKeyChip(text = "N")
            }
        }
    }
}
