@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Composable displaying information about the backend server, user accounts, and logging.
 */
@Composable
fun BackendInfo() {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        Text(
            text = stringResource(Res.string.backend_info_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logging section
            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_logging_heading),
                body = stringResource(Res.string.backend_info_logging_body)
            )

            // Optional account section
            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_account_heading),
                body = stringResource(Res.string.backend_info_account_body)
            )

            // How to get an account section
            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_account_how_heading),
                body = stringResource(Res.string.backend_info_account_how_body)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
        }
    }
}

@Composable
private fun BackendInfoSection(
    heading: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
