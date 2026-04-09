@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

private const val GITHUB_RELEASES_PAGE =
    "https://github.com/julianegner/defender-of-egril/releases/latest"

/**
 * Composable showing download links for all platforms fetched from the GitHub API,
 * sideloading instructions for Android, and the impressum section.
 *
 * Only shown on WASM platform when the withImpressum build flag is enabled.
 */
@Composable
fun DownloadInfo(onNavigateToInstallation: () -> Unit = {}) {
    var assets by remember { mutableStateOf<List<GithubReleaseAsset>?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = fetchLatestReleaseAssets()
        if (result != null) {
            assets = result
        } else {
            loadError = true
        }
    }

    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    url = GITHUB_RELEASES_PAGE,
                    text = stringResource(Res.string.download_info_view_releases)
                )
            }

            when {
                // Still loading
                assets == null && !loadError -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.download_info_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // API unavailable – show fallback message
                loadError || assets?.isEmpty() == true -> {                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(Res.string.download_info_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Assets loaded – show per-file table
                else -> {
                    assets?.forEach { asset ->
                        AssetListItem(asset = asset)
                    }
                }
            }

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

            // Link to installation instructions
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToInstallation,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(Res.string.download_info_goto_installation))
            }

            // Impressum section
            ImpressumSection()
        }
        }
    }
}

@Composable
private fun AssetListItem(asset: GithubReleaseAsset) {
    val platform = asset.platform()
    val fileType = asset.fileType()

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
        DownloadLink(url = asset.downloadUrl, text = asset.name)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DownloadLink(url: String, text: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .padding(start = 8.dp)
            .clickable { uriHandler.openUri(url) }
    )
}

private fun GithubReleaseAsset.platform(): String = when {
    name.endsWith(".apk", ignoreCase = true) -> "Android"
    name.endsWith(".aab", ignoreCase = true) -> "Android"
    name.endsWith(".exe", ignoreCase = true) -> "Windows"
    name.endsWith(".msi", ignoreCase = true) -> "Windows"
    name.endsWith(".deb", ignoreCase = true) -> "Linux"
    name.endsWith(".snap", ignoreCase = true) -> "Linux"
    name.endsWith(".flatpak", ignoreCase = true) -> "Linux"
    name.endsWith(".dmg", ignoreCase = true) -> "macOS"
    name.endsWith(".ipa", ignoreCase = true) -> "iOS"
    else -> "Other"
}

private fun GithubReleaseAsset.fileType(): String = when {
    name.endsWith(".apk", ignoreCase = true) -> "APK"
    name.endsWith(".aab", ignoreCase = true) -> "AAB"
    name.endsWith(".exe", ignoreCase = true) -> "EXE"
    name.endsWith(".msi", ignoreCase = true) -> "MSI"
    name.endsWith(".deb", ignoreCase = true) -> "DEB"
    name.endsWith(".snap", ignoreCase = true) -> "Snap"
    name.endsWith(".flatpak", ignoreCase = true) -> "Flatpak"
    name.endsWith(".dmg", ignoreCase = true) -> "DMG"
    name.endsWith(".ipa", ignoreCase = true) -> "IPA"
    else -> name.substringAfterLast(".").uppercase().ifEmpty { "File" }
}

