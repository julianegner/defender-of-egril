package de.egril.defender.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.egril.defender.model.AttackerType
import de.egril.defender.model.HexDirection
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides directional sprite painters for enemy units.
 *
 * ## Spritesheet convention
 * Each enemy type has a single spritesheet PNG stored at:
 *
 * `composeResources/files/sprites/{key}.png`
 *
 * where `{key}` is the lower-cased [AttackerType] name (see [spriteKey]).  The sheet is a single
 * horizontal strip of [DIRECTION_COUNT] equally-sized frames, one per hexagon direction, laid out
 * left-to-right in the [HexDirection] enum order: `E, NE, NW, W, SW, SE`.  The frame for a given
 * direction is therefore located at `frameWidth * direction.ordinal`.
 *
 * The provider degrades gracefully: it returns `null` when sprites are disabled in settings or when
 * the spritesheet for the requested enemy is not present, letting callers fall back to the drawn
 * vector icon.
 */
object EnemySpriteProvider {
    /** Number of directional frames expected in every spritesheet (one per hexagon side). */
    val DIRECTION_COUNT: Int = HexDirection.entries.size

    // Cache of decoded spritesheets keyed by sprite key. `null` means "already tried, not available".
    private val sheetCache = mutableMapOf<String, ImageBitmap?>()

    /**
     * Maps an [AttackerType] to the sprite file key (the PNG file name without extension).
     */
    fun spriteKey(type: AttackerType): String = type.name.lowercase()

    /**
     * Loads and decodes the spritesheet for the given key, caching the result (including misses).
     *
     * The bytes are looked up under `files/sprites/` (the cross-platform raw-resource location used
     * elsewhere in the app). `files/sprites/` is the correct long-term location. For backward
     * compatibility, the Gradle build rewrites any legacy prepared `drawable/sprites/` resources
     * into `files/sprites/` before Compose resource accessor generation runs.
     */
    private suspend fun loadSheet(key: String): ImageBitmap? {
        if (sheetCache.containsKey(key)) return sheetCache[key]

        val bitmap: ImageBitmap? =
            try {
                val bytes = Res.readBytes("files/sprites/$key.png")
                MapImageProvider.decodeImageBitmap(bytes)
            } catch (_: Exception) {
                null
            }
        sheetCache[key] = bitmap
        return bitmap
    }

    /**
     * Computes the source rectangle (offset + size) of the frame within a spritesheet of the given
     * dimensions for the requested [direction]. Exposed for testing.
     *
     * Returns the full-image bounds when the sheet is too narrow to contain [DIRECTION_COUNT]
     * frames, so a malformed sheet degrades to showing the whole image rather than crashing.
     */
    fun frameBounds(
        sheetWidth: Int,
        sheetHeight: Int,
        direction: HexDirection,
    ): Pair<IntOffset, IntSize> {
        val frameWidth = sheetWidth / DIRECTION_COUNT
        if (frameWidth <= 0) {
            return IntOffset.Zero to IntSize(sheetWidth, sheetHeight)
        }
        return IntOffset(frameWidth * direction.ordinal, 0) to IntSize(frameWidth, sheetHeight)
    }

    /**
     * Builds a [Painter] that renders only the frame of [sheet] for the given [direction].
     */
    private fun framePainter(
        sheet: ImageBitmap,
        direction: HexDirection,
    ): Painter {
        val (srcOffset, srcSize) = frameBounds(sheet.width, sheet.height, direction)
        return BitmapPainter(sheet, srcOffset = srcOffset, srcSize = srcSize)
    }

    /**
     * Composable that resolves the directional sprite [Painter] for an enemy [type].
     *
     * Returns `null` when enemy sprites are disabled or when no spritesheet is available for the
     * enemy, so callers can fall back to the drawn icon.
     */
    @Composable
    fun rememberEnemySpritePainter(
        type: AttackerType,
        direction: HexDirection,
    ): Painter? {
        val useSprites = AppSettings.useSprites.value
        val key = spriteKey(type)

        var sheet by remember(key, useSprites) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(key, useSprites) {
            sheet =
                if (useSprites) {
                    withContext(Dispatchers.Default) { loadSheet(key) }
                } else {
                    null
                }
        }

        val currentSheet = sheet
        return if (useSprites && currentSheet != null) {
            remember(currentSheet, direction) { framePainter(currentSheet, direction) }
        } else {
            null
        }
    }
}
