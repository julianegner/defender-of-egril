package de.egril.defender.ui.gameplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.AlchemyAttackOverlay
import de.egril.defender.ui.animations.BallistaAttackOverlay
import de.egril.defender.ui.animations.BowAttackOverlay
import de.egril.defender.ui.animations.GarokkWarCryOverlay
import de.egril.defender.ui.animations.PikeAttackOverlay
import de.egril.defender.ui.animations.RocketAttackOverlay
import de.egril.defender.ui.animations.SkyIsFallingAnimation
import de.egril.defender.ui.animations.SnotlingCannonThrowOverlay
import de.egril.defender.ui.animations.SpearAttackOverlay
import de.egril.defender.ui.animations.WizardAttackOverlay
import de.egril.defender.ui.editor.map.MapControlState
import de.egril.defender.ui.editor.map.MapControls
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameGrid(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedAttackerId: Int? = null,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction? = null,
    selectedBarricadeAction: BarricadeAction? = null,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
    scrollToPosition: Position? = null,
    onScrollToPositionConsumed: (() -> Unit)? = null,
    isDemoMode: Boolean = false,
    demoHoveredPosition: Position? = null, // overrides the local hover in demo mode
    keyboardHoveredPosition: Position? = null, // overrides the local hover for keyboard build tile selection
    keyboardPlacementCursor: Position? = null, // keyboard cursor tile while placing a support object / targeting a spell
    selectedSupportObject: SupportObjectType? = null, // support object currently selected for placement (barricade/trap/magical trap)
    selectedSupportFief: de.egril.defender.model.FiefType? = null, // fief type currently selected for placement
    extraFocusTrigger: Int = 0,
    mapPanController: MapPanController? = null, // lets screen-level keys pan the map regardless of focus
) {
    // Establish a snapshot dependency on runtime map edits (sandbox tile painting) so the entire
    // grid recomposes and re-derives its tile sets from the updated level when a tile is repainted.
    @Suppress("UNUSED_VARIABLE")
    val mapEditVersion = gameState.mapEditVersion.value

    // State for pan and zoom
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialized by remember { mutableStateOf(false) }

    // State for hover position (for tower placement preview)
    var localHoveredPosition by remember { mutableStateOf<Position?>(null) }
    // In demo mode use the externally-driven hover; keyboard hover overrides local hover for build tile preview
    val hoveredPosition: Position? = if (isDemoMode) demoHoveredPosition else (keyboardHoveredPosition ?: localHoveredPosition)

    val hexSize = 40.dp // Radius of hexagon (center to corner)

    // Initialize viewport: zoom-to-fit in demo mode, show spawn points otherwise
    LaunchedEffect(containerSize, contentSize) {
        if (!isInitialized &&
            containerSize.width > 0 &&
            containerSize.height > 0 &&
            contentSize.width > 0 &&
            contentSize.height > 0
        ) {
            if (isDemoMode) {
                // Zoom to 100% fit-to-screen so the entire map fills the available space
                // (controls panel height is locked at its max so no layout jumps occur)
                val fitScaleX = containerSize.width.toFloat() / contentSize.width.toFloat()
                val fitScaleY = containerSize.height.toFloat() / contentSize.height.toFloat()
                scale = minOf(fitScaleX, fitScaleY).coerceAtLeast(0.2f)
                offsetX = 0f
                offsetY = 0f
            } else {
                // Show spawn points (upper left) instead of center
                val contentWidth = contentSize.width * scale
                val contentHeight = contentSize.height * scale

                if (contentWidth > containerSize.width) {
                    val maxOffsetX = (contentWidth - containerSize.width) / 2
                    offsetX = maxOffsetX
                }

                if (contentHeight > containerSize.height) {
                    val maxOffsetY = (contentHeight - containerSize.height) / 2
                    offsetY = maxOffsetY
                }
            }
            isInitialized = true
        }
    }

    // Scroll to position when requested (e.g. bomb explosion)
    LaunchedEffect(scrollToPosition) {
        if (scrollToPosition != null && containerSize.width > 0 && contentSize.width > 0) {
            // Calculate pixel position of the target hex
            val hexSizePx = hexSize.value
            val hexWidthPx = hexSizePx * kotlin.math.sqrt(3.0).toFloat()
            val hexHeightPx = hexSizePx * 2f
            val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING
            val rowSpacingPx = -hexHeightPx + hexHeightPx * 0.75f + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
            val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
            val col = scrollToPosition.x
            val row = scrollToPosition.y
            val oddRowOffset = if (row % 2 == 1) oddOffsetPx else 0f
            val cellCenterX = col * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset
            val cellCenterY = row * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f
            // Clamp offset so the cell is centered in the viewport
            val maxOffsetX = maxOf(0f, (contentSize.width * scale - containerSize.width) / 2f)
            val maxOffsetY = maxOf(0f, (contentSize.height * scale - containerSize.height) / 2f)
            val targetOffsetX = (contentSize.width * scale / 2f - cellCenterX * scale).coerceIn(-maxOffsetX, maxOffsetX)
            val targetOffsetY = (contentSize.height * scale / 2f - cellCenterY * scale).coerceIn(-maxOffsetY, maxOffsetY)
            offsetX = targetOffsetX
            offsetY = targetOffsetY
            onScrollToPositionConsumed?.invoke()
        }
    }

    DisposableEffect(mapPanController) {
        mapPanController?.handler = { dx, dy ->
            val (constrainedX, constrainedY) = constrainMapOffsets(offsetX + dx, offsetY + dy, scale, containerSize, contentSize)
            offsetX = constrainedX
            offsetY = constrainedY
        }
        onDispose { mapPanController?.handler = null }
    }

    val derived =
        rememberGameGridDerivedState(
            gameState = gameState,
            selectedDefenderType = selectedDefenderType,
            selectedDefenderId = selectedDefenderId,
            selectedAttackerId = selectedAttackerId,
            selectedTargetPosition = selectedTargetPosition,
            selectedSupportObject = selectedSupportObject,
            selectedSupportFief = selectedSupportFief,
            onCellClick = onCellClick,
            hoveredPosition = hoveredPosition,
            hexSize = hexSize,
        )

    with(derived) {
        Box(
            modifier =
                modifier
                    .onSizeChanged { containerSize = it },
        ) {
            if (useLevelMapImage && isLoadingMapImage) {
                LevelLoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                HexagonalMapView(
                    gridWidth = gameState.level.gridWidth,
                    gridHeight = gameState.level.gridHeight,
                    config =
                        HexagonalMapConfig(
                            hexSize = hexSize.value,
                            enableKeyboardNavigation = !isDemoMode, // Disable keyboard navigation in demo mode
                            enablePanNavigation = !isDemoMode, // Disable pan navigation in demo mode
                            panUpBinding = AppSettings.shortcutPanUp.value,
                            panDownBinding = AppSettings.shortcutPanDown.value,
                            panLeftBinding = AppSettings.shortcutPanLeft.value,
                            panRightBinding = AppSettings.shortcutPanRight.value,
                            minScale = if (isDemoMode) 0.2f else 0.5f, // Allow lower zoom in demo mode
                        ),
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    onScaleChange = { newScale -> scale = newScale },
                    onOffsetChange = { newOffsetX, newOffsetY ->
                        offsetX = newOffsetX
                        offsetY = newOffsetY
                    },
                    onActualContentSizeChange = { newContentSize ->
                        contentSize = newContentSize
                    },
                    focusTrigger = Pair(gameState.phase.value, extraFocusTrigger), // Request focus when game phase changes or after dialogs close
                    modifier = Modifier.fillMaxSize(),
                    backgroundContent =
                        if (hasMapImage || hasEventMapImages) {
                            { measuredContentSize ->
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val targetWidthPx = maxOf(hexMapSizePx.first, measuredContentSize.width)
                                val targetHeightPx = maxOf(hexMapSizePx.second, measuredContentSize.height)
                                with(density) {
                                    val imageModifier = Modifier.requiredWidth(targetWidthPx.toDp()).requiredHeight(targetHeightPx.toDp())
                                    androidx.compose.foundation.layout.Box(modifier = imageModifier) {
                                        if (hasMapImage && mapImagePainter != null) {
                                            androidx.compose.foundation.Image(
                                                painter = mapImagePainter,
                                                contentDescription = null,
                                                modifier = Modifier.matchParentSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                                            )
                                        }
                                        gameState.activeTileZoneIds.forEach { zoneId ->
                                            tileZoneImageState.painters[zoneId]?.let { painter ->
                                                androidx.compose.foundation.Image(
                                                    painter = painter,
                                                    contentDescription = null,
                                                    modifier = Modifier.matchParentSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                                                )
                                            }
                                        }
                                        movedZoneTiles.forEach { (zoneId, tiles) ->
                                            tileZoneImageState.painters[zoneId]?.let { painter ->
                                                MovedTileZoneOverlay(
                                                    painter = painter,
                                                    tiles = tiles,
                                                    modifier = Modifier.matchParentSize(),
                                                )
                                            }
                                        }
                                        if (hasEventMapImages) {
                                            EventMapTerrain(
                                                state = gameState,
                                                hexSize = hexSize.value,
                                                hasMapImage = hasMapImage,
                                                visibleZoneIdsByPosition = visibleZoneIdsByPosition,
                                                zoneImageIds = tileZoneImageState.painters.keys,
                                            )
                                        }
                                        EventMapImages(
                                            images = gameState.activeEventMapImages.toList(),
                                            hexSize = hexSize.value,
                                            modifier = Modifier.matchParentSize(),
                                        )
                                    }
                                }
                            }
                        } else {
                            null
                        },
                    overlayContent = { measuredContentSize ->
                        val ballistaEffects = gameState.ballistaAttackEffects.toList()
                        if (ballistaEffects.isNotEmpty()) {
                            BallistaAttackOverlay(
                                effects = ballistaEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val bowEffects = gameState.bowAttackEffects.toList()
                        if (bowEffects.isNotEmpty()) {
                            BowAttackOverlay(
                                effects = bowEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val spearEffects = gameState.spearAttackEffects.toList()
                        if (spearEffects.isNotEmpty()) {
                            SpearAttackOverlay(
                                effects = spearEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val pikeEffects = gameState.pikeAttackEffects.toList()
                        if (pikeEffects.isNotEmpty()) {
                            PikeAttackOverlay(
                                effects = pikeEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val wizardEffects = gameState.wizardAttackEffects.toList()
                        if (wizardEffects.isNotEmpty()) {
                            WizardAttackOverlay(
                                effects = wizardEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val alchemyEffects = gameState.alchemyAttackEffects.toList()
                        if (alchemyEffects.isNotEmpty()) {
                            AlchemyAttackOverlay(
                                effects = alchemyEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val rocketEffects = gameState.rocketAttackEffects.toList()
                        if (rocketEffects.isNotEmpty()) {
                            RocketAttackOverlay(
                                effects = rocketEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val snotlingCannonEffects = gameState.snotlingCannonThrowEffects.toList()
                        if (snotlingCannonEffects.isNotEmpty()) {
                            SnotlingCannonThrowOverlay(
                                effects = snotlingCannonEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        val garokkWarCryEffects = gameState.garokkWarCryEffects.toList()
                        if (garokkWarCryEffects.isNotEmpty()) {
                            GarokkWarCryOverlay(
                                effects = garokkWarCryEffects,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                                animate = AppSettings.enableAnimations.value,
                            )
                        }
                        // Full-map falling-meteor shower for the "Sky is Falling" support power.
                        SkyIsFallingAnimation(
                            triggerKey = gameState.skyIsFallingTrigger.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                        // Freya shield wall boundary: three curvy parallel lines (grey, blue, grey)
                        // drawn above the map following the outer edge of the shield wall formation.
                        if (freyaShieldWallArcs.isNotEmpty()) {
                            FreyaShieldWallMapOverlay(
                                shieldWallArcs = freyaShieldWallArcs,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                            )
                        }
                        // Lines between activated altars shown by level events (green orb to green orb).
                        AltarLinkMapOverlay(
                            links = gameState.activeAltarLinks.toList(),
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                        )
                        // Rift portals: blue entry rune and orange exit rune for each active portal.
                        val activePortals = gameState.activePortals.toList()
                        if (activePortals.isNotEmpty()) {
                            RiftPortalOverlay(
                                portals = activePortals,
                                hexSizeDp = hexSize.value,
                                contentSize = measuredContentSize,
                            )
                        }
                    },
                ) { position ->
                    // Pre-compute the two hover-position-dependent booleans per cell.
                    //
                    // Why Boolean parameters instead of Position:
                    //   When `hoveredPosition` changes (every mouse-move), passing it as a Position
                    //   parameter to GridCell causes ALL 6,400 cells to recompose because their
                    //   `hoveredPosition` parameter changed.  By pre-computing per-cell Booleans here
                    //   and passing those instead, only the 1-2 cells whose Boolean value actually
                    //   changed need to recompose — all others see the same `false` value as before
                    //   and are skipped by Compose's strong-skipping pass.
                    val isHovering = hoveredPosition == position
                    val isBuildingMode = selectedDefenderType != null

                    // Pre-compute isInPreviewRange here because it depends on hoveredPosition (which
                    // is no longer a GridCell parameter).  The expensive path runs only when
                    // hoveredPositionIsBuildableForGrid is true (i.e. a tower type is selected AND
                    // the cursor is over a valid placement tile), keeping the common case cheap.
                    val isInPreviewRange: Boolean =
                        if (
                            !isHovering && hoveredPositionIsBuildableForGrid && hoveredPosition != null && selectedDefenderType != null
                        ) {
                            val dist = hoveredPosition.distanceTo(position)
                            val minRange = selectedDefenderType.minRange
                            val maxRange =
                                selectedDefenderType.maxRange
                                    ?.let { minOf(selectedDefenderType.baseRange, it) }
                                    ?: selectedDefenderType.baseRange
                            val onPathHere = gameState.level.isOnPath(position)
                            val spawnHere = gameState.level.isSpawnPoint(position)
                            val riverHere = gameState.level.isRiverTile(position)
                            val traversable = onPathHere || spawnHere
                            val occupiable = onPathHere || spawnHere || riverHere
                            val areaAttack =
                                selectedDefenderType.attackType == AttackType.AREA ||
                                    selectedDefenderType.attackType == AttackType.LASTING
                            val validTarget = if (areaAttack) occupiable else traversable
                            dist >= minRange && dist <= maxRange && validTarget
                        } else {
                            false
                        }

                    // Per-cell booleans that replace selectedDefenderType: only ~640 buildable cells
                    // see a change when the buy button is clicked; the other ~5,760 stay at false → SKIPPED.

                    // showPlacementPreview: only the 1 hovered buildable cell can be true.
                    val showPlacementPreview =
                        isHovering &&
                            isBuildingMode &&
                            placementPreviewPositions.contains(position)

                    // Green-bordered buildable highlight — excludes the hovered cell (shows preview instead).
                    val isBuildableAndEmpty =
                        isBuildingMode &&
                            placementPreviewPositions.contains(position) &&
                            !showPlacementPreview

                    // Barricade tower-base highlight — only for barricade cells with HP >= 100 and no tower.
                    val canBeUsedAsTowerBase =
                        isBuildingMode &&
                            selectedDefenderType != DefenderType.DWARVEN_MINE &&
                            barricadeTowerBasePositions.contains(position) &&
                            !showPlacementPreview

                    // previewDefenderType: non-null only for the 1 cell showing the placement preview.
                    // All other cells get null → their parameter is null both before and after a buy-button
                    // click → those cells are not marked for recomposition due to this parameter.
                    val previewDefenderType: DefenderType? = if (showPlacementPreview) selectedDefenderType else null

                    // isKeyboardPlacementCursor: only the single tile under the keyboard placement/targeting
                    // cursor is true, so at most one cell recomposes when the cursor moves.
                    val isKeyboardPlacementCursor = keyboardPlacementCursor != null && keyboardPlacementCursor == position

                    // supportObjectPreviewType: non-null only for the single hovered tile that is a valid
                    // placement target for the currently selected support object. Mirrors previewDefenderType
                    // so hovering a support object over a valid tile shows the same ghost preview as the
                    // matching tower-placed barricade/trap.
                    val supportObjectPreviewType: SupportObjectType? =
                        if (isHovering && selectedSupportObject != null && supportObjectPlacementPositions.contains(position)) {
                            selectedSupportObject
                        } else {
                            null
                        }

                    // supportObjectPlacementHighlightType: non-null for every valid placement target of the
                    // currently selected support object, so all reachable tiles are highlighted the same way
                    // a tower highlights its trap/barricade placement range (but without the range limit).
                    val supportObjectPlacementHighlightType: SupportObjectType? =
                        if (selectedSupportObject != null && supportObjectPlacementPositions.contains(position)) {
                            selectedSupportObject
                        } else {
                            null
                        }

                    // True when a fief support token is selected and this tile is a valid placement target.
                    val supportFiefPlacementHighlight: Boolean =
                        selectedSupportFief != null && supportFiefPlacementPositions.contains(position)

                    // Memoize the event-handler lambdas so Compose's strong-skipping can work correctly.
                    //
                    // Without memoization, `{ onCellClick(position) }` and `{ localHoveredPosition = ... }`
                    // create NEW Function0/Function1 objects on every content-lambda invocation (6,400 per
                    // GameGrid recomposition).  Compose compares GridCell parameters by identity for
                    // function types, so new objects always differ → GridCell body runs for all 6,400 cells.
                    //
                    // With remember(position):
                    //   • The same lambda object is returned on every subsequent recomposition (same position).
                    //   • All other per-cell parameters are already stable (Booleans, enums, or stable data).
                    //   • Result: Compose correctly skips GridCells whose parameters didn't change.
                    //
                    // onCellClickState (rememberUpdatedState) gives a stable State<> reference captured once;
                    // reading .value inside the lambda avoids stale-closure issues if onCellClick ever changes.
                    val cellOnClick = remember(position) { { onCellClickState.value(position) } }
                    val cellOnHoverChange =
                        remember(position) {
                            { isHoveringChange: Boolean ->
                                localHoveredPosition = if (isHoveringChange) position else null
                            }
                        }

                    // A tile repainted at runtime (sandbox painting or an active tile zone, e.g. a tide) must
                    // show its new tile image even when the original map is rendered from a single
                    // pre-rendered image. For such tiles we force a non-transparent (opaque tile-image)
                    // background so the new type overlays the old map.
                    val sandboxPaintedType = gameState.paintedTileTypeAt(position)
                    val sandboxPaintedRiverTile = gameState.paintedRiverTileAt(position)
                    val visibleZoneId = visibleZoneIdsByPosition[position]
                    val zoneImageCoversTile = visibleZoneId != null && visibleZoneId in tileZoneImageState.painters

                    GridCell(
                        position = position,
                        gameState = gameState,
                        defender = defendersByPosition[position],
                        attacker = activeAttackersByPosition[position],
                        isDangerous = dangerousAttackerPositions.contains(position),
                        selectedDefender = selectedDefenderForGrid,
                        mapZoom = { scale },
                        isHovering = isHovering,
                        isInPreviewRange = isInPreviewRange,
                        showPlacementPreview = showPlacementPreview,
                        isBuildableAndEmpty = isBuildableAndEmpty,
                        canBeUsedAsTowerBase = canBeUsedAsTowerBase,
                        previewDefenderType = previewDefenderType,
                        isKeyboardPlacementCursor = isKeyboardPlacementCursor,
                        supportObjectPreviewType = supportObjectPreviewType,
                        supportObjectPlacementHighlightType = supportObjectPlacementHighlightType,
                        supportFiefPlacementHighlight = supportFiefPlacementHighlight,
                        // NOTE: the null guard on selectedDefenderId/selectedTargetId is critical for
                        // correctness AND performance.  Without it, `null?.id == null` evaluates to
                        // `null == null = true`, so every cell without a defender/attacker becomes
                        // "selected" (yellow borders + diagonal stripes on 6,000+ tiles) and any
                        // transition from null→id triggers a mass recomposition of all 6,000+ cells.
                        isDefenderSelected =
                            selectedDefenderId != null &&
                                defendersByPosition[position]?.id == selectedDefenderId,
                        isTargetSelected =
                            selectedTargetId != null &&
                                activeAttackersByPosition[position]?.id == selectedTargetId,
                        isInSelectedEnemyPath = selectedEnemyPathPositions.contains(position),
                        isInObsidianProtectorAura = position in obsidianProtectorAuraPositions,
                        selectedDefenderId = selectedDefenderId,
                        selectedMineAction = selectedMineAction,
                        selectedWizardAction = selectedWizardAction,
                        selectedBarricadeAction = selectedBarricadeAction,
                        targetCircleInfo = spellAreaCircleMap[position] ?: targetCircleMap[position] ?: placedBombCircleMap[position],
                        isSelectedAttackTarget = targetCircleMap[position] != null,
                        onClick = cellOnClick,
                        hexSize = hexSize,
                        onHoverChange = cellOnHoverChange,
                        healingEffect = healingEffectsByPosition[position],
                        damageEffect = damageEffectsByPosition[position],
                        deathEffect = defeatedEnemyEffectsByPosition[position],
                        coinGainEffect = coinGainEffectsByPosition[position],
                        towerAttackEffect = towerAttackEffectsByTargetPosition[position],
                        fieldEffect = fieldEffectsByPosition[position],
                        trap = trapsByPositionMap[position],
                        barricade = barricadesByPositionMap[position],
                        fief = fiefsByPositionMap[position],
                        mushroom = mushroomsByPositionMap[position],
                        gargoyle = inactiveGargoylesByPositionMap[position],
                        constructionCompleteEffect = constructionCompleteEffectsByPosition[position],
                        enemySpawnEffect = enemySpawnEffectsByPosition[position],
                        trapTriggerEffect = trapTriggerEffectsByPosition[position],
                        enemyMoveEffect = enemyMoveEffectsByPosition[position],
                        dragonLevelChangeEffect = dragonLevelChangeEffectsByPosition[position],
                        mineDigEffect = mineDigEffectsByPosition[position],
                        arrowAttackEffect = arrowAttackEffectsByAffectedPosition[position],
                        isArrowTargetTile = arrowAttackTargetPositions.contains(position),
                        isBallistaTargetTile = ballistaAttackTargetPositions.contains(position),
                        isBowTargetTile = bowAttackTargetPositions.contains(position),
                        isSpearTargetTile = spearAttackTargetPositions.contains(position),
                        isPikeTargetTile = pikeAttackTargetPositions.contains(position),
                        isWizardTargetTile = wizardAttackTargetPositions.contains(position),
                        isAlchemyTargetTile = alchemyAttackTargetPositions.contains(position),
                        isInWizardAttackArea = wizardAttackAreaPositions.contains(position),
                        isInAlchemyAttackArea = alchemyAttackAreaPositions.contains(position),
                        useTransparentBackground =
                            hasEventMapImages ||
                                (
                                    hasMapImage &&
                                        (
                                            sandboxPaintedType == null ||
                                                (zoneImageCoversTile && !(gameState.level.isSandbox && position in gameState.sandboxPaintedTiles))
                                        )
                                ),
                        terrainIsSeparateLayer = hasEventMapImages,
                        sandboxPaintedType = sandboxPaintedType,
                        sandboxPaintedRiverTile = sandboxPaintedRiverTile,
                    )
                }

                MapControls(
                    mapControlState =
                        MapControlState(
                            zoomLevel = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                        ),
                    onStateChange = { newState ->
                        val newScale = newState.zoomLevel
                        val (constrainedX, constrainedY) =
                            constrainMapOffsets(
                                newState.offsetX,
                                newState.offsetY,
                                newScale,
                                containerSize,
                                contentSize,
                            )
                        scale = newScale
                        offsetX = constrainedX
                        offsetY = constrainedY
                    },
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        if (AppSettings.showMapSizeOverlay.value && contentSize.width > 0 && contentSize.height > 0) {
                            val contentWidthPx = contentSize.width
                            val contentHeightPx = contentSize.height
                            val realWidthPx = hexMapSizePx.first
                            val realHeightPx = hexMapSizePx.second
                            val viewportWidthPx = containerSize.width
                            val viewportHeightPx = containerSize.height
                            val contentWidthDp = with(density) { contentWidthPx.toDp() }
                            val contentHeightDp = with(density) { contentHeightPx.toDp() }
                            val realWidthDp = with(density) { realWidthPx.toDp() }
                            val realHeightDp = with(density) { realHeightPx.toDp() }
                            val viewportWidthDp = with(density) { viewportWidthPx.toDp() }
                            val viewportHeightDp = with(density) { viewportHeightPx.toDp() }
                            Surface(
                                tonalElevation = 2.dp,
                                shadowElevation = 4.dp,
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.debug_map_size_overlay,
                                            realWidthDp.value.roundToInt(),
                                            realHeightDp.value.roundToInt(),
                                            realWidthPx,
                                            realHeightPx,
                                            contentWidthDp.value.roundToInt(),
                                            contentHeightDp.value.roundToInt(),
                                            contentWidthPx,
                                            contentHeightPx,
                                            viewportWidthDp.value.roundToInt(),
                                            viewportHeightDp.value.roundToInt(),
                                            viewportWidthPx,
                                            viewportHeightPx,
                                            offsetX.roundToInt(),
                                            offsetY.roundToInt(),
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        // Minimap
                        Box(
                            modifier = Modifier.size(120.dp),
                        ) {
                            HexagonMinimap(
                                level = gameState.level,
                                config =
                                    MinimapConfig(
                                        showSpawnPoints = true,
                                        showTarget = true,
                                        showTowers = true,
                                        showEnemies = true,
                                        showViewport = true,
                                        minimapSizeDp = 120f,
                                    ),
                                gameState = gameState,
                                scale = scale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                containerSize = containerSize,
                                contentSize = contentSize,
                                modifier = Modifier.fillMaxSize(),
                                onViewportDrag = { newOffsetX, newOffsetY ->
                                    offsetX = newOffsetX
                                    offsetY = newOffsetY
                                },
                            )
                        }
                    }
                }
            } // end else !isLoadingMapImage
        }
    }
}
