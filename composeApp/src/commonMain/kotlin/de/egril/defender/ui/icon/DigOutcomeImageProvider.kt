package de.egril.defender.ui.icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.egril.defender.model.DigOutcome
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image

/**
 * Provides dig outcome images from simple or enhanced subdirectories
 */
object DigOutcomeImageProvider {
    
    /**
     * Gets the appropriate dig outcome image based on settings
     */
    @Composable
    fun getDigOutcomeImage(outcome: DigOutcome): ImageBitmap? {
        val useEnhanced = AppSettings.useEnhancedDigImages.value
        val folder = if (useEnhanced) "enhanced" else "simple"
        
        return remember(outcome, useEnhanced) {
            loadDigOutcomeImage(outcome, folder)
        }
    }
    
    /**
     * Loads a dig outcome image from drawable/{folder}/dig_outcome/
     */
    private fun loadDigOutcomeImage(outcome: DigOutcome, folder: String): ImageBitmap? {
        val filename = when (outcome) {
            DigOutcome.NOTHING -> "rubble.png"
            DigOutcome.BRASS -> "brass.png"
            DigOutcome.SILVER -> "silver.png"
            DigOutcome.GOLD -> "gold.png"
            DigOutcome.GEMS -> {
                // Randomly select gem color
                when (kotlin.random.Random.Default.nextInt(3)) {
                    0 -> "gem_red.png"
                    1 -> "gem_green.png"
                    else -> "gem_blue.png"
                }
            }
            DigOutcome.DIAMOND -> "diamond.png"
            DigOutcome.DRAGON -> "dragon.png"
        }
        
        return try {
            runBlocking {
                val bytes = Res.readBytes("drawable/$folder/dig_outcome/$filename")
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        } catch (e: Exception) {
            // Failed to load, return null
            null
        }
    }
}
