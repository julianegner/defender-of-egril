package de.egril.defender.ui.infopage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.play_store_test_info_play_store_label
import defender_of_egril.composeapp.generated.resources.play_store_test_info_register_step
import defender_of_egril.composeapp.generated.resources.play_store_test_info_search_hint
import defender_of_egril.composeapp.generated.resources.play_store_test_info_title

private const val GOOGLE_PLAY_STORE = "https://play.google.com/store/apps/details?id=de.egril.defender"

@Composable
fun PlayStoreInfo(linkFocusManager: LinkFocusManager? = null) {
    SelectionContainer {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(Res.string.play_store_test_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(Res.string.play_store_test_info_register_step),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(Res.string.play_store_test_info_play_store_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                FocusableLink(
                    url = GOOGLE_PLAY_STORE,
                    text = GOOGLE_PLAY_STORE,
                    contentDesc = GOOGLE_PLAY_STORE,
                    linkFocusManager = linkFocusManager
                )
                Text(
                    text = stringResource(Res.string.play_store_test_info_search_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            @Deprecated("Use PlayStoreInfo instead")
            @Composable
            fun PlayStorePrereleaseInfo(linkFocusManager: LinkFocusManager? = null) {
                PlayStoreInfo(linkFocusManager = linkFocusManager)
            }
        }
    }
}

/**
 * A link that can be focused with Tab and activated with Enter.
 * Shows an "[Enter] to follow link" shortcut chip when focused.
 */
@Composable
private fun FocusableLink(url: String, text: String, contentDesc: String, linkFocusManager: LinkFocusManager? = null) {
    val uriHandler = LocalUriHandler.current
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val myIndex = if (linkFocusManager != null) {
        linkFocusManager.register(focusRequester)
    } else {
        -1
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .semantics { contentDescription = contentDesc }
                .then(
                    if (isFocused) Modifier.border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        RoundedCornerShape(4.dp)
                    ).padding(4.dp)
                    else Modifier
                )
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (state.isFocused && linkFocusManager != null) {
                        linkFocusManager.updateIndex(myIndex)
                    }
                }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        uriHandler.openUri(url)
                        true
                    } else false
                }
                .clickable(role = Role.Button) { uriHandler.openUri(url) }
        )
        if (isFocused && AppSettings.showButtonShortcutHints.value) {
            Spacer(modifier = Modifier.width(4.dp))
            ShortcutKeyChip(text = "Enter")
        }
    }
}
