package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.ui.icon.WarningIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog shown when critical repository data files are missing
 */
@Composable
fun MissingRepositoryDataDialog(
    missingCategories: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WarningIcon(size = 24.dp)
                Text(
                    text = stringResource(Res.string.error_missing_repository_data_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.error_missing_repository_data_message),
                    fontSize = 14.sp
                )
                
                if (missingCategories.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.error_missing_repository_data_categories),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        missingCategories.forEach { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("•", fontSize = 12.sp)
                                Text(category, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Text(
                    text = stringResource(Res.string.error_missing_repository_data_reinstall),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    )
}
