@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.editor.EditorHowToContent
import de.egril.defender.ui.isEditorAvailable
import de.egril.defender.ui.settings.SettingsButton
import defender_of_egril.composeapp.generated.resources.*

/**
 * Main info page screen that combines installation info, audio licenses, and backend info.
 * This screen is accessible via the info button on the main menu (all platforms).
 */
@Composable
fun InfoPageScreen(
    onBack: () -> Unit,
    initialTab: InfoTab = InfoTab.INSTALLATION
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    val visibleTabs = remember(isEditorAvailable()) {
        buildList {
            add(InfoTab.INSTALLATION)
            add(InfoTab.AUDIO_LICENSES)
            add(InfoTab.LICENSE)
            add(InfoTab.KEYBOARD_SHORTCUTS)
            add(InfoTab.BACKEND)
            if (isEditorAvailable()) add(InfoTab.EDITOR_HOWTO)
        }
    }

    // If the initial tab is not visible (e.g. EDITOR_HOWTO on mobile), fall back to INSTALLATION
    if (selectedTab !in visibleTabs) {
        selectedTab = InfoTab.INSTALLATION
    }

    val selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings button in top-right corner
            SettingsButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacer to make room for settings button
                Spacer(modifier = Modifier.height(48.dp))
                
                // Tab selector
                PrimaryTabRow(
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
                                        InfoTab.AUDIO_LICENSES -> stringResource(Res.string.info_tab_audio_licenses)
                                        InfoTab.LICENSE -> stringResource(Res.string.info_tab_license)
                                        InfoTab.KEYBOARD_SHORTCUTS -> stringResource(Res.string.info_tab_keyboard_shortcuts)
                                        InfoTab.BACKEND -> stringResource(Res.string.info_tab_backend)
                                        InfoTab.EDITOR_HOWTO -> stringResource(Res.string.info_tab_editor_howto)
                                    }
                                )
                            }
                        )
                    }
                }
                
                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        InfoTab.INSTALLATION -> InstallationInfo()
                        InfoTab.AUDIO_LICENSES -> AudioLicensesInfo()
                        InfoTab.LICENSE -> LicenseInfo()
                        InfoTab.KEYBOARD_SHORTCUTS -> KeyboardShortcutsInfo()
                        InfoTab.BACKEND -> BackendInfo()
                        InfoTab.EDITOR_HOWTO -> EditorHowToContent()
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
    AUDIO_LICENSES,
    LICENSE,
    KEYBOARD_SHORTCUTS,
    BACKEND,
    EDITOR_HOWTO
}
