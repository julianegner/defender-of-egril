@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.WithImpressum
import de.egril.defender.ui.common.ScrollableTabRowWithHints
import de.egril.defender.ui.editor.EditorHowToContent
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.feedback.FeedbackFormContent
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.isEditorAvailable
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.utils.isPlatformWasm
import de.egril.defender.utils.setInfoPageActive
import de.egril.defender.utils.toUrlSlug
import de.egril.defender.utils.updateBrowserUrl
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.launch

/**
 * Main info page screen that combines installation info, audio licenses, and backend info.
 * This screen is accessible via the info button on the main menu (all platforms).
 */
internal fun shouldUseCompactInfoHeaderLayout(
    isMobileWeb: Boolean,
    isLandscape: Boolean
): Boolean = isMobileWeb && isLandscape

@Composable
fun InfoPageScreen(
    onBack: () -> Unit,
    initialTab: InfoTab = InfoTab.INSTALLATION
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    val visibleTabs = remember(isEditorAvailable()) {
        buildList {
            if (isPlatformWasm && WithImpressum.withImpressum) add(InfoTab.DOWNLOAD)
            add(InfoTab.INSTALLATION)
            add(InfoTab.HOW_TO_PLAY)
            add(InfoTab.KEYBOARD_SHORTCUTS)
            add(InfoTab.AUDIO_LICENSES)
            add(InfoTab.LICENSE)
            add(InfoTab.BACKEND)
            add(InfoTab.FEEDBACK)
            if (isEditorAvailable()) add(InfoTab.EDITOR_HOWTO)
        }
    }

    // If the initial tab is not visible (e.g. EDITOR_HOWTO on mobile), fall back to INSTALLATION
    if (selectedTab !in visibleTabs) {
        selectedTab = InfoTab.INSTALLATION
    }

    val selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)
    
    // Scroll state for content area keyboard scrolling
    val contentScrollState = rememberScrollState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Suppress the HTML portrait-rotation overlay while on the info page and update the URL.
    DisposableEffect(Unit) {
        setInfoPageActive(true)
        onDispose {
            setInfoPageActive(false)
            updateBrowserUrl("/")
        }
    }
    // Keep the browser URL in sync with the selected tab.
    LaunchedEffect(selectedTab) {
        updateBrowserUrl("/info/${selectedTab.toUrlSlug()}")
    }
    
    val focusRequester = remember { FocusRequester() }
    val linkFocusManager = remember { LinkFocusManager() }
    var triggerOpenSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Back, Key.Escape -> {
                            onBack()
                            true
                        }
                        Key.DirectionDown -> {
                            coroutineScope.launch { contentScrollState.animateScrollTo(contentScrollState.value + 150) }
                            true
                        }
                        Key.DirectionUp -> {
                            coroutineScope.launch { contentScrollState.animateScrollTo((contentScrollState.value - 150).coerceAtLeast(0)) }
                            true
                        }
                        Key.DirectionRight -> {
                            val nextIndex = (selectedTabIndex + 1).coerceAtMost(visibleTabs.lastIndex)
                            selectedTab = visibleTabs[nextIndex]
                            true
                        }
                        Key.DirectionLeft -> {
                            val prevIndex = (selectedTabIndex - 1).coerceAtLeast(0)
                            selectedTab = visibleTabs[prevIndex]
                            true
                        }
                        Key.Tab -> {
                            if (selectedTab == InfoTab.DOWNLOAD) {
                                if (event.isShiftPressed) {
                                    linkFocusManager.focusPrevious()
                                } else {
                                    linkFocusManager.focusNext()
                                }
                                true
                            } else {
                                false
                            }
                        }
                        Key.I -> {
                            if (selectedTab == InfoTab.DOWNLOAD && !event.isCtrlPressed && !event.isAltPressed) {
                                selectedTab = InfoTab.INSTALLATION
                                true
                            } else {
                                false
                            }
                        }
                        Key.Comma -> {
                            if (!event.isCtrlPressed && !event.isAltPressed) {
                                triggerOpenSettings = true
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            val isMobileWeb = isMobileWebBrowser()
            val isLandscapeMobileWeb = shouldUseCompactInfoHeaderLayout(
                isMobileWeb = isMobileWeb,
                isLandscape = maxWidth > maxHeight
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLandscapeMobileWeb) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.back),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }

                // Settings button in top-right corner
                SettingsButton(
                    shortcutKey = ",",
                    triggerOpen = triggerOpenSettings,
                    onTriggerHandled = { triggerOpenSettings = false }
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacer to make room for settings button
                Spacer(modifier = Modifier.height(48.dp))
                
                // Tab selector - horizontally scrollable so tabs don't get compressed on mobile
                ScrollableTabRowWithHints(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    visibleTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    when (tab) {
                                        InfoTab.INSTALLATION -> stringResource(Res.string.info_tab_installation)
                                        InfoTab.HOW_TO_PLAY -> stringResource(Res.string.info_tab_how_to_play)
                                        InfoTab.AUDIO_LICENSES -> stringResource(Res.string.info_tab_audio_licenses)
                                        InfoTab.LICENSE -> stringResource(Res.string.info_tab_license)
                                        InfoTab.KEYBOARD_SHORTCUTS -> stringResource(Res.string.info_tab_keyboard_shortcuts)
                                        InfoTab.BACKEND -> stringResource(Res.string.info_tab_backend)
                                        InfoTab.FEEDBACK -> stringResource(Res.string.info_tab_feedback)
                                        InfoTab.EDITOR_HOWTO -> stringResource(Res.string.info_tab_editor_howto)
                                        InfoTab.DOWNLOAD -> stringResource(Res.string.info_tab_download)
                                    }
                                )
                            }
                        )
                    }
                }
                
                // Reset scroll position when tab changes
                LaunchedEffect(selectedTab) {
                    contentScrollState.scrollTo(0)
                }
                
                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        InfoTab.INSTALLATION -> InstallationInfo(scrollState = contentScrollState)
                        InfoTab.HOW_TO_PLAY -> HowToPlayContent(scrollState = contentScrollState)
                        InfoTab.AUDIO_LICENSES -> AudioLicensesInfo(scrollState = contentScrollState)
                        InfoTab.LICENSE -> LicenseInfo(scrollState = contentScrollState)
                        InfoTab.KEYBOARD_SHORTCUTS -> KeyboardShortcutsInfo(scrollState = contentScrollState)
                        InfoTab.BACKEND -> BackendInfo(scrollState = contentScrollState)
                        InfoTab.FEEDBACK -> FeedbackInfo(scrollState = contentScrollState)
                        InfoTab.EDITOR_HOWTO -> EditorHowToContent(scrollState = contentScrollState)
                        InfoTab.DOWNLOAD -> DownloadInfo(onNavigateToInstallation = { selectedTab = InfoTab.INSTALLATION }, scrollState = contentScrollState, linkFocusManager = linkFocusManager)
                    }
                }
                
                if (!isLandscapeMobileWeb) {
                    // Keyboard navigation hints
                    if (AppSettings.showButtonShortcutHints.value) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ShortcutKeyChip(text = "\u2191\u2193")
                                Text(
                                    stringResource(Res.string.keyboard_nav_scroll),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ShortcutKeyChip(text = "\u2190\u2192")
                                Text(
                                    stringResource(Res.string.keyboard_nav_switch_tab),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ShortcutKeyChip(text = ",")
                                Text(
                                    stringResource(Res.string.settings),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Back button
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(min = 200.dp)
                    ) {
                        Text(stringResource(Res.string.back))
                        if (AppSettings.showButtonShortcutHints.value) {
                            Spacer(modifier = Modifier.width(4.dp))
                            ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enum representing the different tabs in the info page
 */
enum class InfoTab {
    INSTALLATION,
    HOW_TO_PLAY,
    AUDIO_LICENSES,
    LICENSE,
    KEYBOARD_SHORTCUTS,
    BACKEND,
    FEEDBACK,
    EDITOR_HOWTO,
    DOWNLOAD
}

/**
 * Standalone feedback tab content: wraps the shared FeedbackFormContent in a scrollable column.
 */
@Composable
private fun FeedbackInfo(scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeedbackFormContent()
        Spacer(modifier = Modifier.height(8.dp))
    }
}
