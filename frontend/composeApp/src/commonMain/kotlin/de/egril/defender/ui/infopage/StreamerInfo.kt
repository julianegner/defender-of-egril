@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.streamer_info_copyable_notice_label
import defender_of_egril.composeapp.generated.resources.streamer_info_copyable_notice_text
import defender_of_egril.composeapp.generated.resources.streamer_info_intro
import defender_of_egril.composeapp.generated.resources.streamer_info_scope
import defender_of_egril.composeapp.generated.resources.streamer_info_title

@Composable
fun StreamerInfo(scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()) {
    SelectionContainer {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.streamer_info_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(Res.string.streamer_info_intro),
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = stringResource(Res.string.streamer_info_scope),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            StreamerNoticeField()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun StreamerNoticeField() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.streamer_info_copyable_notice_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                text = stringResource(Res.string.streamer_info_copyable_notice_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
