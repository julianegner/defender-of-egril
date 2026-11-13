package de.egril.defender.ui.worldmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.editor.worldmap.LocationIconUtils
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.world_map_loading
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val TIDE_BAND_COUNT = 12

/**
 * Every image and animation resource the world map draws. They are fetched together so the
 * map is shown in one piece instead of popping in element by element on web/wasm.
 * Paths must match the tide slices in [WorldMapTideAnimation] and the icons in [LocationIconUtils].
 */
private val worldMapAssetPaths: List<String> =
    buildList {
        add("drawable/world_map_background.png")
        for (band in 1..TIDE_BAND_COUNT) {
            add("drawable/world_map_tide_band${band.toString().padStart(2, '0')}.png")
        }
        LocationIconUtils.AVAILABLE_LOCATION_ICONS.forEach { add("drawable/location_$it.png") }
        add("files/animations/world_map_rivers.json")
    }

// Paths already fetched in this session, so reopening the map does not show the spinner again
private val loadedWorldMapAssetPaths = mutableSetOf<String>()

/**
 * Fetches all world map assets in the background and reports when they are ready.
 * A failed fetch is not retried in the same session and does not block the map forever.
 */
@Composable
internal fun rememberWorldMapAssetsReady(): State<Boolean> =
    produceState(initialValue = worldMapAssetPaths.all { it in loadedWorldMapAssetPaths }) {
        if (value) return@produceState
        coroutineScope {
            worldMapAssetPaths
                .filterNot { it in loadedWorldMapAssetPaths }
                .map { path ->
                    async {
                        runCatching { Res.readBytes(path) }
                        loadedWorldMapAssetPaths.add(path)
                    }
                }.awaitAll()
        }
        value = true
    }

@Composable
internal fun WorldMapLoadingIndicator(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
) {
    Box(
        modifier = modifier.fillMaxSize().background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.world_map_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
