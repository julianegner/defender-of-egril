package de.egril.defender.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

fun Modifier.a11ySemantics(
    role: Role? = null,
    label: String? = null,
    stateDescription: String? = null,
    liveRegionMode: LiveRegionMode? = null
): Modifier = semantics {
    if (!label.isNullOrBlank()) {
        contentDescription = label
    }
    if (!stateDescription.isNullOrBlank()) {
        this.stateDescription = stateDescription
    }
    if (role != null) {
        this.role = role
    }
    if (liveRegionMode != null) {
        this.liveRegion = liveRegionMode
    }
}

fun Modifier.requireContentDescription(
    contentDescription: String?,
    decorative: Boolean = false
): Modifier {
    // Intentionally debug-only: `assert` is enabled in test/debug runs and disabled in release builds.
    assert(decorative || !contentDescription.isNullOrBlank()) {
        "A non-decorative image/icon is missing a contentDescription."
    }
    return this
}
