package de.egril.defender.ui.infopage

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.ImpressumConstants
import de.egril.defender.WithImpressum

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
    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = ImpressumConstants.IMPRESSUM_TITLE,
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
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

/**
 * Impressum wrapper that shows/hides the impressum content
 * Only displayed on WASM platform when withImpressum flag is true
 */
@Composable
fun ImpressumWrapper(rowModifier: Modifier = Modifier) {
    // Only show impressum if the compile flag is set
    if (!WithImpressum.withImpressum) {
        return
    }
    
    var displayImpressum by remember { mutableStateOf(false) }
    
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = rowModifier
    ) {
        if (displayImpressum) {
            Column(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "X",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray,
                        modifier = Modifier
                            .clickable { displayImpressum = false }
                            .padding(8.dp)
                    )
                }
                ImpressumContent()
            }
        } else {
            Text(
                text = ImpressumConstants.IMPRESSUM_TITLE,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clickable { displayImpressum = true }
                    .padding(8.dp)
            )
        }
    }
}
