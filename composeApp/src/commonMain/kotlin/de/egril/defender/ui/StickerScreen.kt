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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for displaying sticker merchandise preview.
 * Reachable with the "sticker" cheat code.
 * 
 * Contains three tabs:
 * Tab 1: Original sticker with ApplicationBanner, tagline and URL
 * Tab 2: Cycling symbol sticker with "Defender of Egril" text and URL
 * Tab 3: QR code sticker with URL title below
 */
@Composable
fun StickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

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
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(Res.string.sticker_tab_original)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(Res.string.sticker_tab_symbols)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(stringResource(Res.string.sticker_tab_qr_code)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> StickerOriginalTab()
                    1 -> StickerSymbolsTab()
                    2 -> StickerQrCodeTab()
                }
            }
        }
    }
}

/**
 * Tab 1: Original sticker content with ApplicationBanner, tagline and URL.
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
 * Describes a single frame in the cycling symbols animation.
 */
private sealed interface SymbolFrame {
    /** The three banner enemies (Goblin, Ork, EvilWizard) together */
    object BannerEnemies : SymbolFrame
    /** The two banner towers (Bow, Wizard) */
    object BannerTowers : SymbolFrame
    /** A single enemy type with outline */
    data class SingleEnemy(val type: AttackerType) : SymbolFrame
}

private val symbolCycleFrames: List<SymbolFrame> = buildList {
    add(SymbolFrame.BannerEnemies)
    add(SymbolFrame.BannerTowers)
    AttackerType.entries.forEach { add(SymbolFrame.SingleEnemy(it)) }
}

/**
 * Tab 2: Cycling symbol sticker.
 * Shows a rotating symbol at the top, followed by "Defender of Egril" text and the URL.
 */
@Composable
private fun StickerSymbolsTab() {
    var frameIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000L)
            frameIndex = (frameIndex + 1) % symbolCycleFrames.size
        }
    }

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
        // Cycling symbol canvas
        val canvasSize = if (isPlatformMobile) 160.dp else 200.dp
        Box(
            modifier = Modifier
                .size(canvasSize)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val iconSize = minOf(size.width, size.height)

                when (val frame = symbolCycleFrames[frameIndex]) {
                    is SymbolFrame.BannerEnemies -> {
                        // Render three banner enemies like ApplicationBanner
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
                    is SymbolFrame.BannerTowers -> {
                        // Render two banner towers like ApplicationBanner
                        val bowOffsetX = if (isPlatformMobile) 40f else -30f
                        val bowOffsetY = if (isPlatformMobile) 0f else -20f
                        val wizardTowerOffsetX = if (isPlatformMobile) 80f else 30f
                        val wizardTowerOffsetY = if (isPlatformMobile) 30f else 20f
                        drawTower(DefenderType.BOW_TOWER, centerX + bowOffsetX, centerY + bowOffsetY, iconSize, lineColor)
                        val wizardCenterX = centerX + wizardTowerOffsetX
                        val wizardCenterY = centerY + wizardTowerOffsetY
                        // Draw background mask for wizard tower (same logic as ApplicationBanner)
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
                    is SymbolFrame.SingleEnemy -> {
                        val s = iconSize * 0.75f
                        when (frame.type) {
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

        // "Defender of" in Great Vibes font
        Text(
            text = "Defender of",
            fontSize = 32.sp,
            fontFamily = greatVibesFont,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground
        )
        // "Egril" in larger Great Vibes font
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
 * Tab 3: QR code sticker.
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

