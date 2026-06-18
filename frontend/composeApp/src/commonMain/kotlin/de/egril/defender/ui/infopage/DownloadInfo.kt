@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Manages focus cycling through download links only.
 * Used by InfoPageScreen to restrict Tab/Shift+Tab to links on the Download tab.
 */
class LinkFocusManager {
    private var _requesters = mutableListOf<FocusRequester>()
    private var _currentIndex = -1

    val size: Int get() = _requesters.size
    val currentIndex: Int get() = _currentIndex

    fun resetRegistration() {
        _requesters.clear()
    }

    fun register(requester: FocusRequester): Int {
        val index = _requesters.size
        _requesters.add(requester)
        return index
    }

    fun focusNext() {
        if (_requesters.isEmpty()) return
        _currentIndex = (_currentIndex + 1) % _requesters.size
        try { _requesters[_currentIndex].requestFocus() } catch (_: Exception) {}
    }

    fun focusPrevious() {
        if (_requesters.isEmpty()) return
        _currentIndex = if (_currentIndex <= 0) _requesters.size - 1 else _currentIndex - 1
        try { _requesters[_currentIndex].requestFocus() } catch (_: Exception) {}
    }

    fun updateIndex(index: Int) {
        _currentIndex = index
    }
}

private const val GITHUB_RELEASES_PAGE =
    "https://github.com/julianegner/defender-of-egril/releases/latest"

/**
 * Composable showing download links for all platforms fetched from the GitHub API,
 * sideloading instructions for Android, and the impressum section.
 *
 * Only shown on WASM platform when the withImpressum build flag is enabled.
 */
@Composable
fun DownloadInfo(onNavigateToInstallation: () -> Unit = {}, scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(), linkFocusManager: LinkFocusManager? = null) {
    var release by remember { mutableStateOf<GithubRelease?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = fetchLatestRelease()
        if (result != null) {
            release = result
        } else {
            loadError = true
        }
    }

    // Reset link registration on each recomposition
    linkFocusManager?.resetRegistration()

    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Text(
            text = stringResource(Res.string.download_info_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Version badge – shown as soon as the release data is available
        val tagName = release?.tagName?.takeIf { it.isNotEmpty() }
        if (tagName != null) {
            Text(
                text = stringResource(Res.string.download_info_latest_version, tagName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // Navigation hint area (only when shortcut chips are ON)
            if (AppSettings.showButtonShortcutHints.value) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.download_nav_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

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
                    text = stringResource(Res.string.download_info_view_releases),
                    linkFocusManager = linkFocusManager
                )
            }

            when {
                // Still loading
                release == null && !loadError -> {
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
                loadError || release?.assets?.isEmpty() == true -> {                    Card(
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
                    release?.assets?.forEach { asset ->
                        AssetListItem(asset = asset, linkFocusManager = linkFocusManager)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PlayStoreInfo(linkFocusManager = linkFocusManager)

            Spacer(modifier = Modifier.height(16.dp))

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onNavigateToInstallation
                ) {
                    Text(stringResource(Res.string.download_info_goto_installation))
                    if (AppSettings.showButtonShortcutHints.value) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ShortcutKeyChip(text = "I", color = LocalContentColor.current.copy(alpha = 0.75f))
                    }
                }
            }

            // Impressum section
            ImpressumSection()
        }
        }
    }
}

@Composable
private fun AssetListItem(asset: GithubReleaseAsset, linkFocusManager: LinkFocusManager? = null) {
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
        // Fixed minimum width so the file-type label always stays on one line
        // ("Flatpak" is the longest value at ~7 chars)
        Text(
            text = fileType,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.widthIn(min = 60.dp)
        )
        // Download link fills remaining space; long filenames are truncated with ellipsis
        // so the entire row always fits on a single line on narrow screens.
        DownloadLink(url = asset.downloadUrl, text = asset.name, modifier = Modifier.weight(1f), linkFocusManager = linkFocusManager)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DownloadLink(url: String, text: String, modifier: Modifier = Modifier, chipText: String = "Enter", linkFocusManager: LinkFocusManager? = null) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val myIndex = if (linkFocusManager != null) {
        linkFocusManager.register(focusRequester)
    } else {
        -1
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 8.dp)
                .then(
                    if (isFocused) Modifier.border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        RoundedCornerShape(4.dp)
                    ).padding(4.dp)
                    else Modifier
                )
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (state.isFocused && linkFocusManager != null) {
                        linkFocusManager.updateIndex(myIndex)
                    }
                }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        uriHandler.openUri(url)
                        true
                    } else false
                }
                .clickable { uriHandler.openUri(url) }
        )
        if (isFocused && AppSettings.showButtonShortcutHints.value) {
            Spacer(modifier = Modifier.width(4.dp))
            ShortcutKeyChip(text = chipText)
        }
    }
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
    name.endsWith(".AppImage", ignoreCase = true) -> "AppImage"
    name.endsWith(".dmg", ignoreCase = true) -> "DMG"
    name.endsWith(".ipa", ignoreCase = true) -> "IPA"
    else -> name.substringAfterLast(".").uppercase().ifEmpty { "File" }
}
