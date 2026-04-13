package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.save.CommunityFileInfo
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.loadgame.SavefileLocationChip
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

private const val DESCRIPTION_PREVIEW_MAX_CHARS = 80

/**
 * Card displayed for a community level that exists on the server but has not yet been
 * downloaded to the local device.  Clicking the card triggers the on-demand download.
 */
@Composable
fun RemoteCommunityLevelCard(
    fileInfo: CommunityFileInfo,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    val backgroundColor = if (isDarkMode) Color(0xFF1A237E) else Color(0xFF3F51B5)
    val textColor = Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: "remote" badge aligned to the end
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SavefileLocationChip(
                    label = stringResource(Res.string.savefile_chip_remote),
                    color = MaterialTheme.colorScheme.primary,
                    onColor = MaterialTheme.colorScheme.onPrimary,
                    isMobile = false
                )
            }

            // Middle: level name + author + description (or downloading spinner)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectableText(
                        text = stringResource(Res.string.community_downloading_level),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                } else {
                    // Display the level id formatted as a readable name
                    val displayName = fileInfo.fileId
                        .replace('_', ' ')
                        .split(' ')
                        .joinToString(" ") { word ->
                            word.replaceFirstChar { it.uppercaseChar() }
                        }
                    SelectableText(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectableText(
                        text = stringResource(Res.string.community_author, fileInfo.authorUsername),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    // Description with truncation and tooltip
                    if (fileInfo.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val needsTruncation = fileInfo.description.length > DESCRIPTION_PREVIEW_MAX_CHARS
                        val truncated = if (needsTruncation) {
                            fileInfo.description.take(DESCRIPTION_PREVIEW_MAX_CHARS) + "…"
                        } else {
                            fileInfo.description
                        }
                        val tooltipText = if (needsTruncation) fileInfo.description else null
                        TooltipWrapper(text = tooltipText) {
                            SelectableText(
                                text = truncated,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // Bottom: tap-to-download hint (hidden while downloading)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isDownloading) {
                    val downloadHint = if (isPlatformMobile) {
                        stringResource(Res.string.community_tap_to_download)
                    } else {
                        stringResource(Res.string.community_click_to_download)
                    }
                    SelectableText(
                        text = downloadHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
