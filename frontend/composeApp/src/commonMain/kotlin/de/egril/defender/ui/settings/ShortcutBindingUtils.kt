package de.egril.defender.ui.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

data class ShortcutBinding(
    val keyToken: String,
    val ctrl: Boolean,
    val alt: Boolean,
    val shift: Boolean,
    val meta: Boolean
)

fun keyToShortcutToken(key: Key): String = when (key) {
    Key.DirectionUp -> "UP"
    Key.DirectionDown -> "DOWN"
    Key.DirectionLeft -> "LEFT"
    Key.DirectionRight -> "RIGHT"
    Key.Enter -> "ENTER"
    Key.Tab -> "TAB"
    Key.Spacebar -> "SPACE"
    Key.Escape -> "ESCAPE"
    Key.Backspace -> "BACKSPACE"
    Key.Delete -> "DELETE"
    else -> {
        // Compose Key.toString() on desktop returns "Key: X" (with "Key: " prefix).
        // Strip that prefix so the token is just the key name (e.g. "X", not "KEY:_X").
        val raw = key.toString()
        val name = if (raw.startsWith("Key: ", ignoreCase = true)) raw.substring(5) else raw
        name.trim().uppercase().replace(' ', '_')
    }
}

fun parseShortcutBinding(binding: String): ShortcutBinding? {
    val tokens = binding
        .split("+")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    var ctrl = false
    var alt = false
    var shift = false
    var meta = false
    var keyToken: String? = null

    tokens.forEach { token ->
        when (token.uppercase()) {
            "CTRL", "CONTROL" -> ctrl = true
            "ALT" -> alt = true
            "SHIFT" -> shift = true
            "META", "CMD", "COMMAND", "WIN" -> meta = true
            // Replace spaces to keep the serialized token format stable
            // with key names like "Page Up" => "PAGE_UP".
            // Also normalize old-format tokens that start with "KEY:_" (produced by
            // a previous version of keyToShortcutToken that didn't strip the "Key: " prefix).
            else -> {
                val raw = token.uppercase().replace(' ', '_')
                // Strip legacy "KEY:_" prefix if present (e.g. "KEY:_X" → "X")
                keyToken = if (raw.startsWith("KEY:_")) raw.removePrefix("KEY:_")
                else if (raw == "KEY:") "KEY"
                else raw
            }
        }
    }

    return keyToken?.let {
        ShortcutBinding(
            keyToken = it,
            ctrl = ctrl,
            alt = alt,
            shift = shift,
            meta = meta
        )
    }
}

fun normalizeShortcutBinding(binding: String, defaultBinding: String): String {
    val parsed = parseShortcutBinding(binding) ?: return defaultBinding
    val parts = buildList {
        if (parsed.ctrl) add("Ctrl")
        if (parsed.alt) add("Alt")
        if (parsed.shift) add("Shift")
        if (parsed.meta) add("Meta")
        add(parsed.keyToken)
    }
    return parts.joinToString("+")
}

fun formatShortcutBindingForDisplay(binding: String): String = binding
    .replace('_', ' ')
    .replace("ESCAPE", "Esc")
    .replace("ENTER", "Enter")
    .replace("SPACE", "Space")
    .replace("BACKSPACE", "Backspace")
    .replace("DELETE", "Del")
    .replace("TAB", "Tab")

fun isShortcutBindingChanged(current: String, defaultBinding: String): Boolean {
    val parsedCurrent = parseShortcutBinding(current) ?: return false
    val parsedDefault = parseShortcutBinding(defaultBinding) ?: return false
    return parsedCurrent.keyToken != parsedDefault.keyToken ||
            parsedCurrent.ctrl != parsedDefault.ctrl ||
            parsedCurrent.alt != parsedDefault.alt ||
            parsedCurrent.shift != parsedDefault.shift ||
            parsedCurrent.meta != parsedDefault.meta
}

fun buildShortcutBindingFromEvent(event: KeyEvent): String? {
    val keyToken = when (event.key) {
        Key.CtrlLeft, Key.CtrlRight,
        Key.ShiftLeft, Key.ShiftRight,
        Key.AltLeft, Key.AltRight,
        Key.MetaLeft, Key.MetaRight -> null
        else -> keyToShortcutToken(event.key)
    } ?: return null

    val parts = buildList {
        if (event.isCtrlPressed) add("Ctrl")
        if (event.isAltPressed) add("Alt")
        if (event.isShiftPressed) add("Shift")
        if (event.isMetaPressed) add("Meta")
        add(keyToken)
    }
    return parts.joinToString("+")
}

fun isShortcutBindingPressed(event: KeyEvent, binding: String): Boolean {
    val parsed = parseShortcutBinding(binding) ?: return false
    return parsed.keyToken == keyToShortcutToken(event.key) &&
            parsed.ctrl == event.isCtrlPressed &&
            parsed.alt == event.isAltPressed &&
            parsed.shift == event.isShiftPressed &&
            parsed.meta == event.isMetaPressed
}
