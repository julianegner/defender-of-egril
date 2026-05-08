package de.egril.defender.ui.infopage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.play_store_test_info_after_registration
import defender_of_egril.composeapp.generated.resources.play_store_test_info_mailing_list_label
import defender_of_egril.composeapp.generated.resources.play_store_test_info_play_store_label
import defender_of_egril.composeapp.generated.resources.play_store_test_info_register_step
import defender_of_egril.composeapp.generated.resources.play_store_test_info_search_hint
import defender_of_egril.composeapp.generated.resources.play_store_test_info_title

private const val GOOGLE_MAILING_LIST = "https://groups.google.com/g/defender-of-egril"
private const val GOOGLE_PLAY_STORE = "https://play.google.com/store/apps/details?id=de.egril.defender"

@Composable
fun PlayStorePrereleaseInfo() {
    val uriHandler = LocalUriHandler.current

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
                text = stringResource(Res.string.play_store_test_info_mailing_list_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = GOOGLE_MAILING_LIST,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(GOOGLE_MAILING_LIST) }
            )
            Text(
                text = stringResource(Res.string.play_store_test_info_after_registration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(Res.string.play_store_test_info_play_store_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = GOOGLE_PLAY_STORE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(GOOGLE_PLAY_STORE) }
            )
            Text(
                text = stringResource(Res.string.play_store_test_info_search_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
