package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.save.CommunityFileInfo
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.loadgame.SavefileLocationChip
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * Card displayed for a community map that exists on the server but has not yet been
 * downloaded to the local device.  Clicking "Download" triggers the on-demand download.
 */
@Composable
fun RemoteCommunityMapCard(
    fileInfo: CommunityFileInfo,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(4f)) {
                    val displayName = fileInfo.fileId
                        .replace('_', ' ')
                        .split(' ')
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
                    SelectableText(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    SelectableText(
                        text = stringResource(Res.string.map_file_id, fileInfo.fileId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fileInfo.authorUsername.isNotEmpty()) {
                        SelectableText(
                            text = fileInfo.authorUsername,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Placeholder for minimap area – remote maps have no local data to render
                Box(
                    modifier = Modifier
                        .weight(6f)
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = stringResource(Res.string.community_downloading_level),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.community_tap_to_download),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(3f))

                Button(
                    onClick = onDownload,
                    enabled = !isDownloading
                ) {
                    Text(stringResource(Res.string.download_button))
                }
            }

            // "remote" badge in upper right corner
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SavefileLocationChip(
                    label = stringResource(Res.string.savefile_chip_remote),
                    color = MaterialTheme.colorScheme.primary,
                    onColor = MaterialTheme.colorScheme.onPrimary,
                    isMobile = false
                )
            }
        }
    }
}
