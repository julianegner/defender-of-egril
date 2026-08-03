package de.egril.defender.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.settings.AppSettings

/** Shared size and shape constants for [SplitButton]. */
object SplitButtonDefaults {
    /** Width of the chevron selector button on the right side. */
    val SelectorButtonWidth = 42.dp

    /** Gap between the main button area and the selector button. */
    val Gap = 3.dp

    /** Default height for a single dropdown list item. */
    val ItemHeight = 64.dp

    /**
     * Shape for the main (left) button — rounded on the start side, flat on the end side.
     * Pass this to the composable rendered in [SplitButton]'s `mainContent` slot.
     */
    val MainButtonShape: Shape =
        RoundedCornerShape(
            topStartPercent = 50,
            bottomStartPercent = 50,
            topEndPercent = 0,
            bottomEndPercent = 0,
        )

    /**
     * Shape of the chevron selector button — flat start side, fully rounded end side.
     * Uses a large fixed corner size that gets clamped to height/2 by the renderer,
     * giving the same semicircle curvature as the left side of [MainButtonShape].
     */
    val SelectorButtonShape: Shape =
        RoundedCornerShape(
            topStartPercent = 0,
            bottomStartPercent = 0,
            topEndPercent = 100,
            bottomEndPercent = 100,
        )
}

/**
 * A generic Material 3-style split button.
 *
 * The control is composed of two parts placed side by side with a small [SplitButtonDefaults.Gap]:
 * - **Main button** (left): arbitrary content provided via [mainContent], rendered inside a [Row]
 *   with `weight(1f)` so it fills all remaining width. The caller should apply
 *   [SplitButtonDefaults.MainButtonShape] to their button for a consistent look.
 * - **Selector button** (right): a fixed-width button showing a chevron icon. Clicking it
 *   toggles the [expanded] dropdown. The chevron rotates 180° while the menu is open.
 *
 * The dropdown opens **above** the split button as an overlay (does not affect layout height),
 * matching the width of the measured button row, and uses theme-aware colors so it works
 * correctly in light mode, dark mode, and colorblind themes.
 *
 * @param expanded         Whether the dropdown is currently visible.
 * @param onExpandedChange Called when the user opens or closes the dropdown.
 * @param itemCount        Number of items in the dropdown, used to estimate the popup height for
 *                         the upward offset calculation.
 * @param buttonHeight     Height of the split button row.
 * @param modifier         Modifier applied to the outer [Box] container.
 * @param enabled          When `false` the selector chevron is disabled and cannot be clicked.
 * @param selectorShortcutText Optional keyboard shortcut label shown on the selector button when
 *                             [AppSettings.showButtonShortcutHints] is enabled.
 * @param onDropdownKeyEvent   Optional key event handler invoked while the dropdown is focused/open.
 *                             Return `true` to consume the event, `false` to let it propagate.
 * @param dropdownContent  Composable content for the dropdown list (rendered in a [ColumnScope]).
 * @param mainContent      Composable content for the main button area (rendered in a [RowScope]).
 *                         Apply `Modifier.weight(1f)` inside here so the area fills available width.
 */
@Composable
fun SplitButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    itemCount: Int,
    buttonHeight: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemHeight: Dp = SplitButtonDefaults.ItemHeight,
    selectorShortcutText: String? = null,
    onDropdownKeyEvent: ((KeyEvent) -> Boolean)? = null,
    dropdownContent: @Composable ColumnScope.() -> Unit,
    mainContent: @Composable RowScope.() -> Unit,
) {
    // Estimate list height to position the popup above the button without overlap.
    val listVerticalPadding = 16.dp
    val listHeightEstimate = (itemHeight * itemCount) + listVerticalPadding

    val rectangularShape = RoundedCornerShape(0.dp)

    // Measure the actual rendered width of the button row so the dropdown matches it exactly.
    var buttonRowWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val buttonRowWidth =
        remember(buttonRowWidthPx, density) {
            with(density) { if (buttonRowWidthPx > 0) buttonRowWidthPx.toDp() else 0.dp }
        }

    Box(modifier = modifier) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier =
                Modifier
                    .let { if (buttonRowWidth > 0.dp) it.width(buttonRowWidth) else it.fillMaxWidth() }
                    .background(color = MaterialTheme.colorScheme.surface, shape = rectangularShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, rectangularShape)
                    .let { if (onDropdownKeyEvent != null) it.onPreviewKeyEvent(onDropdownKeyEvent) else it },
            offset = DpOffset(0.dp, -(listHeightEstimate + buttonHeight + SplitButtonDefaults.Gap)),
            properties = PopupProperties(focusable = false),
            content = dropdownContent,
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .onGloballyPositioned { buttonRowWidthPx = it.size.width },
            horizontalArrangement = Arrangement.spacedBy(SplitButtonDefaults.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            mainContent()

            Button(
                onClick = { onExpandedChange(!expanded) },
                enabled = enabled,
                modifier =
                    Modifier
                        .width(SplitButtonDefaults.SelectorButtonWidth)
                        .fillMaxHeight(),
                shape = SplitButtonDefaults.SelectorButtonShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                if (selectorShortcutText != null && AppSettings.showButtonShortcutHints.value) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        TriangleDownIcon(
                            size = 24.dp,
                            tint = LocalContentColor.current,
                            modifier = Modifier.graphicsLayer(rotationZ = if (expanded) 180f else 0f),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .border(1.dp, LocalContentColor.current, RoundedCornerShape(0.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = selectorShortcutText,
                                color = LocalContentColor.current,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TriangleDownIcon(
                            size = 32.dp,
                            tint = LocalContentColor.current,
                            modifier = Modifier.graphicsLayer(rotationZ = if (expanded) 180f else 0f),
                        )
                    }
                }
            }
        }
    }
}
