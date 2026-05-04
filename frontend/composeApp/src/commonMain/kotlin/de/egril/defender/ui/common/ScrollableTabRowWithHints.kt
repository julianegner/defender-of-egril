package de.egril.defender.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.scroll_hint_more_tabs_left
import defender_of_egril.composeapp.generated.resources.scroll_hint_more_tabs_right

/**
 * A [PrimaryScrollableTabRow] with left/right chevron hints that appear when
 * there are more tabs outside the visible area in that direction.
 *
 * The hints are driven by the tab row's actual scroll state:
 * - Left (‹) shown when the row has been scrolled right (tabs exist to the left)
 * - Right (›) shown when more tabs are still reachable by scrolling right
 *
 * A fixed 20dp box is always reserved on each side so the tab row width stays
 * stable as hints appear and disappear during tab navigation.
 *
 * @param selectedTabIndex Index of the currently selected tab.
 * @param modifier Applied to the outer [Row] that contains the hints and tab row.
 * @param edgePadding Forwarded to [PrimaryScrollableTabRow].
 * @param tabs The [Tab] composables for the row.
 */
@Composable
fun ScrollableTabRowWithHints(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
    tabs: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Left hint – visible once the row has been scrolled past its start
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (scrollState.canScrollBackward) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.scroll_hint_more_tabs_left),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.weight(1f),
            scrollState = scrollState,
            edgePadding = edgePadding
        ) {
            tabs()
        }

        // Right hint – visible when more tabs are reachable by scrolling right
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (scrollState.canScrollForward) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.scroll_hint_more_tabs_right),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
