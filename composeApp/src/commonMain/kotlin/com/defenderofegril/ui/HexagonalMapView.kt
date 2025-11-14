package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.Position
import com.defenderofegril.utils.isPlatformMobile
import kotlin.math.sqrt

/**
 * Configuration for the hexagonal map view
 */
data class HexagonalMapConfig(
    val hexSize: Float = 40f,  // Radius of hexagon (center to corner)
    val enableKeyboardNavigation: Boolean = true,  // Enable arrow keys & WASD navigation
    val enablePanNavigation: Boolean = true,  // Enable mouse drag panning
    val enableBrushMode: Boolean = false,  // Enable brush painting mode (for editor)
    val enableZoomMode: Boolean = true,
    val keyboardPanSpeed: Float = 30f,  // Pixels to pan per key press
    val dragPanSensitivity: Float = 30f,  // Multiplier for drag pan sensitivity
    val minScale: Float = 0.5f,
    val maxScale: Float = 3.0f,
    val zoomDelta: Float = 0.1f  // Amount to zoom per button press
)

/**
 * Universal hexagonal map view with pan, zoom, and keyboard navigation support.
 * This component provides the core map viewing functionality that can be used
 * in both gameplay and editor contexts.
 *
 * @param gridWidth Width of the hexagonal grid
 * @param gridHeight Height of the hexagonal grid
 * @param config Configuration for map behavior
 * @param scale Current zoom scale (can be controlled externally)
 * @param offsetX Current horizontal pan offset (can be controlled externally)
 * @param offsetY Current vertical pan offset (can be controlled externally)
 * @param onScaleChange Callback when zoom scale changes
 * @param onOffsetChange Callback when pan offset changes
 * @param onActualContentSizeChange Callback when content size is measured
 * @param onBrushPaint Callback when brush painting a position (x, y in content coordinates). Only called if enableBrushMode is true.
 * @param modifier Modifier for the container
 * @param content The hexagonal grid content to display. Receives (hexWidth, hexHeight, verticalSpacing, onTilePositioned) where onTilePositioned should be called for each tile.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HexagonalMapView(
    gridWidth: Int,
    gridHeight: Int,
    config: HexagonalMapConfig = HexagonalMapConfig(),
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit,
    onActualContentSizeChange: (IntSize) -> Unit = {},
    onBrushPaint: ((Position) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (
        position: Position
    ) -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var actualContentSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }

    // Calculate hex dimensions for pointy-top hexagons
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = config.hexSize * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = config.hexSize * 2f    // Height of hexagon (point-to-point)
    val verticalSpacing = hexHeight * 0.75f  // For pointy-top hexagons
    
    // Helper function to constrain pan offsets
    fun constrainOffsets(newOffsetX: Float, newOffsetY: Float, currentScale: Float): Pair<Float, Float> {
        // If content size hasn't been measured yet, don't constrain
        if (actualContentSize.width == 0 || actualContentSize.height == 0) {
            return Pair(newOffsetX, newOffsetY)
        }
        
        val contentWidth = actualContentSize.width * currentScale
        val contentHeight = actualContentSize.height * currentScale

        val maxOffsetX = if (contentWidth > containerSize.width) {
            (contentWidth - containerSize.width) / 2
        } else {
            (containerSize.width * (currentScale - 1) / 2).coerceAtLeast(0f)
        }

        val maxOffsetY = if (contentHeight > containerSize.height) {
            (contentHeight - containerSize.height) / 2
        } else {
            (containerSize.height * (currentScale - 1) / 2).coerceAtLeast(0f)
        }

        return Pair(
            newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
            newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
        )
    }

    // Keyboard event handler
    val keyboardHandler: (KeyEvent) -> Boolean = { event ->
        if (config.enableKeyboardNavigation && event.type == KeyEventType.KeyDown) {
            var handled = false
            var newOffsetX = offsetX
            var newOffsetY = offsetY

            when (event.key) {
                // Arrow keys
                Key.DirectionUp -> {
                    newOffsetY += config.keyboardPanSpeed
                    handled = true
                }
                Key.DirectionDown -> {
                    newOffsetY -= config.keyboardPanSpeed
                    handled = true
                }
                Key.DirectionLeft -> {
                    newOffsetX += config.keyboardPanSpeed
                    handled = true
                }
                Key.DirectionRight -> {
                    newOffsetX -= config.keyboardPanSpeed
                    handled = true
                }
                // WASD keys
                Key.W -> {
                    newOffsetY += config.keyboardPanSpeed
                    handled = true
                }
                Key.S -> {
                    newOffsetY -= config.keyboardPanSpeed
                    handled = true
                }
                Key.A -> {
                    newOffsetX += config.keyboardPanSpeed
                    handled = true
                }
                Key.D -> {
                    newOffsetX -= config.keyboardPanSpeed
                    handled = true
                }
            }

            if (handled) {
                val (constrainedX, constrainedY) = constrainOffsets(newOffsetX, newOffsetY, scale)
                onOffsetChange(constrainedX, constrainedY)
            }
            handled
        } else {
            false
        }
    }

    LaunchedEffect(Unit) {
        if (config.enableKeyboardNavigation) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .then(
                if (config.enableKeyboardNavigation) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent(keyboardHandler)
                } else {
                    Modifier
                }
            )
            .then(
                if (config.enableZoomMode) {
                    Modifier.mouseWheelZoom(
                        containerSize = containerSize,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        onScaleChange = onScaleChange,
                        onOffsetChange = onOffsetChange
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (config.enablePanNavigation) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Apply zoom (for pinch gestures on mobile)
                            var newScale = scale
                            if (zoom != 1f) {
                                newScale = (scale * zoom).coerceIn(config.minScale, config.maxScale)
                                onScaleChange(newScale)
                            }

                            // Apply pan
                            val newOffsetX = offsetX + pan.x
                            val newOffsetY = offsetY + pan.y

                            // Constrain pan to keep content visible
                            val (constrainedX, constrainedY) = constrainOffsets(newOffsetX, newOffsetY, newScale)
                            onOffsetChange(constrainedX, constrainedY)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Map content with pan and zoom applied
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    // Measure with infinite constraints to prevent compression
                    val placeable = measurable.measure(
                        constraints.copy(
                            maxWidth = Constraints.Infinity,
                            maxHeight = Constraints.Infinity
                        )
                    )
                    // Capture the actual content size for pan calculations
                    actualContentSize = IntSize(placeable.width, placeable.height)
                    onActualContentSizeChange(actualContentSize)
                    // Report the actual size to parent (for proper container sizing)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)
        ) {
            for (y in 0 until gridHeight) {
                Row(
                    modifier = Modifier
                        .padding(
                            start = if (y % 2 == 1) (hexWidth * 0.42f).dp else 0.dp
                        )
                        .offset(y = (-(y - 1)).dp),
                    horizontalArrangement = Arrangement.spacedBy((-10).dp)
                ) {
                    for (x in 0 until gridWidth) {
                        val position = Position(x, y)
                        content(position)
                    }
                }
            }
        }
    }
}

@Composable
fun BaseGridCell(
    hexSize: androidx.compose.ui.unit.Dp,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = { }
) {
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3
    val hexHeight = hexSize.value * 2f

    Box(
        modifier = Modifier
            .width((hexWidth).dp)
            .height((hexHeight).dp)
            .clip(HexagonShape())
            .background(backgroundColor)
            .border(borderWidth, borderColor, HexagonShape())
            .clickable(onClick = onClick)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
