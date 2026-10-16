package de.egril.defender.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

@Composable
expect fun BoxScope.EditorHorizontalScrollbar(scrollState: ScrollState)
