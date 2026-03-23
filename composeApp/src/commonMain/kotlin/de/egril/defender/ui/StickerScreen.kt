package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.icon.enemy.*
import de.egril.defender.ui.icon.defender.*
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for displaying sticker merchandise preview.
 * Reachable with the "sticker" cheat code.
 *
 * Contains multiple tabs:
 * Tab 0: Original sticker with ApplicationBanner, tagline and URL
 * Tab 1: Banner enemies sticker (Goblin, Ork, EvilWizard group with outline)
 * Tab 2: Banner towers sticker (Bow, Wizard group)
 * Tabs 3-14: One tab per enemy type (each with outline), following AttackerType order
 * Last tab: QR code sticker with URL title below
 */
@Composable
fun StickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Build the ordered list of symbol tabs (group tabs first, then individual enemy tabs)
    val symbolTabs: List<SymbolTab> = remember {
        buildList {
            add(SymbolTab.BannerEnemies)
            add(SymbolTab.BannerTowers)
            AttackerType.entries.forEach { add(SymbolTab.SingleEnemy(it)) }
        }
    }

    // Total tabs = original + symbolTabs + QR code
    val totalTabs = 1 + symbolTabs.size + 1
    val qrCodeTabIndex = totalTabs - 1

    Surface(
        modifier = modifier.fillMaxSize(),
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

            // Back button
            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(200.dp)
                    .height(50.dp)
                    .padding(end = 80.dp, top = 8.dp)
            ) {
                Text(stringResource(Res.string.back))
            }

            // Main content with tabs
            Column(
                modifier = Modifier
                    .padding(top = 70.dp)
                    .fillMaxSize()
            ) {
                // Use ScrollableTabRow because there are many tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tab 0: original sticker
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(Res.string.sticker_tab_original)) }
                    )
                    // Tabs 1..(1+symbolTabs.size-1): symbol tabs
                    symbolTabs.forEachIndexed { idx, tab ->
                        val tabIndex = 1 + idx
                        Tab(
                            selected = selectedTab == tabIndex,
                            onClick = { selectedTab = tabIndex },
                            text = { Text(tab.label()) }
                        )
                    }
                    // Last tab: QR code
                    Tab(
                        selected = selectedTab == qrCodeTabIndex,
                        onClick = { selectedTab = qrCodeTabIndex },
                        text = { Text(stringResource(Res.string.sticker_tab_qr_code)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    selectedTab == 0 -> StickerOriginalTab()
                    selectedTab == qrCodeTabIndex -> StickerQrCodeTab()
                    else -> {
                        val symbolTabIndex = selectedTab - 1
                        if (symbolTabIndex in symbolTabs.indices) {
                            StickerSymbolTab(symbolTabs[symbolTabIndex])
                        }
                    }
                }
            }
        }
    }
}

/**
 * Describes one of the symbol sticker designs.
 */
private sealed interface SymbolTab {
    /** The three banner enemies (Goblin, Ork, EvilWizard) together */
    data object BannerEnemies : SymbolTab
    /** The two banner towers (Bow, Wizard) */
    data object BannerTowers : SymbolTab
    /** A single enemy type with outline */
    data class SingleEnemy(val type: AttackerType) : SymbolTab
}

/**
 * Returns the tab label for this [SymbolTab], using localized strings.
 */
@Composable
private fun SymbolTab.label(): String = when (this) {
    is SymbolTab.BannerEnemies -> stringResource(Res.string.sticker_tab_enemies)
    is SymbolTab.BannerTowers -> stringResource(Res.string.sticker_tab_towers)
    is SymbolTab.SingleEnemy -> type.getLocalizedName()
}

/**
 * Tab 0: Original sticker content with ApplicationBanner, tagline and URL.
 */
@Composable
private fun StickerOriginalTab() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ApplicationBanner()

            Row {
                Text(
                    stringResource(Res.string.game_sticker_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                )
            }
            Row {
                Text(
                    "defender.egril.de",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

/**
 * A symbol sticker tab showing a static symbol at the top, followed by
 * "Defender of Egril" text (Great Vibes font) and the URL.
 */
@Composable
private fun StickerSymbolTab(tab: SymbolTab) {
    val lineColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val outlineColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black
    }
    val greatVibesFont = FontFamily(Font(Res.font.greatvibes_regular))

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val canvasSize = if (isPlatformMobile) 160.dp else 200.dp
        Box(modifier = Modifier.size(canvasSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val iconSize = minOf(size.width, size.height)

                when (tab) {
                    is SymbolTab.BannerEnemies -> {
                        val goblinOffsetX = if (isPlatformMobile) 15f else 20f
                        val goblinOffsetY = if (isPlatformMobile) -15f else -20f
                        val orkOffsetX = if (isPlatformMobile) -50f else 0f
                        val orkOffsetY = if (isPlatformMobile) 20f else -10f
                        val wizardOffsetX = if (isPlatformMobile) -100f else -20f
                        val wizardOffsetY = if (isPlatformMobile) 60f else 0f
                        drawGoblinSymbol(centerX + goblinOffsetX, centerY + goblinOffsetY, iconSize * 0.7f, outlineColor)
                        drawOrkSymbol(centerX + orkOffsetX, centerY + orkOffsetY, iconSize * 0.7f, outlineColor)
                        drawEvilWizardSymbol(centerX + wizardOffsetX, centerY + wizardOffsetY, iconSize * 0.7f, outlineColor)
                    }
                    is SymbolTab.BannerTowers -> {
                        val bowOffsetX = if (isPlatformMobile) 40f else -30f
                        val bowOffsetY = if (isPlatformMobile) 0f else -20f
                        val wizardTowerOffsetX = if (isPlatformMobile) 80f else 30f
                        val wizardTowerOffsetY = if (isPlatformMobile) 30f else 20f
                        drawTower(DefenderType.BOW_TOWER, centerX + bowOffsetX, centerY + bowOffsetY, iconSize, lineColor)
                        val wizardCenterX = centerX + wizardTowerOffsetX
                        val wizardCenterY = centerY + wizardTowerOffsetY
                        val wizardBaseSize = iconSize * 0.8f
                        val topWidth = wizardBaseSize * 0.4f
                        val bottomWidth = wizardBaseSize * 0.6f
                        val towerHeight = wizardBaseSize * 0.6f
                        val top = wizardCenterY - towerHeight / 2
                        val bottom = wizardCenterY + towerHeight / 2
                        val trapezoid = Path().apply {
                            moveTo(wizardCenterX - bottomWidth / 2, bottom)
                            lineTo(wizardCenterX + bottomWidth / 2, bottom)
                            lineTo(wizardCenterX + topWidth / 2, top)
                            lineTo(wizardCenterX - topWidth / 2, top)
                            close()
                        }
                        drawPath(trapezoid, backgroundColor)
                        val battlement = wizardBaseSize * 0.08f
                        for (i in 0..2) {
                            val x = wizardCenterX - topWidth / 2 + (topWidth / 3) * i
                            drawRect(
                                color = backgroundColor,
                                topLeft = Offset(x, top - battlement),
                                size = androidx.compose.ui.geometry.Size(battlement, battlement)
                            )
                        }
                        drawTower(DefenderType.WIZARD_TOWER, wizardCenterX, wizardCenterY, iconSize, lineColor)
                    }
                    is SymbolTab.SingleEnemy -> {
                        val s = iconSize * 0.75f
                        when (tab.type) {
                            AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.ORK -> drawOrkSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, s * 1.05f, outlineColor)
                            AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, s * 1.05f, outlineColor)
                            AttackerType.EVIL_MAGE -> drawEvilMageSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, s, outlineColor)
                            AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, s * 1.1f, outlineColor)
                            AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, s * 1.2f, outlineColor)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Defender of",
            fontSize = 32.sp,
            fontFamily = greatVibesFont,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Egril",
            fontSize = 56.sp,
            fontFamily = greatVibesFont,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "defender.egril.de",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Last tab: QR code sticker.
 * Shows a QR code for "defender.egril.de" with the URL as title below.
 */
@Composable
private fun StickerQrCodeTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val qrSize = if (isPlatformMobile) 200.dp else 280.dp
        Image(
            painter = painterResource(Res.drawable.qr_code_defender),
            contentDescription = "QR Code for defender.egril.de",
            modifier = Modifier.size(qrSize)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "defender.egril.de",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

