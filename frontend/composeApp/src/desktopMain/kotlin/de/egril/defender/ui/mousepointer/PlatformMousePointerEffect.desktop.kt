package de.egril.defender.ui.mousepointer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.MousePointerDirection
import de.egril.defender.ui.settings.MousePointerSize
import de.egril.defender.ui.settings.MousePointerSource
import defender_of_egril.composeapp.generated.resources.Res
import java.awt.Component
import java.awt.Cursor
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.WeakHashMap
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import javax.swing.Timer
import org.jetbrains.compose.resources.ExperimentalResourceApi

private enum class ManagedPointerRole(
    val awtType: Int,
    val hotspotX: Int,
    val hotspotY: Int,
) {
    DEFAULT(Cursor.DEFAULT_CURSOR, 148, 12),
    TEXT(Cursor.TEXT_CURSOR, 132, 52),
    HAND(Cursor.HAND_CURSOR, 148, 12),
    ;

    companion object {
        fun fromCursorType(type: Int): ManagedPointerRole? =
            entries.firstOrNull { it.awtType == type }
    }
}

private const val GAME_POINTER_BASE_WIDTH = 160
private const val TEXT_POINTER_BASE_WIDTH = GAME_POINTER_BASE_WIDTH * 0.8f

private object DesktopMousePointerController {
    private val appliedRoles = WeakHashMap<Component, ManagedPointerRole>()
    private val originalCursors = WeakHashMap<Component, Cursor?>()
    private var currentSource: MousePointerSource = MousePointerSource.DEFAULT
    private var currentSize: MousePointerSize = MousePointerSize.DEFAULT
    private var currentDirection: MousePointerDirection = MousePointerDirection.DEFAULT
    private var currentBrightness: Float = 0f
    private val cursorCache = mutableMapOf<ManagedPointerRole, Cursor>()
    private var cursorBytes = emptyMap<ManagedPointerRole, ByteArray>()
    private var timer: Timer? = null
    private var applying = false

    fun setCursorBytes(bytes: Map<ManagedPointerRole, ByteArray>) {
        runOnEdt {
            cursorBytes = bytes
            cursorCache.clear()
        }
    }

    fun ensureStarted() {
        check(SwingUtilities.isEventDispatchThread())
        if (timer != null) return
        timer =
            Timer(120) {
                Window.getWindows()
                    .filter { it.isShowing }
                    .forEach { updateWindowTree(it) }
            }.also {
                it.isRepeats = true
                it.start()
            }
    }

    fun stop() {
        runOnEdt {
            timer?.stop()
            timer = null
            Window.getWindows()
                .filter { it.isShowing }
                .forEach { restoreWindowTree(it) }
        }
    }

    fun updateSettings(
        source: MousePointerSource,
        size: MousePointerSize,
        direction: MousePointerDirection,
        brightness: Float,
    ) {
        runOnEdt {
            val changed =
                source != currentSource ||
                    size != currentSize ||
                    direction != currentDirection ||
                    brightness != currentBrightness
            currentSource = source
            currentSize = size
            currentDirection = direction
            currentBrightness = brightness
            if (changed) {
                cursorCache.clear()
            }
            if (source == MousePointerSource.GAME) {
                ensureStarted()
            } else {
                timer?.stop()
                timer = null
                Window.getWindows()
                    .filter { it.isShowing }
                    .forEach { restoreWindowTree(it) }
            }
            Window.getWindows()
                .filter { it.isShowing }
                .forEach { updateWindowTree(it) }
        }
    }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }

    private fun updateWindowTree(window: Window) {
        applyComponent(window)
        window.components.forEach { updateComponentTree(it) }
    }

    private fun updateComponentTree(component: Component) {
        applyComponent(component)
        if (component is java.awt.Container) {
            component.components.forEach { updateComponentTree(it) }
        }
    }

    private fun applyComponent(component: Component) {
        if (applying) return
        val appliedRole = appliedRoles[component]
        val currentCursor = component.cursor ?: Cursor.getDefaultCursor()
        if (currentSource == MousePointerSource.SYSTEM) {
            if (appliedRole != null) {
                val originalCursor = originalCursors.remove(component)
                applying = true
                try {
                    component.cursor = originalCursor
                } finally {
                    applying = false
                }
                appliedRoles.remove(component)
            }
            return
        }

        if (currentCursor.type == Cursor.CUSTOM_CURSOR) {
            return
        }

        val role = ManagedPointerRole.fromCursorType(currentCursor.type) ?: run {
            if (appliedRole != null) {
                restoreManagedComponent(component)
            }
            return
        }

        val desired = cursorFor(role) ?: return
        if (currentCursor !== desired || appliedRole != role) {
            if (appliedRole == null && !originalCursors.containsKey(component)) {
                originalCursors[component] = component.cursor
            }
            applying = true
            try {
                component.cursor = desired
            } finally {
                applying = false
            }
            appliedRoles[component] = role
        }
    }

    private fun restoreManagedComponent(component: Component) {
        val originalCursor = originalCursors.remove(component)
        appliedRoles.remove(component)
        applying = true
        try {
            component.cursor = originalCursor
        } finally {
            applying = false
        }
    }

    private fun restoreWindowTree(window: Window) {
        restoreComponent(window)
        window.components.forEach { restoreComponentTree(it) }
    }

    private fun restoreComponentTree(component: Component) {
        restoreComponent(component)
        if (component is java.awt.Container) {
            component.components.forEach { restoreComponentTree(it) }
        }
    }

    private fun restoreComponent(component: Component) {
        if (appliedRoles[component] == null) return
        restoreManagedComponent(component)
    }

    private fun cursorFor(role: ManagedPointerRole): Cursor? =
        cursorCache.getOrPut(role) {
            createCursor(role) ?: Cursor.getPredefinedCursor(role.awtType)
        }

    private fun createCursor(role: ManagedPointerRole): Cursor? {
        val bytes = cursorBytes[role] ?: return null
        val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
        val transformed = transformCursorImage(image, role)
        val toolkit = Toolkit.getDefaultToolkit()
        val bestSize = toolkit.getBestCursorSize(transformed.width, transformed.height)
        if (bestSize.width == 0 || bestSize.height == 0) return null
        val hotspotScaleX = transformed.width.toFloat() / image.width.toFloat()
        val hotspotScaleY = transformed.height.toFloat() / image.height.toFloat()
        val sourceHotspotX =
            when (role) {
                ManagedPointerRole.DEFAULT,
                ManagedPointerRole.HAND,
                -> if (currentDirection == MousePointerDirection.RIGHT) 306 else 771
                ManagedPointerRole.TEXT -> 540
            }
        val sourceHotspotY =
            when (role) {
                ManagedPointerRole.DEFAULT,
                ManagedPointerRole.HAND,
                -> 140
                ManagedPointerRole.TEXT -> 294
            }
        val hotspotX = (sourceHotspotX * hotspotScaleX).toInt().coerceIn(0, transformed.width - 1)
        val hotspotY = (sourceHotspotY * hotspotScaleY).toInt().coerceIn(0, transformed.height - 1)
        return toolkit.createCustomCursor(transformed, java.awt.Point(hotspotX, hotspotY), "defender-${role.name.lowercase()}")
    }

    private fun transformCursorImage(
        image: BufferedImage,
        role: ManagedPointerRole,
    ): BufferedImage {
        val brightnessAdjusted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                brightnessAdjusted.setRGB(x, y, adjustPixel(image.getRGB(x, y)))
            }
        }

        val baseWidth =
            if (role == ManagedPointerRole.TEXT) {
                TEXT_POINTER_BASE_WIDTH
            } else {
                GAME_POINTER_BASE_WIDTH.toFloat()
            }
        val baseScale = baseWidth / brightnessAdjusted.width
        val scaledWidth = (brightnessAdjusted.width * baseScale * currentSize.scale).toInt().coerceAtLeast(16)
        val scaledHeight = (brightnessAdjusted.height * baseScale * currentSize.scale).toInt().coerceAtLeast(16)
        val scaled = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics: Graphics2D = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(brightnessAdjusted, 0, 0, scaledWidth, scaledHeight, null)
        graphics.dispose()
        return scaled
    }

    private fun adjustPixel(argb: Int): Int {
        val alpha = argb ushr 24 and 0xFF
        if (alpha == 0) return argb
        val red = argb ushr 16 and 0xFF
        val green = argb ushr 8 and 0xFF
        val blue = argb and 0xFF
        if (red < 120 || green < 70 || blue < 50 || red < green || green < blue) {
            return argb
        }
        val factor = 1f + currentBrightness
        val adjustedRed = (red * factor).toInt().coerceIn(0, 255)
        val adjustedGreen = (green * factor).toInt().coerceIn(0, 255)
        val adjustedBlue = (blue * factor).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (adjustedRed shl 16) or (adjustedGreen shl 8) or adjustedBlue
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun PlatformMousePointerEffect() {
    val source by AppSettings.mousePointerSource
    val size by AppSettings.mousePointerSize
    val direction by AppSettings.mousePointerDirection
    val brightness by AppSettings.mousePointerSkinBrightness
    val pointerBytes by produceState<Map<ManagedPointerRole, ByteArray>?>(null, direction) {
        val handBytes =
            Res.readBytes(
                if (direction == MousePointerDirection.RIGHT) {
                    "drawable/mouse_pointer_hand_right.png"
                } else {
                    "drawable/mouse_pointer_hand_left.png"
                },
            )
        val textBytes = Res.readBytes("drawable/mouse_pointer_text.png")
        value =
            mapOf(
                ManagedPointerRole.DEFAULT to handBytes,
                ManagedPointerRole.HAND to handBytes,
                ManagedPointerRole.TEXT to textBytes,
            )
    }

    LaunchedEffect(pointerBytes, source, size, direction, brightness) {
        pointerBytes?.let { DesktopMousePointerController.setCursorBytes(it) }
        DesktopMousePointerController.updateSettings(source, size, direction, brightness)
    }

    DisposableEffect(Unit) {
        onDispose {
            DesktopMousePointerController.stop()
        }
    }
}
