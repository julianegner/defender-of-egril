package de.egril.defender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Control pad with 4 directional buttons arranged in a circle
 * Divided into quadrants by perpendicular lines through the center
 * Supports continuous movement when buttons are held down
 */
@Composable
fun ControlPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        // Up button (top quadrant)
        DirectionalButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            onAction = onUp,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Down button (bottom quadrant)
        DirectionalButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            onAction = onDown,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Left button (left quadrant)
        DirectionalButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Left",
            onAction = onLeft,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        // Right button (right quadrant)
        DirectionalButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Right",
            onAction = onRight,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        
        // Center dot/indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
    }
}

/**
 * Helper composable for directional buttons with continuous action on hold
 */
@Composable
private fun DirectionalButton(
    icon: ImageVector,
    contentDescription: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    ContinuousActionButton(
        onAction = onAction,
        initialDelay = 300,
        repeatDelay = 50,
        modifier = modifier.size(60.dp, 60.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Zoom controls with + and - buttons stacked vertically
 * Supports continuous zoom when buttons are held down
 */
@Composable
fun ZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .width(60.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom in button (+)
        ZoomButton(
            icon = Icons.Default.Add,
            contentDescription = "Zoom In",
            onAction = onZoomIn
        )
        
        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
        
        // Zoom out button (-)
        ZoomButton(
            icon = Icons.Default.Remove,
            contentDescription = "Zoom Out",
            onAction = onZoomOut
        )
    }
}

/**
 * Helper composable for zoom buttons with continuous action on hold
 */
@Composable
private fun ZoomButton(
    icon: ImageVector,
    contentDescription: String,
    onAction: () -> Unit
) {
    ContinuousActionButton(
        onAction = onAction,
        initialDelay = 300,
        repeatDelay = 100,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Reusable button component that supports continuous action when held down.
 * Executes the action immediately on press, then after an initial delay,
 * repeatedly executes the action at a specified interval until released.
 *
 * @param onAction Callback to execute on press and repeatedly while held
 * @param initialDelay Delay in milliseconds before starting repeated actions (default: 300ms)
 * @param repeatDelay Delay in milliseconds between repeated actions (default: 100ms)
 * @param modifier Modifier for the button container
 * @param content Content to display in the button
 */
@Composable
private fun ContinuousActionButton(
    onAction: () -> Unit,
    initialDelay: Long = 300,
    repeatDelay: Long = 100,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var pressJob by remember { mutableStateOf<Job?>(null) }
    // Use rememberUpdatedState to always get the latest callback without recreating the gesture handler
    val currentOnAction by rememberUpdatedState(onAction)
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // Start continuous action
                        pressJob = coroutineScope.launch {
                            currentOnAction() // First action immediately
                            delay(initialDelay) // Initial delay before repeat
                            while (true) {
                                currentOnAction()
                                delay(repeatDelay) // Repeat delay
                            }
                        }
                        tryAwaitRelease()
                        pressJob?.cancel()
                        pressJob = null
                    }
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}
