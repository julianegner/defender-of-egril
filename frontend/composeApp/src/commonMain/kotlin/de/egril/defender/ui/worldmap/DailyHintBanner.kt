@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.close
import defender_of_egril.composeapp.generated.resources.daily_hint_title
import dev.vicart.compose.material.symbols.FilledSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import org.jetbrains.compose.resources.StringResource

/**
 * Banner that displays a single daily hint on the world map.
 *
 * The banner adapts to the current Material color scheme (so it works in both light
 * and dark mode), shows the localized title "Daily Hint" with an X close button on
 * the right, and an optional shortcut chip when shortcut hints are enabled.
 *
 * Dismissal is handled by the caller (typically by persisting the shown date so the
 * banner does not reappear today and removing it from the composition).
 *
 * @param messageRes localized message body for the selected hint.
 * @param onDismiss invoked when the user clicks the X button or presses the
 *  shortcut key.
 */
@Composable
fun DailyHintBanner(
    messageRes: StringResource,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(Res.string.daily_hint_title)
    val closeDescription = stringResource(Res.string.close)
    Card(
        modifier = modifier.semantics { contentDescription = title },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (AppSettings.showButtonShortcutHints.value) {
                    ShortcutKeyChip(text = "X")
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = closeDescription }
                ) {
                    Box(modifier = Modifier.clearAndSetSemantics { }) {
                        FilledSymbol(
                            icon = MaterialSymbols.CLOSE,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
