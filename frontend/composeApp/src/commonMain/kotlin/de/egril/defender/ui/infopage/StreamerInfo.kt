@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.utils.safeRun
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.audio_background_music_streamer_notice_intro
import defender_of_egril.composeapp.generated.resources.streamer_info_notice_link_text
import defender_of_egril.composeapp.generated.resources.streamer_info_notice_text
import defender_of_egril.composeapp.generated.resources.streamer_info_mute_music_hint
import defender_of_egril.composeapp.generated.resources.streamer_info_no_content_id_notice
import defender_of_egril.composeapp.generated.resources.streamer_info_scope
import defender_of_egril.composeapp.generated.resources.streamer_info_title

private const val FESLIYAN_POLICY_URL = "https://www.fesliyanstudios.com/policy"

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
                text = stringResource(Res.string.streamer_info_scope),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            FesliyanStreamerNotice()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Shared notice about the Fesliyan Studios YouTube monetization policy for the
 * background music used in Defender of Egril. Reused in the audio licenses
 * page and the dedicated streamer info page so both locations present the
 * same text.
 *
 * Callers are expected to place this inside a single, page-level
 * [SelectionContainer]. Only the notice body (second line) is selectable;
 * the policy link is excluded from selection via [DisableSelection] so that
 * copying the notice never spans two independently-registered selection
 * hierarchies (which previously crashed the app with a
 * "layouts are not part of the same hierarchy" error caused by a nested
 * SelectionContainer).
 */
@Composable
internal fun FesliyanStreamerNotice() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.audio_background_music_streamer_notice_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = stringResource(Res.string.streamer_info_no_content_id_notice),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DisableSelection {
                    Text(
                        text = stringResource(Res.string.streamer_info_notice_link_text),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.Underline,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.clickable {
                                safeRun { uriHandler.openUri(FESLIYAN_POLICY_URL) }
                            },
                    )
                }

                Text(
                    text = stringResource(Res.string.streamer_info_notice_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Text(
            text = stringResource(Res.string.streamer_info_mute_music_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
