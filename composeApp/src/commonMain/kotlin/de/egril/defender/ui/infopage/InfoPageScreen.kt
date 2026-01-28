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
import de.egril.defender.ui.settings.SettingsButton
import defender_of_egril.composeapp.generated.resources.*

/**
 * Main info page screen that combines installation info and audio licenses.
 * This screen is accessible via the info button on the main menu (web version only).
 */
@Composable
fun InfoPageScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(InfoTab.INSTALLATION) }
    
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
                // Tab selector
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = selectedTab == InfoTab.INSTALLATION,
                        onClick = { selectedTab = InfoTab.INSTALLATION },
                        text = { Text(stringResource(Res.string.info_tab_installation)) }
                    )
                    Tab(
                        selected = selectedTab == InfoTab.AUDIO_LICENSES,
                        onClick = { selectedTab = InfoTab.AUDIO_LICENSES },
                        text = { Text(stringResource(Res.string.info_tab_audio_licenses)) }
                    )
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
    AUDIO_LICENSES
}
