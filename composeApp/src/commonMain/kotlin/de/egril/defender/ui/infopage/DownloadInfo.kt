@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

private const val GITHUB_RELEASES_URL =
    "https://github.com/qvest-digital/defender-of-egril-fork/releases/latest"

private const val ANDROID_APK_DIRECT_URL =
    "https://github.com/qvest-digital/defender-of-egril-fork/releases/latest/download/de.egril.defender-productionRelease.apk"

/**
 * Composable showing download links for all platforms, sideloading instructions for Android,
 * and the impressum section.
 *
 * Only shown on WASM platform when the withImpressum build flag is enabled.
 */
@Composable
fun DownloadInfo() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Text(
            text = stringResource(Res.string.download_info_title),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Intro text
            Text(
                text = stringResource(Res.string.download_info_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Link to all releases
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.download_info_all_releases_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 8.dp)
                )
                DownloadLink(
                    url = GITHUB_RELEASES_URL,
                    text = stringResource(Res.string.download_info_view_releases)
                )
            }

            // Note about download links
            Text(
                text = stringResource(Res.string.download_info_platform_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Download list
            DownloadListItem(
                platform = "Android",
                fileType = "APK (sideload)",
                url = ANDROID_APK_DIRECT_URL,
                linkLabel = stringResource(Res.string.download_info_direct_download)
            )
            DownloadListItem(
                platform = "Windows",
                fileType = "Installer (EXE)",
                url = GITHUB_RELEASES_URL,
                linkLabel = stringResource(Res.string.download_info_view_releases)
            )
            DownloadListItem(
                platform = "Windows",
                fileType = "Installer (MSI)",
                url = GITHUB_RELEASES_URL,
                linkLabel = stringResource(Res.string.download_info_view_releases)
            )
            DownloadListItem(
                platform = "Linux",
                fileType = "Package (DEB)",
                url = GITHUB_RELEASES_URL,
                linkLabel = stringResource(Res.string.download_info_view_releases)
            )
            DownloadListItem(
                platform = "Linux",
                fileType = "Package (Snap)",
                url = GITHUB_RELEASES_URL,
                linkLabel = stringResource(Res.string.download_info_view_releases)
            )
            DownloadListItem(
                platform = "Linux",
                fileType = "Bundle (Flatpak)",
                url = GITHUB_RELEASES_URL,
                linkLabel = stringResource(Res.string.download_info_view_releases)
            )
            DownloadListItem(
                platform = "macOS",
                fileType = "Disk Image (DMG)",
                url = GITHUB_RELEASES_URL,
                linkLabel = stringResource(Res.string.download_info_view_releases)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Android sideloading instructions
            Text(
                text = stringResource(Res.string.download_info_sideload_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(Res.string.installation_android_step1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(Res.string.installation_android_step1a),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            Text(
                text = stringResource(Res.string.installation_android_step1b),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            Text(
                text = stringResource(Res.string.installation_android_step2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(Res.string.installation_android_step3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(Res.string.installation_android_step4),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = stringResource(Res.string.installation_android_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Impressum section
            ImpressumSection()
        }
    }
}

@Composable
private fun DownloadListItem(
    platform: String,
    fileType: String,
    url: String,
    linkLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = fileType,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        DownloadLink(url = url, text = linkLabel)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DownloadLink(
    url: String,
    text: String
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .padding(start = 8.dp)
            .clickable { uriHandler.openUri(url) }
    )
}
